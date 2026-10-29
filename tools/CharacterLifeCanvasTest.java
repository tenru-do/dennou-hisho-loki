package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.content.res.*;import android.graphics.*;import android.os.*;import android.view.*;import java.lang.reflect.*;import java.util.*;import java.io.*;
/** Offscreen Android rendering with synthetic state; does not launch the user's app. */
public final class CharacterLifeCanvasTest {
 static int passed;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);passed++;}
 public static int run(Context system,String source)throws Exception{
  String[] args={source,new File(system.getFilesDir(),"preview.png").getPath()};
  AssetManager assets=AssetManager.class.getConstructor().newInstance();ok((Integer)AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,args[0])!=0,"assets");
  Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());
  Context context=new android.view.ContextThemeWrapper(system,android.R.style.Theme_Material_Dialog){public AssetManager getAssets(){return assets;}public Resources getResources(){return resources;}};
  ApprovedFaceRenderer renderer=new ApprovedFaceRenderer(context);ok(renderer.ready(),"face atlas ready");
  Set<Integer> hashes=new HashSet<>();Bitmap sheet=Bitmap.createBitmap(480*4,360*2,Bitmap.Config.ARGB_8888);Canvas page=new Canvas(sheet);page.drawColor(Color.BLACK);
  Field portraitField=ApprovedFaceRenderer.class.getDeclaredField("expressions");portraitField.setAccessible(true);Object portrait=portraitField.get(renderer);
  Field cue=PortraitExpressionRenderer.class.getDeclaredField("cue"),cueAt=PortraitExpressionRenderer.class.getDeclaredField("cueAt");cue.setAccessible(true);cueAt.setAccessible(true);
  for(int i=0;i<8;i++){cue.setInt(portrait,i);cueAt.setLong(portrait,1000);Bitmap b=Bitmap.createBitmap(480,360,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.BLACK);renderer.drawAt(c,480,360,0,false,1400);int[] pixels=new int[480*360];b.getPixels(pixels,0,480,0,0,480,360);hashes.add(Arrays.hashCode(pixels));int visible=0;for(int p:pixels)if((p&0xffffff)!=0)visible++;ok(visible>5000,"visible portrait "+i);page.drawBitmap(b,(i%4)*480,(i/4)*360,null);b.recycle();}
  ok(hashes.size()==8,"eight distinct rendered portraits");try(FileOutputStream out=new FileOutputStream(args[1])){sheet.compress(Bitmap.CompressFormat.PNG,100,out);}sheet.recycle();
  renderer.drawAt(new Canvas(Bitmap.createBitmap(480,360,Bitmap.Config.ARGB_8888)),480,360,6,false,40200);Field previous=ApprovedFaceRenderer.class.getDeclaredField("previous");previous.setAccessible(true);ok(previous.getInt(renderer)==6,"semantic worry preserved");renderer.release();
  passed+=ButtonReactionTest.run(context,args[0]);
  MainActivity owner=new MainActivity();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(owner,context);
  AlertDialog dialog=new AlertDialog.Builder(context).create();Window window=dialog.getWindow();final int[] forwarded={0};
  window.setCallback((Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class[]{Window.Callback.class},(p,m,a)->{forwarded[0]++;return m.getReturnType()==boolean.class?Boolean.FALSE:null;}));
  NavigationDialogWake.install(dialog,owner);int before=forwarded[0];
  window.getCallback().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));ok(forwarded[0]==before+1,"key forwarded once");
  MotionEvent touch=MotionEvent.obtain(0,1,MotionEvent.ACTION_DOWN,10,10,0);window.getCallback().dispatchTouchEvent(touch);touch.recycle();ok(forwarded[0]==before+2,"touch forwarded once");
  window.getCallback().onWindowFocusChanged(true);ok(forwarded[0]==before+3,"non-input callback unchanged");
  System.out.println("PASS "+passed+" native face/button/window callback assertions");return passed;
 }
}
