package com.example.rokidkeyboardbridge;
import android.graphics.*;
/** Decode at most a 160px thumbnail; preserve the original hardware JPEG byte-for-byte. */
final class KeepAutoExposureAnalyzer {
 static KeepAutoExposurePolicy.Stats analyze(byte[] jpeg){
  if(jpeg==null||jpeg.length<4||jpeg.length>8_000_000)return null;Bitmap bitmap=null;
  try{
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,bounds);
   if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>20_000_000L)return null;
   BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while((Math.max(bounds.outWidth,bounds.outHeight)+options.inSampleSize-1)/options.inSampleSize>160)options.inSampleSize*=2;
   options.inPreferredConfig=Bitmap.Config.ARGB_8888;bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options);
   if(bitmap==null||bitmap.getWidth()>160||bitmap.getHeight()>160)return null;
   int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());return KeepAutoExposurePolicy.pixels(pixels);
  }catch(RuntimeException|OutOfMemoryError unavailable){return null;}finally{if(bitmap!=null)bitmap.recycle();}
 }
 private KeepAutoExposureAnalyzer(){}
}
