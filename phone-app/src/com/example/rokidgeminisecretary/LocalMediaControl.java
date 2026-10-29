package com.example.rokidgeminisecretary;

import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.SystemClock;
import org.json.*;
import java.util.*;

/** Controls only an explicitly selected session on THIS Android device. No AVRCP fallback. */
final class LocalMediaControl {
    static String device(Context c){return c.getResources().getConfiguration().smallestScreenWidthDp>=600?"タブレット":"スマホ";}
    interface Session {
        String app(); String identity(); PlaybackState state(); long duration();
        void seek(long position);
    }
    private static final class Entry {
        final Session session; final String identity; final long issued;
        Entry(Session s,long now){session=s;identity=s.identity();issued=now;}
    }
    private static final Map<String,Entry> entries=new LinkedHashMap<>();
    private static final long TTL=120000;
    static synchronized JSONObject list(Context context)throws Exception {
        MediaSessionManager manager=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<Session> sessions=new ArrayList<>();
        try {
            for(MediaController controller:manager.getActiveSessions(new ComponentName(context,MailNotificationService.class))) {
                sessions.add(new Session(){
                    public String app(){try{return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(controller.getPackageName(),0)).toString();}catch(Exception e){return controller.getPackageName();}}
                    public String identity(){MediaMetadata m=controller.getMetadata();return controller.getSessionToken().toString()+"|"+(m==null?"":String.valueOf(m.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))+"|"+String.valueOf(m.getText(MediaMetadata.METADATA_KEY_TITLE))+"|"+m.getLong(MediaMetadata.METADATA_KEY_DURATION));}
                    public PlaybackState state(){return controller.getPlaybackState();}
                    public long duration(){MediaMetadata m=controller.getMetadata();return m==null?-1:m.getLong(MediaMetadata.METADATA_KEY_DURATION);}
                    public void seek(long position){controller.getTransportControls().seekTo(position);}
                });
            }
            return listSessions(sessions,SystemClock.elapsedRealtime()).put("device",device(context));
        } catch(SecurityException denied){return result(false,"permission","再生元のロキで通知へのアクセスを許可してください。").put("sessions",new JSONArray());}
    }
    static synchronized JSONObject listSessions(List<Session> sessions,long now)throws Exception {
        entries.entrySet().removeIf(e->now-e.getValue().issued>TTL);
        JSONArray array=new JSONArray();
        for(Session session:sessions){
            if(array.length()>=16)break;
            String id=UUID.randomUUID().toString();Entry entry=new Entry(session,now);entries.put(id,entry);
            PlaybackState state=session.state();long sampled=SystemClock.elapsedRealtime();boolean supported=seekable(state,session.duration(),sampled);
            array.put(new JSONObject().put("id",id).put("app",session.app()).put("seek",supported)
                .put("reason",supported?"30秒送り・戻し対応":unavailableReason(state,session.duration(),sampled)));
        }
        while(entries.size()>64)entries.remove(entries.keySet().iterator().next());
        return result(true,"ready",array.length()==0?"再生元のアプリで音声を再生してから更新してください。":"この端末の再生アプリを選んでください。").put("sessions",array);
    }
    static String unavailableReason(PlaybackState state,long duration,long now){
        if(state==null)return "再生状態を取得できません。再生元で再生してから更新してください。";
        if((state.getActions()&PlaybackState.ACTION_SEEK_TO)==0)return "このアプリは現在、外部からの位置変更を許可していません。";
        if(state.getPosition()<0)return "再生位置を取得できません。";
        if(duration<=0)return "再生時間を取得できません。ライブ放送などは操作できません。";
        if(state.getState()!=PlaybackState.STATE_PAUSED&&state.getState()!=PlaybackState.STATE_PLAYING)return "読み込み中・停止中です。再生元で再生してから更新してください。";
        if(state.getState()==PlaybackState.STATE_PLAYING&&state.getPlaybackSpeed()!=1f)return "再生速度を確認できません。通常速度で再生してから更新してください。";
        return "再生位置の更新待ちです。再生元で再生・一時停止してから更新してください。";
    }
    static boolean seekable(PlaybackState state,long duration,long now){
        return state!=null&&(state.getActions()&PlaybackState.ACTION_SEEK_TO)!=0
            &&state.getPosition()>=0&&duration>0&&state.getPosition()<=duration
            &&(state.getState()==PlaybackState.STATE_PAUSED||state.getState()==PlaybackState.STATE_PLAYING)
            &&(state.getState()!=PlaybackState.STATE_PLAYING||(state.getLastPositionUpdateTime()>0
                &&now>=state.getLastPositionUpdateTime()&&now-state.getLastPositionUpdateTime()<=duration-state.getPosition()
                &&state.getPlaybackSpeed()==1f));
    }
    static long position(PlaybackState state,long now,long duration){
        long position=state.getPosition();if(state.getState()==PlaybackState.STATE_PLAYING)position+=Math.max(0,now-state.getLastPositionUpdateTime());
        return Math.max(0,Math.min(duration,position));
    }
    static long target(long position,long delta,long duration){return delta>0?position+Math.min(delta,duration-position):Math.max(0,position+delta);}
    static JSONObject result(boolean ok,String status,String message)throws JSONException{return new JSONObject().put("ok",ok).put("status",status).put("message",message);}

    /** Persist an at-most-once receipt BEFORE dispatch. Retries never seek another 30 seconds. */
    static synchronized JSONObject execute(Context context,JSONObject request)throws Exception {
        String id=request.optString("requestId"),sessionId=request.optString("session");long delta=request.optLong("delta");
        long created=request.optLong("createdAt"),wall=System.currentTimeMillis();
        if(!id.matches("[a-f0-9-]{36}")||(delta!=30000&&delta!=-30000)||created>wall+5000||wall-created>30000)
            return result(false,"invalid","操作の有効期限が切れました。再生アプリを選び直してください。");
        android.content.SharedPreferences prefs=context.getSharedPreferences("media_control_receipts",Context.MODE_PRIVATE);
        JSONObject receipts;try{receipts=new JSONObject(prefs.getString("receipts","{}"));}catch(Exception e){return result(false,"storage","操作履歴を確認できません。送信していません。");}
        if(receipts.has(id))return receipts.getJSONObject(id).getJSONObject("result");
        Iterator<String> keys=receipts.keys();List<String> remove=new ArrayList<>();while(keys.hasNext()){String key=keys.next();if(wall-receipts.getJSONObject(key).optLong("at")>120000)remove.add(key);}for(String key:remove)receipts.remove(key);
        if(receipts.length()>=64)return result(false,"busy","操作が多すぎます。しばらく待ってください。");
        Entry entry=entries.get(sessionId);long now=SystemClock.elapsedRealtime();
        if(entry==null||now-entry.issued>TTL||!entry.identity.equals(entry.session.identity()))return result(false,"changed","再生内容が変わりました。再生アプリを選び直してください。");
        context.getSharedPreferences("media_control_receipts",0).edit().putString("lastTarget",device(context)+" / "+entry.session.app()).apply();
        Session session=entry.session;PlaybackState before=session.state();long duration=session.duration();now=SystemClock.elapsedRealtime();
        if(!seekable(before,duration,now))return result(false,"unsupported","この再生状態では30秒操作に対応していません。アプリの再生画面で確認してください。");
        long start=position(before,now,duration),destination=target(start,delta,duration);
        if(delta>0&&before.getState()==PlaybackState.STATE_PLAYING&&destination-start<5000)return result(false,"limit","曲の末尾に近いため、再生元のアプリで位置を調整してください。");
        if(Math.abs(destination-start)<1000)return result(false,"limit","再生位置の端に達しています。");
        JSONObject pending=result(false,"unconfirmed","操作は送信済みですが、位置の変更を確認できません。同じ要求を再実行しません。");
        JSONObject receipt=new JSONObject().put("at",wall).put("result",pending);receipts.put(id,receipt);
        if(!prefs.edit().putString("receipts",receipts.toString()).commit())return result(false,"storage","操作履歴を保存できません。送信していません。");
        JSONObject answer=pending;
        try {
            long sent=SystemClock.elapsedRealtime();session.seek(destination);
            while(SystemClock.elapsedRealtime()-sent<2200){
                SystemClock.sleep(100);PlaybackState after=session.state();long check=SystemClock.elapsedRealtime();
                if(!entry.identity.equals(session.identity())){answer=result(false,"changed","再生内容が変わったため、位置変更を確認できません。");break;}
                if(after==null||(after.getLastPositionUpdateTime()<sent||after.getLastPositionUpdateTime()<before.getLastPositionUpdateTime()))continue;
                long expected=destination+(after.getState()==PlaybackState.STATE_PLAYING?check-sent:0);
                if(Math.abs(position(after,check,duration)-Math.min(duration,expected))<=2000
                    &&(delta>0?position(after,check,duration)>start+500:position(after,check,duration)<start-500)){
                    answer=result(true,"confirmed",(Math.abs(destination-start)/1000)+"秒"+(delta>0?"先へ移動しました。":"戻しました。"))
                        .put("requestedDelta",delta).put("actualDelta",destination-start);break;
                }
            }
        }catch(Exception failure){answer=result(false,"unconfirmed","再生アプリの応答を確認できません。同じ要求を再実行しません。");}
        receipt.put("result",answer);prefs.edit().putString("receipts",receipts.toString()).commit();return answer;
    }
    private LocalMediaControl(){}
}
