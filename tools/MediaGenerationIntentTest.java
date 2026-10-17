package com.example.rokidgeminisecretary;
public final class MediaGenerationIntentTest{
 static void expect(String s,MediaGenerationIntent.Kind previous,boolean recent,MediaGenerationIntent.Kind expected){MediaGenerationIntent.Kind got=MediaGenerationIntent.infer(s,previous,recent);if(got!=expected)throw new AssertionError(s+" expected="+expected+" actual="+got);}
 public static void main(String[]args){MediaGenerationIntent.Kind I=MediaGenerationIntent.Kind.IMAGE,V=MediaGenerationIntent.Kind.VIDEO,N=MediaGenerationIntent.Kind.NONE;
 expect("セクシーにおどって",V,true,V);expect("では水着でダンスして",V,true,V);expect("水着姿を描いて",V,true,I);expect("水着にして",I,true,I);expect("水着にして",V,true,V);expect("この画像を動かして",I,true,V);expect("手を振って",I,true,V);expect("背景を青くして",I,true,I);expect("背景を青くして",V,true,V);expect("猫の絵を描いて",V,true,I);expect("猫の動画を作って",I,true,V);
 for(String s:new String[]{"ダンスの予定を調べて","水着はどこで売ってるか教えて","眠気がくる","病院の予約を変更して","水着イベントの予定を登録して","衣装の写真をメールで送信して","動画作れる？","踊ってと言ったらどうなる？","もう作らないで","この動画を解析して","__LOKI_VIDEO__:abc"})expect(s,V,true,N);
 for(String s:new String[]{"ロキがおどってみて","ロキが踊ってみて","ロキ、ダンスして","ロキに手を振ってほしい","ロキ自身が笑顔で踊ってください"}){expect(s,N,false,V);expect(s,V,false,V);if(!MediaGenerationIntent.selfMotion(s))throw new AssertionError("self identity "+s);}
 for(String s:new String[]{"ロキが踊っている","ロキが踊っているのはなぜ","ロキが踊ってみてと言ったら","ロキが踊ってみてとメールで送信して","ロキが踊ってみてとカレンダーに登録して","ロキが踊れる？","ロキが踊らないで","ロキの動画を再生して"})expect(s,N,false,N);
 expect("セクシーにおどって",N,false,N);expect("水着にして",V,false,N);System.out.println("PASS exact reported dance/swimsuit follow-ups, image/video distinction, image-to-motion, ordinary conversation, stale context, negative and capability exclusions");}
}
