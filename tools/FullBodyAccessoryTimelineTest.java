package com.example.rokidkeyboardbridge;
public final class FullBodyAccessoryTimelineTest {
 public static void main(String[]args){FullBodyAccessoryTimeline t=new FullBodyAccessoryTimeline();
  t.update(390000,2,false,true);if(t.moving()||t.wearing)throw new AssertionError("Interrupts speech");
  t.update(390010,0,true,true);if(t.moving())throw new AssertionError("Interrupts pose blend");
  t.update(390020,0,false,false);if(t.moving())throw new AssertionError("Changes incompatible pose");
  t.update(390030,0,false,true);if(!t.moving()||!t.target)throw new AssertionError("No attachment");
  t.update(390430,0,false,true);long before=t.elapsed(390430);
  t.update(391430,2,false,true);if(t.elapsed(391430)!=before)throw new AssertionError("Speech pause drifts");
  t.update(392430,0,true,true);if(t.elapsed(392430)!=before)throw new AssertionError("Blend pause drifts");
  for(int i=1;i<=6;i++)if(t.pose(i,4)!=328+i)throw new AssertionError("Attachment direction");
  t.update(395030,0,false,true);if(t.moving()||!t.wearing)throw new AssertionError("Attachment not committed");
  t.update(480000,0,false,true);if(!t.moving()||t.target)throw new AssertionError("No removal");
  for(int i=1;i<=6;i++)if(t.pose(i,4)!=335-i)throw new AssertionError("Removal direction");
  t.update(482450,0,false,true);if(t.moving()||t.wearing)throw new AssertionError("Removal not committed");
  if(t.pose(0,13)!=13||t.pose(7,13)!=13)throw new AssertionError("Endpoint stance changed");
  System.out.println("PASS accessory quiet-pose guards, speech/blend pause, six forward/reverse hand frames and wearing-state commits");
 }
}
