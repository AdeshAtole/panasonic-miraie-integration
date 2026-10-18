package in.room.stayawake;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.util.Rational;
import android.view.*;
import android.widget.*;
public class VideoActivity extends Activity {
 static java.lang.ref.WeakReference<VideoActivity> current=new java.lang.ref.WeakReference<>(null);
 static boolean active(){return current.get()!=null;}
 static void stop(){VideoActivity a=current.get();if(a!=null)a.runOnUiThread(()->a.finish());}
 VideoView video;
 boolean ready=false;
 PictureInPictureParams params(){return new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).setAutoEnterEnabled(true).build();}
 public void onCreate(Bundle state){
  super.onCreate(state);current=new java.lang.ref.WeakReference<>(this);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.rgb(17,25,38));
  video=new VideoView(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);frame.addView(video,lp);setContentView(frame);
  video.setKeepScreenOn(true);
  video.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(0,0);ready=true;video.start();setPictureInPictureParams(params());new Handler(getMainLooper()).postDelayed(()->{if(!isFinishing()&&!isInPictureInPictureMode())enterPictureInPictureMode(params());},700);});
  video.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,"Video playback failed",1).show();finish();return true;});
  video.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+in.room.stayawake.R.raw.awake_loop));
 }
 protected void onUserLeaveHint(){super.onUserLeaveHint();if(ready&&!isInPictureInPictureMode())enterPictureInPictureMode(params());}
 protected void onStop(){super.onStop();if(video!=null)video.stopPlayback();finish();}
 protected void onDestroy(){if(current.get()==this)current.clear();if(video!=null)video.stopPlayback();super.onDestroy();}
}
