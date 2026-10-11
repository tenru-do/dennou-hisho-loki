package com.example.rokidkeyboardbridge;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
/** One-hour, explicitly shared plaintext exports. Provider is not exported; recipients need a URI grant. */
public final class PhotoExportProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 static File folder(Context c){File f=new File(c.getCacheDir(),"photo-exports");f.mkdirs();return f;}
 static void prune(Context c){File[] files=folder(c).listFiles();if(files!=null)for(File f:files)if(f.lastModified()<System.currentTimeMillis()-3600000L)f.delete();}
 static Uri write(Context c,byte[] jpeg)throws IOException{prune(c);String name="photo-export-"+java.util.UUID.randomUUID()+".jpg";File f=new File(folder(c),name);try(FileOutputStream out=new FileOutputStream(f)){out.write(jpeg);out.getFD().sync();}return Uri.parse("content://"+c.getPackageName()+".photoexports/"+name);}
 private File target(Uri uri)throws FileNotFoundException{String name=uri.getLastPathSegment();if(!getContext().getPackageName().concat(".photoexports").equals(uri.getAuthority())||uri.getPathSegments().size()!=1||name==null||!name.matches("photo-export-[a-f0-9-]{36}\\.jpg"))throw new FileNotFoundException();File f=new File(folder(getContext()),name);if(!f.isFile()||f.lastModified()<System.currentTimeMillis()-3600000L)throw new FileNotFoundException();return f;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(target(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){return "image/jpeg";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{File f=target(uri);MatrixCursor cursor=new MatrixCursor(new String[]{"_display_name","_size"});cursor.addRow(new Object[]{f.getName(),f.length()});return cursor;}catch(Exception e){return new MatrixCursor(new String[]{"_display_name","_size"});}}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
