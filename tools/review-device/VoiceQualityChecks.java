package com.example.rokidkeyboardbridge;
public final class VoiceQualityChecks {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("voice quality "+n);}
 public static int run(){n=0;
 ok(LokiPersonaPolicy.accepts("マスター、こちらで確認できます。\n\n情報元は\n公式アクセス\nhttps://example.invalid/access/"));
 ok(LokiPersonaPolicy.accepts("## 施設情報\n住所：埼玉県上尾市の合成住所\n公式サイト：https://example.invalid\n駅から徒歩で行けます。"));
 ok(LokiPersonaPolicy.accepts("次の通りです。\n- 施設名\n- 所在地\n確認日：2026年10月22日"));
 ok(LokiPersonaPolicy.accepts("記事には「便利だよ」とあります。確認が必要です。"));
 ok(LokiPersonaPolicy.accepts("かしこまりました。こんにちは、マスター。"));ok(LokiPersonaPolicy.accepts("マスター、こんばんは。"));ok(LokiPersonaPolicy.accepts("はい、マスター。確認します。"));
 ok(!LokiPersonaPolicy.accepts("いいよ。次はこうしよう。"));ok(!LokiPersonaPolicy.accepts("便利だよ。試してね。"));ok(!LokiPersonaPolicy.accepts(""));ok(!LokiPersonaPolicy.accepts(null));ok(!LokiPersonaPolicy.FALLBACK.contains("口調"));
 ok(QueryPolicy.search("ピュアブレッドはどこにある？"));ok(QueryPolicy.search("ピュアブレッドはどこにありますか"));ok(QueryPolicy.search("PUREBREDの所在地は？"));ok(QueryPolicy.search("上尾市のピュアブレッドへのアクセスを教えて"));ok(QueryPolicy.search("東京駅ってどこ？"));ok(!QueryPolicy.search("問題はどこにある？"));ok(!QueryPolicy.search("入力欄はどこにありますか？"));ok(!QueryPolicy.search("鍵をどこに置いたっけ？"));ok(QueryPolicy.search("今日の営業時間を調べて"));
 ok(AnswerDisplayExpiry.DELAY_MS==3000);ok(AnswerDisplayExpiry.clear(1,1,2,2,"same","same",false));ok(!AnswerDisplayExpiry.clear(1,2,2,2,"same","same",false));ok(!AnswerDisplayExpiry.clear(1,1,2,3,"same","same",false));ok(!AnswerDisplayExpiry.clear(1,1,2,2,"old","new",false));ok(!AnswerDisplayExpiry.clear(1,1,2,2,"same","same",true));ok(!AnswerDisplayExpiry.clear(1,1,2,2,"","",false));String reference="合成の過去会話：別の施設の所在地を質問しました。";
 ok(!ConversationContextReview.shouldReview("ピュアブレッドはどこにある？",reference));ok(!ConversationContextReview.shouldReview("ロキ、こんにちは。",reference));ok(!ConversationContextReview.shouldReview("おはようロキ",reference));ok(!ConversationContextReview.shouldReview("今日の天気を教えて",reference));ok(!ConversationContextReview.shouldReview("新しい話題です。それを調べて",reference));
 ok(ConversationContextReview.shouldReview("それはどこにある？",reference));ok(ConversationContextReview.shouldReview("年間では？",reference));ok(ConversationContextReview.shouldReview("元の話を引き継いで",reference));ok(ConversationContextReview.shouldReview("使い分けは？",reference));ok(!ConversationContextReview.shouldReview("それは？","[]"));
 ok(ConversationFirstTurnPolicy.staleFailure("failure","failure",false));ok(!ConversationFirstTurnPolicy.staleFailure("failure","new answer",false));ok(!ConversationFirstTurnPolicy.staleFailure("failure","failure",true));ok(!ConversationFirstTurnPolicy.staleFailure("","",false));
 ok(ConversationFailurePolicy.code(new ConversationContextReview.Unresolved(),0,"",false).equals("CONTEXT_MISMATCH"));ok(ConversationFailurePolicy.title("CONTEXT_MISMATCH").contains("参照先"));ok(ConversationFailurePolicy.validCode("CONTEXT_MISMATCH"));ok(!ConversationFailurePolicy.validCode("invented_failure"));return n;
 }
}
