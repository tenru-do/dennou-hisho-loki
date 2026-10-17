package com.example.rokidgeminisecretary;
import java.io.*;import java.nio.file.*;import java.security.*;import org.json.*;
final class RecorderPhotoStore {
 static File root(android.content.Context c){File f=new File(c.getFilesDir(),"recorder-photos");f.mkdirs();return f;}
 static String hash(byte[] b)throws Exception{StringBuilder s=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(b))s.append(String.format(java.util.Locale.US,"%02x",v&255));return s.toString();}
 static void metadata(File root,String id,JSONObject j)throws Exception{
  if(!j.has("metadata"))return;JSONObject input=j.getJSONObject("metadata"),safe=new JSONObject().put("capturedAt",Long.parseLong(id.substring(0,13))).put("available",false);
  if(input.optBoolean("readOverlay"))safe.put("readOverlay",true);
  if(input.optBoolean("available")){double lat=input.getDouble("latitude"),lon=input.getDouble("longitude"),accuracy=input.getDouble("accuracyMeters");long at=input.getLong("locationAt"),captured=Long.parseLong(id.substring(0,13));if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||accuracy<0||accuracy>500||captured-at<0||captured-at>90000)throw new IOException("invalid location metadata");safe.put("available",true).put("latitude",lat).put("longitude",lon).put("accuracyMeters",accuracy).put("locationAt",at).put("source","phone");}
  File target=new File(root,id+".meta.gcm"),temp=new File(root,id+".meta.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(DeviceSecrets.photos().encrypt(safe.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));out.getFD().sync();}Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
 }
 static JSONObject metadata(File photo)throws Exception{File meta=new File(photo.getParentFile(),photo.getName().replace(".gcm",".meta.gcm"));if(!meta.exists())return new JSONObject().put("available",false);return new JSONObject(new String(read(meta),java.nio.charset.StandardCharsets.UTF_8));}
 static synchronized JSONObject accept(android.content.Context c,JSONObject j)throws Exception{
  String id=j.getString("id"),sha=j.getString("sha");int offset=j.getInt("offset"),total=j.getInt("total");
  if(!id.matches("[0-9]{13}-[a-f0-9-]{36}")||!sha.matches("[a-f0-9]{64}")||total<4||total>8000000||offset<0||offset>=total||offset%131072!=0)throw new IOException("invalid photo metadata");
  File root=root(c),target=new File(root,id+".gcm");if(c.getSharedPreferences("recorder_photo_deletions",0).getBoolean(id,false))return new JSONObject().put("ok",true).put("complete",true).put("deleted",true).put("id",id).put("sha",sha).put("total",total);
  if(target.exists()){if(!hash(DeviceSecrets.photos().decrypt(Files.readAllBytes(target.toPath()))).equals(sha))throw new IOException("photo conflict");metadata(root,id,j);return receipt(target,id,sha,total);}
  byte[] bytes=android.util.Base64.decode(j.getString("data"),android.util.Base64.NO_WRAP);
  if(bytes.length!=Math.min(131072,total-offset))throw new IOException("invalid chunk");
  long used=0;for(File f:root.listFiles()){used+=f.length();if(f.getName().endsWith(".part")&&System.currentTimeMillis()-f.lastModified()>3600000)f.delete();}
  if(used+bytes.length>500000000L||root.getUsableSpace()<100000000L)throw new IOException("phone photo capacity");
  PhotoVaultCipher cipher=DeviceSecrets.photos();File part=new File(root,id+"-"+sha+"-"+offset+".part");
  Files.write(part.toPath(),cipher.encrypt(bytes));metadata(root,id,j);
  boolean complete=true;for(int n=0;n<total;n+=131072)if(!new File(root,id+"-"+sha+"-"+n+".part").isFile())complete=false;
  if(complete){ByteArrayOutputStream out=new ByteArrayOutputStream(total);for(int n=0;n<total;n+=131072)out.write(cipher.decrypt(Files.readAllBytes(new File(root,id+"-"+sha+"-"+n+".part").toPath())));byte[] jpeg=out.toByteArray();
   if(!hash(jpeg).equals(sha)||jpeg[0]!=(byte)255||jpeg[1]!=(byte)216)throw new IOException("photo verification failed");
   File tmp=new File(root,id+".tmp");try(FileOutputStream file=new FileOutputStream(tmp)){file.write(cipher.encrypt(jpeg));file.getFD().sync();}Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
   for(int n=0;n<total;n+=131072)new File(root,id+"-"+sha+"-"+n+".part").delete();
  }return complete?receipt(target,id,sha,total):new JSONObject().put("ok",true).put("complete",false);
 }
 static JSONObject receipt(File target,String id,String sha,int total)throws Exception{
  byte[] stored=read(target);if(stored.length!=total||!hash(stored).equals(sha))throw new IOException("stored photo verification failed");metadata(target);
  return new JSONObject().put("ok",true).put("complete",true).put("stored",true).put("id",id).put("sha",sha).put("total",total);
 }
 static synchronized void delete(android.content.Context context,File photo)throws Exception{
  File directory=root(context).getCanonicalFile();
  if(!photo.getCanonicalFile().getParentFile().equals(directory)||!photo.getName().matches("[0-9]{13}-[a-f0-9-]{36}\\.gcm"))throw new IOException("invalid deletion target");
  String id=photo.getName().replace(".gcm","");if(!context.getSharedPreferences("recorder_photo_deletions",0).edit().putBoolean(id,true).commit())throw new IOException("deletion record failed");
  if(photo.exists()&&!photo.delete())throw new IOException("photo deletion failed");
  File metadata=new File(directory,photo.getName().replace(".gcm",".meta.gcm"));if(metadata.exists()&&!metadata.delete())throw new IOException("metadata deletion failed");
 }
 static byte[] read(File f)throws Exception{return DeviceSecrets.photos().decrypt(Files.readAllBytes(f.toPath()));}
}
