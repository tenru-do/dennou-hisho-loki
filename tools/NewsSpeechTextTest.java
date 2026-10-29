package com.example.rokidgeminisecretary;
public final class NewsSpeechTextTest {
 public static void main(String[] args){int n=0;
 String[][] cases={{"鉄道が開業 - 架空新聞","鉄道が開業 架空新聞","架空新聞",""},{"鉄道が開業","鉄道が開業。","",""},{"鉄道が開業","鉄道が開業。初日は無料です。","","初日は無料です。"},{"鉄道が開業","新駅は来月開業予定です。","","新駅は来月開業予定です。"},{"鉄道が開業 - 架空新聞","鉄道が開業 - 架空新聞","架空新聞",""},{"ＡＩの発表","AIの発表","",""},{"値上げ見送り","値上げ見送りか。検討中です。","","か。検討中です。"}};
 for(String[] c:cases){if(!NewsSpeechText.detail(c[0],c[1],c[2]).equals(c[3]))throw new AssertionError(java.util.Arrays.toString(c));n++;}
 if(!NewsSpeechText.sameHeadline("鉄道が開業 - 架空新聞","鉄道が開業 - 別新聞"))throw new AssertionError("publisher duplicate");n++;
 if(NewsSpeechText.sameHeadline("鉄道が開業","鉄道が開業中止"))throw new AssertionError("different facts retained");n++;
 if(!NewsSpeechText.update("列車の運転見合わせ","列車の運転再開"))throw new AssertionError("follow-up update");n++;
 if(NewsSpeechText.update("列車の運転再開","列車の運転再開"))throw new AssertionError("same is not update");n++;
 if(!NewsSpeechText.update("利用者10万人","利用者20万人"))throw new AssertionError("number update");n++;
 System.out.println("PASS "+n+" news speech deduplication checks");
 }
}
