package com.example.rokidgeminisecretary;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Only a random temporary camera target is grantable; never exposes app data. */
public final class ImageQuestionCameraProvider extends ContentProvider {
    static File target(Context c,String id)throws FileNotFoundException {
        if(id==null||!id.matches("[a-f0-9-]{36}"))throw new FileNotFoundException();
        File root=new File(c.getCacheDir(),"image-camera");root.mkdirs();File[] files=root.listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>ImageQuestions.TTL)f.delete();return new File(root,id+".jpg");
    }
    public boolean onCreate(){return true;}
    public String getType(Uri u){return "image/jpeg";}
    public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException {
        if(!"content".equals(u.getScheme())||!getContext().getPackageName().concat(".image-camera").equals(u.getAuthority())||u.getPathSegments().size()!=1)throw new FileNotFoundException();
        File f=target(getContext(),u.getLastPathSegment());
        if(!f.exists()||System.currentTimeMillis()-f.lastModified()>ImageQuestions.TTL)throw new FileNotFoundException();
        if(!mode.equals("w")&&!mode.equals("rw")&&!mode.equals("r"))throw new FileNotFoundException();
        int flags=mode.equals("r")?ParcelFileDescriptor.MODE_READ_ONLY:(mode.equals("rw")?ParcelFileDescriptor.MODE_READ_WRITE:ParcelFileDescriptor.MODE_WRITE_ONLY)|ParcelFileDescriptor.MODE_TRUNCATE;
        return ParcelFileDescriptor.open(f,flags);
    }
    public Cursor query(Uri u,String[] p,String s,String[] a,String order){return null;}
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
