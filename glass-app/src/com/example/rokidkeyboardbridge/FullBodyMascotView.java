package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.InputStream;
import java.util.Locale;

/** Full-body expression and idle frames. Does not run the portrait animation pipeline. */
public final class FullBodyMascotView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final RectF blendDestination=new RectF();
    private Bitmap bitmap;
    private Bitmap previousBitmap;
    private Bitmap lastDrawnBitmap;
    private int lastDrawnPose;
    private float lastDrawnFraction=1,previousFraction=1;
    private final Paint layerPaint=new Paint();
    private boolean breathingEnabled=true;
    private final float[] breathMesh=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
    private final PorterDuffXfermode additive=new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private int previousPose;
    private long blendStarted;
    private long blendDuration=FullBodyBlendPolicy.POSE_MS;
    private final FullBodyReactionPolicy reactions=new FullBodyReactionPolicy();
    private final FullBodyPoseVariation variation=new FullBodyPoseVariation();
    private final FullBodySpeechStance speechStance=new FullBodySpeechStance();
    private int speechExpression=-1;
    private int previewPose=-1;
    private int approvedMotionPreview=-1;
    private int dramaticPreview=-1,dramaticLoaded=-1,dramaticRequest=0;
    private Bitmap[] dramaticFrames;
    private long dramaticBreathStarted;
    private boolean dramaticLoading,dramaticAuto,dramaticCycle;
    private int situationMask;
    private final FullBodyConversationContinuity continuity=new FullBodyConversationContinuity();
    public void setConversationActive(boolean value){continuity.active(value,android.os.SystemClock.uptimeMillis());}
    public void endConversation(){
        continuity.clear();speechStance.reset();speechStance.observed(null);speechStance.scene("別の話","");
        speechExpression=-1;conversationMotionKind=-1;dramaticAuto=dramaticCycle=false;dramaticPreview=-1;
        situationMask=0;heavyBreathActive=rearStopActive=unwellActive=false;
        heavyBreathStarted=rearStopStarted=unwellStarted=conversationMotionStarted=0;invalidate();
    }
    private void rememberReaction(String text){
        long now=android.os.SystemClock.uptimeMillis();
        int candidate=speechExpression>=0?speechExpression:conversationBasePose;
        speechExpression=continuity.accept(candidate,speechStance.kind(),speechStance.intensity(),FullBodyConversationContinuity.posture(text),now);
    }
    private final FullBodyFaceAnimator[] approvedFaces={new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator()};
    private void prepareApprovedFaces(Bitmap[] frames,int first){if(frames!=null)for(int i=0;i<frames.length;i++)approvedFaces[first+i].prepare(frames[i]);}
    private int approvedFaceIndex(Bitmap frame){
        if(heavyBreathFrames!=null)for(int i=0;i<heavyBreathFrames.length;i++)if(frame==heavyBreathFrames[i])return i;
        if(rearStopFrames!=null)for(int i=0;i<rearStopFrames.length;i++)if(frame==rearStopFrames[i])return 3+i;
        if(unwellFrames!=null)for(int i=0;i<unwellFrames.length;i++)if(frame==unwellFrames[i])return 6+i;
        return -1;
    }
    private final FullBodySituationEffects situationEffects=new FullBodySituationEffects();
    private final FullBodyFaceAnimator[] dramaticFaces={new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator(),new FullBodyFaceAnimator()};
    private void updateDramaticConversation(String text,boolean beginning){
        int effects=speechStance.kind()==FullBodyPoseCatalog.Kind.REFUSE?0:FullBodyDramaticConversationPolicy.effects(text,speechStance.kind());
        if(FullBodyDramaticConversationPolicy.ends(text,speechStance.kind())){
            dramaticPreview=-1;dramaticAuto=false;dramaticCycle=false;situationMask=effects;
        }else situationMask|=effects;
        FullBodyDramaticConversationPolicy.Cue cue=FullBodyDramaticConversationPolicy.select(userContext,text,speechStance.kind());
        if(cue==null){
            FullBodySpeechStance.Cue explicit=FullBodySpeechStance.classify(text);
            if(explicit!=null&&(explicit.kind==FullBodyPoseCatalog.Kind.JOY||explicit.kind==FullBodyPoseCatalog.Kind.CONFIDENT||explicit.kind==FullBodyPoseCatalog.Kind.EXPLAIN||explicit.kind==FullBodyPoseCatalog.Kind.THINK)){
                dramaticPreview=-1;dramaticAuto=dramaticCycle=false;situationMask=effects;
            }
            return;
        }
        if(!beginning&&dramaticAuto&&cue.cycle&&!dramaticCycle){situationMask|=cue.effects;return;}
        boolean changed=!dramaticAuto||dramaticPreview!=cue.scene||dramaticCycle!=cue.cycle;
        dramaticPreview=cue.scene;dramaticAuto=true;dramaticCycle=cue.cycle;situationMask=changed?cue.effects:situationMask|cue.effects;
        if(changed)dramaticBreathStarted=android.os.SystemClock.uptimeMillis();
        requestDramaticImage();
    }
    public void setDramaticPreview(int id){
        endConversation();
        dramaticAuto=false;dramaticCycle=false;situationMask=FullBodyDramaticConversationPolicy.ROUGH_BREATH|(id>=2?FullBodyDramaticConversationPolicy.SWEAT:0);
        dramaticPreview=FullBodyDramaticGallery.valid(id)?id:-1;
        dramaticBreathStarted=android.os.SystemClock.uptimeMillis();
        approvedMotionPreview=-1;previewPose=-1;
        heavyBreathActive=rearStopActive=unwellActive=false;
        cancelPerformance();requestDramaticImage();requestImage();reconcileMotion();invalidate();
    }
    private void requestDramaticImage(){
        final int selected=dramaticPreview,id=dramaticCycle?7:selected;
        if(!FullBodyDramaticGallery.valid(selected)||dramaticLoading||dramaticLoaded==id||!isShown())return;
        dramaticLoading=true;final int token=generation,request=++dramaticRequest;
        new Thread(()->{
            final Bitmap[] result=decodeDramaticBreathFrames(id);
            post(()->{
                if(request!=dramaticRequest){if(result!=null)for(Bitmap frame:result)frame.recycle();return;}
                dramaticLoading=false;
                if(token!=generation||id!=(dramaticCycle?7:dramaticPreview)||!isShown()){
                    if(result!=null)for(Bitmap frame:result)frame.recycle();requestDramaticImage();return;
                }
                if(result==null){dramaticPreview=-1;invalidate();return;}
                // Let the render thread finish using published frames before GC releases them.
                for(int i=0;i<dramaticFaces.length;i++){dramaticFaces[i].clear();if(i<result.length)dramaticFaces[i].prepare(result[i]);}
                dramaticFrames=result;dramaticLoaded=id;dramaticBreathStarted=android.os.SystemClock.uptimeMillis();invalidate();
            });
        },"LokiDramaticGalleryDecode").start();
    }

    private Bitmap[] decodeDramaticBreathFrames(int id){
        Bitmap[] frames=new Bitmap[id==7?8:id==6?2:3];
        try(java.util.zip.ZipFile apk=new java.util.zip.ZipFile(getContext().getApplicationInfo().sourceDir)){
            if(id==7){
                decodeDramaticSheet(apk,FullBodyDramaticGallery.MOTION_SHEETS[2],3,0,3,frames,0);
                decodeDramaticSheet(apk,FullBodyDramaticGallery.MOTION_SHEETS[3],3,0,3,frames,3);
                decodeDramaticSheet(apk,FullBodyDramaticGallery.BRIDGE_SHEET,4,1,2,frames,6);
            }else decodeDramaticSheet(apk,FullBodyDramaticGallery.MOTION_SHEETS[id],frames.length,0,frames.length,frames,0);
            return frames;
        }catch(Exception error){
            for(Bitmap frame:frames)if(frame!=null)frame.recycle();
            android.util.Log.w("LokiFullBody","Dramatic motion sheet unavailable",error);
            return null; // Retain the normal conversation renderer on decode failure.
        }
    }
    private void decodeDramaticSheet(java.util.zip.ZipFile apk,String asset,int cells,int first,int count,Bitmap[] frames,int offset)throws Exception{
        java.util.zip.ZipEntry entry=apk.getEntry("assets/"+asset);
        if(entry==null)throw new java.io.IOException("Missing motion sheet "+asset);
        try(InputStream input=apk.getInputStream(entry)){
            BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(input,false);
            try{
                int width=decoder.getWidth(),height=decoder.getHeight();
                if(width<cells||height<2)throw new java.io.IOException("Invalid motion sheet");
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                for(int i=0;i<count;i++){
                    int cell=first+i;
                    frames[offset+i]=decoder.decodeRegion(new Rect(cell*width/cells,0,(cell+1)*width/cells,height),options);
                    if(frames[offset+i]==null)throw new java.io.IOException("Cannot decode motion frame");
                }
            }finally{decoder.recycle();}
        }
    }
    private void drawDramaticBreathFrame(Canvas canvas,int index,float alpha,boolean add,long elapsed){
        Bitmap frame=dramaticFrames[index];if(frame==null||frame.isRecycled()||alpha<=0)return;
        float scale=Math.min((float)getWidth()/frame.getWidth(),(float)getHeight()/frame.getHeight());
        float width=frame.getWidth()*scale,height=frame.getHeight()*scale;
        float x=(getWidth()-width)/2+width*FullBodyDramaticFeatures.SHIFTX[dramaticLoaded][index];
        float y=getHeight()-height+height*FullBodyDramaticFeatures.SHIFTY[dramaticLoaded][index];
        layerPaint.setAlpha(Math.round(255*alpha));layerPaint.setXfermode(add?additive:null);
        int saved=canvas.saveLayer(0,0,getWidth(),getHeight(),layerPaint);
        canvas.translate(x,y);canvas.scale(scale,scale);canvas.drawBitmap(frame,0,0,paint);
        float[] features=FullBodyDramaticFeatures.ALL[dramaticLoaded][index];
        dramaticFaces[index].drawFeatures(canvas,frame,features,elapsed,mode==2,true);
        situationEffects.draw(canvas,frame,features,elapsed,situationMask);
        canvas.restoreToCount(saved);layerPaint.setAlpha(255);layerPaint.setXfermode(null);
    }
    public void setPreviewPose(int id){endConversation();dramaticPreview=-1;approvedMotionPreview=-1;previewPose=id<0?-1:Math.min(180,id);requestImage();reconcileMotion();}
    public void setApprovedMotionPreview(int kind){
        endConversation();
        if(kind<0||kind>2){setPreviewPose(-1);return;}
        dramaticPreview=-1;approvedMotionPreview=kind;previewPose=-1;
        heavyBreathActive=rearStopActive=unwellActive=false;
        heavyBreathStarted=rearStopStarted=unwellStarted=0;
        cancelPerformance();requestImage();reconcileMotion();
    }
    private String userContext="";
    private boolean observedScene;
    public void setUserContext(String prompt){
        long now=android.os.SystemClock.uptimeMillis();
        if(!continuity.engaged(now)||FullBodyConversationContinuity.topic(prompt)||FullBodyConversationContinuity.end(prompt))endConversation();
        continuity.touch(now);userContext=prompt==null?"":prompt;observedScene=false;speechStance.observed(null);
    }
    public void setObservedEmotion(String emotion){setObservedEmotion(emotion,1);}
    public void setObservedEmotion(String emotion,int intensity){observedScene=true;speechStance.observed(new FullBodySpeechStance.Cue(AmbientScenePolicy.kind(emotion),AmbientScenePolicy.intensity(intensity)));}
    private int conversationBasePose=4;
    private int conversationMotionKind=-1,conversationMotionCached=-1;
    private Bitmap[] conversationMotionFrames;
    private boolean conversationMotionLoading;
    private long conversationMotionStarted;
    private Bitmap[] heavyBreathFrames;
    private boolean heavyBreathLoading,heavyBreathActive;
    private long heavyBreathStarted;
    private void requestHeavyBreathFrames(){
        if(heavyBreathFrames!=null||heavyBreathLoading||!isShown())return;
        heavyBreathLoading=true;final int token=generation;
        new Thread(()->{
            Bitmap[] frames=decodeApprovedFrames(FullBodyHeavyBreathPolicy.ASSETS);
            final Bitmap[] result=frames;
            post(()->{
                heavyBreathLoading=false;
                if(token!=generation||!isShown()){if(result!=null)for(Bitmap b:result)b.recycle();return;}
                heavyBreathFrames=result;prepareApprovedFaces(result,0);if(result==null)heavyBreathActive=false;invalidate();
            });
        },"LokiHeavyBreathDecode").start();
    }
    private Bitmap[] decodeApprovedFrames(String[] assets){
        Bitmap[] frames=new Bitmap[assets.length];
        try(java.util.zip.ZipFile apk=new java.util.zip.ZipFile(getContext().getApplicationInfo().sourceDir)){
            for(int i=0;i<assets.length;i++){
                java.util.zip.ZipEntry entry=apk.getEntry("assets/"+assets[i]);
                if(entry==null)throw new java.io.IOException("Missing approved asset "+assets[i]);
                try(InputStream input=apk.getInputStream(entry)){
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                    frames[i]=BitmapFactory.decodeStream(input,null,options);
                    if(frames[i]==null)throw new java.io.IOException("Approved frame "+assets[i]);
                }
            }
            return frames;
        }catch(Exception error){
            for(Bitmap b:frames)if(b!=null)b.recycle();
            android.util.Log.w("LokiFullBody","Cannot decode approved motion",error);return null;
        }
    }
    private Bitmap[] rearStopFrames;
    private boolean rearStopLoading,rearStopActive;
    private long rearStopStarted;
    private void requestRearStopFrames(){
        if(rearStopFrames!=null||rearStopLoading||!isShown())return;
        rearStopLoading=true;final int token=generation;
        new Thread(()->{
            final Bitmap[] frames=decodeApprovedFrames(FullBodyRearStopPolicy.ASSETS);
            post(()->{
                rearStopLoading=false;
                if(token!=generation||!isShown()){if(frames!=null)for(Bitmap b:frames)b.recycle();return;}
                rearStopFrames=frames;prepareApprovedFaces(frames,3);if(frames==null){rearStopActive=false;if(approvedMotionPreview==1)approvedMotionPreview=-1;}invalidate();
            });
        },"LokiRearStopDecode").start();
    }
    private Bitmap[] unwellFrames;
    private boolean unwellLoading,unwellActive;
    private long unwellStarted;
    private void requestUnwellFrames(){
        if(unwellFrames!=null||unwellLoading||!isShown())return;
        unwellLoading=true;final int token=generation;
        new Thread(()->{
            final Bitmap[] frames=decodeApprovedFrames(FullBodyUnwellPolicy.ASSETS);
            post(()->{
                unwellLoading=false;
                if(token!=generation||!isShown()){if(frames!=null)for(Bitmap b:frames)b.recycle();return;}
                unwellFrames=frames;prepareApprovedFaces(frames,6);
                if(frames==null){unwellActive=false;if(approvedMotionPreview==2)approvedMotionPreview=-1;}
                invalidate();
            });
        },"LokiUnwellDecode").start();
    }
    private void drawApprovedFrame(Canvas canvas,Bitmap frame,float opacity,boolean add){
        drawApprovedFrame(canvas,frame,opacity,add,1f);
    }
    private void drawApprovedFrame(Canvas canvas,Bitmap frame,float opacity,boolean add,float heightFraction){
        if(frame==null||frame.isRecycled()||opacity<=0)return;
        // Fixed whole-image canvas for all phases; preserves the source margins.
        float height=Math.min(getHeight()*heightFraction,getWidth()*1.5f),width=height/1.5f;
        destination.set((getWidth()-width)/2,getHeight()-height,(getWidth()+width)/2,getHeight());
        layerPaint.setAlpha(Math.round(255*Math.min(1,opacity)));layerPaint.setXfermode(add?additive:null);
        int saved=canvas.saveLayer(0,0,getWidth(),getHeight(),layerPaint);
        canvas.drawBitmap(frame,null,destination,paint);
        int face=approvedFaceIndex(frame);
        if(face>=0){
            canvas.translate(destination.left,destination.top);canvas.scale(width/frame.getWidth(),height/frame.getHeight());
            long now=android.os.SystemClock.uptimeMillis();float[] f=FullBodyApprovedFeatures.ALL[face];
            approvedFaces[face].drawFeatures(canvas,frame,f,now,mode==2,true);
            int mask=situationMask;if(face>=6)mask|=FullBodyDramaticConversationPolicy.SWEAT;
            situationEffects.draw(canvas,frame,f,now,mask);
        }
        canvas.restoreToCount(saved);layerPaint.setXfermode(null);layerPaint.setAlpha(255);
    }
    public void beginSpeech(String answer){
        long now=android.os.SystemClock.uptimeMillis();boolean keep=continuity.holds(now);
        if(!keep){dramaticPreview=-1;dramaticAuto=dramaticCycle=false;situationMask=0;}
        conversationBasePose=keep?continuity.pose():loaded>=0&&FullBodyMotionPolicy.isSeated(loaded)?168:4;
        boolean seatedAtSpeech=FullBodyMotionPolicy.isSeated(conversationBasePose);
        approvedMotionPreview=-1;previewPose=-1;speechStance.begin(answer,keep);
        FullBodySemanticReaction.Reaction semantic=FullBodySemanticReaction.find(answer);
        if(semantic!=null)speechStance.semantic(semantic);
        if(!observedScene)speechStance.scene(userContext,answer);speechExpression=speechStance.pose(answer,conversationBasePose);rememberReaction(answer);
        boolean fresh=!keep||semantic!=null||FullBodySpeechStance.classify(answer)!=null
                ||FullBodyScenePolicy.evaluate(userContext,answer)!=null||FullBodyConversationContinuity.posture(answer);
        if(fresh){
            conversationMotionKind=FullBodyConversationMotion.kind(speechStance.kind(),speechStance.laughing(),seatedAtSpeech,answer);
            heavyBreathActive=FullBodyHeavyBreathPolicy.eligible(speechStance.kind(),seatedAtSpeech);heavyBreathStarted=0;
            rearStopActive=FullBodyRearStopPolicy.eligible(speechStance.kind(),seatedAtSpeech,answer);rearStopStarted=0;
            unwellActive=FullBodyUnwellPolicy.eligible(speechStance.kind(),seatedAtSpeech,answer);unwellStarted=0;
            conversationMotionStarted=0;
        }
        FullBodySpeechStance.Cue local=FullBodyScenePolicy.evaluate(userContext,answer);
        if(local==null)local=FullBodySpeechStance.classify(answer);
        if(getVisibility()==VISIBLE)FullBodyReactionAudit.record(getContext(),userContext,answer,speechExpression,semantic==null?null:semantic.cue,local);
        updateDramaticConversation(answer,true);requestImage();reconcileMotion();
    }
    public void setSpeechText(String text){
        if(text==null||text.length()==0){
            continuity.touch(android.os.SystemClock.uptimeMillis());requestImage();reconcileMotion();return;
        }
        else {
            speechExpression=speechStance.pose(text,continuity.holds(android.os.SystemClock.uptimeMillis())?continuity.pose():loaded);rememberReaction(text);
            if(conversationMotionKind>=0 && !FullBodyConversationMotion.continues(conversationMotionKind,speechStance.kind(),speechStance.laughing())) conversationMotionKind=-1;
        }
        FullBodySpeechStance.Cue explicit=FullBodySpeechStance.classify(text);
        if(explicit!=null){
            heavyBreathActive=heavyBreathActive&&FullBodyHeavyBreathPolicy.eligible(speechStance.kind(),false);
            rearStopActive=rearStopActive&&speechStance.kind()==FullBodyPoseCatalog.Kind.REFUSE;
            if(speechStance.kind()==FullBodyPoseCatalog.Kind.RECOVERY||speechStance.kind()==FullBodyPoseCatalog.Kind.JOY||speechStance.kind()==FullBodyPoseCatalog.Kind.REFUSE||speechStance.kind()==FullBodyPoseCatalog.Kind.ANGER)unwellActive=false;
        }
        updateDramaticConversation(text,false);
        if(speechExpression>=0)android.util.Log.i("LokiFullBody","Speech reaction pose="+speechExpression+" tags="+FullBodyPoseCatalog.ALL[speechExpression].kinds);
        requestImage();reconcileMotion();
    }
    private int manualPenKind=-1,manualPenPose=-1;
    private boolean manualPenRequested;
    private long manualPenStarted;
    public void startPenPickup(){
        endConversation();
        dramaticPreview=-1;cancelPerformance();previewPose=-1;
        manualPenPose=loaded>=0?loaded:FullBodyMotionPolicy.poseForState(expression,mode);
        manualPenKind=FullBodyMotionPolicy.isSeated(manualPenPose)?2:3;
        manualPenRequested=true;manualPenStarted=0;requestImage();reconcileMotion();
    }
    private boolean pickingPen(long now){return manualPenRequested||(manualPenStarted>0&&now-manualPenStarted<FullBodyPenCommand.TOTAL);}
    private Bitmap[] performanceFrames;
    private boolean performanceRequested,performanceLoading;
    private long performanceStarted;
    public void startPerformance(){endConversation();dramaticPreview=-1;manualPenRequested=false;manualPenStarted=0;previewPose=-1;performanceRequested=true;performanceStarted=0;requestImage();reconcileMotion();}
    public void cancelPerformance(){manualPenRequested=false;manualPenStarted=0;performanceRequested=false;performanceStarted=0;invalidate();}
    private boolean performing(long now){return performanceRequested||(performanceStarted>0&&now-performanceStarted<FullBodyPerformancePolicy.TOTAL);}
    private void requestPerformanceFrames(){
        if(performanceFrames!=null||performanceLoading||!isShown())return;
        performanceLoading=true;final int token=generation;
        new Thread(()->{
            Bitmap[] frames=decodeGroup(FullBodyPerformancePolicy.frames(),1);
            post(()->{performanceLoading=false;
                if(token!=generation||!isShown()){if(frames!=null)for(Bitmap b:frames)b.recycle();}
                else if(frames!=null){performanceFrames=frames;invalidate();}
                else performanceRequested=false;
            });
        },"LokiFullBodyPerformanceDecode").start();
    }
    public int restExpression(){return loaded>=0&&FullBodyMotionPolicy.isSeated(loaded)?14:4;}
    private long idleStarted=android.os.SystemClock.uptimeMillis();
    private int requestedPose(){
        long now=android.os.SystemClock.uptimeMillis();
        if(approvedMotionPreview==2||(unwellActive&&(mode==2||continuity.holds(now))))return 168;
        if(approvedMotionPreview>=0)return 4;
        if(previewPose>=0)return previewPose;
        if(conversationMotionKind>=0&&(mode==2||continuity.holds(now)))return conversationMotionKind>=2?conversationBasePose:168;
        if(pickingPen(now))return manualPenPose;
        if(performing(now))return 4;
        if(continuity.holds(now))return continuity.pose();
        int cue=expression;
        if(mode==0&&(expression==0||expression==14)){
            int waiting=FullBodySeatedPolicy.waitingPose(now-idleStarted);
            if(waiting!=168)cue=waiting;
        }
        int base=FullBodyMotionPolicy.poseForState(cue,mode);
        if(mode==1&&FullBodyMotionPolicy.isSeated(loaded))base=168;
        if(mode==2&&FullBodyMotionPolicy.isSeated(loaded)&&(cue==0||cue==14))base=168;
        if(mode==2&&speechExpression>=0)base=speechExpression;
        if(mode==0&&expression==4)base=4;
        int seatFrame=FullBodySeatedPolicy.frame(now-seatedStarted);
        int target=variation.target(base,mode,now,speechStance.kind(),seatFrame==0||seatFrame==3);
        if(mode==0 && loaded>=0 && FullBodyMotionPolicy.isSeated(base)==FullBodyMotionPolicy.isSeated(loaded) && FullBodyIdleActivity.kind(now-idleStarted,FullBodyMotionPolicy.isSeated(base))>=0)target=loaded;
        FullBodyPoseCatalog.Kind urgent=speechStance.kind();
        if(mode==2 && speechExpression>=0 && (urgent==FullBodyPoseCatalog.Kind.REFUSE||urgent==FullBodyPoseCatalog.Kind.SURPRISE)
            && (loaded<0||loaded>=181||!FullBodyPoseCatalog.ALL[loaded].supports(urgent)))return target;
        return reactions.targetPose(target,mode,loaded,now,transitionFrames!=null);
    }
    private Bitmap[] seatedFrames;
    private Bitmap[] motionFrames;
    private long seatedStarted;
    private final Bitmap[][] transitionCache = new Bitmap[2][];
    private Bitmap[] transitionFrames;
    private int transitionKind;
    private int transitionFirst=186,cachedLink=-1;
    private Bitmap[] linkFrames;
    private boolean transitionReverse;
    private long transitionStarted;

    private int expression, loaded = -1, generation;
    private Bitmap[] activityFrames;
    private int activityCached=-1;
    private boolean activityLoading;
    private boolean loading;
    private Bitmap[] idleFrames;
    private boolean idleLoading, hostResumed = true;
    private int mode;
    private boolean animateMouth=true;
    private final FullBodyFaceAnimator faceAnimator=new FullBodyFaceAnimator();
    private final FullBodyRokidGlasses rokidGlasses=new FullBodyRokidGlasses();
    public void setSpeechPresentation(boolean value) { animateMouth=value; invalidate(); }
    private long motionStarted = android.os.SystemClock.uptimeMillis();
    private final Runnable motionTick = new Runnable() {
        public void run() {
            if (!hostResumed || !isShown() || getWindowVisibility() != View.VISIBLE) return;
            if(!continuity.engaged(android.os.SystemClock.uptimeMillis())&&(speechExpression>=0||dramaticAuto))endConversation();
            requestImage();
            invalidate();
            postDelayed(this, 33L);
        }
    };

    public void setMode(int value){
        long now=android.os.SystemClock.uptimeMillis();
        if(mode==2&&value!=2)continuity.touch(now);
        if(value==1)continuity.touch(now);
        if(value==0&&mode!=0)idleStarted=now;
        if(!continuity.engaged(now)&&dramaticAuto)endConversation();
        mode=value;requestImage();reconcileMotion();
    }
    public void setHostResumed(boolean value) { hostResumed = value; reconcileMotion(); }
    private void reconcileMotion() {
        if (motionTick == null) return;
        removeCallbacks(motionTick);
        if (hostResumed && isShown() && getWindowVisibility() == View.VISIBLE
                && (bitmap!=null||dramaticPreview>=0)) post(motionTick);
    }

    public FullBodyMascotView(Context context) { super(context); setBackgroundColor(Color.BLACK); }

    public void setExpression(int id) {
        expression = Math.max(0, Math.min(167, id));
        requestImage();
        reconcileMotion();
    }

    private void requestImage() {
        if (!isShown() || getWidth() <= 0 || loading || idleLoading || loaded == requestedPose()) return;
        final int id = requestedPose(), token = generation;
        final int targetHeight = Math.max(1, getHeight());
        final Bitmap[] cachedSeated=seatedFrames;
        final int from = loaded;
        final int link=FullBodyPoseLinks.index(from,id);
        final int kind = link>=0?3:FullBodyTransitionPolicy.kind(from,id);
        final Bitmap[] cached = kind==0 ? null : kind==3?(cachedLink==link?linkFrames:null):transitionCache[kind-1];
        loading = true;
        new Thread(() -> {
            final Bitmap[] seatSteps=id==168 ? (cachedSeated!=null?cachedSeated:decodeSeatedFrames()) : null;
            final Bitmap[] bodySteps=id>=169?decodeMotionFrames(id>=175):null;
            Bitmap result = bodySteps!=null?bodySteps[(id-169)%6]:seatSteps!=null ? seatSteps[0] : decode(id, targetHeight);
            if (result == null && id != 0) result = decode(0, targetHeight);
            final Bitmap image = result;
            final Bitmap[] steps = kind==0 ? null : (cached!=null ? cached : kind==3?decodeGroup(FullBodyPoseLinks.frames(link),1):decodeTransition(kind));
            post(() -> {
                loading = false;
                if (token != generation || !isShown() || id != requestedPose()) {
                    if (image != null && seatSteps==null && bodySteps==null) image.recycle();
                    if(bodySteps!=null)for(Bitmap frame:bodySteps)frame.recycle();
                    if(seatSteps!=null && seatSteps!=cachedSeated)for(Bitmap frame:seatSteps)frame.recycle();
                    if(steps!=null && steps!=cached) for(Bitmap step:steps)step.recycle();
                } else {
                    // Leave the previous bitmap to GC; a render thread may still reference it.
                    previousBitmap=lastDrawnBitmap!=null?lastDrawnBitmap:bitmap;previousPose=lastDrawnBitmap!=null?lastDrawnPose:loaded;blendStarted=android.os.SystemClock.uptimeMillis();
                    previousFraction=lastDrawnBitmap!=null?lastDrawnFraction:FullBodyMotionPolicy.bodyHeightFraction(loaded);
                    blendDuration=FullBodyBlendPolicy.POSE_MS;
                    bitmap = image;
                    motionFrames=bodySteps;
                    if(seatSteps!=null){seatedFrames=seatSteps;if(cachedSeated==null||from<0||!FullBodyMotionPolicy.isSeated(from))seatedStarted=android.os.SystemClock.uptimeMillis();}
                    loaded = id;
                    reactions.committed(android.os.SystemClock.uptimeMillis());
                    if(steps!=null){
                        if(kind==3){cachedLink=link;linkFrames=steps;}else transitionCache[kind-1]=steps;
                        transitionFrames=steps;transitionKind=kind;transitionFirst=kind==3?FullBodyPoseLinks.firstFrame(link):kind==1?186:192;
                        transitionReverse=kind==3?FullBodyPoseLinks.reverse(link,from):FullBodyTransitionPolicy.reverse(kind,from,id);
                        transitionStarted=android.os.SystemClock.uptimeMillis();
                    }
                    else transitionFrames=null;
                    android.util.Log.i("LokiFullBody", "Conversation state="+mode+" pose="+id);
                    reconcileMotion();
                    invalidate();
                }
                requestImage();
            });
        }, "LokiFullBodyDecode").start();
    }

    private void requestIdle() {
        if (!FullBodyMotionPolicy.usesIdleFrames(expression, mode) || idleFrames != null
                || idleLoading || loading || !isShown()) return;
        final int token = generation;
        idleLoading = true;
        new Thread(() -> {
            Bitmap[] frames = decodeIdle();
            post(() -> {
                idleLoading = false;
                if (token != generation || !isShown()) {
                    if (frames != null) for (Bitmap frame : frames) frame.recycle();
                } else {
                    idleFrames = frames;
                    motionStarted = android.os.SystemClock.uptimeMillis();
                    reconcileMotion();
                    invalidate();
                }
                requestImage();
            });
        }, "LokiFullBodyIdleDecode").start();
    }

    private void requestActivity(int kind){
        if(activityLoading || loading || activityCached==kind || !isShown())return;
        activityLoading=true;final int token=generation;
        new Thread(()->{
            Bitmap[] frames=decodeGroup(FullBodyIdleActivity.frames(kind),1);
            post(()->{
                activityLoading=false;
                if(token!=generation || !isShown()){
                    if(frames!=null)for(Bitmap image:frames)image.recycle();
                }else if(frames!=null){activityFrames=frames;activityCached=kind;invalidate();}
            });
        },"LokiFullBodyActivityDecode").start();
    }
    private void requestConversationMotion(){
        int kind=conversationMotionKind;
        if(kind<0||conversationMotionLoading||conversationMotionCached==kind||!isShown())return;
        conversationMotionLoading=true;final int token=generation;
        new Thread(()->{
            Bitmap[] frames=decodeGroup(FullBodyConversationMotion.frames(kind),2);
            post(()->{
                conversationMotionLoading=false;
                if(token!=generation||!isShown()){
                    if(frames!=null)for(Bitmap image:frames)image.recycle();
                }else if(frames!=null){conversationMotionFrames=frames;conversationMotionCached=kind;invalidate();}
            });
        },"LokiConversationMotionDecode").start();
    }
    private Bitmap[] decodeGroup(int[] ids,int sample){
        Bitmap[] frames=new Bitmap[ids.length];
        try(java.util.zip.ZipFile apk=new java.util.zip.ZipFile(getContext().getApplicationInfo().sourceDir)){
            for(int i=0;i<ids.length;i++){frames[i]=FullBodyRealisticAssets.decode(apk,ids[i],sample);if(frames[i]==null)throw new java.io.IOException("Fullbody frame "+ids[i]);}
            return frames;
        }catch(Exception error){for(Bitmap frame:frames)if(frame!=null)frame.recycle();android.util.Log.w("LokiFullBody","Frame group unavailable",error);return null;}
    }
    private Bitmap[] decodeIdle(){return decodeGroup(new int[]{169,170,171,172,173,174},1);}
    private Bitmap[] decodeSeatedFrames(){return decodeGroup(FullBodyRealisticAssets.SEATED,2);}
    private Bitmap decode(int id,int targetHeight){
        try(java.util.zip.ZipFile apk=new java.util.zip.ZipFile(getContext().getApplicationInfo().sourceDir)){
            return FullBodyRealisticAssets.decode(apk,id,id==168?2:1);
        }catch(Exception error){android.util.Log.w("LokiFullBody","Cannot decode fullbody "+id,error);return null;}
    }
    private Bitmap[] decodeTransition(int kind){return decodeGroup(kind==1?new int[]{186,187,188,189,190,191}:new int[]{192,193,194,195,196,197},2);}
    private Bitmap[] decodeMotionFrames(boolean walking){return walking?decodeGroup(new int[]{175,176,177,178,179,180},1):decodeIdle();}

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(dramaticPreview>=0){
            requestDramaticImage();
            int key=dramaticCycle?7:dramaticPreview;
            if(dramaticLoaded==key&&dramaticFrames!=null){
                long elapsed=android.os.SystemClock.uptimeMillis()-dramaticBreathStarted;
                long period=FullBodyDramaticMotionPolicy.period(situationMask);
                int from,to;float weight;
                if(dramaticCycle&&FullBodyDramaticMotionPolicy.bridge(elapsed)){
                    from=FullBodyDramaticMotionPolicy.bridgeFrom(elapsed);to=FullBodyDramaticMotionPolicy.bridgeTo(elapsed);weight=FullBodyDramaticMotionPolicy.bridgeBlend(elapsed);
                }else if(dramaticFrames.length==2){
                    from=0;to=1;weight=FullBodyDramaticBreathPolicy.exhaleWeight(elapsed*900/period);
                }else{
                    long local=dramaticCycle?FullBodyDramaticMotionPolicy.holdElapsed(elapsed):elapsed;
                    int base=dramaticCycle&&FullBodyDramaticMotionPolicy.second(elapsed)?3:0;
                    from=base+FullBodyDramaticMotionPolicy.breathFrom(local,period);to=base+FullBodyDramaticMotionPolicy.breathTo(local,period);weight=FullBodyDramaticMotionPolicy.breathBlend(local,period);
                }
                int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
                drawDramaticBreathFrame(canvas,from,1f-weight,false,elapsed);
                drawDramaticBreathFrame(canvas,to,weight,true,elapsed);
                canvas.restoreToCount(layer);return;
            }
            // During async loading, keep showing the ordinary conversation pose.
        }
        if (bitmap == null || bitmap.isRecycled()) { requestImage(); return; }
        long approvedNow=android.os.SystemClock.uptimeMillis();
        if((((mode==2||continuity.holds(approvedNow))&&unwellActive)||approvedMotionPreview==2)&&previewPose<0&&!performing(approvedNow)&&!pickingPen(approvedNow)){
            requestUnwellFrames();
            if(unwellFrames!=null){
                if(unwellStarted==0)unwellStarted=approvedNow;
                long time=approvedNow-unwellStarted;
                if(approvedMotionPreview==2)time%=3000L;
                float blend=FullBodyUnwellPolicy.blend(time);
                float heightFraction=FullBodyMotionPolicy.bodyHeightFraction(168);
                int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
                drawApprovedFrame(canvas,unwellFrames[FullBodyUnwellPolicy.previous(time)],1-blend,false,heightFraction);
                drawApprovedFrame(canvas,unwellFrames[FullBodyUnwellPolicy.frame(time)],blend,blend<1,heightFraction);
                canvas.restoreToCount(layer);return;
            }
        }
        if((((mode==2||continuity.holds(approvedNow))&&rearStopActive)||approvedMotionPreview==1)&&previewPose<0&&!performing(approvedNow)&&!pickingPen(approvedNow)){
            requestRearStopFrames();
            if(rearStopFrames!=null){
                if(rearStopStarted==0)rearStopStarted=approvedNow;
                long time=approvedNow-rearStopStarted;
                if(approvedMotionPreview==1)time%=2400L;
                float blend=FullBodyRearStopPolicy.blend(time);
                int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
                drawApprovedFrame(canvas,rearStopFrames[FullBodyRearStopPolicy.previous(time)],1-blend,false);
                drawApprovedFrame(canvas,rearStopFrames[FullBodyRearStopPolicy.frame(time)],blend,blend<1);
                canvas.restoreToCount(layer);
                return;
            }
        }
        if((((mode==2||continuity.holds(approvedNow))&&heavyBreathActive)||approvedMotionPreview==0)&&previewPose<0&&!performing(approvedNow)&&!pickingPen(approvedNow)){
            requestHeavyBreathFrames();
            if(heavyBreathFrames!=null){
                long now=android.os.SystemClock.uptimeMillis();
                if(heavyBreathStarted==0)heavyBreathStarted=now;
                Bitmap frame=heavyBreathFrames[FullBodyHeavyBreathPolicy.frame(now-heavyBreathStarted)];
                // Whole-image canvases retain the approved margins and closed eyes.
                // Never add talking mouth/blink overlays to these tired faces.
                drawApprovedFrame(canvas,frame,1,false);
                return;
            }
        }
        long elapsed=android.os.SystemClock.uptimeMillis()-motionStarted;
        Bitmap displayed=bitmap;
        int displayedPose=loaded;
        if(motionFrames!=null&&previewPose<0){int frame=loaded>=175?(int)(elapsed/180L%6):FullBodyMotionPolicy.idleFrame(elapsed);displayed=motionFrames[frame];displayedPose=(loaded>=175?175:169)+frame;}
        if(loaded==168 && mode==0 && seatedFrames!=null){
            int frame=variation.anchorPose()==168?FullBodySeatedPolicy.frame(android.os.SystemClock.uptimeMillis()-seatedStarted):0;
            displayed=seatedFrames[frame];displayedPose=FullBodyRealisticAssets.SEATED[frame];
        }
        if(idleFrames!=null && mode==0 && loaded>=169 && loaded<=174 && speechExpression<0)
            {int frame=FullBodyMotionPolicy.idleFrame(elapsed);displayed=idleFrames[frame];displayedPose=169+frame;}
        long now=android.os.SystemClock.uptimeMillis();
        if(pickingPen(now) && transitionFrames==null){
            requestActivity(manualPenKind);
            if(activityCached==manualPenKind && activityFrames!=null){
                if(manualPenRequested){manualPenRequested=false;manualPenStarted=now;}
                int frame=FullBodyPenCommand.frame(now-manualPenStarted);
                displayed=activityFrames[frame];displayedPose=FullBodyIdleActivity.firstFrame(manualPenKind)+frame;
            }
        }
        int activity=mode==0 && !continuity.holds(now) && previewPose<0 && !performing(now) && !pickingPen(now)
            ?FullBodyIdleActivity.kind(now-idleStarted,FullBodyMotionPolicy.isSeated(loaded)):-1;
        if(activity>=0 && transitionFrames==null){
            requestActivity(activity);
            if(activityCached==activity && activityFrames!=null){
                int frame=FullBodyIdleActivity.frame(activity,now-idleStarted);
                displayed=activityFrames[frame];displayedPose=FullBodyIdleActivity.firstFrame(activity)+frame;
            }
        }
        if(performing(now) && loaded==4 && transitionFrames==null){
            requestPerformanceFrames();
            if(performanceFrames!=null){
                if(performanceRequested){performanceRequested=false;performanceStarted=now;}
                int frame=FullBodyPerformancePolicy.frame(now-performanceStarted);
                displayed=performanceFrames[frame];displayedPose=FullBodyPerformancePolicy.FIRST+frame;
            }
        }
        if((mode==2||continuity.holds(now))&&conversationMotionKind>=0&&!performing(now)&&!pickingPen(now)){
            requestConversationMotion();
            if(conversationMotionFrames!=null&&conversationMotionCached==conversationMotionKind){
                if(conversationMotionStarted==0)conversationMotionStarted=now;
                int frame=FullBodyConversationMotion.frame(conversationMotionKind,now-conversationMotionStarted);
                displayed=conversationMotionFrames[frame];displayedPose=FullBodyConversationMotion.pose(conversationMotionKind,frame);
            }
        }
        if(transitionFrames!=null){
            long time=now-transitionStarted;
            if(time<FullBodyTransitionPolicy.DURATION_MS && previousBitmap!=null){
                float position=FullBodyBlendPolicy.position(time,FullBodyTransitionPolicy.DURATION_MS,7);
                int segment=(int)position;
                float amount=FullBodyBlendPolicy.ease(position-segment);
                Bitmap first=transitionBitmap(segment,displayed);
                Bitmap second=transitionBitmap(segment+1,displayed);
                int firstPose=transitionPose(segment,displayedPose);
                int secondPose=transitionPose(segment+1,displayedPose);
                float height=previousFraction
                    +(FullBodyMotionPolicy.bodyHeightFraction(loaded)-previousFraction)
                    *FullBodyBlendPolicy.ease(time/(float)FullBodyTransitionPolicy.DURATION_MS);
                drawFrame(canvas,first,firstPose,elapsed,height,1-amount,false);
                drawFrame(canvas,second,secondPose,elapsed,height,amount,true);
                return;
            }
            transitionFrames=null;reconcileMotion();
        }
        // Smooth the smaller idle/walking/leg frames as well as whole pose changes.
        if(lastDrawnBitmap!=null && displayed!=lastDrawnBitmap
                && (previousBitmap==null || blendDuration==70L)){
            previousBitmap=lastDrawnBitmap;previousPose=lastDrawnPose;previousFraction=lastDrawnFraction;
            blendStarted=now;blendDuration=FullBodyIdleActivity.isFrame(displayedPose)!=FullBodyIdleActivity.isFrame(lastDrawnPose)?FullBodyBlendPolicy.POSE_MS:displayedPose>=276?90L:displayedPose>=240?220L:70L;
        }
        long blendTime=now-blendStarted;
        if(previousBitmap!=null && blendTime<blendDuration){
            float amount=FullBodyBlendPolicy.ease(blendTime/(float)blendDuration);
            drawFrame(canvas,previousBitmap,previousPose,elapsed,previousFraction,1-amount,false);
            drawFrame(canvas,displayed,displayedPose,elapsed,FullBodyMotionPolicy.bodyHeightFraction(loaded),amount,true);
        }else{
            previousBitmap=null;
            drawFrame(canvas,displayed,displayedPose,elapsed,FullBodyMotionPolicy.bodyHeightFraction(loaded),1,false);
        }
        lastDrawnBitmap=displayed;lastDrawnPose=displayedPose;lastDrawnFraction=FullBodyMotionPolicy.bodyHeightFraction(loaded);
    }

    private Bitmap transitionBitmap(int node,Bitmap target){
        if(node==0)return previousBitmap;
        if(node>=7)return target;
        return transitionFrames[transitionReverse?6-node:node-1];
    }
    private int transitionPose(int node,int target){
        if(node==0)return previousPose;
        if(node>=7)return target;
        return transitionFirst+(transitionReverse?6-node:node-1);
    }
    private void drawFrame(Canvas canvas,Bitmap frame,int pose,long elapsed,float fraction,float opacity,boolean add){
        if(opacity<=0 || frame==null || frame.isRecycled())return;
        int alpha=Math.round(255*Math.min(1,opacity));
        boolean layered=alpha<255||add;
        int layer=0;
        if(layered){
            layerPaint.setAlpha(alpha);layerPaint.setXfermode(add?additive:null);
            layer=canvas.saveLayer(0,0,getWidth(),getHeight(),layerPaint);
        }
        float scale=FullBodyScalePolicy.scale(pose,frame.getWidth(),frame.getHeight(),getWidth(),getHeight());
        float width=frame.getWidth()*scale,height=frame.getHeight()*scale;
        float left=FullBodyAnchorPolicy.left(pose,frame.getWidth(),getWidth(),scale,fraction);
        destination.set(left,getHeight()-height,left+width,getHeight());
        paint.setAlpha(255);
        int local=canvas.save();canvas.translate(left,getHeight()-height);canvas.scale(scale,scale);
        if(breathingEnabled){
            if(mode==2 && speechStance.laughing() && pose<181 && FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.JOY))FullBodyBreathPolicy.laugh(breathMesh,frame.getWidth(),frame.getHeight(),elapsed);
            else FullBodyBreathPolicy.mesh(breathMesh,frame.getWidth(),frame.getHeight(),elapsed);
            canvas.drawBitmapMesh(frame,FullBodyBreathPolicy.COLUMNS,FullBodyBreathPolicy.ROWS,breathMesh,0,null,0,paint);
        }else canvas.drawBitmap(frame,0,0,paint);
        faceAnimator.draw(canvas,frame,pose,android.os.SystemClock.uptimeMillis(),mode==2&&animateMouth);
        if(mode==2||continuity.holds(android.os.SystemClock.uptimeMillis()))situationEffects.draw(canvas,frame,FullBodyFaceAnimator.features(pose),android.os.SystemClock.uptimeMillis(),situationMask);
        rokidGlasses.update(android.os.SystemClock.uptimeMillis(),mode,transitionFrames!=null||previousBitmap!=null);
        rokidGlasses.draw(canvas,frame,pose);
        canvas.restoreToCount(local);
        if(layered)canvas.restoreToCount(layer);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) { generation++; loaded = -1; requestImage(); }
    }
    @Override protected void onVisibilityChanged(View view, int visibility) {
        super.onVisibilityChanged(view, visibility);
        if (isShown()) requestImage(); else release();
        reconcileMotion();
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); requestImage(); reconcileMotion(); }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility); reconcileMotion();
    }
    @Override protected void onDetachedFromWindow() { release(); super.onDetachedFromWindow(); }
    private void release() {
        continuity.clear();
        dramaticPreview=dramaticLoaded=-1;dramaticRequest++;dramaticLoading=false;
        dramaticFrames=null;dramaticBreathStarted=0;dramaticAuto=dramaticCycle=false;situationMask=0;for(FullBodyFaceAnimator face:dramaticFaces)face.clear();for(FullBodyFaceAnimator face:approvedFaces)face.clear();
        unwellFrames=null;unwellLoading=false;unwellActive=false;unwellStarted=0;
        approvedMotionPreview=-1;rearStopFrames=null;rearStopActive=false;rearStopLoading=false;rearStopStarted=0;
        heavyBreathFrames=null;heavyBreathActive=false;heavyBreathLoading=false;heavyBreathStarted=0;
        conversationMotionFrames=null;conversationMotionKind=conversationMotionCached=-1;conversationMotionStarted=0;conversationMotionLoading=false;
        faceAnimator.clear();
        generation++; lastDrawnBitmap=null;previousBitmap=null;reactions.reset();variation.reset();speechStance.reset();speechExpression=-1;bitmap = null; motionFrames=null;idleFrames = null; loaded = -1; transitionFrames=null; seatedFrames=null; transitionCache[0]=null; transitionCache[1]=null;linkFrames=null;cachedLink=-1;performanceFrames=null;performanceRequested=performanceLoading=false;performanceStarted=0;manualPenRequested=false;manualPenStarted=0;manualPenKind=manualPenPose=-1;activityFrames=null;activityCached=-1;activityLoading=false;
        if (motionTick != null) removeCallbacks(motionTick);
    }
}





