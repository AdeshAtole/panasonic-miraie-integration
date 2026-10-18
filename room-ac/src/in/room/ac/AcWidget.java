package in.room.ac;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.*;
import org.json.*;
public class AcWidget extends AppWidgetProvider {
 public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)update(c,manager,id);}
 static void updateAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,AcWidget.class)))update(c,m,id);}
 static void update(Context c,AppWidgetManager m,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.ac_widget);String title="Room AC",status="Open Room AC to sign in";
  try{JSONObject d=Api.selected(c);if(d!=null){title=d.optString("deviceName","Room AC");status=c.getSharedPreferences("view",0).getString("status","Tap Refresh for current status");}}catch(Exception e){status="Open Room AC to sign in";}
  v.setTextViewText(R.id.title,title);v.setTextViewText(R.id.status,status);
  Intent open=new Intent(c,MainActivity.class);v.setOnClickPendingIntent(R.id.title,PendingIntent.getActivity(c,100,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  int[] buttons={R.id.on,R.id.off,R.id.refresh};String[] actions={"on","off","refresh"};
  for(int i=0;i<buttons.length;i++){Intent intent=new Intent(c,ControlService.class).setAction(actions[i]);v.setOnClickPendingIntent(buttons[i],PendingIntent.getForegroundService(c,i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));}m.updateAppWidget(id,v);
 }
}
