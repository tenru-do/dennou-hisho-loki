package com.example.rokidkeyboardbridge;
/** Audited source gutters. Artwork is never resized or stretched by this layout. */
final class PoisedAtlasLayout {
 static int columns(int clip){return clip>=40?3:clip<34?2:4;}
 static int[] bounds(int clip,int frame,int width,int height){int cols=columns(clip),row=clip>=40?clip-40:clip>=36?clip-36:clip>=34?clip-34:clip-32;int[] y;
 if(clip>=40)y=new int[]{0,height*416/1536,height*812/1536,height*1152/1536,height};
 else if(clip>=36)y=new int[]{0,height*400/1536,height*800/1536,height*1152/1536,height};
 else if(clip>=34)y=new int[]{0,height*820/1536,height};
 else y=new int[]{0,height/2,height};
 if(frame<0||frame>=cols||row<0||row>=y.length-1)throw new IllegalArgumentException("Invalid gesture region");return new int[]{width*frame/cols,y[row],width*(frame+1)/cols,y[row+1]};}
 static int reference(int clip){return clip>=40?40:clip>=36?36:clip>=34?34:32;}
}
