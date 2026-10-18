package in.room.stayawake;
import android.app.*;
import android.os.*;
import android.content.*;
public class AwakeService extends Service {
 private PowerManager.WakeLock lock;
 public void onCreate(){super.onCreate();}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null && "stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel("awake","Stay Awake",NotificationManager.IMPORTANCE_LOW));
  PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,AwakeService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(this,"awake").setSmallIcon(in.room.stayawake.R.drawable.ic_awake).setContentTitle("Stay Awake is on").setContentText("Keeping the screen awake. Tap Stop to allow sleep.").setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build();
  startForeground(1,n);
  if(lock==null){lock=getSystemService(PowerManager.class).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"room:stayawake");lock.setReferenceCounted(false);}
  lock.acquire();
  Awake.running=true; android.service.quicksettings.TileService.requestListeningState(this,new ComponentName(this,AwakeTile.class));
  return START_NOT_STICKY;
 }
 public void onDestroy(){if(lock!=null&&lock.isHeld())lock.release();Awake.running=false; android.service.quicksettings.TileService.requestListeningState(this,new ComponentName(this,AwakeTile.class));super.onDestroy();}
 public IBinder onBind(Intent intent){return null;}
}
