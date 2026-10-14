package com.example.rokidkeyboardbridge;
public final class QuerySearchFollowupTest {public static void main(String[] args){int n=0;String first="今週末のイベントを調べて";
 for(String q:new String[]{"地元と秋葉原、上野方面にしぼって","無料のものに限定","ほかにもある？","会場は？","公式サイトも"}){if(!QueryPolicy.search(q,first,90000))throw new AssertionError(q);n++;}
 for(String q:new String[]{"ありがとう","座って","メモを削除して","今日の気分は？"}){if(QueryPolicy.search(q,first,90000))throw new AssertionError(q);n++;}
 if(QueryPolicy.search("上野方面にしぼって",first,900001)||QueryPolicy.search("上野方面にしぼって",first,-1)||QueryPolicy.search("無料のものに限定","座って",100))throw new AssertionError("stale or unrelated context");
 System.out.println("PASS "+(n+3)+" search follow-up and stale/unrelated exclusions");}}
