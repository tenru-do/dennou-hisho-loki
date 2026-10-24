package com.example.rokidgeminisecretary;
import android.content.Context;import android.util.Base64;import org.json.*;import java.io.IOException;
/** Explicit snapshots of a selected photo receipt; never stores extra photos or enqueues. */
final class PhotoFollowUp {
 static JSONObject prepare(JSONObject record,String question)throws Exception{
  if(record==null||!"image-question".equals(record.optString("route")))throw new IOException("写真付きの質問を選んでください");
  JSONObject source=record.getJSONObject("payload");String image=source.optString("image");
  if(!source.optString("parent").isEmpty()||!source.optString("referenceKey").isEmpty()||image.isEmpty()||image.length()>800000)throw new IOException("この控えの写真は使えません。写真を選び直してください");
  int count=source.optInt("imageCount",1);JSONArray kinds=ImageAttachmentKinds.normalize(source.optJSONArray("kinds"),count);
  for(int i=0;i<kinds.length();i++)if("video".equals(kinds.getString(i)))throw new IOException("動画の控えです。写真付きの質問を選んでください");
  byte[] jpeg;try{jpeg=Base64.decode(image,Base64.NO_WRAP);}catch(IllegalArgumentException e){throw new IOException("写真を読み取れません");}
  ImageQuestions.validateInput("photo-followup-preview",question,jpeg);
  return new JSONObject().put("message",question).put("image",image).put("imageCount",count).put("kinds",kinds);
 }
 static JSONObject read(Context c,String id,String question)throws Exception{return prepare(ConversationRecoveryStore.read(c,id),question);}
 static JSONArray list(Context c)throws Exception{
  JSONArray result=new JSONArray(),rows=ConversationRecoveryStore.list(c);
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!"image-question".equals(row.optString("route"))||row.optInt("imageCount")==0)continue;
   try{JSONObject item=ConversationRecoveryStore.read(c,row.getString("id"));prepare(item,"");result.put(row);}catch(Exception unavailable){/* Expired, invalid or non-photo records cannot be selected. */}
  }return result;
 }
 private PhotoFollowUp(){}
}
