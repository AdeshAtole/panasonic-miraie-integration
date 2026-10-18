package in.room.ac;
import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.concurrent.*;
public class ControlService extends Service {
 ExecutorService worker=Executors.newSingleThreadExecutor();
 public int onStartCommand(Intent intent,int flags,int id){
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("controls","AC controls",NotificationManager.IMPORTANCE_LOW));
  startForeground(1,new Notification.Builder(this,"controls").setSmallIcon(R.drawable.ic_ac).setContentTitle("Room AC").setContentText("Contacting your AC…").build());
  final String action=intent==null?"refresh":intent.getAction();
  worker.execute(()->{try{
   if("on".equals(action)||"off".equals(action)){Api.control(this,new JSONObject().put(Api.powerKey(this),action));getSharedPreferences("view",0).edit().putString("status","Command sent · tap Refresh to verify").commit();}
   else if("refresh".equals(action)){JSONObject state=Api.status(this);Api.cacheStatus(this,state);}
  }catch(Exception e){getSharedPreferences("view",0).edit().putString("status",Api.safeError(e)).commit();}finally{AcWidget.updateAll(this);stopSelfResult(id);}});
  return START_NOT_STICKY;
 }
 public IBinder onBind(Intent intent){return null;}
 public void onDestroy(){worker.shutdown();super.onDestroy();}
}
