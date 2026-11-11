package com.example.rokidkeyboardbridge;
/** Audited camera/face landmarks of v2 source artwork. XY is always uniformly scaled. */
final class RestArtworkLayout {
 // head center X, crown Y, heel Y, maximum lateral person extent, source cell width/height.
 private static final int[][] GEOMETRY={{193,11,476,85,341,483,470},{191,11,468,85,341,483,470},{212,15,470,90,342,483,470},{193,8,489,85,341,495,480},{172,27,490,95,341,495,480},{178,11,490,90,342,495,480},{191,8,509,85,341,558,510},{177,9,510,90,341,558,510},{178,8,516,90,342,558,510},{255,15,715,110,512,768,700},{254,17,691,130,512,768,700},{254,17,691,130,512,768,700},{254,18,691,130,512,768,700},{254,18,691,130,512,768,700},{272,18,718,130,511,729,700},{270,18,718,130,511,729,700},{272,23,725,130,511,805,700},{271,23,725,130,511,805,700}};
 static int[] kneeFace(int frame){int x=GEOMETRY[frame][0]-27,y=frame<16?62:67;return new int[]{x,y,x+60,y+56};}
 static int[] bounds(int frame){if(frame>=14){int i=frame-14;return new int[]{i%2==0?0:513,i<2?0:731,511,i<2?729:805};}if(frame<9){int[] xx={0,341,682,1024},yy={0,483,978,1536};int c=frame%3,r=frame/3;return new int[]{xx[c],yy[r],xx[c+1]-xx[c],yy[r+1]-yy[r]};}int index=frame-10;return new int[]{index%2*512,index/2*768,512,768};}
 static float stature(int frame,int height){return GEOMETRY[frame][6]*height/(float)GEOMETRY[frame][5];}
 // Use one neutral eye unit per source family; turning/closing the eyes in
 // adjacent motion frames must not make the body zoom in and out.
 static float eyeSpan(int frame,int height){return (frame<9?19.2f:24.5f)*height/GEOMETRY[frame][5];}
 static float scale(int frame,int imageHeight,int width,int height){return CharacterViewport.eyeScale(eyeSpan(frame,imageHeight),width,height);}
 static float coordinate(int frame,int component,int width,int height){int[] g=GEOMETRY[frame];return g[component]*(component==0||component==3?width/(float)g[4]:height/(float)g[5]);}
 static int[] face(int expression){return expression==0?new int[]{228,46,288,102}:new int[]{expression==1||expression==3?252:206,50,expression==1||expression==3?312:266,106};}
 private RestArtworkLayout(){}
}
