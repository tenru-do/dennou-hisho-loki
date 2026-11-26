package loki.probe;
import android.content.*;import android.hardware.camera2.*;import android.os.*;import java.lang.reflect.*;import java.util.*;
/** Real platform marshaling test, no openCamera/session/capture call. */
public final class ProbeCameraRequestMetadata {
 @SuppressWarnings("unchecked") public static void main(String[]args){try{
  if(Looper.getMainLooper()==null)Looper.prepareMainLooper();Class<?> t=Class.forName("android.app.ActivityThread");Object at=t.getDeclaredMethod("systemMain").invoke(null);Context c=(Context)t.getDeclaredMethod("getSystemContext").invoke(at);
  CameraManager manager=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);CameraCharacteristics metadata=manager.getCameraCharacteristics(manager.getCameraIdList()[0]);
  Class<?> nativeType=Class.forName("android.hardware.camera2.impl.CameraMetadataNative");Method nativeCopy=CameraCharacteristics.class.getDeclaredMethod("getNativeCopy");nativeCopy.setAccessible(true);Object nativeMetadata=nativeCopy.invoke(metadata);CaptureRequest.Builder builder=null;
  for(Constructor<?> constructor:CaptureRequest.Builder.class.getDeclaredConstructors()){
   Class<?>[] types=constructor.getParameterTypes();if(types.length==0||types[0]!=nativeType)continue;Object[] values=new Object[types.length];boolean supported=true;
   for(int i=0;i<types.length;i++){if(types[i]==nativeType)values[i]=nativeMetadata;else if(types[i]==boolean.class)values[i]=false;else if(types[i]==int.class)values[i]=-1;else if(types[i]==String.class)values[i]="0";else if(Set.class.isAssignableFrom(types[i]))values[i]=Collections.emptySet();else supported=false;}
   if(!supported)continue;constructor.setAccessible(true);builder=(CaptureRequest.Builder)constructor.newInstance(values);break;
  }
  if(builder==null)throw new IllegalStateException("No compatible request builder");boolean found=false;
  for(CaptureRequest.Key<?> key:metadata.getAvailableSessionKeys())if(key.getName().equals("org.codeaurora.qcamera3.sessionParameters.enableMFNR")){
   CaptureRequest.Key<int[]> typed=(CaptureRequest.Key<int[]>)key;builder.set(typed,new int[]{0});int[] value=builder.build().get(typed);if(value==null||value.length!=1||value[0]!=0)throw new AssertionError("Vendor array readback");found=true;
  }
  if(!found)throw new IllegalStateException("MFNR session key unavailable");
  System.out.println("PASS real Android vendor MFNR request metadata marshaling and readback: int[1]={0}; camera was not opened");System.exit(0);
 }catch(Throwable failure){while(failure.getCause()!=null)failure=failure.getCause();System.err.println(failure.getClass().getName()+":"+failure.getMessage());for(StackTraceElement frame:failure.getStackTrace())System.err.println(frame);System.exit(2);}}
}
