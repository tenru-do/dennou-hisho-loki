package com.example.rokidgeminisecretary;
import android.app.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.function.BiConsumer;
/** Compact reference availability, never a claim that every turn uses this image. */
final class ImageReferencePanel {
 private final Activity host;private final BiConsumer<String,String> compose;private final LinearLayout root;private final TextView label;private JSONObject current=new JSONObject();
 ImageReferencePanel(Activity host,BiConsumer<String,String> compose){this.host=host;this.compose=compose;root=new LinearLayout(host);label=new TextView(host);label.setTextSize(12);label.setMaxLines(2);root.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button inspect=new Button(host);inspect.setText("参照を見る");inspect.setOnClickListener(v->show());root.addView(inspect);refresh();}
 View view(){return root;}
 void refresh(){current=ImageReferenceStore.view(host);root.setVisibility(current.optBoolean("available")?View.VISIBLE:View.GONE);if(current.optBoolean("available"))label.setText("参照可能な直近の添付："+current.optInt("imageCount")+"件\n"+android.text.format.DateFormat.format("MM/dd HH:mm",current.optLong("at"))+" 読み取り・最大2時間");}
 private void show(){refresh();if(!current.optBoolean("available"))return;final JSONObject selected=current;LinearLayout body=new LinearLayout(host);body.setOrientation(1);body.setPadding(20,12,20,12);add(body,"直近の読み取り結果です。現在の映像ではなく、すべての発言に使われるわけではありません。予定登録済みを意味しません。\n質問："+selected.optString("question"));
  JSONArray parts=selected.optJSONArray("sections");if(parts!=null)for(int i=0;i<parts.length();i++){JSONObject part=parts.optJSONObject(i);int index=part.optInt("index");String kind="video".equals(part.optString("kind"))?"動画（8場面・音声なし）":"image".equals(part.optString("kind"))?"画像":"種類未記録";add(body,"添付"+index+"・"+kind+"\n"+(part.optBoolean("separated")?part.optString("reading"):"個別の読み取り区分を確認できません。全文を確認してください。"));Button follow=new Button(host);follow.setText("添付"+index+"（"+kind+"）について質問・訂正");follow.setOnClickListener(v->{if(!ImageReferenceSummary.sameReference(selected,ImageReferenceStore.view(host))){new AlertDialog.Builder(host).setTitle("参照が更新・失効しています").setMessage("この一覧を閉じ、最新の参照を開き直してください。入力欄は変更していません。").setPositiveButton("閉じる",null).show();return;}compose.accept("この画像の【画像"+index+"】（添付"+index+"・"+kind+"）について、",selected.optString("key"));});body.addView(follow);}
  Button full=new Button(host);full.setText("読み取り結果の全文");full.setOnClickListener(v->new AlertDialog.Builder(host).setTitle("読み取り結果（原文）").setMessage(selected.optString("answer")).setPositiveButton("閉じる",null).show());body.addView(full);
  ScrollView scroll=new ScrollView(host);scroll.addView(body);new AlertDialog.Builder(host).setTitle("参照可能な直近の添付").setView(scroll).setPositiveButton("閉じる",null).show();
 }
 private void add(LinearLayout body,String text){TextView v=new TextView(host);v.setText(text);v.setTextIsSelectable(true);body.addView(v);}
}
