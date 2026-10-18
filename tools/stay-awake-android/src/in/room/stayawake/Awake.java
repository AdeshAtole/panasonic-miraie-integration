package in.room.stayawake;
import android.content.*;
final class Awake {
 static boolean running=false;
 static boolean active(Context c){return running||VideoActivity.active();}
 static void set(Context c,boolean enabled){VideoActivity.stop();Intent i=new Intent(c,AwakeService.class);if(enabled)c.startForegroundService(i);else{c.stopService(i);running=false;}}
}
