package com.example.rokidkeyboardbridge;
public final class HudResumePolicyTest{
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){for(boolean active:new boolean[]{false,true})for(boolean paused:new boolean[]{false,true})for(boolean pending:new boolean[]{false,true})for(boolean topic:new boolean[]{false,true}){
  String label=HudResumePolicy.topicLabel(active,paused,pending,topic),description=HudResumePolicy.topicDescription(active,paused,pending,topic);ok(HudResumePolicy.preferAmbient(active,pending)==(active||pending),"AMB priority");
  ok(label.equals(active?(paused?"AMB休止":"AMB中"):pending?"AMB準備":topic?"STOP":"TOPIC"),"truthful state label");ok(!active||description.contains("停止"),"active button stops");ok(active||!pending||description.contains("取り消し"),"pending is not active");
 }
 for(boolean visible:new boolean[]{false,true})for(boolean interactive:new boolean[]{false,true})for(boolean head:new boolean[]{false,true})for(boolean editing:new boolean[]{false,true})for(boolean dialog:new boolean[]{false,true}){
  boolean actual=HudResumePolicy.resetHeadGlance(visible,interactive,head,editing,dialog);
  if(editing||dialog)ok(!actual,"editing/dialog keep current selection");
  else if(!visible)ok(actual,"hidden HUD begins new READ/AMB selection");
  else if(interactive&&head)ok(!actual,"repeated visible sensor does not reset selection");
  else ok(actual,"actual display wake resets selection");
 }
 ok(!HudResumePolicy.preferAmbient(false,false),"ordinary default is READ");System.out.println("PASS "+n+" READ/AMB priority and state label assertions");}
}
