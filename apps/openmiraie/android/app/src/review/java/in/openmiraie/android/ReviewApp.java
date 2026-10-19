package in.openmiraie.android;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Offline UI review only: separate package, no INTERNET permission or saved account. */
public final class ReviewApp extends MiraieApp {
    @Override Repository createRepository(){return new ReviewRepository(this);}
}

final class ReviewRepository extends Repository {
    int refreshCalls;
    ReviewRepository(Context context){super(context,false);load();}
    @Override void load(){
        try {
            JSONObject model=new JSONObject().put("modelId","140013");JSONArray caps=new JSONArray();
            String[][] mappings={{"PowerStatus","ps"},{"DisplayControl","acdc"},{"ACMode","acmd"},{"ACTemperature","actmp"},{"ACFanSpeed","acfs"},{"ECOMode","acem"},{"PowerfulMode","acpm"},{"Convertible","cnv"},{"EvaporatorClean","acec"},{"VerticalSwing","acvs"},{"HorizontalSwing","achs"}};
            for(String[] mapping:mappings){JSONObject attr=new JSONObject().put("key",mapping[1]);
                switch(mapping[0]){
                    case "ACMode":attr.put("values",new JSONArray("['auto','cool','dry','fan']"));break;
                    case "ACFanSpeed":attr.put("values",new JSONArray("['auto','quiet','low','medium','high']"));break;
                    case "Convertible":attr.put("intValues",new JSONArray("[0,40,50,60,70,80,90,100,110]"));break;
                    case "ACTemperature":attr.put("range",new JSONObject("{min:[{unit:'celcius',value:16}],max:[{unit:'celcius',value:30}],increment:[{unit:'celcius',value:1}]}"));break;
                    case "VerticalSwing":case "HorizontalSwing":attr.put("range",new JSONObject("{min:[{value:0}],max:[{value:5}],increment:[{value:1}]}"));break;
                }
                caps.put(new JSONObject().put("capability",mapping[0]).put("attributes",new JSONArray().put(attr)));
            }
            devices=new ArrayList<>();
            for(String room:new String[]{"Bedroom","Living room"})devices.add(new JSONObject().put("deviceId","synthetic-"+devices.size()).put("category","AC").put("modelId","140013").put("spaceName",room).put("homeName","Sample home"));
            device=devices.get(0);profile=new Profile(model.put("controlCapabilities",caps),device);
            observed=new JSONObject("{ps:'on',acdc:'on',acmd:'cool',actmp:'24',acfs:'auto',acem:'off',acpm:'off',cnv:0,acec:'off',onlineStatus:true,rmtmp:27.4,acvs:1,achs:1}");
            signed=true;discoveryLoaded=true;readAt=System.currentTimeMillis();notice="";
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    @Override void login(String identity,String password){load();publish();}
    @Override void discover(){publish();}
    @Override void discoverNow(){}
    @Override void select(JSONObject selected){device=selected;publish();}
    @Override void refresh(boolean capabilities){if(busy)return;refreshCalls++;busy=true;notice="Refreshing sample readings…";publish();main.postDelayed(()->{refreshNow(capabilities);busy=false;publish();},900);}
    @Override void refreshNow(boolean capabilities){readAt=System.currentTimeMillis();notice="Updated reported state.";}
    @Override synchronized void command(String name,Object value){
        if(busy)return;
        final List<Map.Entry<String,Object>> plan;
        try{plan=CommandRunner.plan(profile,observed,name,value);}catch(Exception e){notice=e.getMessage();publish();return;}
        if(name.equals("ACTemperature")){pendingTemperature=MainActivity.parse(String.valueOf(value));previousTemperature=MainActivity.parse(profile.value(observed,name));}
        pendingControl=name;requestedControlValue=String.valueOf(value);busy=true;notice="Applying sample setting…";publish();
        main.postDelayed(()->{try{for(Map.Entry<String,Object> step:plan){Profile.Command c=profile.command(step.getKey(),step.getValue(),observed);observed.put(c.key(),c.value());}notice="";}catch(Exception e){notice=e.getMessage();}finally{busy=false;pendingControl=requestedControlValue="";pendingTemperature=previousTemperature=null;readAt=System.currentTimeMillis();publish();}},900);
    }
    @Override synchronized void commandAirflow(Integer vertical,Integer horizontal,Integer expectedV,Integer expectedH){
        if(busy)return;
        final List<Map.Entry<String,Object>> steps;
        try{steps=AirflowPlan.plan(profile,observed,vertical,horizontal,expectedV,expectedH);}catch(Exception e){notice=e.getMessage();publish();return;}
        pendingAirflowV=vertical;pendingAirflowH=horizontal;busy=true;notice="Applying sample airflow…";publish();
        main.postDelayed(()->{try{for(Map.Entry<String,Object> step:steps){Profile.Command c=profile.command(step.getKey(),step.getValue(),observed);observed.put(c.key(),c.value());}notice="";}catch(Exception e){notice=e.getMessage();}finally{busy=false;pendingAirflowV=pendingAirflowH=null;publish();}},900);
    }
    @Override void loadEnergy(){try{energy=new JSONArray();for(int i=6;i>=0;i--)energy.put(new JSONObject().put("day",java.time.LocalDate.now().minusDays(i).format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"))).put("power",String.format(Locale.ROOT,"%.2f",1.3+i*.24)));energyAt=System.currentTimeMillis();publish();}catch(JSONException e){throw new IllegalStateException(e);}}
    @Override void diagnostic(){notice="Sample diagnostic; no network request.";publish();}
    @Override void signOut(){signed=false;device=null;notice="";publish();}
}
