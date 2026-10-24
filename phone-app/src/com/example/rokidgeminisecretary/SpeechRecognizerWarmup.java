package com.example.rokidgeminisecretary;
import android.app.*;import android.os.*;import android.content.*;import android.speech.*;
/** Binds recognition without starting a listening session or supplying audio. Main thread only. */
final class SpeechRecognizerWarmup {
 private SpeechRecognizer ready;private long created,lastRequest=-30000;
 static boolean fresh(long created,long now){return created>=0&&now>=created&&now-created<30000;}
 void prepare(Activity owner){owner.runOnUiThread(()->{long now=SystemClock.elapsedRealtime();if(now-lastRequest<10000)return;lastRequest=now;if(ready!=null&&fresh(created,now))return;clear();try{
  SpeechRecognizer r=SpeechRecognizer.createSpeechRecognizer(owner);ready=r;created=now;
  if(Build.VERSION.SDK_INT>=33){Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP");intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);r.checkRecognitionSupport(intent,owner.getMainExecutor(),new RecognitionSupportCallback(){public void onSupportResult(RecognitionSupport result){}public void onError(int code){}});}
  new Handler(Looper.getMainLooper()).postDelayed(()->{if(ready==r)clear();},30000);
 }catch(RuntimeException unavailable){clear();}});}
 SpeechRecognizer take(){SpeechRecognizer r=ready;boolean valid=r!=null&&fresh(created,SystemClock.elapsedRealtime());ready=null;if(valid)return r;if(r!=null)r.destroy();return null;}
 void clear(){if(ready!=null){try{ready.destroy();}catch(RuntimeException ignored){}ready=null;}}
}
