package com.example.rokidkeyboardbridge;
public final class FullBodyReactionTest {
 public static void main(String[]a){
  FullBodyReactionPolicy p=new FullBodyReactionPolicy();p.committed(10000);
  if(p.target(0,0,0,10010,false)!=0||p.target(0,0,0,11000,false)!=0||p.target(0,0,0,11900,false)!=168)throw new AssertionError("Delayed rest");
  if(p.target(0,1,168,12000,false)!=168||p.target(0,2,168,12100,false)!=168)throw new AssertionError("Stable conversational posture");
  p.target(7,2,168,13000,false);if(p.target(7,2,168,13400,true)!=168||p.target(7,2,168,13400,false)!=7)throw new AssertionError("Thought transition");
  p.reset();if(p.target(0,0,-1,1,false)!=168)throw new AssertionError("Initial display");
  System.out.println("PASS existing emotion mapping, delayed rest, stable seat, transition completion, reset");
 }
}
