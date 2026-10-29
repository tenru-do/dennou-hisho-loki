package com.example.rokidkeyboardbridge;
/** Explicit everyday relaxation controls. This policy never sends model requests. */
final class ComfortPoseIntent {
 static final String[] COMMANDS={"膝を抱えて座って","うつ伏せで本を読んで","横向きで休んで","軽くストレッチして"};
 static int action(String text){int off=OffDutyPoseIntent.action(text);if(off>=0)return off;String t=RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");if(t.length()>80)return -1;if(adjacent(t))return 0;if(t.matches("(?s).*(ないで|しない|ほしくない|とは|意味|方法|について|彼女|彼が|友人|私が|私は|僕が|引用|と言った|と言って|写真|画像|動画|検索|教えて|[？?]).*"))return -1;
 String tail="(?:みて|ください|ね|よ)?[。！!\\s]*";
 if(t.matches("(?:両膝|膝|両足|両脚)を抱え(?:込んで|て)(?:座って|すわって)?"+tail))return 0;
 if(t.matches("(?:うつ伏せ|うつぶせ)(?:になって|で(?:本を)?(?:読んで|読書して))?"+tail))return 1;
 if(t.matches("(?:横向きで(?:休んで|寝て|くつろいで)|横向きになって)"+tail))return 2;
 if(t.matches("(?:(?:軽く|軽い)?ストレッチして|肩を伸ばして)"+tail))return 3;return -1;}
 static boolean adjacent(String text){String t=RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");return t.matches("(?:(?:隣|となり|そば)(?:に|へ)(?:(?:来て|きて)[、, ]*)?(?:座って|すわって)|こっちに(?:来て|きて)[、, ]*(?:座って|すわって)|(?:隣|となり)で(?:膝|両膝)を抱えて座って)(?:みて|ください|ね|よ|くれる[？?]?)?[。！!\\s]*");}
 static String reply(int action){if(action>=4)return OffDutyPoseIntent.reply(action);return new String[]{"膝を抱えて、ゆっくり座ります。","うつ伏せで本を読みながら、くつろぎます。","横向きになって、ゆっくり休みます。","座ったまま、肩を軽く伸ばします。"}[action];}
 static boolean unsupportedFace(int action){return action==5||action==7||action==8||action==16;}
 static final String FACE_HELP="この姿勢では、落ち着いた表情・微笑み・考える表情・心配そうな表情を選べます。";
 static boolean active(int motion){return motion>=8&&motion<=16;}
 static boolean unsupportedFace(int action,int motion){return unsupportedFace(action)||OffDutyPoseIntent.active(motion)&&(action==6||action==17);}
 private ComfortPoseIntent(){}
}
