package com.example.rokidgeminisecretary;
import android.app.*;
import android.content.*;
import android.widget.*;
import org.json.*;

final class ReadAnswerSettings {
 static JSONObject state(Context c)throws JSONException{return new JSONObject().put("quizFirst",c.getSharedPreferences("read_answer",0).getBoolean("quizFirst",true)).put("instruction",c.getSharedPreferences("read_answer",0).getString("instruction",""));}
 static void show(Activity a){
  LinearLayout box=new LinearLayout(a);box.setOrientation(1);box.setPadding(24,16,24,16);
  CheckBox quiz=new CheckBox(a);quiz.setText("クイズ・問題を最優先にして、回答のみを伝える");quiz.setChecked(a.getSharedPreferences("read_answer",0).getBoolean("quizFirst",true));box.addView(quiz);
  EditText text=new EditText(a);text.setHint("追加の読み取り方針");text.setMinLines(3);text.setText(a.getSharedPreferences("read_answer",0).getString("instruction",""));box.addView(text);
  TextView help=new TextView(a);help.setText("次のグラスREADから反映します。不鮮明な問題は推測せず、読み取れないことを伝えます。");box.addView(help);
  new AlertDialog.Builder(a).setTitle("READ：読み取り・回答方針").setView(box).setNegativeButton("戻る",null).setPositiveButton("保存",(d,w)->{String value=text.getText().toString().trim();if(value.length()>2000){Toast.makeText(a,"方針は2000文字以内にしてください",0).show();return;}boolean saved=a.getSharedPreferences("read_answer",0).edit().putBoolean("quizFirst",quiz.isChecked()).putString("instruction",value).commit();Toast.makeText(a,saved?"READの方針を保存しました":"保存できませんでした",0).show();}).show();
 }
}
