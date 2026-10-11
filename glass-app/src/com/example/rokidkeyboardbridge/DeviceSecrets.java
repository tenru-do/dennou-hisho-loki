package com.example.rokidkeyboardbridge;
import android.security.keystore.*;
import javax.crypto.*;
import java.security.*;
final class DeviceSecrets {
 static synchronized javax.crypto.SecretKey key(String alias,String algorithm,int purpose)throws Exception{
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(!store.containsAlias(alias)){
   KeyGenerator generator=KeyGenerator.getInstance(algorithm,"AndroidKeyStore");KeyGenParameterSpec.Builder spec=new KeyGenParameterSpec.Builder(alias,purpose);
   if("AES".equals(algorithm))spec.setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
   else spec.setDigests(KeyProperties.DIGEST_SHA256);
   generator.init(spec.build());generator.generateKey();
  }
  return (javax.crypto.SecretKey)store.getKey(alias,null);
 }
 static PhotoVaultCipher photos()throws Exception{return new PhotoVaultCipher(key("loki-photo-vault-v1","AES",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT));}
 static String auditHash(String text)throws Exception{
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(key("loki-reaction-audit-v2","HmacSHA256",KeyProperties.PURPOSE_SIGN));StringBuilder b=new StringBuilder();for(byte n:mac.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)))b.append(String.format(java.util.Locale.US,"%02x",n&255));return b.toString();
 }
}
