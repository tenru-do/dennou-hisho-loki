package com.example.rokidgeminisecretary;
import java.util.ArrayList;
import org.json.JSONObject;
/** Only ID-capable clients may bypass a held conversation. Legacy ACK still means head. */
final class CodexNoticeQueue {
    static final String PREFIX="__CODEX_NOTIFY__:";
    static String id(String wire){
        if(wire==null||!wire.startsWith(PREFIX))return "";
        try{JSONObject row=new JSONObject(wire.substring(PREFIX.length()));String id=row.optString("id"),kind=row.optString("kind");
            return id.matches("[a-f0-9]{64}")&&(kind.equals("completed")||kind.equals("question")||kind.equals("status"))&&row.optLong("issuedAt")>0?id:"";
        }catch(Exception invalid){return "";}
    }
    static boolean removeMatching(ArrayList<String> queue,String id){
        if(id==null||!id.matches("[a-f0-9]{64}"))return false;
        for(int i=0;i<queue.size();i++)if(id.equals(id(queue.get(i)))){queue.remove(i);return true;}
        return false;
    }
}
