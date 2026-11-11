package com.example.rokidkeyboardbridge;
import android.graphics.*;import android.view.View;import java.io.InputStream;
/** Quiet images plus one lazy eight-frame leg cache. Owns no timer, sensor or network. */
final class ApprovedQuietPoseRenderer {
 private final View host;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final FullBodyFaceAnimator face=new FullBodyFaceAnimator();private final float[] mesh=new float[(16+1)*(32+1)*2];
 private final float[] presence=new float[4];
 private final QuietPosePerformance performance=new QuietPosePerformance();
 private SeatedLegMotionRenderer legArtwork;private boolean natural;private int presentationMode,presentationScene;
 private WarmPresencePolicy warmth;void warmth(WarmPresencePolicy value){warmth=value;}
 private Bitmap current,previous,prefetched;private int selected=-1,previousIndex=-1,prefetchedIndex=-1,wanted=-1,generation;private boolean loading,manual,held;private long changedAt,retryAt,prefetchRetryAt,loadAt;private int retryIndex=-1;
 ApprovedQuietPoseRenderer(View host){this.host=host;}
 boolean transitioning(long now){return previous!=null&&now-changedAt<1250||performance.active(now)||legArtwork!=null&&legArtwork.moving();}
 void accepted(long now,boolean contact){performance.accepted(now,contact);host.invalidate();}
 void accepted(long now,boolean contact,int point,int direction){performance.accepted(now,contact,point,direction);host.invalidate();}
 void state(int mode,FullBodyPoseCatalog.Kind kind,long now){presentationMode=mode;presentationScene=CharacterChoreography.scene;performance.update(mode,CharacterChoreography.phase,presentationScene,kind,now);}
 void natural(boolean value){natural=value;}
 private SeatedLegMotionRenderer legArtwork(){if(legArtwork==null)legArtwork=new SeatedLegMotionRenderer(host,performance);return legArtwork;}
 boolean requestLegChange(long now){return legArtwork().request(now);}
 boolean requestLegChange(long now,int action){return legArtwork().request(now,action);}
 void cancelLegChange(long now){if(legArtwork!=null)legArtwork.cancel(now);}
 void pauseLegMotion(long now){if(legArtwork!=null)legArtwork.pause(now,natural);}
 boolean legMotionActive(){return legArtwork!=null&&legArtwork.active();}
 boolean legMotionWaiting(){return legArtwork!=null&&legArtwork.waiting();}
 void manual(boolean value){manual=value;}
 void held(boolean value){held=value;}
 int selectedIndex(){return selected;}
 String preparationReason(long now){return loading&&wanted!=selected&&now>=loadAt&&now-loadAt<3000?"画像を準備中・前の表示を保持":legArtwork==null?"":legArtwork.preparationReason();}
 String diagnostics(long now){return "asset="+(ApprovedQuietPosePolicy.valid(selected)?ApprovedQuietPosePolicy.ASSETS[selected]:"none")+" stage="+QuietPosePerformance.transitionStage(now-changedAt)+" loading="+loading+" prefetched="+prefetchedIndex+" retained="+((current==null?0:1)+(previous==null?0:1)+(prefetched==null?0:1))+(legArtwork==null?"":" "+legArtwork.diagnostics());}
 private void commit(Bitmap result,int index,long now){if(previous!=null)previous.recycle();previousIndex=selected;if(current!=null&&!QuietPosePerformance.samePosture(selected,index)){current.recycle();previous=null;}else previous=current;current=result;selected=index;changedAt=now;face.clear();face.prepare(result);host.invalidate();}
 private void prefetch(long now){
  if(selected<3||selected==7||manual||held||now<prefetchRetryAt||current==null||previous!=null||prefetched!=null||loading||CharacterRenderBudget.quality!=0||!performance.mayChange(now,false))return;
  int next=selected<3?(selected+1)%3:selected==3?4:3;final int token=generation;loading=true;loadAt=now;
  new Thread(()->{Bitmap b=null;try(InputStream in=host.getContext().getAssets().open(ApprovedQuietPosePolicy.ASSETS[next])){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=next==1||next==2?2:1;b=BitmapFactory.decodeStream(in,null,o);}catch(Exception|OutOfMemoryError ignored){}final Bitmap result=b;host.post(()->{if(token!=generation){if(result!=null)result.recycle();return;}loading=false;if(result==null){prefetchRetryAt=android.os.SystemClock.uptimeMillis()+60000;return;}if(wanted==selected&&previous==null){prefetched=result;prefetchedIndex=next;}else if(wanted==next)commit(result,next,android.os.SystemClock.uptimeMillis());else result.recycle();host.invalidate();});},"ApprovedPosePrefetch").start();
 }
 private void request(int index,long now){
  wanted=index;
  if(prefetched!=null&&CharacterRenderBudget.quality>0){prefetched.recycle();prefetched=null;prefetchedIndex=-1;}
  if(current!=null&&index!=6&&QuietPosePerformance.samePosture(selected,index)&&!performance.mayChange(now,manual))return;
  if(!ApprovedQuietPosePolicy.valid(index)||index==selected||loading||now<retryAt&&retryIndex==index||previous!=null&&now-changedAt<1250&&QuietPosePerformance.samePosture(index,selected))return;
  if(prefetched!=null){Bitmap cached=prefetched;int cachedIndex=prefetchedIndex;prefetched=null;prefetchedIndex=-1;if(cachedIndex==index){commit(cached,index,now);return;}cached.recycle();}
  if(previous!=null){previous.recycle();previous=null;}
  loading=true;loadAt=now;final int token=generation;
  new Thread(()->{Bitmap image=null;try(InputStream in=host.getContext().getAssets().open(ApprovedQuietPosePolicy.ASSETS[index])){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=index==1||index==2?2:1;image=BitmapFactory.decodeStream(in,null,options);
  }catch(Exception|OutOfMemoryError error){android.util.Log.w("ApprovedQuietPose","Cannot decode approved pose",error);}
  final Bitmap result=image;host.post(()->{if(token!=generation){if(result!=null)result.recycle();return;}loading=false;if(index!=wanted){if(result!=null)result.recycle();host.invalidate();return;}if(result==null){retryIndex=index;retryAt=android.os.SystemClock.uptimeMillis()+60000;return;}
   commit(result,index,android.os.SystemClock.uptimeMillis());
  });},"ApprovedQuietPoseDecode").start();
 }
 private static float weight(float x,float y,float cx,float cy,float rx,float ry){float dx=(x-cx)/rx,dy=(y-cy)/ry;float v=Math.max(0,1-dx*dx-dy*dy);return v*v;}
 private void image(Canvas c,Bitmap b,int index,int width,int height,long now,int alpha,boolean animate,boolean speaking,float prepare,float settle){
  float[] box=ApprovedQuietPosePolicy.fit(index,width,height,b.getWidth(),b.getHeight());int saved=c.saveLayerAlpha(0,0,width,height,alpha);c.translate(box[0],box[1]);c.scale(box[2]/b.getWidth(),box[3]/b.getHeight());
  // Very small chest movement, with the head and all frame edges fixed. No body-length changes.
  float[] f=ApprovedQuietPosePolicy.FACE[index];float faceBottom=(f[15]+f[16])/f[1];
  if(animate){int at=0;float pulse=(float)Math.sin(now*2*Math.PI/6200),center=Math.min(.55f,faceBottom+.16f),gain=held?0:performance.gain()*CharacterMotionTuning.body();
   for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){float x=col/16f,y=row/32f,band=Math.max(0,1-Math.abs(y-center)/.13f);band*=band;float edge=(float)Math.sin(Math.PI*x);
    float head=weight(x,y,(f[2]+f[8])*.5f/f[0],f[3]/f[1],.14f,.11f),hand=weight(x,y,ApprovedQuietPosePolicy.seatedIndex(index)?.43f:.63f,ApprovedQuietPosePolicy.seatedIndex(index)?.22f:.33f,.13f,.15f);
    if(y<faceBottom+.04f)hand=0; // Additional hand motion never enters the eyes/mouth zone.
    float dx=(x-.5f)*.003f*band*pulse*edge-.0025f*prepare*hand;
    float dy=-.0008f*band*pulse*edge+.003f*prepare*hand+.0015f*settle*band+.0025f*performance.hand(now)*hand+.0015f*performance.shoulder(now)*band;
    mesh[at++]=x*b.getWidth()+b.getWidth()*dx*gain;mesh[at++]=y*b.getHeight()+b.getHeight()*dy*gain+b.getHeight()*.002f*performance.nod(now)*head;
   }
   if(index>=0&&index<3){
    SeatedPresenceMotion.sample(presence,now,gain,speaking,performance.expression(now),performance.nod(now));
    SeatedPresenceMotion.fill(mesh,16,32,b.getWidth(),b.getHeight(),faceBottom,presence);
    if(natural)DrapedSeatedMotion.apply(mesh,16,32,b.getWidth(),b.getHeight(),index,now,gain);
   }
   c.drawBitmapMesh(b,16,32,mesh,0,null,0,paint);
  }else c.drawBitmap(b,0,0,paint);
  if(animate&&index==selected&&ApprovedQuietPosePolicy.awake(index)){float warm=warmth==null?0:warmth.smile(now,speaking?2:0,false);int expression=performance.expression(now);boolean receptive=expression==QuietPosePerformance.CALM||expression==QuietPosePerformance.JOY||expression==QuietPosePerformance.RELIEF;face.attention(performance.gazeX(now)*performance.gain()+(warmth==null?0:warmth.gaze(now,speaking?2:0,false)),performance.gazeY()*performance.gain(),speaking?.45f:0);int faceSave=c.save();if(index<3)c.translate(presence[0]*b.getWidth(),presence[1]*b.getHeight());face.drawQuietFeatures(c,b,f,now,speaking,receptive&&warm>0?QuietPosePerformance.JOY:expression,receptive?Math.max(warm,performance.expressionGain(now)):performance.expressionGain(now));c.restoreToCount(faceSave);}
  c.restoreToCount(saved);
 }
 boolean draw(Canvas c,int width,int height,int index,long now,boolean animate,boolean speaking){
  if(held&&QuietPosePerformance.samePosture(selected,index))index=selected;
  request(index,now);if(current==null)return false;
  // Keep the previous confirmed picture while decoding the next one of the SAME posture only.
  if(ApprovedQuietPosePolicy.seatedIndex(selected)!=ApprovedQuietPosePolicy.seatedIndex(index))return false;
  boolean ordinarySeated=selected>=0&&selected<3&&index>=0&&index<3&&!manual;
  if(ordinarySeated&&(natural||legArtwork!=null)){
   if(legArtwork().draw(c,width,height,now,animate,speaking||presentationMode==2,natural,held,
           presentationMode==1||presentationScene==1,warmth))return true;
  }else if(legArtwork!=null)legArtwork.cancel(now);
  c.drawColor(Color.BLACK);long age=now-changedAt;float blend=animate?QuietPosePerformance.transitionBlend(age):1;
  if(previous!=null&&blend<1){float prep=age<240?QuietPosePerformance.prepare(age):1-blend;image(c,previous,previousIndex,width,height,now,255,animate,false,prep,0);if(blend>0)image(c,current,selected,width,height,now,Math.round(255*blend),animate,speaking,0,0);}
  else{if(previous!=null&&(!animate||age>=1250)){previous.recycle();previous=null;}image(c,current,selected,width,height,now,255,animate,speaking,0,QuietPosePerformance.settle(age));}
  prefetch(now);return true;
 }
 void release(){generation++;loading=false;selected=previousIndex=prefetchedIndex=wanted=-1;retryAt=prefetchRetryAt=0;retryIndex=-1;if(legArtwork!=null){legArtwork.release();legArtwork=null;}face.clear();if(current!=null)current.recycle();if(previous!=null)previous.recycle();if(prefetched!=null)prefetched.recycle();current=previous=prefetched=null;}
}
