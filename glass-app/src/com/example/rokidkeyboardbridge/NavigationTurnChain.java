package com.example.rokidkeyboardbridge;
/** A close following maneuver is part of the current cue, never a separate queued instruction. */
public final class NavigationTurnChain {
 public static String append(String cue,String following,String distance,String mode,double speed){if(cue==null||cue.isEmpty()||following==null||!(mode.equals("walking")||mode.equals("bicycling")))return cue;String next=following.trim();if(next.length()>80||next.contains("駅")||next.contains("下車")||next.contains("乗換")||!(next.contains("右折")||next.contains("左折")||next.contains("右に曲")||next.contains("左に曲")))return cue;double metres=NavigationTurnCue.metres(distance),v=Double.isFinite(speed)&&speed>=0?speed:mode.equals("bicycling")?4:1.2;double limit=mode.equals("bicycling")?Math.max(60,Math.min(120,v*15)):Math.max(25,Math.min(45,v*20));if(metres<0||metres>limit)return cue;return cue+" その後すぐ、"+(next.contains("右")&&!next.contains("左")?"右折です。":next.contains("左")&&!next.contains("右")?"左折です。":NavigationConversation.concise(next));}
 private NavigationTurnChain(){}
}
