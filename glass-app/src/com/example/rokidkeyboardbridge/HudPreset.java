package com.example.rokidkeyboardbridge;
/** Changes layout only. No camera, recording, navigation, network or audio action. */
public final class HudPreset {
 public static String order(String mode){switch(mode){case "conversation":return "VOICE,TOPIC,PLAY";case "camera":return "READ,CAM,KEEP,VOICE";case "travel":return "NAV,VOICE,READ";default:throw new IllegalArgumentException("配置を選び直してください");}}
 private HudPreset(){}
}
