package com.example.rokidkeyboardbridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;
import java.io.InputStream;

/** Seven wardrobe-matched ordinary seated keyframes; caller cadence only, no sensor or network work. */
final class SeatedLegMotionRenderer {
 static final String ASSET="loki_seated_leg_transition_v742.png";
 // One manually reviewed source calibration for every frame. Never fit individual feet.
 private static final float[] FACE={362,543,164,60,7,6,2,1,184,62,7,6,2,1,174,80,9,3,2};
 private final SeatedAnkleMotion ankleMotion=new SeatedAnkleMotion();
 private long ankleAge;private boolean ankleAllowed;
 private final View host;
 private final QuietPosePerformance performance;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final float[] mesh=new float[17*33*2],presence=new float[4];
 private boolean heldFrame;private int tweenTarget;private float tweenAmount;
 private final Paint additiveLayer=new Paint();
 private final FullBodyFaceAnimator[] faces={new FullBodyFaceAnimator(),new FullBodyFaceAnimator()};
 private final int[] faceFrames={-1,-1};
 private SeatedLegMotionPolicy timeline=new SeatedLegMotionPolicy();
 private Bitmap[] frames;

 private int generation;
 private boolean loading,played;
 private long retryAt;

 SeatedLegMotionRenderer(View host,QuietPosePerformance performance){this.host=host;this.performance=performance;additiveLayer.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));}
 boolean request(long now){boolean accepted=timeline.request(now,true);if(accepted)host.invalidate();return accepted;}
 boolean request(long now,int action){
  if(action==SeatedLegAction.SWITCH)return request(now);
  int target=action==SeatedLegAction.CROSS?(timeline.settledSide()==SeatedLegMotionPolicy.NEUTRAL?SeatedLegMotionPolicy.RIGHT:timeline.settledSide()):action;
  boolean accepted=timeline.requestTarget(now,true,target);if(accepted)host.invalidate();return accepted;
 }
 boolean active(){return timeline.active();}
 boolean waiting(){return timeline.waiting();}
 boolean moving(){return timeline.active()&&!timeline.paused();}
 void pause(long now,boolean natural){timeline.update(now,true,true,false,false,natural);ankleMotion.update(now,false);}
 int cachedFrames(){return frames==null?0:frames.length;}
 String diagnostics(){return "legFrame="+timeline.frame()+" next="+timeline.nextFrame()+" active="+timeline.active()+" paused="+timeline.paused()+" waiting="+timeline.waiting()+" cached="+cachedFrames();}
 String preparationReason(){return timeline.waiting()&&frames==null?"脚の動きを準備中・前の姿勢を保持":"";}

 /** Context changes discard pending work; the next ordinary seated session starts cleanly. */
 void cancel(long now){boolean reset=played||timeline.active()||timeline.waiting();timeline.cancel(now);if(reset)timeline=new SeatedLegMotionPolicy();played=false;ankleMotion.reset();}

 private static void recycle(Bitmap[] images){if(images!=null)for(Bitmap image:images)if(image!=null&&!image.isRecycled())image.recycle();}
 private void requestFrames(long now){
  if(frames!=null||loading||now<retryAt)return;
  loading=true;final int token=generation;
  new Thread(()->{
   Bitmap atlas=null;Bitmap[] result=null;
   try(InputStream in=host.getContext().getAssets().open(ASSET)){
    BitmapFactory.Options options=new BitmapFactory.Options();options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    atlas=BitmapFactory.decodeStream(in,null,options);
    if(atlas==null||atlas.getWidth()%4!=0||atlas.getHeight()%2!=0)
     throw new IllegalStateException("Invalid seven-keyframe seated atlas");
    int width=atlas.getWidth()/4,height=atlas.getHeight()/2;
    result=new Bitmap[7];
    for(int i=0;i<result.length;i++)result[i]=Bitmap.createBitmap(atlas,(i%4)*width,(i/4)*height,width,height);
      }catch(Exception|OutOfMemoryError problem){
    recycle(result);result=null;android.util.Log.w("LokiSeatedLeg","Seated leg artwork unavailable",problem);
   }finally{if(atlas!=null&&!atlas.isRecycled())atlas.recycle();}
   final Bitmap[] decoded=result;
   if(!host.post(()->{
    if(token!=generation){recycle(decoded);return;}
    loading=false;frames=decoded;
    retryAt=decoded==null?android.os.SystemClock.uptimeMillis()+60000:0;
    host.invalidate();
   }))recycle(decoded);
  },"LokiSeatedLegDecode").start();
 }

 boolean draw(Canvas canvas,int width,int height,long now,boolean animate,boolean speaking,
              boolean natural,boolean held,boolean quiet,WarmPresencePolicy warmth){
  boolean blocked=quiet||!animate||CharacterRenderBudget.quality>=2;
  if((natural||timeline.waiting()||timeline.active()||played)&&!blocked&&!held&&!speaking)requestFrames(now);
  // Missing artwork is a pause, never proof that a queued gesture has played.
  timeline.update(now,true,blocked||frames==null,held,speaking,natural);
  if(timeline.active())played=true;
  if(frames==null||!played)return false;
  heldFrame=held||quiet;
  ankleAllowed=animate&&natural&&!blocked&&!held&&!speaking;
  ankleAge=ankleMotion.update(now,ankleAllowed);
  float phase=timeline.frame()+(timeline.nextFrame()-timeline.frame())*timeline.blend();
  int current=0;while(current<6&&phase>=SeatedLegWardrobe.PHASES[current+1])current++;
  int next=Math.min(6,current+1);
  float blend=current==next?0:(phase-SeatedLegWardrobe.PHASES[current])/(SeatedLegWardrobe.PHASES[next]-SeatedLegWardrobe.PHASES[current]);
  float[] box=ApprovedQuietPosePolicy.seatedFit(width,height,frames[current].getWidth(),frames[current].getHeight(),FACE,4,538);
  canvas.drawColor(Color.BLACK);
  float opacityBlend=Math.max(0,Math.min(1,(blend-.85f)/.15f));
  opacityBlend=opacityBlend*opacityBlend*(3-2*opacityBlend);
  tweenTarget=next;tweenAmount=blend;
  image(canvas,frames[current],current,box,now,Math.round(255*(1-opacityBlend)),animate,speaking,warmth,0);
  if(opacityBlend>0&&next!=current){tweenTarget=current;tweenAmount=1-blend;image(canvas,frames[next],next,box,now,Math.round(255*opacityBlend),animate,speaking,warmth,1);}
  return true;
 }

 private void image(Canvas canvas,Bitmap bitmap,int index,float[] box,long now,int alpha,
                    boolean animate,boolean speaking,WarmPresencePolicy warmth,int layer){
  if(alpha<=0)return;
  // Add the second weighted image on black: two ordinary translucent layers
  // would multiply the first weight again and dim a 50/50 face to 75%.
  int saved;
  if(layer==1){additiveLayer.setAlpha(alpha);saved=canvas.saveLayer(0,0,canvas.getWidth(),canvas.getHeight(),additiveLayer);}
  else saved=canvas.saveLayerAlpha(0,0,canvas.getWidth(),canvas.getHeight(),alpha);
  canvas.translate(box[0],box[1]);canvas.scale(box[2]/bitmap.getWidth(),box[3]/bitmap.getHeight());
  if(tweenAmount>0||animate&&CharacterRenderBudget.quality<2&&!heldFrame){
   SeatedPresenceMotion.sample(presence,now,animate&&CharacterRenderBudget.quality<2&&!heldFrame?performance.gain()*CharacterMotionTuning.body():0,speaking,performance.expression(now),performance.nod(now));
   SeatedPresenceMotion.fill(mesh,16,32,bitmap.getWidth(),bitmap.getHeight(),(FACE[15]+FACE[16])/FACE[1],presence);
   SeatedLegTween.apply(mesh,16,32,bitmap.getWidth(),bitmap.getHeight(),index,tweenTarget,tweenAmount,SeatedLegWardrobe.JOINTS);
   if(ankleAllowed)SeatedAnkleMotion.apply(mesh,16,32,bitmap.getWidth(),bitmap.getHeight(),SeatedLegWardrobe.JOINTS[index],ankleAge,performance.gain());
   canvas.drawBitmapMesh(bitmap,16,32,mesh,0,null,0,paint);
  }else canvas.drawBitmap(bitmap,0,0,paint);
  if(animate&&CharacterRenderBudget.quality<2){
   FullBodyFaceAnimator face=faces[layer];
   if(faceFrames[layer]!=index){face.clear();face.prepare(bitmap);faceFrames[layer]=index;}
   float warm=warmth==null?0:warmth.smile(now,speaking?2:0,false);
   int expression=performance.expression(now);
   boolean receptive=expression==QuietPosePerformance.CALM||expression==QuietPosePerformance.JOY||expression==QuietPosePerformance.RELIEF;
   face.attention(performance.gazeX(now)*performance.gain()+(warmth==null?0:warmth.gaze(now,speaking?2:0,false)),performance.gazeY()*performance.gain(),speaking?.45f:0);
   int faceSave=canvas.save();if(!heldFrame)canvas.translate(presence[0]*bitmap.getWidth(),presence[1]*bitmap.getHeight());face.drawQuietFeatures(canvas,bitmap,FACE,now,speaking,receptive&&warm>0?QuietPosePerformance.JOY:expression,
           receptive?Math.max(warm,performance.expressionGain(now)):performance.expressionGain(now));canvas.restoreToCount(faceSave);
  }
  canvas.restoreToCount(saved);
 }

 void release(){
  generation++;loading=false;retryAt=0;cancel(android.os.SystemClock.uptimeMillis());
  for(int i=0;i<faces.length;i++){faces[i].clear();faceFrames[i]=-1;}
  recycle(frames);frames=null;
 }
}
