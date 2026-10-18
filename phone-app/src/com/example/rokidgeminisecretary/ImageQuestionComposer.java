package com.example.rokidgeminisecretary;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.UUID;

/** Image attachments beside the conversation. Selection never enqueues a question. */
final class ImageQuestionComposer {
 static final int PICK=1701,CAPTURE=1702;
 private final Activity host;private final Runnable changed;
 private final LinearLayout root,attachment;private final ImageView preview;private final TextView status;
 private final Button choose,camera,remove;
 private int imageCount=1;private boolean videoAttachment;private final java.util.ArrayList<String> attachmentKinds=new java.util.ArrayList<>();private final java.util.ArrayList<byte[]> attachmentImages=new java.util.ArrayList<>();
 private byte[] image;private Bitmap thumbnail;private boolean working,closed;
 private String draftId=UUID.randomUUID().toString(),cameraId="",requestId="",submittedQuestion="";
 ImageQuestionComposer(Activity host,Runnable changed){
  this.host=host;this.changed=changed;status=new TextView(host);root=new LinearLayout(host);root.setOrientation(LinearLayout.VERTICAL);
  LinearLayout actions=new LinearLayout(host);
  choose=new Button(host);choose.setText("画像・動画を選ぶ");choose.setOnClickListener(v->choose());actions.addView(choose,new LinearLayout.LayoutParams(0,-2,1));
  camera=new Button(host);camera.setText("撮影して添付");camera.setOnClickListener(v->capture());actions.addView(camera,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
  Button paste=new Button(host);paste.setText("画像を貼り付け");paste.setOnClickListener(v->{if(working)return;android.content.ClipboardManager cm=(android.content.ClipboardManager)host.getSystemService(Context.CLIPBOARD_SERVICE);ClipData clip=cm.getPrimaryClip();java.util.ArrayList<Uri> uris=new java.util.ArrayList<>();if(clip!=null)for(int i=0;i<Math.min(7,clip.getItemCount());i++){Uri u=clip.getItemAt(i).getUri();if(u!=null)uris.add(u);}if(uris.isEmpty())status.setText("画像をコピーしてから押してください。URLや文章は画像として読み込みません");else load(uris,"");});root.addView(paste);
  attachment=new LinearLayout(host);attachment.setOrientation(LinearLayout.HORIZONTAL);
  preview=new ImageView(host);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setContentDescription("送信前の添付画像");
  attachment.addView(preview,new LinearLayout.LayoutParams(0,dp(88),1));
  remove=new Button(host);remove.setText("画像を外す");remove.setOnClickListener(v->clear());attachment.addView(remove,new LinearLayout.LayoutParams(-2,-2));
  root.addView(attachment);attachment.setVisibility(View.GONE);
  status.setTextSize(12);status.setText("画像・動画は合計6件（動画3本まで、各2分・50MB、8場面・音声なし）。Googleフォトは写真選択のクラウド設定または共有から");root.addView(status);update();
 }
 View view(){return root;}
 boolean attached(){return image!=null;}
 boolean busy(){return working;}
 private int dp(int n){return Math.round(n*host.getResources().getDisplayMetrics().density);}
 private boolean live(){return !closed&&!host.isFinishing()&&!host.isDestroyed();}
 private void busy(boolean value){working=value;update();}
 private void update(){choose.setEnabled(!working);camera.setEnabled(!working);remove.setEnabled(!working);changed.run();}
 private Uri cameraUri(){return Uri.parse("content://"+host.getPackageName()+".image-camera/"+cameraId);}
 private File draft()throws IOException{
  if(!draftId.matches("[a-f0-9-]{36}"))throw new IOException("invalid draft");
  File dir=new File(host.getCacheDir(),"image-question-drafts");dir.mkdirs();File[] files=dir.listFiles();
  if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>ImageQuestions.TTL)f.delete();return new File(dir,draftId+".gcm");
 }
 private void choose(){if(working)return;try{busy(true);
  Intent intent;
  if(android.os.Build.VERSION.SDK_INT>=33)intent=new Intent(MediaStore.ACTION_PICK_IMAGES).putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,6);
  else intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"}).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addCategory(Intent.CATEGORY_OPENABLE);
  try{host.startActivityForResult(intent,PICK);}catch(android.content.ActivityNotFoundException e){host.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"}).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true).addCategory(Intent.CATEGORY_OPENABLE),PICK);}
 }catch(Exception e){status.setText("写真選択画面を開けません");busy(false);}}
 boolean share(Intent intent){
  try{return shareMedia(intent);}catch(RuntimeException e){status.setText("共有された添付を開けません。写真選択から読み込んでください");busy(false);return true;}
 }
 private boolean shareMedia(Intent intent){
  String action=intent==null?null:intent.getAction(),type=intent==null?null:intent.getType();
  if((!Intent.ACTION_SEND.equals(action)&&!Intent.ACTION_SEND_MULTIPLE.equals(action))||type==null||(!type.startsWith("image/")&&!type.startsWith("video/")))return false;
  if(working){status.setText("準備が終わってからもう一度共有してください");return true;}
  java.util.ArrayList<Uri> uris=new java.util.ArrayList<>();
  if(Intent.ACTION_SEND_MULTIPLE.equals(action)){java.util.ArrayList<Uri> shared=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(shared!=null){if(shared.size()>6){status.setText("画像は一度に6枚までです");intent.setAction(null);return true;}uris.addAll(shared);}}else{Uri uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);if(uri!=null)uris.add(uri);}
  if(uris.isEmpty()&&intent.getClipData()!=null)for(int i=0;i<Math.min(7,intent.getClipData().getItemCount());i++){Uri uri=intent.getClipData().getItemAt(i).getUri();if(uri!=null)uris.add(uri);}
  intent.setAction(null);load(uris,"");return true;
 }
 private void capture(){if(working)return;try{
  cameraId=UUID.randomUUID().toString();try(FileOutputStream out=new FileOutputStream(ImageQuestionCameraProvider.target(host,cameraId))){}
  Uri uri=cameraUri();Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,uri);
  intent.setClipData(ClipData.newRawUri("撮影先",uri));intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  busy(true);host.startActivityForResult(intent,CAPTURE);
 }catch(Exception e){cleanupCamera(cameraId);cameraId="";status.setText("カメラを開けません。画像を添付してください");busy(false);}}
 boolean result(int code,int result,Intent data){
  if(code!=PICK&&code!=CAPTURE)return false;
  final String captured=code==CAPTURE?cameraId:"";
  if(result!=Activity.RESULT_OK){cleanupCamera(captured);cameraId="";busy(false);return true;}
  java.util.ArrayList<Uri> uris=new java.util.ArrayList<>();
  if(code==CAPTURE)uris.add(cameraUri());else if(data!=null){if(data.getClipData()!=null)for(int i=0;i<Math.min(7,data.getClipData().getItemCount());i++){Uri u=data.getClipData().getItemAt(i).getUri();if(u!=null)uris.add(u);}else if(data.getData()!=null)uris.add(data.getData());}
  load(uris,captured);return true;
 }
 private void load(java.util.List<Uri> uris,String captured){
  busy(true);status.setText("添付を準備中…（画像・動画は合計6件／動画3本まで・各2分50MB）");
  new Thread(()->{try{
   java.util.ArrayList<byte[]> originals=new java.util.ArrayList<>(attachmentImages);java.util.ArrayList<String> kinds=new java.util.ArrayList<>(attachmentKinds);
   if(originals.isEmpty()&&image!=null){if(imageCount>1)throw new IOException("以前の一覧画像は追加できません。一度外して選び直してください");originals.add(image);kinds.add(videoAttachment?"video":"image");}
   if(originals.size()+uris.size()>6)throw new IOException("画像・動画は合計6件までです。添付済みは保持しています");
   for(Uri u:uris){String type=host.getContentResolver().getType(u);kinds.add(type!=null&&type.startsWith("video/")?"video":"image");}
   int videos=0;for(String kind:kinds)if(kind.equals("video"))videos++;if(videos>3)throw new IOException("動画は合計3本までです。添付済みは保持しています");final boolean video=videos>0;
   for(Uri u:uris)originals.add(MediaAttachmentSheet.prepare(host,java.util.Collections.singletonList(u)));byte[] next=MediaAttachmentSheet.combineImages(originals,kinds);
   org.json.JSONArray parts=new org.json.JSONArray(),kindArray=new org.json.JSONArray();for(byte[] part:originals)parts.put(android.util.Base64.encodeToString(part,android.util.Base64.NO_WRAP));for(String kind:kinds)kindArray.put(kind);
   byte[] draftData=new org.json.JSONObject().put("image",android.util.Base64.encodeToString(next,android.util.Base64.NO_WRAP)).put("parts",parts).put("kinds",kindArray).put("video",video).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
   java.nio.file.Files.write(draft().toPath(),DeviceSecrets.photos().encrypt(draftData));
   host.runOnUiThread(()->{if(!live())return;image=next;attachmentImages.clear();attachmentImages.addAll(originals);attachmentKinds.clear();attachmentKinds.addAll(kinds);videoAttachment=video;imageCount=originals.size();requestId="";submittedQuestion="";showImage();status.setText("添付"+imageCount+"件です。画像・動画を追加できます。動画は各8場面の抜粋のみ・音声なし。送信でGeminiへ画像を送ります");busy(false);});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("添付を読み込めません："+e.getMessage());busy(false);}});}
  finally{cleanupCamera(captured);}},"LokiInlineMediaSelect").start();
 }
 private void showImage(){
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;
  Bitmap next=BitmapFactory.decodeByteArray(image,0,image.length,options);preview.setImageBitmap(next);
  if(thumbnail!=null)thumbnail.recycle();thumbnail=next;attachment.setVisibility(View.VISIBLE);
 }
 void clear(){if(working)return;image=null;imageCount=1;attachmentImages.clear();attachmentKinds.clear();videoAttachment=false;requestId="";submittedQuestion="";preview.setImageDrawable(null);if(thumbnail!=null){thumbnail.recycle();thumbnail=null;}
  attachment.setVisibility(View.GONE);try{draft().delete();}catch(Exception ignored){}status.setText("画像・動画は合計6件（動画3本まで、各2分・50MB、8場面・音声なし）。Googleフォトは写真選択のクラウド設定または共有から");update();}
 boolean submit(String question,Runnable sent){
  if(working)return true;if(image==null)return false;
  if(question.length()>2400){status.setText("画像付きの質問は2400文字以内にしてください");return true;}
  final byte[] selected=image;final int selectedCount=imageCount;
  if(requestId.isEmpty()||!question.equals(submittedQuestion))requestId=UUID.randomUUID().toString();submittedQuestion=question;final String id=requestId;
  busy(true);status.setText("画像と質問を送信しています…");
  new Thread(()->{try{
   org.json.JSONObject receipt=ImageQuestions.enqueue(host,id,question,selected,selectedCount);try{draft().delete();}catch(Exception ignored){}
   if(!receipt.optBoolean("duplicate"))MainActivity.addAiLog("ユーザー: "+(question.isEmpty()?"この画像の内容を教えて":question)+"（画像添付・送信待ち）");
   host.runOnUiThread(()->{if(!live())return;busy(false);clear();sent.run();status.setText(receipt.optBoolean("videoGenerated")?"動画生成を受け付けました。会話を続けられます。生成動画一覧で進捗と結果を確認できます。":receipt.optBoolean("generated")?"画像生成を受け付けました。生成画像一覧で進捗と結果を確認できます。":receipt.optBoolean("complete")?"同じ画像と質問は送信済みです。上の会話ログをご確認ください。":"グラスへ送信待ちです。返信は上の会話ログに表示します。");});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("送信を確認できませんでした。画像を残しています。同じ内容で再送できます。");busy(false);}});}},"LokiInlineImageSend").start();return true;
 }
 Bundle save(){Bundle b=new Bundle();b.putInt("imageCount",imageCount);b.putBoolean("video",videoAttachment);b.putString("draft",draftId);b.putString("camera",cameraId);b.putString("request",requestId);b.putString("submitted",submittedQuestion);return b;}
 void restore(Bundle b){if(b==null)return;String id=b.getString("draft","");if(!id.matches("[a-f0-9-]{36}"))return;draftId=id;videoAttachment=b.getBoolean("video",false);imageCount=Math.max(1,Math.min(6,b.getInt("imageCount",1)));
  String camera=b.getString("camera","");cameraId=camera.matches("[a-f0-9-]{36}")?camera:"";requestId=b.getString("request","");submittedQuestion=b.getString("submitted","");
  try{File file=draft();if(!file.isFile()||System.currentTimeMillis()-file.lastModified()>ImageQuestions.TTL)return;}catch(Exception ignored){return;}
  busy(true);new Thread(()->{try{byte[] restored=DeviceSecrets.photos().decrypt(java.nio.file.Files.readAllBytes(draft().toPath()));
   byte[] restoredImage=restored;java.util.ArrayList<byte[]> parts=new java.util.ArrayList<>();java.util.ArrayList<String> restoredKinds=new java.util.ArrayList<>();boolean video=videoAttachment;
   if(restored.length>0&&restored[0]=='{'){org.json.JSONObject json=new org.json.JSONObject(new String(restored,java.nio.charset.StandardCharsets.UTF_8));restoredImage=android.util.Base64.decode(json.getString("image"),android.util.Base64.NO_WRAP);org.json.JSONArray array=json.getJSONArray("parts");if(array.length()>6)throw new IOException("draft bounds");for(int i=0;i<array.length();i++)parts.add(android.util.Base64.decode(array.getString(i),android.util.Base64.NO_WRAP));video=json.optBoolean("video");org.json.JSONArray types=json.optJSONArray("kinds");for(int i=0;i<array.length();i++)restoredKinds.add(types!=null&&"video".equals(types.optString(i))?"video":"image");}
   final byte[] restoredFinal=restoredImage;final boolean restoredVideo=video;
   host.runOnUiThread(()->{if(!live())return;image=restoredFinal;attachmentImages.clear();attachmentImages.addAll(parts);attachmentKinds.clear();attachmentKinds.addAll(restoredKinds);videoAttachment=restoredVideo;if(!parts.isEmpty())imageCount=parts.size();showImage();status.setText("添付画像を復元しました。内容を確認して送信してください");busy(false);});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("画像を再選択してください");busy(false);}});}},"LokiInlineImageRestore").start();
 }
 private void cleanupCamera(String id){if(id.isEmpty())return;try{Uri uri=Uri.parse("content://"+host.getPackageName()+".image-camera/"+id);host.revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);ImageQuestionCameraProvider.target(host,id).delete();}catch(Exception ignored){}}
 void close(){closed=true;image=null;attachmentImages.clear();attachmentKinds.clear();preview.setImageDrawable(null);if(thumbnail!=null){thumbnail.recycle();thumbnail=null;}
  if(host.isFinishing()&&!working){cleanupCamera(cameraId);try{draft().delete();}catch(Exception ignored){}}
 }
}
