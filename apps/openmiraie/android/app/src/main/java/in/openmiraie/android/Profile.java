package in.openmiraie.android;

import org.json.*;
import java.util.*;

/** Reviewed AC capability interpretation. Unknown sections/attributes remain in raw metadata. */
final class Profile {
    final JSONObject raw;
    final JSONObject device;
    static final Map<String,Integer> INDEX = new LinkedHashMap<>();
    static { INDEX.put("ACMode",0); INDEX.put("PowerStatus",1); INDEX.put("ACTemperature",2); INDEX.put("PowerfulMode",3); INDEX.put("ACFanSpeed",4); INDEX.put("VerticalSwing",5); INDEX.put("HorizontalSwing",6); INDEX.put("DisplayControl",8); INDEX.put("ECOMode",10); INDEX.put("Convertible",28); }
    Profile(JSONObject raw) { this(raw,null); }
    Profile(JSONObject raw,JSONObject device) { this.raw = raw == null ? new JSONObject() : raw;this.device=device==null?new JSONObject():device; }
    JSONObject attribute(String name) {
        JSONArray caps = raw.optJSONArray("controlCapabilities");
        if(caps == null) return null;
        for(int i=0;i<caps.length();i++) {
            JSONObject cap=caps.optJSONObject(i);
            if(cap!=null && name.equals(cap.optString("capability"))) {
                JSONArray a=cap.optJSONArray("attributes");
                // Multiple attributes require a separately reviewed builder (e.g. sleep profiles).
                return a!=null && a.length()==1 ? a.optJSONObject(0) : null;
            }
        }
        return null;
    }
    String key(String name) { JSONObject a=attribute(name); return a==null?"":a.optString("key"); }
    boolean supports(String name) { return Swing.axis(name)?!Swing.positions(this,name).isEmpty():INDEX.containsKey(name) && !key(name).isEmpty(); }
    List<String> choices(String name) {
        if(Swing.axis(name)){List<String> codes=new ArrayList<>();for(int code:Swing.positions(this,name))codes.add(Integer.toString(code));return codes;}
        JSONObject a=attribute(name); List<String> out=new ArrayList<>();
        if(a==null)return out;
        JSONArray values=a.optJSONArray(name.equals("Convertible")?"intValues":"values");
        if(values!=null)for(int i=0;i<values.length();i++) {
            String v=String.valueOf(values.opt(i));
            if(name.equals("Convertible")&&!java.util.Arrays.asList("0","40","50","60","70","80","90","100","110").contains(v))continue;
            if(name.equals("ACMode")&&!java.util.Arrays.asList("auto","cool","dry","fan").contains(v.toLowerCase(Locale.ROOT)))continue;
            if(!out.contains(v))out.add(v);
        }
        return out;
    }
    double[] range(String mode) {
        JSONObject a=attribute("ACTemperature"); Object ranges=a==null?null:a.opt("range");
        JSONArray list=ranges instanceof JSONArray?(JSONArray)ranges:new JSONArray().put(ranges);
        for(int i=0;i<list.length();i++) {
            JSONObject r=list.optJSONObject(i); if(r==null)continue;
            if(!r.optString("mode").isEmpty()&&!r.optString("mode").equalsIgnoreCase(mode))continue;
            double min=celsius(r.optJSONArray("min")),max=celsius(r.optJSONArray("max")),step=celsius(r.optJSONArray("increment"));
            if(Double.isFinite(min)&&Double.isFinite(max)&&Double.isFinite(step)&&min<=max&&step>0)return new double[]{min,max,step};
        }
        return null;
    }
    private double celsius(JSONArray a) {
        if(a!=null)for(int i=0;i<a.length();i++) {
            JSONObject v=a.optJSONObject(i); if(v==null)continue;
            if(java.util.Arrays.asList("celsius","celcius","c","°c").contains(v.optString("unit").toLowerCase(Locale.ROOT)))return v.optDouble("value",Double.NaN);
        }
        return Double.NaN;
    }
    String value(JSONObject fields,String name) { return Data.value(fields,key(name)); }
    boolean on(JSONObject fields,String name) { return "on".equalsIgnoreCase(value(fields,name)); }
    boolean online(JSONObject fields) { return java.util.Arrays.asList("true","online","on","1").contains(Data.value(fields,"onlineStatus").toLowerCase(Locale.ROOT)); }
    boolean cleaning(JSONObject f) { return "on".equalsIgnoreCase(Data.value(f,key("EvaporatorClean").isEmpty()?"acec":key("EvaporatorClean"))); }
    String blocked(JSONObject f,String name) {
        if(Swing.axis(name))return Swing.blocked(this,f,name);
        if(!supports(name))return "Capability unavailable. Refresh capabilities.";
        if(!online(f))return "AC offline or connection state not reported. Refresh before changing settings.";
        String current=value(f,name);
        if(current.isEmpty())return "Current setting not reported. Refresh first.";
        if(name.equals("PowerStatus")||name.equals("DisplayControl"))return java.util.Arrays.asList("on","off").contains(current.toLowerCase(Locale.ROOT))?"":"Current switch state is unknown.";
        if(!on(f,"PowerStatus"))return "Power on the AC to change this setting.";
        String mode=value(f,"ACMode").toLowerCase(Locale.ROOT);
        if(!name.equals("ACMode")&&!choices("ACMode").contains(mode))return "Applied mode is not reported or supported. Refresh first.";
        if(name.equals("ACTemperature")&&(mode.equals("fan")||cleaning(f)||range(mode)==null))return "Temperature unavailable in this mode or during cleaning.";
        if(name.equals("ACFanSpeed")&&(mode.equals("dry")||cleaning(f)))return "Dry mode or cleaning manages the fan automatically.";
        if((name.equals("ECOMode")||name.equals("PowerfulMode"))&&!mode.equals("cool"))return "Available in Cool mode only.";
        if(name.equals("PowerfulMode")&&cleaning(f))return "Powerful is unavailable during cleaning.";
        if(name.equals("Convertible")&&"on".equalsIgnoreCase(Data.value(f,"acjs")))return "Active JetStream needs a separately verified cancellation flow. Use MirAIe.";
        return "";
    }
    Command command(String name,Object value,JSONObject state)throws Exception {
        String why=blocked(state,name); if(!why.isEmpty())throw new IllegalArgumentException(why);
        String v=String.valueOf(value);
        if(Swing.axis(name)){
            if(!(value instanceof Integer)||!Swing.positions(this,name).contains(value))throw new IllegalArgumentException("Choose an advertised integer swing code.");
        } else if(name.equals("ACTemperature")) {
            double n=Double.parseDouble(v);double[] r=range(value(state,"ACMode"));
            if(!Double.isFinite(n)||r==null||n<r[0]||n>r[1]||Math.abs((n-r[0])/r[2]-Math.rint((n-r[0])/r[2]))>0.00001)throw new IllegalArgumentException("Target is outside the advertised range.");
        } else if(java.util.Arrays.asList("ACMode","ACFanSpeed","Convertible").contains(name)) {
            if(!choices(name).contains(v))throw new IllegalArgumentException("Setting not advertised by this model.");
        } else if(!java.util.Arrays.asList("on","off").contains(v))throw new IllegalArgumentException("Invalid switch setting.");
        return new Command(name,key(name),INDEX.get(name),name.equals("Convertible")||Swing.axis(name)?Integer.valueOf(v):v);
    }
    record Command(String capability,String key,int index,Object value) {
        JSONObject params()throws JSONException {return new JSONObject().put("cnt","an").put("sid","0").put("ki",index).put(key,value);}
    }
}
