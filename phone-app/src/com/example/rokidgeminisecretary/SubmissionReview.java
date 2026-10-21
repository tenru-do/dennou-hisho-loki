package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;
/** Local estimate only; no provider calls or enqueue. */
final class SubmissionReview {
 static JSONObject preview(Context c,JSONObject input)throws Exception{String text=input.optString("message");boolean attachment=input.optBoolean("hasImage"),parent=!input.optString("parent").isEmpty();MediaGenerationIntent.Kind kind=parent?MediaGenerationContext.kind(c,text,true):MediaGenerationContext.video(c,text)?MediaGenerationIntent.Kind.VIDEO:ImageGeneration.handles(c,text)?MediaGenerationIntent.Kind.IMAGE:MediaGenerationIntent.Kind.NONE;boolean review=attachment||parent||kind==MediaGenerationIntent.Kind.IMAGE||kind==MediaGenerationIntent.Kind.VIDEO;return new JSONObject().put("requiresReview",review).put("textChars",text.length()).put("imageBytes",Math.max(0,input.optLong("imageBytes"))).put("imageCount",Math.max(0,Math.min(6,input.optInt("imageCount")))).put("kind",kind.toString()).put("message","Geminiへの送信内容を確認してください。入力本文 "+text.length()+"文字、今回の添付 "+Math.max(0,input.optInt("imageCount"))+"件・画像本体約"+Math.max(0,input.optLong("imageBytes"))+"バイト。編集元を指定した場合や継続する生成では、選択済み・直近の生成物、ロキ本人の依頼では登録キャラクター画像が参照に加わる場合があります。これは入力分の概算で、内部の指示文・参照画像・符号化後の送信総量ではありません。動画添付は抜粋画像だけで音声は含みません。生成には料金が発生する可能性があり、中止しても送信済み分や課金は取り消せません。");}
 private SubmissionReview(){}
}
