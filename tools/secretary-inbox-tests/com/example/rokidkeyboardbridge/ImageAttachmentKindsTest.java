package com.example.rokidkeyboardbridge;
import org.json.*;
public class ImageAttachmentKindsTest {
 static int n;static void ok(boolean b,String label){if(!b)throw new AssertionError(label);n++;}
 public static void main(String[]args)throws Exception{
  JSONArray input=new JSONArray().put("image").put("video").put("image");JSONArray result=ImageAttachmentKinds.normalize(input,3);ok(result.toString().equals(input.toString()),"positional order retained");result.put(0,"unknown");ok(input.getString(0).equals("image"),"does not mutate source");ok(ImageAttachmentKinds.normalize(null,2).getString(1).equals("unknown"),"legacy kinds not invented");
  boolean bad=false;try{ImageAttachmentKinds.normalize(input,2);}catch(IllegalArgumentException e){bad=true;}ok(bad,"count mismatch rejected");bad=false;try{ImageAttachmentKinds.normalize(new JSONArray().put("<script>"),1);}catch(IllegalArgumentException e){bad=true;}ok(bad,"arbitrary metadata rejected");bad=false;try{ImageAttachmentKinds.normalize(null,7);}catch(IllegalArgumentException e){bad=true;}ok(bad,"too many attachments rejected");bad=false;try{ImageAttachmentKinds.normalize(new JSONArray().put("video").put("video").put("video").put("video"),4);}catch(IllegalArgumentException e){bad=true;}ok(bad,"video limit validated");ok(ImageAttachmentKinds.normalize(new JSONArray().put("video").put("video").put("video"),3).length()==3,"three videos allowed");
  System.out.println("PASS attachment kind metadata "+n+" checks");
 }
}
