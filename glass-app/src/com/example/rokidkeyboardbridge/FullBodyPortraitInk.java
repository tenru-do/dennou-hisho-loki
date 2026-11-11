package com.example.rokidkeyboardbridge;
import android.graphics.Bitmap;

/** Adopted skin-lit cyan rendering, matching the review's default 75 percent. */
final class FullBodyPortraitInk {
    private static final int[] TONE=new int[256];static{for(int i=0;i<256;i++)TONE[i]=(int)Math.round(255*Math.pow(i/255.0,.90));}
    static Bitmap render(Bitmap source) {
        // New natural-color atlases already carry their final skin tones and alpha.
        // Callers recycle the input, so return an independent bitmap.
        if (FullBodyFaceAnimator.naturalColor(source))
            return source.copy(Bitmap.Config.ARGB_8888, false);
        int w=source.getWidth(),h=source.getHeight();
        int[] pixels=new int[w*h];source.getPixels(pixels,0,w,0,0,w,h);
        for(int i=0;i<pixels.length;i++) {
            int v=pixels[i];
            int gray=(((v>>16)&255)*54+((v>>8)&255)*183+(v&255)*19)>>8;
            gray=TONE[gray];
            pixels[i]=0xff000000|((gray*158/255)<<8)|(gray*165/255);
        }
        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
    }
}
