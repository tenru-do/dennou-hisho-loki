package com.example.rokidgeminisecretary;
import org.json.*;
/** Positional source types only; no filenames, inferred MIME types or content extraction. */
final class ImageAttachmentKinds {
 static JSONArray normalize(JSONArray source,int count)throws Exception{
  if(count<1||count>6||source!=null&&source.length()!=count)throw new IllegalArgumentException("Attachment kind count mismatch");
  JSONArray result=new JSONArray();int videos=0;for(int i=0;i<count;i++){String kind=source==null?"unknown":source.getString(i);if(!kind.equals("image")&&!kind.equals("video")&&!kind.equals("unknown"))throw new IllegalArgumentException("Invalid attachment kind");if(kind.equals("video"))videos++;result.put(kind);}
  if(videos>3)throw new IllegalArgumentException("Too many video attachments");return result;
 }
 private ImageAttachmentKinds(){}
}
