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
    public void setPreviewPose(int id){previewPose=id<0?-1:Math.min(180,id);requestImage();reconcileMotion();}
    private String userContext="";
    private boolean observedScene;
    public void setUserContext(String prompt){userContext=prompt==null?"":prompt;observedScene=false;speechStance.observed(null);}
    public void setObservedEmotion(String emotion){setObservedEmotion(emotion,1);}
    public void setObservedEmotion(String emotion,int intensity){observedScene=true;speechStance.observed(new FullBodySpeechStance.Cue(AmbientScenePolicy.kind(emotion),AmbientScenePolicy.intensity(intensity)));}
    public void beginSpeech(String answer){
        previewPose=-1;speechStance.begin(answer);
        FullBodySemanticReaction.Reaction semantic=FullBodySemanticReaction.find(answer);
        if(semantic!=null)speechStance.semantic(semantic);
        if(!observedScene)speechStance.scene(userContext,answer);speechExpression=speechStance.pose(answer,loaded);
        FullBodySpeechStance.Cue local=FullBodyScenePolicy.evaluate(userContext,answer);
        if(local==null)local=FullBodySpeechStance.classify(answer);
        if(getVisibility()==VISIBLE)FullBodyReactionAudit.record(getContext(),userContext,answer,speechExpression,semantic==null?null:semantic.cue,local);
        requestImage();reconcileMotion();
    }
    public void setSpeechText(String text){
        if(text==null||text.length()==0){speechStance.reset();speechExpression=-1;}
        else speechExpression=speechStance.pose(text,loaded);
        if(speechExpression>=0)android.util.Log.i("LokiFullBody","Speech reaction pose="+speechExpression+" tags="+FullBodyPoseCatalog.ALL[speechExpression].kinds);
        requestImage();reconcileMotion();
    }
    private int manualPenKind=-1,manualPenPose=-1;
    private boolean manualPenRequested;
    private long manualPenStarted;
    public void startPenPickup(){
        cancelPerformance();previewPose=-1;
        manualPenPose=loaded>=0?loaded:FullBodyMotionPolicy.poseForState(expression,mode);
        manualPenKind=FullBodyMotionPolicy.isSeated(manualPenPose)?2:3;
        manualPenRequested=true;manualPenStarted=0;requestImage();reconcileMotion();
    }
    private boolean pickingPen(long now){return manualPenRequested||(manualPenStarted>0&&now-manualPenStarted<FullBodyPenCommand.TOTAL);}
    private Bitmap[] performanceFrames;
    private boolean performanceRequested,performanceLoading;
    private long performanceStarted;
    public void startPerformance(){manualPenRequested=false;manualPenStarted=0;previewPose=-1;performanceRequested=true;performanceStarted=0;requestImage();reconcileMotion();}
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
        if(previewPose>=0)return previewPose;
        if(pickingPen(now))return manualPenPose;
        if(performing(now))return 4;
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
            requestImage();
            invalidate();
            postDelayed(this, 33L);
        }
    };

    public void setMode(int value) { if(value==0&&mode!=0){idleStarted=android.os.SystemClock.uptimeMillis();speechStance.reset();speechExpression=-1;} mode = value; requestImage(); reconcileMotion(); }
    public void setHostResumed(boolean value) { hostResumed = value; reconcileMotion(); }
    private void reconcileMotion() {
        if (motionTick == null) return;
        removeCallbacks(motionTick);
        if (hostResumed && isShown() && getWindowVisibility() == View.VISIBLE
                && bitmap!=null) post(motionTick);
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
        if (bitmap == null || bitmap.isRecycled()) { requestImage(); return; }
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
        int activity=mode==0 && previewPose<0 && !performing(now) && !pickingPen(now)
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
            blendStarted=now;blendDuration=FullBodyIdleActivity.isFrame(displayedPose)!=FullBodyIdleActivity.isFrame(lastDrawnPose)?FullBodyBlendPolicy.POSE_MS:displayedPose>=240?220L:70L;
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
        faceAnimator.clear();
        generation++; lastDrawnBitmap=null;previousBitmap=null;reactions.reset();variation.reset();speechStance.reset();speechExpression=-1;bitmap = null; motionFrames=null;idleFrames = null; loaded = -1; transitionFrames=null; seatedFrames=null; transitionCache[0]=null; transitionCache[1]=null;linkFrames=null;cachedLink=-1;performanceFrames=null;performanceRequested=performanceLoading=false;performanceStarted=0;manualPenRequested=false;manualPenStarted=0;manualPenKind=manualPenPose=-1;activityFrames=null;activityCached=-1;activityLoading=false;
        if (motionTick != null) removeCallbacks(motionTick);
    }
}






