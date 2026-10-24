package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.widget.*;import android.util.AtomicFile;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Literal, longest-first, single-pass substitutions only at speech transport. */
public final class PronunciationDictionary {
 public static LinkedHashMap<String,String> parse(String raw)throws IOException{return PronunciationPolicy.parse(raw);}
 public static String replace(String text,Map<String,String> dictionary){return PronunciationPolicy.replace(text,dictionary);}
 private static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"pronunciation-v1.gcm"));}
 private static String read(Context c)throws Exception{try{if(file(c).getBaseFile().length()>20000)throw new IOException("辞書の容量を確認してください");return new String(DeviceSecrets.photos().decrypt(file(c).readFully()),StandardCharsets.UTF_8);}catch(FileNotFoundException empty){return "";}}
 public static org.json.JSONObject status(Context c){try{return snapshot(c);}catch(Exception unreadable){return new org.json.JSONObject();}}
 public static synchronized org.json.JSONObject snapshot(Context c)throws Exception{String text=read(c);parse(text);return new org.json.JSONObject().put("text",text).put("revision",PronunciationPolicy.revision(text));}
 public static synchronized void save(Context c,String raw,String revision)throws Exception{parse(raw);if(!PronunciationPolicy.revision(read(c)).equals(revision))throw new IOException("辞書が更新されました。開き直してください");AtomicFile f=file(c);FileOutputStream out=null;try{out=f.startWrite();out.write(DeviceSecrets.photos().encrypt(raw.getBytes(StandardCharsets.UTF_8)));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}}
 static String spoken(Context c,String original){try{return replace(original,parse(read(c)));}catch(Exception unavailable){return original;}}
 static void show(Activity owner){try{String original=read(owner);EditText input=new EditText(owner);input.setText(original);input.setHint("Rokid=ロキッド\n電脳秘書=でんのうひしょ");input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8000)});
  AlertDialog dialog=new LokiAlertDialogBuilder(owner).setTitle("読み上げ辞書（このメガネのみ）").setMessage("1行に 単語=よみ。表示・会話履歴は変えません。50件まで。空欄で辞書を解除します。").setView(input).setPositiveButton("保存",null).setNegativeButton("戻る",null).create();dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{try{String raw=input.getText().toString();save(owner,raw,PronunciationPolicy.revision(original));dialog.dismiss();}catch(Exception e){input.setError(e.getMessage());}}));dialog.show();
 }catch(Exception e){new LokiAlertDialogBuilder(owner).setMessage("辞書を開けません。保存内容は保持しています").setPositiveButton("閉じる",null).show();}}
 private PronunciationDictionary(){}
}
