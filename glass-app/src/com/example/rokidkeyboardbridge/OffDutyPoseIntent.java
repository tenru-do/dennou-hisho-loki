package com.example.rokidkeyboardbridge;
final class OffDutyPoseIntent {
 static final String FACE_HELP="この寝姿では、落ち着いた表情・微笑み・考える表情・眠そうな表情を選べます。";
 static final String[] COMMANDS={"枕を抱えて横になって","両手を頭の後ろに置いて休んで","寝たまま頬に手を添えて","悪夢でうなされている感じにして"};
 static String clean(String text){return RestPoseIntent.normalize(text).replaceFirst("^ロキ[、, :：]*","");}
 static boolean excluded(String t){return t.length()>90||t.matches("(?s).*(ないで|しない|ほしくない|とは|意味|方法|について|彼女|彼が|友人|私が|私は|僕が|引用|と言った|と言って|写真|画像|動画|検索|教えて|[？?]).*");}
 static int action(String text){if(RestLegRelaxIntent.request(text))return 8;String t=clean(text);if(excluded(t))return -1;String tail="(?:みて|ください|ね|よ)?[。！!\\s]*";
 if(t.matches("(?:枕|抱き枕|クッション)を(?:抱え(?:て|込んで)(?:横になって|寝て|休んで|くつろいで)?|(?:体|胸|お腹)に(?:載せて|乗せて|のせて))"+tail)||casual(text))return 4;
 if(t.matches("(?:(?:寝たまま|寝そべって|仰向けで)[、, ]*)?(?:両手|手)を頭の後ろに(?:置いて|おいて|回して)(?:休んで|くつろいで)?"+tail))return 5;
 if(t.matches("(?:寝たまま|寝そべったまま|仰向けで)?頬に手を添えて"+tail))return 6;
 if(t.matches("(?:悪夢で(?:うなされて|うなされている感じにして)|うなされる寝顔にして)"+tail))return 7;return -1;}
 static boolean casual(String text){String t=clean(text);return !excluded(t)&&t.matches("(?:(?:秘書っぽくなく|オフの感じで|休日みたいに|だらっと|だらだら)(?:くつろいで|寝そべって|休んで)|ソファでだらけて)(?:ください|ね)?[。！!\\s]*");}
 static boolean sleepy(String text){String t=clean(text).replaceFirst("^(?:寝たまま|寝そべったまま)[、, ]*","");return !excluded(t)&&t.matches("(?:眠そうな顔にして|うとうとして|目を閉じて休んで)(?:ください|ね)?[。！!\\s]*");}
 static boolean shift(String text){String t=clean(text);return !excluded(t)&&t.matches("(?:寝たまま|寝そべったまま)?(?:身もだえして|身悶えして|もぞもぞして|小さく身をよじって|身じろぎして)(?:ください|ね)?[。！!\\s]*");}
 static boolean unavailable(String text){String t=clean(text);return !excluded(t)&&t.matches("(?:抱き枕|枕|クッション)を(?:膝|足|脚)の間に(?:挟んで|はさんで)(?:横になって|寝て|休んで)?(?:ください|ね)?[。！!\\s]*");}
 static boolean active(int motion){return motion>=12&&motion<=16;}
 static String reply(int action){if(action==8)return RestLegRelaxIntent.REPLY;return new String[]{"抱き枕を体に載せて、仰向けにくつろぎます。","両手を頭の後ろに置いて、気楽に休みます。","仰向けのまま頬に手を添えて、ゆっくりします。","悪夢でうなされる寝顔と、小さな身じろぎに切り替えます。"}[action-4];}
}
