package in.openmiraie.android;
import android.app.*;
import android.app.job.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.RemoteViews;

public class CompactWidget extends AppWidgetProvider {
    static Repository repo(Context c){return ((MiraieApp)c.getApplicationContext()).repository;}
    static void updateAll(Context c){AppWidgetManager manager=AppWidgetManager.getInstance(c);for(Class<?> type:new Class<?>[]{CompactWidget.class,WideWidget.class}){int[] ids=manager.getAppWidgetIds(new ComponentName(c,type));for(int id:ids)render(c,manager,id,type==WideWidget.class);}}
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){updateAll(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){updateAll(c);}
    static PendingIntent open(Context c,int id){return PendingIntent.getActivity(c,id,new Intent(c,MainActivity.class).setAction("in.openmiraie.android.OPEN_CONTROLLER").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static PendingIntent action(Context c,int id,String cap,String value,Repository r){Intent i=new Intent(c,CompactWidget.class).setAction("in.openmiraie.android.WIDGET").setData(android.net.Uri.parse("openmiraie://widget/"+id+"/"+cap)).putExtra("cap",cap).putExtra("value",value).putExtra("device",r.device==null?"":r.device.optString("deviceId")).putExtra("readAt",r.readAt);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void render(Context c,AppWidgetManager manager,int id,boolean wide){Repository r=repo(c);RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.widget);view.setTextViewText(R.id.widget_title,r.signed?Data.title(r.device):"openMirAIe · Sign in");
        String mode=r.profile.value(r.observed,"ACMode"),target=r.profile.value(r.observed,"ACTemperature");
        boolean available=r.signed&&r.device!=null&&r.readAt>0;boolean small=manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,180)<220;
        String recovery=!r.signed?"Sign in to read your AC":r.device==null?"Choose an AC":r.readAt==0?"Refresh to load readings":"";
        String state=r.busy?"Updating…":!r.notice.isEmpty()&&!r.notice.equals("Updated reported state.")&&!r.notice.startsWith("AC reports ")?"Open app to check":"";
        if(small){int pad=Math.round(8*c.getResources().getDisplayMetrics().density);view.setViewPadding(R.id.widget_root,pad,pad,pad,pad);}
        view.setViewVisibility(R.id.widget_badge,small?View.GONE:View.VISIBLE);view.setTextViewTextSize(R.id.widget_target,android.util.TypedValue.COMPLEX_UNIT_SP,small?(wide?26:30):36);
        view.setTextViewText(R.id.widget_badge,available?(r.profile.on(r.observed,"PowerStatus")?"On":"Off")+" · "+MainActivity.friendly(mode):"Not connected");
        view.setTextViewText(R.id.widget_target,available?(mode.equalsIgnoreCase("fan")?"Fan":MainActivity.targetLabel(target)+"°C"):"—");
        view.setTextViewText(R.id.widget_room,"Room "+Data.room(r.observed));view.setViewVisibility(R.id.widget_room,wide&&available?View.VISIBLE:View.GONE);
        view.setTextViewText(R.id.widget_state,recovery.isEmpty()?state:recovery);view.setViewVisibility(R.id.widget_state,recovery.isEmpty()&&state.isEmpty()?View.GONE:View.VISIBLE);
        view.setViewVisibility(R.id.widget_time,small&&(!state.isEmpty()||!recovery.isEmpty())?View.GONE:View.VISIBLE);view.setTextViewText(R.id.widget_time,r.readAt==0?"No reading":(r.profile.online(r.observed)?"Updated ":"Offline / unknown · ")+android.text.format.DateFormat.getTimeFormat(c).format(new java.util.Date(r.readAt)));
        view.setOnClickPendingIntent(R.id.widget_title,open(c,id));view.setOnClickPendingIntent(R.id.widget_state,open(c,id));view.setOnClickPendingIntent(R.id.widget_target,open(c,id));view.setOnClickPendingIntent(R.id.widget_room,open(c,id));
        view.setViewVisibility(R.id.widget_display,wide?View.VISIBLE:View.GONE);
        String power=r.profile.on(r.observed,"PowerStatus")?"off":"on",display=r.profile.on(r.observed,"DisplayControl")?"off":"on";
        view.setTextViewText(R.id.widget_power,"Turn "+power);view.setTextViewText(R.id.widget_display,"Display "+display);
        view.setBoolean(R.id.widget_power,"setEnabled",!r.busy&&r.signed&&r.profile.blocked(r.observed,"PowerStatus").isEmpty());view.setBoolean(R.id.widget_display,"setEnabled",!r.busy&&r.signed&&r.profile.blocked(r.observed,"DisplayControl").isEmpty());view.setBoolean(R.id.widget_refresh,"setEnabled",!r.busy&&r.signed&&r.device!=null);
        view.setOnClickPendingIntent(R.id.widget_power,action(c,id,"PowerStatus",power,r));view.setOnClickPendingIntent(R.id.widget_display,action(c,id,"DisplayControl",display,r));view.setOnClickPendingIntent(R.id.widget_refresh,action(c,id,"refresh","",r));
        manager.updateAppWidget(id,view);
    }
    @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);if(!"in.openmiraie.android.WIDGET".equals(intent.getAction()))return;
        Repository r=repo(c);String device=intent.getStringExtra("device");
        if(r.busy||!r.signed||r.device==null||!r.device.optString("deviceId").equals(device)){updateAll(c);return;}
        PersistableBundle b=new PersistableBundle();b.putString("device",device);b.putString("cap",intent.getStringExtra("cap"));b.putString("value",intent.getStringExtra("value"));b.putLong("created",System.currentTimeMillis());b.putLong("readAt",intent.getLongExtra("readAt",0));
        JobInfo job=new JobInfo.Builder(410,new ComponentName(c,WidgetService.class)).setExtras(b).setOverrideDeadline(0).build();
        if(c.getSystemService(JobScheduler.class).schedule(job)!=JobScheduler.RESULT_SUCCESS){r.notice="Widget request could not start. Open app to check.";r.publish();}
    }
}
