package com.example.rokidkeyboardbridge;
/** Fast, smooth shoulder/mouth breathing loop for explicitly selected dramatic scenes. */
final class FullBodyDramaticBreathPolicy {
 static final long CYCLE_MS=900L;
 static float exhaleWeight(long elapsed){
  long phase=Math.max(0L,elapsed)%CYCLE_MS;
  return (float)(0.5-0.5*Math.cos(2*Math.PI*phase/CYCLE_MS));
 }
}
