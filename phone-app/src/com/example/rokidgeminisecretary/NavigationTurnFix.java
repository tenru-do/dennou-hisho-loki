package com.example.rokidgeminisecretary;
public final class NavigationTurnFix {public static boolean fresh(long now,long sample,double accuracy){return sample>0&&now>=sample&&now-sample<=30000&&Double.isFinite(accuracy)&&accuracy>=0&&accuracy<=40;}private NavigationTurnFix(){}}
