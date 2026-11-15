package com.example.rokidkeyboardbridge;
/** Four genuine in-between drawings supplement each original four-frame greeting. */
final class GreetingBridgeAtlas {
 static String asset(int clip){if(clip==0)return "loki_standing_greeting_bridge_v750.png";if(clip==1)return "loki_seated_greeting_bridge_v750.png";throw new IllegalArgumentException("greeting bridge clip");}
 private GreetingBridgeAtlas(){}
}
