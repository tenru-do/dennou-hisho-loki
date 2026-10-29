package com.example.rokidkeyboardbridge;
import android.graphics.Bitmap;
/** Generated sheets are not guaranteed to place the row break at exact half height. */
final class FullBodyGestureRegions {
 static int split(Bitmap bitmap){
  int w=bitmap.getWidth(),h=bitmap.getHeight(),from=h*42/100,to=h*64/100;int[] row=new int[w];
  int bestStart=-1,bestLength=0,start=-1;
  for(int y=from;y<to;y++){
   bitmap.getPixels(row,0,w,0,y,w,1);boolean empty=true;
   for(int p:row)if(Math.max((p>>16)&255,Math.max((p>>8)&255,p&255))>12){empty=false;break;}
   if(empty&&start<0)start=y;
   if((!empty||y==to-1)&&start>=0){int length=y-start;if(length>bestLength){bestStart=start;bestLength=length;}start=-1;}
  }
  return bestLength>=2?bestStart+bestLength/2:h/2;
 }
}
