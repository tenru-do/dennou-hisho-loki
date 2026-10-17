package com.example.rokidkeyboardbridge;
import android.graphics.*;
import android.media.ExifInterface;
import java.io.*;
final class ImageQuestionBitmap {
 static final int MAX_SOURCE=20000000,MAX_IMAGE=600000;
    static byte[] normalize(byte[] source)throws Exception {
        return normalize(source,2048,MAX_IMAGE);
    }
    static byte[] normalize(byte[] source,int maxEdge,int maxBytes)throws Exception {
        return normalize(source,maxEdge,maxBytes,null);
    }
    static byte[] normalize(byte[] source,int maxEdge,int maxBytes,String[] footer)throws Exception {
        if(maxEdge<1||maxEdge>2048||maxBytes<1||maxBytes>MAX_IMAGE)throw new IllegalArgumentException("image limits");
        if(source.length>MAX_SOURCE)throw new IOException("画像は20MB以内にしてください");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(source,0,source.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>80000000L)throw new IOException("画像を読み取れません（最大8000万画素）");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>maxEdge)options.inSampleSize*=2;
        Bitmap decoded=BitmapFactory.decodeByteArray(source,0,source.length,options);
        if(decoded==null)throw new IOException("画像の形式を確認してください");
        Bitmap rotated=decoded,flat=null;
        try{
            Matrix matrix=new Matrix();
            try{int o=new ExifInterface(new ByteArrayInputStream(source)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                switch(o){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;default:break;}
            }catch(IOException ignored){}
            if(!matrix.isIdentity())rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);
            int footerHeight=footer==null?0:Math.round(Math.max(14,rotated.getWidth()/46f)*(footer.length*1.35f+0.6f));
            flat=Bitmap.createBitmap(rotated.getWidth(),rotated.getHeight()+footerHeight,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(flat);canvas.drawColor(Color.WHITE);canvas.drawBitmap(rotated,0,0,null);
            if(footer!=null)drawFooter(canvas,rotated.getWidth(),rotated.getHeight(),footerHeight,footer);
            for(int quality=90;quality>=30;quality-=10){ByteArrayOutputStream out=new ByteArrayOutputStream();flat.compress(Bitmap.CompressFormat.JPEG,quality,out);if(out.size()<=maxBytes)return out.toByteArray();}
            throw new IOException("画像が複雑すぎます。必要な部分を切り取って再送してください");
        }finally{if(flat!=null)flat.recycle();if(rotated!=decoded)rotated.recycle();decoded.recycle();}
    }
    private static void drawFooter(Canvas canvas,int width,int top,int height,String[] lines){
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);canvas.drawRect(0,top,width,top+height,paint);
        paint.setColor(Color.WHITE);float size=Math.max(14,width/46f),pad=Math.max(4,width/100f),y=top+size*1.15f;
        for(String line:lines){paint.setTextSize(size);float measured=paint.measureText(line);if(measured>width-pad*2)paint.setTextSize(size*(width-pad*2)/measured);canvas.drawText(line,pad,y,paint);y+=size*1.35f;}
    }
}
