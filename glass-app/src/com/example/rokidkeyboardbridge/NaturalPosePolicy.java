package com.example.rokidkeyboardbridge;
/** Presentation-only choices. Uptime clock; no timers, IO, sensors or scene mutation. */
final class NaturalPosePolicy {
 private static final int[] REST_ALTERNATIVES={10,1,2,12,13,14};
 static final long DURATION=6400,FADE=650;private boolean enabled=true,flexible,held;private int active=-1,sequence,poseSequence,micro=-1;private long at=-1,due=-1,microDue=-1,blockedUntil,resumeAfter;
 static int control(String text){String t=RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");if(t.length()>70)return -1;if(t.matches("(?:(?:自然に)?(?:いろんな|いろいろな|いろいろ|色々な)?ポーズ(?:を|も)(?:とって|取って)|ポーズ(?:は|を)おまかせ(?:にして)?|自然なポーズにして)(?:ください|ね)?[。！!\\s]*"))return 1;if(t.matches("(?:ポーズを固定して|その姿勢のままで|自然なポーズ切り替えを止めて)(?:ください|ね)?[。！!\\s]*"))return 0;return -1;}
 boolean enabled(){return enabled;}void enabled(boolean value){if(value&&!enabled){held=false;flexible=true;due=microDue=-1;}enabled=value;if(!value)cancel();}
 void clear(){cancel();flexible=held=false;due=microDue=-1;blockedUntil=resumeAfter=0;}
 void cancel(){active=micro=-1;at=-1;}
 void user(String text,long now){int c=control(text);if(c>=0){enabled=c==1;flexible=c==1;held=c==0;cancel();due=now+12000;microDue=now+10000;resumeAfter=0;return;}int action=RestAnimationIntent.action(text),rest=RestPoseIntent.kind(text);boolean visual=EmbodiedScenePolicy.visualCommand(text);if(!visual){if(active>=0)due=now+12000;cancel();resumeAfter=now+1500;return;}cancel();due=now+22000;microDue=now+12000;resumeAfter=0;if(action==12){held=true;flexible=false;return;}if(OffDutyPoseIntent.casual(text)||ComfortPoseIntent.adjacent(text)||rest==RestPoseIntent.RELAX||RestPoseIntent.wantsLie(rest)){held=false;flexible=true;if(RestPoseIntent.wantsLie(rest))due=now+12000;return;}if(!(action>=5&&action<=8||action>=13&&action<=17)){held=false;flexible=false;}}

 void answer(FullBodyPoseCatalog.Kind kind,long now){if(kind==FullBodyPoseCatalog.Kind.ANGER||kind==FullBodyPoseCatalog.Kind.REFUSE||kind==FullBodyPoseCatalog.Kind.SAD||kind==FullBodyPoseCatalog.Kind.CRY||kind==FullBodyPoseCatalog.Kind.WORRY||kind==FullBodyPoseCatalog.Kind.TIRED||kind==FullBodyPoseCatalog.Kind.STRAIN){blockedUntil=now+90000;cancel();}microDue=Math.max(microDue,now+8000);}
 void pause(long now){cancel();resumeAfter=now+1500;}
 void mode(int value,long now){mode(value,now,value==1);}
 void mode(int value,long now,boolean requestBusy){if(value==2||requestBusy){if(active>=0)due=now+12000;cancel();resumeAfter=Math.max(resumeAfter,now+1500);}}
 int select(int base,long now,int mode,boolean quiet,boolean pinned,boolean movement){return select(base,now,mode,quiet,pinned,movement,mode==1);}
 int select(int base,long now,int mode,boolean quiet,boolean pinned,boolean movement,boolean requestBusy){micro=-1;if(!enabled||held||quiet||pinned||!movement||base<0){cancel();resumeAfter=Math.max(resumeAfter,now+1500);return base;}if(mode==2||requestBusy||mode<0||mode>2){if(active>=0)due=now+12000;cancel();resumeAfter=Math.max(resumeAfter,now+1500);return base;}if(now<blockedUntil||now<resumeAfter)return base;if(due<0){due=now+22000;microDue=now+16000;}if(at>=0&&now<at){cancel();due=now+12000;microDue=now+10000;return base;}if(active>=0){if(now-at<DURATION)return active;cancel();due=now+32000+(poseSequence%3)*8000;microDue=now+12000;}int next=flexible?alternative(base,poseSequence):-1;if(next>=0&&now>=due){active=next;at=now;poseSequence++;return active;}if(now>=microDue){micro=sequence++%3;microDue=now+26000+(sequence%3)*7000;}return base;}

 static int alternative(int base,int sequence){if(base==0)return REST_ALTERNATIVES[Math.floorMod(sequence,REST_ALTERNATIVES.length)];if(OffDutyPoseIntent.active(base)){int next=12+Math.floorMod(sequence,3);return next==base?12+Math.floorMod(sequence+1,3):next;}return alternative(base);}
 static int alternative(int base){return base==8?11:base==11?8:base==0||base==9?10:-1;}
 int takeMicro(){int value=micro;micro=-1;return value;}long age(long now){return at<0?0:Math.max(0,now-at);}boolean active(){return active>=0;}boolean moving(long now){return active>=0&&now>=at&&now-at<DURATION&&(now-at<2800||now-at>DURATION-FADE);}
 static float alpha(long age){if(age<=0||age>=DURATION)return 0;float enter=Math.min(1,age/(float)FADE),leave=Math.min(1,(DURATION-age)/(float)FADE);return smooth(Math.min(enter,leave));}private static float smooth(float v){return v*v*(3-2*v);}
 static int everyday(long idle,boolean seated,boolean enabled){if(!enabled||CharacterMotionTuning.body()==0||idle<0)return -1;long phase=idle%180000;if(phase<100000||phase>=104600)return -1;return FullBodyEverydayGesture.clip(seated,(idle/180000)%2==0?3:12);}static long everydayAge(long idle){return Math.max(0,idle%180000-100000);}
}
