package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.*;import android.view.View;import java.io.InputStream;
/** Bounded background decode, cached keyframes and shared floor/head anchors. */
final class FullBodyGestureRenderer {
 private final Context context;private final View host;private final Bitmap[][] frames=new Bitmap[44][];private final float[][] anchors=new float[44][];private final boolean[] loading=new boolean[44];private int generation;private final long[] retryAfter=new long[44];
 private final Rect[][] headBounds=new Rect[44][];private final Paint headPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private boolean windedHead;private final RectF headDestination=new RectF();
 private final Bitmap[][] lyingPatches=new Bitmap[2][];private final Rect[][] lyingRects=new Rect[2][];private final float[] faceMesh=new float[13*17*2];private int lyingExpression=-1,previousFrame=-1;
 void lyingExpression(int value){lyingExpression=value;}boolean transitioning(long now){return previousFrame>=0&&now>=frameChanged&&now-frameChanged<240;}
 private final long[] used=new long[44];
 int cachedFamilies(){int n=0;for(Bitmap[] f:frames)if(f!=null)n++;return n;}
 private void trim(int current){while(cachedFamilies()>4){int oldest=-1;for(int i=0;i<frames.length;i++)if(frames[i]!=null&&i!=current&&i!=cacheClip(Math.max(0,lastClip))&&i!=32&&i!=33&&(oldest<0||used[i]<used[oldest]))oldest=i;if(oldest<0)return;frames[oldest]=null;anchors[oldest]=null;headBounds[oldest]=null;}}
 private int preview=-1;private long previewAt,previewUntil;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private final float[] approachMesh=new float[17*33*2];
 FullBodyGestureRenderer(View view){host=view;context=view.getContext();}
 void preview(boolean seated,boolean ear,long now){preview(seated,ear?1:0,now);}
 void preview(boolean seated,int family,long now){if(family<0||family>12)return;preview=FullBodyEverydayGesture.clip(seated,family);previewAt=now;previewUntil=now+4600;request(preview);}
 void clearPreview(){preview=-1;previewUntil=0;}
 boolean active(long now){return preview>=0&&now>=previewAt&&now<previewUntil;}
 private static int cacheClip(int clip){return clip>=8&&clip<22?4+clip%2:clip;}
 static int sourceFrame(int clip,int requested){return clip==0&&requested==3?1:requested;}
 static String assetForClip(int requested){int clip=cacheClip(requested);if(StandingGestureAtlas.supports(clip))return StandingGestureAtlas.asset(clip);if(SeatedGestureAtlas.supports(clip))return SeatedGestureAtlas.asset(clip);return clip>=40?"loki_relaxed_emotions_v670.png":clip>=36?"loki_relaxed_expressions_v670.png":clip>=34?"loki_poised_gestures_v670.png":clip>=32?"loki_relaxed_poses_v670.png":clip==31?"loki_body_leg_change_v669.png":clip>=30?"loki_body_seated_kiss_v667.png":clip>=28?"loki_body_overhead_v667.png":clip>=26?"loki_body_actions_v667.png":clip==1?"loki_body_greeting_seated_v667.png":clip==0?"loki_body_greeting_standing_v667.png":clip>=24?"loki_notebook_sequence_v667.png":clip>=22?"loki_contact_states_v669.png":clip==5?"loki_body_affection_seated_v669.png":new String[]{"loki_body_greeting_v654.png","loki_body_ear_v672.png","loki_body_affection_v669.png","loki_body_invite_v667.png"}[clip/2];}
 boolean ready(int clip){return clip>=0&&clip<44&&frames[cacheClip(clip)]!=null;}
 private static float[] measure(Bitmap frame){
  int w=frame.getWidth(),h=frame.getHeight(),top=h,bottom=0,left=w,right=0;int[] pixels=new int[w*h];frame.getPixels(pixels,0,w,0,0,w,h);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]&255)>64){top=Math.min(top,y);bottom=Math.max(bottom,y);left=Math.min(left,x);right=Math.max(right,x);}
  // Faint antialiased shoe edges still contribute visible pixels on Canvas.
  // Anchor the complete silhouette, not only the brighter interior of the shoe.
  outer:for(int y=h-1;y>=top;y--)for(int x=0;x<w;x++)if((pixels[y*w+x]&0xffffff)!=0){bottom=y;break outer;}
  if(bottom<=top)throw new IllegalStateException("Empty body keyframe");
  long sum=0,count=0;int headEnd=Math.min(h,top+Math.max(8,(bottom-top)/7));
  for(int y=top;y<headEnd;y++)for(int x=left;x<=right;x++)if((pixels[y*w+x]&255)>64){sum+=x;count++;}
  return new float[]{count>0?sum/(float)count:w*.5f,bottom,top,right-left+1,left,right};
 }
 private static Rect headBox(Bitmap b,float[] geometry,int offset){
  int top=Math.max(0,(int)geometry[offset+2]-2),span=Math.max(16,(int)(geometry[offset+1]-geometry[offset+2]));int center=Math.round(geometry[offset]),radius=Math.max(12,span*14/100),left=Math.max(0,center-radius),right=Math.min(b.getWidth()-1,center+radius);int[] px=new int[b.getWidth()*b.getHeight()];b.getPixels(px,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());int neck=Math.min(b.getHeight()-1,top+span*20/100),best=Integer.MAX_VALUE;
  for(int y=Math.min(b.getHeight()-1,top+span*12/100);y<=Math.min(b.getHeight()-1,top+span*25/100);y++){int lo=right,hi=left;for(int x=left;x<=right;x++)if((px[y*b.getWidth()+x]&255)>24){lo=Math.min(lo,x);hi=Math.max(hi,x);}int width=hi-lo;if(width>5&&width<best){best=width;neck=y;}}
  int lo=right,hi=left;for(int y=top;y<=neck;y++)for(int x=left;x<=right;x++)if((px[y*b.getWidth()+x]&255)>24){lo=Math.min(lo,x);hi=Math.max(hi,x);}return new Rect(Math.max(0,lo-2),top,Math.min(b.getWidth(),hi+3),Math.min(b.getHeight(),neck+3));
 }
 private boolean drawExpression(Canvas canvas,int width,int height,int clip,int expression,long elapsed,long now,int sceneType){
  int pose=clip>=40?clip-40:clip-36,base=pose<2?32:33,bodyFrame=pose%2;request(base);if(!ready(base))return false;
  int savedPose=contactPose;boolean oldWinded=windedHead;contactPose=bodyFrame;windedHead=clip>=40&&expression==2;boolean drawn;try{drawn=draw(canvas,width,height,base,elapsed,now,sceneType);}finally{contactPose=savedPose;windedHead=oldWinded;}if(!drawn)return false;
  Bitmap body=frames[base][bodyFrame],face=frames[clip][expression];float[] a=anchors[base];float min=0,max=0;for(int i=0;i<frames[base].length;i++){min=Math.min(min,a[i*6+4]-a[i*6]);max=Math.max(max,a[i*6+5]-a[i*6]);}float span=Math.max(1,max-min),scale=CharacterViewport.scale(a[frames[base].length*6],width,height),head=(width-span*scale)*.5f-min*scale,left=head-a[bodyFrame*6]*scale,top=CharacterViewport.top(height*.985f-a[bodyFrame*6+1]*scale,Math.max(0,a[bodyFrame*6+2]-3),scale,height);
  Rect target=headBounds[base][bodyFrame],source=headBounds[clip][expression];float x=left+target.left*scale,y=top+target.top*scale,w=target.width()*scale,h=target.height()*scale;headPaint.setColor(Color.BLACK);headPaint.setAlpha(255);canvas.drawRect(x,y,x+w,y+h,headPaint);float fit=Math.min(w/source.width(),h/source.height()),fw=source.width()*fit,fh=source.height()*fit;headDestination.set(x+(w-fw)*.5f,y+h-fh,x+(w+fw)*.5f,y+h);int hs=canvas.save();if(sceneType==ShyFidgetMotion.TYPE)canvas.rotate(ShyFidgetMotion.headAngle(elapsed),x+w*.5f,y+h);canvas.drawBitmap(face,source,headDestination,headPaint);canvas.restoreToCount(hs);return true;
 }
 void request(int requested){
  if(requested<0||requested>43)return;final int clip=cacheClip(requested);used[clip]=android.os.SystemClock.uptimeMillis();
  if(clip<0||clip>43||frames[clip]!=null||loading[clip]||android.os.SystemClock.uptimeMillis()<retryAfter[clip])return;
  if(clip>=36)request((clip>=40?clip-40:clip-36)<2?32:33);loading[clip]=true;final int token=generation;
  new Thread(()->{Bitmap atlas=null;Bitmap[] result=null;float[] info=null;Rect[] headResult=null;
   try(InputStream input=context.getAssets().open(assetForClip(clip))){
    BitmapFactory.Options options=new BitmapFactory.Options();options.inMutable=true;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    atlas=BitmapFactory.decodeStream(input,null,options);if(atlas==null)throw new IllegalStateException("Missing body gesture atlas");
    // Nearly-black generation grain is not part of the silhouette on black glass.
    // Normalize it in the render cache so faint background pixels cannot move the floor anchor.
    int[] clean=new int[atlas.getWidth()*atlas.getHeight()];atlas.getPixels(clean,0,atlas.getWidth(),0,0,atlas.getWidth(),atlas.getHeight());
    for(int i=0;i<clean.length;i++)if(Math.max((clean[i]>>16)&255,Math.max((clean[i]>>8)&255,clean[i]&255))<=12)clean[i]=Color.BLACK;
    atlas.setPixels(clean,0,atlas.getWidth(),0,0,atlas.getWidth(),atlas.getHeight());
    int split=clip>=36?atlas.getHeight()/4:FullBodyGestureRegions.split(atlas);int effective=clip>=22?clip:clip>=8?4+(clip%2):clip;
    int w=atlas.getWidth()/(effective>=40?3:effective==32||effective==33?2:effective>=6&&effective<22?3:4),rowTop=effective>=36?(effective-(effective>=40?40:36))*split:effective==30?split:effective%2==0?0:split,h=effective>=36?split:effective==30?atlas.getHeight()-split:effective%2==0?split:atlas.getHeight()-split,count=effective>=40?3:effective==32||effective==33?2:effective>=22||effective==2||effective==3?4:effective>=2?3:4;
    if(clip==7)count=4;result=new Bitmap[count];info=new float[count*6+1];
    // Use the standing reference height for both rows, preserving person scale when seated.
    int[] ref=SeatedGestureAtlas.supports(clip)?SeatedGestureAtlas.bounds(clip,0,atlas.getWidth(),atlas.getHeight()):StandingGestureAtlas.supports(clip)?StandingGestureAtlas.bounds(clip,0,atlas.getWidth(),atlas.getHeight()):clip>=32?PoisedAtlasLayout.bounds(PoisedAtlasLayout.reference(clip),0,atlas.getWidth(),atlas.getHeight()):new int[]{0,0,w,split};Bitmap standard=Bitmap.createBitmap(atlas,ref[0],ref[1],ref[2]-ref[0],ref[3]-ref[1]);float[] standing=measure(standard);info[count*6]=(standing[1]-standing[2])/(SeatedGestureAtlas.supports(clip)||clip==32||clip==33||clip>=36?.76f:1f);standard.recycle();
    for(int i=0;i<count;i++){int[] region=SeatedGestureAtlas.supports(clip)?SeatedGestureAtlas.bounds(clip,i,atlas.getWidth(),atlas.getHeight()):StandingGestureAtlas.supports(clip)?StandingGestureAtlas.bounds(clip,i,atlas.getWidth(),atlas.getHeight()):clip>=32?PoisedAtlasLayout.bounds(clip,i,atlas.getWidth(),atlas.getHeight()):new int[]{i*w,rowTop,(i+1)*w,rowTop+h};result[i]=Bitmap.createBitmap(atlas,region[0],region[1],region[2]-region[0],region[3]-region[1]);float[] f=measure(result[i]);if(StandingGestureAtlas.supports(clip))f[0]=StandingGestureAtlas.headCenter(clip,i,atlas.getWidth());System.arraycopy(f,0,info,i*6,6);}
    if(clip>=28){headResult=new Rect[result.length];for(int i=0;i<result.length;i++)headResult[i]=headBox(result[i],info,i*6);}
   }catch(Exception|OutOfMemoryError error){if(result!=null)for(Bitmap b:result)if(b!=null)b.recycle();result=null;android.util.Log.w("LokiBodyGesture","Gesture decode failed",error);}finally{if(atlas!=null)atlas.recycle();}
   try{if(result!=null&&(clip==28||clip==29)){Bitmap[] patches=new Bitmap[result.length];Rect[] rects=new Rect[result.length];for(int i=0;i<result.length;i++){Bitmap b=result[i];int mx=LyingFaceMotion.x(clip,i,b.getWidth()),my=LyingFaceMotion.y(clip,i,b.getHeight());Rect rect=new Rect(Math.max(0,mx-25),Math.max(0,my-45),Math.min(b.getWidth(),mx+28),Math.min(b.getHeight(),my+8));rects[i]=rect;patches[i]=Bitmap.createBitmap(b,rect.left,rect.top,rect.width(),rect.height());}final Bitmap[] prepared=patches;final Rect[] places=rects;host.post(()->{if(token==generation){lyingPatches[clip-28]=prepared;lyingRects[clip-28]=places;}else for(Bitmap b:prepared)b.recycle();});}
   }catch(OutOfMemoryError optionalFace){android.util.Log.w("LokiBodyGesture","Optional lying face patch unavailable");}
   final Bitmap[] decoded=result;final float[] geometry=info;final Rect[] computedHeads=headResult;
   host.post(()->{if(token!=generation){if(decoded!=null)for(Bitmap b:decoded)b.recycle();return;}loading[clip]=false;retryAfter[clip]=decoded==null?android.os.SystemClock.uptimeMillis()+60000:0;frames[clip]=decoded;anchors[clip]=geometry;headBounds[clip]=decoded==null?null:computedHeads;trim(clip);host.invalidate();});
  },"LokiBodyGestureDecode").start();
 }
 private int contactPose;private float contactPulse;private int lastClip=-1,lastFrame=-1;private long frameChanged;
 void contactPose(int pose,float pulse){contactPose=Math.max(0,Math.min(3,pose));contactPulse=pulse;}
 private CharacterRapportPolicy rapport;private int rapportMode;
 void rapport(CharacterRapportPolicy value,int mode){rapport=value;rapportMode=mode;if(value!=null)contactPulse=0;}
 boolean draw(Canvas canvas,int width,int height,int automatic,long local,long now){return draw(canvas,width,height,automatic,local,now,-1);}
 boolean draw(Canvas canvas,int width,int height,int automatic,long local,long now,int sceneType){
  int clip=active(now)?preview:automatic;if(clip<0)return false;request(clip);if(!ready(clip))return false;
  long elapsed=CharacterMotionTuning.time(active(now)?now-previewAt:local);int frame=clip>=26?EmbodiedScenePolicy.frame(clip,elapsed,contactPose):clip>=24?Math.min(3,(int)(Math.max(0,elapsed)/1300)):clip>=22?contactPose:FullBodyEverydayGesture.frame(clip,elapsed);frame=sourceFrame(clip,frame);if(clip>=36)return drawExpression(canvas,width,height,clip,frame,elapsed,now,sceneType);int cached=cacheClip(clip);Bitmap bitmap=frames[cached][frame];float[] a=anchors[cached];
  float minRelative=0,maxRelative=0;
  for(int i=0;i<frames[cached].length;i++){minRelative=Math.min(minRelative,a[i*6+4]-a[i*6]);maxRelative=Math.max(maxRelative,a[i*6+5]-a[i*6]);}
  float spanWidth=Math.max(1,maxRelative-minRelative);
  float scale=CharacterViewport.scale(a[frames[cached].length*6],width,height);
  float headScreen=(width-spanWidth*scale)*.5f-minRelative*scale;
  float left=headScreen-a[frame*6]*scale,top=CharacterViewport.top(height*.985f-a[frame*6+1]*scale,Math.max(0,a[frame*6+2]-3),scale,height);
  if(lastClip!=clip||lastFrame!=frame){previousFrame=lastClip==clip?lastFrame:-1;lastClip=clip;lastFrame=frame;frameChanged=now;}
  float entry=CharacterGestureLife.smooth(Math.max(0,now-frameChanged)/240f);if(transitioning(now)&&previousFrame<frames[cached].length){Bitmap prior=frames[cached][previousFrame];paint.setAlpha(Math.round(255*(1-entry)));canvas.save();canvas.translate(headScreen-a[previousFrame*6]*scale,CharacterViewport.top(height*.985f-a[previousFrame*6+1]*scale,Math.max(0,a[previousFrame*6+2]-3),scale,height));canvas.scale(scale,scale);canvas.drawBitmap(prior,0,0,paint);canvas.restore();}else previousFrame=-1;paint.setAlpha(Math.round(255*(previousFrame>=0?entry:1)));
  int save=canvas.save();canvas.translate(0,height*.002f*(1-entry));canvas.translate(left,top);canvas.scale(scale,scale);{
   boolean rigidScene=clip==27||clip==30;int type=rigidScene&&sceneType>=0&&sceneType<=6?sceneType:clip>=8&&clip<22?(clip-8)/2:-1;float sway=KissGesturePolicy.sway(type,elapsed);float pulse=type<0?0:KissGesturePolicy.envelope(type,elapsed),gain=KissGesturePolicy.approach(type)*pulse*CharacterMotionTuning.scale()*CharacterMotionTuning.approach();int n=0;
   float span=Math.max(1,a[frame*6+1]-a[frame*6+2]);
   float proximity=Math.max(0,Math.min(.035f,gain+(rigidScene?.006f*sway*CharacterMotionTuning.scale()*CharacterMotionTuning.approach():0)));canvas.scale(1+proximity,1+proximity,a[frame*6],a[frame*6+1]);
   for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){
    float x=bitmap.getWidth()*col/16f,y=bitmap.getHeight()*row/32f;
    float upper=rigidScene?0:Math.max(0,1-(y-a[frame*6+2])/span/.5f);upper*=upper;
    approachMesh[n++]=x+bitmap.getWidth()*.006f*pulse*upper*(KissGesturePolicy.base(type)==3?-1:1)+bitmap.getWidth()*.018f*sway*upper;
    approachMesh[n++]=y+span*(.008f*pulse+.006f*sway)*upper;
   }
   if(clip>=22&&clip<24){float breath=(float)Math.sin(now/1400.0);for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){float ratio=row/32f;if(ratio<.24f||ratio>.58f)continue;float weight=(float)Math.pow(Math.sin(Math.PI*(ratio-.24f)/.34f),2);int ix=(row*17+col)*2;approachMesh[ix]+=bitmap.getWidth()*.009f*contactPulse*weight;approachMesh[ix+1]-=bitmap.getHeight()*.0018f*breath*weight;}}
   if(rapport!=null&&(clip<22||clip>=24&&clip<26))rapport.deform(approachMesh,16,32,bitmap.getWidth(),bitmap.getHeight(),now,rapportMode,false,.24f);
   if(clip<22||clip>=24)GestureMicroMotion.apply(approachMesh,16,32,bitmap.getWidth(),bitmap.getHeight(),a[frame*6],a[frame*6+2],a[frame*6+1],now,CharacterMotionTuning.scale()*CharacterMotionTuning.body());
   if(clip>=32){float breath=(float)Math.sin(now/1700.0),settle=(float)Math.sin(Math.min(1,Math.max(0,elapsed)/2200f)*Math.PI/2);for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){float ratio=row/32f,weight=Math.max(0,1-Math.abs(ratio-.38f)/.28f);int ix=(row*17+col)*2;approachMesh[ix+1]-=bitmap.getHeight()*.0018f*breath*weight;approachMesh[ix]+=bitmap.getWidth()*.003f*settle*weight;if(windedHead){Rect hb=headBounds[cached][frame];float ty=(bitmap.getHeight()*ratio-hb.bottom)/Math.max(1f,hb.height()),tx=(bitmap.getWidth()*col/16f-(hb.centerX()+hb.width()*.25f))/Math.max(1f,hb.width());float chest=Math.max(0,1-Math.abs(ty-.5f)*2)*Math.max(0,1-Math.abs(tx));approachMesh[ix+1]-=hb.height()*.012f*(float)Math.sin(now/550.0)*chest;}}}
   if(sceneType==ShyFidgetMotion.TYPE&&clip>=28){Rect hb=headBounds[cached][frame];ShyFidgetMotion.apply(approachMesh,16,32,bitmap.getWidth(),bitmap.getHeight(),hb.centerX(),hb.bottom,hb.width(),hb.height(),elapsed);}
   if(StandingGestureAtlas.supports(cached)||SeatedGestureAtlas.supports(cached))StandingGestureAtlas.protectHead(approachMesh,16,32,bitmap.getWidth(),bitmap.getHeight(),a[frame*6+2],a[frame*6+1]);
   if(CharacterRenderBudget.quality<2)canvas.drawBitmapMesh(bitmap,16,32,approachMesh,0,null,0,paint);else canvas.drawBitmap(bitmap,0,0,paint);
   if((clip==28||clip==29)&&lyingExpression>0&&lyingPatches[clip-28]!=null){Rect rect=lyingRects[clip-28][frame];Bitmap patch=lyingPatches[clip-28][frame];LyingFaceMotion.apply(faceMesh,12,16,patch.getWidth(),patch.getHeight(),LyingFaceMotion.x(clip,frame,bitmap.getWidth())-rect.left,LyingFaceMotion.y(clip,frame,bitmap.getHeight())-rect.top,lyingExpression,elapsed);canvas.translate(rect.left,rect.top);canvas.drawBitmapMesh(patch,12,16,faceMesh,0,null,0,paint);}

  }canvas.restoreToCount(save);return true;
 }
 void release(){for(int i=0;i<2;i++){if(lyingPatches[i]!=null)for(Bitmap b:lyingPatches[i])if(b!=null&&!b.isRecycled())b.recycle();lyingPatches[i]=null;lyingRects[i]=null;}generation++;preview=-1;for(int c=0;c<44;c++){loading[c]=false;retryAfter[c]=0;if(frames[c]!=null)for(Bitmap b:frames[c])if(b!=null&&!b.isRecycled())b.recycle();frames[c]=null;anchors[c]=null;headBounds[c]=null;}}
}
