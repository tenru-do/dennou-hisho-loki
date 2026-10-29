package com.example.rokidkeyboardbridge;
public final class CharacterGestureLifeTest {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  CharacterGestureLife g=new CharacterGestureLife();
  for(int level=0;level<3;level++){CharacterMotionTuning.set(level);
   for(int k=0;k<8;k++){g.event(k,10000);ok(g.kind()>=0&&g.kind()<8,"event registered");
    for(int mode=0;mode<3;mode++)for(int a=-100;a<5000;a+=20){long t=10000+a;
     float v=g.amount(t,mode,false);ok(Float.isFinite(v)&&v>=0&&v<=1.25f,"bounded event");ok(g.amount(t,mode,true)==0&&g.tilt(t,mode,true)==0&&g.lean(t,mode,true)==0,"navigation priority");
     ok(Math.abs(g.tilt(t,mode,false))<3&&Math.abs(g.nod(t,mode,false))<.011&&Math.abs(g.lean(t,mode,false))<.012,"no extreme body distortion");
     if(a<0||a>=4400)ok(v==0,"event ends and reversed clock excluded");
    }
   }
  }
  g.event(4,10000);g.event(4,15000);ok(g.kind()==7,"no consecutive same joy");g.answer("訂正します",FullBodyPoseCatalog.Kind.EXPLAIN,20000);ok(g.kind()==5,"correction priority");g.answer("右へ進んでください",FullBodyPoseCatalog.Kind.EXPLAIN,21000);ok(g.kind()==3,"direction cue");g.answer("いいえ",FullBodyPoseCatalog.Kind.REFUSE,22000);ok(!g.moving(22001),"refusal clears visual cue");
  for(int f=0;f<4;f++){CharacterMotionTuning.intimacy(f);long period=CharacterGestureLife.invitationPeriod();ok(f!=0||period==0,"off disables invitation");if(period>0){ok(CharacterGestureLife.invitation(period+16000,0,false)>0,"invitation reachable");ok(CharacterGestureLife.invitation(period+16000,1,false)==0&&CharacterGestureLife.invitation(period+16000,2,false)==0&&CharacterGestureLife.invitation(period+16000,0,true)==0,"no invitation while busy");}}
  g.event(2,1000);int cols=16,rows=32;float[] mesh=new float[(cols+1)*(rows+1)*2];g.deform(mesh,cols,rows,500,900,2200,0,false,.24f);boolean changed=false;for(int y=0;y<=rows;y++)for(int x=0;x<=cols;x++){int i=(y*(cols+1)+x)*2;if(y/(float)rows<=.24f||y/(float)rows>=.70f)ok(mesh[i]==0&&mesh[i+1]==0,"face and feet stay fixed");else changed|=mesh[i]!=0||mesh[i+1]!=0;}ok(changed,"shoulder band moves");
  g.touch(10000,1,600,true);float slow=g.touch(11000,false);g.touch(13000,1,100,true);float tap=g.touch(14000,false);ok(slow!=tap,"slow stroke differs from tap");g.touch(13200,2,100,true);ok(g.touch(13600,true)==0,"touch quiet");g.clear();ok(!g.moving(14000),"release clears");
  CharacterTouchTempo.note(100,700);ok(CharacterTouchTempo.duration(150)==700&&CharacterTouchTempo.duration(500)==0&&CharacterTouchTempo.duration(99)==0,"touch duration bounded lifetime");
  CharacterMotionTuning.set(1);CharacterMotionTuning.intimacy(1);System.out.println("PASS "+n+" gesture lifecycle, context, touch, preferences, anchors and quiet checks");
 }
}
