package com.example.rokidgeminisecretary;
public final class SavedRouteTicket {
 private String token="",route="",context="";private int revision,index;private long at;private boolean claimed;
 public synchronized String begin(String route,int revision,int index,String context,long now){this.token=java.util.UUID.randomUUID().toString();this.route=route;this.revision=revision;this.index=index;this.context=context;at=now;claimed=false;return token;}
 public synchronized void check(String token,String route,int revision,int index,String context,long now)throws Exception{if(claimed||!this.token.equals(token)||!this.route.equals(route)||this.revision!=revision||this.index!=index||!this.context.equals(context)||now<at||now-at>180000)throw new Exception("ルート・案内状態が変わったか候補の期限が切れました。候補を取り直してください");}
 public synchronized void invalidate(){token="";claimed=true;}
 public synchronized void claim()throws Exception{if(claimed||token.isEmpty())throw new Exception("開始要求は送信済みです。現在の案内を確認してください");claimed=true;}
}
