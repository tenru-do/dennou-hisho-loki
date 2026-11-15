package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.Bitmap;import java.util.zip.ZipFile;
/** Reviewed ordinary knees-together pose with BOTH hands on lap, unchanged original pixels. */
final class SeatedNeutralSource {
 static final int POSE=168;
 static boolean replaces(int clip,int frame){return clip==5&&frame==0||clip==23&&frame==3;}
 static Bitmap decode(Context context)throws Exception{try(ZipFile apk=new ZipFile(context.getApplicationInfo().sourceDir)){return FullBodyRealisticAssets.decode(apk,POSE,1);}}
 static float unit(){return CharacterDistanceCalibration.BODY[POSE];}
 static float center(){float[] f=FullBodyRealisticFeatures.ALL[POSE];return (f[2]+f[8])*.5f;}
 static float crown(){return Math.max(0,FullBodyFrameScalePolicy.BOUNDS[POSE][2]-2);}
 private SeatedNeutralSource(){}
}
