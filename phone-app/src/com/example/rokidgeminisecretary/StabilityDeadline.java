package com.example.rokidgeminisecretary;
import java.util.UUID;
/** Monotonic total deadline. Does not retry or infer remote completion. */
public final class StabilityDeadline {
 public static final int NONE=0,START=1,END=2,EXPIRED=3;
 private final long limit;private long generation,since;private boolean active,expired;private String token="";
 public StabilityDeadline(long limit){if(limit<1)throw new IllegalArgumentException("positive deadline required");this.limit=limit;}
 public synchronized int observe(boolean running,long version,long now){if(now<0)throw new IllegalArgumentException("negative monotonic time");if(!running){if(!active)return NONE;active=false;return END;}if(!active&&expired&&generation==version)return NONE;if(!active||generation!=version){active=true;expired=false;generation=version;since=now;token=UUID.randomUUID().toString();return START;}if(now<since){since=now;return NONE;}if(now-since>=limit){active=false;expired=true;return EXPIRED;}return NONE;}
 public synchronized String token(){return token;}public synchronized long elapsed(long now){return Math.max(0,now-since);}
}
