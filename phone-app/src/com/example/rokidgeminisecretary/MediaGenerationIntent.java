package com.example.rokidgeminisecretary;
/** Infer only visual imperatives in recent creative context; never bill ordinary questions. */
final class MediaGenerationIntent {
 enum Kind { NONE, IMAGE, VIDEO }
 static boolean excluded(String s){return s==null||s.trim().isEmpty()||s.startsWith("__")||s.matches("(?s).*(しないで|作らないで|描かないで|動かさないで|やめて|止めて|生成でき|作れる|作れます|できますか|できる[？?]|可能ですか|方法を|作り方|機能を|実装して|インストール|削除|解析して|分析して|読み取って|何が映|何が写|調べて|検索して|教えて|説明して|と言ったら|って言ったら).*");}
 static boolean motion(String s){return s!=null&&s.matches("(?s).*(?:踊って|おどって|ダンスして|ダンスしてください|ダンスをして|踊らせて|踊らせて|歩いて|歩かせて|走って|走らせて|手を振って|手を振らせて|振り向いて|振り向かせて|回転して|回って|動かして|動いて|動きを.*(?:増やして|つけて)|アニメーションにして).*");}
 static boolean change(String s){return s!=null&&s.matches("(?s).*(?:背景|服装|衣装|水着|髪型|髪色|表情|笑顔|目|口|色|画風|リアル|明る|暗く|ポーズ|その姿|同じ人物|同じ服).*(?:にして|に変えて|変えて|変える|して|くして|増やして|もっと|もう少し|描いて|作って).*" );}
 static boolean selfMotion(String s){
  if(excluded(s)||s.matches("(?s).*(?:予定|カレンダー|予約|メール|LINE|ナビ|経路|通知|録音|TOPIC|KEEP|Bluetooth|登録して|保存して|送信して|再生して).*"))return false;
  String v=s.replaceAll("[\\s　、，。！!？?]", "");
  return v.matches("(?:電脳秘書)?ロキ(?:自身)?(?:が|を|に|も)?(?:ちょっと|少し|楽しく|笑顔で|軽く|もう一度|もう一回)*(?:踊って|おどって|ダンスして|ダンスをして|踊らせて|歩いて|歩かせて|手を振って|振り向いて)(?:みて|ください|ほしい|欲しい|くれる|くれ|よ|ね)*");
 }
 static Kind explicit(String s){if(excluded(s))return Kind.NONE;if(selfMotion(s)||VideoGenerationPolicy.requests(s))return Kind.VIDEO;if(ImageGenerationPolicy.requests(s,false)||s.matches("(?s).*(?:画像|静止画|イラスト|絵).*(?:にして|作って|作成して|生成して|描いて).*"))return Kind.IMAGE;return Kind.NONE;}
 static Kind infer(String s,Kind previous,boolean recent){if(excluded(s))return Kind.NONE;Kind named=explicit(s);if(named!=Kind.NONE)return named;if(s.matches("(?s).*(予定|カレンダー|予約|メール|LINE|ナビ|経路|通知|録音|TOPIC|KEEP|Bluetooth|登録して|保存して|送信して|再生して).*"))return Kind.NONE;if(!recent||previous==Kind.NONE)return Kind.NONE;if(motion(s))return Kind.VIDEO;if(change(s)||ImageGenerationPolicy.requests(s,true))return previous;return Kind.NONE;}
 static boolean continuation(String s){return s!=null&&s.matches("(?s).*(?:この|その|さっき|直前|今の|同じ|もっと|もう少し|では|じゃあ|それで|動画にして|画像にして|静止画にして).*");}
 private MediaGenerationIntent(){}
}
