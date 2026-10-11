package com.example.rokidkeyboardbridge;
/** Explicit standing panting only; no unsolicited idle or seated posture change. */
final class FullBodyHeavyBreathPolicy {
 static final String[] ASSETS={"loki_heavy_breath_base_v462.png","loki_heavy_breath_inhale_v462.png","loki_heavy_breath_exhale_v462.png"};
 static boolean eligible(FullBodyPoseCatalog.Kind kind,boolean seated){return kind==FullBodyPoseCatalog.Kind.PANT&&!seated;}
 static int frame(long elapsed){long t=Math.max(0,elapsed)%1000L;return t<200?0:t<450?1:t<600?0:2;}
}
