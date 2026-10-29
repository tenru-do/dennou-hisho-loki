package com.example.lokihealbe;
import android.content.*;import org.json.*;import java.io.*;
public final class HealbeSnapshotDeviceChecks {
 public static int run(Context c)throws Exception{int n=0;SnapshotStore.clear(c);if(SnapshotStore.read(c).length()!=0)throw new AssertionError("empty cache");n++;
  JSONObject sample=new JSONObject().put("balanceKcal",500).put("source","HEALBE_SDK").put("measuredAt",123).put("observedAt",456);SnapshotStore.write(c,sample);if(SnapshotStore.read(c).getInt("balanceKcal")!=500)throw new AssertionError("encrypted roundtrip");n++;
  byte[] bytes=java.nio.file.Files.readAllBytes(new File(c.getFilesDir(),"energy.gcm").toPath());if(new String(bytes,"ISO-8859-1").contains("balanceKcal"))throw new AssertionError("plaintext cache");n++;
  SnapshotStore.write(c,new JSONObject(sample.toString()).put("balanceKcal",-300));if(SnapshotStore.read(c).getInt("balanceKcal")!=-300)throw new AssertionError("atomic replacement");n++;
  try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"energy.gcm"))){out.write(new byte[]{1,2,3});}if(SnapshotStore.read(c).length()!=0)throw new AssertionError("corrupt cache must not become measurement");n++;SnapshotStore.clear(c);return n;
 }
}
