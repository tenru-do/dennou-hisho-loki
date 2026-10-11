package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyCatalogTest {
 static int checks;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[]args){
  boolean[] reachable=new boolean[181];
  for(FullBodyPoseCatalog.Kind kind:FullBodyPoseCatalog.Kind.values())for(int intensity=0;intensity<=3;intensity++)for(int current:new int[]{4,168})for(int variant=0;variant<181;variant++){
   int pose=FullBodyPoseCatalog.select(kind,intensity,current,variant);check(FullBodyPoseCatalog.ALL[pose].supports(kind),"Wrong category "+kind+" -> "+pose);reachable[pose]=true;
  }
  for(int id=0;id<181;id++)check(reachable[id],"Unreachable registered image "+id);
  String[] lines={"ありがとうございます。","とても怒っています。","少し悲しいです。","激しく号泣しています。","びっくりしました！","照れちゃいます。","疲れ果てています。","（息を切らしている）","（軽く吐息を漏らす）","息を整えます。","確認します。","説明します。","一緒に歩きます。"};
  FullBodyPoseCatalog.Kind[] expected={JOY,ANGER,SAD,CRY,SURPRISE,SHY,TIRED,PANT,BREATH,RECOVERY,THINK,EXPLAIN,WALK};
  for(int i=0;i<lines.length;i++){
   FullBodySpeechStance p=new FullBodySpeechStance();p.begin(lines[i]);int pose=p.pose(lines[i],4);
   check(pose>=0&&FullBodyPoseCatalog.ALL[pose].supports(expected[i]),"Utterance "+lines[i]);
  }
  for(String text:new String[]{"「やめてください」という言い方です。","喘ぎとは何ですか？","悲しくありません。","怒っていません。","喘ぐことはありません。","彼女は激しく喘いでいます。","彼は怒っています。","セクハラ防止研修を説明します。"}){
   FullBodySpeechStance.Cue cue=FullBodySpeechStance.classify(text);
   check(cue==null||cue.kind==EXPLAIN,"False emotion "+text);
  }
  FullBodySpeechStance p=new FullBodySpeechStance();p.begin("...");
  int sad=p.pose("悲しいです。",168);check(FullBodyMotionPolicy.isSeated(sad),"Seated sadness preserved");
  check(FullBodyPoseCatalog.ALL[p.pose("それでも続けます。",sad)].supports(SAD),"Emotion reset by neutral connector");
  p.pose("触らないでください。",4);check(FullBodyPoseCatalog.ALL[p.pose("別の話にしましょう。",4)].supports(REFUSE),"Refusal continuity");
  check(FullBodyPoseCatalog.ALL[p.pose("落ち着きました。",4)].supports(RECOVERY),"Explicit recovery");
  p.begin("...");check(FullBodyPoseCatalog.ALL[p.pose("ありがとうございます。",4)].supports(JOY),"New answer reset");
  check(FullBodySpeechStance.classify("激しく喘いでいます。").intensity==3,"Pant intensity");
  p.begin("...");check(FullBodyMotionPolicy.isSeated(p.pose("（椅子に座って悲しんでいる）",4)),"Explicit seated situation");
  check(FullBodySpeechStance.classify("無理しないでください。").kind==WORRY,"Supportive negative imperative");
  java.util.Set<Integer> variants=new java.util.HashSet<>();
  for(int n=0;n<30;n++){p.begin("...");variants.add(p.pose("説明します。",4));}
  check(variants.size()>6,"Repeated replies must rotate image candidates");
  p.begin("説明します。あとで悲しいですと言います。");check(FullBodyPoseCatalog.ALL[p.pose("説明します。",4)].supports(EXPLAIN),"Future sentence spoiled current reaction");
  check(FullBodySpeechStance.classify("「嬉しいです」").kind==JOY,"Own quoted utterance");
  boolean[] seatFrames=new boolean[6];for(long t=0;t<180000;t+=50)seatFrames[FullBodySeatedPolicy.frame(t)]=true;for(boolean used:seatFrames)check(used,"Unused seat frame");
  p.begin("...");p.pose("やめてください。",4);check(FullBodyPoseCatalog.ALL[p.pose("泣いています。",4)].supports(CRY),"Refusal must allow distress reactions");
  check(FullBodySpeechStance.classify("そのような行為は適切ではありません。").kind==REFUSE,"Natural boundary wording");
  check(FullBodySpeechStance.classify("いいですね！").kind==JOY,"Natural positive response");
  System.out.println("PASS full catalogue: all 180 registered images + front seat reachable; "+checks+" checks");
  if(args.length>0)try(java.io.PrintWriter out=new java.io.PrintWriter(args[0],"UTF-8")){
   out.println("id,kinds,intensity,posture,gesture");
   for(FullBodyPoseCatalog.Pose pose:FullBodyPoseCatalog.ALL){String tags="";for(FullBodyPoseCatalog.Kind k:FullBodyPoseCatalog.Kind.values())if(pose.supports(k))tags+=(tags.isEmpty()?"":"/")+k.name();out.println(pose.id+","+tags+","+pose.intensity+","+(pose.seated?"seated":"standing")+","+pose.gesture);}
  }catch(Exception error){throw new RuntimeException(error);}
  if(args.length>0)try(java.io.PrintWriter out=new java.io.PrintWriter(new java.io.File(new java.io.File(args[0]).getParentFile(),"scenarios.csv"),"UTF-8")){
   out.println("utterance,kind,selected_pose");
   for(int n=0;n<lines.length;n++){FullBodySpeechStance state=new FullBodySpeechStance();state.begin(lines[n]);out.println(lines[n]+","+expected[n]+","+state.pose(lines[n],4));}
  }catch(Exception error){throw new RuntimeException(error);}
 }
}
