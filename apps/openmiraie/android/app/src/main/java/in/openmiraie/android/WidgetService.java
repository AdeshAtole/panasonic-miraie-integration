package in.openmiraie.android;
import android.app.job.*;
import android.os.*;

/** One-shot user gesture. No persisted jobs, retries, periodic work, or reconnect queues. */
public final class WidgetService extends JobService {
    Runnable listener;
    @Override public boolean onStartJob(JobParameters params){Repository r=CompactWidget.repo(this);PersistableBundle b=params.getExtras();
        if(System.currentTimeMillis()-b.getLong("created")>15000||r.busy||!r.signed||r.device==null||!r.device.optString("deviceId").equals(b.getString("device"))){return false;}
        String cap=b.getString("cap","");
        if(!cap.equals("refresh")&&r.readAt!=b.getLong("readAt")){r.notice="Widget state changed. Refresh before changing it.";r.publish();return false;}
        listener=()->{if(!r.busy){r.unlisten(listener);listener=null;jobFinished(params,false);}};r.listen(listener);
        if(cap.equals("refresh"))r.refresh(false);else if(cap.equals("PowerStatus")||cap.equals("DisplayControl"))r.command(cap,b.getString("value"));else{r.unlisten(listener);listener=null;return false;}return true;
    }
    @Override public boolean onStopJob(JobParameters params){if(listener!=null){CompactWidget.repo(this).unlisten(listener);listener=null;}return false;}
}
