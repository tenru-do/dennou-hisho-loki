package com.example.rokidkeyboardbridge;
public final class NavigationCommuteVoice {
 public static String intent(String input,boolean active){if(!active||input==null)return "";String s=input.trim().replaceAll("[\\s　。？！?!]","");if(s.matches("次は|つぎは|次に何する|次は何をする"))return "next";if(s.matches("あと何駅|あと何停留所|残り何駅|残り何停留所"))return "stops";if(s.matches("どこで降りる|どこで降りればいい|降りる駅は|降りる停留所は"))return "alight";if(s.matches("乗れなかった|乗り遅れた|電車に乗れなかった|バスに乗れなかった"))return "missed";return "";}
 private NavigationCommuteVoice(){}
}
