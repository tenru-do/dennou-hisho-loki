package com.example.rokidkeyboardbridge;
import org.json.*;import java.nio.file.*;
/** Contract checks only: never substitutes for actual provider comprehension tests. */
public final class LocalConversationRegressionTest {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 public static void main(String[] args)throws Exception{
  JSONArray tests=new JSONArray(Files.readString(Path.of(args[0])));for(int i=0;i<tests.length();i++){JSONObject t=tests.getJSONObject(i);if(ConversationEntryPolicy.discussion(t.getString("input"))!=t.getBoolean("discussion"))throw new AssertionError(t.getString("id"));checks++;}
  ok(ConversationEntryPolicy.localRead("今 何時？"));ok(ConversationEntryPolicy.localRead("バッテリーを教えて"));ok(!ConversationEntryPolicy.localRead("バッテリーの劣化って何？"));
  long now=10000000;JSONArray corrections=ConversationCorrections.update(new JSONArray(),ConversationEntryPolicy.correction("駅名は架空中央だよ"),now);ok(corrections.length()==1);ok(ConversationCorrections.block(corrections,now).contains("架空中央"));ok(ConversationCorrections.block(corrections,now+7200000).isEmpty());ok(ConversationEntryPolicy.correction("駅名は何ですか？").equals("駅名は何ですか？"));
  JSONArray history=new JSONArray().put(new JSONObject().put("user","時給1000円で月160時間働くと？").put("time",now-900000));JSONObject preview=ConversationReferentPreview.build(history,"年間では？",now);ok(preview.getString("state").equals("CANDIDATE"));ok(ConversationReferentPreview.label(preview).contains("未確定"));history.put(new JSONObject().put("user","別の展示会の予定も考えたい").put("time",now-1000));ok(ConversationReferentPreview.build(history,"この前の件",now).getString("state").equals("AMBIGUOUS"));ok(ConversationReferentPreview.build(history,"新しい話",now).getString("state").equals("NONE"));ok(ConversationReferentPreview.build(history,"それ",now+7200001).getString("state").equals("NONE"));
  ok(KeepTopicImagePolicy.reason(now,now-15001,0,100,100000000,false,false).equals("STALE"));ok(KeepTopicImagePolicy.reason(now,now-15000,0,100,100000000,false,false).isEmpty());ok(KeepTopicImagePolicy.reason(now,now-1000,now-10000,100,100000000,false,false).equals("RATE"));ok(!KeepTopicImagePolicy.canReuse(now,now+1,0,100,100000000,false,false));ok(!KeepTopicImagePolicy.canReuse(now,now-1000,0,100,100000000,false,true));
  int scenarioChecks=checks;NativeSpeechSession speech=new NativeSpeechSession();for(int i=0;i<18000;i++){long at=now+i*100;ok(!speech.takeStop(at));if(i%300==0){speech.sent(at,4000);ok(speech.takeStop(at+2000));ok(!speech.takeStop(at+2001));}}
  System.out.println("PASS "+scenarioChecks+" local scenario assertions + "+(checks-scenarioChecks)+" simulated media-state transitions; external requests=0; actual model quality NOT tested");
 }
}
