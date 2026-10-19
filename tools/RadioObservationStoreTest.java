package com.example.rokidgeminisecretary;
import org.json.*;import java.util.Base64;
public class RadioObservationStoreTest {
 static int checks;static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static JSONObject payload(String id,int bytes)throws Exception{return new JSONObject().put("requestId",id).put("revision",7).put("audio",Base64.getEncoder().encodeToString(new byte[bytes]));}
 public static void main(String[] a)throws Exception{
 RadioObservationStore.begin("long",7);JSONObject p=payload("long",384044);ok(RadioObservationStore.accept(p,7,true),"12second WAV accepted");ok(!RadioObservationStore.accept(p,7,true),"duplicate rejected");ok(RadioObservationStore.take("long")==p,"same complete observation consumed");ok(RadioObservationStore.take("long")==null,"no replay");
 RadioObservationStore.begin("new",7);ok(!RadioObservationStore.accept(payload("new",384048),7,true),"oversized audio rejected");ok(!RadioObservationStore.accept(payload("old",100),7,true),"old session rejected");ok(!RadioObservationStore.accept(payload("new",100),8,true),"revision rejected");ok(!RadioObservationStore.accept(payload("new",100),7,false),"stopped mode rejected");RadioObservationStore.clear();ok(!RadioObservationStore.accept(payload("new",100),7,true),"cancel clears acceptance");
 System.out.println("PASS observation transport "+checks+" checks; full WAV, bounded size, stale and cancelled result suppression");
 }
}
