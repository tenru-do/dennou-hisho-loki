package com.example.rokidkeyboardbridge;
/** Individually approved original-outfit stills, selected explicitly in the gallery. */
final class FullBodyDramaticGallery {
 // Lower-body anchors measured from the source cells; keep feet/knees fixed during blending.
 static final float[] EXHALE_SHIFT_X={0.03969071f,0.00168373f,0.08532680f,0.03831364f,0.04821716f,0.03478413f,0.01416236f};
 static final float[] EXHALE_SHIFT_Y={-0.00092081f,0f,0f,0f,0f,0f,-0.00092081f};
 static final String[] BREATH_SHEETS={"loki_barrier_standing_breath_v467.png","loki_lock_kneeling_breath_v467.png","loki_pressure_wall_breath_v467.png","loki_pressure_breath_breath_v467.png","loki_override_arm_breath_v467.png","loki_environment_air_breath_v467.png","loki_environment_low_breath_v467.png"};
 static final String[] ASSETS={"loki_barrier_standing_v466.png","loki_lock_kneeling_v466.png","loki_pressure_wall_v466.png","loki_pressure_breath_v466.png","loki_override_arm_v466.png","loki_environment_air_v466.png","loki_environment_low_v466.png"};
 static final String[] LABELS={"\u5834\u9762\uff1a\u30d0\u30ea\u30a2\u505c\u6b62\uff08\u7acb\u4f4d\uff09","\u5834\u9762\uff1a\u30d7\u30ed\u30b0\u30e9\u30e0\u30ed\u30c3\u30af\uff08\u819d\u7acb\u3061\uff09","\u5834\u9762\uff1a\u58c1\u969b\u3067\u306e\u52d5\u63fa","\u5834\u9762\uff1a\u7dca\u5f35\u3057\u3066\u547c\u5438\u3092\u6574\u3048\u308b","\u5834\u9762\uff1a\u5236\u5fa1\u306e\u596a\u9084","\u5834\u9762\uff1a\u74b0\u5883\u7570\u5e38\uff08\u7acb\u4f4d\uff09","\u5834\u9762\uff1a\u74b0\u5883\u7570\u5e38\uff08\u4f4e\u59ff\u52e2\uff09","場面：エラーウィンドウの拒絶","場面：強制再起動への抵抗","場面：重力増幅・低姿勢","場面：外部コマンドへの抵抗","場面：つまずき・転倒回避","場面：正面の圧力に耐える","場面：悪寒・肩を抱く","場面：めまい・平衡感覚","場面：胃部不快感","場面：狭い空間への抵抗","場面：床で重圧に耐える","場面：警告に怯える","場面：背後の気配への硬直","場面：しゃがんで呼吸を整える","場面：足を滑らせバランスを取る","場面：疲れをほぐす伸び","場面：腰を落として踏ん張る"};
 static final String[] MOTION_SHEETS={"loki_barrier_standing_motion_v468.png","loki_lock_kneeling_motion_v468.png","loki_pressure_wall_motion_v468.png","loki_pressure_breath_motion_v468.png","loki_override_arm_motion_v468.png","loki_environment_air_motion_v468.png","loki_environment_low_breath_v467.png"};
 static final String BRIDGE_SHEET="loki_stress_bridge_motion_v468.png";
 static boolean valid(int id){return id>=0&&id<LABELS.length;}
}
