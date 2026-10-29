package com.example.rokidkeyboardbridge;
import android.graphics.Canvas;
/** A bounded visual overlay: never changes speech, scene, pose or expression state. */
final class ButtonReactionMotion {
    private long started=-1;
    private boolean contact,closing;
    private int variation;
    private int region=CharacterContactPoint.SHOULDER;
    void region(int value){region=Math.max(0,Math.min(2,value));}
    private int swipe;private int touchStyle;
    static final long DURATION_MS=900;
    void begin(long now,boolean touching,boolean close){touchStyle=touching&&started>=0&&now>=started&&now-started<850?2:touching&&CharacterTouchTempo.duration(now)>=450?1:0;started=now;contact=touching;closing=close;variation=(variation+1)%3;swipe=0;}
    void swipe(long now,boolean touching,int direction){begin(now,touching,false);swipe=direction;}
    boolean active(long now){return started>=0&&now>=started&&now-started<DURATION_MS;}
    void clear(){started=-1;}
    void apply(Canvas canvas,int width,int height,long now){
        if(!active(now))return;
        double p=(now-started)/(double)DURATION_MS;
        float wave=Math.min(1,(now-started+16)/80f)*(float)Math.pow(Math.sin(Math.PI*(.1+.9*p)),2)*CharacterMotionTuning.scale();
        if(contact&&touchStyle==1)wave*=.55f;else if(contact&&touchStyle==2)wave*=(float)Math.sin((now-started)/90.0)*.65f;
        // A brief directional response overlays the current scene without changing it.
        if(swipe!=0){
            float horizontal=swipe==1?1:swipe==2?-1:0;
            float vertical=swipe==3?1:swipe==4?-1:0;
            canvas.translate(horizontal*width*(contact?.035f:.018f)*wave,vertical*height*.020f*wave);
            canvas.rotate(horizontal*(contact?3.5f:1.8f)*wave,width*.5f,height*.70f);
        }
        // Contact is a short recoil/shoulder-like lean; ordinary actions get a nod.
        canvas.translate((contact?.018f:variation==2?-.012f:0)*width*wave,(contact?-.012f:variation==0?.012f:.022f)*height*wave);
        canvas.rotate((contact?-2.2f:closing?-1.2f:variation==0?-1.8f:variation==2?2.2f:.7f)*wave,width*.5f,height*.70f);
        if(!contact)canvas.scale(1,1-.018f*wave,width*.5f,height*.85f);
        if(contact){float pivot=CharacterContactPoint.vertical(region,false);canvas.rotate((region==CharacterContactPoint.HAND?.7f:region==CharacterContactPoint.CHEEK?-1.1f:1.5f)*wave,width*.5f,height*pivot);}
    }
    int face(int original,long now){return active(now)&&!contact&&(original==0||original==14)?1:original;}
}
