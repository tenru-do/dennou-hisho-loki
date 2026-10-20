package com.example.rokidkeyboardbridge;
import android.app.*;import android.os.*;import android.widget.*;import java.io.*;import java.util.Locale;
/** Read-only bounded inventory; no automatic cleanup of user data. */
final class StorageOverviewDialog {
 static void show(Activity owner){TextView view=new TextView(owner);view.setPadding(20,12,20,12);view.setText("容量を確認中…");AlertDialog dialog=new AlertDialog.Builder(owner).setTitle("グラスの容量").setView(view).setPositiveButton("閉じる",null).show();
  new Thread(()->{String text;try{StatFs fs=new StatFs(owner.getFilesDir().getAbsolutePath());File external=owner.getExternalFilesDir(null);long backup=0;int count=0;if(external!=null){File[] files=external.listFiles();if(files!=null)for(File f:files)if(f.isFile()&&f.getName().matches("loki-pre-[A-Za-z0-9._-]+-backup\\.zip")){backup+=f.length();count++;}}
   text=String.format(Locale.JAPAN,"データ領域：空き %.2f GiB / 全体 %.2f GiB\n更新バックアップZIP：%d個 / %.2f GiB\n\n写真・録音・設定は自動削除しません。バックアップZIPはPCへのコピーとSHA-256・ZIP検証を終えてから整理します。",fs.getAvailableBytes()/1073741824d,fs.getTotalBytes()/1073741824d,count,backup/1073741824d);
  }catch(Exception e){text="容量を確認できませんでした。データは変更していません。";}final String result=text;owner.runOnUiThread(()->{if(!owner.isFinishing()&&dialog.isShowing())view.setText(result);});},"LocalStorageOverview").start();
 }
 private StorageOverviewDialog(){}
}
