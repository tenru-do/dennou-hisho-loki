package com.example.rokidgeminisecretary;
/** Compare a new route to the recent remaining estimate, never an old whole-trip total. */
public final class NavigationRouteChange {
 private String destination="",mode="",reason="",message="";private double seconds=-1,metres=-1;private long at,published;
 public synchronized void begin(String d,String m,String why,double s,double distance,long sample,long now,boolean clear){
  message="";published=0;
  if(clear||sample<=0||now<sample||now-sample>15000||!Double.isFinite(s)||s<0){at=0;message="";return;}destination=d;mode=m;reason=why;seconds=s;metres=distance;at=sample;
 }
 public synchronized void finish(String d,String m,double newSeconds,double newMetres,long now){
  if(at<=0||!destination.equals(d)||!mode.equals(m)||now<at||now-at>60000||!Double.isFinite(newSeconds)||newSeconds<0){at=0;message="";published=0;return;}
  long minutes=Math.round((newSeconds-Math.max(0,seconds-(now-at)/1000.0))/60.0);String time=minutes==0?"残り時間の見込みはほぼ同じ":minutes>0?"残り時間の見込みは約"+minutes+"分増":"残り時間の見込みは約"+(-minutes)+"分減";
  String distance="";if(now-at<=3000&&Double.isFinite(newMetres)&&newMetres>=0&&Double.isFinite(metres)&&metres>=0){long delta=Math.round((newMetres-metres)/100)*100;if(Math.abs(delta)>=100)distance="、距離は約"+Math.abs(delta)+"m"+(delta>0?"増":"減");}
  message=(reason.isEmpty()?"経路更新":reason)+"："+time+distance+"（推定）";published=now;at=0;
 }
 public synchronized String message(long now){return published>0&&now>=published&&now-published<=45000?message:"";}
}
