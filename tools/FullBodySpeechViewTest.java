package com.example.rokidkeyboardbridge;
import android.content.*;
import android.content.res.*;
import android.graphics.Bitmap;
import android.os.Looper;
import java.lang.reflect.Method;
/** Read-only Android decoder check against the candidate APK, without installing it. */
public final class FullBodySpeechViewTest {
 public static void main(String[] args) throws Exception {
  Looper.prepareMainLooper();
  Class<?> at=Class.forName("android.app.ActivityThread");
  Object thread=at.getMethod("systemMain").invoke(null);
  Context system=(Context)at.getMethod("getSystemContext").invoke(thread);
  AssetManager assets=AssetManager.class.getConstructor().newInstance();
  if ((int)AssetManager.class.getMethod("addAssetPath",String.class).invoke(assets,args[0])==0) throw new AssertionError("APK assets");
  Resources resources=new Resources(assets,system.getResources().getDisplayMetrics(),system.getResources().getConfiguration());
  Context context=new ContextWrapper(system){
   public android.content.pm.ApplicationInfo getApplicationInfo(){android.content.pm.ApplicationInfo info=new android.content.pm.ApplicationInfo();info.sourceDir=args[0];return info;}
   public AssetManager getAssets(){return assets;}
   public Resources getResources(){return resources;}
  };
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,240,616);java.lang.reflect.Field breathing=FullBodyMascotView.class.getDeclaredField("breathingEnabled");breathing.setAccessible(true);breathing.setBoolean(view,false);Method decode=FullBodyMascotView.class.getDeclaredMethod("decode",int.class,int.class);decode.setAccessible(true);Bitmap pose=(Bitmap)decode.invoke(view,0,616);
  String[] names={"bitmap","loaded","mode","animateMouth","motionStarted"};Object[] values={pose,0,2,false,android.os.SystemClock.uptimeMillis()-220};for(int i=0;i<names.length;i++){java.lang.reflect.Field f=FullBodyMascotView.class.getDeclaredField(names[i]);f.setAccessible(true);f.set(view,values[i]);}
  Method draw=FullBodyMascotView.class.getDeclaredMethod("onDraw",android.graphics.Canvas.class);draw.setAccessible(true);Bitmap speaking=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(speaking));java.lang.reflect.Field mode=FullBodyMascotView.class.getDeclaredField("mode");mode.setAccessible(true);mode.setInt(view,0);Bitmap silent=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(silent));float scale=FullBodyScalePolicy.scale(0,pose.getWidth(),pose.getHeight(),240,616);float[] landmarks=FullBodyRealisticFeatures.ALL[0];float mouthBottom=616-pose.getHeight()*scale+(landmarks[15]+landmarks[16])*pose.getWidth()/landmarks[0]*scale;int changes=0;for(int y=0;y<616;y++)for(int x=0;x<240;x++)if(silent.getPixel(x,y)!=speaking.getPixel(x,y)){changes++;if(y>mouthBottom)throw new AssertionError("Body moved");}if(changes==0)throw new AssertionError("Narration suppresses mouth");
  try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-speech-proof.png")){speaking.compress(Bitmap.CompressFormat.PNG,100,out);}System.out.println("PASS narration animateMouth=false still moves full-body mouth in mode2; changed pixels="+changes);
  java.lang.reflect.Field loaded=FullBodyMascotView.class.getDeclaredField("loaded");loaded.setAccessible(true);loaded.setInt(view,-1);mode.setInt(view,2);
  Method requested=FullBodyMascotView.class.getDeclaredMethod("requestedPose");requested.setAccessible(true);
  view.beginSpeech("触らないでください。別の話にしましょう。");view.setExpression(0);
  for(String beat:new String[]{"触らないでください。","別の話にしましょう。"}){view.setSpeechText(beat);int selected=(int)requested.invoke(view);if(!FullBodyPoseCatalog.ALL[selected].supports(FullBodyPoseCatalog.Kind.REFUSE))throw new AssertionError("View dropped refusal: "+selected);}
  view.beginSpeech("ありがとうございます。");view.setSpeechText("ありがとうございます。");if(!FullBodyPoseCatalog.ALL[(int)requested.invoke(view)].supports(FullBodyPoseCatalog.Kind.JOY))throw new AssertionError("Next response stuck refusing");
  view.beginSpeech("...");view.setSpeechText("（激しく喘いでいる）");if(!FullBodyPoseCatalog.ALL[(int)requested.invoke(view)].supports(FullBodyPoseCatalog.Kind.PANT))throw new AssertionError("View dropped breathing reaction");
  view.beginSpeech("...");view.setSpeechText("説明します。");view.setSpeechText("ご案内します。");if((int)requested.invoke(view)!=6)throw new AssertionError("Direct full-body ID was remapped using portrait semantics");
  mode.setInt(view,0);view.setSpeechText("");view.setExpression(4);if((int)requested.invoke(view)!=4)throw new AssertionError("Standing waiting frames not connected");
  view.beginSpeech("ひゃっ、耳に息を吹きかけられてびっくりしちゃいました。");
  mode.setInt(view,2);
  if(!FullBodyPoseCatalog.ALL[(int)requested.invoke(view)].supports(FullBodyPoseCatalog.Kind.SURPRISE))throw new AssertionError("Whole answer surprise missing");
  FullBodySpeechStance.Cue startled=FullBodySpeechStance.classify("はぁ…っ、少々動揺してしまいます…。");
  if(startled==null||startled.kind!=FullBodyPoseCatalog.Kind.SURPRISE||startled.intensity!=1)throw new AssertionError("Latest ear reply missed");
  String actualReply="耳に息を吹きかけるのは、くすぐったいような、少しドキッとするような感覚がありますね。";
  FullBodySpeechStance.Cue actual=FullBodySpeechStance.classify(actualReply);
  if(actual==null||actual.kind!=FullBodyPoseCatalog.Kind.SURPRISE||actual.intensity!=1)throw new AssertionError("Actual device reply missed mild surprise");
  view.beginSpeech(actualReply);int reaction=(int)requested.invoke(view);
  Bitmap reactionImage=(Bitmap)decode.invoke(view,reaction,616);
  String[] injected={"bitmap","loaded","idleFrames","motionFrames","previousBitmap","lastDrawnBitmap","transitionFrames","linkFrames"};
  Object[] replacements={reactionImage,reaction,new Bitmap[]{pose,pose,pose,pose,pose,pose},null,null,null,null,null};
  for(int i=0;i<injected.length;i++){java.lang.reflect.Field f=FullBodyMascotView.class.getDeclaredField(injected[i]);f.setAccessible(true);f.set(view,replacements[i]);}
  Bitmap reactionProof=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(reactionProof));
  java.lang.reflect.Field shown=FullBodyMascotView.class.getDeclaredField("lastDrawnPose");shown.setAccessible(true);
  if(shown.getInt(view)!=reaction)throw new AssertionError("Idle artwork overwrote spoken reaction");
  try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-surprise-v430.png")){reactionProof.compress(Bitmap.CompressFormat.PNG,100,out);}
  System.out.println("PASS actual device reply mild surprise, actual Canvas reaction retained with stale idle cache");
  FullBodySpeechStance stance=new FullBodySpeechStance();
  stance.pose("あはは、思わず笑ってしまいました。",4);
  if(!stance.laughing())throw new AssertionError("Laughing cue missing");
  stance.pose("ありがとうございます。",4);
  if(stance.laughing()||FullBodySpeechStance.laughCue("彼女は笑っています。")||FullBodySpeechStance.laughCue("笑っていません。"))throw new AssertionError("Spurious laughter");
  float[] a=new float[330],b=new float[330];FullBodyBreathPolicy.laugh(a,240,598,90);FullBodyBreathPolicy.mesh(b,240,598,90);
  boolean movement=false;for(int i=0;i<a.length;i++){if(a[i]!=b[i])movement=true;}
  if(!movement||a[1]!=b[1]||a[329]!=b[329])throw new AssertionError("Laugh motion missing or changes head/floor");
  view.setUserContext("胸を揉む");view.beginSpeech("そのようなことはお控えください。");mode.setInt(view,2);
  for(String beat:new String[]{"そのようなことはお控えください。","説明します。","ありがとうございます。"}){
   view.setSpeechText(beat);int selected=(int)requested.invoke(view);
   if(!FullBodyPoseCatalog.ALL[selected].supports(FullBodyPoseCatalog.Kind.REFUSE))throw new AssertionError("Scene not wired to actual View: "+selected);
  }
  int guarded=(int)requested.invoke(view);Bitmap guardedImage=(Bitmap)decode.invoke(view,guarded,616);
  for(String field:new String[]{"bitmap","loaded","previousBitmap","lastDrawnBitmap"}){java.lang.reflect.Field f=FullBodyMascotView.class.getDeclaredField(field);f.setAccessible(true);f.set(view,field.equals("bitmap")?guardedImage:field.equals("loaded")?guarded:null);}
  Bitmap guardedProof=Bitmap.createBitmap(240,616,Bitmap.Config.ARGB_8888);draw.invoke(view,new android.graphics.Canvas(guardedProof));
  if(shown.getInt(view)!=guarded)throw new AssertionError("Guarded pose not actually drawn");
  try(java.io.FileOutputStream out=new java.io.FileOutputStream("/data/local/tmp/loki-scene-v432.png")){guardedProof.compress(Bitmap.CompressFormat.PNG,100,out);}
  view.setUserContext("ごめんなさい、もうやめます。");view.beginSpeech("安心しました。");
  requested.invoke(view);Thread.sleep(1000);
  if(!FullBodyPoseCatalog.ALL[(int)requested.invoke(view)].supports(FullBodyPoseCatalog.Kind.RECOVERY))throw new AssertionError("Actual View remains refusing after apology");
  System.out.println("PASS scene context reaches actual View and Canvas, guidance suppressed, apology exits");
  System.out.println("PASS whole answer surprise; laughter limited to own amusement and shoulder movement");
  System.out.println("PASS real view overrides generic presentation with refusal, maintains boundary, resets for next answer");
 }
}
