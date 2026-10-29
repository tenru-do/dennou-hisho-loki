package com.example.rokidkeyboardbridge;
public class AvatarPostureContextTest {
 public static void main(String[] args){int n=0;for(String t:new String[]{"ロキ、寝そべってみて","寝転がってみて","横になって","仰向けになって"}){if(!AvatarPostureContext.pose(t).equals("LYING"))throw new AssertionError(t);EmbodiedScenePolicy p=new EmbodiedScenePolicy();p.user(t,1000);if(p.clip(1100,false)<28)throw new AssertionError("missing visual "+t);n++;}
 for(String t:new String[]{"彼女が寝そべっているとは？","寝そべる方法を教えて","今日は天気を教えて"}){if(!AvatarPostureContext.block(t).isEmpty())throw new AssertionError(t);n++;}
 for(String t:new String[]{"寝そべらないで","寝転がらないで"}){if(!AvatarPostureContext.pose(t).equals("CANCEL_LYING"))throw new AssertionError(t);n++;}
 for(String t:new String[]{"ロキ、座って","ロキ、立って"}){if(AvatarPostureContext.block(t).isEmpty())throw new AssertionError(t);n++;}
 for(String t:new String[]{"脚を組み替えて","足を組みかえて","脚を組んで"}){EmbodiedScenePolicy p=new EmbodiedScenePolicy();p.user(t,1000);if(p.posture()!=1||p.clip(1100,false)!=31||!AvatarPostureContext.pose(t).equals("SEATED_LEG_CHANGE"))throw new AssertionError(t);int a=EmbodiedScenePolicy.frame(31,0,p.selected(1100)),z=EmbodiedScenePolicy.frame(31,3300,p.selected(4300));if(a==z)throw new AssertionError("no visible change");p.answer("脚を組み替えますね。",FullBodyPoseCatalog.Kind.NEUTRAL,4500);if(p.posture()!=1||p.clip(4600,false)!=31)throw new AssertionError("reply cannot stand up");n++;}
 for(String t:new String[]{"脚を組み替えないで","彼女が脚を組み替えてと言った","足を組む方法を教えて"}){if(!AvatarPostureContext.block(t).isEmpty())throw new AssertionError(t);n++;}
 System.out.println("PASS "+n+" posture request/context/visual checks");}
}
