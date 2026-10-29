package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import java.io.InputStream;

/** The reviewed atlas is the single identity used for idle, listening and speech. */
final class ApprovedFaceRenderer {
    private final PortraitAttentionMotion attention=new PortraitAttentionMotion();
    private Bitmap atlas;
    private PortraitExpressionRenderer expressions;
    private KissPortraitRenderer kisses;
    void cancelPreviews(){if(expressions!=null)expressions.cancelPreview();if(kisses!=null)kisses.clearPreview();}
    void clearContactGestures(){if(kisses!=null){kisses.clearPreview();kisses.scene(-1,0);}if(expressions!=null)expressions.clearContactCues();}
    void virtualContact(VirtualContactPolicy.Frame frame,long now){if(expressions!=null)expressions.virtualContact(frame,now);}
    void previewKiss(int type,long now){if(kisses!=null)kisses.preview(type,now);}
    void kissScene(int type,long elapsed){if(kisses!=null)kisses.scene(type,elapsed);}
    void previewSequence(int cell,long now){expressions.preview(8+Math.max(0,Math.min(15,cell)),now);}
 void attention(float x,float y,long now){if(expressions!=null)expressions.rapport.life.performance.button(x,y,now);}
    void rapportUser(String text,long now){if(expressions!=null)expressions.rapport.user(text,now);}
    void rapportAnswer(String text,FullBodyPoseCatalog.Kind kind,long now){if(expressions!=null)expressions.rapport.answer(text,kind,now);}
    boolean waitingForReaction(long now){return expressions!=null&&expressions.rapport.waiting(now);}
    void previewConversationGesture(int kind,long now){if(expressions!=null)expressions.rapport.life.event(kind,now,true);}
    void previewRapport(long now){if(kisses!=null)kisses.clearPreview();if(expressions!=null)expressions.rapport.preview(now);}
    boolean needsFastFrames(long now){return (expressions!=null&&(expressions.rapport.active(now)||expressions.rapport.life.moving(now)||expressions.virtualMoving()||expressions.transitioning(now)))||kisses!=null&&kisses.needsFastFrames(now);}
    boolean hasLifePortraits(){return expressions!=null&&expressions.ready();}
    void preview(int cell,long now){if(kisses!=null)kisses.clearPreview();if(expressions!=null)expressions.preview(cell,now);}
    void react(long now,boolean touching,int direction){if(expressions!=null)expressions.react(now,touching,direction);}
    private Bitmap angledAtlas, frontAtlas;
    private boolean frontFacing, previousFacing;
    private final Paint image = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint black = new Paint();
    private final Paint motionInk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final RectF region = new RectF();
    private final Path contour = new Path();
    private int previous = -1;
    private long changedAt;
    private Bitmap faceFrame;
    private int faceCell=-1;
    private boolean uiPriority;
    private int lifeMode;
    void setUiPriority(boolean value) { uiPriority=value; }
    Bitmap[] retainedBitmaps() { Bitmap[] more=expressions==null?new Bitmap[]{null,null}:expressions.retained();Bitmap[] extra=kisses==null?new Bitmap[]{null,null}:kisses.retained();return new Bitmap[]{atlas,faceFrame,more[0],more[1],more.length>2?more[2]:null,more.length>3?more[3]:null,extra[0],extra[1]}; }
    void release() {
        if(expressions!=null)expressions.release();expressions=null;
        if(kisses!=null)kisses.release();kisses=null;
        if(faceFrame!=null && faceFrame!=atlas && !faceFrame.isRecycled())faceFrame.recycle();
        if(atlas!=null && !atlas.isRecycled())atlas.recycle();
        atlas=angledAtlas=frontAtlas=faceFrame=null;faceCell=-1;
    }
    private final float[] faceMesh=new float[49*61*2];
    private static float warp(float x,float y,float cx,float cy,float rx,float ry,float scale) {
        float dx=(x-cx)/rx,dy=(y-cy)/ry;
        float weight=Math.max(0f,1f-dx*dx-dy*dy);
        return (y-cy)*(scale-1f)*weight;
    }
    private void drawFaceMesh(Canvas canvas,int cell,boolean speaking,long now) {
        if(faceCell!=cell || faceFrame==null) {
            if(faceFrame!=null)faceFrame.recycle();
            faceFrame=Bitmap.createBitmap(atlas,source.left,source.top,source.width(),source.height());
            faceCell=cell;
        }
        float[] f=landmarks(cell,frontFacing);
        float phase=(now%4100)/1000f;
        float blink=phase<.24f ? .12f+.88f*Math.abs(phase-.12f)/.12f : 1f;
        float articulation=(float)(.5+.35*Math.sin(now/113.0)+.15*Math.sin(now/67.0));
        boolean smile=cell==1||cell==2||cell==5||cell==7||cell==12||cell==29||cell==30;
        boolean stableSmile=CoolSmilePolicy.portrait(cell);
        float mouth=stableSmile?1f:speaking?(smile?.72f+.70f*articulation:.72f+.75f*articulation):1f+(smile?.14f:.025f)*(float)Math.sin(now/750.0);
        float closedScale=1f+.18f*(float)Math.sin(now/900.0);
        float leftScale=FullBodyPortraitGeometry.CLOSED[cell][0]?closedScale:blink;
        float rightScale=FullBodyPortraitGeometry.CLOSED[cell][1]?closedScale:blink;
        float gaze=PortraitLifeMotion.gaze(now,lifeMode,uiPriority)+attention.gaze(now,uiPriority);
        int cols=uiPriority?24:48,rows=uiPriority?30:60,index=0;
        for(int row=0;row<=rows;row++)for(int col=0;col<=cols;col++) {
            float x=col/(float)cols,y=row/(float)rows,dy=0f;
            for(int eye=0;eye<2;eye++) {
                float scale=eye==0?leftScale:rightScale;
                dy+=warp(x,y,f[eye*2],f[eye*2+1],stableSmile?.065f:.08f,stableSmile?.025f:.045f,scale);
                if(FullBodyPortraitGeometry.CLOSED[cell][eye]){
                    float dx=(x-f[eye*2])/.07f,ey=(y-f[eye*2+1])/.03f;
                    dy+=.0035f*(float)Math.sin(now/450.0)*Math.max(0f,1f-dx*dx-ey*ey);
                }
            }
            dy+=warp(x,y,f[4],f[5],smile?.12f:.10f,FullBodyPortraitGeometry.OPEN[cell]?.09f:.06f,mouth);
            float eyeWeight=0;
            if(gaze!=0)for(int eye=0;eye<2;eye++){float ex=(x-f[eye*2])/.085f,ey=(y-f[eye*2+1])/.045f;eyeWeight+=Math.max(0,1-ex*ex-ey*ey);}
            faceMesh[index++]=destination.left+(x+gaze*eyeWeight)*destination.width();
            faceMesh[index++]=destination.top+(y+dy)*destination.height();
        }
        canvas.drawBitmapMesh(faceFrame,cols,rows,faceMesh,0,null,0,image);
        if(speaking&&!FullBodyPortraitGeometry.OPEN[cell]&&articulation>.12f){
            float x=destination.left+f[4]*destination.width(),y=destination.top+f[5]*destination.height();
            float eyeDistance=(float)Math.hypot(f[2]-f[0],f[3]-f[1]);
            float rx=Math.max(.021f,Math.min(.048f,eyeDistance*.20f))*destination.width();
            float gap=(.003f+.012f*articulation)*destination.height();
            int save=canvas.save();canvas.rotate((float)Math.toDegrees(Math.atan2(f[3]-f[1],f[2]-f[0]))+(stableSmile?CoolSmilePolicy.mouthEyeAngle():0),x,y);
            contour.reset();contour.moveTo(x-rx,y);contour.cubicTo(x-rx*.5f,y-gap,x+rx*.5f,y-gap,x+rx,y);
            contour.cubicTo(x+rx*.5f,y+gap,x-rx*.5f,y+gap,x-rx,y);contour.close();
            motionInk.setColor(Color.rgb(0,18,20));motionInk.setStyle(Paint.Style.FILL);canvas.drawPath(contour,motionInk);
            motionInk.setColor(Color.rgb(0,118,124));motionInk.setStyle(Paint.Style.STROKE);motionInk.setStrokeWidth(Math.max(.5f,destination.width()*.002f));canvas.drawPath(contour,motionInk);canvas.restoreToCount(save);
        }
    }
    ApprovedFaceRenderer(Context context) {
        CharacterMotionPreferences.load(context);
        expressions=new PortraitExpressionRenderer(context);kisses=new KissPortraitRenderer(context);
        try (InputStream stream = context.getAssets().open("loki_face_cool_v559.png")) {
            Bitmap original=BitmapFactory.decodeStream(stream);
            if(original==null)throw new IllegalStateException("Full-body portrait atlas missing");
            atlas=FullBodyPortraitInk.render(original);
            original.recycle();
        } catch (Exception error) { android.util.Log.e("ApprovedFace", "Approved atlas failed", error); }
        angledAtlas=atlas;
        // Speaking keeps the face's actual source angle and identity.
        frontAtlas=atlas;
        ink.setColor(Color.rgb(0, 210, 220));
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeCap(Paint.Cap.ROUND);
        black.setColor(Color.BLACK);
    }
    boolean ready() { return atlas != null && !atlas.isRecycled(); }
    static int cellFor(int expression) {
        if (expression >= 0 && expression < 37) return expression;
        switch (expression) {
            case 37: case 46: return 27;
            case 38: case 39: return 4;
            case 40: case 53: return 29;
            case 41: case 48: return 6;
            case 42: return 0;
            case 43: return 8;
            case 44: case 45: return 11;
            case 47: case 49: return 15;
            case 50: return 28;
            case 51: case 52: return 30;
            case 54: return 17;
            case 55: case 56: case 57: return 2;
            default: return 20;
        }
    }
    private static boolean closedEyes(int cell) {
        return cell == 13 || cell == 19 || cell == 20 || cell == 21 || cell == 22
            || cell == 24 || cell == 25 || cell == 28 || cell == 29 || cell == 32
            || cell == 33 || cell == 34 || cell == 35;
    }
    static boolean hasTears(int cell) {
        return cell==10||cell==21||cell==28||cell==36;
    }
    static boolean hasSweat(int cell) {
        return cell==3||cell==8||cell==15||cell==18||cell==20||cell==27||cell==32||cell==34;
    }
    // Each portrait selects registered eye and lip anchors, including tilted and raised heads.
    static float[] landmarks(int cell,boolean front) {
        return FullBodyPortraitGeometry.LANDMARKS[cell].clone();
    }
    private static float[] legacyLandmarks(int cell,boolean front) {
        float[] a=front ? new float[]{.415f,.615f,.705f,.615f,.56f,.81f}
            : new float[]{.545f,.595f,.795f,.595f,.665f,.80f};
        if(front) {
            if(cell==26)return new float[]{.42f,.52f,.665f,.52f,.55f,.705f};
        } else {
            switch(cell) {
                case 6:return new float[]{.48f,.56f,.735f,.595f,.595f,.78f};
                case 24:case 25:return new float[]{.48f,.61f,.70f,.63f,.615f,.805f};
                case 26:return new float[]{.545f,.455f,.765f,.425f,.750f,.555f};
                case 33:return new float[]{.50f,.59f,.73f,.625f,.615f,.805f};
            }
        }
        return a;
    }
    private void drawDroplet(Canvas canvas,float x,float y,float radius,float opacity) {
        if(opacity<=0f)return;
        float px=destination.left+x*destination.width(),py=destination.top+y*destination.height();
        float rw=radius*destination.width(),rh=rw*1.6f;
        ink.setAlpha(Math.round(190f*opacity));
        contour.reset();contour.moveTo(px,py-rh);
        contour.cubicTo(px-rw*1.3f,py,px-rw,py+rh,px,py+rh);
        contour.cubicTo(px+rw,py+rh,px+rw*1.3f,py,px,py-rh);
        canvas.drawPath(contour,ink);ink.setAlpha(255);
    }
    private void map(float l, float t, float r, float b) {
        region.set(destination.left + destination.width()*l, destination.top + destination.height()*t,
            destination.left + destination.width()*r, destination.top + destination.height()*b);
    }
    private final Rect detailSource = new Rect();
    private final RectF detailDestination = new RectF();
    private final Rect edgeSource = new Rect();
    private final RectF edgeDestination = new RectF();
    private void deformRegion(Canvas canvas,float l,float t,float r,float b,float verticalScale) {
        map(l,t,r,b);
        detailSource.set(source.left+Math.round(source.width()*l),source.top+Math.round(source.height()*t),
            source.left+Math.round(source.width()*r),source.top+Math.round(source.height()*b));
        float cy=region.centerY(),half=region.height()*.5f*verticalScale;
        detailDestination.set(region.left,cy-half,region.right,cy+half);
        int save=canvas.save();canvas.clipRect(region);
        // Extend the patch's skin edges instead of leaving black rectangles at closed eyes/lips.
        if(verticalScale<1f) {
            edgeSource.set(detailSource.left,detailSource.top,detailSource.right,detailSource.top+1);
            edgeDestination.set(region.left,region.top,region.right,detailDestination.top);
            canvas.drawBitmap(atlas,edgeSource,edgeDestination,image);
            edgeSource.set(detailSource.left,detailSource.bottom-1,detailSource.right,detailSource.bottom);
            edgeDestination.set(region.left,detailDestination.bottom,region.right,region.bottom);
            canvas.drawBitmap(atlas,edgeSource,edgeDestination,image);
        }
        canvas.drawBitmap(atlas,detailSource,detailDestination,image);canvas.restoreToCount(save);
    }
    void draw(Canvas canvas, int width, int height, int expression, boolean speaking, int mode) {
        lifeMode=mode;
        frontFacing = mode == 1 || speaking;
        drawAt(canvas, width, height, expression, speaking, SystemClock.uptimeMillis());
    }
    // Explicit time permits native Canvas verification without changing user settings.
    void drawAt(Canvas canvas, int width, int height, int expression, boolean speaking, long now) {
        if (!ready() || width <= 0 || height <= 0) return;attention.mode(lifeMode,now);
        if(kisses!=null&&kisses.draw(canvas,width,height,lifeMode,uiPriority,now))return;
        if(expressions!=null&&expressions.draw(canvas,width,height,expression,lifeMode,speaking,uiPriority,now))return;
        atlas=frontFacing && frontAtlas!=null ? frontAtlas : angledAtlas;
        int cell = PortraitLifeMotion.face(cellFor(expression),now,lifeMode,uiPriority);
        if (cell != previous || previousFacing != frontFacing) { previous = cell; previousFacing=frontFacing; changedAt = now; }
        int cw = atlas.getWidth()/8, ch = atlas.getHeight()/5;
        source.set(cell%8*cw, cell/8*ch, (cell%8+1)*cw, (cell/8+1)*ch);
        float h = Math.min(height*.94f, width*(float)ch/cw);
        float w = h*cw/ch;
        float x = (width-w)*.5f, y = (height-h)*.45f;
        float breath = (float)Math.sin(now/680.0)*h*.005f;
        float nod=(PortraitLifeMotion.nod(now,lifeMode,uiPriority)+attention.nod(now,uiPriority))*h;
        destination.set(x, y+breath+nod, x+w, y+h+breath+nod);
        int save=canvas.save();
        canvas.rotate((float)Math.sin(now/2100.0)*.65f+PortraitLifeMotion.tilt(now,lifeMode,uiPriority)+attention.tilt(now,uiPriority), destination.centerX(), destination.bottom);
        // Brief dim-to-bright transition shows one aligned face, avoiding doubled features.
        image.setAlpha(Math.round(255f*(.75f+.25f*Math.min(1f,Math.max(0,now-changedAt)/180f))));
        int artworkClip=canvas.save();
        float topInset=0f;
        canvas.clipRect(destination.left,destination.top+destination.height()*topInset,
            destination.right,destination.bottom-destination.height()*.01f);
        drawFaceMesh(canvas,cell,speaking,now);
        canvas.restoreToCount(artworkClip);
        ink.setStrokeWidth(Math.max(.8f,w*.005f));
        float[] features = landmarks(cell,frontFacing);
        float blinkPhase=(now%4100)/1000f;
        float eyeScale=blinkPhase<.24f ? .12f+.88f*Math.abs(blinkPhase-.12f)/.12f : 1f;
        /* Facial movement is rendered by the continuous mesh above.
        for(int eye=0;eye<2;eye++) {
            float cx=features[eye*2],cy=features[eye*2+1];
            float scale=FullBodyPortraitGeometry.CLOSED[cell][eye] ? 1f+.04f*(float)Math.sin(now/900.0) : eyeScale;
            if(Math.abs(scale-1f)>.001f)
                deformRegion(canvas,cx-.07f,cy-.032f,cx+.07f,cy+.032f,scale);
        }
        float mx=features[4],my=features[5];
        float mouthScale=1f+(speaking?.16f:.025f)*(float)Math.sin(now/(speaking?105.0:750.0));
        boolean open=FullBodyPortraitGeometry.OPEN[cell];
        float mh=open?.062f:.036f;
        deformRegion(canvas,mx-.07f,my-mh,mx+.07f,my+mh,mouthScale);
        */
        float mx=features[4],my=features[5];
        if(hasTears(cell)) {
            for(int eye=0;eye<2;eye++) {
                float phase=((now+eye*720)%2200)/2200f;
                drawDroplet(canvas,features[eye*2],features[eye*2+1]+.025f+phase*.16f,
                    .007f,(float)Math.sin(phase*Math.PI));
            }
        }
        if(hasSweat(cell)) {
            float phase=(now%3500)/3500f;
            drawDroplet(canvas,Math.min(.88f,features[2]+.10f),features[3]-.12f+phase*.16f,.006f,
                (float)Math.sin(phase*Math.PI));
        }
        // A small nonsexual saliva droplet accompanies severe breathless/disarray states only.
        if(expression>=58 && expression<=61) {
            float phase=(now%2700)/2700f;
            drawDroplet(canvas,mx+.045f,my+.025f+phase*.065f,.0045f,
                (float)Math.sin(phase*Math.PI));
        }
        canvas.restoreToCount(save);
    }
}
