package com.example.rokidkeyboardbridge;
/** Latest visual request and actual-render receipt. No provider, persistence or commands. */
final class AvatarActionDirector {
 static final int STAND=0,SEAT=1,LIE=2,ANY=-1;
 private int drawn=-1;int drawn(){return drawn;}
 private long epoch,at=-1,confirmed=-1,lastFrame=-1,recovered=-30000;private int expected=-1,attempts;private boolean pending,failed;private String request="";
 static boolean stop(String text){String t=RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");return t.matches("(?:動き(?:を|は)?(?:止めて|停止して)|動かないで|その姿勢で止まって|仕草を止めて)[。！!\\s]*");}
 static int posture(String text){int p=EmbodiedScenePolicy.postureCommand(text);return p>=0?p:RestPoseIntent.wantsLie(RestPoseIntent.kind(text))?LIE:ANY;}
 long begin(String text,long now){String t=text==null?"":text;if(t.equals(request)&&pending&&now>=at&&now-at<1500)return epoch;epoch++;attempts=0;request=t;at=now;confirmed=-1;failed=false;pending=true;expected=posture(t);return epoch;}
 long epoch(){return epoch;}boolean current(long ticket){return ticket==epoch;}
 void passiveFrame(int posture,long now){drawn=posture;if(expected>=0)frame(posture,now);else lastFrame=now;}
 void frame(int posture,long now){drawn=posture;lastFrame=now;if(pending&&!failed&&(expected==ANY||expected==posture)){pending=false;confirmed=now;}}
 void fail(long now){if(at<0)at=now;failed=true;pending=false;confirmed=-1;}
 boolean recovery(long now){if(pending&&now>=at&&now-at>=6000)fail(now);if(!failed||attempts>=1||now<recovered||now-recovered<20000)return false;recovered=now;attempts++;return true;}
 void retry(long now){retry(now,false);}
 void retry(long now,boolean manual){if(manual)attempts=0;failed=false;pending=true;at=now;confirmed=-1;epoch++;}
 void stop(long now){epoch++;pending=failed=false;confirmed=-1;at=now;request="";expected=ANY;}
 boolean pending(){return pending;}boolean failed(){return failed;}long lastFrame(){return lastFrame;}
 String label(long now){if(pending&&now>=at&&now-at>=6000)fail(now);if(failed)return "姿勢表示を確認できません・SETで復旧";if(pending)return "姿勢を準備中";return confirmed>=0&&now>=confirmed&&now-confirmed<2400?"姿勢を表示しました":"";}
 static String acknowledgement(int posture){return posture==STAND?"立つ姿へ切り替えます。":posture==SEAT?"座る姿へ切り替えます。":posture==LIE?"寝姿へ切り替えます。":"動作を切り替えます。";}
}
