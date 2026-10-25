package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.pm.*;import org.json.*;
/** Installed public metadata only. No Android ID, serial, path, account or credentials. */
public final class InstallationIdentity {
 private static String cachedOwner="";
 private static JSONObject cached;
 public static synchronized JSONObject snapshot(Context c)throws Exception{
  String owner=c.getPackageName()+"|"+c.getApplicationInfo().sourceDir;
  if(cached!=null&&owner.equals(cachedOwner))return new JSONObject(cached.toString());
  PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);JSONArray certificates=new JSONArray();if(info.signingInfo!=null)for(Signature signature:info.signingInfo.getApkContentsSigners()){java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");StringBuilder text=new StringBuilder();for(byte b:hash.digest(signature.toByteArray()))text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));certificates.put(text.toString());}cached=new JSONObject().put("version",info.getLongVersionCode()).put("debuggable",(c.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0).put("signingSha256",certificates);cachedOwner=owner;return new JSONObject(cached.toString());}
 private InstallationIdentity(){}
}
