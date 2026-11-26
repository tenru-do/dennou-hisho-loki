package com.example.rokidgeminisecretary;
import java.io.IOException;
/** Null contexts ensure the retired entry points fail before any files, permissions or network. */
public final class MeetingRecordingRemovedEntryTest {
 interface Operation{void run()throws Exception;}
 static int checks;static void rejected(Operation operation)throws Exception{try{operation.run();throw new AssertionError("Retired operation accepted");}catch(IOException expected){if(!MeetingFeatureRetired.MESSAGE.equals(expected.getMessage()))throw new AssertionError("Wrong retirement error");checks++;}}
 public static void main(String[]args)throws Exception{
  rejected(()->MeetingControl.queue(null,null));
  rejected(()->MeetingAudioStore.accept(null,null));
  rejected(()->MeetingAudioStore.analyze(null,null));
  rejected(()->MeetingMediaImport.begin(null,null));
  rejected(()->MeetingMediaImport.chunk(null,null));
  rejected(()->MeetingMediaImport.finish(null,null));
  if(MeetingAudioStore.busy()||!MeetingMediaImport.active.isEmpty())throw new AssertionError("Background work started");
  System.out.println("PASS "+checks+" retired recording/import/AI entry points reject before accessing context or data");
 }
}
