package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.ImageFormat;import android.hardware.camera2.*;import android.hardware.camera2.params.StreamConfigurationMap;import android.media.*;import android.os.*;import android.util.*;
import java.io.IOException;import java.util.*;import java.util.concurrent.*;import java.util.function.BooleanSupplier;

/** JPEG-only persistent session. No preview, repeating requests, video, disk cache or image queue. */
final class KeepCameraSession implements KeepSessionController.Backend {
 private final Context context;private final CaptureLease lease;private final HandlerThread thread;private final Handler handler;
 private final CountDownLatch ready=new CountDownLatch(1),closed=new CountDownLatch(1);
 private volatile boolean closing,openRequested;private volatile Throwable failure;
 private CameraDevice device;private CameraCaptureSession session;private ImageReader reader;private CameraCharacteristics characteristics;
 private volatile Frame frame;private boolean aeStable;private int orientation,evMin,evMax,currentEv,iso;private float evStep;private long shutter;
 private static final class Frame {
  final CountDownLatch done=new CountDownLatch(1);final BooleanSupplier cancelled;final KeepAutoExposureCycle automatic=new KeepAutoExposureCycle();final long started=SystemClock.elapsedRealtime();
  final int mode;CaptureRequest.Builder request;byte[] jpeg;Throwable failure;boolean inFlight,wantsNext;int warmup;
  Frame(BooleanSupplier cancelled,int mode){this.cancelled=cancelled;this.mode=mode;}
 }
 KeepCameraSession(Context context,BooleanSupplier cancelled)throws Exception{
  this.context=context.getApplicationContext();lease=CaptureLease.acquire(CaptureLease.CAMERA,cancelled,3);
  thread=new HandlerThread("KeepCameraSession");thread.start();handler=new Handler(thread.getLooper());
  handler.post(()->open(cancelled));
  try{await(ready,cancelled,8000);if(failure!=null)throwFailure(failure);if(closing||session==null)throw new IOException("KEEP session unavailable");}
  catch(Exception error){try{close();}catch(Exception closeError){error.addSuppressed(closeError);}throw error;}
 }
 private static void await(CountDownLatch latch,BooleanSupplier cancelled,long duration)throws Exception{
  long end=SystemClock.elapsedRealtime()+duration;
  while(!cancelled.getAsBoolean()&&SystemClock.elapsedRealtime()<end){if(latch.await(50,TimeUnit.MILLISECONDS))return;}
  if(cancelled.getAsBoolean())throw new InterruptedException("KEEP yields camera");throw new IOException("KEEP camera timed out");
 }
 private static void throwFailure(Throwable error)throws Exception{if(error instanceof Exception)throw (Exception)error;throw new IOException("KEEP camera failed",error);}
 private void open(BooleanSupplier cancelled){try{
  if(closing||cancelled.getAsBoolean()){ready.countDown();finishClosed();return;}
  CameraManager manager=(CameraManager)context.getSystemService(Context.CAMERA_SERVICE);String[] ids=manager.getCameraIdList();if(ids.length==0)throw new IOException("No KEEP camera");
  characteristics=manager.getCameraCharacteristics(ids[0]);Integer rotation=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);orientation=rotation==null?0:rotation;
  StreamConfigurationMap streams=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);Size[] sizes=streams==null?null:streams.getOutputSizes(ImageFormat.JPEG);Size chosen=null;
  if(sizes!=null)for(Size size:sizes)if(size.getWidth()<=1920&&size.getHeight()<=1080&&(chosen==null||size.getWidth()*size.getHeight()>chosen.getWidth()*chosen.getHeight()))chosen=size;
  if(chosen==null&&sizes!=null)for(Size size:sizes)if(chosen==null||size.getWidth()*size.getHeight()<chosen.getWidth()*chosen.getHeight())chosen=size;
  if(chosen==null||chosen.getWidth()*(long)chosen.getHeight()>1920L*1080)throw new IOException("No bounded KEEP JPEG output");
  Range<Integer> range=characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);Rational step=characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
  if(range!=null&&step!=null&&step.floatValue()>0){evMin=range.getLower();evMax=range.getUpper();evStep=step.floatValue();}
  reader=ImageReader.newInstance(chosen.getWidth(),chosen.getHeight(),ImageFormat.JPEG,2);reader.setOnImageAvailableListener(this::imageAvailable,handler);
  openRequested=true;
  manager.openCamera(ids[0],new CameraDevice.StateCallback(){
   @Override public void onOpened(CameraDevice camera){device=camera;if(closing){camera.close();return;}try{
    CameraCaptureSession.StateCallback callbacks=new CameraCaptureSession.StateCallback(){
     @Override public void onConfigured(CameraCaptureSession configured){session=configured;if(closing){configured.close();camera.close();return;}ready.countDown();Log.i("LokiKeepSession","persistent JPEG session ready");}
     @Override public void onConfigureFailed(CameraCaptureSession configured){session=configured;fail(new IOException("KEEP session configuration failed"));}
    };android.hardware.camera2.params.SessionConfiguration config=new android.hardware.camera2.params.SessionConfiguration(android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,Collections.singletonList(new android.hardware.camera2.params.OutputConfiguration(reader.getSurface())),command->handler.post(command),callbacks);CaptureRequest.Builder parameters=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);boolean mfnrDisabled=false;java.util.List<CaptureRequest.Key<?>> keys=characteristics.getAvailableSessionKeys();if(keys!=null)for(CaptureRequest.Key<?> key:keys)if(key.getName().equals("org.codeaurora.qcamera3.sessionParameters.enableMFNR")){@SuppressWarnings("unchecked") CaptureRequest.Key<int[]> integerArrayKey=(CaptureRequest.Key<int[]>)key;parameters.set(integerArrayKey,new int[]{0});mfnrDisabled=true;}if(mfnrDisabled)config.setSessionParameters(parameters.build());Log.i("LokiKeepSession","single-frame session, MFNR disabled="+mfnrDisabled);camera.createCaptureSession(config);
   }catch(Exception error){fail(error);}}
   @Override public void onDisconnected(CameraDevice camera){device=camera;CameraPriority.SHARED.defer(SystemClock.elapsedRealtime(),5000);fail(new InterruptedException("KEEP camera preempted"));}
   @Override public void onError(CameraDevice camera,int error){device=camera;boolean busy=error==ERROR_CAMERA_IN_USE||error==ERROR_MAX_CAMERAS_IN_USE;if(busy)CameraPriority.SHARED.defer(SystemClock.elapsedRealtime(),5000);fail(busy?new InterruptedException("KEEP yields external camera"):new IOException("KEEP camera device error"));}
   @Override public void onClosed(CameraDevice camera){finishClosed();}
  },handler);
 }catch(Exception error){openRequested=false;if(error instanceof CameraAccessException&&(((CameraAccessException)error).getReason()==CameraAccessException.CAMERA_IN_USE||((CameraAccessException)error).getReason()==CameraAccessException.MAX_CAMERAS_IN_USE)){CameraPriority.SHARED.defer(SystemClock.elapsedRealtime(),5000);error=new InterruptedException("KEEP yields external camera");}fail(error);}}
 private void fail(Throwable error){failure=error;Frame current=frame;if(current!=null){current.failure=error;current.done.countDown();}ready.countDown();requestClose();}
 @Override public boolean alive(){return !closing&&failure==null&&closed.getCount()!=0;}
 @Override public byte[] capture(BooleanSupplier cancelled)throws Exception{
  if(!alive())throw new IOException("KEEP session closed");Frame current=new Frame(cancelled,KeepExposureSettings.mode(context));
  handler.post(()->{if(closing||cancelled.getAsBoolean()){current.failure=new InterruptedException("KEEP cancelled");current.done.countDown();return;}if(frame!=null){current.failure=new IOException("KEEP capture already in flight");current.done.countDown();return;}frame=current;try{
   current.request=device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);current.request.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);java.util.List<CaptureRequest.Key<?>> supported=characteristics.getAvailableCaptureRequestKeys();if(supported!=null&&supported.contains(CaptureRequest.CONTROL_ENABLE_ZSL))current.request.set(CaptureRequest.CONTROL_ENABLE_ZSL,false);int[] edgeModes=characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);if(edgeModes!=null)for(int mode:edgeModes)if(mode==CaptureRequest.EDGE_MODE_FAST){current.request.set(CaptureRequest.EDGE_MODE,mode);break;}int[] noiseModes=characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);if(noiseModes!=null)for(int mode:noiseModes)if(mode==CaptureRequest.NOISE_REDUCTION_MODE_FAST){current.request.set(CaptureRequest.NOISE_REDUCTION_MODE,mode);break;}current.request.addTarget(reader.getSurface());current.request.set(CaptureRequest.JPEG_QUALITY,(byte)85);current.request.set(CaptureRequest.JPEG_ORIENTATION,orientation);current.request.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
   int base=evStep>0?KeepExposurePolicy.compensation(current.mode,evStep,evMin,evMax):0;
   aeStable=false;currentEv=base;if(evStep>0)current.request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,currentEv);scheduleFrame(current);
  }catch(Exception error){current.failure=error;current.done.countDown();}});
  try{await(current.done,cancelled,8000);if(current.failure!=null)throwFailure(current.failure);if(failure!=null)throwFailure(failure);if(current.jpeg==null)throw new IOException("KEEP JPEG missing");return current.jpeg;}
  finally{handler.post(()->{if(frame==current)frame=null;current.jpeg=null;});}
 }
 private void scheduleFrame(Frame current){handler.post(()->requestFrame(current));}
 private void requestFrame(Frame current){
  if(current!=frame||closing||current.cancelled.getAsBoolean()){current.failure=new InterruptedException("KEEP cancelled");current.done.countDown();return;}
  if(current.inFlight){current.wantsNext=true;return;}current.inFlight=true;current.wantsNext=false;
  try{session.capture(current.request.build(),new CameraCaptureSession.CaptureCallback(){
   @Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest request,TotalCaptureResult result){if(frame!=current)return;current.inFlight=false;Integer ae=result.get(CaptureResult.CONTROL_AE_STATE);aeStable=ae!=null&&(ae==CaptureResult.CONTROL_AE_STATE_CONVERGED||ae==CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED||ae==CaptureResult.CONTROL_AE_STATE_LOCKED);Long exposure=result.get(CaptureResult.SENSOR_EXPOSURE_TIME);Integer sensitivity=result.get(CaptureResult.SENSOR_SENSITIVITY);shutter=exposure==null?0:exposure;iso=sensitivity==null?0:sensitivity;if(current.wantsNext)scheduleFrame(current);else if(current.jpeg!=null)current.done.countDown();}
   @Override public void onCaptureFailed(CameraCaptureSession s,CaptureRequest request,CaptureFailure failure){current.failure=failure.getReason()==CaptureFailure.REASON_FLUSHED&&current.cancelled.getAsBoolean()?new InterruptedException("KEEP capture preempted"):new IOException("KEEP frame failed: reason="+failure.getReason()+", frame="+failure.getFrameNumber()+", imageCaptured="+failure.wasImageCaptured());current.done.countDown();}
  },handler);}catch(Exception error){current.failure=error;current.done.countDown();}
 }
 private void imageAvailable(ImageReader source){Frame current=frame;try(Image image=source.acquireLatestImage()){
  if(image==null||current==null||closing)return;
  if(current.cancelled.getAsBoolean()){current.failure=new InterruptedException("KEEP cancelled");current.done.countDown();return;}

  if(current.warmup<6&&SystemClock.elapsedRealtime()-current.started<1500&&(current.warmup<2||!aeStable)){current.warmup++;scheduleFrame(current);return;}
  if(current.automatic.correcting()&&current.automatic.discardSettling(SystemClock.elapsedRealtime())){scheduleFrame(current);return;}
  java.nio.ByteBuffer buffer=image.getPlanes()[0].getBuffer();if(buffer.remaining()<4||buffer.remaining()>8000000)throw new IOException("KEEP JPEG size invalid");byte[] jpeg=new byte[buffer.remaining()];buffer.get(jpeg);
  if(current.mode==0&&current.automatic.needsAnalysis()&&evStep>0&&(evMin<0||evMax>0)){
   float ev=current.automatic.decide(KeepAutoExposureAnalyzer.analyze(jpeg),shutter,iso,evStep);int selected=KeepAutoExposurePolicy.compensation(ev,evStep,evMin,evMax);
   if(selected!=currentEv){currentEv=selected;current.request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,selected);current.automatic.applied(SystemClock.elapsedRealtime());aeStable=false;scheduleFrame(current);return;}
  }
  current.jpeg=jpeg;if(!current.inFlight)current.done.countDown();
 }catch(Throwable error){if(current!=null){current.failure=error;current.done.countDown();}}}
 private void requestClose(){closing=true;ready.countDown();Frame current=frame;if(current!=null){current.failure=current.failure==null?new InterruptedException("KEEP session closing"):current.failure;current.done.countDown();}
  try{if(session!=null){session.close();session=null;}}finally{if(device!=null){device.close();}else if(!openRequested)finishClosed();}
 }
 private void finishClosed(){if(closed.getCount()==0)return;closing=true;try{if(reader!=null){reader.close();reader=null;}}finally{frame=null;lease.close();closed.countDown();thread.quitSafely();Log.i("LokiKeepSession","session and image buffers released");}}
 @Override public void close()throws Exception{
  if(closed.getCount()==0)return;handler.post(this::requestClose);boolean interrupted=Thread.interrupted();long end=SystemClock.elapsedRealtime()+3000;
  try{while(closed.getCount()!=0&&SystemClock.elapsedRealtime()<end){try{closed.await(50,TimeUnit.MILLISECONDS);}catch(InterruptedException e){interrupted=true;}}if(closed.getCount()!=0)throw new IOException("KEEP camera close not confirmed; camera ownership retained");}
  finally{if(interrupted)Thread.currentThread().interrupt();}
 }
}
