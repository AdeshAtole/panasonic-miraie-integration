package in.room.stayawake;
import android.service.quicksettings.*;
import android.widget.Toast;
public class AwakeTile extends TileService {
 public void onStartListening(){update();}
 public void onClick(){try{Awake.set(this,!Awake.active(this));}catch(Exception e){Toast.makeText(this,"Open Stay Awake to finish setup",1).show();}update();}
 void update(){Tile t=getQsTile();if(t!=null){t.setState(Awake.active(this)?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);t.setSubtitle(Awake.active(this)?"On":"Off");t.updateTile();}}
}
