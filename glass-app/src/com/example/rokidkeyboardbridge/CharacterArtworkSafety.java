package com.example.rokidkeyboardbridge;
/** Static decode geometry guard. It cannot certify anatomy; source review/native pixel tests do that. */
final class CharacterArtworkSafety {
 static boolean geometry(int w,int h,float fw,float fh,float center,float crown,float unit){return w>1&&h>1&&Float.isFinite(fw)&&Float.isFinite(fh)&&fw>1&&fh>1&&Float.isFinite(center)&&center>=0&&center<fw&&Float.isFinite(crown)&&crown>=0&&crown<fh&&Float.isFinite(unit)&&unit>0&&Math.abs((w/(float)h)/(fw/fh)-1)<.08f;}
 private CharacterArtworkSafety(){}
}
