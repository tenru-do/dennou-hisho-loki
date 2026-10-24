package com.example.rokidgeminisecretary;
import android.content.*;import android.net.*;import org.json.*;import java.net.*;import java.io.*;
/** Restrict only bulk requests. Bind their connections to the approved Network, never the process. */
final class TransferNetwork {
 static final long LARGE=1048576;
 static final class Blocked extends IOException{Blocked(){super("大きなデータはWi-Fi待ちです。検証済み・非従量制のWi-Fiに接続し、内容を確認して再送してください。自動再送はしません");}}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("loki-transfer-network",0);}
 static String mode(Context c){return prefs(c).getString("mode","wifi").equals("any")?"any":"wifi";}
 static boolean allowed(String mode,boolean wifi,boolean validated,boolean unmetered,boolean vpn){return "any".equals(mode)||wifi&&validated&&unmetered&&!vpn;}
 static boolean bulk(long bytes,boolean video){return video||bytes>=LARGE;}
 static Network approved(Context c)throws Blocked{try{ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);Network network=cm==null?null:cm.getActiveNetwork();NetworkCapabilities caps=network==null?null:cm.getNetworkCapabilities(network);if(caps!=null&&allowed("wifi",caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)))return network;}catch(SecurityException unavailable){}throw new Blocked();}
 static void require(Context c,long bytes,boolean video)throws Blocked{if(bulk(bytes,video)&&!mode(c).equals("any"))approved(c);}
 static HttpURLConnection open(Context c,URL url,long bytes,boolean video)throws Exception{if(bulk(bytes,video)&&!mode(c).equals("any"))return (HttpURLConnection)approved(c).openConnection(url);return (HttpURLConnection)url.openConnection();}
 static long encodedEstimate(long bytes){if(bytes<0||bytes>100000000)throw new IllegalArgumentException("容量を確認してください");return ((bytes+2)/3)*4;}
 static String describe(Context c,long bytes,boolean video){String state="利用可能な回線から送信";if(!mode(c).equals("any")){try{approved(c);state="Wi-Fiのみ：現在は送信条件を満たしています";}catch(Blocked wait){state="Wi-Fiのみ：現在は条件未確認・Wi-Fi待ち";}}return state+"\n添付の符号化後目安 "+encodedEstimate(Math.max(0,bytes))+"バイト（指示文・追加参照・通信ヘッダーは別）"+(video?"。生成動画の取得容量は完成前には不明・最大32MB":"");}
 static JSONObject state(Context c)throws Exception{return new JSONObject().put("mode",mode(c)).put("largeBytes",LARGE).put("description",describe(c,0,false));}
 static void save(Context c,String mode)throws Exception{if(!mode.matches("wifi|any"))throw new IOException("回線を選び直してください");if(!prefs(c).edit().putString("mode",mode).commit())throw new IOException("通信設定を保存できません");}
 static void show(MainActivity c){ReliabilityDialog ui=new ReliabilityDialog(c);android.widget.LinearLayout b=ui.box();ui.text(b,describe(c,0,false)+"\n対象：画像・動画の生成と取得、1MiB以上の会議音声送信、動画の抜粋画像。Wi-FiはOSが非従量制と判定した回線を使います。テザリング元の料金までは判定できません。VPN経由はWi-Fiのみ設定では保留します。回線の自動切り替えや、自動再送は行いません。");ui.button(b,"大きなデータはWi-Fiのみ",()->ui.work(()->{save(c,"wifi");return state(c);},v->ui.display("通信設定を保存",textBox(ui,v.optString("description")))));ui.button(b,"モバイルを含む利用可能な回線を許可",()->ui.confirm("モバイル回線や従量制Wi-Fiでも大きなデータを送信・取得できるようにします。通信料が発生する可能性があります。",()->ui.work(()->{save(c,"any");return state(c);},v->ui.display("通信設定を保存",textBox(ui,v.optString("description"))))));ui.display("通信回線・送信容量",b);}
 static android.widget.LinearLayout textBox(ReliabilityDialog ui,String text){android.widget.LinearLayout b=ui.box();ui.text(b,text);return b;}
 private TransferNetwork(){}
}
