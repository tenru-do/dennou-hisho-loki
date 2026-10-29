package com.example.rokidkeyboardbridge;
/** Bounded visual hint. Never changes contact consent, conversation or posture. */
final class CharacterContactPoint {
 static final int HAND=0,CHEEK=1,SHOULDER=2;
 private static long at=-1;private static float x=.5f,y=.45f;private static int zone=SHOULDER;
 static int zone(float vertical,boolean portrait){return vertical<(portrait?.68f:.24f)?CHEEK:vertical<(portrait?.88f:.45f)?SHOULDER:HAND;}
 static void note(float horizontal,float vertical,boolean portrait,long now){note(horizontal,vertical,portrait,false,now);}
 static void note(float horizontal,float vertical,boolean portrait,boolean seated,long now){if(!Float.isFinite(horizontal)||!Float.isFinite(vertical))return;x=Math.max(0,Math.min(1,horizontal));y=Math.max(0,Math.min(1,vertical));float local=y-(!portrait&&seated?.18f:0);zone=zone(local,portrait);if(!portrait&&Math.abs(x-.5f)>.12f&&local>.34f&&local<.75f)zone=HAND;at=now;}
 static int context(String text){String t=text==null?"":text;return t.matches("(?s).*(頬|ほほ|顔|耳|髪|頭).*" )?CHEEK:t.matches("(?s).*(手を|手に|手の|指先|握手).*" )?HAND:SHOULDER;}
 static int resolve(String text,long now){return at>=0&&now>=at&&now-at<=350?zone:context(text);}
 static float horizontal(long now){return at>=0&&now>=at&&now-at<=350?x:.5f;}
 static float vertical(int kind,boolean portrait){return kind==CHEEK?(portrait?.47f:.14f):kind==HAND?(portrait?.94f:.55f):(portrait?.81f:.32f);}
 private CharacterContactPoint(){}
}
