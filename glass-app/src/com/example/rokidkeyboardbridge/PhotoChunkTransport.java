package com.example.rokidkeyboardbridge;
import java.net.HttpURLConnection;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.function.*;import org.json.*;
/** Every chunk resolves the current bridge destinations. Retries are idempotent by photo ID/hash/offset. */
final class PhotoChunkTransport {
 interface Opener { HttpURLConnection open(String endpoint)throws Exception; }
 static JSONObject send(byte[] body,Supplier<String[]> destinations,Opener opener,Consumer<String> accepted,BooleanSupplier cancelled)throws Exception {
  for(String endpoint:destinations.get()){
   if(cancelled.getAsBoolean())throw new InterruptedIOException("写真転送を中断しました");
   HttpURLConnection connection=null;
   try{
    connection=opener.open(endpoint);connection.setConnectTimeout(1800);connection.setReadTimeout(15000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");connection.setFixedLengthStreamingMode(body.length);
    try(OutputStream out=connection.getOutputStream()){out.write(body);}
    if(connection.getResponseCode()!=200)throw new IOException("写真を受信できません");
    ByteArrayOutputStream response=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(cancelled.getAsBoolean())throw new InterruptedIOException("写真転送を中断しました");if(response.size()+n>65536)throw new IOException("受領情報が大きすぎます");response.write(bytes,0,n);}}
    JSONObject ack=new JSONObject(new String(response.toByteArray(),StandardCharsets.UTF_8));if(!ack.optBoolean("ok"))throw new IOException("写真の保存を確認できません");accepted.accept(endpoint);return ack;
   }catch(InterruptedIOException interrupted){if(cancelled.getAsBoolean())throw interrupted;}
    catch(Exception unavailable){/* Try the next authenticated bridge destination; retain the source. */}
    finally{if(connection!=null)connection.disconnect();}
  }
  throw new IOException("スマホに写真を送れません。接続・空き容量を確認してください");
 }
 private PhotoChunkTransport(){}
}
