package com.example.rokidkeyboardbridge;
public final class NavigationExperiencePolicy {
 public static boolean urgentTurn(double speed,String distance){double m=NavigationTurnCue.metres(distance);return m>=0&&m<=NavigationRefinements.lead(speed,3);}
 public static boolean wait(boolean urgent,boolean speaking,boolean answering,long remaining){return speaking||!urgent&&(answering||remaining>0);}
 public static String detail(String text,int level,String following,boolean urgent){String base=NavigationConversation.concise(text);if(urgent)return base;if(level==0){int i=base.indexOf('。');return i>=0?base.substring(0,i+1):base;}if(level==2&&following!=null&&!following.trim().isEmpty())return base+" 続いて、"+NavigationConversation.concise(following);return base;}
 public static String replacement(String text,boolean pending){if(!pending||text==null||text.length()>160||ConversationEntryPolicy.discussion(text))return "";String s=text.trim().replaceFirst("[。?!？！]+$","");java.util.regex.Matcher m=java.util.regex.Pattern.compile("^.{1,60}(?:じゃなくて|ではなく|でなく)(.{1,80})$").matcher(s);if(!m.matches())return "";return m.group(1).replaceFirst("(にして|へ変更して|に変更して|をお願い)$","").trim();}
 public static boolean cancelStopover(String text,boolean pending){return pending&&text!=null&&text.replaceAll("[\\s　。?!？！]","").matches("(コンビニ|立ち寄り|寄り道)(は|を)?(やめる|やめて|取り消す|取り消して)");}
 public static boolean alternative(String text){return text!=null&&!ConversationEntryPolicy.discussion(text)&&text.replaceAll("[\\s　。?!？！]","").matches("この道(は|を)?通れない|この道は通れません|別の道にしたい|別の経路にしたい|別の道を探して|別ルートを確認して");}
 public static String trouble(String code){return "LINK".equals(code)?"スマホからナビ情報を受信できません。接続とスマホのロキを確認してください。":"LOCATION".equals(code)?"位置が更新されていません。スマホの位置情報とGPSを確認してください。":"ROUTE".equals(code)?"スマホとの受信はできていますが、経路取得に失敗しています。通信と地図API設定を確認してください。":"ROUTE_MISSING".equals(code)?"経路が未取得です。スマホで目的地と地図API設定を確認してください。":"UPDATING".equals(code)?"経路を更新中です。新しい案内をお待ちください。":"";}
 private NavigationExperiencePolicy(){}
}
