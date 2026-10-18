package in.room.ac;
import org.json.*;
import java.util.*;
final class Telemetry {
 static String value(JSONObject fields,String key){Object v=fields.opt(key);if(v==null||v==JSONObject.NULL)return null;return v.toString();}
 static String panel(DeviceProfile profile,JSONObject fields){
  StringBuilder out=new StringBuilder();String room=value(fields,profile.key("RoomTemperature","rmtmp"));
  if(room!=null){try{double n=Double.parseDouble(room);if(!Double.isFinite(n)||n==-1)room=null;}catch(Exception e){room=null;}}
  out.append("Room temperature: ").append(room==null?"Not reported":room+"°C");
  String fan=value(fields,profile.key("ACFanSpeed","acfs"));out.append("\nFan setting: ").append(fan==null?"Not reported":DeviceProfile.label(fan));
  out.append("\nMeasured fan RPM: Not available in these readings");
  String online=value(fields,"onlineStatus");if(online!=null)out.append("\nOnline: ").append(online);
  String rssi=value(fields,"rssi");if(rssi!=null)out.append("\nWi-Fi RSSI (reported): ").append(rssi);
  String errors=value(fields,"errors");if(errors!=null)out.append("\nErrors: ").append(errors.isEmpty()?"None reported":errors);
  String warnings=value(fields,"warnings");if(warnings!=null)out.append("\nWarnings: ").append(warnings.isEmpty()?"None reported":warnings);
  String[][] extra={{"totalOperatingHours","Operating hours"},{"filterDustLevel","Filter dust level (reported)"},{"filterCleaningRequired","Filter cleaning required"},{"filterCleaningCause","Filter cleaning reason"}};
  for(String[] row:extra){String v=value(fields,row[0]);if(v!=null)out.append("\n").append(row[1]).append(": ").append(v);}
  for(String capability:new String[]{"PM25Sensor","CurrentPowerConsumption","MotionSensorStatus","TimeBasedFilterHealth","Humidity"}){JSONObject cap=profile.capability(capability);if(cap==null)continue;String key=profile.key(capability,capability),v=value(fields,key);if(v!=null)out.append("\n").append(capability).append(" (reported): ").append(v);}
  return out.toString();
 }
 static String all(JSONObject fields){Map<String,String> values=new TreeMap<>();MetricStore.flatten(fields,"",values);StringBuilder out=new StringBuilder();for(Map.Entry<String,String> entry:values.entrySet())out.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');return out.length()==0?"No readings yet. Tap Refresh status.":out.toString();}
}
