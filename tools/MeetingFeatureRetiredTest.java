package com.example.rokidgeminisecretary;
import java.lang.reflect.*;
public final class MeetingFeatureRetiredTest {
 public static void main(String[]args)throws Exception{
  int checks=0;Class<?> glass=Class.forName("com.example.rokidkeyboardbridge.MeetingFeatureRetired");Method method=glass.getDeclaredMethod("blocksPath",String.class);method.setAccessible(true);
  for(String path:new String[]{"/meeting_state","/meeting_audio","/web/api/meeting-control","/web/api/meeting-analyze","/web/api/meetings","/web/api/meeting-import-chunk","/web/api/maintenance-tasks","/web/api/maintenance-task-save","/web/api/maintenance-candidates","/web/api/maintenance-meetings","/web/api/meeting-control?return=/photos"}){
   if(!MeetingFeatureRetired.blocksPath(path)||!(Boolean)method.invoke(null,path))throw new AssertionError("Retired route remains available: "+path);checks+=2;
  }
  for(String path:new String[]{"/health","/web/api/photos","/web/api/photo-import-status","/web/api/delete","/web/api/workspace","/web/api/secretary-inbox","/web/api/profile-preview?mode=meeting","/web/api/reading-history","/web/api/maintenance-schedule","/web/api/meetingless-photo"}){
   if(MeetingFeatureRetired.blocksPath(path)||(Boolean)method.invoke(null,path))throw new AssertionError("Unrelated route blocked: "+path);checks+=2;
  }
  if(!MeetingFeatureRetired.removed())throw new AssertionError("Meeting feature not retired");
  System.out.println("PASS "+(checks+1)+" meeting endpoint retirement and unaffected KEEP, voice-health, notes, navigation/profile routes");
 }
}
