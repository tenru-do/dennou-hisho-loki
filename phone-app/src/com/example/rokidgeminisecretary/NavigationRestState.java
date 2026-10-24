package com.example.rokidgeminisecretary;
import android.content.Context;import android.util.AtomicFile;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;
/** An encrypted, destination-bound pause. Resumption always needs explicit action. */
final class NavigationRestState {
 static File file(Context c){return new File(c.getFilesDir(),"navigation-rest.gcm");}
 static synchronized void pause(Context c,String destination,String mode,String point)throws Exception{if(destination==null||destination.isEmpty()||destination.length()>600||!mode.matches("walking|transit|bicycling|driving"))throw new IOException("案内中の目的地を確認してください");if(point!=null&&!point.isEmpty())NavigationStopover.point(destination,point);JSONObject j=new JSONObject().put("destination",destination).put("mode",mode).put("point",point==null?"":point).put("at",System.currentTimeMillis());AtomicFile f=new AtomicFile(file(c));FileOutputStream stream=null;try{stream=f.startWrite();stream.write(DeviceSecrets.photos().encrypt(j.toString().getBytes(StandardCharsets.UTF_8)));f.finishWrite(stream);}catch(Exception e){if(stream!=null)f.failWrite(stream);throw e;}}
 static synchronized JSONObject read(Context c)throws Exception{AtomicFile f=new AtomicFile(file(c));if(!f.getBaseFile().exists()&&!new File(f.getBaseFile()+".bak").exists())return null;byte[] data=f.readFully();if(data.length>8192)throw new IOException("一時停止の記録を確認してください");JSONObject j=new JSONObject(new String(DeviceSecrets.photos().decrypt(data),StandardCharsets.UTF_8));long age=System.currentTimeMillis()-j.getLong("at");if(age<0||age>86400000)j.put("needsReview",true);return j;}
 static boolean matches(JSONObject state,String destination,String mode){return state!=null&&destination.equals(state.optString("destination"))&&mode.equals(state.optString("mode"));}
 static synchronized void clear(Context c){new AtomicFile(file(c)).delete();}
 private NavigationRestState(){}
}
