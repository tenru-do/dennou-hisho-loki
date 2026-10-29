package com.example.rokidkeyboardbridge;
import java.text.Normalizer;
/** Shared interpretation for the avatar director and its conversation context. */
final class RestPoseIntent {
 static final int NONE=0,RELAX=1,LIE=2,REPAIR_LIE=3,PROHIBIT_LIE=4;
 static String normalize(String text){String t=text==null?"":Normalizer.normalize(text,Normalizer.Form.NFKC).trim();return t.replace("ねそべ","寝そべ").replace("ねころが","寝転が").replace("ねころん","寝ころん").replace("寛いで","くつろいで");}
 static int kind(String text){String t=normalize(text);if(t.matches("(?s).*(とは|意味|方法|について|彼女|彼が|友人|登場人物|例文|小説|引用|私が|私は|私も|僕が|僕は|自分が|写真|画像|動画|説明|教えて|検索|調べ|探して|天気|ニュース|予定|メール|会議|と言った|と言って).*"))return NONE;
 if(t.matches("(?s).*(寝そべ|寝転が|寝転|寝ころ|横にな|横たわ).*(ないで|なくていい|ないように|ほしくない|するな).*"))return PROHIBIT_LIE;
 if(t.matches("(?s).*(寝そべ(?:ら|て|って|れ)|寝転が(?:ら|って)|寝転んで|寝ころんで|横にな(?:ら|って)|横たわ(?:ら|って))ない(?:な|ね|よ|じゃ|んだけど|のだけど|です|ですよ|ぞ|んだ).*"))return REPAIR_LIE;
 if(t.matches("(?s).*(寝そべって|寝転がって|寝転んで|寝ころんで|横になって|横たわって|仰向けになって|うつ伏せになって).*"))return LIE;
 if(t.contains("くつろいで")||t.contains("リラックスして")){if(t.contains("ないで"))return NONE;return RELAX;}return NONE;}
 static String localReply(String text,boolean quiet){if(text==null||text.length()>80||ConversationEntryPolicy.discussion(text))return "";int k=kind(text);if(k==NONE)return "";if(quiet)return "ナビや録音の表示中です。終了してから姿勢を指定してください。";if(k==REPAIR_LIE)return "失礼しました。寝姿への切り替えをやり直します。";if(k==LIE)return "寝姿に切り替えます。";if(k==PROHIBIT_LIE)return "寝姿をやめて、座る姿勢にします。";return "くつろぐ姿勢に切り替えます。";}
 static String systemRule(String text){int k=kind(text);if(k==NONE)return "";if(k==PROHIBIT_LIE)return "今回の発話は画面内アバターを寝そべらせないという明示的な禁止である。寝姿への切り替えを取り消す意図に短く応じる。";String base="今回の発話はロキの画面内アバターの姿勢を選ぶ操作であり、挨拶やユーザーへの気遣いだけで済ませない。外部検索や予定操作は不要。表示用の寝姿とくつろぐ姿勢があり、肉体がないという断りは不要。未確認の実表示が完了したとは断定せず、今回の表示切り替えへ一文程度で直接応じる。";if(k==REPAIR_LIE)return base+" アプリ側の判定はDISPLAY_REPAIR、要求姿勢はLYINGである。『ねそべらないな』は表示が指示通りでないという指摘であり、寝そべらないでという禁止ではない。過去履歴の椅子に座っているという文章は現在の実画面の根拠ではない。椅子に座ったままと説明して終わらず、寝姿への切り替えをやり直す意図に応じる。例えば『失礼しました。寝姿への切り替えをやり直します。』。";return base+(k==LIE?" 要求姿勢はLYING。寝姿に切り替える意図へ応じる。":" 要求はRELAX。現在の姿勢に合わせてくつろぐ表示へ切り替える意図へ応じる。");}
 static boolean wantsLie(int kind){return kind==LIE||kind==REPAIR_LIE;}
}
