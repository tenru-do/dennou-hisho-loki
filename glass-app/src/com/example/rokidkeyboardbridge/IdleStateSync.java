package com.example.rokidkeyboardbridge;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
/** Coalesce only status snapshots. Commands and transfer receipts never use this gate. */
final class IdleStateSync {
 private byte[] deliveredHash;
 private long deliveredAt=-1;
 private static byte[] hash(String payload){try{return MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));}catch(Exception unavailable){throw new IllegalStateException(unavailable);}}
 synchronized boolean shouldSend(String payload,long now,long heartbeatMs){
  return deliveredAt<0||now<deliveredAt||now-deliveredAt>=heartbeatMs||!Arrays.equals(deliveredHash,hash(payload));
 }
 synchronized void delivered(String payload,long now){deliveredHash=hash(payload);deliveredAt=now;}
}
