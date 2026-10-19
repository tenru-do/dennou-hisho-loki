package com.example.rokidkeyboardbridge;
import java.util.*;
/** Final delivery gate only. No model calls, recording controls or persistent transcript. */
final class ProactiveDeliveryPolicy {
 static final long WINDOW_MS=90000L;
 private final LinkedHashMap<String,Long> delivered=new LinkedHashMap<>();
 static String blocked(boolean enabled,boolean foreground,boolean navigation,boolean paused,boolean voice,boolean conversation,boolean speaking,boolean pending,boolean draft){
  if(!enabled)return "OFF";if(!foreground)return "BACKGROUND";if(navigation)return "NAVIGATION";if(paused)return "USER_PAUSE";if(voice)return "VOICE";if(conversation)return "CONVERSATION";if(speaking)return "SPEAKING";if(pending)return "PENDING";if(draft)return "DRAFT";return "";
 }
 static String normalized(String text){
  if(text==null)return "";String s=text.replaceAll("(?s)\\[\\[.*?\\]\\]", "").replaceAll("(?m)^【AMB[^\\n]*】\\s*", "").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
  s=s.replaceFirst("^(?:なるほど|そうですね|マスター)+", "");
  if(s.length()>30)s=s.replaceFirst("(?:面白いですね|いいですね|興味深いですね)$", "");return s;
 }
 private void expire(long now){java.util.Iterator<Map.Entry<String,Long>> it=delivered.entrySet().iterator();while(it.hasNext()){long at=it.next().getValue();if(now<at||now-at>=WINDOW_MS)it.remove();}}
 private static boolean warning(String text){return text.matches("(?s).*(危険|警報|避難|救急|緊急|火災|事故|注意喚起).*");}
 synchronized boolean duplicate(String text,long now){expire(now);String key=normalized(text);return !key.isEmpty()&&!warning(key)&&delivered.containsKey(key);}
 synchronized void remember(String text,long now){expire(now);String key=normalized(text);if(key.isEmpty()||key.length()>2400)return;delivered.remove(key);delivered.put(key,now);while(delivered.size()>8)delivered.remove(delivered.keySet().iterator().next());}
 ProactiveDeliveryPolicy(){}
}
