package com.example.rokidkeyboardbridge;
/** Polls are hints, never inferred position updates. At most one request remains in flight. */
public final class NavigationPollCadence {
 public static long delay(boolean active,boolean fresh,boolean recovering,String mode,String distance,double speed){if(!active)return 8000;if(recovering||!fresh)return 1500;double metres=NavigationTurnCue.metres(distance);if(!(mode.equals("walking")||mode.equals("bicycling")||mode.equals("driving"))||metres<0)return 2000;double near=mode.equals("driving")?Math.max(250,Math.min(600,Double.isFinite(speed)&&speed>=0?speed*25:250)):mode.equals("bicycling")?Math.max(100,Math.min(200,Double.isFinite(speed)&&speed>=0?speed*20:100)):60;if(metres<=near)return 750;if(metres>(mode.equals("driving")?2000:mode.equals("bicycling")?1000:500))return 3000;return 2000;}
 private NavigationPollCadence(){}
}
