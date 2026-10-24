package com.example.rokidgeminisecretary;
import android.app.Activity;import org.json.JSONObject;
/** Process-local verification dependencies. No intent, preference or bridge can enable this. */
final class NavigationNativeEnvironment {
 interface Request {JSONObject run(String operation,String value)throws Exception;}
 final Activity dialogs;final Request request;final Runnable started;
 NavigationNativeEnvironment(Activity dialogs,Request request,Runnable started){this.dialogs=dialogs;this.request=request;this.started=started;}
}
