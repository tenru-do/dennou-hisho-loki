package com.example.rokidkeyboardbridge;
public final class CharacterLifePolicyTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 public static void main(String[]args){
  boolean smile=false;float left=0,right=0,nod=0;
  for(long t=0;t<72000;t+=40){
   for(int mode=0;mode<=2;mode++){
    long d=PortraitLifeMotion.delay(t,mode,false);check(d>=40&&d<=250,"bounded active cadence");
    check(PortraitLifeMotion.delay(t,mode,true)>=80,"quiet cadence");
    check(PortraitLifeMotion.tilt(t,mode,true)==0&&PortraitLifeMotion.gaze(t,mode,true)==0,"quiet navigation/meeting");
    for(int expression=2;expression<62;expression++)if(expression!=14)
     check(PortraitLifeMotion.face(expression,t,mode,false)==expression,"semantic expression retained");
    if(mode!=0)check(PortraitLifeMotion.face(0,t,mode,false)==0,"no idle smile during listening/speech");
   }
   smile|=PortraitLifeMotion.face(0,t,0,false)==1;
   left=Math.min(left,PortraitLifeMotion.gaze(t,0,false));right=Math.max(right,PortraitLifeMotion.gaze(t,0,false));
   nod=Math.max(nod,PortraitLifeMotion.nod(t,0,false));
   check(Math.abs(PortraitLifeMotion.tilt(t,0,false))<=3.001,"bounded head tilt");
  }
  check(smile&&left<0&&right>0&&nod>.017,"four idle gesture variants are reachable");
  check(PortraitLifeMotion.gesture(3000,0)==0&&PortraitLifeMotion.gesture(5400,0)==0,"gesture endpoints settle");
  check(CharacterSwipePolicy.direction(50,2,24,100)==1,"right");check(CharacterSwipePolicy.direction(-50,2,24,100)==2,"left");
  check(CharacterSwipePolicy.direction(2,50,24,100)==3,"down");check(CharacterSwipePolicy.direction(2,-50,24,100)==4,"up");
  check(CharacterSwipePolicy.direction(2,3,24,100)==0,"tap/jitter excluded");check(CharacterSwipePolicy.direction(50,0,24,2001)==0,"long drag excluded");
  check(CharacterSwipePolicy.contact("頭を撫でる",1000,100),"contact context");
  check(!CharacterSwipePolicy.contact("触らないで",1000,100),"refusal");
  check(!CharacterSwipePolicy.contact("頭を撫でる",180100,100),"context expires");
  check(!CharacterSwipePolicy.contact("頭を撫でる",99,100),"clock reversal");
  check(!CharacterSwipePolicy.contact("頭を撫でる",10,0),"unset context");
  check(DialogInteractionPolicy.active(true,1000,91000),"dialog active");check(!DialogInteractionPolicy.active(false,1000,91000),"background releases");
  check(!DialogInteractionPolicy.active(true,91000,91000),"idle lease expires");check(DialogInteractionPolicy.IDLE_MS==90000,"bounded lease");
  System.out.println("PASS "+checks+" character cadence/semantic preservation/swipe/foreground lease assertions");
 }
}
