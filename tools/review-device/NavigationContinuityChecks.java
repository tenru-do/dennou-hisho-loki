package com.example.rokidgeminisecretary;
import org.json.*;import android.os.*;import java.util.concurrent.*;
public final class NavigationContinuityChecks {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("navigation continuity "+n);}
 public static int run()throws Exception{n=0;
 NavigationProgressLatch latch=new NavigationProgressLatch();ok(Double.isNaN(latch.accept("a",1000,1000,60,.2)));ok(latch.accept("a",2000,2000,5,.2)==.2);ok(latch.accept("a",3000,3000,5,.3)==.3);ok(latch.accept("a",4000,4000,5,.28)==.3);ok(latch.accept("a",4000,4000,5,.6)==.3);ok(latch.accept("a",5000,1000,5,.8)==.3);ok(latch.accept("a",5000,5000,80,.8)==.3);ok(latch.accept("a",5000,5000,5,Double.NaN)==.3);ok(latch.accept("a",5000,5000,5,2)==.3);ok(latch.accept("a",5000,5000,5,.4)==.4);ok(latch.accept("a",36000,36000,5,.1)==.1);ok(Double.isNaN(latch.accept("b",37000,37000,50,.7)));ok(latch.accept("b",38000,38000,5,.15)==.15);
 NavigationGeometryCache geometry=new NavigationGeometryCache("[[0,0],[0,0.001],[0,0.002]]","[]");ok(geometry.matches("[[0,0],[0,0.001],[0,0.002]]","[]"));ok(!geometry.matches("[]","[]"));ok(Math.abs(geometry.distance(0,1)-111.195)<.1);ok(Math.abs(geometry.distance(0,2)-222.39)<.1);ok(geometry.distance(2,0)==0);ok(geometry.distance(-1,99)==geometry.distance(0,2));ok(new NavigationGeometryCache("[]","[]").distance(0,1)==0);
 NavigationLatestJob job=new NavigationLatestJob();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),ui=new CountDownLatch(1);try{job.submit(()->{entered.countDown();release.await(5,TimeUnit.SECONDS);return 1;},(t,v,e)->{});ok(entered.await(3,TimeUnit.SECONDS));new Handler(Looper.getMainLooper()).post(()->ui.countDown());ok(ui.await(2,TimeUnit.SECONDS));ok(release.getCount()==1);}finally{release.countDown();job.close();}
 return n;}
}
