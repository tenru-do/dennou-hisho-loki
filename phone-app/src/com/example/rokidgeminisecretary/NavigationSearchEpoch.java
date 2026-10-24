package com.example.rokidgeminisecretary;
public final class NavigationSearchEpoch {
 private long sequence;
 public synchronized long begin(){return ++sequence;}
 public synchronized void cancel(){sequence++;}
 public synchronized void require(long token)throws Exception{if(token!=sequence||Thread.currentThread().isInterrupted())throw new InterruptedException("検索の選択が更新されました。最後の操作を確認してください");}
}
