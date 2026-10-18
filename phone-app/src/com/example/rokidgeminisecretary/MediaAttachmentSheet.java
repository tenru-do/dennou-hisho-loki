package com.example.rokidgeminisecretary;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import java.io.*;
import java.util.*;

/** Local-only, bounded visual overview. Video audio is deliberately not included. */
final class MediaAttachmentSheet {
 static final int MAX_IMAGES=6, FRAMES=8;
 static final long MAX_VIDEO_BYTES=50_000_000L, MAX_VIDEO_MS=120_000L;
 static byte[] combineImages(List<byte[]> images)throws Exception {return combineImages(images,java.util.Collections.nCopies(images.size(),"image"));}
 static byte[] combineImages(List<byte[]> images,List<String> kinds)throws Exception {
  if(images.isEmpty()||images.size()>MAX_IMAGES)throw new IOException("画像は合計6枚までです");if(images.size()==1)return images.get(0);
  boolean tall=kinds.contains("video");final int cols=tall?Math.min(3,images.size()):2,cell=tall&&images.size()<=2?896:640,panel=tall?(images.size()<=2?1984:1420):cell,label=48;Bitmap sheet=Bitmap.createBitmap(cell*cols,64+((images.size()+cols-1)/cols)*(panel+label),Bitmap.Config.ARGB_8888);
  try{Canvas c=new Canvas(sheet);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(24);c.drawText("IMAGES: read in numbered order",16,40,p);
   for(int i=0;i<images.size();i++){byte[] bytes=images.get(i);Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(b==null)throw new IOException("添付画像を読み取れません");try{int x=(i%cols)*cell,y=64+(i/cols)*(panel+label);c.drawText("IMAGE "+(i+1)+(kinds.get(i).equals("video")?" / VIDEO 8 FRAMES, NO AUDIO":""),x+12,y+32,p);float scale=Math.min(cell/(float)b.getWidth(),panel/(float)b.getHeight()),w=b.getWidth()*scale,h=b.getHeight()*scale;c.drawBitmap(b,null,new RectF(x+(cell-w)/2,y+label+(panel-h)/2,x+(cell+w)/2,y+label+(panel+h)/2),p);}finally{b.recycle();}}
   for(int q=90;q>=30;q-=10){ByteArrayOutputStream out=new ByteArrayOutputStream();sheet.compress(Bitmap.CompressFormat.JPEG,q,out);if(out.size()<=ImageQuestions.MAX_IMAGE)return out.toByteArray();}throw new IOException("画像が細かすぎます。枚数を減らしてください");
  }finally{sheet.recycle();}
 }
 static byte[] prepare(Context c,List<Uri> uris)throws Exception {
  if(uris.isEmpty()||uris.size()>MAX_IMAGES)throw new IOException("画像は一度に6枚まで、動画は1本を選んでください");
  boolean video=false;
  for(Uri u:uris){if(!"content".equals(u.getScheme()))throw new IOException("写真選択画面から選んでください");String t=c.getContentResolver().getType(u);if(t!=null&&t.startsWith("video/"))video=true;else if(t==null||!t.startsWith("image/"))throw new IOException("画像または動画を選んでください");}
  if(video&&uris.size()!=1)throw new IOException("動画は画像と混ぜず、1本ずつ選んでください");
  if(!video&&uris.size()==1)return ImageQuestions.normalize(ImageQuestions.readBounded(c.getContentResolver().openInputStream(uris.get(0))));
  int count=video?FRAMES:uris.size(),cols=2,cell=video?448:640,label=48,rows=(count+cols-1)/cols;
  Bitmap sheet=Bitmap.createBitmap(cols*cell,rows*(cell+label)+64,Bitmap.Config.ARGB_8888);
  Canvas canvas=new Canvas(sheet);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(24);
  canvas.drawText(video?"VIDEO: sampled frames only / NO AUDIO":"IMAGES: read in numbered order",16,40,paint);
  MediaMetadataRetriever retriever=null;
  try {
   long duration=0;
   if(video){Uri uri=uris.get(0);try(android.os.ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r")){if(fd==null)throw new IOException("動画を開けません");long bytes=fd.getStatSize();if(bytes<0||bytes>MAX_VIDEO_BYTES)throw new IOException("動画は端末に保存した50MB以内のファイルを選んでください");}
    retriever=new MediaMetadataRetriever();retriever.setDataSource(c,uri);String raw=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);duration=raw==null?0:Long.parseLong(raw);if(duration<=0||duration>MAX_VIDEO_MS)throw new IOException("動画は2分以内にしてください");}
   for(int i=0;i<count;i++){
    Bitmap b=null;
    try {long ms=video?Math.round(Math.max(0,duration-Math.min(100,duration/8))*(i/(double)(count-1))):0;
     boolean keyframe=false;
     if(video){b=retriever.getScaledFrameAtTime(ms*1000,MediaMetadataRetriever.OPTION_CLOSEST,cell,cell);if(b==null){b=retriever.getScaledFrameAtTime(ms*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,cell,cell);keyframe=true;}}
     else {byte[] jpeg=ImageQuestions.normalize(ImageQuestions.readBounded(c.getContentResolver().openInputStream(uris.get(i))));b=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length);}
     if(b==null)throw new IOException("動画の場面"+(i+1)+"を読み取れません。別の形式で保存してください");
     int x=(i%cols)*cell,y=(i/cols)*(cell+label)+64;canvas.drawText(video?String.format(Locale.ROOT,"%d / %02d:%02d.%03d",i+1,ms/60000,(ms/1000)%60,ms%1000)+(keyframe?" ~KEYFRAME":""):"IMAGE "+(i+1),x+12,y+32,paint);
     float scale=Math.min(cell/(float)b.getWidth(),cell/(float)b.getHeight());float w=b.getWidth()*scale,h=b.getHeight()*scale;
     canvas.drawBitmap(b,null,new RectF(x+(cell-w)/2,y+label+(cell-h)/2,x+(cell+w)/2,y+label+(cell+h)/2),paint);
    }finally{if(b!=null)b.recycle();}
   }
   for(int q=90;q>=30;q-=10){ByteArrayOutputStream out=new ByteArrayOutputStream();sheet.compress(Bitmap.CompressFormat.JPEG,q,out);if(out.size()<=ImageQuestions.MAX_IMAGE)return out.toByteArray();}
   throw new IOException("添付が細かすぎます。画像の枚数や範囲を減らしてください");
  }finally{sheet.recycle();if(retriever!=null)retriever.release();}
 }
}
