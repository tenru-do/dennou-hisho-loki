package com.example.rokidgeminisecretary;
import org.json.*;
import java.util.regex.*;
/** Display existing readings only; missing sections are not inferred. */
final class ImageReferenceSummary {
 static final long TTL=2L*60*60*1000;
 static String referenceKey(JSONObject input)throws Exception{
  byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(checked(input).toString().getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
 }
 static boolean matchesKey(String key,JSONObject latest){try{return key!=null&&key.matches("[a-f0-9]{64}")&&latest!=null&&latest.optBoolean("available")&&key.equals(referenceKey(latest));}catch(Exception invalid){return false;}}
 static boolean sameReference(JSONObject selected,JSONObject latest){
  if(selected==null||latest==null||!selected.optBoolean("available")||!latest.optBoolean("available"))return false;
  return selected.optLong("at")>0&&selected.optLong("at")==latest.optLong("at")&&selected.optInt("imageCount")==latest.optInt("imageCount")&&selected.optString("question").equals(latest.optString("question"))&&selected.optString("answer").equals(latest.optString("answer"))&&String.valueOf(selected.optJSONArray("kinds")).equals(String.valueOf(latest.optJSONArray("kinds")));
 }
 static JSONObject checked(JSONObject input)throws Exception{
  long at=input.getLong("at");int count=input.getInt("imageCount");String question=input.getString("question"),answer=input.getString("answer");
  if(at<=0||count<1||count>6||question.length()>2400||answer.length()>24000||answer.trim().isEmpty())throw new IllegalArgumentException("Invalid image reference");
  JSONObject checked=new JSONObject().put("at",at).put("imageCount",count).put("kinds",ImageAttachmentKinds.normalize(input.optJSONArray("kinds"),count)).put("question",question).put("answer",answer);if(input.has("selectedAt"))checked.put("selectedAt",Math.max(at,input.optLong("selectedAt",at)));if(input.optLong("capturedAt")>0&&input.optLong("capturedAt")<=at)checked.put("capturedAt",input.optLong("capturedAt"));String thumbnail=input.optString("thumbnail");if(thumbnail.length()<=24000&&thumbnail.matches("[A-Za-z0-9+/=]*")&&!thumbnail.isEmpty())checked.put("thumbnail",thumbnail);return checked;
 }
 static JSONObject view(JSONObject input,long now)throws Exception{
  JSONObject data=checked(input);long at=data.getLong("at");if(now<at||now-at>=TTL)return new JSONObject().put("available",false);
  int count=data.getInt("imageCount");String answer=data.getString("answer");JSONArray sections=new JSONArray();
  Matcher m=Pattern.compile("【画像([1-6])】").matcher(answer);int previous=0,start=0;boolean ordered=true;java.util.Map<Integer,String> readings=new java.util.LinkedHashMap<>();
  while(m.find()){int index=Integer.parseInt(m.group(1));if(index<=previous||index>count)ordered=false;if(previous>0)readings.put(previous,answer.substring(start,m.start()).trim());previous=index;start=m.end();}
  if(previous>0)readings.put(previous,answer.substring(start).trim());
  for(int i=1;i<=count;i++){String reading=count==1?answer:ordered?readings.get(i):null;sections.put(new JSONObject().put("index",i).put("kind",data.getJSONArray("kinds").getString(i-1)).put("separated",reading!=null&&!reading.isEmpty()).put("reading",reading==null?"":reading));}
  return data.put("key",referenceKey(data)).put("available",true).put("sections",sections);
 }
 private ImageReferenceSummary(){}
}
