package com.example.rokidgeminisecretary;
/** Only one delayed refresh is scheduled; later requests replace its payload. */
public final class NavigationRefreshSlot {
 public static final class Request {public final String destination,mode;public final long generation;Request(String d,String m,long g){destination=d;mode=m;generation=g;}}
 private Request pending;
 public synchronized boolean offer(String destination,String mode,long generation){boolean schedule=pending==null;pending=new Request(destination,mode,generation);return schedule;}
 public synchronized Request take(){Request next=pending;pending=null;return next;}
}
