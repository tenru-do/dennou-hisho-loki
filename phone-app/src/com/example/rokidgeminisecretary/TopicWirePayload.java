package com.example.rokidgeminisecretary;
import android.content.Context;
import android.util.Log;
import org.json.*;

/** /morning consumers use script/title; never transmit duplicate full article/history arrays. */
final class TopicWirePayload {
    static String newsText(JSONArray top,JSONArray local, boolean repeated) {
        StringBuilder out=new StringBuilder(repeated?"主なニュースの最新取得分を再掲します。":"主なニュースです。");
        appendArticles(out,top,2);
        if(top==null || top.length()==0)appendArticles(out,local,2);
        if((top==null||top.length()==0)&&(local==null||local.length()==0))
            return repeated?"新しいニュースはありません。既にお伝えした見出しは省きます。":"ニュースは更新を再試行中です。取得でき次第お届けします。";
        return TopicTextPolicy.limit(out.toString(),TopicTextPolicy.NEWS_CHARS);
    }
    private static void appendArticles(StringBuilder out,JSONArray articles,int max) {
        if(articles==null)return;
        java.util.Set<String> seen=new java.util.HashSet<>(); int emitted=0;
        for(int i=0;i<articles.length()&&emitted<max;i++) {
            JSONObject a=articles.optJSONObject(i);if(a==null)continue;
            String title=a.optString("title");if(title.trim().isEmpty())continue; if(!seen.add(NewsSpeechText.key(NewsSpeechText.headline(title))))continue; emitted++;
            out.append("\n\n").append(TopicTextPolicy.limit(title,90)).append("。");
        }
    }
    static JSONObject prepare(Context context,JSONObject source) throws Exception {
        if(!source.optBoolean("ok"))return source;
        JSONObject cache=MorningBriefingManager.newsCache(context);
        JSONArray top=source.optJSONArray("topNews"),local=source.optJSONArray("localNews");
        boolean repeated=source.optJSONArray("skippedTopNews")!=null && source.optJSONArray("skippedTopNews").length()>0;
        if(top==null) {
            top=cache.optJSONArray("topNews");
            repeated=source.optJSONArray("skippedTopNews")!=null && source.optJSONArray("skippedTopNews").length()>0;
        }
        if(local==null)local=cache.optJSONArray("localNews");
        if((top==null||top.length()==0)&&(local==null||local.length()==0)
                && MorningBriefingManager.initialNewsPending(context))
            return new JSONObject().put("ok",false).put("status","preparing");
        String news=newsText(top,local,repeated);
        JSONArray events=source.optJSONArray("localEvents");
        if(events==null||events.length()==0)events=cache.optJSONArray("upcomingEvents");
        StringBuilder nearby=new StringBuilder();
        if(events!=null&&events.length()>0) {
            nearby.append("今後30日以内の周辺イベントです。");
            for(int i=0;i<events.length()&&i<2;i++) {
                JSONObject event=events.optJSONObject(i);if(event==null)continue;
                nearby.append("\n").append(event.optString("eventDateLabel")).append("、").append(event.optString("title")).append("。");
            }
        } else {
            JSONArray pois=NearbyTopicPois.cached(context);
            if(pois.length()>0){
                nearby.append("開催日を確認できるイベント情報はありませんが、10キロ圏の立ち寄り候補は、");
                for(int i=0;i<pois.length()&&i<3;i++)nearby.append(pois.getJSONObject(i).optString("title")).append("、");
                nearby.append("です。営業時間は施設の公式案内をご確認ください。");
                if(pois.getJSONObject(0).optString("source").equals("Google Maps"))nearby.append("施設情報はGoogle Mapsによります。");
            }else nearby.append("イベント検索を再試行中です。周辺の立ち寄り先は、現在地を更新して公園や商業施設で検索できます。");
        }
        String original=source.optString("script");
        int heading=original.indexOf("ここからは主なニュース");
        String personal=heading>=0?original.substring(0,heading):source.optBoolean("fallback")?original:"";
        String script=TopicTextPolicy.limit(news+"\n"+TopicTextPolicy.limit(nearby.toString(),300)+"\n"+TopicTextPolicy.limit(personal,700),TopicTextPolicy.SCRIPT_CHARS);
        JSONObject wire=new JSONObject().put("ok",true).put("status","ready")
                .put("title","ロキ・トピック").put("date",source.optString("date"))
                .put("fallback",source.optBoolean("fallback")).put("generatedAt",source.optLong("generatedAt"))
                .put("script",script).put("summary",TopicTextPolicy.limit(news,160))
                .put("newsChars",news.length()).put("characterCount",script.length());
        while(TopicTextPolicy.bytes(wire.toString())>TopicTextPolicy.WIRE_BYTES && script.length()>1) {
            script=TopicTextPolicy.limit(script,Math.max(1,script.length()-100));
            wire.put("script",script).put("characterCount",script.length());
        }
        Log.i("LokiTopicData","TOPIC/wire newsChars="+news.length()+" scriptChars="+script.length()
                +" payloadBytes="+TopicTextPolicy.bytes(wire.toString())+" limitBytes="+TopicTextPolicy.WIRE_BYTES);
        return wire;
    }
}
