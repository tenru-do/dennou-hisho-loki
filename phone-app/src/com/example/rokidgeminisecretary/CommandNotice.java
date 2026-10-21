package com.example.rokidgeminisecretary;
/** No payload or credentials retained. A bounded wait releases the monitor on sleep. */
final class CommandNotice {
 private static long revision;
 static synchronized long revision(){return revision;}
 static synchronized void changed(){revision++;CommandNotice.class.notifyAll();}
 static synchronized void await(long seen)throws InterruptedException{
  long end=android.os.SystemClock.elapsedRealtime()+1000;
  while(revision==seen){long left=end-android.os.SystemClock.elapsedRealtime();if(left<=0)return;CommandNotice.class.wait(left);}
 }
 private CommandNotice(){}
}
