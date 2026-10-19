package com.example.rokidgeminisecretary;
/** Explicit reference reselection only. No sending, persistence or inferred target. */
final class ReferenceDraftPolicy {
 static String retarget(String current,String oldPrefix,String nextPrefix,int limit){
  if(current==null)current="";if(oldPrefix==null)oldPrefix="";
  if(nextPrefix==null||nextPrefix.isEmpty())throw new IllegalArgumentException("参照を選び直してください");
  String next=!oldPrefix.isEmpty()&&current.startsWith(oldPrefix)?nextPrefix+current.substring(oldPrefix.length()):nextPrefix;
  if(limit<1||next.length()>limit)throw new IllegalArgumentException("質問が長すぎます。本文は残しています。短くしてから選び直してください");
  return next;
 }
 static boolean bound(String current,String prefix){return current!=null&&prefix!=null&&!prefix.isEmpty()&&current.startsWith(prefix);}
 private ReferenceDraftPolicy(){}
}
