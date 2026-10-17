package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;
/** Shared by smartphone, browser and glasses; only recent successful/in-flight media sets context. */
final class MediaGenerationContext {
 static final long TTL=2*60*60*1000L;
 /** Read atomic record files without taking the image/video worker locks in opposite order. */
 static JSONObject latest(Context c){JSONObject best=new JSONObject();for(String kind:new String[]{"IMAGE","VIDEO"}){java.io.File[] files=(kind.equals("IMAGE")?ImageGeneration.root(c):VideoGeneration.root(c)).listFiles();if(files==null)continue;for(java.io.File f:files){if(!f.getName().matches("[a-f0-9-]{36}\\.json\\.gcm"))continue;try{String id=f.getName().substring(0,36);JSONObject j=kind.equals("IMAGE")?ImageGeneration.read(c,id):VideoGeneration.read(c,id);String state=j.optString("state");if(!state.equals("completed")&&!state.equals("queued")&&!state.equals("running")&&!state.equals("submitting"))continue;if(System.currentTimeMillis()-j.optLong("time")>TTL)continue;if(j.optLong("time")<=best.optLong("time"))continue;best=new JSONObject().put("kind",kind).put("id",j.getString("id")).put("time",j.getLong("time")).put("prompt",j.optString("prompt")).put("self",j.optInt("selfReferenceCount")>0||j.optBoolean("selfSubject")||LokiSelfProfile.refers(j.optString("prompt")));}catch(Exception ignored){}}}return best;}
 static JSONObject state(Context c)throws Exception{JSONObject context=latest(c);return new JSONObject().put("kind",context.optString("kind","NONE")).put("time",context.optLong("time"));}
 static MediaGenerationIntent.Kind kind(Context c,String s,boolean attachment){JSONObject last=attachment?new JSONObject():latest(c);MediaGenerationIntent.Kind previous=attachment?MediaGenerationIntent.Kind.IMAGE:MediaGenerationIntent.Kind.valueOf(last.optString("kind","NONE"));return MediaGenerationIntent.infer(s,previous,attachment||last.has("id"));}
 static boolean video(Context c,String s){return kind(c,s,false)==MediaGenerationIntent.Kind.VIDEO;}
 static boolean video(Context c,String s,boolean attachment){return kind(c,s,attachment)==MediaGenerationIntent.Kind.VIDEO;}
 static boolean image(Context c,String s){return kind(c,s,false)==MediaGenerationIntent.Kind.IMAGE;}
 static JSONObject continuation(Context c,String prompt)throws Exception{JSONObject last=latest(c);if(!last.has("id"))return new JSONObject();MediaGenerationIntent.Kind explicit=MediaGenerationIntent.explicit(prompt);if(explicit!=MediaGenerationIntent.Kind.NONE&&!MediaGenerationIntent.continuation(prompt))return new JSONObject();if(MediaGenerationIntent.infer(prompt,MediaGenerationIntent.Kind.valueOf(last.getString("kind")),true)==MediaGenerationIntent.Kind.NONE)return new JSONObject();return last;}
 static String prompt(JSONObject last,String current){String prior=last.optString("prompt");if(prior.length()>500)prior=prior.substring(0,500);return "前の制作と同じ人物・画風を基本にしてください。今回指定された衣装や動作の変更を優先してください。前の制作指示（参考）: "+prior+"\n今回の指示: "+current;}
 private MediaGenerationContext(){}
}
