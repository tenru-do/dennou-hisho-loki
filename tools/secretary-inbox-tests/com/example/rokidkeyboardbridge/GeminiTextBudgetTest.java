package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
public final class GeminiTextBudgetTest {
 public static void main(String[] args)throws Exception {
  for(int old:new int[]{1100,1500,2200,4096}){
   JSONObject config=new JSONObject().put("maxOutputTokens",old).put("temperature",0.4);
   GeminiTextBudget.apply(config,"gemini-2.5-flash",false);
   check(config.getInt("maxOutputTokens")==Math.max(3072,old));
   check(config.getJSONObject("thinkingConfig").getInt("thinkingBudget")==512);
   check(config.getDouble("temperature")==0.4);
  }
  for(String model:new String[]{"gemini-2.5-pro","gemini-3-flash-preview","",null}){
   JSONObject c=new JSONObject().put("maxOutputTokens",1100);String before=c.toString();
   GeminiTextBudget.apply(c,model,false);check(before.equals(c.toString()));
  }
  JSONObject image=new JSONObject().put("maxOutputTokens",8192);String before=image.toString();
  GeminiTextBudget.apply(image,"gemini-2.5-flash",true);check(before.equals(image.toString()));
  System.out.println("PASS 17 text response budget / model isolation / image preservation checks");
 }
 static void check(boolean value){if(!value)throw new AssertionError();}
}
