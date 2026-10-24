package com.example.rokidkeyboardbridge;
import org.json.JSONObject;
/** Passive receipt of current phone guidance does not start a route or clear a user's hide choice. */
public final class NavigationEntryPolicy {
 public static String[] menu(){return new String[]{"現在の案内を表示","目的地を指定","行きのルート","帰りのルート","案内の操作・地図サイズ","保存経路（通信不要）","通知タイミング・表示","ナビの応答時間","予定から目的地"};}
 public static String[] menu(boolean navigating){String[] base=menu();if(!navigating)return base;String[] out=new String[base.length+3];out[0]="小さい地図・操作モード";out[1]="大きい地図・現在地追従";out[2]="全体マップ";System.arraycopy(base,0,out,3,base.length);return out;}
 public static boolean autoDisplay(JSONObject packet,long now){if(packet==null||!packet.optBoolean("navigationActive")||packet.optBoolean("suppressed")||packet.optBoolean("navigationComplete"))return false;long at=packet.optLong("time",0);return at>0&&now>=at&&now-at<=6L*60*60*1000;}
 private NavigationEntryPolicy(){}
}
