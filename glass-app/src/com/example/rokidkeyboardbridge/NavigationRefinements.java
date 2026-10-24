package com.example.rokidkeyboardbridge;
public final class NavigationRefinements {
 public static double lead(double speed,int stage){if(!Double.isFinite(speed)||speed<0||speed>3)return stage==1?120:stage==2?35:12;double v=speed;return stage==1?Math.max(60,Math.min(120,v*55)):stage==2?Math.max(22,Math.min(45,v*22)):Math.max(8,Math.min(15,v*8));}
 public static boolean genericRepeat(String s){return s!=null&&s.replaceAll("[\\s　。?!？！]","").matches("もう一回|もう一度|もう一回お願い|もう一度お願い");}
 public static String repeatTarget(long now,long answerAt,long navAt){boolean a=answerAt>0&&now>=answerAt&&now-answerAt<=300000,n=navAt>0&&now>=navAt&&now-navAt<=300000;return a&&(!n||answerAt>navAt)?"answer":n?"navigation":"ask";}
 public static boolean emphasize(String instruction,String distance,boolean fresh){double m=NavigationTurnCue.metres(distance);return fresh&&m>=0&&m<=45&&instruction!=null&&(instruction.contains("右")||instruction.contains("左")||instruction.contains("分岐"));}
 public static String focusText(String instruction,String distance){String s=instruction==null?"":instruction;String arrow=s.contains("右")&&!s.contains("左")?"➜":s.contains("左")&&!s.contains("右")?"⬅":"分岐";return arrow+" "+(distance==null?"":distance)+"\n"+NavigationConversation.concise(s);}
 private NavigationRefinements(){}
}
