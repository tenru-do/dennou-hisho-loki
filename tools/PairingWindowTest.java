package com.example.rokidgeminisecretary;
public final class PairingWindowTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  PairingWindow w=new PairingWindow();check(!w.claim(0,"123456"),"closed by default");w.open(1000);check(!w.claim(1001,""),"missing code");check(!w.claim(1001,"1234567"),"invalid code");check(w.claim(1002,"123456"),"valid request still admitted after invalid requests");check(!w.claim(1003,"123456"),"one shot");w.open(1000);check(!w.claim(61000,"123456"),"strict expiry");w.open(100000);
  java.util.concurrent.atomic.AtomicInteger accepted=new java.util.concurrent.atomic.AtomicInteger();java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);Thread[] threads=new Thread[24];for(int i=0;i<threads.length;i++){threads[i]=new Thread(()->{try{start.await();if(w.claim(100001,"234567"))accepted.incrementAndGet();}catch(InterruptedException e){throw new RuntimeException(e);}});threads[i].start();}start.countDown();for(Thread t:threads)t.join();check(accepted.get()==1,"concurrent one-shot admission");System.out.println("PASS closed/invalid/expired pairing and 24 concurrent requests admitted exactly once; phone approval remains required");
 }
}
