package in.openmiraie.android;

import org.json.*;
import java.math.BigDecimal;
import java.util.*;

/** Model-scoped range adapter and presentation state. No networking or Android dependencies. */
final class Swing {
    static final String VERTICAL="VerticalSwing",HORIZONTAL="HorizontalSwing";
    static boolean axis(String cap){return VERTICAL.equals(cap)||HORIZONTAL.equals(cap);}
    static String title(String cap){return VERTICAL.equals(cap)?"Vertical swing":"Horizontal swing";}
    private static boolean absent(JSONObject source,String key){Object value=source.opt(key);return value==null||value==JSONObject.NULL||"".equals(value);}
    static boolean modelSupported(Profile profile){JSONObject device=profile.device,model=profile.raw;return "140013".equals(device.optString("modelId"))&&"AC".equals(device.optString("category"))&&"140013".equals(model.optString("modelId",device.optString("modelId")))&&(absent(device,"thirdPartyService")||Boolean.FALSE.equals(device.opt("thirdPartyService")))&&absent(device,"parentDeviceId")&&(absent(model,"thirdPartyService")||Boolean.FALSE.equals(model.opt("thirdPartyService")))&&absent(model,"parentDeviceId");}
    static boolean advertised(Profile profile,String cap){return profile.attribute(cap)!=null;}
    private static JSONObject attribute(Profile profile,String cap){
        if(!axis(cap)||!modelSupported(profile))return null;
        JSONArray capabilities=profile.raw.optJSONArray("controlCapabilities");if(capabilities==null)return null;
        JSONObject attr=null;int matches=0;
        for(int i=0;i<capabilities.length();i++){JSONObject c=capabilities.optJSONObject(i);if(c==null)continue;if(cap.equals(c.optString("capability"))){matches++;JSONArray attrs=c.optJSONArray("attributes");if(attrs==null||attrs.length()!=1)return null;attr=attrs.optJSONObject(0);}}
        if(matches!=1||attr==null||!(attr.opt("key") instanceof String))return null;
        String key=attr.optString("key");if(!key.matches("[A-Za-z_][A-Za-z0-9_]*")||Arrays.asList("cnt","sid","ki","deviceId").contains(key))return null;
        for(int i=0;i<capabilities.length();i++){JSONObject c=capabilities.optJSONObject(i);if(c==null||cap.equals(c.optString("capability")))continue;JSONArray attrs=c.optJSONArray("attributes");if(attrs!=null)for(int j=0;j<attrs.length();j++){JSONObject other=attrs.optJSONObject(j);if(other!=null&&key.equals(other.optString("key")))return null;}}
        return attr;
    }
    private static Integer integer(Object value){if(!(value instanceof Number))return null;try{return new BigDecimal(value.toString()).intValueExact();}catch(Exception e){return null;}}
    private static Integer bound(JSONObject range,String name){JSONArray entries=range.optJSONArray(name);if(entries==null||entries.length()!=1)return null;JSONObject entry=entries.optJSONObject(0);if(entry==null)return null;Object unit=entry.opt("unit");if(unit!=null&&unit!=JSONObject.NULL&&!"".equals(unit))return null;return integer(entry.opt("value"));}
    static List<Integer> positions(Profile profile,String cap){
        JSONObject attr=attribute(profile,cap);if(attr==null)return Collections.emptyList();List<Integer> positions=null;
        if(attr.has("range")){Object raw=attr.opt("range");if(raw instanceof JSONArray ranges){if(ranges.length()!=1)return Collections.emptyList();raw=ranges.opt(0);}if(!(raw instanceof JSONObject range)||range.has("mode"))return Collections.emptyList();Integer min=bound(range,"min"),max=bound(range,"max"),step=range.has("increment")?bound(range,"increment"):Integer.valueOf(1);if(min==null||max==null||step==null||min<0||max>5||min>max||step<1)return Collections.emptyList();positions=new ArrayList<>();for(int code=min;code<=max;code++)if((code-min)%step==0)positions.add(code);}
        if(attr.has("intValues")){JSONArray values=attr.optJSONArray("intValues");if(values==null||values.length()==0||values.length()>6)return Collections.emptyList();List<Integer> explicit=new ArrayList<>();for(int i=0;i<values.length();i++){Integer code=integer(values.opt(i));if(code==null||code<0||code>5||explicit.contains(code)||(positions!=null&&!positions.contains(code)))return Collections.emptyList();explicit.add(code);}positions=explicit;}
        if(positions==null||positions.isEmpty())return Collections.emptyList();Collections.sort(positions);return Collections.unmodifiableList(new ArrayList<>(positions));
    }
    static Integer reportedCode(Profile profile,JSONObject state,String cap){Object value=state.opt(profile.key(cap));if(value==null||value==JSONObject.NULL||value instanceof Boolean)return null;try{int code=new BigDecimal(value.toString()).intValueExact();return positions(profile,cap).contains(code)?code:null;}catch(Exception e){return null;}}
    static String blocked(Profile profile,JSONObject state,String cap){
        if(!modelSupported(profile))return "Swing is verified only for direct AC model 140013.";
        if(positions(profile,cap).isEmpty())return "Swing capability or integer range is unavailable. Refresh capabilities.";
        if(!profile.online(state))return "AC offline or connection state not reported. Refresh before changing swing.";
        if(!profile.on(state,"PowerStatus"))return "Power on the AC to change swing.";
        String cleaningKey=profile.key("EvaporatorClean");if(cleaningKey.isEmpty())cleaningKey="acec";
        if(!"off".equalsIgnoreCase(Data.value(state,cleaningKey)))return "Swing requires cleaning to be reported off. Refresh first.";
        if(reportedCode(profile,state,cap)==null)return "Current swing code is not reported or supported. Refresh first.";
        return "";
    }
    static String description(String cap,int code){
        if(code==0)return "Oscillate";
        if(VERTICAL.equals(cap))return switch(code){case 1->"Position 1 · angle unverified";case 2->"Lower than 1";case 3->"Lower than 2";case 4->"Lower than 3";case 5->"Lowest tested · fixed";default->"Unverified position";};
        return switch(code){case 1->"Appeared centered";case 2->"Leftmost observed";case 3->"Inner left · left of 1";case 4->"Moved right from 3";case 5->"Rightmost tested · fixed";default->"Unverified position";};
    }
    static String label(String cap,int code){return "Code "+code+" · "+description(cap,code);}
    static String guidance(String cap){return (VERTICAL.equals(cap)?"On the tested unit, 1→5 moved progressively down. Position 1’s exact angle and fixedness were not established.":"Left/right is while facing the AC. The owner confirmed the left-to-right order 2, 3, 1, 4, 5: 2 is far left, 3 inner left, and 1 appeared centered. Fixedness at 1/2 remains unverified.")+" Owner observations: one model 140013 unit, 19 Oct 2026. Firmware and calibrated angles are unknown. These historical observations do not confirm a new physical movement.";}
    record UiState(String observedLabel,Integer observedCode,List<Integer> choices,Integer draft,boolean pending,boolean canApply,String reason) {}
    static UiState uiState(Profile profile,JSONObject state,String cap,Integer draft,boolean pending,boolean modeDraft){Integer observed=reportedCode(profile,state,cap);String reason=pending?"A request is in progress.":modeDraft?"Apply or cancel the selected mode first.":blocked(profile,state,cap);List<Integer> choices=positions(profile,cap);boolean apply=reason.isEmpty()&&draft!=null&&choices.contains(draft)&&!draft.equals(observed);return new UiState(observed==null?"Not reported / unsupported":label(cap,observed),observed,choices,draft,pending,apply,reason);}
}
