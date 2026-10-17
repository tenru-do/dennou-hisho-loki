package com.example.lokisecuritytest;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Rect;import android.view.*;import android.widget.*;import java.lang.reflect.*;
public final class ReadButtonNativeInstrumentation extends Instrumentation{
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();Activity a=startActivitySync(new Intent().setComponent(new ComponentName(c.getPackageName(),c.getPackageName()+".MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  waitForIdleSync();SystemClock.sleep(600);
  runOnMainSync(()->{try{Field hold=a.getClass().getDeclaredField("hudHoldUntil");hold.setAccessible(true);hold.setLong(a,System.currentTimeMillis()+120000L);Method show=a.getClass().getDeclaredMethod("showControlsTemporarily");show.setAccessible(true);show.invoke(a);}catch(Exception e){throw new RuntimeException(e);}});
  waitForIdleSync();
  final Throwable[] failure={null};final String[] metrics={""};
  runOnMainSync(()->{try{
   Field field=a.getClass().getDeclaredField("buttonPanel");field.setAccessible(true);LinearLayout row=(LinearLayout)field.get(a);if(row.getChildCount()!=9)throw new AssertionError("expected 9 controls");
   View read=row.getChildAt(1);if(!(read instanceof Button)||!"READ".contentEquals(((Button)read).getText()))throw new AssertionError("READ not next to CAM");
   StringBuilder sizes=new StringBuilder();for(int i=0;i<row.getChildCount();i++){Button b=(Button)row.getChildAt(i);Rect rect=new Rect();if(!b.getGlobalVisibleRect(rect)||rect.left<0||rect.right>480||rect.width()<=0||b.getPaint().measureText(b.getText().toString())>b.getWidth()-b.getPaddingLeft()-b.getPaddingRight())throw new AssertionError("control clipped: "+b.getText());sizes.append(b.getText()).append(':').append(rect.width()).append(' ');}
   row.getChildAt(0).requestFocus();Method navigate=a.getClass().getDeclaredMethod("handleFocusNavigation",View.class,int.class);navigate.setAccessible(true);if(!((Boolean)navigate.invoke(a,row.getChildAt(0),KeyEvent.KEYCODE_DPAD_RIGHT))||!read.hasFocus())throw new AssertionError("READ navigation focus");metrics[0]=sizes.toString();
  }catch(Throwable e){failure[0]=e;}});
  if(failure[0]!=null)throw new RuntimeException(failure[0]);
  result.putString("readButtonNative","PASS 9 controls visible, labels fit, READ next to CAM, D-pad focus");result.putString("widths",metrics[0]);result.putString("cameraAndGemini","NOT_TRIGGERED");finish(Activity.RESULT_OK,result);
 }catch(Throwable error){result.putString("failure",error.toString());finish(Activity.RESULT_CANCELED,result);}}
}
