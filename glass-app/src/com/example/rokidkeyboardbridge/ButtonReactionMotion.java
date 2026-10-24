package com.example.rokidkeyboardbridge;
import android.graphics.Canvas;
/** A bounded visual overlay: never changes speech, scene, pose or expression state. */
final class ButtonReactionMotion {
    private long started=-1;
    private boolean contact,closing;
    static final long DURATION_MS=900;
    void begin(long now,boolean touching,boolean close){started=now;contact=touching;closing=close;}
    boolean active(long now){return started>=0&&now>=started&&now-started<DURATION_MS;}
    void clear(){started=-1;}
    void apply(Canvas canvas,int width,int height,long now){
        if(!active(now))return;
        double p=(now-started)/(double)DURATION_MS;
        float wave=(float)Math.pow(Math.sin(Math.PI*p),2);
        // Contact is a short recoil/shoulder-like lean; ordinary actions get a nod.
        canvas.translate((contact?.018f:0)*width*wave,(contact?-.012f:.018f)*height*wave);
        canvas.rotate((contact?-2.2f:closing?-1.2f:.7f)*wave,width*.5f,height*.70f);
        canvas.scale(1,1-(contact?.012f:.018f)*wave,width*.5f,height*.85f);
    }
    int face(int original,long now){return active(now)&&!contact&&(original==0||original==14)?1:original;}
}
