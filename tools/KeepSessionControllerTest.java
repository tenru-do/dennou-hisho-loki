package com.example.rokidkeyboardbridge;
import java.util.concurrent.atomic.*;import java.util.function.*;
public final class KeepSessionControllerTest {
 static int passed;static void check(boolean condition,String message){passed++;if(!condition)throw new AssertionError(message);}
 static final class Fake implements KeepSessionController.Backend {
  boolean alive=true,failCapture,failClose;int captures,closes;Runnable during;
  public byte[] capture(BooleanSupplier cancelled)throws Exception{captures++;if(during!=null)during.run();if(cancelled.getAsBoolean())throw new InterruptedException();if(failCapture)throw new IllegalStateException();return new byte[]{(byte)255,(byte)216,1,2};}
  public boolean alive(){return alive;}
  public void close()throws Exception{closes++;alive=false;if(failClose)throw new IllegalStateException();}
 }
 public static void main(String[]args)throws Exception{
  CameraPriority priority=new CameraPriority();AtomicLong clock=new AtomicLong();AtomicInteger opens=new AtomicInteger();Fake[] latest={null};
  KeepSessionController c=new KeepSessionController(priority,clock::get,stop->{opens.incrementAndGet();return latest[0]=new Fake();});
  for(int i=0;i<559;i++)check(c.capture(()->false).length==4,"captured JPEG returned");
  check(opens.get()==1&&latest[0].captures==559,"559 photos reuse one backend rather than 559 opens");
  Fake first=latest[0];try(CameraPriority.ReadRequest read=priority.read()){
   c.yieldIfNeeded(()->false);check(first.closes==1&&!c.ownsCamera(),"READ releases retained backend during idle");
   boolean blocked=false;try{c.capture(()->false);}catch(InterruptedException e){blocked=true;}check(blocked&&opens.get()==1,"READ owns priority; KEEP cannot reopen");
  }
  c.capture(()->false);check(opens.get()==2,"one fresh session after READ ends");
  priority.externalUnavailable();c.yieldIfNeeded(()->false);check(latest[0].closes==1,"external device releases KEEP");
  boolean blocked=false;try{c.capture(()->false);}catch(InterruptedException e){blocked=true;}check(blocked,"external busy does not retry open");
  priority.externalAvailable(clock.get());clock.set(1600);c.capture(()->false);check(opens.get()==3,"external quiet window then reacquire once");
  c.close();c.close();check(latest[0].closes==1,"stop releases once even on repeated close");
  blocked=false;try{c.capture(()->false);}catch(InterruptedException e){blocked=true;}check(blocked&&opens.get()==3,"stopped controller never restarts itself");
  CameraPriority p2=new CameraPriority();Fake inFlight=new Fake();KeepSessionController c2=new KeepSessionController(p2,clock::get,stop->inFlight);
  CameraPriority.ReadRequest[] pending={null};inFlight.during=()->pending[0]=p2.read();blocked=false;
  try{c2.capture(()->false);}catch(InterruptedException e){blocked=true;}finally{if(pending[0]!=null)pending[0].close();}
  check(blocked&&inFlight.closes==1,"READ arriving during capture cancels and releases");c2.close();
  Fake fault=new Fake();fault.failCapture=true;KeepSessionController c3=new KeepSessionController(new CameraPriority(),clock::get,stop->fault);
  boolean failed=false;try{c3.capture(()->false);}catch(IllegalStateException e){failed=true;}check(failed&&fault.closes==1,"capture exception closes backend");c3.close();
  Fake dead=new Fake();KeepSessionController c4=new KeepSessionController(new CameraPriority(),clock::get,stop->dead);c4.capture(()->false);dead.alive=false;
  failed=false;try{c4.yieldIfNeeded(()->false);}catch(IllegalStateException e){failed=true;}check(failed&&dead.closes==1,"unexpected device loss is terminal instead of reopen loop");
  Fake closeFault=new Fake();closeFault.failClose=true;KeepSessionController c5=new KeepSessionController(new CameraPriority(),clock::get,stop->closeFault);c5.capture(()->false);
  failed=false;try{c5.release();}catch(IllegalStateException e){failed=true;}check(failed,"unconfirmed close surfaced");
  blocked=false;try{c5.capture(()->false);}catch(InterruptedException e){blocked=true;}check(blocked,"unconfirmed close cannot create another backend");
  AtomicBoolean stop=new AtomicBoolean(true);AtomicInteger cancelledOpens=new AtomicInteger();KeepSessionController c6=new KeepSessionController(new CameraPriority(),clock::get,cancel->{cancelledOpens.incrementAndGet();return new Fake();});
  blocked=false;try{c6.capture(stop::get);}catch(InterruptedException e){blocked=true;}check(blocked&&cancelledOpens.get()==0,"cancel before start never opens camera");c6.close();
  for(int run=0;run<1000;run++){
   AtomicInteger lifecycleOpens=new AtomicInteger();Fake reusable=new Fake();KeepSessionController repeated=new KeepSessionController(new CameraPriority(),clock::get,cancel->{lifecycleOpens.incrementAndGet();return reusable;});
   for(int n=0;n<5;n++)repeated.capture(()->false);repeated.close();
   check(lifecycleOpens.get()==1&&reusable.captures==5&&reusable.closes==1,"start/5 captures/stop owns one bounded backend");
  }
  System.out.println("PASS "+passed+" persistent KEEP session, reuse, READ preemption, external camera, cancellation and lifecycle checks; no real camera/network");
 }
}
