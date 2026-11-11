package com.example.rokidkeyboardbridge;
import android.content.*;
/** The requested automatic upgrade is applied once; later manual choices remain available. */
final class KeepExposureSettings {
 static int mode(Context c){SharedPreferences p=c.getSharedPreferences("keep_camera",0);if(p.getInt("auto_schema",0)<2){if(!p.edit().putInt("exposure_mode",0).putInt("auto_schema",2).commit())return 0;}return KeepExposurePolicy.mode(p.getInt("exposure_mode",0));}
 private KeepExposureSettings(){}
}
