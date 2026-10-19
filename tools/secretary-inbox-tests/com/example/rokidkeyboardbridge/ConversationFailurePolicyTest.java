package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
public final class ConversationFailurePolicyTest {
 static int count;
 static void ok(boolean b){if(!b)throw new AssertionError("check "+count);count++;}
 static String code(Throwable e){return ConversationFailurePolicy.code(e,0,"",false);}
 public static void main(String[] args)throws Exception{
  String secret="PRIVATE_QUERY https://secret.invalid/?key=SECRET";
  ok(code(new java.net.SocketTimeoutException(secret)).equals("TIMEOUT"));
  ok(code(new Exception(secret,new java.net.SocketTimeoutException(secret))).equals("TIMEOUT"));
  ok(code(new java.net.UnknownHostException(secret)).equals("CONNECTION"));
  ok(code(new Exception(secret,new java.net.ConnectException(secret))).equals("CONNECTION"));
  ok(code(new InterruptedException(secret)).equals("INTERRUPTED"));
  ok(code(new java.util.concurrent.CancellationException(secret)).equals("INTERRUPTED"));
  ok(code(new Exception(secret)).equals("PROCESSING"));ok(code(null).equals("PROCESSING"));
  ok(ConversationFailurePolicy.code(new java.net.SocketTimeoutException(secret),0,"",true).equals("SAFETY_STOP"));
  ok(code(new Exception(secret,new LocalConversationStop("REFERENCE_CHANGED"))).equals("REFERENCE_CHANGED"));
  int[] status={429,401,403,402,404,500,503,599,400,418};
  String[] expected={"RATE_LIMIT","AUTHORIZATION","AUTHORIZATION","BILLING","MODEL_UNAVAILABLE","SERVICE_UNAVAILABLE","SERVICE_UNAVAILABLE","SERVICE_UNAVAILABLE","API_REJECTED","API_REJECTED"};
  for(int i=0;i<status.length;i++)ok(ConversationFailurePolicy.code(null,status[i],secret,false).equals(expected[i]));
  ok(ConversationFailurePolicy.code(null,0,"MAX_TOKENS",false).equals("OUTPUT_LIMIT"));
  ok(ConversationFailurePolicy.code(null,0,secret,false).equals("NO_ANSWER"));
  for(String c:new String[]{"REFERENCE_CHANGED","SAFETY_STOP","TIMEOUT","CONNECTION","INTERRUPTED","RATE_LIMIT","AUTHORIZATION","BILLING","MODEL_UNAVAILABLE","SERVICE_UNAVAILABLE","API_REJECTED","OUTPUT_LIMIT","NO_ANSWER","PROCESSING"}){
   JSONObject record=ConversationFailurePolicy.record(c,ConversationProgressPolicy.REVIEW,1234567);
   ok(record.length()==3&&record.getString("stage").equals("REVIEW"));
   String summary=ConversationFailurePolicy.summary(record.toString());
   ok(!summary.isEmpty()&&!summary.contains("SECRET")&&!summary.contains("PRIVATE_QUERY")&&!summary.contains("https://"));
   ok(!ConversationFailurePolicy.title(c).contains(secret)&&!ConversationFailurePolicy.detail(c).contains(secret));
  }
  ok(ConversationFailurePolicy.record("TIMEOUT",secret,1).getString("stage").equals("PROCESS"));
  ok(ConversationFailurePolicy.summary("broken").isEmpty());
  ok(ConversationFailurePolicy.summary("{\"code\":\"SECRET\",\"at\":1}").isEmpty());
  ok(ConversationFailurePolicy.summary("{\"code\":\"TIMEOUT\",\"at\":0}").isEmpty());
  ok(!ConversationFailurePolicy.summary("{\"code\":\"TIMEOUT\",\"at\":1,\"stage\":\"SECRET\"}").contains("SECRET"));
  boolean bad=false;try{ConversationFailurePolicy.record(secret,secret,1);}catch(IllegalArgumentException e){bad=true;}ok(bad);
  Throwable cycle=new Exception(secret){public synchronized Throwable getCause(){return this;}};ok(code(cycle).equals("PROCESSING"));
  Throwable a=new Exception(secret),b=new Exception(secret,a);a.initCause(b);ok(code(a).equals("PROCESSING"));
  ok(ConversationProgressPolicy.failure(secret,new Exception(secret),false).contains("会話の処理"));
  ok(!ConversationProgressPolicy.failure(secret,new Exception(secret),false).contains("SECRET"));
  ok(ConversationProgressPolicy.failure(ConversationProgressPolicy.ANSWER,new Exception(),false,"RATE_LIMIT").contains("利用制限"));
  System.out.println("PASS "+count+" failure classification / privacy / bounded causes checks");
 }
}
