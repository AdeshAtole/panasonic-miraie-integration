package in.room.ac;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
final class Vault {
 static javax.crypto.SecretKey key()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias("room-ac-session")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("room-ac-session",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  return (javax.crypto.SecretKey)ks.getKey("room-ac-session",null);
 }
 static synchronized JSONObject load(Context c)throws Exception{
  String s=c.getSharedPreferences("vault",0).getString("session",null);if(s==null)return new JSONObject();
  JSONObject j=new JSONObject(s);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(j.getString("iv"),0)));return new JSONObject(new String(cipher.doFinal(Base64.decode(j.getString("data"),0)),java.nio.charset.StandardCharsets.UTF_8));
 }
 static synchronized void save(Context c,JSONObject session)throws Exception{
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());JSONObject j=new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),2)).put("data",Base64.encodeToString(cipher.doFinal(session.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),2));
  if(!c.getSharedPreferences("vault",0).edit().putString("session",j.toString()).commit())throw new Exception("Could not save session");
 }
 static void clear(Context c){c.getSharedPreferences("vault",0).edit().clear().commit();c.getSharedPreferences("view",0).edit().clear().commit();}
}
