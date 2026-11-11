package com.example.rokidkeyboardbridge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Pure routing/crop regression for the dedicated ordinary seated greeting. */
public final class SeatedGreetingAtlasTest {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void boundsEqual(int[] actual,int[] expected,String message){check(Arrays.equals(actual,expected),message+": "+Arrays.toString(actual));}
 private static int[] previousBounds(int clip,int frame,int width,int height){
  if(clip==3||clip==7)return new int[]{frame%2*width/2,frame/2*height/2,(frame%2+1)*width/2,(frame/2+1)*height/2};
  int row=clip==23?4:clip==25?5:clip/2,column=frame;
  if(clip==23){if(frame==0)column=1;if(frame==1){row=2;column=2;}}
  return new int[]{column*width/4,row*height/6+(row>0?Math.max(1,height/768):0),(column+1)*width/4,(row+1)*height/6};
 }
 private static void rejects(int clip,int frame){
  boolean rejected=false;
  try{SeatedGestureAtlas.bounds(clip,frame,1024,1536);}catch(IllegalArgumentException expected){rejected=true;}
  check(rejected,"Invalid seated clip/frame rejected: "+clip+"/"+frame);
 }
 public static void main(String[] args){
  check(SeatedGestureAtlas.GREETING.equals("loki_seated_greeting_v732.png"),"Dedicated greeting filename");
  check(SeatedGestureAtlas.asset(1).equals(SeatedGestureAtlas.GREETING),"Seated greeting selects only the new artwork");
  for(int clip:new int[]{3,5,7,23,25}){
   String expected=clip==3?"loki_seated_ear_v689.png":clip==7?"loki_seated_beckon_v689.png":"loki_seated_gestures_v687.png";
   check(SeatedGestureAtlas.asset(clip).equals(expected),"Other seated asset unchanged: "+clip);
  }
  for(int clip=-1;clip<=44;clip++){
   boolean supported=clip==1||clip==3||clip==5||clip==7||clip==23||clip==25;
   check(SeatedGestureAtlas.supports(clip)==supported,"Supported clip set unchanged: "+clip);
  }
  for(int[] size:new int[][]{{1024,1536},{512,768}}){
   int width=size[0],height=size[1];Set<String> crops=new HashSet<>();
   for(int frame=0;frame<4;frame++){
    int[] bounds=SeatedGestureAtlas.bounds(1,frame,width,height);
    int column=frame%2,row=frame/2;
    boundsEqual(bounds,new int[]{column*width/2,row*height/2,(column+1)*width/2,(row+1)*height/2},"Greeting frame order: "+frame);
    check(bounds[0]>=0&&bounds[1]>=0&&bounds[2]<=width&&bounds[3]<=height,"Greeting crop in bounds: "+frame);
    check(bounds[2]-bounds[0]==width/2&&bounds[3]-bounds[1]==height/2,"Four equal greeting cells: "+frame);
    check(crops.add(Arrays.toString(bounds)),"Unique greeting crop: "+frame);
   }
   check(crops.size()==4,"All four greeting frames registered");
   boundsEqual(SeatedGestureAtlas.bounds(1,0,width,height),new int[]{0,0,width/2,height/2},"Greeting begins in top-left cell");
   boundsEqual(SeatedGestureAtlas.bounds(1,3,width,height),new int[]{width/2,height/2,width,height},"Greeting ends in bottom-right cell");
   for(int clip:new int[]{3,5,7,23,25})for(int frame=0;frame<4;frame++)
    boundsEqual(SeatedGestureAtlas.bounds(clip,frame,width,height),previousBounds(clip,frame,width,height),"Other seated crop unchanged: "+clip+"/"+frame);
  }
  // Existing ear/beckon assets retain their original square resolution as well.
  for(int clip:new int[]{3,7})for(int frame=0;frame<4;frame++)
   boundsEqual(SeatedGestureAtlas.bounds(clip,frame,1254,1254),previousBounds(clip,frame,1254,1254),"Existing square atlas unchanged: "+clip+"/"+frame);
  for(int clip:new int[]{1,3,5,7,23,25}){rejects(clip,-1);rejects(clip,4);}
  for(int clip:new int[]{-1,0,2,4,6,8,22,24,26,44})rejects(clip,0);
  System.out.println("PASS "+checks+" dedicated seated greeting route/crop and unaffected seated-family assertions");
 }
}
