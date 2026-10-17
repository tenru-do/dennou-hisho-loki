package com.example.rokidkeyboardbridge;
/** Shared ordered scene registry: speech keywords, hidden model actions and review export. */
final class FullBodyConversationScenes {
 static final class Rule {
  final int scene;final String key;final String[][] words;
  Rule(int scene,String key,String[][] words){this.scene=scene;this.key=key;this.words=words;}
  boolean matches(String text){for(String[] group:words)if(!FullBodyDramaticConversationPolicy.has(text,group))return false;return true;}
  int effects(){if(FullBodyAttachedMotion.group(scene)>=0)return FullBodyAttachedMotion.effects(scene);return scene>=7?FullBodyCyberMotion.effects(scene):scene>=2?FullBodyDramaticConversationPolicy.SWEAT:0;}
 }
 static final Rule[] ALL={
  new Rule(23,"LOW_BRACED",new String[][]{{"低い姿勢","腰を落と"},{"踏ん張","足を開","膝に手"}}),
  new Rule(20,"CROUCH_STRAIN",new String[][]{{"しゃがみ","しゃがんで","しゃがんだ"},{"息が荒","呼吸が荒","息苦し","負荷に耐"}}),
  new Rule(21,"SLIP_BALANCE",new String[][]{{"足を滑らせ","滑って転びかけ","尻もちをつきかけ","転倒しかけ"}}),
  new Rule(22,"OVERHEAD_STRETCH",new String[][]{{"伸びをしています","伸びをして","背伸びをして","ストレッチをして","両腕を上げて伸び"}}),
  new Rule(7,"ERROR_DISMISS",new String[][]{{"エラーウィンドウ","警告ウィンドウ","見えないウィンドウ","エラー画面","警告画面"},{"払","閉じ","消去","押し","消す"}}),
  new Rule(8,"REBOOT_RESIST",new String[][]{{"強制再起動","メモリ過負荷","脳内エラー","頭部の負荷","メモリの過負荷","再起動に抗"}}),
  new Rule(9,"GRAVITY_CROUCH",new String[][]{{"重力増幅","重力エラー","見えない重圧","重力が強"},{"しゃが","膝","低","床","沈み"}}),
  new Rule(10,"PROGRAM_RESIST",new String[][]{{"強制プログラム","外部コマンド","行動制限","強制コマンド"},{"手首","抵抗","止め","抑え"}}),
  new Rule(11,"BALANCE_RECOVER",new String[][]{{"つまず","躓","転倒を防","バランスを崩","足を取ら","転びそう","よろけ","おっとっと"}}),
  new Rule(12,"FRONT_PRESSURE",new String[][]{{"強烈なG","強いG","急激なG","風圧","正面からの圧力","向かい風","加速度の負荷"}}),
  new Rule(13,"CHILL",new String[][]{{"悪寒","寒気","凍え","冷気","寒く"},{"肩","抱","縮","震","冷え"}}),
  new Rule(14,"DIZZY",new String[][]{{"めまい","目眩","平衡感覚","ふらつ","くらくら","クラクラ"}}),
  new Rule(15,"NAUSEA",new String[][]{{"吐き気","胃部不快","みぞおち","胃がむか","胃の不快","気分が悪"}}),
  new Rule(16,"NARROW_SPACE",new String[][]{{"極狭空間","狭い空間","透明な壁","狭い場所"},{"押","縮","手のひら","手を突き"}}),
  new Rule(17,"FLOOR_PRESSURE",new String[][]{{"上方からの重圧","床への圧力","床に押し","上から押さ","床で重圧"}}),
  new Rule(18,"WARNING_TENSE",new String[][]{{"叱責","威圧","警告に怯","叱ら","厳しい警告"},{"肩","縮","怯","怖","緊張"}}),
  new Rule(19,"BEHIND_STARTLE",new String[][]{{"背後の気配","背後からの音","後ろの気配","後ろから音"},{"硬直","驚","身構","戦慄","びく"}}),
  new Rule(1,"LOCK_KNEEL",new String[][]{{"バリア","プログラムロック","動作ロック","制御ロック"},{"膝","ひざ","膝立ち"}}),
  new Rule(0,"BARRIER_STAND",new String[][]{{"バリア","プログラムロック","動作ロック","制御ロック"}}),
  new Rule(4,"OVERRIDE_ARM",new String[][]{{"ウイルス","オーバーライド","ハッキング","制御を取り戻","腕の震えを抑"}}),
  new Rule(6,"LOW_BREATH",new String[][]{{"両手と両膝","両手と膝","床に手","四つん這い","低い姿勢"},{"環境異常","環境シミュレーション","呼吸","息"}}),
  new Rule(5,"AIR_STRAIN",new String[][]{{"環境異常","環境シミュレーション","酸欠","気圧異常","首元","喉元","鎖骨"},{"呼吸","息"}}),
  new Rule(2,"WALL_TENSION",new String[][]{{"壁に背","壁に寄","壁際","壁にもたれ"}}),
  new Rule(3,"HAND_CHEST",new String[][]{{"胸元に手","胸元に両手","両手を胸元","胸元で両手"}})
 };
 static Rule action(String action){if(action==null)return null;for(Rule r:ALL)if(action.equals("SCENE_"+r.key))return r;return null;}
 static String actions(){StringBuilder b=new StringBuilder();for(Rule r:ALL){if(b.length()>0)b.append(',');b.append("SCENE_").append(r.key);}return b.toString();}
 static String guidance(){StringBuilder b=new StringBuilder();for(Rule r:ALL)b.append("SCENE_").append(r.key).append('=').append(FullBodyDramaticGallery.LABELS[r.scene]).append(';');return b.toString();}
}
