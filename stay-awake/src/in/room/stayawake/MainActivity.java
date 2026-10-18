package in.room.stayawake;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.service.quicksettings.TileService;
import android.view.*;
import android.widget.*;
public class MainActivity extends Activity {
 TextView status;
 public void onCreate(Bundle state){super.onCreate(state); render();}
 void render(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(48,72,48,40);box.setBackgroundColor(Color.rgb(17,25,38));
  TextView title=new TextView(this);title.setText("Stay Awake");title.setTextSize(32);title.setTextColor(Color.WHITE);box.addView(title);
  status=new TextView(this);status.setTextSize(18);status.setTextColor(Color.rgb(154,215,205));status.setPadding(0,30,0,30);box.addView(status);
  TextView note=new TextView(this);note.setText("Keep the screen on while you use MirAIe or any other app.\n\nChoose the foreground service or a silent looping video in picture-in-picture. The power button still locks your phone. Organization policies may still limit awake time.\n\nClose the PiP window or tap Restore normal sleep to stop. The foreground service also has a notification Stop button. Keeping the display on uses more battery.");note.setTextColor(Color.LTGRAY);note.setTextSize(16);box.addView(note);
  button(box,"Start silent video in PiP",v->{Awake.set(this,false);startActivity(new Intent(this,VideoActivity.class));});
  button(box,"Allow notifications",v->requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1));
  button(box,"Keep screen awake",v->change(true));button(box,"Restore normal sleep",v->change(false));
  button(box,"Add Quick Settings tile",v->{getSystemService(StatusBarManager.class).requestAddTileService(new ComponentName(this,AwakeTile.class),"Stay Awake",Icon.createWithResource(this,in.room.stayawake.R.drawable.ic_awake),getMainExecutor(),result->{});});
  button(box,"Open MirAIe",v->{try{startActivity(getPackageManager().getLaunchIntentForPackage("com.panasonic.in.miraie"));}catch(Exception e){Toast.makeText(this,"Open MirAIe from your home screen",1).show();}});
  ScrollView scroll=new ScrollView(this);scroll.addView(box);setContentView(scroll);refresh();
 }
 void button(LinearLayout box,String text,View.OnClickListener listener){Button b=new Button(this);b.setText(text);b.setOnClickListener(listener);box.addView(b,new LinearLayout.LayoutParams(-1,-2));}
 void change(boolean enabled){try{Awake.set(this,enabled);new Handler(getMainLooper()).postDelayed(()->refresh(),300);TileService.requestListeningState(this,new ComponentName(this,AwakeTile.class));}catch(Exception e){new AlertDialog.Builder(this).setMessage("Could not start Stay Awake. Please open the app and try again.").setPositiveButton("OK",null).show();}}
 void refresh(){if(status!=null)status.setText(Awake.active(this)?"ON · screen stays awake":"OFF · normal screen timeout");}
 protected void onResume(){super.onResume();refresh();}
}
