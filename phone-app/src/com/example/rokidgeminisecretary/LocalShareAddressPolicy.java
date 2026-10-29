package com.example.rokidgeminisecretary;
import java.util.Locale;
/** Pure policy for local sharing; never advertise a cellular/VPN address as a Wi-Fi link. */
final class LocalShareAddressPolicy {
 static int priority(String name,boolean wifi,boolean ethernet,boolean vpn){String n=name==null?"":name.toLowerCase(Locale.ROOT);if(vpn||n.matches("(?:lo|rmnet.*|v4-rmnet.*|ccmni.*|pdp.*|wwan.*|clat.*|tun.*|ppp.*|dummy.*)"))return -1;if(n.matches("(?:swlan.*|ap[0-9_].*|ap[0-9]|softap.*)"))return 400;if(wifi)return 300;if(ethernet)return 250;if(n.matches("(?:wlan.*|wifi.*|swlan.*|ap[0-9_].*|ap[0-9]|softap.*)"))return 200;if(n.matches("(?:eth.*|en[0-9].*)"))return 150;return -1;}
 static boolean valid(String ip){if(ip==null||!ip.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}"))return false;String[] p=ip.split("\\.");int a=Integer.parseInt(p[0]),b=Integer.parseInt(p[1]);for(String n:p)if(Integer.parseInt(n)>255)return false;return a>0&&a<224&&a!=127&&!(a==169&&b==254);}
 static final class Choice {String address="";int score=-1;void offer(String ip,String name,boolean wifi,boolean ethernet,boolean vpn,boolean active){int p=priority(name,wifi,ethernet,vpn);if(p<0||!valid(ip))return;if(active)p+=10;if(p>score){score=p;address=ip;}}}
 private LocalShareAddressPolicy(){}
}
