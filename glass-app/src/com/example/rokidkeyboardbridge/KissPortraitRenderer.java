package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.*;import java.io.InputStream;
/** Four approved portrait expression pairs with distinct, bounded romantic timing. */
final class KissPortraitRenderer {
 private Bitmap atlas,frame;private int current=-1,preview=-1,scene=-1;private long previewAt,sceneElapsed,changedAt;
 private int split;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private final float[] mesh=new float[25*25*2];
 KissPortraitRenderer(Context context){try(InputStream stream=context.getAssets().open("loki_kiss_faces_v665.png")){Bitmap raw=BitmapFactory.decodeStream(stream);if(raw!=null){atlas=FullBodyPortraitInk.render(raw);raw.recycle();split=FullBodyGestureRegions.split(atlas);}}catch(Exception|OutOfMemoryError error){android.util.Log.w("LokiKissPortrait","Portrait pairs unavailable",error);}}
 boolean needsFastFrames(long now){boolean demo=preview>=0&&now>=previewAt&&now-previewAt<6000;int type=demo?preview:scene;long elapsed=demo?now-previewAt:sceneElapsed;return KissGesturePolicy.strong(type)&&elapsed>=0&&elapsed<KissGesturePolicy.duration(type);}
 boolean ready(){return atlas!=null&&!atlas.isRecycled();}
 Bitmap[] retained(){return new Bitmap[]{atlas,frame};}
 void preview(int type,long now){if(type>=0&&type<7){preview=type;previewAt=now;}}
 void clearPreview(){preview=-1;}
 void scene(int type,long elapsed){scene=type;sceneElapsed=elapsed;}
 void release(){if(frame!=null&&!frame.isRecycled())frame.recycle();if(atlas!=null&&!atlas.isRecycled())atlas.recycle();frame=atlas=null;current=-1;}
 boolean draw(Canvas canvas,int width,int height,int mode,boolean quiet,long now){
  boolean demo=preview>=0&&now>=previewAt&&now-previewAt<6000;int type=demo?preview:scene;long elapsed=demo?now-previewAt:sceneElapsed;
  if(!ready()||type<0||type>6||mode==1||quiet||elapsed<0||elapsed>=KissGesturePolicy.duration(type)||width<=0||height<=0)return false;
  int phase=KissGesturePolicy.faceFrame(type,elapsed),key=KissGesturePolicy.base(type)+phase*4;
  if(current!=key||frame==null){if(frame!=null)frame.recycle();int w=atlas.getWidth()/4,top=phase==0?0:split,h=phase==0?split:atlas.getHeight()-split;frame=Bitmap.createBitmap(atlas,KissGesturePolicy.base(type)*w,top,w,h);current=key;changedAt=now;}
  boolean intense=KissGesturePolicy.strong(type);float h=Math.min(height*(intense?.84f:.94f),width*frame.getHeight()/(float)frame.getWidth()),w=h*frame.getWidth()/frame.getHeight();
  float pulse=KissGesturePolicy.envelope(type,elapsed),gain=KissGesturePolicy.approach(type)*pulse*CharacterMotionTuning.scale();
  float sway=KissGesturePolicy.sway(type,elapsed);float x=(width-w)*.5f,y=(height-h)*.45f;int n=0;
  for(int row=0;row<=24;row++)for(int col=0;col<=24;col++){
   float u=col/24f,v=row/24f,upper=Math.max(0,1-v/.92f);upper*=upper;
   mesh[n++]=x+(u+(u-.5f)*gain*upper+.012f*sway*upper)*w;mesh[n++]=y+(v+(.007f*pulse+(intense?.008f*sway:0))*upper)*h;
  }
  int saved=canvas.save();canvas.rotate((intense?5.2f*sway:(type==1?2.2f:type==2?-2.4f:type==3?2.8f:-.7f)*pulse),width*.5f,y+h);
  paint.setAlpha(Math.round(255*(.80f+.20f*Math.min(1,Math.max(0,now-changedAt)/120f))));canvas.drawBitmapMesh(frame,24,24,mesh,0,null,0,paint);canvas.restoreToCount(saved);return true;
 }
}
