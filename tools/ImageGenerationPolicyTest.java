package com.example.rokidgeminisecretary;
public final class ImageGenerationPolicyTest {
 static void check(boolean condition,String test){if(!condition)throw new AssertionError(test);}
 public static void main(String[] args){
  for(String s:new String[]{"猫のイラストを描いて","猫の画像を生成して","この写真をイラストにして","青い空の絵を作って"})check(ImageGenerationPolicy.requests(s,false),s);
  for(String s:new String[]{"画像生成できますか？","猫は描ける？","画像を生成する方法を教えて","猫を描かないで","ロキに画像生成機能を実装して","__LOKI_GENERATED__:abc","この画像を説明して","この画像に何が写ってる？","眠気がくる"})check(!ImageGenerationPolicy.requests(s,true),s);
  for(String s:new String[]{"背景を青く","背景を夕焼けにして","もう少しリアルに","この写真の背景を変えて"}){check(ImageGenerationPolicy.requests(s,true),s);check(!ImageGenerationPolicy.requests(s,false),"no implicit source "+s);}
  System.out.println("PASS new generation, image edit continuation, attached-image edits, capability/analysis/negative/internal-command exclusions");
 }
}
