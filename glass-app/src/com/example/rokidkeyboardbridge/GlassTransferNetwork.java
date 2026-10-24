package com.example.rokidkeyboardbridge;
import android.content.*;import android.net.*;import java.io.*;
final class GlassTransferNetwork {
 static void require(Context c,String mode)throws IOException{if(mode.equals("any"))return;if(!mode.equals("wifi"))throw new IOException("通信設定を確認できません");try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities n=cm==null?null:cm.getNetworkCapabilities(cm.getActiveNetwork());if(n!=null&&n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)&&!n.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return;}catch(SecurityException unavailable){}throw new IOException("動画の抜粋画像はメガネ側も非従量制Wi-Fiに接続してから送信してください");}
}
