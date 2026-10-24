package com.example.rokidkeyboardbridge;
/** Equal GPS samples need no repeated map-camera calculation. New samples always propagate. */
public final class NavigationPoseGate {
 private boolean present;private double lat,lng;private float bearing,speed,accuracy;private long sample;
 public synchronized boolean changed(double lat,double lng,float bearing,float speed,float accuracy,long sample){boolean equal=present&&Double.compare(this.lat,lat)==0&&Double.compare(this.lng,lng)==0&&Float.compare(this.bearing,bearing)==0&&Float.compare(this.speed,speed)==0&&Float.compare(this.accuracy,accuracy)==0&&this.sample==sample;this.present=true;this.lat=lat;this.lng=lng;this.bearing=bearing;this.speed=speed;this.accuracy=accuracy;this.sample=sample;return !equal;}
}
