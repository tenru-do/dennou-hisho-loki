package com.example.rokidgeminisecretary;
/** Distinct accurate fixes latch forward progress; noisy/backwards fixes cannot revive a passed turn. */
public final class NavigationProgressLatch {
 private String identity="";private double progress=Double.NaN;private long last;
 public synchronized double accept(String route,long now,long sample,double accuracy,double candidate){
  if(!route.equals(identity)){identity=route;progress=Double.NaN;last=0;}
  if(!Double.isFinite(candidate)||candidate<0||candidate>1)return progress;
  if(!NavigationTurnFix.fresh(now,sample,accuracy)||accuracy>25||sample<=last)return progress;
  // A long observation gap invalidates progress; the next good fix must reacquire it.
  if(last>0&&sample-last>30000)progress=Double.NaN;
  last=sample;if(!Double.isFinite(progress)||candidate>progress)progress=candidate;
  return progress;
 }
}
