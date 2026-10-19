package in.openmiraie.android;

import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Runs only against the offline review build; saves actual emulator captures and geometry. */
@SuppressWarnings("deprecation")
public final class LayoutReviewTest extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{testScreens();result.putString("stream","Layout audit passed\n");finish(-1,result);}catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));finish(0,result);}}
    private Instrumentation getInstrumentation(){return this;}
    private void assertEquals(Object expected,Object actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    private void assertTrue(String message,boolean value){if(!value)throw new AssertionError(message);}
    private MainActivity activity;
    private final List<String> findings=new ArrayList<>();
    private String scenario;
    private final StringBuilder geometry=new StringBuilder();
    public void testScreens()throws Exception {
        assertEquals("in.openmiraie.android.review",getInstrumentation().getTargetContext().getPackageName());
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,getInstrumentation().getTargetContext().checkSelfPermission("android.permission.INTERNET"));
        activity=(MainActivity)startActivitySync(new Intent().setClassName("in.openmiraie.android.review","in.openmiraie.android.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        geometry.append(activity.getResources().getConfiguration().toString()).append("\n");
        capture("controller");bottom("controller");ui(()->activity.scroll.scrollTo(0,0));
        sheet("mode",()->activity.modeChoice());
        sheet("fan",()->activity.choice("Fan speed","ACFanSpeed",activity.repo.profile.choices("ACFanSpeed")));
        sheet("airflow",()->activity.swingOverview());
        sheet("vertical",()->activity.swingChoice(Swing.VERTICAL,null));
        sheet("horizontal",()->activity.swingChoice(Swing.HORIZONTAL,null));
        sheet("more",()->activity.moreControls());
        for(String page:new String[]{"readings","energy","snapshots","settings","devices"}){
            ui(()->{if(page.equals("energy"))activity.repo.loadEnergy();activity.navigate(page);});capture(page);bottom(page);
        }
        sheet("details",()->activity.details());
        ui(()->{activity.repo.device.put("spaceName","North-facing upstairs bedroom");activity.navigate("controller");});capture("long-room");
        ui(()->{activity.repo.device.put("spaceName","Bedroom");activity.repo.observed.put("acmd","dry");activity.render();});capture("dry");
        ui(()->{activity.repo.observed.put("acmd","fan");activity.render();});capture("fan-mode");
        ui(()->{activity.repo.observed.put("acmd","cool");activity.repo.observed.put("onlineStatus",false);activity.render();});capture("offline");
        ui(()->{activity.repo.observed.put("onlineStatus",true);activity.repo.pendingTemperature=25d;activity.repo.previousTemperature=24d;activity.repo.busy=true;activity.render();});capture("pending");
        ui(()->{activity.repo.busy=false;activity.repo.pendingTemperature=null;activity.repo.previousTemperature=null;activity.repo.signOut();});capture("signin");bottom("signin");
        File out=new File(activity.getExternalFilesDir(null),"layout-review");
        try(FileWriter writer=new FileWriter(new File(out,"geometry.txt"))){writer.write(geometry.toString());}
        try(FileWriter writer=new FileWriter(new File(out,"findings.txt"))){for(String f:findings)writer.write(f+"\n");}
        ui(()->{activity.repo.load();activity.navigate("controller");});
        assertTrue(String.join("\n",findings),findings.isEmpty());
    }
    interface Action{void run()throws Exception;}
    private void ui(Action action)throws Exception {final Throwable[] error={null};getInstrumentation().runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});getInstrumentation().waitForIdleSync();if(error[0]!=null)throw new RuntimeException(error[0]);}
    private void sheet(String name,Action action)throws Exception{ui(action);capture(name);ui(()->{ViewGroup decor=(ViewGroup)activity.modal.getWindow().getDecorView();scrollBottom(decor);});capture(name+"-bottom");ui(()->activity.closeModal());}
    private void bottom(String name)throws Exception{ui(()->activity.scroll.fullScroll(View.FOCUS_DOWN));capture(name+"-bottom");}
    private void scrollBottom(View view){if(view instanceof ScrollView)((ScrollView)view).fullScroll(View.FOCUS_DOWN);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)scrollBottom(((ViewGroup)view).getChildAt(i));}
    private void capture(String name)throws Exception{
        scenario=name;Thread.sleep(350);getInstrumentation().waitForIdleSync();
        ui(()->{inspect(activity.modal!=null&&activity.modal.isShowing()?activity.modal.getWindow().getDecorView():activity.root);
            geometry.append(name).append(" root padding ").append(activity.root.getPaddingTop()).append("/").append(activity.root.getPaddingBottom()).append(" scroll ").append(activity.root.getScrollY()).append("\n");
            WindowInsets insets=activity.root.getRootWindowInsets();
            if(insets!=null&&activity.root.getPaddingTop()<insets.getInsets(WindowInsets.Type.statusBars()).top)report("Missing status-bar inset");
        });
        File dir=new File(activity.getExternalFilesDir(null),"layout-review");dir.mkdirs();Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    private void report(String message){String finding=scenario+": "+message;if(!findings.contains(finding))findings.add(finding);}
    private void inspect(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        float density=activity.getResources().getDisplayMetrics().density;
        String name=view instanceof TextView?((TextView)view).getText().toString():String.valueOf(view.getContentDescription());
        if(view.isClickable()&&view.isEnabled()&&(view.getWidth()+1<48*density||view.getHeight()+1<48*density))report("Small touch target "+name+" "+view.getWidth()/density+"x"+view.getHeight()/density);
        if(view instanceof TextView){TextView text=(TextView)view;if(text.getEllipsize()==null&&text.getLayout()!=null&&text.getLayout().getHeight()>text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+1)report("Clipped text height: "+name);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;
            if(!(group instanceof ScrollView)&&group.getClipChildren()&&(child.getLeft()<0||child.getTop()<0||child.getRight()>group.getWidth()+1||child.getBottom()>group.getHeight()+1))report("Child exceeds parent: "+child.getClass().getSimpleName()+" "+child.getContentDescription()+" ["+child.getLeft()+","+child.getTop()+","+child.getRight()+","+child.getBottom()+"] parent "+group.getWidth()+"x"+group.getHeight());inspect(child);}}
    }
}
