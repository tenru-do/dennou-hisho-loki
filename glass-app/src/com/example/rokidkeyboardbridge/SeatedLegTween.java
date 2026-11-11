package com.example.rokidkeyboardbridge;
/** Source-space knee/ankle alignment for continuous in-betweens; upper body remains fixed. */
final class SeatedLegTween {
 // Anatomical control points in one unchanged 362x543 source cell: two knees, two ankles.
 static final float[][] JOINTS={
 {231,258,188,301,282,451,207,507}, {226,259,191,302,239,428,207,507},
 {202,281,215,299,194,486,214,508}, {182,285,207,285,184,509,211,509},
 {183,280,210,282,153,480,214,508}, {189,265,213,293,146,469,216,508},
 {189,261,214,296,139,487,216,508}, {188,259,213,299,133,495,216,508}};
 static void apply(float[] mesh,int columns,int rows,int width,int height,int source,int target,float amount){
  apply(mesh,columns,rows,width,height,source,target,amount,JOINTS);
 }
 static void apply(float[] mesh,int columns,int rows,int width,int height,int source,int target,float amount,float[][] joints){
  if(source<0||source>=joints.length||target<0||target>=joints.length||!Float.isFinite(amount))throw new IllegalArgumentException("seated tween");
  amount=Math.max(0,Math.min(1,amount));if(source==target||amount==0)return;
  float[] a=joints[source],b=joints[target];
  boolean wardrobe=joints!=JOINTS;
  for(int row=0;row<=rows;row++)for(int col=0;col<=columns;col++){
   float x=col/(float)columns*362,y=row/(float)rows*543;
   // Rigid head/chin hand/torso and transparent boundary; no image-wide scaling.
   if(y<=220||row==rows||col==0||col==columns)continue;
   float fade=Math.min(1,(y-220)/40f),dx=0,dy=0,total=1;
   for(int joint=0;joint<4;joint++){
    int j=joint*2;float nx=(x-a[j])/60f,ny=(y-a[j+1])/72f;
    float q=Math.max(0,1-(nx*nx+ny*ny));q=q*q*4;
    dx+=(b[j]-a[j])*q;dy+=(b[j+1]-a[j+1])*q;total+=q;
   }
   if(total==0)continue;
   float maxX=wardrobe?8:30,maxY=wardrobe?7:24;
   int at=(row*(columns+1)+col)*2;
   mesh[at]+=width/362f*Math.max(-maxX,Math.min(maxX,dx/total))*amount*fade;
   mesh[at+1]+=height/543f*Math.max(-maxY,Math.min(maxY,dy/total))*amount*fade;
  }
 }
 private SeatedLegTween(){}
}
