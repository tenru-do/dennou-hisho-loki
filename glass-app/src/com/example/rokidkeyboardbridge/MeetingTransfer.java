package com.example.rokidkeyboardbridge;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.file.*;
final class MeetingTransfer {
    interface Post {String send(String form)throws Exception;}
    interface Progress {void update(String phase,int expected,int confirmed,int unitsExpected,int unitsConfirmed)throws Exception;}
    static int send(Context c,Post post)throws Exception {return send(c,post,(phase,expected,confirmed,units,done)->{});}
    static int send(Context c,Post post,Progress progress)throws Exception {
        if(MeetingRecorderService.active!=null)throw new IOException("録音終了後に転送してください");
        MeetingAudioArchive.recover(c);
        int count=0;File[] sessions=MeetingAudioArchive.root(c).listFiles();if(sessions==null)throw new IOException("録音一覧を読み取れません");
        int expected=0,units=0,done=0;for(File folder:sessions){File manifest=new File(folder,"session.json");if(!manifest.isFile())continue;JSONObject m=new JSONObject(new String(Files.readAllBytes(manifest.toPath()),java.nio.charset.StandardCharsets.UTF_8));if("stopped".equals(m.optString("state"))){expected++;units+=m.getJSONArray("parts").length();}}
        progress.update("RUNNING",expected,0,units,0);
        for(File folder:sessions){File manifest=new File(folder,"session.json");if(!manifest.isFile())continue;
            JSONObject meta=new JSONObject(new String(Files.readAllBytes(manifest.toPath()),java.nio.charset.StandardCharsets.UTF_8));if(!"stopped".equals(meta.optString("state")))continue;
            JSONArray parts=meta.getJSONArray("parts");
            for(int i=0;i<parts.length();i++){JSONObject p=parts.getJSONObject(i);byte[] audio=DeviceSecrets.photos().decrypt(Files.readAllBytes(new File(folder,p.getString("file")).toPath()));
                if(!MeetingAudioArchive.hash(audio).equals(p.getString("sha")))throw new IOException("録音ファイルのハッシュ不一致");
                JSONObject packet=new JSONObject().put("id",meta.getString("id")).put("index",i).put("sha",p.getString("sha"))
                        .put("encoding","base64url").put("data",android.util.Base64.encodeToString(audio,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE));
                JSONObject ack=new JSONObject(post.send("request="+java.net.URLEncoder.encode(packet.toString(),"UTF-8")));
                if(!ack.optBoolean("ok"))throw new IOException(ack.optString("error","転送結果を確認できません"));
                if(!p.getString("sha").equals(ack.optString("sha")))throw new IOException("転送確認のハッシュ不一致");done++;progress.update("RUNNING",expected,count,units,done);
            }
            ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(java.util.zip.GZIPOutputStream gzip=new java.util.zip.GZIPOutputStream(compressed)){gzip.write(meta.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            JSONObject packet=new JSONObject().put("id",meta.getString("id")).put("manifestGzip",android.util.Base64.encodeToString(compressed.toByteArray(),android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE));
            JSONObject ack=new JSONObject(post.send("request="+java.net.URLEncoder.encode(packet.toString(),"UTF-8")));
            if(!MeetingReceiptPolicy.verified(ack.optBoolean("ok"),ack.optBoolean("complete"),ack.optString("id"),meta.getString("id"),ack.optInt("parts",-1),parts.length(),ack.optLong("pcmBytes",-1),meta.getLong("pcmBytes")))throw new IOException("会議記録全体の保存確認が一致しません。両端末の更新・接続を確認してください");count++;progress.update("RUNNING",expected,count,units,done);
        }progress.update("COMPLETED",expected,count,units,done);return count;
    }
}
