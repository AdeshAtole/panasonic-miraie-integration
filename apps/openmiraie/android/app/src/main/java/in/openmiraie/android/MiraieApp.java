package in.openmiraie.android;
import android.app.Application;
public class MiraieApp extends Application {
    Repository repository;
    Repository createRepository(){return new Repository(this);}
    public void onCreate(){super.onCreate();repository=createRepository();}
}
