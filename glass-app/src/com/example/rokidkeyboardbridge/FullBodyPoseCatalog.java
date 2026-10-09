package com.example.rokidkeyboardbridge;

/** Visual review of the actual artwork, not the portrait ID taxonomy.
 * BREATH/PANT describe an open mouth and chest/head gesture: still art cannot
 * establish the reason for breathing, so dialogue must provide that context.
 */
final class FullBodyPoseCatalog {
 enum Kind { NEUTRAL, LISTEN, EXPLAIN, THINK, JOY, CONFIDENT, ANGER, SAD, CRY,
     SURPRISE, SHY, WORRY, REFUSE, TIRED, BREATH, PANT, RECOVERY, STRAIN, WALK }
 static final class Pose {
  final int id, intensity; final boolean seated; final long kinds; final String gesture;
  Pose(int id,String spec){
   this.id=id; String[] fields=spec.split(":"); long mask=0;
   for(String name:fields[0].split("/"))mask|=1L<<Kind.valueOf(name.trim()).ordinal();
   kinds=mask;intensity=Integer.parseInt(fields[1]);gesture=fields[2];
   seated=FullBodyMotionPolicy.isSeated(id);
  }
  boolean supports(Kind kind){return (kinds&(1L<<kind.ordinal()))!=0;}
 }
 // Six entries per source sheet, in visual ID order. Additional front seat=168.
 private static final String[] SHEETS={
  "EXPLAIN:1:palm_up,JOY:1:smiling_chin,REFUSE/ANGER:1:arms_crossed,WORRY/SAD:1:hand_chest,LISTEN/NEUTRAL:0:arms_down,CONFIDENT/JOY:1:hip_palm_up",
  "EXPLAIN:1:palm_up,THINK:1:chin_down,SHY:1:hands_clasped,LISTEN:0:hand_chest,THINK:1:chin,SURPRISE:2:raised_palms",
  "EXPLAIN:1:palm_up,THINK:1:chin,CONFIDENT:1:hand_hip,NEUTRAL:0:arms_down,CONFIDENT:2:both_hips,SHY:1:hands_clasped",
  "SURPRISE/JOY:1:open_hands,WORRY:2:hands_face,REFUSE/STRAIN:2:folded_arms,THINK:1:chin,SHY:1:hands_clasped,EXPLAIN:1:palm_up",
  "SHY:1:hands_clasped,SHY:1:hands_clasped,BREATH/STRAIN:2:head_up_open_mouth,SURPRISE:3:raised_palms,TIRED/SAD:2:head_down_temple,REFUSE/STRAIN:2:arms_crossed_down",
  "EXPLAIN:1:chest_palm_up,NEUTRAL:0:arms_down,CRY/STRAIN:3:clenched_hands_open_mouth,PANT/STRAIN:2:head_up_chest,CRY/STRAIN:3:hands_face,TIRED:2:hand_stomach",
  "WORRY/SAD:2:hands_clasped,NEUTRAL:0:arms_down,THINK:2:chin,SHY:1:hands_clasped,EXPLAIN/CONFIDENT:1:palm_up,THINK:1:chin",
  "LISTEN:1:hands_waist,WORRY:1:hands_clasped,EXPLAIN:1:chest_palm_up,SURPRISE:2:raised_palms,SURPRISE:2:raised_hand,STRAIN/THINK:1:chin",
  "WORRY:1:chest,ANGER:2:clenched_fists,SURPRISE:1:open_palms,RECOVERY/BREATH:1:chest_closed_eyes,SHY/RECOVERY:1:hands_clasped,EXPLAIN:1:palm_up",
  "CONFIDENT:1:glasses_phone,LISTEN:1:glasses_clasped,EXPLAIN:1:phone_chest,EXPLAIN:1:phone_open_mouth,THINK/STRAIN:2:temple,CRY/STRAIN:3:open_mouth_arms_down",
  "TIRED/SAD:2:clasped_down,WORRY/BREATH:1:chest,REFUSE:2:crossed_arms, BREATH/RECOVERY:1:chest_open_hand,SHY:1:hands_clasped,WORRY/STRAIN:2:clasped_chest",
  "EXPLAIN:1:palm_up,WORRY:2:clasped_chest,SHY:1:clasped_down,STRAIN/BREATH:2:open_arms,REFUSE/STRAIN:2:arms_crossed,SURPRISE:2:raised_palms",
  "WORRY:2:open_hands,THINK:1:chin,RECOVERY:1:hand_waist,EXPLAIN:1:palm_up,SHY:1:clasped,RECOVERY/NEUTRAL:0:clasped",
  "EXPLAIN:1:palm_up,BREATH/STRAIN:2:head_up_clasped,STRAIN:2:raised_fist,SURPRISE:1:hand_ear,SURPRISE:3:raised_palms,CRY:2:hand_eye",
  "SURPRISE:3:open_hands,WORRY:2:clasped_chest,STRAIN:2:raised_fist,SURPRISE:3:raised_palms,STRAIN/SAD:2:clasped_down,CRY:2:hand_eye",
  "PANT/STRAIN:3:head_up_open_arms,PANT/STRAIN:2:head_up_clasped,SURPRISE:3:raised_palms,STRAIN:3:open_mouth_fists,REFUSE:2:protective_chest,CRY:2:hand_eye",
  "ANGER:3:clenched_fists,REFUSE:2:protective_chest,SAD:2:clasped_down,STRAIN:3:open_mouth_hands,STRAIN:3:open_mouth_hands,STRAIN:2:clenched_hands",
  "STRAIN:3:raised_tense_hands,CRY:2:hand_eye,REFUSE:2:arms_crossed,SAD:2:clasped_down,STRAIN:2:clenched_hands,BREATH/STRAIN:2:head_up_clasped",
  "WORRY:2:clasped_chest,CRY:2:hands_eyes,SAD:2:clasped_down,CRY:2:hands_face,CRY:2:hand_eye,PANT/CRY:3:head_up_open_mouth",
  "REFUSE:2:crossed_arms,EXPLAIN:2:chest_palm_up,CRY/STRAIN:3:open_mouth_fists,EXPLAIN:2:raised_hand,SAD:2:clasped_down,CRY:2:hand_eye",
  "PANT/STRAIN:3:head_up_open_hands,PANT/CRY:3:head_up_clasped,BREATH/STRAIN:2:head_up_crossed_arms,THINK/RECOVERY:1:chin_up,CRY:3:head_up_open_mouth,SAD:2:clasped_down",
  "SAD:2:hand_chest,CRY/STRAIN:3:head_up_open_hands,SAD:2:seated_clasped_down,CRY:2:seated_tilted_head,REFUSE/STRAIN:2:seated_protective_chest,BREATH/STRAIN:2:seated_head_up",
  "TIRED/SAD:1:seated_crossed_legs,CRY:2:seated_hand_eye,BREATH/STRAIN:2:seated_head_up,THINK:1:seated_chin,SAD:2:seated_clasped_down,CRY:2:seated_hand_eye",
  "PANT/STRAIN:3:seated_head_up,CRY:3:seated_head_up_open_mouth,CRY:2:seated_tilted_head,STRAIN:2:seated_head_up_protective,CRY:3:seated_clasped_head_up,TIRED/CRY:3:seated_head_down_temple",
  "TIRED/SAD:2:seated_clasped_down,PANT/STRAIN:3:seated_head_up,REFUSE:2:crossed_arms,CRY:3:head_up_clasped,PANT:2:head_up_open_arms,CRY:2:hand_eye",
  "SAD:2:clasped_down,CRY/STRAIN:3:open_mouth_fists,REFUSE:2:protective_chest,PANT/CRY:3:head_up_open_hands,STRAIN:3:open_mouth_hand_thigh,THINK:1:chin_up",
  "CRY/STRAIN:3:open_mouth_clasped,PANT/CRY:3:head_up_open_hands,TIRED:2:clasped_down,CRY/STRAIN:3:head_up_open_hands,TIRED/SAD:3:seated_head_down_temple,TIRED/SAD:3:seated_head_down_temple",
  "EXPLAIN:1:palm_up,THINK:1:chin, WORRY/SAD:1:hand_chest,REFUSE/NEUTRAL:1:seated_folded_arms,TIRED/SAD:1:seated_hands_knees,THINK:1:seated_chin"
 };
 static final Pose[] ALL=new Pose[181];
 static {
  int id=0;for(String sheet:SHEETS)for(String spec:sheet.split(","))ALL[id]=new Pose(id++,spec.trim());
  if(id!=168)throw new IllegalStateException("Full-body catalogue size "+id);
  ALL[168]=new Pose(168,"NEUTRAL/LISTEN/RECOVERY:0:seated_front");
  for(int frame=0;frame<6;frame++){
   ALL[169+frame]=new Pose(169+frame,"NEUTRAL/LISTEN:0:idle_frame_"+frame);
   ALL[175+frame]=new Pose(175+frame,"WALK:1:walk_frame_"+frame);
  }
 }
 static int select(Kind kind,int intensity,int current,int variant){
  boolean seated=current>=0&&FullBodyMotionPolicy.isSeated(current);
  int best=1000,count=0;
  for(Pose p:ALL)if(p.supports(kind)){
   int score=Math.abs(p.intensity-intensity)*4+(p.seated==seated?0:3);
   if(score<best){best=score;count=1;}else if(score==best)count++;
  }
  if(count==0)return current>=0?current:4;
  int index=Math.floorMod(variant,count);
  for(Pose p:ALL)if(p.supports(kind)&&Math.abs(p.intensity-intensity)*4+(p.seated==seated?0:3)==best){if(index--==0)return p.id;}
  return 4;
 }
 static String label(int id){
  String[] names={"通常","傾聴","説明","思案","喜び","自信","怒り","悲しみ","涙","驚き","照れ","心配","拒否","疲労","吐息","息切れ・喘ぎ","安堵","緊張","歩行"};
  Pose p=ALL[id];String tags="";
  for(Kind kind:Kind.values())if(p.supports(kind))tags+=(tags.isEmpty()?"":"・")+names[kind.ordinal()];
  return String.format(java.util.Locale.US,"%03d ",id)+tags+"／"+(p.seated?"座り":"立ち")+"（"+p.intensity+"）";
 }
}
