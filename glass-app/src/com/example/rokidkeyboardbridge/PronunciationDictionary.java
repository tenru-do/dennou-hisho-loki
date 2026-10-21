package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.widget.*;import android.util.AtomicFile;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Literal, longest-first, single-pass substitutions only at speech transport. */
public final class PronunciationDictionary {
 public static LinkedHashMap<String,String> parse(String raw)throws IOException{
  if(raw==null||raw.length()>8000)throw new IOException("辞書は8000文字以内です");
  LinkedHashMap<String,String> out=new LinkedHashMap<>();
  for(String line:raw.split("\n",-1)){if(line.trim().isEmpty())continue;int split=line.indexOf('=');
   if(split<1||split!=line.lastIndexOf('='))throw new IOException("1行に単語=よみを入力してください");
   String key=line.substring(0,split).trim(),value=line.substring(split+1).trim();
   if(key.isEmpty()||value.isEmpty()||key.length()>60||value.length()>120||key.matches(".*[\\p{Cntrl}].*")||value.matches(".*[\\p{Cntrl}].*")||out.containsKey(key))throw new IOException("空欄・重複・制御文字・長すぎる項目があります");
   out.put(key,value);if(out.size()>50)throw new IOException("辞書は50件までです");
  }return out;
 }
 public static String replace(String text,Map<String,String> dictionary){
  if(text==null||text.isEmpty()||dictionary.isEmpty())return text;
  ArrayList<String> keys=new ArrayList<>(dictionary.keySet());keys.sort((a,b)->Integer.compare(b.length(),a.length()));StringBuilder out=new StringBuilder();
  for(int i=0;i<text.length();){String match=null;for(String key:keys)if(text.startsWith(key,i)){match=key;break;}if(match==null)out.append(text.charAt(i++));else{out.append(dictionary.get(match));i+=match.length();}if(out.length()>60000)return text;}
  return out.toString();
 }
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"pronunciation-v1.gcm"));}
 private static String read(Context c)throws Exception{try{if(file(c).getBaseFile().length()>20000)throw new IOException("辞書の容量を確認してください");return new String(DeviceSecrets.photos().decrypt(file(c).readFully()),StandardCharsets.UTF_8);}catch(FileNotFoundException empty){return "";}}
 static String spoken(Context c,String original){try{return replace(original,parse(read(c)));}catch(Exception unavailable){return original;}}
 static void show(Activity owner){try{String original=read(owner);EditText input=new EditText(owner);input.setText(original);input.setHint("Rokid=ロキッド\n電脳秘書=でんのうひしょ");input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8000)});
  AlertDialog dialog=new AlertDialog.Builder(owner).setTitle("読み上げ辞書（このメガネのみ）").setMessage("1行に 単語=よみ。表示・会話履歴は変えません。50件まで。空欄で辞書を解除します。").setView(input).setPositiveButton("保存",null).setNegativeButton("戻る",null).create();dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{try{String raw=input.getText().toString();parse(raw);if(!read(owner).equals(original))throw new IOException("辞書が更新されました。開き直してください");AtomicFile f=file(owner);FileOutputStream out=null;try{out=f.startWrite();out.write(DeviceSecrets.photos().encrypt(raw.getBytes(StandardCharsets.UTF_8)));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}dialog.dismiss();}catch(Exception e){input.setError(e.getMessage());}}));dialog.show();
 }catch(Exception e){new AlertDialog.Builder(owner).setMessage("辞書を開けません。保存内容は保持しています").setPositiveButton("閉じる",null).show();}}
 private PronunciationDictionary(){}
}
