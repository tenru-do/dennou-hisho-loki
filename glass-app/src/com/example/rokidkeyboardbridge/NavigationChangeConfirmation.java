package com.example.rokidkeyboardbridge;
/** A single expiring proposal bound to the active route revision. */
public final class NavigationChangeConfirmation {
 private String action="",target="",route="";private long revision,until;
 public synchronized void arm(String action,String target,String route,long revision,long now){this.action=action;this.target=target;this.route=route;this.revision=revision;until=now+60000;}
 public synchronized boolean pending(){return !action.isEmpty();}
 public synchronized void clear(){action="";target="";}
 public synchronized String[] consume(boolean yes,boolean active,String route,long revision,long now){String a=action,t=target;clear();if(a.isEmpty())return new String[]{"empty",""};if(!yes)return new String[]{"cancel",""};if(!active||now>until||now<until-60000||!this.route.equals(route)||this.revision!=revision)return new String[]{"stale",""};return new String[]{a,t};}
}
