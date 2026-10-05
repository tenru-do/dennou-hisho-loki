package com.example.rokidkeyboardbridge;
public class QueryPolicyTest {
    public static void main(String[] args) {
        if (!QueryPolicy.clock("今何時？")) throw new AssertionError("clock");
        for (String q : new String[]{"パウパウアクアガーデン新宿店の営業時間は？", "店は何時まで？", "明日の会議は何時？", "病院まで何時間かかる？"}) {
            if (QueryPolicy.clock(q)) throw new AssertionError("wrong clock: " + q);
        }
        if (!QueryPolicy.search("今日のブレイキングダウンの結果は？")) throw new AssertionError("live result");
        if (!QueryPolicy.search("パウパウアクアガーデン新宿店の営業時間は？")) throw new AssertionError("shop");
        if (QueryPolicy.search("おはよう")) throw new AssertionError("unnecessary search");
        if (QueryPolicy.search("明日の会議は何時から？")) throw new AssertionError("private schedule search");
        System.out.println("query regression checks passed");
    }
}
