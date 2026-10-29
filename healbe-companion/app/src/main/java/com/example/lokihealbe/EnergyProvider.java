package com.example.lokihealbe;
import android.content.*;import android.database.*;import android.net.Uri;
public final class EnergyProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){String caller=getCallingPackage();if(!"/latest".equals(uri.getPath())||!("com.example.rokidgeminisecretary".equals(caller)||"com.example.lokihealbe".equals(caller)))throw new SecurityException("Loki health reader only");MatrixCursor cursor=new MatrixCursor(new String[]{"json"});cursor.addRow(new Object[]{getContext().getSharedPreferences("link",0).getBoolean("linked",false)&&getContext().getSharedPreferences("link",0).getBoolean("officialMatchVerified",true)?SnapshotStore.read(getContext()).toString():"{}"});return cursor;}
 public String getType(Uri uri){return "application/json";}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 public int update(Uri uri,ContentValues values,String s,String[] args){throw new UnsupportedOperationException();}
 public int delete(Uri uri,String s,String[] args){throw new UnsupportedOperationException();}
}
