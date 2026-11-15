package com.example.rokidkeyboardbridge;
import java.util.concurrent.*;
public final class CameraPriorityTest {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 public static void main(String[] args)throws Exception{
  CameraPriority p=new CameraPriority();long keep=p.keepTicket(1000);check(keep>=0);check(!p.yieldKeep(keep,1000));
  CameraPriority.ReadRequest a=p.read();check(p.yieldKeep(keep,1000));check(p.keepTicket(1000)<0);
  CameraPriority.ReadRequest b=p.read();a.close();a.close();check(p.keepTicket(1000)<0);b.close();check(p.keepTicket(1000)>=0);check(p.yieldKeep(keep,1000));
  p.externalUnavailable();check(p.keepTicket(5000)<0);p.externalAvailable(5000);check(p.keepTicket(6499)<0);check(p.keepTicket(6500)>=0);
  keep=p.keepTicket(7000);p.defer(7000,5000);check(p.yieldKeep(keep,7000));check(p.keepTicket(11999)<0);check(p.keepTicket(12000)>=0);
  p.externalUnavailable();p.resetExternal();check(p.keepTicket(13000)>=0);
  Semaphore camera=CaptureLease.CAMERA;long ticket=p.keepTicket(14000);
  CaptureLease owned=CaptureLease.acquire(camera,()->false);CountDownLatch requested=new CountDownLatch(1),acquired=new CountDownLatch(1);
  Thread read=new Thread(()->{try(CameraPriority.ReadRequest req=p.read()){requested.countDown();try(CaptureLease lease=CaptureLease.acquire(camera,()->false)){acquired.countDown();}}catch(Exception e){throw new RuntimeException(e);}});
  read.start();check(requested.await(1,TimeUnit.SECONDS));check(p.yieldKeep(ticket,14000));check(acquired.getCount()==1);owned.close();owned.close();check(acquired.await(1,TimeUnit.SECONDS));read.join(1000);check(!read.isAlive());check(camera.availablePermits()==1);check(p.keepTicket(15000)>=0);
  try{CaptureLease.acquire(camera,()->true);throw new AssertionError();}catch(InterruptedException expected){check(camera.availablePermits()==1);}
  System.out.println("Camera priority PASS: "+n+" checks; no camera/audio/API calls");
 }
}
