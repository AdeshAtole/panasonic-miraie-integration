package in.room.ac;
import org.json.*;
import java.util.*;
/** Interprets the model capability schema recovered from MirAIe; no guessed mode buttons. */
final class DeviceProfile {
 final JSONObject model;
 DeviceProfile(JSONObject model){this.model=model==null?new JSONObject():model;}
 JSONObject capability(String name){for(String section:new String[]{"controlCapabilities","statusCapabilitiesV2","configCapabilities"}){JSONArray list=model.optJSONArray(section);if(list==null)continue;for(int i=0;i<list.length();i++){JSONObject cap=list.optJSONObject(i);if(cap!=null&&name.equalsIgnoreCase(cap.optString("capability")))return cap;}}return null;}
 JSONObject attribute(String name){JSONObject cap=capability(name);if(cap==null)return null;JSONArray attrs=cap.optJSONArray("attributes");return attrs==null?null:attrs.optJSONObject(0);}
 String key(String name,String fallback){JSONObject a=attribute(name);return a==null?fallback:a.optString("key",fallback);}
 List<String> values(String name){JSONObject a=attribute(name);List<String> result=new ArrayList<>();JSONArray values=a==null?null:a.optJSONArray("values");if(values!=null)for(int i=0;i<values.length();i++){String v=values.optString(i,"");if(!v.isEmpty()&&!result.contains(v))result.add(v);}return result;}
 List<String> modes(){List<String> result=new ArrayList<>();for(String v:values("ACMode"))if(Arrays.asList("cool","dry","fan","auto","heat").contains(v.toLowerCase(Locale.ROOT))&&!result.contains(v.toLowerCase(Locale.ROOT)))result.add(v.toLowerCase(Locale.ROOT));return result;}
 List<String> fanSpeeds(){return values("ACFanSpeed");}
 List<Integer> intValues(String name){List<Integer> out=new ArrayList<>();JSONObject a=attribute(name);JSONArray values=a==null?null:a.optJSONArray("intValues");if(values!=null)for(int i=0;i<values.length();i++){Object raw=values.opt(i);try{int n=Integer.parseInt(String.valueOf(raw));if(!out.contains(n))out.add(n);}catch(Exception ignored){}}return out;}
 double bound(JSONArray entries,double fallback){if(entries==null)return fallback;for(int i=0;i<entries.length();i++){Object o=entries.opt(i);if(o instanceof Number)return ((Number)o).doubleValue();if(o instanceof JSONObject){JSONObject e=(JSONObject)o;String u=e.optString("unit");if(u.isEmpty()||(u.equalsIgnoreCase("Celsius")||u.equalsIgnoreCase("celcius"))||u.equalsIgnoreCase("c")||u.equals("°C"))return e.optDouble("value",fallback);}}return fallback;}
 double[] temperatureRange(String mode){JSONObject a=attribute("ACModeTemperature");if(a==null)a=attribute("Temperature");if(a==null)a=attribute("ACTemperature");Object ranges=a==null?null:a.opt("range");List<JSONObject> items=new ArrayList<>();if(ranges instanceof JSONArray){JSONArray r=(JSONArray)ranges;for(int i=0;i<r.length();i++)if(r.optJSONObject(i)!=null)items.add(r.optJSONObject(i));}else if(ranges instanceof JSONObject)items.add((JSONObject)ranges);
  for(JSONObject r:items){if(!r.optString("mode").isEmpty()&&!mode.equalsIgnoreCase(r.optString("mode")))continue;double min=bound(r.optJSONArray("min"),16),max=bound(r.optJSONArray("max"),30),step=bound(r.optJSONArray("increment"),1);if(Double.isFinite(min)&&Double.isFinite(max)&&Double.isFinite(step)&&min<=max&&step>0)return new double[]{min,max,step};}return new double[]{16,30,1};
 }
 static String label(String value){switch(value.toLowerCase(Locale.ROOT)){case "cool":return "Cool";case "dry":return "Dry";case "fan":return "Fan only";case "auto":return "Auto";case "heat":return "Heat";case "quiet":return "Quiet";case "low":return "Low";case "medium":return "Medium";case "high":return "High";case "on":return "On";case "off":return "Off";default:return value;}}
}
