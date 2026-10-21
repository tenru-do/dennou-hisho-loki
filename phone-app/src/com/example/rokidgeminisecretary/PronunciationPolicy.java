package com.example.rokidgeminisecretary;
import java.io.*;import java.util.*;import java.security.*;import java.nio.charset.StandardCharsets;
/** Literal, longest-first, single-pass speech substitutions. No regular expressions from user input. */
public final class PronunciationPolicy {
 public static LinkedHashMap<String,String> parse(String raw)throws IOException{
  if(raw==null||raw.length()>8000)throw new IOException("辞書は8000文字以内です");LinkedHashMap<String,String> out=new LinkedHashMap<>();
  for(String line:raw.split("\n",-1)){if(line.trim().isEmpty())continue;int split=line.indexOf('=');if(split<1||split!=line.lastIndexOf('='))throw new IOException("1行に単語=よみを入力してください");String key=line.substring(0,split).trim(),value=line.substring(split+1).trim();
   if(key.isEmpty()||value.isEmpty()||key.length()>60||value.length()>120||control(key)||control(value)||out.containsKey(key))throw new IOException("空欄・重複・制御文字・長すぎる項目があります");out.put(key,value);if(out.size()>50)throw new IOException("辞書は50件までです");}return out;
 }
 private static boolean control(String value){for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))return true;return false;}
 public static String replace(String text,Map<String,String> dictionary){if(text==null||text.isEmpty()||dictionary.isEmpty())return text;ArrayList<String> keys=new ArrayList<>(dictionary.keySet());keys.sort((a,b)->Integer.compare(b.length(),a.length()));StringBuilder out=new StringBuilder();for(int i=0;i<text.length();){String match=null;for(String key:keys)if(text.startsWith(key,i)){match=key;break;}if(match==null)out.append(text.charAt(i++));else{out.append(dictionary.get(match));i+=match.length();}if(out.length()>60000)return text;}return out.toString();}
 public static String revision(String text)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
 private PronunciationPolicy(){}
}
