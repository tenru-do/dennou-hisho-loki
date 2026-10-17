package com.example.rokidkeyboardbridge;
public final class FullBodyAttachmentTest {
 static int n;static void check(boolean v,String m){if(!v)throw new AssertionError(m);n++;}
 public static void main(String[]args){
  for(int id=0;id<FullBodyAttachedMotion.COUNT;id++){float[] f=FullBodyAttachedMotion.FEATURES[id],b=FullBodyAttachedMotion.BOUNDS[id];check(f.length==19&&f[4]>0&&f[10]>0&&f[16]>0,"visible face "+id);check(FullBodyAttachedMotion.group(20+id)==id,"registry "+id);
   for(int[] size:new int[][]{{240,601},{480,640},{240,299},{200,400}}){float s=FullBodyAttachedMotion.scale(id,size[0],size[1]),x=FullBodyAttachedMotion.left(id,size[0],s),y=FullBodyAttachedMotion.top(id,size[1],s);check(Float.isFinite(s)&&s>0,"scale");check(x+b[0]*s>=6.99f&&x+b[1]*s<=size[0]-6.99f,"horizontal fit");check(y+b[2]*s>=5.99f&&y+b[3]*s<=size[1]-5.99f,"vertical fit");}
   check(Math.abs(FullBodyAttachedMotion.rock(id,1000))>.05f,"ongoing movement");
  }
  check(FullBodyAttachedMotion.group(19)==-1&&FullBodyAttachedMotion.group(20+FullBodyAttachedMotion.COUNT)==-1,"bounds");
  check(FullBodyAttachedMotion.effects(20)==5&&FullBodyAttachedMotion.effects(21)==1&&FullBodyAttachedMotion.effects(22)==0&&FullBodyAttachedMotion.effects(23)==1,"context effects");
  System.out.println("PASS "+n+" attachment face, scale, animation and situation checks");
 }
}
