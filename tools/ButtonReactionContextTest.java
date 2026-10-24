package com.example.rokidkeyboardbridge;
public final class ButtonReactionContextTest {
 public static void main(String[] args){
  String[] yes={"頭を撫でる","肩をたたいてあげる","手を握る","手をつないでいる","身体に触れている","触れられている","くすぐる","抱きしめる"};
  String[] no={"触っていない","触ってない","触れていない","触るな","触らないで","触れないで","触れるのはやめて","会話を終了してから触れる","話題を変えて触れる","天気はどう？"};
  for(String text:yes)if(!ButtonReactionContext.touching(text))throw new AssertionError("Missing contact cue");
  for(String text:no)if(ButtonReactionContext.touching(text))throw new AssertionError("Unwanted contact cue");
  if(ButtonReactionContext.touching(null))throw new AssertionError("Null context");
  System.out.println("PASS 19 contact, ordinary and stop-context cases");
 }
}
