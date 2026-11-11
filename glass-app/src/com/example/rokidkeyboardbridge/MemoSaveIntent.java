package com.example.rokidkeyboardbridge;
import org.json.*;import java.util.regex.*;
/** Explicit local memo commands only. Reference instructions are never executed. */
final class MemoSaveIntent {
 static final Pattern VERB=Pattern.compile("覚えておいて|覚えといて|覚えとけ|覚えていて|覚えてね|覚えてください|記憶して(?:おいて|ください)?|記録して(?:おいて|ください)?|メモ(?:として|に)?(?:残して|保存して|して|しといて|しとけ)(?:おいて|ください|ね)?|メモを残して(?:おいて|ください)?");
 static String clean(String s){return s.trim().replaceFirst("^[\\s、，。:：「『\"]+","").replaceFirst("[」』\"]+$","").trim();}
 static String content(String text){if(!"save_memory".equals(MemoryRequestPolicy.requiredFunction(text)))return "";String s=text.replaceFirst("^(?:保留案件|保留|案件)(?:というか|じゃなく(?:て)?|ではなく(?:て)?)[、，\\s]*","");if(s.matches("^メモ[：:].+"))return clean(s.substring(3));Matcher m=VERB.matcher(s);if(!m.find())return "";String after=clean(s.substring(m.end())),before=clean(s.substring(0,m.start()).replaceFirst("^(?:ロキ[、，\\s]*)?(?:ちょっと|少し|お願い[、，\\s]*)*", ""));String value=!after.isEmpty()?after:before;value=value.replaceFirst("(?:を|は)$","").trim();if(value.length()<4||value.matches("(?:これ|それ|この内容|その内容|さっきの|先ほどの|メモ|保留案件|この件|その件)(?:を|は)?"))return "";return value.length()<=2400?value:"";}
 static String resolve(String text,JSONObject cache,JSONArray users,long now)throws Exception{
  String value=content(text);if(!value.isEmpty())return value;
  long at=cache==null?0:cache.optLong("at");if(at>0&&now>=at&&now-at<=900000&&!cache.optString("content").isEmpty())return cache.getString("content");
  if(users!=null)for(int i=0;i<users.length();i++){JSONObject row=users.getJSONObject(i);long time=row.optLong("time");if(time<=0||now<time||now-time>7200000)continue;String request=row.optString("message");value=content(request);if(!value.isEmpty())return value;if("save_memory".equals(MemoryRequestPolicy.requiredFunction(request))||request.matches("[はいそうお願い了解。、\\s]+"))continue;break;}
  return "";
 }
 static String topic(String content){String title=content.split("なんだけど|。|\\n",2)[0].trim();return title.substring(0,Math.min(60,title.length()));}
}
