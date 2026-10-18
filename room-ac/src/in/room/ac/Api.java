package in.room.ac;
import android.content.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
final class Api {
 static final String AUTH="https://auth.miraie.in/simplifi/v1/",BASE="https://app.miraie.in/simplifi/v1/",CLIENT="292d99d7-5623-4157-8a71-846ee3ff5dbf";
 static class HttpError extends Exception{int status;HttpError(int code){super("Panasonic returned HTTP "+code);status=code;}}
 static Object request(String url,String method,JSONObject body,String token)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Content-Type","application/json");if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
  try{if(body!=null){c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}}
   int code=c.getResponseCode();if(code<200||code>=300)throw new HttpError(code);
   if(code==204)return new JSONObject();ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>4*1024*1024)throw new IOException("Response too large");b.write(buf,0,n);}}
   String text=b.toString("UTF-8");return text.trim().isEmpty()?new JSONObject():new JSONTokener(text).nextValue();
  }finally{c.disconnect();}
 }
 static void login(Context c,String identity,String password)throws Exception{
  JSONObject body=new JSONObject().put(identity.contains("@")?"email":"mobile",identity).put("password",password).put("clientId",CLIENT).put("scope","an_"+(System.nanoTime()%1000000000L));
  Object response=request(AUTH+"userManagement/login","POST",body,null);
  if(!(response instanceof JSONObject))throw new IOException("Unexpected login response");JSONObject s=(JSONObject)response;
  if(s.optString("accessToken").isEmpty())throw new IOException("Login response did not include an access token");
  JSONObject saved=new JSONObject().put("accessToken",s.getString("accessToken")).put("refreshToken",s.optString("refreshToken")).put("userId",s.optString("userId"));Vault.save(c,saved);
 }
 static Object authorized(Context c,String path,String method,JSONObject body)throws Exception{
  JSONObject session=Vault.load(c);String token=session.optString("accessToken");if(token.isEmpty())throw new IOException("Open Room AC and sign in first");
  try{return request(BASE+path,method,body,token);}catch(HttpError e){if(e.status!=401&&e.status!=406)throw e;}
  String refresh=session.optString("refreshToken");if(refresh.isEmpty())throw new IOException("Session expired. Please sign in again");
  Object refreshed=request(AUTH+"userManagement/tokenRefresh","POST",new JSONObject().put("clientId",CLIENT).put("token",refresh),null);
  if(!(refreshed instanceof JSONObject))throw new IOException("Unexpected token refresh response");JSONObject s=(JSONObject)refreshed;
  if(s.optString("accessToken").isEmpty())throw new IOException("Session expired. Please sign in again");
  session.put("accessToken",s.getString("accessToken"));if(!s.optString("refreshToken").isEmpty())session.put("refreshToken",s.getString("refreshToken"));Vault.save(c,session);
  return request(BASE+path,method,body,session.getString("accessToken"));
 }
 static JSONArray devices(Context c)throws Exception{
  Object data=authorized(c,"homeManagement/homes","GET",null);JSONArray homes=data instanceof JSONArray?(JSONArray)data:new JSONArray().put(data);JSONArray result=new JSONArray();
  for(int h=0;h<homes.length();h++){JSONObject home=homes.optJSONObject(h);if(home==null)continue;JSONArray spaces=home.optJSONArray("spaces");if(spaces==null)continue;
   for(int s=0;s<spaces.length();s++){JSONObject space=spaces.optJSONObject(s);if(space==null)continue;JSONArray ds=space.optJSONArray("devices");if(ds==null)continue;
    for(int i=0;i<ds.length();i++){JSONObject d=ds.optJSONObject(i);if(d==null)continue;String cat=d.optString("category");if("AC".equalsIgnoreCase(cat)||"ODMIRB-AC".equalsIgnoreCase(cat)){d.put("homeId",home.optString("homeId"));d.put("spaceName",space.optString("spaceName"));result.put(d);}}
   }
  }return result;
 }
 static JSONObject selected(Context c)throws Exception{return Vault.load(c).optJSONObject("device");}
 static JSONObject status(Context c)throws Exception{JSONObject d=selected(c);if(d==null)throw new IOException("Choose an AC in Room AC first");String id=d.getString("deviceId");Object data=authorized(c,"deviceManagement/devices/"+URLEncoder.encode(id,"UTF-8")+"/mobile/status","GET",null);if(!(data instanceof JSONObject))throw new IOException("Unexpected device status response");return (JSONObject)data;}
 static void control(Context c,JSONObject changes)throws Exception{JSONObject d=selected(c);if(d==null)throw new IOException("Choose an AC first");JSONObject params=new JSONObject().put("cnt","an").put("sid","0");if(changes.length()==1&&!changes.has("ki")){DeviceProfile p=new DeviceProfile(cachedProfile(c));String[] names={"ACMode","PowerStatus","ACTemperature","PowerfulMode","ACFanSpeed","ECOMode","Convertible","ACModeTemperature"};int[] indexes={0,1,2,3,4,10,28,257};for(int i=0;i<names.length;i++){JSONObject cap=p.capability(names[i]);if(cap!=null&&changes.has(p.key(names[i],names[i])))params.put("ki",indexes[i]);}}Iterator<String> keys=changes.keys();while(keys.hasNext()){String k=keys.next();params.put(k,changes.get(k));}authorized(c,"deviceManagement/control","POST",new JSONObject().put("deviceId",d.getString("deviceId")).put("control_params",params));}
 static JSONObject fields(JSONObject state){JSONObject s=state;for(String key:new String[]{"status","data","params","state"}){JSONObject nested=s.optJSONObject(key);if(nested!=null)s=nested;}
  return s;}
 static String summary(JSONObject state){JSONObject s=fields(state);String power=s.optString("ps","unknown"),mode=s.optString("acmd",""),temp=s.optString("actmp","");return power+(mode.isEmpty()?"":" · "+mode)+(temp.isEmpty()?"":" · "+temp+"°C");}
 static String powerKey(Context c){return new DeviceProfile(cachedProfile(c)).key("PowerStatus","ps");}
 static JSONObject cachedProfile(Context c){try{JSONObject session=Vault.load(c),d=session.optJSONObject("device");if(d!=null&&d.optString("deviceId").equals(session.optString("modelDeviceId")))return session.optJSONObject("modelProfile");}catch(Exception ignored){}return new JSONObject();}
 static void cacheStatus(Context c,JSONObject state)throws Exception{
  JSONObject f=fields(state);DeviceProfile profile=new DeviceProfile(cachedProfile(c));String mode=f.optString(profile.key("ACMode","acmd")),power=f.optString(profile.key("PowerStatus","ps"),"unknown"),target=f.optString(profile.key("ACModeTemperature","actmp"));
  String value=power+(mode.isEmpty()?"":" · "+DeviceProfile.label(mode));if(!mode.equals("fan")&&!target.isEmpty())value+=" · "+target+"°C";
  android.content.SharedPreferences.Editor editor=c.getSharedPreferences("view",0).edit().putString("status",value+"\nRead at "+android.text.format.DateFormat.format("HH:mm:ss",System.currentTimeMillis())).putString("fields",redact(f).toString()).putLong("read_at",System.currentTimeMillis());
  try{double temp=Double.parseDouble(target);if(Double.isFinite(temp))editor.putFloat("target_value",(float)temp);}catch(Exception ignored){}editor.commit();
  try{MetricStore.record(c,state);c.getSharedPreferences("view",0).edit().remove("recording_error").commit();}catch(Exception e){c.getSharedPreferences("view",0).edit().putString("recording_error","Could not save this reading locally.").commit();}
  AcWidget.updateAll(c);
 }
 static JSONObject cachedFields(Context c){try{return new JSONObject(c.getSharedPreferences("view",0).getString("fields","{}"));}catch(Exception e){return new JSONObject();}}
 static JSONObject profile(Context c,boolean force)throws Exception{
  JSONObject device=selected(c);if(device==null)throw new IOException("Choose an AC first");JSONObject session=Vault.load(c);
  if(!force && device.optString("deviceId").equals(session.optString("modelDeviceId")) && session.optJSONObject("modelProfile")!=null && System.currentTimeMillis()-session.optLong("modelFetchedAt",0)<86400000L)return session.getJSONObject("modelProfile");
  String modelId=device.optString("modelId");if(modelId.isEmpty())throw new IOException("The AC did not report a model identifier");
  Object response=authorized(c,"deviceManagement/deviceModels/modelId/"+URLEncoder.encode(modelId,"UTF-8"),"GET",null);
  if(!(response instanceof JSONObject))throw new IOException("Unexpected model capability response");JSONObject model=(JSONObject)response;
  if(!model.has("controlCapabilities") && model.optJSONObject("data")!=null)model=model.getJSONObject("data");
  session=Vault.load(c);session.put("modelDeviceId",device.optString("deviceId")).put("modelProfile",model).put("modelFetchedAt",System.currentTimeMillis());Vault.save(c,session);return model;
 }
 static JSONObject inspect(Context c)throws Exception{
  JSONObject old=selected(c);if(old==null)throw new IOException("Choose an AC first");JSONArray list=devices(c);JSONObject device=old;
  for(int i=0;i<list.length();i++){JSONObject candidate=list.getJSONObject(i);if(old.optString("deviceId").equals(candidate.optString("deviceId"))){device=candidate;break;}}
  JSONObject session=Vault.load(c);session.put("device",device).put("devices",list);Vault.save(c,session);
  JSONObject metadata=new JSONObject();for(String k:new String[]{"category","modelId","model","capabilities","capability","deviceType","deviceModelInfo","features","modelInfo","supportedFeatures"})if(device.has(k))metadata.put(k,device.get(k));
  JSONObject report=new JSONObject().put("observedAt",java.time.Instant.now().toString()).put("device_keys",device.names()).put("metadata",redact(metadata)).put("model",redact(profile(c,true))).put("status",redact(status(c)));
  try{report.put("energy_history",redact(energyHistory(c)));}catch(Exception e){report.put("energy_history_error",safeError(e));}return report;
 }
 static JSONObject energyHistory(Context c)throws Exception{
  JSONObject d=selected(c);if(d==null)throw new IOException("Choose an AC first");java.time.LocalDate to=java.time.LocalDate.now(),from=to.minusDays(6);java.time.format.DateTimeFormatter format=java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy",java.util.Locale.UK);
  String path="powerConsumption/devices/"+URLEncoder.encode(d.getString("deviceId"),"UTF-8")+"?grain=Daily&startDate="+from.format(format)+"&endDate="+to.format(format);
  Object data=authorized(c,path,"GET",null);return new JSONObject().put("grain","Daily").put("start_date",from.toString()).put("end_date",to.toString()).put("unit","kWh").put("data",data);
 }
 static Object redact(Object value)throws Exception{
  if(value instanceof JSONObject){JSONObject src=(JSONObject)value,out=new JSONObject();Iterator<String> keys=src.keys();while(keys.hasNext()){String k=keys.next();String l=k.toLowerCase(java.util.Locale.ROOT);if(l.contains("token")||l.contains("password")||l.contains("secret")||l.contains("topic")||l.contains("mac")||l.contains("email")||l.contains("mobile")||l.contains("username")||l.contains("ssid")||l.contains("address")||l.equals("dealer")||l.equals("homeid")||l.equals("spaceid")||l.equals("userid")||l.equals("deviceid")||l.equals("serialnumber"))continue;out.put(k,redact(src.get(k)));}return out;}
  if(value instanceof JSONArray){JSONArray src=(JSONArray)value,out=new JSONArray();for(int i=0;i<src.length();i++)out.put(redact(src.get(i)));return out;}return value;
 }
 static String safeError(Exception e){if(e instanceof HttpError)return e.getMessage();if(e instanceof java.net.UnknownHostException)return "Cannot reach Panasonic. Check internet access.";if(e instanceof java.net.SocketTimeoutException)return "Panasonic request timed out. Tap Refresh to check the actual state before retrying a command.";if(e instanceof javax.net.ssl.SSLException)return "Secure connection to Panasonic failed.";if(e instanceof org.json.JSONException)return "Panasonic returned an unexpected response format.";return e.getMessage()==null?"Request failed":e.getMessage();}
}
