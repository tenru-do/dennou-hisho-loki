package com.example.rokidkeyboardbridge;
public final class FullBodyPoseVariationTest {
 public static void main(String[]args){
  FullBodyPoseVariation v=new FullBodyPoseVariation();int checks=0;
  for(int base=0;base<181;base++)for(int mode=0;mode<3;mode++){
   v.reset();v.target(base,mode,0,FullBodyPoseCatalog.Kind.CRY);
   for(long t=0;t<360000;t+=250){int p=v.target(base,mode,t,FullBodyPoseCatalog.Kind.CRY);checks++;
    if(FullBodyMotionPolicy.isSeated(base)!=FullBodyMotionPolicy.isSeated(p))throw new AssertionError("Posture timing changed "+base+" -> "+p);
   }
  }
  v.reset();if(v.target(168,0,0,null)!=168||v.target(168,0,3999,null)!=168||v.target(168,0,4000,null)==168)throw new AssertionError("Quiet seated variation");
  if(v.target(4,1,16000,null)!=4)throw new AssertionError("New standing target must retain timing");
  v.reset();v.target(168,0,0,null,false);
  for(long t=60000;t<90000;t+=250)if(v.target(168,0,t,null,false)!=168)throw new AssertionError("Crossed-leg phase interrupted");
  for(FullBodyPoseCatalog.Pose base:FullBodyPoseCatalog.ALL)for(FullBodyPoseCatalog.Kind kind:FullBodyPoseCatalog.Kind.values())if(base.supports(kind)){
   v.reset();v.target(base.id,2,0,kind);
   for(long t=9000;t<90000;t+=9000){int id=v.target(base.id,2,t,kind);FullBodyPoseCatalog.Pose pose=FullBodyPoseCatalog.ALL[id];
    if(!pose.supports(kind)||pose.intensity!=base.intensity||pose.seated!=base.seated)throw new AssertionError("Speech emotion drift "+base.id+" -> "+id);
   }
  }
  for(int base:new int[]{1,2,32,49,128,134,160}){
   v.reset();if(v.target(base,0,0,null)!=base||v.target(base,0,14999,null)!=base)throw new AssertionError("Reaction hold lost "+base);
   java.util.HashSet<Integer> seen=new java.util.HashSet<>();
   for(long t=15000;t<=120000;t+=15000)seen.add(v.target(base,0,t,null));
   if(seen.size()<2)throw new AssertionError("Idle stuck on emotional pose "+base);
  }
  for(int base:new int[]{168,169,4}){
   v.reset();int prior=v.target(base,0,0,null),changes=0;long lastChange=0;
   java.util.HashSet<Integer> poses=new java.util.HashSet<>();
   for(long t=0;t<120000;t+=100){int pose=v.target(base,0,t,null);poses.add(pose);
    if(pose!=prior){if(t-lastChange>10000)throw new AssertionError("Calm idle held too long");lastChange=t;changes++;prior=pose;}
   }
   if(changes<12||poses.size()<3)throw new AssertionError("Insufficient quiet pose variety");
  }
  v.reset();v.target(4,0,0,null);
  for(long t=4000;t<300000;t+=100){int pose=v.target(4,0,t,null);String gesture=FullBodyPoseCatalog.ALL[pose].gesture;
   if(gesture.contains("palm")||gesture.contains("crossed")||FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.JOY))throw new AssertionError("Inappropriate idle guidance/laughter: "+pose);
  }
  System.out.println("PASS within-posture variation; standing/sitting invariance "+checks+" checks");
 }
}
