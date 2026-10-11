package com.example.rokidgeminisecretary;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
/** Versioned authenticated encryption. Keys are supplied by Android Keystore in production. */
final class PhotoVaultCipher {
 private static final byte[] MAGIC="LOKIENC1".getBytes(StandardCharsets.US_ASCII);
 private final SecretKey key;
 PhotoVaultCipher(SecretKey key){this.key=key;}
 byte[] encrypt(byte[] plain)throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);c.updateAAD(MAGIC);byte[] iv=c.getIV();if(iv.length!=12)throw new java.security.GeneralSecurityException("unexpected GCM nonce");byte[] encrypted=c.doFinal(plain);
  byte[] result=new byte[20+encrypted.length];System.arraycopy(MAGIC,0,result,0,8);System.arraycopy(iv,0,result,8,12);System.arraycopy(encrypted,0,result,20,encrypted.length);return result;
 }
 byte[] decrypt(byte[] bytes)throws Exception {
  if(bytes.length<36||!Arrays.equals(MAGIC,Arrays.copyOf(bytes,8)))throw new java.security.GeneralSecurityException("invalid encrypted photo");
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(bytes,8,20)));c.updateAAD(MAGIC);return c.doFinal(bytes,20,bytes.length-20);
 }
}
