package in.openmiraie.android;
import org.json.*;
import java.util.*;

/** No retries of mutations. Each dependent step waits for a matching reported value. */
final class CommandRunner {
    interface Port { void send(Profile.Command c)throws Exception; JSONObject read()throws Exception; void observed(JSONObject state)throws Exception; }
    interface Wait { void sleep(long ms)throws Exception; }
    interface Progress { void update(String message); }
    record Result(boolean confirmed,int completed,int total,JSONObject observed,String message,boolean accepted) { boolean physicalConfirmed(){return false;} }
    final Port port; final Wait wait; final Progress progress;
    CommandRunner(Port port,Wait wait,Progress progress){this.port=port;this.wait=wait;this.progress=progress;}
    static List<Map.Entry<String,Object>> plan(Profile p,JSONObject s,String name,Object value)throws Exception {
        // Validate the final operation before causing any coupled effects.
        p.command(name,value,s);
        List<Map.Entry<String,Object>> steps=new ArrayList<>();
        if(name.equals("ECOMode")&&value.equals("on")) {
            p.command("ACTemperature","26",s);
            steps.add(new java.util.AbstractMap.SimpleImmutableEntry<>(name,value));steps.add(new java.util.AbstractMap.SimpleImmutableEntry<>("ACTemperature","26"));return steps;
        }
        if(name.equals("PowerfulMode")&&value.equals("on")&&p.supports("Convertible")) {
            String capacity=p.value(s,"Convertible");
            if(capacity.isEmpty())throw new IllegalArgumentException("Refresh capacity before enabling Powerful.");
            if(!Data.same(capacity,"0")) {p.command("Convertible",0,s);steps.add(new java.util.AbstractMap.SimpleImmutableEntry<>("Convertible",0));}
        }
        if(name.equals("Convertible")&&!Data.same(value,"0")&&p.supports("PowerfulMode")) {
            String powerful=p.value(s,"PowerfulMode");
            if(!java.util.Arrays.asList("on","off").contains(powerful))throw new IllegalArgumentException("Refresh Powerful before setting capacity.");
            if(powerful.equals("on")){p.command("PowerfulMode","off",s);steps.add(new java.util.AbstractMap.SimpleImmutableEntry<>("PowerfulMode","off"));}
        }
        steps.add(new java.util.AbstractMap.SimpleImmutableEntry<>(name,value));return steps;
    }
    Result execute(Profile profile,JSONObject initial,String name,Object value)throws Exception {
        List<Map.Entry<String,Object>> steps=plan(profile,initial,name,value);
        JSONObject observed=initial;int completed=0;boolean accepted=false;
        for(Map.Entry<String,Object> step:steps) {
            Profile.Command command;
            try {command=profile.command(step.getKey(),step.getValue(),observed);}
            catch(IllegalArgumentException e){return new Result(false,completed,steps.size(),observed,"Stopped after "+completed+" of "+steps.size()+" confirmed steps. "+e.getMessage(),accepted);}
            progress.update(Swing.axis(step.getKey())?"Sending "+Swing.title(step.getKey()).toLowerCase(java.util.Locale.ROOT)+" code "+step.getValue()+"…":"Sending "+step.getKey()+" → "+step.getValue()+"…");
            try {port.send(command);accepted=true;} catch(Exception e) {return new Result(false,completed,steps.size(),observed,"Request failed; delivery may have occurred. Refresh to check. Confirmed "+completed+" of "+steps.size()+" steps. "+Api.safe(e),accepted);}
            boolean matched=false;
            progress.update(Swing.axis(step.getKey())?"Swing request accepted. Waiting for cloud readback; physical movement is not confirmed.":"Checking reported state…");
            for(int attempt=1;attempt<=5;attempt++) {
                wait.sleep(Swing.axis(step.getKey())&&attempt==1?3000L:attempt*600L);
                try {observed=port.read();port.observed(observed);}catch(Exception e){return new Result(false,completed,steps.size(),observed,"Readback failed. Refresh to check. Confirmed "+completed+" of "+steps.size()+" steps. "+Api.safe(e),accepted);}
                if(Swing.axis(step.getKey())&&!Swing.blocked(profile,observed,step.getKey()).isEmpty())return new Result(false,completed,steps.size(),observed,"Swing readback cannot confirm safely. "+Swing.blocked(profile,observed,step.getKey())+" Refresh to check.",accepted);
                if(Data.same(Data.value(observed,command.key()),command.value())) {matched=true;break;}
                if(!profile.online(observed))break;
            }
            if(!matched)return new Result(false,completed,steps.size(),observed,"Change sent; AC hasn’t reported it yet. Refresh to check. Confirmed "+completed+" of "+steps.size()+" steps.",accepted);
            completed++;
        }
        return new Result(true,completed,steps.size(),observed,Swing.axis(name)?"AC reports "+Swing.title(name).toLowerCase(java.util.Locale.ROOT)+" code "+value+". Physical movement is not confirmed.":"AC reports the updated setting.",accepted);
    }
}
