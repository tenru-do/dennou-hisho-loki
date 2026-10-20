package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Short observations between utterances; never saves recordings or photos to disk. */
final class TopicObservationCapture {
    interface AudioProvider { byte[] read(java.util.function.BooleanSupplier cancelled) throws Exception; }
    static JSONObject capture(Context context, JSONObject request, BooleanSupplier cancelled) throws Exception {
        return capture(context, request, cancelled, null);
    }
    static JSONObject capture(Context context, JSONObject request, BooleanSupplier cancelled, AudioProvider sharedAudio) throws Exception {
        JSONObject result = new JSONObject().put("requestId", request.getString("requestId"))
                .put("revision", request.getLong("revision"));
        String mode = request.optString("mode");
        android.util.Log.i("CameraTopic","TOPIC capture begin mode="+mode);
        if ("sound".equals(mode) || "both".equals(mode)) {
            try {
                if (context.checkSelfPermission("android.permission.RECORD_AUDIO") != 0) throw new IllegalStateException();
                result.put("audio", Base64.encodeToString(sharedAudio == null ? audio(context, cancelled, request.optInt("maxAudioMs",4000)) : wav(compatibleAudio(sharedAudio.read(cancelled),request.optInt("maxAudioMs",4000))), Base64.NO_WRAP));
                if (sharedAudio != null) result.put("audioDuringAssistantSpeech", true);
            } catch (Exception error) { result.put("audioUnavailable", true); }
        }
        if (!cancelled.getAsBoolean() && ("vision".equals(mode) || "both".equals(mode))) {
            PhotoRecorderService keep=PhotoRecorderService.active;MeetingRecorderService meeting=MeetingRecorderService.active;
            String resource=keep==null&&meeting==null?ResourceLoad.optional(context):"";
            if(!resource.isEmpty()){result.put("imageSkipped",resource);}
            else if(keep!=null) {
                // KEEP owns the camera and image-save cycle. Optional TOPIC vision
                // only reads an already-saved, recent frame; never opens another camera.
                result.put("imageSkipped","KEEP撮影・保存優先：画像判定は待機中");
                try {
                    PhotoRecorderService.TopicPhoto saved=keep.awaitTopicPhoto(cancelled);
                    if(saved!=null){result.put("image",Base64.encodeToString(saved.jpeg,Base64.NO_WRAP))
                        .put("imageSource","keep_saved_photo").put("imageCapturedAt",saved.capturedAt).put("imageAgeMs",saved.ageMs);result.remove("imageSkipped");}
                    else result.put("imageSkipped",keep.topicImageWaitReason());
                }catch(Exception unavailable){result.put("imageSkipped",keep.topicImageWaitReason()); /* KEEP continues. */ }
            }else if(meeting!=null&&!meeting.reserveCamera()){
                result.put("imageSkipped",meeting.cameraWaitReason());
            }else {
            try (AutoCloseable mascotPause = context instanceof MainActivity
                    ? ((MainActivity) context).pauseMascotForCamera() : () -> {}) {
                if (context.checkSelfPermission("android.permission.CAMERA") != 0) throw new IllegalStateException();
                long capturedAt=System.currentTimeMillis(),captureStart=SystemClock.elapsedRealtime();
                byte[] fresh=photo(context,cancelled);long age=SystemClock.elapsedRealtime()-captureStart;
                if(age<0||age>15000)result.put("imageSkipped","取得に時間がかかったため画像を使用しません");
                else result.put("image", Base64.encodeToString(fresh, Base64.NO_WRAP)).put("imageSource","topic_camera").put("imageCapturedAt",capturedAt).put("imageAgeMs",age);
            } catch (Exception error) {
                if(PhotoRecorderService.active!=null)result.put("imageSkipped","KEEP撮影開始を優先：画像判定は待機中");
                else result.put("imageUnavailable", true);
            }
            }
        }
        if (cancelled.getAsBoolean()) throw new InterruptedException("observation cancelled");
        android.util.Log.i("CameraTopic","TOPIC capture completed image="+result.has("image")+" audio="+result.has("audio"));
        return result;
    }
    private static byte[] audio(Context context, BooleanSupplier cancelled) throws Exception {return audio(context,cancelled,12000);}
    private static byte[] compatibleAudio(byte[] pcm,int limit){if(pcm==null)return null;int maximum=(int)Math.max(4000,Math.min(12000,limit))*32;return pcm.length<=maximum?pcm:java.util.Arrays.copyOfRange(pcm,pcm.length-maximum,pcm.length);}
    private static byte[] audio(Context context, BooleanSupplier cancelled,int limit) throws Exception {
        try (CaptureLease lease = CaptureLease.acquire(CaptureLease.MICROPHONE, cancelled)) {
        int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) throw new IllegalStateException("microphone unavailable");
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, 6400));
        MicrophoneRouting.preferUsb(context, recorder);
        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException();
            recorder.startRecording();
            android.util.Log.i("CameraTopic","audio recording started");
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException();
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] frame = new byte[640];
            long started = SystemClock.elapsedRealtime();
            ObservationAudioPolicy timing=new ObservationAudioPolicy(limit);
            while (!cancelled.getAsBoolean() && !timing.stop(SystemClock.elapsedRealtime()-started,pcm.size())) {
                int count = recorder.read(frame, 0, Math.min(frame.length, ObservationAudioPolicy.MAX_BYTES - pcm.size()), AudioRecord.READ_NON_BLOCKING);
                if (count < 0) throw new IllegalStateException();
                if (count > 0) { MicrophoneRouting.boostAndLog(frame, count, "TOPIC", MediaRecorder.AudioSource.DEFAULT); pcm.write(frame, 0, count); timing.observe(SystemClock.elapsedRealtime()-started,Pcm16Gain.meanAbsolute(frame,count)); } else Thread.sleep(5L);
            }
            if (cancelled.getAsBoolean() || pcm.size() < 3200) throw new InterruptedException();
            return wav(pcm.toByteArray());
        } finally { try { recorder.stop(); } catch (Exception ignored) {} recorder.release(); android.util.Log.i("CameraTopic","audio released"); }
        }
    }
    private static byte[] wav(byte[] data) {
            if(data==null||data.length<3200||data.length>ObservationAudioPolicy.MAX_BYTES||(data.length&1)!=0)throw new IllegalArgumentException("invalid observation PCM");
            ByteBuffer wav = ByteBuffer.allocate(44 + data.length).order(ByteOrder.LITTLE_ENDIAN);
            wav.put(new byte[]{'R','I','F','F'}).putInt(36 + data.length).put(new byte[]{'W','A','V','E','f','m','t',' '});
            wav.putInt(16).putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000);
            wav.putShort((short) 2).putShort((short) 16).put(new byte[]{'d','a','t','a'}).putInt(data.length).put(data);
            return wav.array();
    }
    static byte[] recorderPhoto(Context context, BooleanSupplier cancelled) throws Exception {return photo(context,cancelled,true);}
    private static byte[] photo(Context context, BooleanSupplier cancelled) throws Exception {
        return photo(context,()->cancelled.getAsBoolean()||PhotoRecorderService.active!=null,false);
    }
    private static byte[] photo(Context context, BooleanSupplier cancelled, boolean recorder) throws Exception {
        try (CaptureLease lease = CaptureLease.acquire(CaptureLease.CAMERA, cancelled)) {
        HandlerThread thread = new HandlerThread("TopicCamera");thread.start();
        Handler cameraHandler=new Handler(thread.getLooper());
        CountDownLatch done=new CountDownLatch(1),openCompleted=new CountDownLatch(1),deviceClosed=new CountDownLatch(1);
        android.hardware.camera2.CameraDevice[] device={null};android.hardware.camera2.CameraCaptureSession[] session={null};
        android.media.ImageReader[] reader={null};byte[][] jpeg={null};int[] orientation={0};Throwable[] failure={null};
        java.util.concurrent.atomic.AtomicBoolean closing=new java.util.concurrent.atomic.AtomicBoolean();
        Runnable[] nextExposureFrame={null};int[] exposureFrames={0};long[] exposureStart={0};boolean[] aeStable={false};
        try {
            cameraHandler.post(()->{
                try {
                    if(cancelled.getAsBoolean()){openCompleted.countDown();deviceClosed.countDown();done.countDown();return;}
                    android.hardware.camera2.CameraManager manager=(android.hardware.camera2.CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
                    String[] ids=manager.getCameraIdList();if(ids.length==0)throw new IllegalStateException("No TOPIC camera");
                    String id=ids[0];android.hardware.camera2.CameraCharacteristics characteristics=manager.getCameraCharacteristics(id);
                    Integer rotation=characteristics.get(android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION);orientation[0]=rotation==null?0:rotation;
                    android.hardware.camera2.params.StreamConfigurationMap streams=characteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    android.util.Size[] sizes=streams.getOutputSizes(android.graphics.ImageFormat.JPEG);android.util.Size chosen=null;
                    for(android.util.Size size:sizes)if(size.getWidth()<=(recorder?1920:640)&&size.getHeight()<=(recorder?1080:480)
                            &&(chosen==null||size.getWidth()*size.getHeight()>chosen.getWidth()*chosen.getHeight()))chosen=size;
                    if(chosen==null)for(android.util.Size size:sizes)if(chosen==null||size.getWidth()*size.getHeight()<chosen.getWidth()*chosen.getHeight())chosen=size;
                    if(chosen==null)throw new IllegalStateException("No bounded JPEG size");
                    reader[0]=android.media.ImageReader.newInstance(chosen.getWidth(),chosen.getHeight(),android.graphics.ImageFormat.JPEG,2);
                    android.util.Log.i("TopicCamera","Camera2 JPEG "+chosen+" maxImages=2; no preview surface");
                    reader[0].setOnImageAvailableListener(source->{
                        try(android.media.Image image=source.acquireLatestImage()) {
                            if(image!=null&&!closing.get()){
                                // Warm-up JPEGs are closed without allocating/copying their buffers.
                                // Only REC waits for auto-exposure; existing TOPIC capture stays unchanged.
                                if(recorder&&exposureFrames[0]<6&&SystemClock.elapsedRealtime()-exposureStart[0]<1500L
                                        &&(exposureFrames[0]<2||!aeStable[0])&&!cancelled.getAsBoolean()){
                                    exposureFrames[0]++;cameraHandler.post(nextExposureFrame[0]);return;
                                }
                                java.nio.ByteBuffer buffer=image.getPlanes()[0].getBuffer();
                                if(buffer.remaining()>(recorder?8000000:1000000))throw new IllegalStateException("JPEG buffer too large");
                                byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);jpeg[0]=bytes;
                            }
                        }catch(Throwable error){failure[0]=error;}
                        finally{android.util.Log.i("TopicCamera","ImageReader image.close completed");if(!recorder||jpeg[0]!=null||failure[0]!=null||closing.get()||cancelled.getAsBoolean())done.countDown();}
                    },cameraHandler);
                    manager.openCamera(id,new android.hardware.camera2.CameraDevice.StateCallback(){
                        public void onOpened(android.hardware.camera2.CameraDevice camera){
                            device[0]=camera;openCompleted.countDown();
                            if(closing.get()){camera.close();return;}
                            android.util.Log.i("TopicCamera","camera opened");
                            try{camera.createCaptureSession(java.util.Collections.singletonList(reader[0].getSurface()),new android.hardware.camera2.CameraCaptureSession.StateCallback(){
                                public void onConfigured(android.hardware.camera2.CameraCaptureSession configured){
                                    session[0]=configured;if(closing.get()){configured.close();return;}
                                    try{android.hardware.camera2.CaptureRequest.Builder request=camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE);
                                        request.addTarget(reader[0].getSurface());request.set(android.hardware.camera2.CaptureRequest.JPEG_QUALITY,(byte)(recorder?85:70));
                                        request.set(android.hardware.camera2.CaptureRequest.JPEG_ORIENTATION,recorder?orientation[0]:0);
                                        if(recorder)request.set(android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON);
                                        exposureStart[0]=SystemClock.elapsedRealtime();
                                        android.hardware.camera2.CameraCaptureSession.CaptureCallback callback=new android.hardware.camera2.CameraCaptureSession.CaptureCallback(){
                                            public void onCaptureCompleted(android.hardware.camera2.CameraCaptureSession s,android.hardware.camera2.CaptureRequest r,android.hardware.camera2.TotalCaptureResult result){
                                                Integer state=result.get(android.hardware.camera2.CaptureResult.CONTROL_AE_STATE);
                                                aeStable[0]=state!=null&&(state==android.hardware.camera2.CaptureResult.CONTROL_AE_STATE_CONVERGED||state==android.hardware.camera2.CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED||state==android.hardware.camera2.CaptureResult.CONTROL_AE_STATE_LOCKED);
                                                if(recorder)android.util.Log.i("LokiRecorderExposure","warmup="+exposureFrames[0]+" ae="+state+" exposure_ns="+result.get(android.hardware.camera2.CaptureResult.SENSOR_EXPOSURE_TIME)+" iso="+result.get(android.hardware.camera2.CaptureResult.SENSOR_SENSITIVITY));
                                            }
                                            public void onCaptureFailed(android.hardware.camera2.CameraCaptureSession s,android.hardware.camera2.CaptureRequest r,android.hardware.camera2.CaptureFailure reason){failure[0]=new IllegalStateException("TOPIC capture failed: "+reason.getReason());done.countDown();}
                                        };
                                        nextExposureFrame[0]=()->{if(closing.get()||cancelled.getAsBoolean()){done.countDown();return;}try{configured.capture(request.build(),callback,cameraHandler);}catch(Throwable error){failure[0]=error;done.countDown();}};
                                        nextExposureFrame[0].run();
                                    }catch(Throwable error){failure[0]=error;done.countDown();}
                                }
                                public void onConfigureFailed(android.hardware.camera2.CameraCaptureSession configured){session[0]=configured;configured.close();failure[0]=new IllegalStateException("TOPIC capture session unavailable");done.countDown();}
                            },cameraHandler);}catch(Throwable error){failure[0]=error;done.countDown();}
                        }
                        public void onDisconnected(android.hardware.camera2.CameraDevice camera){device[0]=camera;openCompleted.countDown();camera.close();failure[0]=new IllegalStateException("TOPIC camera disconnected");done.countDown();}
                        public void onError(android.hardware.camera2.CameraDevice camera,int error){device[0]=camera;openCompleted.countDown();camera.close();failure[0]=new IllegalStateException("TOPIC camera error "+error);done.countDown();}
                        public void onClosed(android.hardware.camera2.CameraDevice camera){deviceClosed.countDown();android.util.Log.i("TopicCamera","CameraDevice onClosed");}
                    },cameraHandler);
                }catch(Throwable error){failure[0]=error;openCompleted.countDown();deviceClosed.countDown();done.countDown();}
            });
            long until=SystemClock.elapsedRealtime()+8000L;
            while(!cancelled.getAsBoolean()&&SystemClock.elapsedRealtime()<until&&!done.await(100,TimeUnit.MILLISECONDS)){}
            if(cancelled.getAsBoolean())throw new InterruptedException("TOPIC camera cancelled");
            if(jpeg[0]==null)throw new IllegalStateException("TOPIC camera unavailable",failure[0]);
            // Recorder keeps the hardware JPEG and its orientation metadata. Avoid
            // full-resolution decode/rotate/re-encode allocations beside the HUD.
            if(recorder)return jpeg[0];
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = recorder?1:Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 640);
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length, options);
            if (bitmap == null) throw new IllegalStateException();
            try {
                android.graphics.Matrix rotation = new android.graphics.Matrix(); rotation.postRotate(orientation[0]);
                Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try { oriented.compress(Bitmap.CompressFormat.JPEG, recorder?85:65, out); }
                finally { if (oriented != bitmap) oriented.recycle(); }
                if (out.size() > (recorder?8000000:250000)) throw new IllegalStateException("image too large");
                return out.toByteArray();
            } finally { bitmap.recycle(); }
        }finally{
            closing.set(true);boolean interrupted=false;
            // Keep the handler alive until pending open callbacks can close the device.
            while(openCompleted.getCount()!=0){try{openCompleted.await();}catch(InterruptedException error){interrupted=true;}}
            CountDownLatch released=new CountDownLatch(1);
            cameraHandler.post(()->{try{
                if(session[0]!=null){session[0].close();session[0]=null;}
                if(device[0]!=null){device[0].close();device[0]=null;}
                if(reader[0]!=null){reader[0].close();reader[0]=null;}
            }finally{released.countDown();}});
            while(released.getCount()!=0){try{released.await();}catch(InterruptedException error){interrupted=true;}}
            while(deviceClosed.getCount()!=0){try{deviceClosed.await();}catch(InterruptedException error){interrupted=true;}}
            thread.quitSafely();
            while(thread.isAlive()){try{thread.join();}catch(InterruptedException error){interrupted=true;}}
            android.util.Log.i("TopicCamera","camera released; ImageReader closed; threadAlive=false");
            if(interrupted)Thread.currentThread().interrupt();
        }
        }
    }
}
