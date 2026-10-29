package com.example.rokidkeyboardbridge;
import java.util.*;
/** Bounded per-feature circuit. Transport polling and acknowledgements always bypass it. */
final class FeatureResilience {
 static final class Entry{int failures;long until;String reason="";}
 private static final Map<String,Entry> states=new LinkedHashMap<>();
 static String family(String path){String p=path.split("[?]",2)[0];if(p.matches("media_.*|/web/api/media-(seek|sessions)"))return "media";if(p.matches(".*(navigation|route_candidates|calendar_navigation).*"))return "navigation";if(p.matches(".*(photo_follow|photo_import|transfer_start).*"))return "transfer";return "";}
 static synchronized void check(String path,long now)throws java.io.IOException{String f=family(path);Entry e=states.get(f);if(e!=null&&now<e.until)throw new java.io.IOException(f+"：復旧待ちです。"+Math.max(1,(e.until-now+999)/1000)+"秒後に再操作できます");}
 static synchronized void success(String path){states.remove(family(path));}
 static synchronized void failed(String path,Throwable failure,long now){String f=family(path);if(f.isEmpty())return;Entry e=states.get(f);if(e==null){e=new Entry();states.put(f,e);}e.reason=failure.getClass().getSimpleName();e.failures=Math.min(100,e.failures+1);if(e.failures>=3)e.until=now+15000;}
 static synchronized String summary(long now){StringBuilder b=new StringBuilder();for(Map.Entry<String,Entry>x:states.entrySet())b.append(x.getKey()).append(": ").append(x.getValue().failures).append("回失敗 / ").append(now<x.getValue().until?"復旧待ち":"再操作可能").append("\n");return b.length()==0?"機能別の連続通信失敗は記録されていません":b.toString();}
 static synchronized void reset(){states.clear();}
 private FeatureResilience(){}
}
