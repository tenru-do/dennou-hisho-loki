package com.example.rokidgeminisecretary;

import android.content.*;
import android.app.AlertDialog;
import android.graphics.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import org.json.*;

/** Restricted browser API. Browser credentials never authorize the bridge endpoints. */
final class LokiWebConsole {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static String code = "", invitation = "";
    private static long codeUntil;
    private static int attempts;
    private static final int PHOTO_PAGE_SIZE = 200;
    private static final Object PHOTO_WORK = new Object();
    private static final java.util.concurrent.atomic.AtomicBoolean GOOGLE_PHOTO_WORK=new java.util.concurrent.atomic.AtomicBoolean();
    static synchronized JSONObject trackedReceipt(Context owner,JSONObject data)throws Exception {
        String id=data.getString("id"),event=data.getString("event");JSONArray all=new JSONArray(prefs(owner).getString("outbox","[]"));boolean found=false,changed=false,execute=false;String finalState="";
        for(int i=0;i<all.length();i++){JSONObject old=all.getJSONObject(i);if(!id.equals(old.optString("key")))continue;
            JSONObject next=TrackedWebRequest.receipt(old,event,data.optString("reason",""),System.currentTimeMillis());
            finalState=next.optString("requestState");execute=TrackedWebRequest.executionGrant(old.optString("requestState"),event,finalState);
            changed=!next.toString().equals(old.toString());all.put(i,next);found=true;break;}
        if(!found)throw new IllegalArgumentException("Unknown request ID");
        // Save receipt before acknowledgement. Retrying an acknowledgement can only remove the matching head ID.
        if(changed&&!prefs(owner).edit().putString("outbox",all.toString()).commit())throw new IOException("Receipt storage failed");
        boolean ack="RECEIVED".equals(event)&&MainActivity.ackTrackedWebCommand(owner,id);
        return new JSONObject().put("ok",true).put("ack",ack).put("execute",execute).put("requestState",finalState);
    }
    static synchronized boolean queueReferenceConversation(Context c,String requestId,String text,String referenceKey)throws Exception{
        if(!WorkspaceCommandPolicy.validId(requestId))throw new IOException("参照を選び直してください");
        String key=digest("native-reference:"+requestId);JSONArray old=new JSONArray(prefs(c).getString("outbox","[]"));
        for(int i=0;i<old.length();i++){JSONObject row=old.getJSONObject(i);if(key.equals(row.optString("key"))){if(!text.equals(row.optString("message"))||!referenceKey.equals(row.optString("referenceKey")))throw new IOException("前の送信内容から変わっています。送信状況を確認し、参照を選び直してください");return row.optBoolean("accepted");}}
        if(!referenceKey.isEmpty()&&!ImageReferenceSummary.matchesKey(referenceKey,ImageReferenceStore.view(c)))throw new IOException("画像参照が更新・失効しています。選び直してください");
        TrackedWebRequest.wire(key,text,referenceKey);
        ConversationRecoveryStore.save(c,key,"send",new JSONObject().put("requestId",requestId).put("message",text).put("referenceKey",referenceKey));
        JSONObject record=new JSONObject().put("key",key).put("time",System.currentTimeMillis()).put("message",text).put("referenceKey",referenceKey).put("device","スマホ").put("status","受付中").put("tracked",true).put("requestState","CREATED");
        JSONArray next=WebRequestLedger.append(old,record);if(next==null)throw new IOException("未完了の送信が100件あります。送信状況を確認してください");
        if(!prefs(c).edit().putString("outbox",next.toString()).commit())throw new IOException("送信記録を保存できません");
        boolean queued=MainActivity.queueTrackedWebCommand(c,key,text,referenceKey);record.put("accepted",queued).put("requestState",queued?"QUEUED":"FAILED").put("status",queued?"グラスへ送信待ち":"送信できませんでした");
        if(!prefs(c).edit().putString("outbox",next.toString()).commit())throw new IOException("送信状態未確認です。再送前に送信状況を確認してください");return queued;
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("loki_web_console", 0); }
    static synchronized String openCode() {
        code = String.format(Locale.US, "%06d", RANDOM.nextInt(1000000));
        codeUntil = android.os.SystemClock.elapsedRealtime() + 120000;
        attempts = 0;
        byte[] random=new byte[32];RANDOM.nextBytes(random);StringBuilder ticket=new StringBuilder();for(byte b:random)ticket.append(String.format(Locale.US,"%02x",b&255));invitation=ticket.toString();
        return code;
    }
    private static synchronized boolean claim(String candidate) {
        if (code.isEmpty() || android.os.SystemClock.elapsedRealtime() >= codeUntil || ++attempts > 5) return false;
        if (!java.security.MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8))) return false;
        code = ""; invitation=""; return true;
    }
    static synchronized void revokeAll(Context c) {
        code = ""; invitation=""; codeUntil = 0;
        if (!prefs(c).edit().remove("sessions").commit()) throw new IllegalStateException("認証の解除に失敗しました");
    }
    static synchronized String invitationLink(String ip) {
        openCode();return "https://"+ip+":8766/web/#join="+invitation;
    }
    private static synchronized boolean claimConnection(String ticket) {
        if(code.isEmpty()||android.os.SystemClock.elapsedRealtime()>=codeUntil||++attempts>5)return false;
        if(!ticket.isEmpty()&&(!ticket.matches("[a-f0-9]{64}")||!java.security.MessageDigest.isEqual(ticket.getBytes(StandardCharsets.UTF_8),invitation.getBytes(StandardCharsets.UTF_8))))return false;
        code="";invitation="";return true;
    }
    static void show(MainActivity owner, String ip) {
        String link=invitationLink(ip), base="https://"+ip+":8766/web/";
        android.widget.LinearLayout content=new android.widget.LinearLayout(owner);content.setOrientation(1);int pad=(int)(16*owner.getResources().getDisplayMetrics().density);content.setPadding(pad,pad,pad,pad);
        android.widget.TextView description=new android.widget.TextView(owner);description.setText("タブレット：このQRをカメラで読み取ってください。\nPC：接続リンクを共有するか、登録済みのリンクを開いてください。\nURL・コードの手入力は不要です。最後にこのスマホで許可します。\n同じWi-Fiで使います。招待リンクは2分・1回限りです。");content.addView(description);
        Bitmap bitmap=LokiConnectionQr.image(link);android.widget.ImageView qr=new android.widget.ImageView(owner);qr.setImageBitmap(bitmap);qr.setContentDescription("ロキWeb接続用QRコード");int size=(int)(280*owner.getResources().getDisplayMetrics().density);android.widget.LinearLayout.LayoutParams qrLayout=new android.widget.LinearLayout.LayoutParams(size,size);qrLayout.gravity=android.view.Gravity.CENTER_HORIZONTAL;content.addView(qr,qrLayout);
        android.widget.TextView url=new android.widget.TextView(owner);url.setText(base);url.setTextIsSelectable(true);content.addView(url);
        android.widget.Button share=new android.widget.Button(owner);share.setText("接続リンクを共有");share.setOnClickListener(v->{Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,link);owner.startActivity(Intent.createChooser(send,"PC・タブレットへ接続リンクを送る"));});content.addView(share);
        android.widget.Button copy=new android.widget.Button(owner);copy.setText("接続リンクをコピー");copy.setOnClickListener(v->{((ClipboardManager)owner.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Loki connection",link));android.widget.Toast.makeText(owner,"接続リンクをコピーしました",0).show();});content.addView(copy);
        android.widget.TextView details=new android.widget.TextView(owner);String fingerprint;try{fingerprint=BridgeTlsServer.fingerprint();}catch(Exception e){fingerprint="取得できません";}details.setText("初回はこのスマホの証明書を確認してください。\nSHA256: "+fingerprint+"\n登録した端末は30日間再接続できます。共有PCでは端末登録をオフにしてください。");content.addView(details);
        android.widget.ScrollView scroll=new android.widget.ScrollView(owner);scroll.addView(content);
        new AlertDialog.Builder(owner).setTitle("QR・リンクでPC／タブレットを接続").setView(scroll)
            .setNeutralButton("全Web端末の認証を解除",(d,w)->new AlertDialog.Builder(owner).setMessage("PC・タブレットの接続をすべて解除しますか？").setPositiveButton("解除",(a,b)->{revokeAll(owner);android.widget.Toast.makeText(owner,"Web端末の認証を解除しました",0).show();}).setNegativeButton("取消",null).show())
            .setNegativeButton("閉じる",null).show();
    }
    private static String digest(String value) throws Exception { return RecorderPhotoStore.hash(value.getBytes(StandardCharsets.UTF_8)); }
    private static synchronized JSONObject authenticate(Context c, String token) throws Exception {
        if (token == null || !token.matches("[a-f0-9]{64}")) return null;
        JSONArray sessions = new JSONArray(prefs(c).getString("sessions", "[]"));
        String hash = digest(token);
        for (int i=0;i<sessions.length();i++) {
            JSONObject s=sessions.getJSONObject(i);
            if (s.getLong("expires") > System.currentTimeMillis() && java.security.MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8),s.getString("hash").getBytes(StandardCharsets.UTF_8))) return s;
        }
        return null;
    }
    private static synchronized JSONObject enroll(Context c,String name) throws Exception {return enroll(c,name,false);}
    private static synchronized JSONObject enroll(Context c,String name,boolean remember) throws Exception {
        JSONArray old=new JSONArray(prefs(c).getString("sessions","[]")), sessions=new JSONArray();
        for(int i=0;i<old.length();i++) if(old.getJSONObject(i).getLong("expires")>System.currentTimeMillis()) sessions.put(old.getJSONObject(i));
        if(sessions.length()>=8) throw new IOException("端末数の上限です。スマホで認証を解除してください");
        int hours=remember?30*24:24;byte[] random=new byte[32];RANDOM.nextBytes(random);StringBuilder t=new StringBuilder();for(byte b:random)t.append(String.format(Locale.US,"%02x",b&255));
        String token=t.toString();sessions.put(new JSONObject().put("hash",digest(token)).put("name",name).put("expires",System.currentTimeMillis()+hours*60*60*1000L));
        if(!prefs(c).edit().putString("sessions",sessions.toString()).commit())throw new IOException("認証を保存できません");
        return new JSONObject().put("token",token).put("expiresInHours",hours).put("remember",remember);
    }
    private static File photo(Context c,String id) throws Exception {
        if(!id.matches("[0-9]{13}-[a-f0-9-]{36}"))throw new IllegalArgumentException("invalid id");
        File f=new File(RecorderPhotoStore.root(c),id+".gcm");if(!f.isFile())throw new FileNotFoundException();return f;
    }
    private static ArrayList<File> photos(Context c) {
        ArrayList<File> out=new ArrayList<>();File[] files=RecorderPhotoStore.root(c).listFiles();
        if(files!=null)for(File f:files)if(f.getName().matches("[0-9]{13}-[a-f0-9-]{36}\\.gcm"))out.add(f);
        out.sort((a,b)->b.getName().compareTo(a.getName()));return out;
    }
    private static String id(File f){return f.getName().substring(0,f.getName().length()-4);}
    private static byte[] asset(Context context,String name) throws IOException {
        // Only fixed assets selected by the route handler are read.
            try(InputStream in=context.getAssets().open("loki-web/"+name);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] b=new byte[8192];int n;while((n=in.read(b))>0){if(out.size()+n>262144)throw new IOException("web asset too large");out.write(b,0,n);}return out.toByteArray();
            }
    }
    static boolean handle(MainActivity owner, Socket socket, String request, String body, String token, String origin, String host) throws Exception {
        if(request==null)return false;
        String[] parts=request.split(" ");if(parts.length<2)return false;
        String method=parts[0], target=parts[1];if(!target.equals("/web")&&!target.startsWith("/web/"))return false;
        try {
            URI uri=new URI(target);String path=uri.getPath();
            if(method.equals("GET")&&path.equals("/web/icon.png")) {
                respond(socket,200,"image/png",asset(owner,"icon.png"));return true;
            }
            if(method.equals("GET")&&(path.equals("/web")||path.equals("/web/")||path.equals("/web/app.js")||path.equals("/web/media-attachments.js")||path.equals("/web/local-records.js")||path.equals("/web/style.css"))) {
                String asset=path.equals("/web/local-records.js")?"local-records.js":path.equals("/web/media-attachments.js")?"media-attachments.js":path.endsWith(".js")?"app.js":path.endsWith(".css")?"style.css":"index.html";
                respond(socket,200,asset.endsWith(".js")?"text/javascript; charset=utf-8":asset.endsWith(".css")?"text/css; charset=utf-8":"text/html; charset=utf-8",asset(owner,asset));return true;
            }
            // No CORS. Reject cross-origin fetches even if a browser retained a token.
            if(!origin.isEmpty()&&!origin.equals("https://"+host)){json(socket,403,new JSONObject().put("error","別のサイトからの操作は許可されません"));return true;}
            if(method.equals("POST")&&path.equals("/web/api/connect")) {
                JSONObject input=new JSONObject(body);String name=input.optString("name","ブラウザ").trim();if(name.isEmpty())name="ブラウザ";if(name.length()>40)name=name.substring(0,40);
                if(!claimConnection(input.optString("invitation",""))){json(socket,403,new JSONObject().put("error","スマホの共有画面を開いてから接続してください。招待リンクは2分間有効です"));return true;}
                int permission=owner.confirmWebConnection(socket,name,input.optBoolean("remember",false));
                if(permission==0){json(socket,403,new JSONObject().put("error","スマホでの許可がありません。共有画面を開き直してください"));return true;}
                json(socket,200,enroll(owner,name,permission==2));return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/pair")) {
                JSONObject input=new JSONObject(body);String candidate=input.optString("code");
                if(!claim(candidate)||!owner.confirmPairing(socket,candidate)){json(socket,403,new JSONObject().put("error","コードが無効、期限切れ、またはスマホで拒否されました"));return true;}
                String name=input.optString("name","ブラウザ").trim();if(name.isEmpty())name="ブラウザ";if(name.length()>40)name=name.substring(0,40);
                json(socket,200,enroll(owner,name));return true;
            }
            JSONObject session=authenticate(owner,token);if(session==null){json(socket,401,new JSONObject().put("error","スマホで接続コードを発行し、接続してください"));return true;}
            if(method.equals("POST")&&path.equals("/web/api/secretary-inbox-action")){json(socket,200,SecretaryInbox.action(owner,new JSONObject(body)));return true;}
            if(method.equals("POST")&&path.equals("/web/api/media-drop-status")) {
                JSONObject input=new JSONObject(body),record=new JSONObject().put("time",System.currentTimeMillis());
                JSONArray types=input.optJSONArray("types"),safeTypes=new JSONArray();if(types!=null)for(int i=0;i<Math.min(12,types.length());i++){String t=types.optString(i);if(t.matches("[a-zA-Z0-9/_+.-]{1,60}"))safeTypes.put(t);}
                record.put("types",safeTypes).put("files",Math.max(0,Math.min(100,input.optInt("files")))).put("images",Math.max(0,Math.min(100,input.optInt("images")))).put("stage",input.optString("stage").equals("received")?"received":"failed");
                prefs(owner).edit().putString("media_drop_status",record.toString()).apply();json(socket,200,new JSONObject().put("ok",true));return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/google-photo-import")) {
                if(!GOOGLE_PHOTO_WORK.compareAndSet(false,true))throw new IOException("画像の取り込み中です。少し待って再度ドロップしてください");
                try{byte[] jpeg=GooglePhotoImport.read(new JSONObject(body).getString("url"));json(socket,200,new JSONObject().put("image",android.util.Base64.encodeToString(jpeg,android.util.Base64.NO_WRAP)));}finally{GOOGLE_PHOTO_WORK.set(false);}return true;
            }
            Map<String,String> query=new HashMap<>();if(uri.getRawQuery()!=null)for(String part:uri.getRawQuery().split("&")){String[] q=part.split("=",2);query.put(q[0],q.length==2?URLDecoder.decode(q[1],"UTF-8"):"");}
            if(method.equals("POST")&&path.equals("/web/api/media-generate")){JSONObject input=new JSONObject(body);String rawId=input.getString("requestId"),parent=input.optString("parent"),message=input.getString("message");if(!rawId.matches("[a-zA-Z0-9-]{8,80}")||parent.isEmpty())throw new IOException("元画像を選択してください");MediaGenerationIntent.Kind kind=MediaGenerationContext.kind(owner,message,true);String requestId=digest(session.getString("hash")+":"+rawId);ConversationRecoveryStore.save(owner,requestId,"media-generate",input);if(kind==MediaGenerationIntent.Kind.VIDEO)json(socket,200,VideoGeneration.enqueue(owner,requestId,message,null,parent));else if(kind==MediaGenerationIntent.Kind.IMAGE)json(socket,200,ImageGeneration.enqueue(owner,requestId,message,null,parent));else throw new IOException("元画像をどう変えるか、どんな動きを付けるか入力してください");return true;}
            if(method.equals("GET")&&path.equals("/web/api/secretary-inbox")){json(socket,200,query.containsKey("id")?SecretaryInbox.get(owner,query.get("id")):SecretaryInbox.list(owner));return true;}
            if(method.equals("GET")&&path.equals("/web/api/video-generations")){json(socket,200,new JSONObject().put("items",VideoGeneration.list(owner)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/generated-video")) {String id=query.getOrDefault("id","");JSONObject j=VideoGeneration.read(owner,id);if(!"completed".equals(j.optString("state")))throw new IOException("動画がまだ完成していません");OutputStream out=socket.getOutputStream();String headers="HTTP/1.1 200 OK\r\nContent-Type: video/mp4\r\nContent-Length: "+j.getInt("bytes")+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\nContent-Disposition: inline; filename=\"Loki-video-"+id+".mp4\"\r\nConnection: close\r\n\r\n";out.write(headers.getBytes(StandardCharsets.US_ASCII));VideoGeneration.writeVideo(owner,id,out);out.flush();return true;}
            if(method.equals("POST")&&path.equals("/web/api/video-generate")) {JSONObject input=new JSONObject(body);String rawId=input.getString("requestId");if(!rawId.matches("[a-zA-Z0-9-]{8,80}"))throw new IOException("送信IDが無効です");byte[] image=null;String encoded=input.optString("image");if(!encoded.isEmpty()){if(encoded.length()>800000)throw new IOException("元画像が大きすぎます");image=ImageQuestions.normalize(android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP));}json(socket,200,VideoGeneration.enqueue(owner,digest(session.getString("hash")+":"+rawId),input.getString("message"),image,input.optString("parent")));return true;}
            if(method.equals("POST")&&path.equals("/web/api/generated-video-delete")){VideoGeneration.delete(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("POST")&&path.equals("/web/api/generated-video-resume")){VideoGeneration.resume(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("GET")&&path.equals("/web/api/image-generations")){json(socket,200,new JSONObject().put("items",ImageGeneration.list(owner)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/generated-image")) {String id=query.getOrDefault("id","");boolean thumb="thumb".equals(query.get("size"));respond(socket,200,thumb?"image/jpeg":ImageGeneration.read(owner,id).getString("mime"),ImageGeneration.image(owner,id,thumb));return true;}
            if(method.equals("POST")&&path.equals("/web/api/image-generate")) {
                JSONObject input=new JSONObject(body);String rawId=input.getString("requestId");if(!rawId.matches("[a-zA-Z0-9-]{8,80}"))throw new IllegalArgumentException("request id");byte[] image=null;String encoded=input.optString("image");if(!encoded.isEmpty()){if(encoded.length()>800000)throw new IOException("画像が大きすぎます");image=ImageQuestions.normalize(android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP));}
                json(socket,200,ImageGeneration.enqueue(owner,digest(session.getString("hash")+":"+rawId),input.getString("message"),image,input.optString("parent")));return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/generated-delete")){ImageGeneration.delete(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("POST")&&path.equals("/web/api/image-question")) {
                JSONObject input=new JSONObject(body);String encoded=input.getString("image");if(encoded.length()>800000)throw new IllegalArgumentException("image too large");
                byte[] jpeg=ImageQuestions.normalize(android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP));
                String rawId=input.getString("requestId");if(!rawId.matches("[a-zA-Z0-9-]{8,80}"))throw new IllegalArgumentException("invalid request id");
                String requestId=digest(session.getString("hash")+":"+rawId);
                json(socket,200,ImageQuestions.enqueue(owner,requestId,input.optString("message",""),jpeg,input.optInt("imageCount",1),input.optJSONArray("kinds")));return true;
            }
            if(method.equals("GET")&&path.equals("/web/api/photos")) {
                int offset=Math.max(0,Integer.parseInt(query.getOrDefault("offset","0")));ArrayList<File> all=photos(owner);offset=Math.min(offset,Math.max(0,((all.size()-1)/PHOTO_PAGE_SIZE)*PHOTO_PAGE_SIZE));JSONArray items=new JSONArray();
                for(int i=offset;i<Math.min(all.size(),offset+PHOTO_PAGE_SIZE);i++){File f=all.get(i);items.put(new JSONObject().put("id",id(f)).put("time",Long.parseLong(id(f).substring(0,13))).put("metadata",RecorderPhotoStore.metadata(f)));}
                json(socket,200,new JSONObject().put("total",all.size()).put("offset",offset).put("pageSize",PHOTO_PAGE_SIZE).put("items",items));return true;
            }
            if(method.equals("GET")&&path.equals("/web/api/meeting-import")){json(socket,200,MeetingMediaImport.list(owner));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-import-begin")){json(socket,200,MeetingMediaImport.begin(owner,new JSONObject(body)));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-import-chunk")){json(socket,200,MeetingMediaImport.chunk(owner,new JSONObject(body)));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-import-finish")){MeetingMediaImport.finish(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-import-cancel")){MeetingMediaImport.cancel(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("GET")&&path.equals("/web/api/meeting-control")){json(socket,200,MeetingControl.status(owner));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-control")){json(socket,200,MeetingControl.queue(owner,new JSONObject(body)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/local-records")){json(socket,200,UnifiedRecords.search(owner,query.getOrDefault("q","")));return true;}
            if(method.equals("GET")&&path.equals("/web/api/reading-history")){JSONArray list=ReadingHistory.list(owner),items=new JSONArray();for(int i=list.length()-1;i>=0;i--){JSONObject row=list.getJSONObject(i);items.put(new JSONObject().put("id",row.getString("id")).put("at",row.optLong("at")).put("question",row.optString("question")).put("correctionCount",row.getJSONArray("corrections").length()));}json(socket,200,new JSONObject().put("items",items));return true;}
            if(method.equals("GET")&&path.equals("/web/api/reading-detail")){json(socket,200,ReadingHistory.get(owner,query.getOrDefault("id","")));return true;}
            if(method.equals("POST")&&path.equals("/web/api/reading-correct")){JSONObject input=new JSONObject(body);json(socket,200,ReadingHistory.correct(owner,input.getString("id"),input.getInt("revision"),input.getString("changeId"),input.getString("field"),input.optString("before"),input.getString("after")));return true;}
            if(method.equals("POST")&&path.equals("/web/api/reading-delete")){ReadingHistory.delete(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("GET")&&path.equals("/web/api/departure-check")){json(socket,200,DepartureDialog.read(owner));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-action-candidates")){JSONObject actionPacket=new JSONObject(body);json(socket,200,MeetingActions.preview(owner,actionPacket.getString("id")));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-action-save")){JSONObject actionPacket=new JSONObject(body);json(socket,200,MeetingActions.save(owner,actionPacket.getString("id"),actionPacket.getString("sourceHash"),actionPacket.getInt("index"),actionPacket.getString("text")));return true;}
            if(method.equals("GET")&&path.equals("/web/api/meetings")){json(socket,200,new JSONObject().put("items",MeetingAudioStore.list(owner)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/meeting-audio")){
                File f=MeetingAudioStore.folder(owner,query.getOrDefault("id",""));JSONObject m=MeetingAudioStore.meta(f);int index=Integer.parseInt(query.getOrDefault("part","0"));if(index>=m.getJSONArray("parts").length())throw new IOException("invalid recording part");respond(socket,200,"audio/wav",MeetingAudioStore.audio(f,index));return true;
            }
            if(method.equals("GET")&&path.equals("/web/api/meeting-transcript")){
                File f=MeetingAudioStore.folder(owner,query.getOrDefault("id",""));byte[] text=DeviceSecrets.photos().decrypt(java.nio.file.Files.readAllBytes(new File(f,"transcript.gcm").toPath()));respond(socket,200,"text/plain; charset=utf-8",text);return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/meeting-analyze")){MeetingAudioStore.analyze(owner,new JSONObject(body).getString("id"));json(socket,200,new JSONObject().put("ok",true));return true;}
            if(method.equals("GET")&&path.equals("/web/api/photo")) {
                synchronized(PHOTO_WORK) {
                    File f=photo(owner,query.getOrDefault("id",""));ByteArrayOutputStream out=new ByteArrayOutputStream();
                    if(query.getOrDefault("size","").equals("thumb")){Bitmap bitmap=RecorderGalleryActivity.decodePhoto(f,256);try{bitmap.compress(Bitmap.CompressFormat.JPEG,75,out);}finally{bitmap.recycle();}}
                    else RecorderGalleryActivity.exportJpeg(owner,f,out);
                    respond(socket,200,"image/jpeg",out.toByteArray());
                }return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/delete")) {
                JSONObject input=new JSONObject(body);JSONArray ids=input.getJSONArray("ids");if(ids.length()<1||ids.length()>PHOTO_PAGE_SIZE)throw new IllegalArgumentException("1ページの選択範囲で削除してください");
                ArrayList<File> selected=new ArrayList<>();for(int i=0;i<ids.length();i++)selected.add(photo(owner,ids.getString(i)));int deleted=0;for(File f:selected){RecorderPhotoStore.delete(owner,f);deleted++;}
                json(socket,200,new JSONObject().put("deleted",deleted));return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/delete-all")) {
                JSONObject input=new JSONObject(body);ArrayList<File> all=photos(owner);
                String revision=digest(all.toString());if(!input.optString("revision").equals(revision)){json(socket,409,new JSONObject().put("error","写真の一覧が変わりました。もう一度確認してください"));return true;}
                int deleted=0;for(File f:all){RecorderPhotoStore.delete(owner,f);deleted++;}json(socket,200,new JSONObject().put("deleted",deleted));return true;
            }
            if(method.equals("GET")&&path.equals("/web/api/delete-preview")){ArrayList<File> all=photos(owner);json(socket,200,new JSONObject().put("count",all.size()).put("revision",digest(all.toString())));return true;}
            if(method.equals("GET")&&path.equals("/web/api/operations")){json(socket,200,OperationOverview.read(owner));return true;}
            if(method.equals("POST")&&path.equals("/web/api/local-health-check")){LocalHealthCheck.start(owner,true);json(socket,200,new JSONObject().put("started",true));return true;}
            if(method.equals("POST")&&path.equals("/web/api/calendar-preview")){try{json(socket,200,CalendarWriter.preview(owner,new JSONObject(body)));}catch(Exception error){json(socket,400,new JSONObject().put("error","候補・日時・カレンダーを確認できません。登録していません"));}return true;}
            if(method.equals("GET")&&path.equals("/web/api/workspace")){json(socket,200,ConversationWorkspace.snapshot(owner));return true;}
            if(method.equals("POST")&&path.equals("/web/api/workspace-command")){json(socket,200,ConversationWorkspace.submit(owner,new JSONObject(body)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/navigation-health")){json(socket,200,owner.navigationHealth());return true;}
            if(method.equals("POST")&&path.equals("/web/api/navigation-retry")){owner.runOnUiThread(owner::retryNavigationForUser);json(socket,200,new JSONObject().put("queued",true));return true;}
            if(method.equals("POST")&&path.equals("/web/api/meeting-verify")){json(socket,200,MeetingAudioStore.verify(owner,new JSONObject(body).getString("id")));return true;}
            if(method.equals("GET")&&path.equals("/web/api/recovery")){json(socket,200,query.containsKey("id")?ConversationRecoveryStore.read(owner,query.get("id")):new JSONObject().put("items",ConversationRecoveryStore.list(owner)));return true;}
            if(method.equals("GET")&&path.equals("/web/api/history")){json(socket,200,new JSONObject().put("items",owner.webHistory()).put("requests",new JSONArray(prefs(owner).getString("outbox","[]"))).put("runtime",owner.webConversationRuntime()).put("imageReference",ImageReferenceStore.view(owner)).put("progress",ConversationWorkspace.snapshot(owner).optJSONObject("conversation")).put("progressFresh",ConversationWorkspace.snapshot(owner).optBoolean("connected")));return true;}
            if(method.equals("POST")&&path.equals("/web/api/send")) {
                synchronized(LokiWebConsole.class) {
                    JSONObject input=new JSONObject(body);String text=input.getString("message").trim(), requestId=input.getString("requestId");
                    if(text.isEmpty()||text.length()>2800||text.contains("__LOKI_")||text.contains("__CODEX_")||!requestId.matches("[a-zA-Z0-9-]{8,80}"))throw new IllegalArgumentException("送信内容が無効です（最大2800文字）");
                    if(input.has("referenceKey")&&(MediaGenerationContext.video(owner,text)||ImageGeneration.handles(owner,text)))throw new IOException("参照付きの質問から生成は開始しません。生成する場合は質問の書き出しを消して新しく依頼してください");
                    ConversationRecoveryStore.save(owner,digest(session.getString("hash")+":"+requestId),"send",input);
                    if(MediaGenerationContext.video(owner,text)){json(socket,200,VideoGeneration.enqueue(owner,digest(session.getString("hash")+":"+requestId),text,null,""));return true;}
                    if(ImageGeneration.handles(owner,text)){json(socket,200,ImageGeneration.enqueue(owner,digest(session.getString("hash")+":"+requestId),text,null,""));return true;}
                    JSONArray old=new JSONArray(prefs(owner).getString("outbox","[]"));String key=digest(session.getString("hash")+":"+requestId);
                    ConversationRecoveryStore.save(owner,key,"send",input);
                    for(int i=0;i<old.length();i++)if(old.getJSONObject(i).optString("key").equals(key)){boolean accepted=old.getJSONObject(i).optBoolean("accepted",old.getJSONObject(i).optString("status").equals("グラスへ送信待ち"));json(socket,accepted?200:409,new JSONObject().put("queued",accepted).put("duplicate",true).put("error",accepted?"":"送信完了を確認できません。スマホの送信状況を確認してください"));return true;}
                    if(input.has("referenceKey")&&!ImageReferenceSummary.matchesKey(input.optString("referenceKey"),ImageReferenceStore.view(owner)))throw new IOException("参照が更新・失効しています。本文を残して最新の参照を選び直してください。まだ送信していません");
                    // Persist intent before enqueue. Retries never enqueue the same browser request twice.
                    JSONObject record=new JSONObject().put("key",key).put("time",System.currentTimeMillis()).put("message",text).put("device",session.getString("name")).put("status","受付中").put("tracked",true).put("requestState","CREATED");
                    JSONArray next=WebRequestLedger.append(old,record);
                    if(next==null){json(socket,409,new JSONObject().put("queued",false).put("error","未完了の送信記録が100件あります。送信状況を確認してください。追加送信はしていません"));return true;}
                    if(!prefs(owner).edit().putString("outbox",next.toString()).commit())throw new IOException("送信記録を保存できません");
                    boolean queued=MainActivity.queueTrackedWebCommand(owner,key,text,input.optString("referenceKey"));record.put("accepted",queued).put("requestState",queued?"QUEUED":"FAILED").put("status",queued?"グラスへ送信待ち":"送信できませんでした（再入力してください）");
                    if(!prefs(owner).edit().putString("outbox",next.toString()).commit())throw new IOException("送信状態を保存できません");
                    json(socket,queued?200:409,new JSONObject().put("queued",queued).put("error",queued?"":"グラスの送信待ちが満杯です"));
                }return true;
            }
            if(method.equals("POST")&&path.equals("/web/api/import")){boolean queued=MainActivity.queueBridgeCommand(owner,"__LOKI_PHOTO_SYNC__",true);json(socket,queued?200:409,new JSONObject().put("queued",queued));return true;}
            if(method.equals("POST")&&path.equals("/web/api/logout")) {
                synchronized(LokiWebConsole.class){JSONArray old=new JSONArray(prefs(owner).getString("sessions","[]")),next=new JSONArray();for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).getString("hash").equals(session.getString("hash")))next.put(old.getJSONObject(i));if(!prefs(owner).edit().putString("sessions",next.toString()).commit())throw new IOException();}
                json(socket,200,new JSONObject().put("ok",true));return true;
            }
            json(socket,404,new JSONObject().put("error","この操作はありません"));
        }catch(FileNotFoundException e){json(socket,404,new JSONObject().put("error","写真が見つかりません。一覧を更新してください"));}
        catch(IOException e){json(socket,409,new JSONObject().put("error",e.getMessage()==null?"保存先または接続を確認してください":e.getMessage()));}
        catch(IllegalArgumentException|JSONException e){json(socket,400,new JSONObject().put("error","入力を確認してください"));}
        catch(Exception e){android.util.Log.w("LokiWebConsole","Browser request failed: "+e.getClass().getSimpleName());json(socket,500,new JSONObject().put("error","処理できませんでした。スマホの状態を確認してください"));}
        return true;
    }
    private static void json(Socket socket,int status,JSONObject body)throws IOException{respond(socket,status,"application/json; charset=utf-8",body.toString().getBytes(StandardCharsets.UTF_8));}
    private static void respond(Socket socket,int status,String type,byte[] bytes)throws IOException {
        String text=status==200?"OK":status==401?"Unauthorized":status==403?"Forbidden":status==404?"Not Found":status==409?"Conflict":status==400?"Bad Request":"Internal Server Error";
        String headers="HTTP/1.1 "+status+" "+text+"\r\nContent-Type: "+type+"\r\nContent-Length: "+bytes.length+"\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; img-src 'self' blob:; style-src 'self'; script-src 'self'; connect-src 'self'; media-src 'self' blob:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'\r\nConnection: close\r\n\r\n";
        OutputStream out=socket.getOutputStream();out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();
    }
}
