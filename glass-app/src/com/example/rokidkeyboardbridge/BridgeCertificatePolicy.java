package com.example.rokidkeyboardbridge;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.*;
final class BridgeCertificatePolicy {
 static String fingerprint(byte[] certificate)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(certificate);StringBuilder b=new StringBuilder();for(byte v:digest)b.append(String.format(java.util.Locale.US,"%02x",v&255));return b.toString();}
 static String normalize(String value){String v=value==null?"":value.replace(":","").replace(" ","").trim().toLowerCase(java.util.Locale.US);if(!v.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("スマホで表示した証明書SHA256を入力してください");return v;}
 static String proof(String token,String fingerprint)throws Exception{if(token==null||token.length()<16)throw new GeneralSecurityException("missing bridge credential");Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return java.util.Base64.getEncoder().encodeToString(mac.doFinal(("LOKI-TLS-v1:"+normalize(fingerprint)).getBytes(StandardCharsets.US_ASCII)));}
 static boolean verifies(String token,String fingerprint,String signature)throws Exception{return MessageDigest.isEqual(proof(token,fingerprint).getBytes(StandardCharsets.US_ASCII),signature.getBytes(StandardCharsets.US_ASCII));}
 private static final java.util.LinkedHashMap<String,SSLSocketFactory> factories=new java.util.LinkedHashMap<String,SSLSocketFactory>(4,.75f,true){protected boolean removeEldestEntry(java.util.Map.Entry<String,SSLSocketFactory> row){return size()>4;}};
 static synchronized SSLSocketFactory pinned(String pin)throws Exception{final String expected=normalize(pin);SSLSocketFactory cached=factories.get(expected);if(cached!=null)return cached;X509TrustManager trust=new X509TrustManager(){public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}public void checkClientTrusted(X509Certificate[] c,String a)throws CertificateException{throw new CertificateException("client certificates not accepted");}public void checkServerTrusted(X509Certificate[] c,String a)throws CertificateException{try{if(c==null||c.length==0||!fingerprint(c[0].getEncoded()).equals(expected))throw new CertificateException("bridge certificate mismatch");c[0].checkValidity();}catch(CertificateException e){throw e;}catch(Exception e){throw new CertificateException(e);}}};SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(null,new TrustManager[]{trust},new SecureRandom());SSLSocketFactory created=ssl.getSocketFactory();factories.put(expected,created);return created;}
}
