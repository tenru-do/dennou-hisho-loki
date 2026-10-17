package com.example.rokidgeminisecretary;
public final class ImageGenerationCharacterTest{
 public static void main(String[] args){
 for(String s:new String[]{"ロキ自身の画像を生成して","電脳秘書ロキを描いて","ロキの立ち絵を描いて","あなた自身の画像を描いて","自画像を描いて"})if(!ImageGenerationCharacter.context(s).contains("外見の正本は登録画像"))throw new AssertionError(s);
 for(String s:new String[]{"北欧神話のロキを描いて","猫を描いて","背景を青く", ""})if(!ImageGenerationCharacter.context(s).isEmpty())throw new AssertionError(s);
 System.out.println("PASS app character and self-portrait resolved; mythology, unrelated image and edit instructions unchanged");
 }
}
