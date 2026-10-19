package in.openmiraie.android;
import org.json.*;
import java.math.BigDecimal;
import java.util.*;
import java.time.*;
import java.time.format.*;

final class Data {
    static String value(JSONObject data,String key){Object v=data.opt(key);return v==null||v==JSONObject.NULL?"":v.toString();}
    static boolean same(Object a,Object b){if(a==null||b==null)return false;try{return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()))==0;}catch(NumberFormatException e){return a.toString().equalsIgnoreCase(b.toString());}}
    static String room(JSONObject s){String raw=value(s,"rmtmp");try{double n=Double.parseDouble(raw);return Double.isFinite(n)&&n!=-1?raw+"°C":"Not reported";}catch(Exception e){return "Not reported";}}
    static String shown(String s){return s.isEmpty()?"Not reported":s;}
    static String csv(String s){String probe=s.replaceFirst("^\\s+", "");if(!probe.isEmpty()&&"=+-@\t\r\n".indexOf(probe.charAt(0))>=0&&!probe.matches("[-+]?\\d+(\\.\\d+)?"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
    static String row(String... values){List<String> out=new ArrayList<>();for(String s:values)out.add(csv(s));return String.join(",",out)+"\r\n";}
    static Object redact(Object data)throws JSONException {
        if(data instanceof JSONObject src){JSONObject out=new JSONObject();Iterator<String> keys=src.keys();while(keys.hasNext()){String k=keys.next();String l=k.toLowerCase(Locale.ROOT);if(l.matches(".*(token|password|secret|topic|mac|email|mobile|username|ssid|address|serial|pair|udp|credential|key|iv).*" )||java.util.Arrays.asList("deviceid","homeid","spaceid","userid","dealer","parentdeviceid").contains(l))continue;out.put(k,redact(src.get(k)));}return out;}
        if(data instanceof JSONArray src){JSONArray out=new JSONArray();for(int i=0;i<src.length();i++)out.put(redact(src.get(i)));return out;}return data;
    }
    static List<JSONObject> discovery(Object response)throws JSONException {
        if(!(response instanceof JSONArray homes))throw new JSONException("Expected homes array");
        List<JSONObject> out=new ArrayList<>();
        for(int h=0;h<homes.length();h++){JSONObject home=homes.getJSONObject(h);JSONArray spaces=home.optJSONArray("spaces");if(spaces==null)continue;
            for(int s=0;s<spaces.length();s++){JSONObject space=spaces.getJSONObject(s);JSONArray devices=space.optJSONArray("devices");if(devices==null)continue;
                for(int d=0;d<devices.length();d++){JSONObject item=new JSONObject(devices.getJSONObject(d).toString());item.put("homeId",home.optString("homeId")).put("homeName",home.optString("homeName")).put("spaceName",space.optString("spaceName"));out.add(item);}}}
        return out;
    }
    static boolean supportedDevice(JSONObject d){return "AC".equalsIgnoreCase(d.optString("category"))&&d.optString("thirdPartyService").isEmpty()&&d.optString("parentDeviceId").isEmpty();}
    static String title(JSONObject d){if(d==null)return "Choose an AC";return d.optString("spaceName","").isEmpty()?d.optString("deviceName","Air conditioner"):d.optString("spaceName");}
    static JSONArray energy(Object data)throws JSONException {if(data instanceof JSONArray a)return a;if(data instanceof JSONObject o&&o.optJSONArray("data")!=null)return o.getJSONArray("data");throw new JSONException("Unexpected energy history");}
    static String date(String day){try{return LocalDate.parse(day,DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT)).toString();}catch(Exception e){return day;}}
    static String energyCsv(JSONArray rows){StringBuilder out=new StringBuilder(row("date","energy_kwh","source"));for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null)out.append(row(date(r.optString("day")),value(r,"power"),"Panasonic Daily history"));}return out.toString();}
    static String energySummary(JSONArray rows){BigDecimal total=BigDecimal.ZERO;StringBuilder out=new StringBuilder();for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r==null)continue;String day=date(r.optString("day")),power=value(r,"power");out.append(day).append(day.equals(LocalDate.now().toString())?" (today, partial)":"").append("  ·  ").append(shown(power)).append(" kWh\n");try{total=total.add(new BigDecimal(power));}catch(Exception ignored){}}return out+"\nReported-entry total: "+total.toPlainString()+" kWh\nMissing days are not zero-filled.";}
}
