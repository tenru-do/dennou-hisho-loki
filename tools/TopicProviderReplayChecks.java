package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;import java.io.*;
public final class TopicProviderReplayChecks {
 static int n;static void ok(boolean v,String why){n++;if(!v)throw new AssertionError(why);}
 static JSONObject read(Context c,String name)throws Exception{try(InputStream in=new FileInputStream(new File(c.getFilesDir(),name))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int size;while((size=in.read(b))!=-1)out.write(b,0,size);return new JSONObject(out.toString("UTF-8"));}}
 public static int run(Context c)throws Exception{
  boolean[] expected={true,false,true};
  for(int i=0;i<3;i++){JSONObject value=read(c,"topic-provider-replay-"+i+".json");String message=ColumnSpeechFormatter.format(value.getString("message"),true);JSONArray claims=value.getJSONArray("coveredPoints");boolean schema=!message.isEmpty()&&message.length()<=2000&&!value.getString("topicKey").isEmpty()&&claims.length()>=1&&claims.length()<=6;boolean accepted=schema&&LokiPersonaPolicy.accepts(message)&&TopicResponseLength.accepts(message,i==0);ok(accepted==expected[i],"actual provider replay acceptance "+i);}
  ok(TopicResponseLength.accepts(new String(new char[385]).replace('\0','文'),false),"385-character column is not discarded");ok(!TopicResponseLength.accepts(new String(new char[349]).replace('\0','文'),false),"short greeting still rejected");ok(TopicResponseLength.accepts(new String(new char[750]).replace('\0','文'),false),"delivery upper bound");ok(!TopicResponseLength.accepts(new String(new char[751]).replace('\0','文'),false),"overlong column bounded");ok(TopicResponseLength.accepts(new String(new char[80]).replace('\0','文'),true),"sensor response minimum preserved");ok(!TopicResponseLength.accepts(new String(new char[79]).replace('\0','文'),true),"sensor minimum bounded");return n;
 }
}
