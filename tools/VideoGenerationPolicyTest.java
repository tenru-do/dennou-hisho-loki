package com.example.rokidgeminisecretary;
public final class VideoGenerationPolicyTest{
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 public static void main(String[]args){for(String s:new String[]{"ロキが笑顔で手を振る動画を作って","ロキ自身の動画を生成して","この画像を動画にして","この画像を動かして","猫のビデオを作成して"})check(VideoGenerationPolicy.requests(s),s);for(String s:new String[]{"動画作れる？","動画を生成できますか","動画を作らないで","この動画を解析して","動画生成機能を実装して","__LOKI_VIDEO__:abc","動画の作り方を教えて","眠い","この画像を説明して"})check(!VideoGenerationPolicy.requests(s),s);check(VideoGenerationPolicy.usesImage("この画像を動画にして"),"source image");check(!VideoGenerationPolicy.usesImage("猫の動画を作って"),"no incidental source");check(VideoGenerationPolicy.aspect("横長で",true).equals("16:9"),"explicit aspect overrides self");System.out.println("PASS explicit video requests, capability/negative/analysis/internal exclusions, selected-image source and aspect");}
}
