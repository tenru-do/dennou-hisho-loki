package com.example.rokidkeyboardbridge;
public final class CharacterRapportPolicyTest {
 static int n;static void ok(boolean v,String m){n++;if(!v)throw new AssertionError(m);}
 public static void main(String[]args){
 ok(CharacterSwipePolicy.feedback(false,true),"pending invitation accepts glass swipe without physical conversation");ok(!CharacterSwipePolicy.feedback(false,false),"idle dpad remains ordinary");ok(CharacterSwipePolicy.feedback(true,false),"prior contact swipe preserved");
 CharacterRapportPolicy p=new CharacterRapportPolicy();p.user("甘えて",100);p.answer("手を差し出して待っています。",FullBodyPoseCatalog.Kind.SHY,200);
 ok(!p.waiting(500),"anticipation");ok(p.waiting(1500),"wait for human");ok(p.touch(1600),"accept touch");ok(!p.touch(1700),"no repeated accept");ok(p.cell(2000)==7,"happy acknowledgement");ok(!p.active(6400),"settles after deadline");
 for(int level=0;level<3;level++){CharacterMotionTuning.set(level);p.preview(10000);for(int mode=0;mode<3;mode++)for(int t=0;t<8000;t+=10){float a=p.envelope(10000+t,mode,false);ok(Float.isFinite(a)&&a>=0&&a<=1.57f,"bounded phase");ok(p.envelope(10000+t,mode,true)==0,"quiet priority");}ok(p.envelope(12000,2,false)<p.envelope(12000,0,false),"reading reduced");}
 CharacterMotionTuning.set(1);p.preview(20000);float before=p.lean(21500,0,false);p.preview(30000);ok(before*p.lean(31500,0,false)<0,"alternates sides");p.user("やめて",32000);ok(!p.active(32000)&&!p.touch(32000),"stop clears");p.preview(40000);p.user("天気を教えて",41000);ok(!p.active(41000),"new request clears");p.user("甘えて",50000);p.answer("できません",FullBodyPoseCatalog.Kind.REFUSE,50100);ok(!p.active(50200),"refusal preserved");
 p.preview(60000);int cols=16,rows=32,w=500,h=900;float[] mesh=new float[(cols+1)*(rows+1)*2];for(int y=0;y<=rows;y++)for(int x=0;x<=cols;x++){int i=(y*(cols+1)+x)*2;mesh[i]=w*x/(float)cols;mesh[i+1]=h*y/(float)rows;}float[] copy=mesh.clone();p.deform(mesh,cols,rows,w,h,62000,0,false,.22f);boolean moved=false;for(int y=0;y<=rows;y++)for(int x=0;x<=cols;x++){int i=(y*(cols+1)+x)*2;if(y/(float)rows<=.22f||y/(float)rows>=.60f)ok(mesh[i]==copy[i]&&mesh[i+1]==copy[i+1],"face and floor fixed");else moved|=mesh[i]!=copy[i];}ok(moved,"shoulder coordination");
 for(int i=0;i<32;i++)ok(CharacterMotionTuning.variant(i)!=CharacterMotionTuning.variant(i+1),"no adjacent idle repetition");System.out.println("PASS "+n+" rapport phase/touch/variation/reading/settings/mesh checks");
 }
}
