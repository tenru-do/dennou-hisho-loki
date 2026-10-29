package com.example.rokidkeyboardbridge;
/** Explicit everyday gestures; no automatic clothing changes or sensor work. */
final class PoisedMotionPolicy {
 static boolean valid(String t){return !EmbodiedScenePolicy.excluded(t)&&!EmbodiedScenePolicy.has(t,"ないで","しない","と言った","と言って","他の人","彼女","彼が");}
 static int command(String text){String t=EmbodiedScenePolicy.clean(text);if(!valid(t))return -1;
 if(EmbodiedScenePolicy.has(t,"髪を耳にかけて","髪を耳へかけて","髪をかき上げて"))return 2;
 if(EmbodiedScenePolicy.has(t,"肩越しに振り向いて","肩越しに振り返って","振り向いて"))return 3;
 if(EmbodiedScenePolicy.has(t,"流し目して","流し目をして","こちらに視線を向けて","目線を向けて"))return 1;
 if(EmbodiedScenePolicy.has(t,"落ち着いた仕草をして","仕草を順番に見せて"))return 0;return -1;}
 static int relaxed(String text,boolean reclining){String t=EmbodiedScenePolicy.clean(text);int rest=RestPoseIntent.kind(t);if(RestPoseIntent.wantsLie(rest)&&!t.contains("上から"))return 2;if(rest==RestPoseIntent.RELAX)return reclining?2:0;if(!valid(t))return -1;
 boolean lie=!EmbodiedScenePolicy.has(t,"座って","すわって")&&(reclining||EmbodiedScenePolicy.has(t,"寝そべ","寝転","横にな","横たわ","ソファ"));
 if(EmbodiedScenePolicy.has(t,"襟元をゆるめて","襟元に触れて","襟に触れて","襟元を緩めて"))return lie?3:1;
 if(EmbodiedScenePolicy.has(t,"脚を少し開いて","足を少し開いて","脚を開いて","足を開いて","膝元に手を重ねて","膝に手を重ねて","リラックスして座って"))return lie?2:0;
 if(EmbodiedScenePolicy.has(t,"ソファで寝そべって","リラックスして寝そべって","ソファに横になって"))return 2;return -1;}
 static int expression(String text){String t=EmbodiedScenePolicy.clean(text);if(!valid(t))return -1;if(EmbodiedScenePolicy.has(t,"焦った顔にして","焦り顔にして","焦っている顔にして"))return 4;if(EmbodiedScenePolicy.has(t,"泣き顔にして","泣いた顔にして"))return 5;if(EmbodiedScenePolicy.has(t,"息が上がった顔にして","呼吸の荒い顔にして","呼吸が荒くなった顔にして","呼吸の荒くなっている顔にして"))return 6;if(EmbodiedScenePolicy.has(t,"普通の表情に戻して","落ち着いた表情にして"))return 0;if(EmbodiedScenePolicy.has(t,"ウインクして","ウィンクして"))return 3;if(EmbodiedScenePolicy.has(t,"照れ笑いして","照れた顔にして"))return 2;if(EmbodiedScenePolicy.has(t,"微笑んで","にっこりして"))return 1;return -1;}
 static int frame(long age,int selected){long t=Math.max(0,age);if(selected==0){int[] s={0,1,0,2,0,3,0};return s[Math.min(s.length-1,(int)(t/1000))];}return t<500?0:t<1100?Math.min(1,selected):Math.max(0,Math.min(3,selected));}
}
