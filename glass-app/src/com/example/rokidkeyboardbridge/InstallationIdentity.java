package com.example.rokidkeyboardbridge;
import android.content.*;import android.content.pm.*;import org.json.*;
/** Installed public metadata only. No Android ID, serial, path, account or credentials. */
public final class InstallationIdentity {
 public static JSONObject snapshot(Context c)throws Exception{PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);JSONArray certificates=new JSONArray();if(info.signingInfo!=null)for(Signature signature:info.signingInfo.getApkContentsSigners()){java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");StringBuilder text=new StringBuilder();for(byte b:hash.digest(signature.toByteArray()))text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));certificates.put(text.toString());}return new JSONObject().put("version",info.getLongVersionCode()).put("debuggable",(c.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0).put("signingSha256",certificates);}
 private InstallationIdentity(){}
}
