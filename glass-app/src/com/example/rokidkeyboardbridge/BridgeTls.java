package com.example.rokidkeyboardbridge;
import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import org.json.JSONObject;
/** Only public certificate proof crosses HTTP. Payloads and bearer tokens always use pinned TLS. */
final class BridgeTls {
 private static final java.util.concurrent.ConcurrentHashMap<String,String> pins=new java.util.concurrent.ConcurrentHashMap<>();
 static java.net.URLConnection open(URL url,String token,String explicitPin)throws Exception{
  if(url.getPort()!=8765&&url.getPort()!=8766)return url.openConnection();
  if(!"http".equals(url.getProtocol())&&!"https".equals(url.getProtocol()))throw new SecurityException("unsupported bridge protocol");
  String pin;
  if(explicitPin!=null&&!explicitPin.trim().isEmpty())pin=BridgeCertificatePolicy.normalize(explicitPin);
  else{
   if(token==null||token.length()<16)throw new SecurityException("先にスマホの証明書SHA256を登録してください");
   String key=url.getHost()+":"+BridgeCertificatePolicy.fingerprint(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));pin=pins.get(key);
   if(pin==null){
    HttpURLConnection probe=(HttpURLConnection)new URL("http",url.getHost(),8765,"/tls-auth").openConnection();probe.setInstanceFollowRedirects(false);probe.setConnectTimeout(1500);probe.setReadTimeout(1500);
    try{if(probe.getResponseCode()!=200)throw new SecurityException("secure bridge discovery unavailable");ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=probe.getInputStream()){byte[] buf=new byte[1024];int n;while((n=in.read(buf))!=-1){if(b.size()+n>16384)throw new SecurityException("bridge proof too large");b.write(buf,0,n);}}JSONObject proof=new JSONObject(b.toString("UTF-8"));pin=BridgeCertificatePolicy.normalize(proof.getString("fingerprint"));if(!BridgeCertificatePolicy.verifies(token,pin,proof.getString("proof")))throw new SecurityException("スマホのTLS認証が一致しません。平文には戻しません");pins.put(key,pin);}finally{probe.disconnect();}
   }
  }
  HttpsURLConnection c=(HttpsURLConnection)new URL("https",url.getHost(),8766,url.getFile()).openConnection();
  final String expected=pin;c.setSSLSocketFactory(BridgeCertificatePolicy.pinned(pin));c.setHostnameVerifier((host,session)->{try{java.security.cert.Certificate peer=session.getPeerCertificates()[0];((java.security.cert.X509Certificate)peer).checkValidity();return BridgeCertificatePolicy.fingerprint(peer.getEncoded()).equals(expected);}catch(Exception e){return false;}});c.setInstanceFollowRedirects(false);return c;
 }
}
