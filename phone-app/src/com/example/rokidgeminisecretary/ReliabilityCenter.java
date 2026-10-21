package com.example.rokidgeminisecretary;
import org.json.*;import java.util.*;import java.io.*;
final class ReliabilityCenter {
 static JSONObject get(MainActivity owner,String route,Map<String,String> q)throws Exception{if(route.startsWith("maintenance-"))return MaintenanceCenter.get(owner,route,q);switch(route){
 case "storage":return new JSONObject().put("items",VerifiedStorage.list(owner));
 case "storage-check":return q.getOrDefault("type","").equals("photo")?VerifiedStorage.photo(owner,q.getOrDefault("id","")):MeetingAudioStore.verify(owner,q.getOrDefault("id",""));
 case "notes":return new JSONObject().put("items",PersonalNotes.list(owner)).put("trash",PersonalNotes.trash(owner));
 case "note-detail":return PersonalNotes.detail(owner,q.getOrDefault("id",""));
 case "inbox":return SecretaryInbox.list(owner).put("trash",SecretaryInbox.trash(owner));
 case "inbox-detail":return SecretaryInbox.get(owner,q.getOrDefault("id","")).getJSONObject("item");
 case "jobs":return new JSONObject().put("items",CancellableWork.list(owner));
 case "meeting-preview":return MeetingConsent.preview(owner,q.getOrDefault("id",""));
 case "timeline":return new JSONObject().put("items",TranscriptTimeline.read(owner,q.getOrDefault("id","")));
 case "memory":return new JSONObject().put("items",MemoryProvenance.list(owner)).put("state",new DynamicMemorySync(owner).state());
 case "notices":return NotificationReview.list(owner);
 case "profile-preview":return ProfilePresets.preview(owner,q.getOrDefault("mode",""));
 case "day":return DailyReview.read(owner,q.getOrDefault("day",java.time.LocalDate.now().toString()));
 default:throw new IOException("画面を確認してください");}}
 static JSONObject post(MainActivity owner,String route,JSONObject input)throws Exception{if(route.startsWith("maintenance-"))return MaintenanceCenter.post(owner,route,input);switch(route){
 case "send-preview":return SubmissionReview.preview(owner,input);
 case "note-action":return PersonalNotes.change(owner,input);
 case "inbox-action":return SecretaryInbox.action(owner,input);
 case "storage-repair":return VerifiedStorage.queuePhotoRepair(owner,input.getString("id"));
 case "meeting-analyze":MeetingConsent.consume(owner,input.getString("id"),input.getString("token"));MeetingAudioStore.analyze(owner,input.getString("id"));return new JSONObject().put("queued",true);
 case "meeting-repair":return MeetingControl.queue(owner,new JSONObject().put("action","transfer").put("requestId",java.util.UUID.randomUUID().toString()));
 case "job-stop":return CancellableWork.cancel(owner,input.getString("kind"),input.getString("id"));
 case "memory-source":return MemoryProvenance.save(owner,input);
 case "memory-edit":MemoryProvenance.edit(owner,input);return new JSONObject().put("queued",true);
 case "notice-read":NotificationReview.mark(owner,input.getString("id"),input.getInt("revision"));break;
 case "notices-toggle":if(!owner.getSharedPreferences("notification_review",0).edit().putBoolean("enabled",input.getBoolean("enabled")).commit())throw new IOException("設定を保存できません");break;
 case "notices-clear":ReviewStore.write(owner,"notices",new JSONObject().put("items",new JSONArray()));break;
 case "profile-apply":ProfilePresets.apply(owner,input.getString("mode"),input.getString("revision"));break;
 case "profile-undo":DeviceTuning.restore(owner);return new JSONObject().put("message",SettingsJournal.undo(owner));
 case "day-prepare":return DailyReview.prepare(owner,input);
 default:throw new IOException("操作を確認してください");}return new JSONObject().put("ok",true);}
 private ReliabilityCenter(){}
}
