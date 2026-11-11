package com.example.rokidkeyboardbridge;
import android.content.Context;import android.hardware.camera2.*;import org.json.JSONObject;
/** Static capability query only. This never opens a camera or obtains a frame. */
final class KeepExposureSupport {
 static JSONObject read(Context c)throws Exception{
  CameraManager manager=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);String[] ids=manager.getCameraIdList();if(ids.length==0)throw new IllegalStateException("no camera");
  CameraCharacteristics chars=manager.getCameraCharacteristics(ids[0]);android.util.Range<Integer> range=chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);android.util.Rational step=chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
  float value=step==null?0:step.floatValue();boolean supported=range!=null&&value>0&&(range.getLower()<0||range.getUpper()>0);
  return new JSONObject().put("known",true).put("supported",supported).put("step",value).put("minimum",range==null?0:range.getLower()).put("maximum",range==null?0:range.getUpper());
 }
 private KeepExposureSupport(){}
}
