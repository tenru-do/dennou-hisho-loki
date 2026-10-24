package com.example.rokidkeyboardbridge;
import java.util.concurrent.*;
/** Two bounded workers, one replaceable pending task. Old provider calls may run until their timeout. */
public final class NavigationLatestJob {
 public interface Done<T>{void accept(long token,T value,Exception error);}
 private final ThreadPoolExecutor pool=new ThreadPoolExecutor(2,2,10,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"NavigationLatest");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());private Future<?> latest;private long generation;
 public synchronized long submit(Callable<?> work,Done<Object> done){cancel();long token=generation;latest=pool.submit(()->{Object value=null;Exception failure=null;try{if(!current(token))return;value=work.call();}catch(Exception e){failure=e;}if(current(token))done.accept(token,value,failure);});return token;}
 public synchronized void cancel(){generation++;if(latest!=null)latest.cancel(true);pool.purge();}
 public synchronized boolean current(long token){return generation==token;}
 public int pending(){return pool.getQueue().size();}public int active(){return pool.getActiveCount();}
 public synchronized void close(){cancel();pool.shutdownNow();}
}
