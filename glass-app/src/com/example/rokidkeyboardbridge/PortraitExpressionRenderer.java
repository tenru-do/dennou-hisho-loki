package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.*;import java.io.InputStream;
/** New registered bust artwork, animated locally; no sensors, network or view timers. */
final class PortraitExpressionRenderer {
 private final PortraitAttentionMotion attention=new PortraitAttentionMotion();
 final CharacterRapportPolicy rapport=new CharacterRapportPolicy();
 private VirtualContactPolicy.Frame contact=VirtualContactPolicy.Frame.EMPTY;private long contactTime;
 void virtualContact(VirtualContactPolicy.Frame value,long now){contact=value;contactTime=now;}
 boolean virtualMoving(){return VirtualContactPresentation.pulse(contact,System.currentTimeMillis())>0;}
 private Bitmap sequenceAtlas;
 private Bitmap atlas,frame,previousFrame;private long frameChangedAt;private int frameCell=-1,previous=-1,ack;
 private long cueAt=-1,changedAt,previewUntil;private int cue,previewCell=-1;
 private static final int[] ACK={5,2,0,6};
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final RectF transitionRect=new RectF();
 private final Rect src=new Rect();private final RectF dst=new RectF();private final float[] mesh=new float[25*25*2];
 PortraitExpressionRenderer(Context context){
  try(InputStream input=context.getAssets().open("loki_identity_portraits_v686.png")){
   Bitmap raw=BitmapFactory.decodeStream(input);if(raw==null)throw new IllegalStateException("missing portrait atlas");
   atlas=FullBodyPortraitInk.render(raw);raw.recycle();
  }catch(Exception error){android.util.Log.e("LokiPortraitLife","New portrait atlas unavailable",error);}
  sequenceAtlas=atlas; // One shared identity, decoded once for both expression libraries.
 }
 void cancelPreview(){previewCell=-1;previewUntil=0;}
 void clearContactCues(){rapport.clear();cueAt=-1;previewUntil=0;previewCell=-1;}
 boolean transitioning(long now){return previousFrame!=null&&now>=frameChangedAt&&now-frameChangedAt<240;}
 boolean ready(){return atlas!=null&&!atlas.isRecycled();}
 Bitmap[] retained(){return new Bitmap[]{atlas,frame,sequenceAtlas,previousFrame};}
 void preview(int cell,long now){if(cell>=0&&cell<24){previewCell=cell;previewUntil=now+6000;}}
 void react(long now,boolean touching,int direction){rapport.life.touch(now,direction,CharacterTouchTempo.duration(now),touching);rapport.touch(now);cueAt=now;cue=touching?7:ACK[ack];ack=(ack+1)%ACK.length;if(direction!=0&&!touching)cue=direction%2==0?2:5;}
 void release(){rapport.clear();if(previousFrame!=null&&!previousFrame.isRecycled())previousFrame.recycle();previousFrame=null;if(frame!=null&&!frame.isRecycled())frame.recycle();if(atlas!=null&&!atlas.isRecycled())atlas.recycle();if(sequenceAtlas!=null&&sequenceAtlas!=atlas&&!sequenceAtlas.isRecycled())sequenceAtlas.recycle();frame=atlas=sequenceAtlas=null;frameCell=-1;}
 private static float weight(float x,float y,float cx,float cy,float rx,float ry){float dx=(x-cx)/rx,dy=(y-cy)/ry;return Math.max(0,1-dx*dx-dy*dy);}
 boolean draw(Canvas canvas,int width,int height,int expression,int mode,boolean speaking,boolean quiet,long now){
  rapport.life.performance.mode(now,mode);attention.mode(mode,now);
  int cell=PortraitExpressionPolicy.cell(expression,mode,now,cueAt,cue,quiet);boolean asking=CharacterChoreography.clarifying(now)&&!quiet&&!speaking;if(asking)cell=1;
  if(cell<0&&expression==8&&contact.visible()&&!quiet)cell=7;
  if(cell<0&&expression==8&&rapport.active(now)&&!quiet)cell=7;
  if(!ready()||cell<0||width<=0||height<=0)return false;
  if(!quiet&&mode!=1&&rapport.active(now)&&now>=previewUntil)cell=rapport.cell(now);
  int semanticCell=rapport.life.face(now,mode,quiet);if(!quiet&&PortraitExpressionPolicy.supports(expression)&&!rapport.active(now)&&semanticCell>=0&&now>=previewUntil)cell=semanticCell;
  if(!quiet&&PortraitExpressionPolicy.supports(expression)&&!rapport.life.moving(now)&&!contact.visible()&&rapport.life.invitationFor(now,mode,quiet)>.1f&&now>=previewUntil)cell=7;
  if(!quiet&&previewCell>=0&&now>=previewUntil-6000&&now<previewUntil)cell=previewCell;
  if(!quiet&&VirtualContactPresentation.eligible(expression)&&contact.visible()&&now>=previewUntil)cell=VirtualContactPresentation.face(contact);
  int sequenceCell=-1;
  boolean sequenceAllowed=!quiet&&sequenceAtlas!=null&&(PortraitExpressionPolicy.supports(expression)||expression==8&&contact.visible());
  if(sequenceAllowed){
   sequenceCell=rapport.life.performance.portraitCell(now,rapport.life.kind(),rapport.life.age(now),mode);
   if(contact.visible()){long age=Math.max(0,contactTime-contact.at);sequenceCell=contact.kind==VirtualContactPolicy.OFFER?4:contact.kind==VirtualContactPolicy.HAND?5:7;}
   if(previewCell>=8&&now<previewUntil)sequenceCell=previewCell-8;
   else if(previewCell>=0&&now<previewUntil)sequenceCell=-1;
  }
  if(asking)sequenceCell=15;
  int key=sequenceCell>=0?32+sequenceCell:cell;
  if(frameCell!=key||frame==null){if(previousFrame!=null&&!previousFrame.isRecycled())previousFrame.recycle();previousFrame=frame;frameChangedAt=now;Bitmap selected=sequenceCell>=0?sequenceAtlas:atlas;int selectedCell=sequenceCell>=0?sequenceCell:cell;int w=selected.getWidth()/4,h=selected.getHeight()/6;if(sequenceCell>=0)selectedCell+=8;
   src.set(selectedCell%4*w,selectedCell/4*h,(selectedCell%4+1)*w,(selectedCell/4+1)*h);frame=Bitmap.createBitmap(selected,src.left,src.top,w,h);frameCell=key;}
  if(previous!=cell){previous=cell;changedAt=now;}
  float h=Math.min(height*.94f,width*.94f*frame.getHeight()/(float)frame.getWidth()),w=h*frame.getWidth()/frame.getHeight();
  float pulse=quiet?0:PortraitLifeMotion.pulse(now,mode==1?4300:6200,600,2400);
  float contactPulse=quiet?0:VirtualContactPresentation.pulse(contact,contactTime)*(mode==2?.25f:1f);
  float gazeY=quiet?0:rapport.life.performance.buttonVertical(now)*80;
  float rapportLean=rapport.lean(now,mode,quiet)+(contact.direction==2?-1:1)*.7f*contactPulse,rapportGaze=rapport.gaze(now,mode,quiet);
  float invitation=contact.visible()?0:rapport.life.invitationFor(now,mode,quiet);
  rapportLean+=invitation*.35f+rapport.life.lean(now,mode,quiet)*35;rapportGaze+=rapport.life.gaze(now,mode,quiet)*80;rapportGaze+=attention.gaze(now,quiet)*80;if(!quiet)rapportGaze+=CharacterChoreography.lead(rapport.life.age(now))*.25f;rapportGaze*=CharacterMotionTuning.eyes();rapportLean*=CharacterMotionTuning.body();
  pulse*=CharacterMotionTuning.scale();
  float nod=h*((mode==1?.018f:.010f)*pulse*CharacterMotionTuning.face()+rapport.life.nod(now,mode,quiet)+attention.nod(now,quiet)),breath=quiet?0:h*.006f*(rapport.life.performance.breath(now)-.5f)*CharacterMotionTuning.body();
  float x=(width-w)*.5f,y=(height-h)*.45f+nod+breath;dst.set(x,y,x+w,y+h);
  float blinkPhase=Math.max(0,now)%4700;float blink=blinkPhase<280?.12f+.88f*Math.abs(blinkPhase-140)/140:1;
  blink=1-(1-blink)*CharacterMotionTuning.eyes();
  float talk=speaking?rapport.life.performance.mouth(now)*CharacterMotionTuning.face():0;
  int n=0;
  for(int row=0;row<=24;row++)for(int col=0;col<=24;col++){
   float u=col/24f,v=row/24f,dx=.008f*rapportLean*(1-v),dy=-.004f*Math.abs(rapportLean)*(1-v);
   float shoulder=v>.70f&&v<.97f?(float)Math.sin(Math.PI*(v-.70f)/.27f):0;
   dx+=.004f*rapportLean*shoulder;dy-=.003f*Math.abs(rapportLean)*shoulder;
   for(int eye=0;eye<2;eye++){
    float ex=eye==0?.414f:.607f,ey=.455f;
    float eyeWeight=weight(u,v,ex,ey,.075f,.040f);
    boolean closed=cell==3||(cell==2&&eye==1);
    if(!closed)dy+=(v-ey)*(blink-1)*eyeWeight;
    if(!quiet&&!closed)dx+=(.006f*pulse*(cell==4?-1:1)*CharacterMotionTuning.eyes()+.007f*rapportGaze)*eyeWeight;
    if(!quiet&&!closed)dy+=.007f*gazeY*eyeWeight;
    if(!quiet)dy-=.003f*Math.abs(rapportLean)*weight(u,v,ex,.410f,.085f,.025f);
   }
   if(speaking){float mw=weight(u,v,.50f,.625f,.085f,.046f);dy+=(v-.625f)*talk*.9f*mw;}
   mesh[n++]=dst.left+(u+dx)*w;mesh[n++]=dst.top+(v+dy)*h;
  }
  int save=canvas.save();canvas.rotate(quiet?0:CharacterMotionTuning.face()*(rapport.life.tilt(now,mode,quiet)+attention.tilt(now,quiet)+2.0f*rapportLean+(cell==4?-2f:cell==5?1.5f:cell==7?-1.5f:1)*pulse),dst.centerX(),dst.bottom);
  paint.setAlpha(Math.round(255*(.80f+.20f*Math.min(1,Math.max(0,now-changedAt)/180f))));
  float proximity=quiet?0:Math.min(.022f,Math.abs(rapportLean)*.014f*CharacterMotionTuning.approach());canvas.scale(1+proximity,1+proximity,dst.centerX(),dst.bottom);
  float transition=Math.min(1,Math.max(0,now-frameChangedAt)/240f);
  if(previousFrame!=null){if(transition<1){paint.setAlpha(Math.round(255*(1-transition)));float ph=Math.min(height*.94f,width*.94f*previousFrame.getHeight()/(float)previousFrame.getWidth()),pw=ph*previousFrame.getWidth()/previousFrame.getHeight();transitionRect.set((width-pw)/2,(height-ph)*.45f,(width+pw)/2,(height-ph)*.45f+ph);canvas.drawBitmap(previousFrame,null,transitionRect,paint);}else{previousFrame.recycle();previousFrame=null;}}
  paint.setAlpha(Math.round(255*(previousFrame==null?1:transition)));
  if(CharacterRenderBudget.quality<2)canvas.drawBitmapMesh(frame,24,24,mesh,0,null,0,paint);else canvas.drawBitmap(frame,null,dst,paint);
  if(speaking&&sequenceCell<0&&cell!=6&&talk>.2f){
   paint.setColor(Color.rgb(0,18,20));float mx=dst.left+w*.50f,my=dst.top+h*.625f;
   canvas.drawOval(mx-w*.024f,my-h*.006f*talk,mx+w*.024f,my+h*.012f*talk,paint);paint.setColor(Color.WHITE);
  }
  canvas.restoreToCount(save);return true;
 }
}
