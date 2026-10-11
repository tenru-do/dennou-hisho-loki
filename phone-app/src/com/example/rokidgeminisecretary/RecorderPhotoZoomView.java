package com.example.rokidgeminisecretary;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Matrix-only pinch/pan: reuses the existing decoded bitmap at every zoom level. */
final class RecorderPhotoZoomView extends ImageView {
 private final Matrix transform=new Matrix();
 private final float[] values=new float[9];
 private final RectF bounds=new RectF();
 private final ScaleGestureDetector pinch;
 private final GestureDetector gestures;
 private float fitScale=1;
 RecorderPhotoZoomView(Context context){
  super(context);setScaleType(ScaleType.MATRIX);setBackgroundColor(android.graphics.Color.BLACK);
  setContentDescription("写真。2本指で拡大縮小、ドラッグで移動、ダブルタップで拡大または全体表示");
  pinch=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
   public boolean onScale(ScaleGestureDetector detector){zoom(detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());return true;}
  });
  gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
   public boolean onDown(MotionEvent event){return true;}
   public boolean onScroll(MotionEvent first,MotionEvent current,float dx,float dy){if(!pinch.isInProgress()){transform.postTranslate(-dx,-dy);apply();}return true;}
   public boolean onDoubleTap(MotionEvent event){if(scale()>fitScale*1.1f)fit();else zoom(2.5f,event.getX(),event.getY());return true;}
   public boolean onSingleTapUp(MotionEvent event){performClick();return true;}
  });
 }
 public void setImageBitmap(android.graphics.Bitmap bitmap){super.setImageBitmap(bitmap);if(transform!=null)fit();}
 private float scale(){transform.getValues(values);return values[Matrix.MSCALE_X];}
 protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);fit();}
 private void fit(){if(getDrawable()==null||getWidth()==0||getHeight()==0)return;float w=getDrawable().getIntrinsicWidth(),h=getDrawable().getIntrinsicHeight();fitScale=Math.min(getWidth()/w,getHeight()/h);transform.setScale(fitScale,fitScale);transform.postTranslate((getWidth()-w*fitScale)/2,(getHeight()-h*fitScale)/2);apply();}
 private void zoom(float factor,float x,float y){float current=scale();if(current<=0)return;float target=Math.max(fitScale,Math.min(fitScale*8,current*factor));transform.postScale(target/current,target/current,x,y);apply();}
 private void apply(){if(getDrawable()==null)return;bounds.set(0,0,getDrawable().getIntrinsicWidth(),getDrawable().getIntrinsicHeight());transform.mapRect(bounds);float dx=bounds.width()<=getWidth()?(getWidth()-bounds.width())/2-bounds.left:bounds.left>0?-bounds.left:bounds.right<getWidth()?getWidth()-bounds.right:0;float dy=bounds.height()<=getHeight()?(getHeight()-bounds.height())/2-bounds.top:bounds.top>0?-bounds.top:bounds.bottom<getHeight()?getHeight()-bounds.bottom:0;transform.postTranslate(dx,dy);setImageMatrix(transform);invalidate();}
 public boolean onTouchEvent(MotionEvent event){getParent().requestDisallowInterceptTouchEvent(true);pinch.onTouchEvent(event);gestures.onTouchEvent(event);return true;}
 public boolean performClick(){super.performClick();return true;}
}
