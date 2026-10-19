package in.openmiraie.android;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class Api {
    static final String AUTH="https://auth.miraie.in/simplifi/v1/",BASE="https://app.miraie.in/simplifi/v1/",CLIENT="292d99d7-5623-4157-8a71-846ee3ff5dbf";
    static final class Expired extends IOException {Expired(){super("Session expired. Sign in again. No command will be replayed.");}}
    static final class Http extends IOException {final int code;Http(int c){super("Service returned HTTP "+c+". Refresh before retrying.");code=c;}}
    final SessionStore store;
    Api(SessionStore s){store=s;}
    Object request(String url,String method,JSONObject body,String token)throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(12000);connection.setReadTimeout(15000);
        connection.setRequestMethod(method);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Content-Type","application/json");
        if(token!=null)connection.setRequestProperty("Authorization","Bearer "+token);
        try {
            if(body!=null){connection.setDoOutput(true);try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=connection.getResponseCode();if(code<200||code>=300)throw new Http(code);
            if(code==204)return new JSONObject();
            ByteArrayOutputStream result=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(result.size()+n>4*1024*1024)throw new IOException("Response exceeded size limit.");result.write(buffer,0,n);}}
            String text=result.toString(StandardCharsets.UTF_8.name());return text.trim().isEmpty()?new JSONObject():new JSONTokener(text).nextValue();
        }finally{connection.disconnect();}
    }
    void login(String identity,String password)throws Exception {
        JSONObject body=new JSONObject().put(identity.contains("@")?"email":"mobile",identity).put("password",password).put("clientId",CLIENT).put("scope","an_"+UUID.randomUUID());
        JSONObject response=(JSONObject)request(AUTH+"userManagement/login","POST",body,null);
        if(response.optString("accessToken").isEmpty())throw new IOException("Sign-in response was incomplete.");
        JSONObject saved=new JSONObject();for(String field:java.util.Arrays.asList("accessToken","refreshToken","userId","expiresIn"))if(response.has(field))saved.put(field,response.get(field));store.save(saved);
    }
    synchronized Object authorized(String path,String method,JSONObject body)throws Exception {
        JSONObject session=store.load();if(session.optString("accessToken").isEmpty())throw new Expired();
        try{return request(BASE+path,method,body,session.getString("accessToken"));}
        catch(Http failure){
            if(failure.code!=401&&failure.code!=406)throw failure;
            // Refresh reads once. Mutation authorization failures require explicit sign-in; never replay.
            if(!method.equals("GET")){store.clear();throw new Expired();}
            try {
                String token=session.optString("refreshToken");if(token.isEmpty())throw new Expired();
                JSONObject refreshed=(JSONObject)request(AUTH+"userManagement/tokenRefresh","POST",new JSONObject().put("clientId",CLIENT).put("token",token),null);
                if(refreshed.optString("accessToken").isEmpty())throw new Expired();
                session.put("accessToken",refreshed.getString("accessToken"));if(!refreshed.optString("refreshToken").isEmpty())session.put("refreshToken",refreshed.getString("refreshToken"));store.save(session);
                return request(BASE+path,method,null,session.getString("accessToken"));
            }catch(Http e){if(e.code!=401&&e.code!=406)throw e;store.clear();throw new Expired();}
            catch(Expired e){store.clear();throw e;}
        }
    }
    static String encode(String id){try{return URLEncoder.encode(id,"UTF-8").replace("+","%20");}catch(Exception e){throw new IllegalArgumentException(e);}}
    List<JSONObject> discover()throws Exception{return Data.discovery(authorized("homeManagement/homes","GET",null));}
    JSONObject model(JSONObject device)throws Exception {
        String id=device.optString("modelId");if(id.isEmpty())throw new IOException("Model identifier missing; controls unavailable.");
        JSONObject result=(JSONObject)authorized("deviceManagement/deviceModels/modelId/"+encode(id),"GET",null);
        if(!result.has("controlCapabilities")&&result.optJSONObject("data")!=null)return result.getJSONObject("data");return result;
    }
    JSONObject status(JSONObject device)throws Exception {
        JSONObject raw=(JSONObject)authorized("deviceManagement/devices/"+encode(device.getString("deviceId"))+"/mobile/status","GET",null);
        // Preserve the returned object; only unwrap one documented response envelope.
        if(raw.length()==1&&raw.optJSONObject("status")!=null)return raw.getJSONObject("status");
        if(raw.length()==1&&raw.optJSONObject("data")!=null)return raw.getJSONObject("data");
        return raw;
    }
    void send(JSONObject device,Profile.Command command)throws Exception {authorized("deviceManagement/control","POST",new JSONObject().put("deviceId",device.getString("deviceId")).put("control_params",command.params()));}
    JSONArray energy(JSONObject device)throws Exception {
        LocalDate end=LocalDate.now(),start=end.minusDays(6);DateTimeFormatter f=DateTimeFormatter.ofPattern("ddMMyyyy",Locale.ROOT);
        return Data.energy(authorized("powerConsumption/devices/"+encode(device.getString("deviceId"))+"?grain=Daily&startDate="+start.format(f)+"&endDate="+end.format(f),"GET",null));
    }
    static String safe(Exception e){if(e instanceof Expired||e instanceof Http||e instanceof IllegalArgumentException)return e.getMessage();if(e instanceof UnknownHostException)return "No internet connection. Last-known values are shown.";if(e instanceof SocketTimeoutException)return "Request timed out. Refresh before retrying a change.";if(e instanceof javax.net.ssl.SSLException)return "Secure connection failed.";return "Could not complete the request. Check your connection and refresh.";}
}
