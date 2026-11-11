package com.example.rokidkeyboardbridge;
import android.graphics.Canvas;import android.graphics.Matrix;

/** Translation-only crown protection after outer reaction/approach transforms. */
final class CharacterHeadGuard {
 private final Matrix base=new Matrix(),inverse=new Matrix(),current=new Matrix();
 private final Matrix relative=new Matrix(),shift=new Matrix(),corrected=new Matrix();
 private final float[] crown=new float[6];
 void begin(Canvas canvas){canvas.getMatrix(base);}
 void apply(Canvas canvas,int width,int height){
  if(width<=0||height<=0||!base.invert(inverse))return;
  canvas.getMatrix(current);relative.setConcat(inverse,current);
  float center=width*.5f,radius=CharacterViewport.eyeSpan(width,height)*3,margin=CharacterViewport.headMargin(height);
  crown[0]=center-radius;crown[1]=margin;crown[2]=center;crown[3]=margin;crown[4]=center+radius;crown[5]=margin;
  relative.mapPoints(crown);
  float dy=margin-Math.min(crown[1],Math.min(crown[3],crown[5]));
  if(!Float.isFinite(dy)||dy<=.001f)return;
  // Correct in view coordinates, then restore the parent's matrix. This also
  // works when the view is mirrored or its parent is translated/rotated/scaled.
  shift.setTranslate(0,dy);corrected.setConcat(shift,relative);
  current.setConcat(base,corrected);canvas.setMatrix(current);
 }
}
