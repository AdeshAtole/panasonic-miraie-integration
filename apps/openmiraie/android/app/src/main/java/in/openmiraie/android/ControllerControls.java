package in.openmiraie.android;

import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Approved inline controller controls, built from platform Views and native drawing. */
final class ControllerControls {
    final MainActivity a;final Repository repo;ReleaseSeekBar fan;CapacityControlView capacity;AirflowSceneView airflow;
    ControllerControls(MainActivity a){this.a=a;repo=a.repo;}
    void cancel(){if(fan!=null)fan.cancelInteraction();if(capacity!=null)capacity.cancelInteraction();if(airflow!=null)airflow.cancelInteraction();}
    void build(){
        boolean swingSupported=Swing.advertised(repo.profile,Swing.VERTICAL)||Swing.advertised(repo.profile,Swing.HORIZONTAL);
        String fanValue=a.friendly(a.reported("ACFanSpeed"));if(a.reported("ACMode").equalsIgnoreCase("dry"))fanValue="Managed by Dry";
        boolean unknownAirflow=Swing.advertised(repo.profile,Swing.VERTICAL)&&code(Swing.VERTICAL)==null||Swing.advertised(repo.profile,Swing.HORIZONTAL)&&code(Swing.HORIZONTAL)==null;
        String airflowValue=swingSupported?(unknownAirflow?"Unknown":(Integer.valueOf(0).equals(code(Swing.VERTICAL))||Integer.valueOf(0).equals(code(Swing.HORIZONTAL))?"Swing":"Fixed")):"Unavailable";
        if(a.openControl.isEmpty()){
            LinearLayout row=a.row(a.body);if(a.getResources().getConfiguration().fontScale>=1.5f)row.setOrientation(LinearLayout.VERTICAL);
            a.controlCard(row,"Fan",fanValue,"fan",a.allowed("ACFanSpeed"),()->toggle("fan"));
            a.controlCard(row,"Airflow",airflowValue,"breeze",swingSupported&&!repo.busy,()->toggle("airflow"));
        }else{
            LinearLayout fanCard=panel("Fan",fanValue,"fan",a.openControl.equals("fan"),a.allowed("ACFanSpeed"),()->toggle("fan"));
            if(a.openControl.equals("fan"))fan(fanCard);
            LinearLayout airCard=panel("Airflow",airflowValue,"breeze",a.openControl.equals("airflow"),swingSupported&&!repo.busy,()->toggle("airflow"));
            if(a.openControl.equals("airflow"))airflow(airCard);
        }
        LinearLayout row=a.row(a.body);boolean large=a.getResources().getConfiguration().fontScale>=1.5f;if(large)row.setOrientation(LinearLayout.VERTICAL);
        for(String name:new String[]{"DisplayControl","PowerfulMode","ECOMode"}){
            String label=name.equals("DisplayControl")?"Display":name.equals("PowerfulMode")?"Powerful":"Eco";
            Button button=chip(row,label,repo.profile.on(repo.observed,name),a.allowed(name),()->utility(name));
            button.setTag("utility-"+name);button.setContentDescription(label+", "+(a.reported(name).isEmpty()?"unknown":a.reported(name))+(!a.allowed(name)?". "+a.disabled(name):""));
        }
        a.space(a.body,8);
        List<String> values=new ArrayList<>(repo.profile.choices("Convertible"));values.sort(Comparator.comparingInt(Integer::parseInt));
        if(!a.reported("Convertible").equals("0"))a.capacityExpanded=false;
        capacity=new CapacityControlView(a,values,a.reported("Convertible"),a.capacityExpanded,a.allowed("Convertible"),new CapacityControlView.Listener(){public void expanded(boolean value){a.capacityExpanded=value;}public void commit(String value){if(!a.allowed("Convertible"))return;a.capacityExpanded=false;Runnable send=()->{if(a.allowed("Convertible"))repo.command("Convertible",value);};if(!value.equals("0")&&repo.profile.on(repo.observed,"PowerfulMode"))a.confirm("Set Converti8 to "+value+"%?","This turns Powerful off before changing capacity. Each step is verified separately.",send);else send.run();}});
        if(repo.pendingControl.equals("Convertible"))capacity.pendingValue=repo.requestedControlValue;capacity.setTag("capacity");a.body.addView(capacity,new LinearLayout.LayoutParams(-1,-2));
        if(!a.allowed("Convertible")&&!repo.busy&&repo.profile.supports("Convertible"))a.muted(a.body,a.disabled("Convertible"),11);
    }
    void toggle(String name){if(repo.busy)return;a.openControl=a.openControl.equals(name)?"":name;a.render();if(!a.openControl.isEmpty())a.scroll.post(()->{View target=a.body.findViewWithTag("expanded-"+name);if(target!=null){int y=target.getTop();a.scroll.smoothScrollTo(0,y);if(android.animation.ValueAnimator.areAnimatorsEnabled()){target.setAlpha(0);target.setTranslationY(a.dp(8));target.animate().alpha(1).translationY(0).setDuration(180).start();}}});}
    LinearLayout panel(String title,String value,String glyph,boolean open,boolean enabled,Runnable action){LinearLayout panel=a.column();panel.setPadding(a.dp(16),a.dp(4),a.dp(16),open?a.dp(12):a.dp(4));panel.setBackground(a.rounded(R.color.panel,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,a.dp(12));a.body.addView(panel,lp);if(open)panel.setTag("expanded-"+(glyph.equals("breeze")?"airflow":glyph));
        LinearLayout header=a.row(panel);header.setMinimumHeight(a.dp(56));header.setGravity(Gravity.CENTER_VERTICAL);header.addView(a.icon(glyph,R.color.muted,20));TextView name=a.muted(header,title,14);LinearLayout.LayoutParams n=new LinearLayout.LayoutParams(0,-2,1);n.setMarginStart(a.dp(8));name.setLayoutParams(n);TextView current=a.text(header,value,16);current.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));current.setGravity(Gravity.END);ImageView arrow=a.icon("down",R.color.muted,18);arrow.setRotation(open?180:0);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(a.dp(18),a.dp(18));ap.setMarginStart(a.dp(8));arrow.setLayoutParams(ap);header.addView(arrow);header.setContentDescription(title+", "+value+", "+(open?"expanded. Collapse":"collapsed. Expand"));header.setFocusable(true);header.setEnabled(enabled);header.setAlpha(enabled?1f:.55f);header.setOnClickListener(v->{if(enabled)action.run();});return panel;}
    Button chip(LinearLayout parent,String label,boolean selected,boolean enabled,Runnable action){Button b=a.button(parent,label,enabled,action);b.setTextSize(14);b.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(8));b.setSelected(selected);b.setBackground(new android.graphics.drawable.InsetDrawable(a.ripple(selected?R.color.violet:R.color.panel,12),0,a.dp(6),0,a.dp(6)));b.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(8));b.setTextColor(a.color(selected?R.color.primary:R.color.ink));a.weighted(b);LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)b.getLayoutParams();p.setMargins(parent.getChildCount()>1?a.dp(8):0,a.dp(2),0,a.dp(2));if(parent.getOrientation()==LinearLayout.VERTICAL)p.setMargins(0,a.dp(2),0,a.dp(2));b.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setCheckable(true);info.setChecked(selected);}});return b;}
    void fan(LinearLayout panel){List<String> choices=repo.profile.choices("ACFanSpeed");boolean enabled=a.allowed("ACFanSpeed");String current=a.reported("ACFanSpeed");if(choices.contains("auto")){Button auto=chip(panel,"Auto",current.equals("auto"),enabled,()->{if(a.allowed("ACFanSpeed")&&!a.reported("ACFanSpeed").equals("auto"))repo.command("ACFanSpeed","auto");});if(!current.equals("auto"))insetOff(auto);}
        List<String> manual=new ArrayList<>();for(String v:new String[]{"quiet","low","medium","high"})if(choices.contains(v))manual.add(v);
        if(!manual.isEmpty()){fan=new ReleaseSeekBar(a,manual,current,value->{if(a.allowed("ACFanSpeed"))repo.command("ACFanSpeed",value);});fan.setTag("fan-slider");fan.setEnabled(enabled);fan.setProgressTintList(ColorStateList.valueOf(a.color(R.color.primary)));fan.setThumbTintList(ColorStateList.valueOf(a.color(R.color.primary)));panel.addView(fan,new LinearLayout.LayoutParams(-1,a.dp(48)));LinearLayout labels=a.row(panel);TextView low=a.muted(labels,a.friendly(manual.get(0)),12);a.weighted(low);TextView high=a.muted(labels,a.friendly(manual.get(manual.size()-1)),12);a.weighted(high);high.setGravity(Gravity.END);}
        if(!enabled)a.muted(panel,a.disabled("ACFanSpeed"),12);
    }
    Integer code(String axis){return Swing.reportedCode(repo.profile,repo.observed,axis);}
    void airflow(LinearLayout panel){Integer v=code(Swing.VERTICAL),h=code(Swing.HORIZONTAL);for(String axis:new String[]{Swing.VERTICAL,Swing.HORIZONTAL}){Integer code=code(axis);if(code!=null&&code>0)a.lastFixed.put(axis,code);}
        boolean ev=a.allowed(Swing.VERTICAL),eh=a.allowed(Swing.HORIZONTAL);
        airflow=new AirflowSceneView(a,v,h,Swing.positions(repo.profile,Swing.VERTICAL),Swing.positions(repo.profile,Swing.HORIZONTAL),ev,eh,(vertical,horizontal)->repo.commandAirflow(vertical,horizontal,v,h));airflow.pendingV=repo.pendingAirflowV;airflow.pendingH=repo.pendingAirflowH;airflow.setTag("airflow-scene");panel.addView(airflow,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout heading=a.row(panel);TextView label=a.muted(heading,"Swing",12);a.weighted(label);a.iconButton(heading,"sliders","Airflow position options and illustration details",!repo.busy,()->{a.swingOverview();});
        LinearLayout row=a.row(panel);if(a.getResources().getConfiguration().fontScale>=1.5f)row.setOrientation(LinearLayout.VERTICAL);
        for(String axis:new String[]{Swing.VERTICAL,Swing.HORIZONTAL}){Integer code=code(axis);boolean on=Integer.valueOf(0).equals(code);List<Integer> choices=Swing.positions(repo.profile,axis);Button b=chip(row,axis.equals(Swing.VERTICAL)?"Vertical":"Horizontal",on,a.allowed(axis)&&choices.contains(0),()->{if(!a.allowed(axis))return;if(!on)repo.command(axis,0);else{Integer last=a.lastFixed.get(axis);if(last!=null&&choices.contains(last))repo.command(axis,last);else a.swingChoice(axis,null);}});if(!on)insetOff(b);b.setTag("swing-"+axis);b.setContentDescription(Swing.title(axis)+", "+(code==null?"unknown":on?"on":"off"));}
        if(!ev&&!eh&&!repo.busy)a.muted(panel,Swing.blocked(repo.profile,repo.observed,Swing.VERTICAL),12);
    }
    void insetOff(Button b){b.setBackground(new android.graphics.drawable.InsetDrawable(a.ripple(R.color.surface,12),0,a.dp(6),0,a.dp(6)));b.setPadding(a.dp(4),a.dp(8),a.dp(4),a.dp(8));}
    void utility(String cap){if(!a.allowed(cap))return;boolean on=repo.profile.on(repo.observed,cap);Runnable send=()->{if(a.allowed(cap))repo.command(cap,on?"off":"on");};if(!on&&cap.equals("ECOMode"))a.confirm("Enable Eco?","Eco also sets the target to 26°C. Each step is verified separately.",send);else if(!on&&cap.equals("PowerfulMode")&&repo.profile.supports("Convertible")&&!Data.same(a.reported("Convertible"),"0"))a.confirm("Enable Powerful?","Converti8 turns off and capacity returns to Normal before Powerful is enabled.",send);else send.run();}
}
