package com.example.rokidkeyboardbridge;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.io.*;
/** Replays supplied dialogue through production stance, scene and continuity policies. Emits no dialogue text. */
public final class FullBodyHistoryReplayTest {
 static String decode(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
 public static void main(String[] args)throws Exception{
  boolean baseline=args.length>2&&args[2].equals("baseline");
  FullBodySpeechStance stance=new FullBodySpeechStance();FullBodyConversationContinuity hold=new FullBodyConversationContinuity();
  int current=4,total=0,held=0,scenes=0,refusals=0;long previous=-1;
  try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8))){
   out.println("id,timeMillis,emotion,pose,afterSpeechPose,newScene,afterSpeechScene,reason,seated,assets,crop");
   for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)){
    String[] f=line.split("\t",-1);int id=Integer.parseInt(f[0]);long time=Long.parseLong(f[1]);String prompt=decode(f[2]),answer=decode(f[3]);
    if(previous>=0&&time-previous>=FullBodyConversationContinuity.GAP_MS){hold.clear();stance.reset();stance.scene("別の話","");current=FullBodyMotionPolicy.isSeated(current)?168:4;}
    if(FullBodyConversationContinuity.topic(prompt)){hold.clear();stance.reset();current=FullBodyMotionPolicy.isSeated(current)?168:4;}
    boolean keep=!baseline&&hold.holds(time);if(baseline)stance.begin(answer);else stance.begin(answer,keep);stance.observed(null);stance.scene(prompt,answer);
    FullBodySemanticReaction.Reaction semantic=FullBodySemanticReaction.find(answer);if(semantic!=null)stance.semantic(semantic);
    int pose=stance.pose(answer,current);if(pose<0)pose=current;
    if(!baseline)pose=hold.accept(pose,stance.kind(),stance.intensity(),FullBodyConversationContinuity.posture(answer),time);
    FullBodyDramaticConversationPolicy.Cue dramatic=FullBodyDramaticConversationPolicy.select(prompt,answer,stance.kind());
    int scene=dramatic==null?-1:dramatic.scene;
    boolean boundary=stance.kind()==FullBodyPoseCatalog.Kind.REFUSE;
    if(!baseline&&boundary&&scene>=0)throw new AssertionError("Boundary incorrectly selected dramatic scene at record "+id);
    if(!baseline){hold.active(false,time);if(!hold.holds(time+1500)||hold.pose()!=pose)throw new AssertionError("Speech/listening reset at record "+id);}
    int after=baseline?(FullBodyMotionPolicy.isSeated(pose)?168:4):pose;
    int afterScene=baseline?-1:scene;
    if(after==pose&&afterScene==scene)held++;
    if(scene>=0)scenes++;if(boundary)refusals++;
    String reason=scene>=0?(dramatic.cycle?"natural-tension-cycle":"explicit-scene"):boundary?"boundary-refusal":FullBodyDramaticConversationPolicy.explanatory(answer)?"explanation-keeps-ordinary-pose":"no-new-dramatic-scene-cue";
    String assets=scene>=0?FullBodyDramaticGallery.MOTION_SHEETS[scene]:FullBodyRealisticAssets.NAMES[FullBodyRealisticAssets.SHEET[pose]];
    if(dramatic!=null&&dramatic.cycle)assets+=";"+FullBodyDramaticGallery.MOTION_SHEETS[3]+";"+FullBodyDramaticGallery.BRIDGE_SHEET;
    if(scene<0){
     boolean seated=FullBodyMotionPolicy.isSeated(current);
     if(FullBodyHeavyBreathPolicy.eligible(stance.kind(),seated))assets+=";"+String.join(";",FullBodyHeavyBreathPolicy.ASSETS);
     if(FullBodyRearStopPolicy.eligible(stance.kind(),seated,answer))assets+=";"+String.join(";",FullBodyRearStopPolicy.ASSETS);
     if(FullBodyUnwellPolicy.eligible(stance.kind(),seated,answer))assets+=";"+String.join(";",FullBodyUnwellPolicy.ASSETS);
    }
    int[] crop=FullBodyRealisticAssets.REGIONS[pose];
    out.println(id+","+time+","+(stance.kind()==null?"NEUTRAL":stance.kind())+","+pose+","+after+","+scene+","+afterScene+","+reason+","+FullBodyMotionPolicy.isSeated(pose)+","+assets+","+crop[0]+";"+crop[1]+";"+crop[2]+";"+crop[3]);
    current=pose;previous=time;total++;
   }
  }
  System.out.println("PASS "+(baseline?"v468 baseline":"v469")+" historical replay: "+total+" responses; retained after speech="+held+"; dramatic matches="+scenes+"; boundary refusals="+refusals);
 }
}
