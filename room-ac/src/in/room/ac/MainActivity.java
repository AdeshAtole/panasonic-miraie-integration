package in.room.ac;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
public class MainActivity extends Activity {
 final ExecutorService worker=Executors.newSingleThreadExecutor();
 LinearLayout box,tempPanel,fanPanel;ScrollView scroll;TextView message,tempLabel,modeNote,fanNote;
 Spinner modeSpinner,fanSpinner;Button modeButton,tempButton,fanButton;
 DeviceProfile profile;JSONObject fields=new JSONObject();List<String> modes=new ArrayList<>(),fans=new ArrayList<>();
 String chosenMode="",chosenFan="";double chosenTemp=24;boolean busy=false,alive=true;
 interface Job{String run()throws Exception;}
 public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);show();try{if(Api.selected(this)!=null)refresh(false);}catch(Exception ignored){}}
 void show(){
  box=new LinearLayout(this);box.setOrientation(1);box.setPadding(40,100,40,60);box.setBackgroundColor(Color.rgb(17,25,38));
  text("Room AC",32,Color.WHITE);text("Your room. Your controls.",16,Color.rgb(154,215,205));message=text("",16,Color.LTGRAY);message.setPadding(0,28,0,28);
  try{if(Vault.load(this).optString("accessToken").isEmpty())loginForm();else controls();}catch(Exception e){text("Could not read the saved session. Sign in again.",16,Color.LTGRAY);button("Clear local session",()->{Vault.clear(this);show();});}
  scroll=new ScrollView(this);scroll.addView(box);setContentView(scroll);
 }
 void redraw(){int y=scroll==null?0:scroll.getScrollY();show();scroll.post(()->scroll.scrollTo(0,y));}
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);box.addView(t);return t;}
 EditText input(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine();e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setInputType(type);box.addView(e);return e;}
 Button button(String name,Runnable action){Button b=new Button(this);b.setText(name);b.setOnClickListener(v->{if(!busy)action.run();});box.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;}
 void loginForm(){
  text("Sign in to your Panasonic MirAIe account",20,Color.WHITE);text("Credentials go directly to Panasonic over HTTPS. Your password is not saved. The saved session is encrypted on this phone.",15,Color.LTGRAY);
  EditText identity=input("Mobile number or email",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);identity.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
  EditText password=input("MirAIe password",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
  button("Sign in",()->{String who=identity.getText().toString().trim(),pass=password.getText().toString();if(who.isEmpty()||pass.isEmpty()){message.setText("Enter your mobile number or email and password.");return;}password.setText("");work("Signing in…",()->{Api.login(this,who,pass);return "Signed in";},()->discover());});
 }
 void discover(){work("Finding your air conditioners…",()->{JSONArray ds=Api.devices(this);JSONObject s=Vault.load(this);s.put("devices",ds);if(ds.length()==1)s.put("device",ds.getJSONObject(0));Vault.save(this,s);return ds.length()==0?"No supported ACs were found.":"Choose your AC";},()->{show();try{if(Api.selected(this)!=null)refresh(false);}catch(Exception ignored){}});}
 void controls()throws Exception{
  JSONObject session=Vault.load(this),selected=session.optJSONObject("device");JSONArray devices=session.optJSONArray("devices");
  if(selected==null){text("Choose an air conditioner",20,Color.WHITE);if(devices!=null)for(int i=0;i<devices.length();i++){JSONObject d=devices.getJSONObject(i);button(d.optString("deviceName","AC")+" · "+d.optString("spaceName"),()->work("Selecting AC…",()->{JSONObject s=Vault.load(this);s.put("device",d);Vault.save(this,s);getSharedPreferences("view",0).edit().clear().commit();return "Selected";},()->{show();refresh(false);}));}button("Find my ACs",()->discover());button("Sign out",()->signOut());return;}
  profile=new DeviceProfile(Api.cachedProfile(this));fields=Api.cachedFields(this);
  message.setText(getSharedPreferences("view",0).getString("status","Tap Refresh to read your AC status."));
  text(selected.optString("deviceName","Panasonic AC"),24,Color.WHITE);text(selected.optString("spaceName"),15,Color.LTGRAY);
  button("Refresh status",()->refresh(false));button("Power on",()->send(Api.powerKey(this),"on"));button("Power off",()->send(Api.powerKey(this),"off"));
  text("Operating mode",20,Color.WHITE);
  String currentMode=fields.optString(profile.key("ACMode","acmd"),"");modes=profile.modes();
  if(modes.isEmpty()){
   text(currentMode.isEmpty()?"Supported modes have not loaded.":"Current mode: "+DeviceProfile.label(currentMode),16,Color.LTGRAY);
   text("Refresh device capabilities to load this AC’s mode controls.",14,Color.GRAY);
  }else{
   chosenMode=modes.contains(currentMode)?currentMode:modes.get(0);modeSpinner=spinner(modes,chosenMode,"Operating mode");
   modeButton=button("Apply mode",()->applyMode());
   modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){chosenMode=modes.get(pos);updateModeControls();}});
  }
  modeNote=text("",15,Color.LTGRAY);
  tempPanel=new LinearLayout(this);tempPanel.setOrientation(1);box.addView(tempPanel);
  TextView th=new TextView(this);th.setText("Target temperature");th.setTextSize(18);th.setTextColor(Color.WHITE);tempPanel.addView(th);
  chosenTemp=getSharedPreferences("view",0).getFloat("target_value",24);
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);tempLabel=new TextView(this);tempLabel.setTextSize(28);tempLabel.setTextColor(Color.WHITE);tempLabel.setGravity(Gravity.CENTER);
  Button minus=new Button(this);minus.setText("−");minus.setContentDescription("Decrease selected temperature");minus.setOnClickListener(v->{if(!busy){double[] r=profile.temperatureRange(chosenMode);chosenTemp=Math.max(r[0],chosenTemp-r[2]);updateTemperatureLabel();}});
  Button plus=new Button(this);plus.setText("+");plus.setContentDescription("Increase selected temperature");plus.setOnClickListener(v->{if(!busy){double[] r=profile.temperatureRange(chosenMode);chosenTemp=Math.min(r[1],chosenTemp+r[2]);updateTemperatureLabel();}});
  row.addView(minus,new LinearLayout.LayoutParams(0,-2,1));row.addView(tempLabel,new LinearLayout.LayoutParams(0,-2,2));row.addView(plus,new LinearLayout.LayoutParams(0,-2,1));tempPanel.addView(row);
  tempButton=new Button(this);tempButton.setText("Set temperature");tempButton.setOnClickListener(v->{if(!busy&&tempButton.isEnabled())send(profile.key("ACModeTemperature","actmp"),Double.toString(chosenTemp));});tempPanel.addView(tempButton);
  fans=profile.fanSpeeds();fanPanel=new LinearLayout(this);fanPanel.setOrientation(1);box.addView(fanPanel);
  TextView fh=new TextView(this);fh.setText("Fan speed");fh.setTextSize(18);fh.setTextColor(Color.WHITE);fanPanel.addView(fh);
  if(!fans.isEmpty()){String current=fields.optString(profile.key("ACFanSpeed","acfs"));chosenFan=fans.contains(current)?current:fans.get(0);fanSpinner=spinnerIn(fanPanel,fans,chosenFan,"Fan speed");fanSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){chosenFan=fans.get(pos);}});fanButton=new Button(this);fanButton.setText("Set fan speed");fanButton.setOnClickListener(v->{if(!busy&&fanButton.isEnabled())send(profile.key("ACFanSpeed","acfs"),chosenFan);});fanPanel.addView(fanButton);}else{fanSpinner=null;fanButton=null;}
  fanNote=text("",15,Color.LTGRAY);updateModeControls();
  if(profile.capability("ECOMode")!=null||profile.capability("PowerfulMode")!=null||!profile.intValues("Convertible").isEmpty())button("Additional AC modes",()->additionalModes());
  text("Reported readings",20,Color.WHITE);text(Telemetry.panel(profile,fields),16,Color.LTGRAY);
  text("Recorded snapshots: "+MetricStore.count(this)+" · saved when you refresh",14,Color.GRAY);
  String recordingError=getSharedPreferences("view",0).getString("recording_error","");if(!recordingError.isEmpty())text(recordingError,14,Color.LTGRAY);
  String energy=getSharedPreferences("view",0).getString("energy_summary","");if(!energy.isEmpty())text(energy,16,Color.LTGRAY);
  button("Load recent energy use",()->work("Reading recent energy use…",()->{JSONObject history=Api.energyHistory(this);Object data=history.opt("data");JSONArray readings=data instanceof JSONArray?(JSONArray)data:(data instanceof JSONObject?((JSONObject)data).optJSONArray("data"):null);if(readings==null)throw new java.io.IOException("Energy history returned an unexpected format");java.math.BigDecimal total=java.math.BigDecimal.ZERO;int count=0;for(int i=0;i<readings.length();i++){JSONObject reading=readings.optJSONObject(i);if(reading!=null&&reading.has("power")){try{total=total.add(new java.math.BigDecimal(reading.getString("power")));count++;}catch(Exception ignored){}}}String label=count==0?"No energy readings were returned for the last 7 days.":"Reported energy, "+history.optString("start_date")+" to "+history.optString("end_date")+": "+total.stripTrailingZeros().toPlainString()+" kWh ("+count+" reported entries)";getSharedPreferences("view",0).edit().putString("energy_summary",label).putString("energy_history",Api.redact(history).toString()).commit();return label;},()->redraw()));
  if(!getSharedPreferences("view",0).getString("energy_history","").isEmpty())button("Export energy history (CSV)",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv").putExtra(Intent.EXTRA_TITLE,"room-ac-energy-"+android.text.format.DateFormat.format("yyyyMMdd-HHmmss",System.currentTimeMillis())+".csv"),401));
  button("All reported fields",()->new AlertDialog.Builder(this).setTitle("All reported AC fields").setMessage(Telemetry.all(Api.cachedFields(this))).setPositiveButton("Close",null).show());
  button("Export readings (CSV)",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv").putExtra(Intent.EXTRA_TITLE,"room-ac-readings-"+android.text.format.DateFormat.format("yyyyMMdd-HHmmss",System.currentTimeMillis())+".csv"),400));
  button("Refresh device capabilities",()->refresh(true));
  button("Inspect AC data",()->work("Reading AC capabilities and telemetry…",()->{JSONObject report=Api.inspect(this);java.io.File f=new java.io.File(getExternalFilesDir(null),"ac-diagnostics.json");try(java.io.FileOutputStream out=new java.io.FileOutputStream(f)){out.write(report.toString(2).getBytes("UTF-8"));}Api.cacheStatus(this,report.getJSONObject("status"));return "AC data snapshot collected.";},()->redraw()));
  button("Add home-screen widget",()->{AppWidgetManager manager=getSystemService(AppWidgetManager.class);if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,AcWidget.class),null,null);else message.setText("Long-press your home screen, choose Widgets, then Room AC.");});
  button("Choose another AC",()->work("Loading…",()->{JSONObject s=Vault.load(this);s.remove("device");Vault.save(this,s);getSharedPreferences("view",0).edit().clear().commit();return "Choose an AC";},()->{AcWidget.updateAll(this);show();}));
  button("Sign out",()->signOut());text("Internet access is required. Readings are cloud-reported snapshots, not a continuous sensor stream. The widget refreshes manually.",14,Color.GRAY);
 }
 Spinner spinner(List<String> values,String selected,String name){return spinnerIn(box,values,selected,name);}
 Spinner spinnerIn(LinearLayout parent,List<String> values,String selected,String name){Spinner s=new Spinner(this);s.setContentDescription(name);List<String> labels=new ArrayList<>();for(String value:values)labels.add(DeviceProfile.label(value));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(adapter);s.setSelection(Math.max(0,values.indexOf(selected)));parent.addView(s,new LinearLayout.LayoutParams(-1,-2));return s;}
 void enabled(View view,boolean value){if(view==null)return;view.setEnabled(value);if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)enabled(((android.view.ViewGroup)view).getChildAt(i),value);view.setAlpha(value?1f:0.45f);}
 void updateTemperatureLabel(){if(tempLabel!=null)tempLabel.setText(format(chosenTemp)+"°C");}
 static String format(double n){return Math.rint(n)==n?Long.toString(Math.round(n)):java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();}
 void updateModeControls(){if(modeNote==null||tempPanel==null||profile==null)return;
  String current=fields.optString(profile.key("ACMode","acmd"));boolean powered="on".equalsIgnoreCase(fields.optString(profile.key("PowerStatus","ps"))),pending=!chosenMode.equals(current),ready=powered&&!pending&&!busy&&!modes.isEmpty();boolean cleaning="on".equalsIgnoreCase(fields.optString(profile.key("EvaporatorClean","acec")));
  if(modeSpinner!=null){modeSpinner.setEnabled(powered&&!busy);modeButton.setEnabled(powered&&!busy);}
  double[] range=profile.temperatureRange(chosenMode);chosenTemp=Math.max(range[0],Math.min(range[1],chosenTemp));updateTemperatureLabel();
  boolean fanOnly="fan".equals(chosenMode),dry="dry".equals(chosenMode);tempPanel.setVisibility(fanOnly||modes.isEmpty()?View.GONE:View.VISIBLE);enabled(tempPanel,ready&&!cleaning);
  modeNote.setText(!powered?"Power on the AC to change mode and settings.":pending?"Apply the selected mode to update the AC controls.":cleaning?"Evaporator cleaning is active; temperature and fan controls are unavailable.":fanOnly?"Fan-only circulates air; there is no temperature setting.":"Temperature range: "+format(range[0])+"–"+format(range[1])+"°C");
  fanPanel.setVisibility(fans.isEmpty()?View.GONE:View.VISIBLE);enabled(fanPanel,ready&&!dry&&!cleaning);
  fanNote.setText(dry?"Dry mode manages fan speed automatically.":fans.isEmpty()?"Fan-speed choices are not reported by this model.":"Fan speed is a setting, not measured RPM.");
 }
 void applyMode(){if(!modes.contains(chosenMode))return;try{JSONObject changes=new JSONObject().put(profile.key("ACMode","acmd"),chosenMode);command(changes);}catch(Exception e){message.setText(Api.safeError(e));}}
 void additionalModes(){
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(32,20,32,20);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Additional AC modes").setView(content).setNegativeButton("Close",null).create();
  boolean powered="on".equalsIgnoreCase(fields.optString(profile.key("PowerStatus","ps")));
  String mode=fields.optString(profile.key("ACMode","acmd"));boolean coolMode="cool".equals(mode),cleaning="on".equalsIgnoreCase(fields.optString(profile.key("EvaporatorClean","acec")));
  if(!coolMode){TextView restriction=new TextView(this);restriction.setText("Eco and Powerful are available in Cool mode, matching Panasonic’s app.");content.addView(restriction);}
  if(profile.capability("ECOMode")!=null){boolean on="on".equalsIgnoreCase(fields.optString(profile.key("ECOMode","acem")));Button eco=new Button(this);eco.setText("Eco: "+(on?"On — turn off":"Off — turn on"));eco.setEnabled(powered&&coolMode);content.addView(eco);TextView info=new TextView(this);info.setText("Enabling Eco sets the target to 26°C, matching Panasonic’s app.");content.addView(info);eco.setOnClickListener(v->{dialog.dismiss();try{JSONObject toggle=new JSONObject().put(profile.key("ECOMode","acem"),on?"off":"on").put("ki",10);if(!on)sequence(toggle,new JSONObject().put(profile.key("ACTemperature","actmp"),"26.0"));else sequence(toggle);}catch(Exception e){message.setText(Api.safeError(e));}});}
  if(profile.capability("PowerfulMode")!=null){boolean on="on".equalsIgnoreCase(fields.optString(profile.key("PowerfulMode","acpm")));Button powerful=new Button(this);powerful.setText("Powerful: "+(on?"On — turn off":"Off — turn on"));powerful.setEnabled(powered&&coolMode&&!cleaning);content.addView(powerful);powerful.setOnClickListener(v->{dialog.dismiss();try{JSONObject toggle=new JSONObject().put(profile.key("PowerfulMode","acpm"),on?"off":"on").put("ki",3);if(!on&&profile.capability("Convertible")!=null)sequence(new JSONObject().put(profile.key("Convertible","cnv"),0).put("ki",28),toggle);else sequence(toggle);}catch(Exception e){message.setText(Api.safeError(e));}});}
  List<Integer> capacities=profile.intValues("Convertible");if(!capacities.isEmpty()){TextView label=new TextView(this);label.setText("Convertible capacity setting");content.addView(label);List<String> names=new ArrayList<>();for(Integer n:capacities)names.add(n==0?"Off (normal)":n+"%");Spinner capacity=new Spinner(this);capacity.setContentDescription("Convertible capacity");ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);capacity.setAdapter(adapter);capacity.setSelection(Math.max(0,capacities.indexOf(fields.optInt(profile.key("Convertible","cnv"),0))));content.addView(capacity);Button apply=new Button(this);apply.setText("Set capacity");apply.setEnabled(powered);content.addView(apply);apply.setOnClickListener(v->{int n=capacities.get(capacity.getSelectedItemPosition());dialog.dismiss();try{JSONObject change=new JSONObject().put(profile.key("Convertible","cnv"),n).put("ki",28);if(n!=0&&"on".equals(fields.optString(profile.key("PowerfulMode","acpm"))))sequence(new JSONObject().put(profile.key("PowerfulMode","acpm"),"off").put("ki",3),change);else sequence(change);}catch(Exception e){message.setText(Api.safeError(e));}});TextView note=new TextView(this);note.setText("Powerful mode and a fixed capacity setting are mutually exclusive.");content.addView(note);}
  dialog.show();
 }
 void signOut(){if(busy)return;Vault.clear(this);AcWidget.updateAll(this);show();}
 void refresh(boolean force){work("Reading AC status…",()->{try{Api.profile(this,force);getSharedPreferences("view",0).edit().remove("capabilities_error").commit();}catch(Exception e){getSharedPreferences("view",0).edit().putString("capabilities_error",Api.safeError(e)).commit();}JSONObject state=Api.status(this);Api.cacheStatus(this,state);return "Status refreshed";},()->redraw());}
 void send(String key,String value){try{command(new JSONObject().put(key,value));}catch(Exception e){message.setText(Api.safeError(e));}}
 void command(JSONObject changes){sequence(changes);}
 boolean confirmed(JSONObject state,JSONObject changes){JSONObject f=Api.fields(state);Iterator<String> keys=changes.keys();while(keys.hasNext()){String k=keys.next();if(k.equals("ki"))continue;Object a=f.opt(k),b=changes.opt(k);if(a==null||a==JSONObject.NULL)return false;if(String.valueOf(a).equalsIgnoreCase(String.valueOf(b)))continue;try{if(Double.compare(Double.parseDouble(a.toString()),Double.parseDouble(b.toString()))==0)continue;}catch(Exception ignored){}return false;}return true;}
 void sequence(JSONObject... commands){work("Sending command…",()->{JSONObject expected=new JSONObject();JSONObject state=null;for(JSONObject changes:commands){Api.control(this,changes);Iterator<String> keys=changes.keys();while(keys.hasNext()){String key=keys.next();if(!key.equals("ki"))expected.put(key,changes.get(key));}state=Api.status(this);for(int i=0;i<5&&!confirmed(state,expected);i++){Thread.sleep(600L*(i+1));state=Api.status(this);}if(!confirmed(state,expected))break;}if(state==null)state=Api.status(this);Api.cacheStatus(this,state);getSharedPreferences("view",0).edit().putString("command_note",confirmed(state,expected)?"":"Command accepted; some settings are not confirmed yet. Refresh to check.").commit();return "Command processed";},()->{redraw();String note=getSharedPreferences("view",0).getString("command_note","");if(!note.isEmpty())message.append("\n"+note);});}
 void work(String label,Job job,Runnable done){if(busy)return;busy=true;message.setText(label);updateModeControls();worker.execute(()->{String value;boolean ok;try{value=job.run();ok=true;}catch(Exception e){value=Api.safeError(e);ok=false;}final String result=value;final boolean success=ok;runOnUiThread(()->{if(!alive)return;busy=false;message.setText(result);updateModeControls();if(success&&done!=null)done.run();});});}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if((request==400||request==401)&&result==RESULT_OK&&data!=null&&data.getData()!=null){android.net.Uri uri=data.getData();work("Exporting readings…",()->{try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Could not open the selected file");if(request==401)MetricStore.exportEnergy(new JSONObject(getSharedPreferences("view",0).getString("energy_history","{}")),out);else MetricStore.export(this,out);}return "Readings exported.";},null);}}
 protected void onDestroy(){alive=false;worker.shutdown();super.onDestroy();}
}
