package com.example.rokidkeyboardbridge;
public final class NavigationCueTiming {
 public static double lead(String mode,int setting,double speed,int stage){setting=Math.max(0,Math.min(2,setting));if(mode.equals("bicycling")){double scale=setting==0?.67:setting==2?1.5:1;if(!Double.isFinite(speed)||speed<0||speed>20){double first=setting==0?150:setting==2?400:250;return stage==1?first:stage==2?first*.35:30;}double first=Math.max(150,Math.min(500,speed*45))*scale;return stage==1?first:stage==2?Math.max(45,Math.min(180,speed*15))*scale:Math.max(15,Math.min(50,speed*5));}double scale=setting==0?2.0/3:setting==2?1.5:1;return NavigationRefinements.lead(speed,stage)*scale;}
 private NavigationCueTiming(){}
}
