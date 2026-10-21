package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
final class BookmarkNotes {
 static synchronized JSONObject read(Context c,String meeting)throws Exception{MeetingAudioStore.folder(c,meeting);try{return ConversationWorkspace.read(c,"bookmark-notes-"+meeting);}catch(FileNotFoundException absent){return new JSONObject();}}
 static synchronized JSONObject save(Context c,JSONObject input)throws Exception{String meeting=input.getString("meeting"),id=input.getString("bookmark"),text=input.getString("text").trim();if(text.length()>800)throw new IOException("しおりメモは800文字までです");boolean exists=false;JSONArray marks=BookmarkStore.read(MeetingAudioStore.folder(c,meeting));for(int i=0;i<marks.length();i++)if(marks.getJSONObject(i).getString("id").equals(id))exists=true;if(!exists)throw new IOException("しおりが見つかりません");JSONObject rows=read(c,meeting),old=rows.optJSONObject(id);int revision=old==null?0:old.optInt("revision");if(revision!=input.getInt("revision"))throw new IOException("メモが更新されています。開き直してください");JSONObject value=new JSONObject().put("text",text).put("revision",revision+1).put("at",System.currentTimeMillis());rows.put(id,value);ConversationWorkspace.write(c,"bookmark-notes-"+meeting,rows);return value;}
 private BookmarkNotes(){}
}
