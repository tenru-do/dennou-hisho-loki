package com.example.rokidgeminisecretary;
/** Compares arrival timestamps, not naturally decreasing remaining durations. */
public final class NavigationEtaChange {
 private String route="",message="";private long sample,arrival,published,revision;
 public synchronized String update(String identity,long routeRevision,double seconds,long now,boolean usable,String observedChange){
  if(!usable||!Double.isFinite(seconds)||seconds<0||seconds>604800){sample=0;published=0;message="";return "";}
  long next=now+Math.round(seconds*1000);
  if(!identity.equals(route)||sample<=0||now<sample||now-sample>120000){route=identity;sample=now;arrival=next;revision=routeRevision;published=0;message="";return "";}
  long delta=Math.round((next-arrival)/60000.0);
  if(Math.abs(delta)>=2&&(published==0||now-published>=120000)){
   String evidence=routeRevision!=revision?"経路を再取得しています。":observedChange!=null&&!observedChange.isEmpty()?observedChange+"。":"変化の原因は取得できていません。";
   message="到着見込みが約"+Math.abs(delta)+"分"+(delta>0?"延びました。":"早まりました。")+evidence;published=now;arrival=next;
  }
  sample=now;revision=routeRevision;return published>0&&now>=published&&now-published<=45000?message:"";
 }
}
