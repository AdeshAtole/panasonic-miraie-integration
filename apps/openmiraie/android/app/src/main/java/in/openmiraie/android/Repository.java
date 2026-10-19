package in.openmiraie.android;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.Instant;

/** Application-scoped serialized operations survive configuration changes, never queue commands. */
class Repository {
    final Context context;final SessionStore sessions;final Api api;final SnapshotStore snapshots;
    final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler main=new Handler(Looper.getMainLooper());
    final List<Runnable> listeners=new ArrayList<>();
    volatile boolean busy=false,signed=false;JSONObject device;Profile profile=new Profile(null);JSONObject observed=new JSONObject();
    List<JSONObject> devices=new ArrayList<>();JSONArray energy;long readAt=0,energyAt=0;int sampleCount=0;
    volatile Double pendingTemperature,previousTemperature;
    volatile String pendingControl="",requestedControlValue="";volatile Integer pendingAirflowV,pendingAirflowH;
    String notice="",recordingWarning="";boolean discoveryLoaded=false;
    Repository(Context c){this(c,true);}
    Repository(Context c,boolean loadSession){context=c;sessions=new SessionStore(c);api=new Api(sessions);snapshots=new SnapshotStore(c);if(loadSession)load();}
    void listen(Runnable r){listeners.add(r);}void unlisten(Runnable r){listeners.remove(r);}
    void publish(){main.post(()->{for(Runnable r:new ArrayList<>(listeners))r.run();CompactWidget.updateAll(context);});}
    interface Work {void run()throws Exception;}
    synchronized boolean task(String text,Work work){
        if(busy)return false;busy=true;notice=text;publish();worker.execute(()->{
            try{work.run();}catch(Exception e){notice=Api.safe(e);try{if(e instanceof Api.Expired||sessions.load().optString("accessToken").isEmpty())clearAccount();}catch(Exception ignored){clearAccount();}}
            finally{pendingTemperature=null;previousTemperature=null;pendingControl=requestedControlValue="";pendingAirflowV=pendingAirflowH=null;busy=false;publish();}
        });return true;
    }
    void load(){task("Loading secure session…",()->{JSONObject s=sessions.load();signed=!s.optString("accessToken").isEmpty();device=s.optJSONObject("selected");notice="";if(signed){discoverNow();if(device!=null)refreshNow(true);}});}
    void login(String identity,String password){task("Signing in…",()->{api.login(identity,password);signed=true;device=null;resetValues();discoverNow();if(device!=null)refreshNow(true);});}
    void discover(){task("Finding your ACs…",()->{discoverNow();if(device!=null)refreshNow(true);});}
    void discoverNow()throws Exception {
        devices=api.discover();discoveryLoaded=true;
        if(device!=null){String id=device.optString("deviceId");JSONObject found=null;for(JSONObject d:devices)if(id.equals(d.optString("deviceId"))&&Data.supportedDevice(d))found=d;device=found;resetValues();}
        List<JSONObject> acs=new ArrayList<>();for(JSONObject d:devices)if(Data.supportedDevice(d))acs.add(d);
        if(device==null&&acs.size()==1)device=acs.get(0);
        saveSelection();notice=acs.isEmpty()?"No supported ACs found. Check account and setup in Panasonic MirAIe.":device==null?"Choose an AC.":"";
    }
    void select(JSONObject selected){if(busy)return;device=selected;resetValues();publish();task("Loading this AC…",()->{saveSelection();refreshNow(true);});}
    private void saveSelection()throws Exception {JSONObject s=sessions.load();if(device==null)s.remove("selected");else s.put("selected",device);sessions.save(s);}
    void resetValues(){profile=new Profile(null);observed=new JSONObject();energy=null;readAt=energyAt=0;sampleCount=0;recordingWarning="";}
    void clearAccount(){sessions.clear();signed=false;device=null;devices=new ArrayList<>();discoveryLoaded=false;resetValues();}
    void signOut(){if(busy)return;clearAccount();notice="Signed out. Local snapshots remain on this phone.";publish();}
    void refresh(boolean capabilities){task("Updating reported state…",()->refreshNow(capabilities));}
    void refreshNow(boolean capabilities)throws Exception {
        if(device==null)throw new IllegalArgumentException("Choose an AC first.");
        String warning="";if(capabilities||profile.raw.length()==0){try{profile=new Profile(api.model(device),device);}catch(Api.Expired e){throw e;}catch(Exception e){warning="Capabilities unavailable; retained any known profile. ";}}
        observe(api.status(device));notice=warning+(profile.online(observed)?"Updated reported state.":"AC offline or online state not reported. Last-known values are shown.");
    }
    void observe(JSONObject state)throws Exception {observed=state;readAt=System.currentTimeMillis();try{snapshots.record(device.getString("deviceId"),state,Instant.ofEpochMilli(readAt));sampleCount=snapshots.count(device.getString("deviceId"));recordingWarning="";}catch(Exception e){recordingWarning="Read succeeded, but this snapshot could not be saved.";}publish();}
    synchronized void command(String name,Object value){if(busy)return;pendingControl=name;requestedControlValue=String.valueOf(value);if(name.equals("ACTemperature")){pendingTemperature=MainActivity.parse(String.valueOf(value));previousTemperature=MainActivity.parse(profile.value(observed,"ACTemperature"));}final String consentMode=profile.value(observed,"ACMode"),consentCapacity=profile.value(observed,"Convertible"),consentPowerful=profile.value(observed,"PowerfulMode");task("Checking current state before sending…",()->{
        if(device==null)throw new IllegalArgumentException("Choose an AC first.");
        if(Swing.axis(name)&&!Data.supportedDevice(device))throw new IllegalArgumentException("Swing is unavailable for this transport.");
        // A widget or stale screen cannot issue a write against an old/offline snapshot.
        observe(api.status(device));
        if((!name.equals("PowerStatus")&&!name.equals("DisplayControl")&&!Data.same(consentMode,profile.value(observed,"ACMode"))) ||
           (name.equals("PowerfulMode")&&!Data.same(consentCapacity,profile.value(observed,"Convertible"))) ||
           (name.equals("Convertible")&&!Data.same(consentPowerful,profile.value(observed,"PowerfulMode"))))
            throw new IllegalArgumentException("Reported mode or coupled setting changed. Review it and explicitly apply again.");
        CommandRunner runner=new CommandRunner(new CommandRunner.Port(){
            public void send(Profile.Command command)throws Exception{api.send(device,command);}
            public JSONObject read()throws Exception{return api.status(device);}
            public void observed(JSONObject s)throws Exception{observe(s);}
        },Thread::sleep,message->{notice=message;publish();});
        CommandRunner.Result result=runner.execute(profile,observed,name,value);notice=result.message()+(result.confirmed()?"":" Requested "+(Swing.axis(name)?Swing.title(name)+" code "+value:name+" → "+value)+".");
        if(sessions.load().optString("accessToken").isEmpty())clearAccount();
        if(result.confirmed()&&name.equals("ACMode")){try{profile=new Profile(api.model(device),device);}catch(Exception e){notice+=" Could not refresh mode-dependent capabilities.";}}
    });}
    synchronized void commandAirflow(Integer vertical,Integer horizontal,Integer expectedV,Integer expectedH){
        if(busy||device==null)return;pendingAirflowV=vertical;pendingAirflowH=horizontal;
        final String mode=profile.value(observed,"ACMode");
        task("Checking current airflow…",()->{
            observe(api.status(device));
            if(!Data.same(mode,profile.value(observed,"ACMode")))throw new IllegalArgumentException("Mode changed. Review airflow before trying again.");
            List<Map.Entry<String,Object>> steps=AirflowPlan.plan(profile,observed,vertical,horizontal,expectedV,expectedH);
            CommandRunner runner=new CommandRunner(new CommandRunner.Port(){public void send(Profile.Command c)throws Exception{api.send(device,c);}public JSONObject read()throws Exception{return api.status(device);}public void observed(JSONObject s)throws Exception{observe(s);}},Thread::sleep,message->{notice=message;publish();});
            int completed=0;
            for(Map.Entry<String,Object> step:steps){
                // Recheck both axes: the prior confirmed step may change only its own expected code.
                Integer v=completed>0&&steps.get(0).getKey().equals(Swing.VERTICAL)?(Integer)steps.get(0).getValue():expectedV;
                Integer h=completed>0&&steps.get(0).getKey().equals(Swing.HORIZONTAL)?(Integer)steps.get(0).getValue():expectedH;
                if(!Data.same(mode,profile.value(observed,"ACMode"))||!Objects.equals(v,Swing.reportedCode(profile,observed,Swing.VERTICAL))||!Objects.equals(h,Swing.reportedCode(profile,observed,Swing.HORIZONTAL))){notice="Airflow changed after "+completed+" confirmed steps. Review before trying again.";return;}
                CommandRunner.Result result=runner.execute(profile,observed,step.getKey(),step.getValue());
                if(!result.confirmed()){notice="Confirmed "+completed+" of "+steps.size()+" airflow axes. "+result.message();return;}completed++;
            }
            notice=steps.isEmpty()?"Airflow unchanged.":"AC reports the requested airflow. Physical movement is not confirmed.";
        });
    }
    void loadEnergy(){task("Loading seven-day energy history…",()->{if(device==null)throw new IllegalArgumentException("Choose an AC.");energy=api.energy(device);energyAt=System.currentTimeMillis();notice=energy.length()==0?"No energy entries returned.":"Energy loaded. Today is partial.";});}
    void diagnostic(){task("Collecting diagnostic snapshot…",()->{refreshNow(true);JSONObject report=new JSONObject().put("observedAt",Instant.ofEpochMilli(readAt).toString()).put("model",Data.redact(profile.raw)).put("status",Data.redact(observed));try(java.io.FileOutputStream out=context.openFileOutput("diagnostic.json",Context.MODE_PRIVATE)){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}notice="Redacted diagnostic collected in app-private storage.";});}
}
