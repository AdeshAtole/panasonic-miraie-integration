package in.room.ac;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;
import java.io.*;
/** Local snapshots taken only when the user refreshes; no polling or background timer. */
final class MetricStore extends SQLiteOpenHelper {
 MetricStore(Context c){super(c,"ac-readings.db",null,1);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE samples (_id INTEGER PRIMARY KEY, device TEXT NOT NULL, observed_at TEXT NOT NULL, payload TEXT NOT NULL)");db.execSQL("CREATE INDEX sample_device ON samples(device,_id)");}
 public void onUpgrade(SQLiteDatabase db,int a,int b){}
 static String deviceRef(Context c)throws Exception{JSONObject d=Api.selected(c);if(d==null)throw new IOException("Choose an AC first");byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(d.getString("deviceId").getBytes("UTF-8"));StringBuilder s=new StringBuilder();for(byte x:hash)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
 static void record(Context c,JSONObject state)throws Exception{try(MetricStore store=new MetricStore(c)){SQLiteDatabase db=store.getWritableDatabase();ContentValues row=new ContentValues();row.put("device",deviceRef(c));row.put("observed_at",java.time.Instant.now().toString());row.put("payload",Api.redact(Api.fields(state)).toString());db.insertOrThrow("samples",null,row);db.execSQL("DELETE FROM samples WHERE _id NOT IN (SELECT _id FROM samples ORDER BY _id DESC LIMIT 5000)");}}
 static int count(Context c){try(MetricStore store=new MetricStore(c);Cursor cursor=store.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM samples WHERE device=?",new String[]{deviceRef(c)})){return cursor.moveToFirst()?cursor.getInt(0):0;}catch(Exception e){return 0;}}
 static void flatten(JSONObject data,String prefix,Map<String,String> out){List<String> keys=new ArrayList<>();Iterator<String> it=data.keys();while(it.hasNext())keys.add(it.next());Collections.sort(keys);for(String key:keys){Object value=data.opt(key);String path=prefix.isEmpty()?key:prefix+"."+key;if(value instanceof JSONObject)flatten((JSONObject)value,path,out);else out.put(path,value==null||value==JSONObject.NULL?"":value.toString());}}
 static String csv(String value){if(!value.isEmpty()&&"=+@-".indexOf(value.charAt(0))>=0&&!value.matches("[-+]?\\d+(\\.\\d+)?"))value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
 static void export(Context c,OutputStream output)throws Exception{
  List<Map<String,String>> rows=new ArrayList<>();TreeSet<String> fields=new TreeSet<>();
  try(MetricStore store=new MetricStore(c);Cursor cursor=store.getReadableDatabase().rawQuery("SELECT observed_at,payload FROM samples WHERE device=? ORDER BY _id",new String[]{deviceRef(c)})){
   while(cursor.moveToNext()){Map<String,String> row=new LinkedHashMap<>();row.put("observed_at_utc",cursor.getString(0));row.put("source","REST mobile/status");Map<String,String> values=new LinkedHashMap<>();flatten(new JSONObject(cursor.getString(1)),"",values);for(Map.Entry<String,String> e:values.entrySet())row.put("reported."+e.getKey(),e.getValue());fields.addAll(row.keySet());rows.add(row);}
  }
  List<String> columns=new ArrayList<>();columns.add("observed_at_utc");columns.add("source");fields.removeAll(columns);columns.addAll(fields);
  java.io.Writer writer=new java.io.OutputStreamWriter(output,"UTF-8");writeRow(writer,columns);for(Map<String,String> row:rows){List<String> values=new ArrayList<>();for(String col:columns)values.add(row.getOrDefault(col,""));writeRow(writer,values);}writer.flush();
 }
 static void exportEnergy(JSONObject history,OutputStream output)throws Exception{
  Object data=history.opt("data");JSONArray rows=data instanceof JSONArray?(JSONArray)data:(data instanceof JSONObject?((JSONObject)data).optJSONArray("data"):null);if(rows==null)throw new IOException("Load energy history before exporting it");
  Writer writer=new OutputStreamWriter(output,"UTF-8");writeRow(writer,Arrays.asList("date","energy_kwh","source"));
  java.time.format.DateTimeFormatter format=java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy",Locale.UK);
  for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;String date=row.optString("day");try{date=java.time.LocalDate.parse(date,format).toString();}catch(Exception ignored){}writeRow(writer,Arrays.asList(date,row.optString("power"),"Panasonic Daily energy history"));}writer.flush();
 }
 static void writeRow(java.io.Writer writer,List<String> cells)throws IOException{for(int i=0;i<cells.size();i++){if(i>0)writer.write(',');writer.write(csv(cells.get(i)));}writer.write("\r\n");}
}
