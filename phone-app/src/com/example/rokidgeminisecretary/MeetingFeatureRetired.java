package com.example.rokidgeminisecretary;
/** Recording is retired. Existing encrypted archives remain for backup compatibility only. */
final class MeetingFeatureRetired {
 static final String MESSAGE="ロキの会議メモ機能は終了しました。公式アプリの会議メモをご利用ください。";
 static boolean removed(){return true;}
 static boolean blocksPath(String path){if(path==null)return false;int q=path.indexOf('?');if(q>=0)path=path.substring(0,q);String leaf=path.substring(path.lastIndexOf('/')+1);return leaf.equals("meeting")||leaf.equals("meetings")||leaf.startsWith("meeting-")||leaf.startsWith("meeting_")||leaf.startsWith("maintenance-task")||leaf.equals("maintenance-candidates")||leaf.equals("maintenance-meetings");}
 private MeetingFeatureRetired(){}
}
