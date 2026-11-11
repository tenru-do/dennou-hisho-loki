package com.example.rokidgeminisecretary;
import android.content.Context;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
/** Expansion belongs to this card, including programmatic opening. Never recreates its input views. */
public final class PhoneFoldSection extends LinearLayout {
 private final Button heading;private final LinearLayout body;private final View content;private final String title,description;private boolean open;
 public PhoneFoldSection(Context c,String title,String description,View content){super(c);this.title=title;this.description=description;this.content=content;setOrientation(VERTICAL);setPadding(0,0,0,0);heading=new Button(c);heading.setAllCaps(false);heading.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);heading.setTextSize(15);heading.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);heading.setMinHeight(dp(76));heading.setPadding(dp(16),dp(12),dp(16),dp(12));addView(heading,new LayoutParams(-1,-2));body=new LinearLayout(c);body.setOrientation(VERTICAL);body.setPadding(dp(12),dp(6),dp(12),dp(12));PhoneWorkspaceLayout.detach(content);body.addView(content,new LayoutParams(-1,-2));addView(body,new LayoutParams(-1,-2));heading.setOnClickListener(v->setOpen(!open));setOpen(false);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable background(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(10));d.setStroke(dp(1),stroke);return d;}
 public void setOpen(boolean value){open=value;content.setVisibility(value?VISIBLE:GONE);body.setVisibility(value?VISIBLE:GONE);String state=value?"－ 開いています · タップで閉じる":"＋ 閉じています · タップで開く";android.text.SpannableString label=new android.text.SpannableString(title+"\n"+description+"\n"+state);label.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),0,title.length(),33);int sub=title.length()+1;label.setSpan(new android.text.style.AbsoluteSizeSpan(12,true),sub,label.length(),33);label.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(85,105,120)),sub,sub+description.length(),33);label.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),sub+description.length()+1,label.length(),33);heading.setText(label);heading.setTextColor(value?Color.rgb(14,88,88):Color.rgb(38,55,75));heading.setBackgroundTintList(null);heading.setBackground(background(value?Color.rgb(220,242,239):Color.WHITE,value?Color.rgb(64,148,140):Color.rgb(201,212,223)));body.setBackground(background(Color.rgb(241,249,247),Color.rgb(160,201,194)));heading.setContentDescription(title+"、"+(value?"展開中、閉じる":"折り畳み中、開く")+"。"+description);if(android.os.Build.VERSION.SDK_INT>=30)heading.setStateDescription(value?"開いています":"閉じています");}
 public boolean isOpen(){return open;}
 public Button heading(){return heading;}
 public static String description(String title){
  if(title.equals("接続・音声・読み上げ"))return "Bluetooth・30秒操作・読み上げ・静かな時間";
  if(title.startsWith("詳細設定"))return "API設定・グラス接続などの詳細な操作";
  if(title.equals("よく使うルート"))return "行き・帰り・登録した区間を選ぶ";
  if(title.equals("案内中の操作・状態"))return "一時停止・再開・経路の状態を確認";
  if(title.equals("通知・省電力・応答時間"))return "ナビの通知と位置更新を調整";
  if(title.equals("保存経路・出口・駐輪場所"))return "通信なしで使う経路・保存した地点";
  if(title.startsWith("その他のナビ"))return "地図・経路の追加操作";
  if(title.equals("メモ・回答・参照セット"))return "覚えた内容・保存した回答を探す";
  if(title.equals("写真・録音・生成した画像"))return "KEEP写真・録音・生成メディアを開く";
  if(title.equals("自動で話す（TOPIC）"))return "自動コメントのオン・オフ・間隔";
  if(title.equals("更新・バックアップ・動作確認"))return "更新情報・保存・復旧・端末の状態";
  return "この中の設定と操作を表示";
 }
}
