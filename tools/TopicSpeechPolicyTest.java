package com.example.rokidgeminisecretary;
public final class TopicSpeechPolicyTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check="+checks);}
    public static void main(String[] args){
        check(!TopicSpeechPolicy.accepts(null));check(!TopicSpeechPolicy.accepts(""));
        check(!TopicSpeechPolicy.accepts("単文だけです。"));
        check(TopicSpeechPolicy.accepts("背景を説明します。補足をお伝えします。"));
        check(TopicSpeechPolicy.accepts("背景です。理由です。補足です。"));
        check(!TopicSpeechPolicy.accepts("背景です。理由です。補足です。続きです。"));
        check(TopicSpeechPolicy.accepts("『本当ですか？』と聞きました。説明します。"));
        check(!TopicSpeechPolicy.accepts("本当ですか！？"));
        check(TopicSpeechPolicy.accepts("一文です。\n\n二文です。"));
        System.out.println("TopicSpeechPolicy PASS checks="+checks);
    }
}
