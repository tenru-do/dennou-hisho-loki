package com.example.rokidkeyboardbridge;

public final class ConversationFollowupPolicyTest {
    public static void main(String[] args) {
        String[] followups = {"年間では？", "手取りだと？", "前の質問忘れた？？？？", "週３日なら？", "月２２日にしたら？", "それなら", "じゃあ一年でいくら？", "何倍？", "さっきの条件で計算して"};
        for (String input : followups) check(ConversationFollowupPolicy.referencesPrevious(input), input);
        String[] newTopics = {"今週末のイベントを調べて", "別の話で、年収の平均を検索して", "ところで、時給を調べて", "眠気がくる", "今日の天気", "", "会話をリセット"};
        for (String input : newTopics) check(!ConversationFollowupPolicy.referencesPrevious(input), input);
        long now=10_000_000L;
        check(ConversationFollowupPolicy.fresh(now-14*60_000L, now), "14 minute pause");
        check(!ConversationFollowupPolicy.fresh(now-121*60_000L, now), "expired history");
        check(!ConversationFollowupPolicy.fresh(now+1, now), "future timestamp");
        check(!ConversationFollowupPolicy.fresh(0, now), "no history");
        check(ConversationFollowupPolicy.needsContextRepair("前の質問忘れた？", "忘れてはおりません。何か別のことでしたでしょうか。", true), "defensive non-answer");
        check(ConversationFollowupPolicy.needsContextRepair("年間では？", "何についての年間ですか。", true), "lost antecedent");
        check(!ConversationFollowupPolicy.needsContextRepair("手取りだと？", "扶養人数を教えてください。", true), "specific missing tax condition");
        check(!ConversationFollowupPolicy.needsContextRepair("年間では？", "月額の12倍で7,873,920円です。", true), "direct answer");
        check(!ConversationFollowupPolicy.needsContextRepair("年間では？", "何についての年間ですか。", false), "genuinely unavailable history");
        System.out.println("PASS 25 follow-up / new-topic / elapsed-time / repair checks");
    }
    private static void check(boolean ok,String name) {if(!ok)throw new AssertionError(name);}
}
