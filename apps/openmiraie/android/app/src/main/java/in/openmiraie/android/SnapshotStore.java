package in.openmiraie.android;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import org.json.*;
import java.time.Instant;
import java.util.*;

final class SnapshotStore extends SQLiteOpenHelper {
    SnapshotStore(Context c){super(c,"snapshots.db",null,1);}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE samples (_id INTEGER PRIMARY KEY, device TEXT NOT NULL, observed TEXT NOT NULL, payload TEXT NOT NULL)");db.execSQL("CREATE INDEX by_device ON samples(device,_id)");}
    public void onUpgrade(SQLiteDatabase db,int old,int next){throw new IllegalStateException("Migration required");}
    static String ref(String device)throws Exception{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(device.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b & 255));return out.toString();}
    void record(String device,JSONObject raw,Instant time)throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues row=new ContentValues();row.put("device",ref(device));row.put("observed",time.toString());row.put("payload",Data.redact(raw).toString());db.insertOrThrow("samples",null,row);db.execSQL("DELETE FROM samples WHERE _id NOT IN (SELECT _id FROM samples ORDER BY _id DESC LIMIT 5000)");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    int count(String device)throws Exception{try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM samples WHERE device=?",new String[]{ref(device)})){return c.moveToFirst()?c.getInt(0):0;}}
    String csv(String device)throws Exception {
        StringBuilder out=new StringBuilder(Data.row("observed_at_utc","source","reported_field","reported_value"));
        try(Cursor c=getReadableDatabase().rawQuery("SELECT observed,payload FROM samples WHERE device=? ORDER BY _id",new String[]{ref(device)})){
            while(c.moveToNext()){JSONObject data=new JSONObject(c.getString(1));List<String> keys=new ArrayList<>();data.keys().forEachRemaining(keys::add);Collections.sort(keys);for(String k:keys)out.append(Data.row(c.getString(0),"REST mobile/status",k,Data.value(data,k)));}}
        return out.toString();
    }
}
