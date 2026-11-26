package com.example.rokidkeyboardbridge;
import java.util.function.*;
/** One backend per uninterrupted KEEP run, with cooperative READ/external-camera yielding. */
final class KeepSessionController implements AutoCloseable {
 interface Backend {byte[] capture(BooleanSupplier cancelled)throws Exception;boolean alive();void close()throws Exception;}
 interface Factory {Backend open(BooleanSupplier cancelled)throws Exception;}
 private final CameraPriority priority;private final LongSupplier clock;private final Factory factory;
 private volatile Backend backend;private long ticket=-1;private boolean closed;
 KeepSessionController(CameraPriority priority,LongSupplier clock,Factory factory){this.priority=priority;this.clock=clock;this.factory=factory;}
 boolean ownsCamera(){return backend!=null&&backend.alive();}
 void release()throws Exception{Backend old=backend;backend=null;ticket=-1;if(old!=null)try{old.close();}catch(Exception failure){closed=true;throw failure;}}
 void yieldIfNeeded(BooleanSupplier cancelled)throws Exception{
  if(backend==null)return;boolean yielding=cancelled.getAsBoolean()||priority.yieldKeep(ticket,clock.getAsLong());
  if(!backend.alive()){release();if(!yielding){closed=true;throw new IllegalStateException("KEEP camera ended unexpectedly; no automatic reopen");}}
  else if(yielding)release();
 }
 byte[] capture(BooleanSupplier cancelled)throws Exception{
  if(closed||cancelled.getAsBoolean())throw new InterruptedException("KEEP session stopped");
  yieldIfNeeded(cancelled);
  if(closed||cancelled.getAsBoolean())throw new InterruptedException("KEEP session stopped");
  long next=priority.keepTicket(clock.getAsLong());if(next<0)throw new InterruptedException("KEEP yields camera");
  if(backend==null){ticket=next;final long openingTicket=ticket;backend=factory.open(()->cancelled.getAsBoolean()||priority.yieldKeep(openingTicket,clock.getAsLong()));}
  final long currentTicket=ticket;
  try{return backend.capture(()->cancelled.getAsBoolean()||priority.yieldKeep(currentTicket,clock.getAsLong()));}
  catch(Exception failure){try{release();}catch(Exception closeFailure){failure.addSuppressed(closeFailure);closed=true;}throw failure;}
 }
 @Override public void close()throws Exception{closed=true;release();}
}
