package com.example.rokidkeyboardbridge;
/** Audited camera/face landmarks of v2 source artwork. XY is always uniformly scaled. */
final class RestArtworkLayout {
 // head center X, crown Y, heel Y, maximum lateral person extent, source cell width/height.
 private static final int[][] GEOMETRY={{190,15,474,85,342,483,470},{185,15,470,85,339,483,470},{177,14,466,90,343,483,470},{190,15,487,85,342,495,480},{187,50,487,95,339,495,480},{182,18,487,90,343,495,480},{190,15,510,85,342,558,510},{183,15,510,90,339,558,510},{180,16,514,90,343,558,510},{255,15,715,110,512,768,700},{284,25,693,130,512,741,700},{238,25,693,130,512,741,700},{282,30,710,130,512,795,700},{231,31,710,130,512,795,700},{272,18,718,130,511,729,700},{270,18,718,130,511,729,700},{272,23,725,130,511,805,700},{271,23,725,130,511,805,700}};
 static int[] kneeFace(int frame){int x=GEOMETRY[frame][0]-27,y=frame<16?62:67;return new int[]{x,y,x+60,y+56};}
 static int[] bounds(int frame){if(frame>=14){int i=frame-14;return new int[]{i%2==0?0:513,i<2?0:731,511,i<2?729:805};}if(frame<9){int[] xx={0,342,681,1024},yy={0,483,978,1536};int c=frame%3,r=frame/3;return new int[]{xx[c],yy[r],xx[c+1]-xx[c],yy[r+1]-yy[r]};}int index=frame-10;int[] yy={0,741,1536};return new int[]{index%2*512,yy[index/2],512,yy[index/2+1]-yy[index/2]};}
 static float stature(int frame,int height){return GEOMETRY[frame][6]*height/(float)GEOMETRY[frame][5];}
 static float coordinate(int frame,int component,int width,int height){int[] g=GEOMETRY[frame];return g[component]*(component==0||component==3?width/(float)g[4]:height/(float)g[5]);}
 static int[] face(int expression){return expression==0?new int[]{228,46,288,102}:new int[]{expression==1||expression==3?252:206,50,expression==1||expression==3?312:266,106};}
 private RestArtworkLayout(){}
}
