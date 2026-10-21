package com.example.rokidgeminisecretary;
import android.graphics.Bitmap;
/** Local heuristic only. Flat drawings and low light can resemble blur. Never rejects an image. */
public final class PhotoQuality {
 public static String assess(Bitmap source){if(source==null)return "画質を判定できません。必要なら撮り直してください。";Bitmap small=Bitmap.createScaledBitmap(source,96,96,true);try{int[] px=new int[9216];small.getPixels(px,0,96,0,0,96,96);return pixels(px,96,96);}finally{if(small!=source)small.recycle();}}
 public static String pixels(int[] px,int width,int height){if(width<3||height<3||px==null||px.length<width*height)return "画質を判定できません";double light=0,edge=0;int count=0;
  for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++){int i=y*width+x;double l=luma(px[i]);light+=l;double lap=4*l-luma(px[i-1])-luma(px[i+1])-luma(px[i-width])-luma(px[i+width]);edge+=lap*lap;count++;}
  String result="";if(light/count<45)result="暗い写真です。明るい場所で撮り直すと読み取りやすくなります。";if(edge/count<55)result+="細部が少ない写真です。ぼけ・ピントを確認してください（無地の画像でも表示します）。";return result;
 }
 private static double luma(int p){return .299*((p>>>16)&255)+.587*((p>>>8)&255)+.114*(p&255);}
 private PhotoQuality(){}
}
