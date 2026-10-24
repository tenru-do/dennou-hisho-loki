package com.example.rokidkeyboardbridge;
/** Distance stages per maneuver. Missing/stale fixes never become turn instructions. */
public final class NavigationTurnCue {
 private double walkingSpeed=-1;private String travelMode="walking";private int timing=1;
 public synchronized void configure(String mode,int setting){String next=mode.equals("bicycling")?"bicycling":"walking";int value=Math.max(0,Math.min(2,setting));if(!next.equals(travelMode)||value!=timing){step="";stage=0;lastSample=0;}travelMode=next;timing=value;}
 public synchronized boolean urgent(String distance){double metres=metres(distance);return metres>=0&&metres<=NavigationCueTiming.lead(travelMode,timing,walkingSpeed,3);}
 public synchronized void speed(double value){walkingSpeed=Double.isFinite(value)&&value>=0&&value<=(travelMode.equals("bicycling")?20:3)?value:-1;}
 private String route="",step="";private int stage;private long lastAt,lastSample;
 public static boolean fresh(long now,long sample,double accuracy){return sample>0&&now>=sample&&now-sample<=30000&&Double.isFinite(accuracy)&&accuracy>=0&&accuracy<=40;}
 public synchronized String update(String identity,String maneuver,String distance,long now,long sample,double accuracy,boolean walking,boolean usable){
  if(!identity.equals(route)){route=identity;step="";stage=0;lastAt=lastSample=0;}
  if(!walking||!usable||!fresh(now,sample,accuracy))return "";
  if(sample<=lastSample)return "";lastSample=sample;
  String text=maneuver==null?"":maneuver.trim();
  if(!(text.contains("右折")||text.contains("左折")||text.matches(".*[右左](?:に|へ)(?:曲|進).*")||text.contains("斜め右")||text.contains("斜め左"))||text.contains("右")&&text.contains("左")||text.contains("駅")||text.contains("下車")||text.contains("乗換"))return "";
  double metres=metres(distance);if(metres<0||metres>NavigationCueTiming.lead(travelMode,timing,walkingSpeed,1))return "";
  if(!text.equals(step)){step=text;stage=0;}
  int next=metres<=NavigationCueTiming.lead(travelMode,timing,walkingSpeed,3)?3:metres<=NavigationCueTiming.lead(travelMode,timing,walkingSpeed,2)?2:1;
  if(next<=stage||lastAt>0&&now-lastAt<3500)return "";
  stage=next;lastAt=now;String landmark="";java.util.regex.Matcher named=java.util.regex.Pattern.compile("([一-龠々ァ-ヶーA-Za-z0-9]{2,18}交差点)").matcher(text);if(named.find())landmark=named.group(1)+"で、";String direction=text.contains("斜め")?NavigationConversation.concise(text):text.contains("右")?"右へ。":"左へ。";return (next==3?"すぐ先で":next==2?"まもなく":"約"+(Math.round(metres/10)*10)+"メートル先で")+landmark+direction;
 }
 public static double metres(String value){if(value==null)return -1;String s=value.replace(",","").trim().toLowerCase(java.util.Locale.ROOT);java.util.regex.Matcher m=java.util.regex.Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*(km|m|キロメートル|メートル)$").matcher(s);if(!m.matches())return -1;double n=Double.parseDouble(m.group(1));return Double.isFinite(n)?n*(m.group(2).equals("km")||m.group(2).equals("キロメートル")?1000:1):-1;}
}
