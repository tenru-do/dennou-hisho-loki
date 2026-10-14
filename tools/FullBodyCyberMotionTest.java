package com.example.rokidkeyboardbridge;
import static com.example.rokidkeyboardbridge.FullBodyPoseCatalog.Kind.*;
public final class FullBodyCyberMotionTest {
 static int checks;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
 public static void main(String[] args){
  String[] cues={"エラーウィンドウを払い落としています","強制再起動に抵抗しています","重力増幅で低い姿勢になっています","外部コマンドに抵抗して手首を掴んでいます","つまずいて転倒を防いでいます","風圧に耐えています","悪寒で肩を抱いて震えています","めまいでふらついています","吐き気に耐えています","極狭空間で手のひらを押しています","上方からの重圧に耐えています","警告に怯えて肩を縮めています","背後の気配で硬直しています"};
  for(int g=0;g<cues.length;g++){
   FullBodyDramaticConversationPolicy.Cue c=FullBodyDramaticConversationPolicy.select("","私は"+cues[g]+"。",STRAIN);
   check(c!=null&&c.scene==7+g&&!c.cycle,"self scene "+g);
   check(FullBodyDramaticConversationPolicy.select("","あなたは"+cues[g]+"。",STRAIN)==null,"user excluded");
   check(FullBodyDramaticConversationPolicy.select("","私は"+cues[g]+"。説明します。",STRAIN)==null,"explanation excluded");
   check(FullBodyDramaticGallery.valid(g+7),"gallery scene");
   for(int f=0;f<6;f++)check(FullBodyCyberMotion.groupForPose(365+g*6+f)==g,"pose group");
  }
  check(FullBodyDramaticConversationPolicy.select("","私はめまいはありません。",STRAIN)==null,"negated symptom");
  check(FullBodyDramaticConversationPolicy.ends("私は吐き気がありません。",STRAIN),"negative ends scene");
  check(FullBodyDramaticGallery.LABELS.length==20+FullBodyAttachedMotion.COUNT,"legacy + new gallery");
  check(FullBodyDramaticConversationPolicy.recovery("私はエラーを解消しました。"),"recovery");
  for(long t=0;t<7200;t++)check(FullBodyCyberMotion.position(t)>=0&&FullBodyCyberMotion.position(t)<6,"loop bounds");
  float[] mesh=new float[(FullBodyBreathPolicy.COLUMNS+1)*(FullBodyBreathPolicy.ROWS+1)*2];
  FullBodyBreathPolicy.resting(mesh,240,601,350,.2f);float[] before=mesh.clone();FullBodyCyberMotion.rough(mesh,240,601,350,.2f);
  boolean moved=false;for(int row=0;row<=32;row++)for(int col=0;col<=16;col++){int i=(row*17+col)*2;check(mesh[i]==before[i],"no horizontal distortion");if(row/32f<=.225f||row==32)check(mesh[i+1]==before[i+1],"face floor fixed");moved|=mesh[i+1]!=before[i+1];}
  check(moved,"rough breath moves");System.out.println("PASS cyber scenes "+checks+" checks");
 }
}
