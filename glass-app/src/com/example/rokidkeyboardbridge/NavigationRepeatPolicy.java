package com.example.rokidkeyboardbridge;
public final class NavigationRepeatPolicy {
 public static String text(boolean active,boolean usable,long now,long sample,double accuracy,String instruction,String distance){if(!active||!usable||!NavigationTurnCue.fresh(now,sample,accuracy)||instruction==null||instruction.trim().isEmpty()||instruction.contains("確認中")||instruction.contains("更新中"))return "";String s=instruction.trim();if(s.length()>160)s=s.substring(0,160);String d=distance==null?"":distance.trim();return d.isEmpty()?s:d+"先、"+s;}
 private NavigationRepeatPolicy(){}
}
