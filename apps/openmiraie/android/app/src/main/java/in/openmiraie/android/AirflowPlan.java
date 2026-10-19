package in.openmiraie.android;
import java.util.*;
import org.json.JSONObject;
/** A gesture never stops an oscillating axis. Revalidate both codes before sending. */
final class AirflowPlan {
    static List<Map.Entry<String,Object>> plan(Profile p,JSONObject state,Integer vertical,Integer horizontal,Integer expectedV,Integer expectedH)throws Exception{
        Integer v=Swing.reportedCode(p,state,Swing.VERTICAL),h=Swing.reportedCode(p,state,Swing.HORIZONTAL);
        if(!Objects.equals(v,expectedV)||!Objects.equals(h,expectedH))throw new IllegalArgumentException("Airflow changed. Review its current position and try again.");
        List<Map.Entry<String,Object>> steps=new ArrayList<>();String[] axes={Swing.VERTICAL,Swing.HORIZONTAL};Integer[] old={v,h},targets={vertical,horizontal};
        for(int i=0;i<2;i++)if(targets[i]!=null&&old[i]!=null&&old[i]!=0&&!targets[i].equals(old[i])){if(targets[i]==0)throw new IllegalArgumentException("Use the swing button to start oscillating.");p.command(axes[i],targets[i],state);steps.add(new AbstractMap.SimpleImmutableEntry<>(axes[i],targets[i]));}
        return steps;
    }
}
