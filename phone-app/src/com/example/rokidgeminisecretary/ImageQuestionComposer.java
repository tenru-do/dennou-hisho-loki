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
 private byte[] image;private Bitmap thumbnail;private boolean working,closed;
 private String draftId=UUID.randomUUID().toString(),cameraId="",requestId="",submittedQuestion="";
 ImageQuestionComposer(Activity host,Runnable changed){
  this.host=host;this.changed=changed;root=new LinearLayout(host);root.setOrientation(LinearLayout.VERTICAL);
  LinearLayout actions=new LinearLayout(host);
  choose=new Button(host);choose.setText("画像を添付");choose.setOnClickListener(v->choose());actions.addView(choose,new LinearLayout.LayoutParams(0,-2,1));
  camera=new Button(host);camera.setText("撮影して添付");camera.setOnClickListener(v->capture());actions.addView(camera,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
  attachment=new LinearLayout(host);attachment.setOrientation(LinearLayout.HORIZONTAL);
  preview=new ImageView(host);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setContentDescription("送信前の添付画像");
  attachment.addView(preview,new LinearLayout.LayoutParams(0,dp(88),1));
  remove=new Button(host);remove.setText("画像を外す");remove.setOnClickListener(v->clear());attachment.addView(remove,new LinearLayout.LayoutParams(-2,-2));
  root.addView(attachment);attachment.setVisibility(View.GONE);
  status=new TextView(host);status.setTextSize(12);status.setText("画像は送信を押すまで解析されません");root.addView(status);update();
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
 private void choose(){if(working)return;try{busy(true);host.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK);}catch(Exception e){status.setText("画像を選ぶアプリがありません");busy(false);}}
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
  final Uri uri=code==CAPTURE?cameraUri():data==null?null:data.getData();
  if(uri==null){cleanupCamera(captured);cameraId="";status.setText("画像が選択されていません");busy(false);return true;}
  busy(true);status.setText("画像を準備しています…");
  new Thread(()->{try{
   byte[] next=ImageQuestions.normalize(ImageQuestions.readBounded(host.getContentResolver().openInputStream(uri)));
   java.nio.file.Files.write(draft().toPath(),DeviceSecrets.photos().encrypt(next));
   host.runOnUiThread(()->{if(!live())return;image=next;requestId="";submittedQuestion="";showImage();status.setText("画像と質問を確認し、送信を押してください（Geminiへ送信）");busy(false);});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("画像を読み込めません。形式・サイズを確認してください");busy(false);}});}
  finally{cleanupCamera(captured);}},"LokiInlineImageSelect").start();return true;
 }
 private void showImage(){
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;
  Bitmap next=BitmapFactory.decodeByteArray(image,0,image.length,options);preview.setImageBitmap(next);
  if(thumbnail!=null)thumbnail.recycle();thumbnail=next;attachment.setVisibility(View.VISIBLE);
 }
 void clear(){if(working)return;image=null;requestId="";submittedQuestion="";preview.setImageDrawable(null);if(thumbnail!=null){thumbnail.recycle();thumbnail=null;}
  attachment.setVisibility(View.GONE);try{draft().delete();}catch(Exception ignored){}status.setText("画像は送信を押すまで解析されません");update();}
 boolean submit(String question,Runnable sent){
  if(working)return true;if(image==null)return false;
  if(question.length()>2400){status.setText("画像付きの質問は2400文字以内にしてください");return true;}
  final byte[] selected=image;
  if(requestId.isEmpty()||!question.equals(submittedQuestion))requestId=UUID.randomUUID().toString();submittedQuestion=question;final String id=requestId;
  busy(true);status.setText("画像と質問を送信しています…");
  new Thread(()->{try{
   org.json.JSONObject receipt=ImageQuestions.enqueue(host,id,question,selected);try{draft().delete();}catch(Exception ignored){}
   if(!receipt.optBoolean("duplicate"))MainActivity.addAiLog("ユーザー: "+(question.isEmpty()?"この画像の内容を教えて":question)+"（画像添付・送信待ち）");
   host.runOnUiThread(()->{if(!live())return;busy(false);clear();sent.run();status.setText(receipt.optBoolean("videoGenerated")?"動画生成を受け付けました。会話を続けられます。生成動画一覧で進捗と結果を確認できます。":receipt.optBoolean("generated")?"画像生成を受け付けました。生成画像一覧で進捗と結果を確認できます。":receipt.optBoolean("complete")?"同じ画像と質問は送信済みです。上の会話ログをご確認ください。":"グラスへ送信待ちです。返信は上の会話ログに表示します。");});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("送信を確認できませんでした。画像を残しています。同じ内容で再送できます。");busy(false);}});}},"LokiInlineImageSend").start();return true;
 }
 Bundle save(){Bundle b=new Bundle();b.putString("draft",draftId);b.putString("camera",cameraId);b.putString("request",requestId);b.putString("submitted",submittedQuestion);return b;}
 void restore(Bundle b){if(b==null)return;String id=b.getString("draft","");if(!id.matches("[a-f0-9-]{36}"))return;draftId=id;
  String camera=b.getString("camera","");cameraId=camera.matches("[a-f0-9-]{36}")?camera:"";requestId=b.getString("request","");submittedQuestion=b.getString("submitted","");
  try{File file=draft();if(!file.isFile()||System.currentTimeMillis()-file.lastModified()>ImageQuestions.TTL)return;}catch(Exception ignored){return;}
  busy(true);new Thread(()->{try{byte[] restored=DeviceSecrets.photos().decrypt(java.nio.file.Files.readAllBytes(draft().toPath()));
   host.runOnUiThread(()->{if(!live())return;image=restored;showImage();status.setText("添付画像を復元しました。内容を確認して送信してください");busy(false);});
  }catch(Exception e){host.runOnUiThread(()->{if(live()){status.setText("画像を再選択してください");busy(false);}});}},"LokiInlineImageRestore").start();
 }
 private void cleanupCamera(String id){if(id.isEmpty())return;try{Uri uri=Uri.parse("content://"+host.getPackageName()+".image-camera/"+id);host.revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);ImageQuestionCameraProvider.target(host,id).delete();}catch(Exception ignored){}}
 void close(){closed=true;image=null;preview.setImageDrawable(null);if(thumbnail!=null){thumbnail.recycle();thumbnail=null;}
  if(host.isFinishing()&&!working){cleanupCamera(cameraId);try{draft().delete();}catch(Exception ignored){}}
 }
}
