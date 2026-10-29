package com.example.rokidkeyboardbridge;
/** Bounded, local avatar display commands; ordinary questions and quoted speech remain conversation. */
final class RestAnimationIntent {
 static int action(String text){String t=RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");if(t.length()>80||t.matches("(?s).*(ないで|しない|ほしくない|とは|意味|方法|について|彼女|彼が|友人|私が|私は|僕が|引用|と言った|と言って|写真|動画|検索|教えて|[？?]).*"))return -1;
 if(t.matches(".*(?:寝返りして|寝返りを打って|寝返りをうって)(?:みて|ください|ね)?[。！!\\s]*"))return 1;
 if(t.matches(".*(?:背伸びして|背伸びをして|伸びをして)(?:みて|ください|ね)?[。！!\\s]*"))return 2;
 if(t.matches(".*(?:照れた身じろぎをして|照れて身じろぎして|照れて身をよじって|もじもじして)(?:みて|ください|ね)?[。！!\\s]*"))return 3;
 if(EmbodiedScenePolicy.waveCommand(t.replaceFirst("^(寝たまま|寝そべったまま)[、,\\s]*","")))return 4;
 if(t.matches(".*(?:表情をおまかせにして|表情を自動にして)(?:ください|ね)?[。！!\\s]*"))return 13;
 if(t.matches(".*(?:うれしい顔にして|嬉しい顔にして|喜んだ顔にして)(?:ください|ね)?[。！!\\s]*"))return 14;
 if(t.matches(".*(?:考える顔にして|考え中の顔にして)(?:ください|ね)?[。！!\\s]*"))return 15;
 if(t.matches(".*(?:驚いた顔にして|びっくりした顔にして)(?:ください|ね)?[。！!\\s]*"))return 16;
 if(t.matches(".*(?:心配そうな顔にして|気遣う顔にして)(?:ください|ね)?[。！!\\s]*"))return 17;
 if(t.matches(".*(?:立膝|膝を立てた|立て膝).*(?:身じろぎして|身じろぎをして|もじもじして)(?:みて|ください|ね)?[。！!\\s]*"))return 11;
 if(t.matches(".*(?:身じろぎを止めて|身じろぎをやめて|その姿勢のまま止めて)(?:ください|ね)?[。！!\\s]*"))return 12;
 if(t.matches(".*(?:立膝にして|立て膝にして|片膝を立てて|膝を立てて)(?:みて|ください|ね)?[。！!\\s]*"))return 9;
 if(t.matches(".*(?:脚を伸ばして|足を伸ばして|膝を下ろして|立膝を戻して)(?:みて|ください|ね)?[。！!\\s]*"))return 10;
 if(t.matches(".*(?:キス顔にして|キス顔をして|キス顔を見せて)(?:みて|ください|ね)?[。！!\\s]*"))return 5;
 if(t.matches(".*(?:困った顔にして|困り顔にして)(?:みて|ください|ね)?[。！!\\s]*"))return 6;
 if(t.matches(".*(?:せつない顔にして|切ない顔にして)(?:みて|ください|ね)?[。！!\\s]*"))return 7;
 if(t.matches(".*(?:耐える顔にして|こらえる顔にして|気持ちをこらえた顔にして)(?:みて|ください|ね)?[。！!\\s]*"))return 8;return -1;}
 static boolean applies(String text,boolean lying){int a=action(text);return a>=0&&(lying||a==1||RestPoseIntent.normalize(text).matches(".*(?:寝たまま|寝そべ|横にな).*"));}
 static String reply(int action){return new String[]{"","寝返りを打ちます。","寝たまま背伸びします。","寝たまま、照れた仕草に切り替えます。","寝たまま手を振ります。","寝姿のままキス顔に切り替えます。","寝姿のまま困った表情に切り替えます。","寝姿のまませつない表情に切り替えます。","寝姿のまま気持ちをこらえる表情に切り替えます。","寝たまま片膝を立てます。","寝たまま脚を伸ばします。","立膝を保ったまま、照れた小さな仕草に切り替えます。","身じろぎを止め、今の寝姿を保ちます。","寝姿の表情を会話に合わせます。","寝姿のままうれしい表情に切り替えます。","寝姿のまま考える表情に切り替えます。","寝姿のまま驚いた表情に切り替えます。","寝姿のまま気遣う表情に切り替えます。"}[action];}
}
