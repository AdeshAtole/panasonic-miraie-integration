package in.openmiraie.android;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Account session only. Passwords never enter storage or diagnostics. */
final class SessionStore {
    private final Context context;
    SessionStore(Context c){context=c;}
    private javax.crypto.SecretKey key()throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias("openmiraie.session.v1")) {
            KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder("openmiraie.session.v1",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();
        }
        return (javax.crypto.SecretKey)store.getKey("openmiraie.session.v1",null);
    }
    synchronized JSONObject load()throws Exception {
        String data=context.getSharedPreferences("secure_session",0).getString("encrypted",null);if(data==null)return new JSONObject();
        JSONObject envelope=new JSONObject(data);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(envelope.getString("iv"),Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(envelope.getString("data"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
    }
    synchronized void save(JSONObject session)throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        JSONObject envelope=new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(cipher.doFinal(session.toString().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP));
        if(!context.getSharedPreferences("secure_session",0).edit().putString("encrypted",envelope.toString()).commit())throw new java.io.IOException("Could not save secure session.");
    }
    // Synchronous removal prevents another process gesture observing a signed-out token.
    @android.annotation.SuppressLint("ApplySharedPref")
    synchronized void clear(){context.getSharedPreferences("secure_session",0).edit().clear().commit();}
}
