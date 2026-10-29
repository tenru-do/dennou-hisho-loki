package com.example.rokidgeminisecretary;
import android.content.Context;import org.json.*;import java.lang.reflect.*;
public final class NewsSpeechDeviceChecks {
 public static int run(Context c,String apk)throws Exception{
  NewsRssFallback.Article a=NewsRssFallback.parse("<rss><channel><item><title>架空駅が開業 - テスト新聞</title><description>架空駅が開業 テスト新聞</description><source>テスト新聞</source></item></channel></rss>",2).get(0);
  Method bind=MorningBriefingManager.class.getDeclaredMethod("bindNewsArticle",NewsRssFallback.Article.class);bind.setAccessible(true);
  JSONObject first=(JSONObject)bind.invoke(null,a);if(!first.getString("description").isEmpty())throw new AssertionError("RSS fallback duplicate survives");
  JSONArray items=new JSONArray().put(first).put(new JSONObject().put("title","架空駅が開業 - 別新聞")).put(new JSONObject().put("title","公園を公開").put("description","公園を公開。明日から入場無料です。"));
  String text=TopicWirePayload.newsText(items,null,false);
  if(text.indexOf("架空駅が開業")!=text.lastIndexOf("架空駅が開業"))throw new AssertionError("headline spoken twice");
  if(!text.contains("公園を公開")||text.contains("明日から入場無料"))throw new AssertionError("headline missing or body leaked");
  if(text.indexOf("公園を公開")!=text.lastIndexOf("公園を公開"))throw new AssertionError("description repeats title");
  JSONArray fallback=TopicFallbackContent.freshNews(c,new JSONArray().put(new JSONObject().put("title","合成見出し"+System.nanoTime()).put("description","本文は読まない")));
  if(!fallback.getJSONObject(0).getString("description").isEmpty())throw new AssertionError("fallback body leaked");
  if(!CalendarLocationPolicy.explicit("架空県架空市1丁目2-3")||CalendarLocationPolicy.explicit("市民会館"))throw new AssertionError("calendar specificity");
  if(!TopicWirePayload.newsText(new JSONArray(),new JSONArray(),true).contains("新しいニュースはありません"))throw new AssertionError("deduped list must not trigger recap");
  ReconnectReview.last=-1;long now=System.currentTimeMillis();c.getSharedPreferences("loki_web_console",0).edit().putString("outbox",new JSONArray().put(new JSONObject().put("key","synthetic-pending").put("time",now)).toString()).commit();ReconnectReview.contact(c,1000000,new JSONArray().put(new JSONObject().put("id","synthetic-pending")));if(!ReconnectReview.held(c,"synthetic-pending"))throw new AssertionError("reconnect auto-dispatches unreviewed work");
  return 8;
 }
}
