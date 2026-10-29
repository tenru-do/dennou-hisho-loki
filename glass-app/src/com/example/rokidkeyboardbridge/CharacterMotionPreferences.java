package com.example.rokidkeyboardbridge;
import android.content.Context;
final class CharacterMotionPreferences {
 static void load(Context c){CharacterMotionTuning.naturalPoses=c.getSharedPreferences("character_motion",0).getInt("natural_poses",1)>0;CharacterMotionTuning.set(c.getSharedPreferences("character_motion",0).getInt("level",1));CharacterMotionTuning.intimacy(c.getSharedPreferences("character_motion",0).getInt("intimacy",1));android.content.SharedPreferences p=c.getSharedPreferences("character_motion",0);CharacterChoreography.talkLevel=p.getInt("scene_talk",2);CharacterChoreography.navLevel=p.getInt("scene_nav",0);CharacterChoreography.readLevel=p.getInt("scene_read",1);CharacterMotionTuning.parts(p.getInt("eyes",2),p.getInt("face",2),p.getInt("body",2),p.getInt("approach",2));}
 static boolean put(Context c,String key,int value){try{SettingsJournal.change(c,"character_motion",new org.json.JSONObject().put(key,value));load(c);return true;}catch(Exception e){return false;}}
 static boolean save(Context c,int value){return put(c,"level",Math.max(0,Math.min(2,value)));}
 static boolean saveIntimacy(Context c,int value){return put(c,"intimacy",Math.max(0,Math.min(3,value)));}
 static boolean savePart(Context c,String key,int value){return key.matches("eyes|face|body|approach")&&put(c,key,Math.max(0,Math.min(2,value)));}
 static boolean saveScene(Context c,int scene,int value){return scene>=0&&scene<3&&put(c,new String[]{"scene_talk","scene_nav","scene_read"}[scene],Math.max(0,Math.min(2,value)));}
 private CharacterMotionPreferences(){}
}
