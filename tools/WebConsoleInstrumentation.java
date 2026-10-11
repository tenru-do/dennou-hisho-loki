package com.example.rokidgeminisecretary;
import android.app.*;import android.os.Bundle;import android.content.*;import android.graphics.*;
import java.io.*;import java.net.*;import java.lang.reflect.*;import java.util.*;import org.json.*;
public class WebConsoleInstrumentation extends Instrumentation {
 private MainActivity owner;private String token;private final ArrayList<String> checks=new ArrayList<>();
 private void check(boolean value,String name){if(!value)throw new AssertionError(name);checks.add(name);}
 public void onCreate(Bundle args){super.onCreate(args);start();}
 private byte[] request(String method,String path,String data,String credential,String origin)throws Exception {
  ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());Socket client=new Socket(InetAddress.getLoopbackAddress(),server.getLocalPort());Socket accepted=server.accept();server.close();
  Method handler=MainActivity.class.getDeclaredMethod("handleClient",Socket.class);handler.setAccessible(true);final Throwable[] fail={null};Thread t=new Thread(()->{try{handler.invoke(owner,accepted);}catch(Throwable e){fail[0]=e;}finally{try{accepted.close();}catch(Exception ignored){}}});t.start();
  client.setSoTimeout(10000);byte[] body=data==null?new byte[0]:data.getBytes("UTF-8");String headers=method+" "+path+" HTTP/1.1\r\nHost: localhost:8766\r\nX-Loki-Web-Token: "+credential+"\r\n"+(origin.isEmpty()?"":"Origin: "+origin+"\r\n")+"Content-Length: "+body.length+"\r\n\r\n";
  client.getOutputStream().write(headers.getBytes("ISO-8859-1"));client.getOutputStream().write(body);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=client.getInputStream().read(b))>0)out.write(b,0,n);client.close();t.join(10000);if(fail[0]!=null)throw new AssertionError(fail[0]);return out.toByteArray();
 }
 private String text(byte[] response)throws Exception{return new String(response,"UTF-8");}
 private byte[] body(byte[] response){for(int i=0;i<response.length-3;i++)if(response[i]==13&&response[i+1]==10&&response[i+2]==13&&response[i+3]==10)return Arrays.copyOfRange(response,i+4,response.length);throw new AssertionError("missing headers");}
 private JSONObject json(String method,String path,JSONObject data)throws Exception {byte[] response=request(method,path,data==null?null:data.toString(),token,"https://localhost:8766");check(text(response).startsWith("HTTP/1.1 200 "),method+" "+path.split("\\?")[0]);return new JSONObject(new String(body(response),"UTF-8"));}
 private void restore(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> v:values.entrySet()){Object x=v.getValue();if(x instanceof String)e.putString(v.getKey(),(String)x);else if(x instanceof Boolean)e.putBoolean(v.getKey(),(Boolean)x);else if(x instanceof Integer)e.putInt(v.getKey(),(Integer)x);else if(x instanceof Long)e.putLong(v.getKey(),(Long)x);else if(x instanceof Float)e.putFloat(v.getKey(),(Float)x);else if(x instanceof Set)e.putStringSet(v.getKey(),(Set<String>)x);}if(!e.commit())throw new AssertionError("preferences restore");}
 public void onStart(){Bundle report=new Bundle();SharedPreferences web=getTargetContext().getSharedPreferences("loki_web_console",0),deletions=getTargetContext().getSharedPreferences("recorder_photo_deletions",0);Map<String,?> oldWeb=web.getAll(),oldDeleted=deletions.getAll();SharedPreferences bridge=null;Map<String,?> oldBridge=null;ArrayList<File> fixtures=new ArrayList<>();Set<String> originals=new HashSet<>();
 try {
  owner=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  Method getPrefs=MainActivity.class.getDeclaredMethod("getPreferences");getPrefs.setAccessible(true);bridge=(SharedPreferences)getPrefs.invoke(owner);oldBridge=bridge.getAll();
  for(File f:RecorderPhotoStore.root(owner).listFiles())originals.add(f.getName());
  check(text(request("GET","/web/",null,"","")).contains("PC・タブレット"),"public landing asset");
  check(text(request("GET","/web/api/photos",null,"","")).startsWith("HTTP/1.1 401 "),"unauthenticated photo list denied");
  check(text(request("GET","/web/api/history",null,"","")).startsWith("HTTP/1.1 401 "),"unauthenticated history denied");
  String code=LokiWebConsole.openCode();Method claim=LokiWebConsole.class.getDeclaredMethod("claim",String.class);claim.setAccessible(true);check(!(Boolean)claim.invoke(null,"invalid"),"wrong code denied");check((Boolean)claim.invoke(null,code),"valid code admission");check(!(Boolean)claim.invoke(null,code),"code one use");
  Method enroll=LokiWebConsole.class.getDeclaredMethod("enroll",Context.class,String.class);enroll.setAccessible(true);token=((JSONObject)enroll.invoke(null,owner,"検証端末")).getString("token");
  check(!web.getString("sessions","").contains(token),"credential stored as hash only");
  check(text(request("GET","/web/api/photos",null,token,"https://evil.invalid")).startsWith("HTTP/1.1 403 "),"cross origin denied");
  check(text(request("GET","/memory",null,token,"")).startsWith("HTTP/1.1 401 "),"browser credential cannot authorize bridge");
  check(text(request("GET","/web/api/unknown",null,token,"")).startsWith("HTTP/1.1 404 "),"restricted routes");
  Bitmap image=Bitmap.createBitmap(32,16,Bitmap.Config.ARGB_8888);image.eraseColor(Color.RED);ByteArrayOutputStream jpg=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.JPEG,90,jpg);image.recycle();byte[] jpeg=jpg.toByteArray();
  for(int i=0;i<3;i++){String id=(9900000000000L+i)+"-"+UUID.randomUUID();File f=new File(RecorderPhotoStore.root(owner),id+".gcm");fixtures.add(f);JSONObject metadata=new JSONObject().put("available",true).put("latitude",35.0).put("longitude",139.0).put("accuracyMeters",10).put("locationAt",9900000000000L+i);RecorderPhotoStore.accept(owner,new JSONObject().put("id",id).put("sha",RecorderPhotoStore.hash(jpeg)).put("offset",0).put("total",jpeg.length).put("data",android.util.Base64.encodeToString(jpeg,2)).put("metadata",metadata));}
  JSONObject list=json("GET","/web/api/photos",null);check(list.getJSONArray("items").length()<=60&&list.getInt("total")>=3,"bounded list and total");String id=fixtures.get(0).getName().replace(".gcm","");
  byte[] thumb=request("GET","/web/api/photo?id="+id+"&size=thumb",null,token,"");Bitmap decoded=BitmapFactory.decodeByteArray(body(thumb),0,body(thumb).length);check(decoded!=null,"thumbnail decoded");decoded.recycle();
  byte[] full=body(request("GET","/web/api/photo?id="+id,null,token,""));android.media.ExifInterface exif=new android.media.ExifInterface(new ByteArrayInputStream(full));float[] latlon=new float[2];check(exif.getLatLong(latlon)&&Math.abs(latlon[0]-35)<.001&&exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL)!=null,"export JPEG location and capture date");
  check(text(request("GET","/web/api/photo?id=..%2Fsecret",null,token,"")).startsWith("HTTP/1.1 400 "),"path traversal denied");
  check(text(request("POST","/web/api/delete-all","{\"revision\":\"wrong\"}",token,"")).startsWith("HTTP/1.1 409 "),"changed all-delete snapshot denied");
  json("POST","/web/api/delete",new JSONObject().put("ids",new JSONArray().put(id)));check(!fixtures.get(0).exists(),"browser delete reflected in phone store");
  JSONObject retry=RecorderPhotoStore.accept(owner,new JSONObject().put("id",id).put("sha",RecorderPhotoStore.hash(jpeg)).put("offset",0).put("total",jpeg.length));check(retry.optBoolean("deleted")&&!fixtures.get(0).exists(),"deletion tombstone prevents resurrection");
  check(text(request("POST","/web/api/send","{\"message\":\"__LOKI_PHOTO_SYNC__\",\"requestId\":\"test-invalid-123\"}",token,"")).startsWith("HTTP/1.1 400 "),"chat cannot inject internal controls");
  JSONObject sent=new JSONObject().put("message","Web検証の送信（テスト専用）").put("requestId",UUID.randomUUID().toString());json("POST","/web/api/send",sent);JSONObject duplicate=json("POST","/web/api/send",sent);check(duplicate.optBoolean("duplicate"),"send retry deduplicated");
  json("GET","/web/api/history",null);json("POST","/web/api/logout",new JSONObject());check(text(request("GET","/web/api/photos",null,token,"")).startsWith("HTTP/1.1 401 "),"logout revokes token");
  report.putString("result","PASS "+checks.size()+" assertions: "+checks.toString());report.putString("certificateSHA256",BridgeTlsServer.fingerprint());
 }catch(Throwable e){report.putString("failure",e.toString());}
 finally{try{for(File f:fixtures){if(f.exists())RecorderPhotoStore.delete(getTargetContext(),f);File meta=new File(f.getParentFile(),f.getName().replace(".gcm",".meta.gcm"));if(meta.exists())meta.delete();}restore(web,oldWeb);restore(deletions,oldDeleted);if(bridge!=null&&oldBridge!=null)restore(bridge,oldBridge);Set<String> after=new HashSet<>();for(File f:RecorderPhotoStore.root(getTargetContext()).listFiles())after.add(f.getName());if(!after.equals(originals))report.putString("failure","original photo inventory changed");}catch(Throwable e){report.putString("failure","cleanup: "+e);}}
 finish(report.containsKey("failure")?0:-1,report);
 }
}
