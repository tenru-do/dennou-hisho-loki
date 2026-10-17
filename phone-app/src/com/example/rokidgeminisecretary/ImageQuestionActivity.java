package com.example.rokidgeminisecretary;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import android.graphics.BitmapFactory;
import java.io.*;
import java.util.UUID;

public final class ImageQuestionActivity extends Activity {
    private byte[] image;
    private Uri cameraUri;
    private String cameraId="",draftId="";
    private EditText question;
    private TextView status;
    private ImageView preview;
    private Button send,choose,camera;
    private boolean working;
    protected void onCreate(Bundle state){super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,20,20,20);
        TextView title=new TextView(this);title.setText("画像をロキに質問");title.setTextSize(22);root.addView(title);
        TextView help=new TextView(this);help.setText("送信すると画像と質問をグラスのGeminiへ送ります。氏名などの個人情報も含まれます。予約の登録は読み取り後に日時・カレンダーを確認します。");root.addView(help);
        choose=new Button(this);choose.setText("画像を選択");choose.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(i,701);}catch(Exception e){status.setText("画像を選ぶアプリがありません");}});root.addView(choose);
        camera=new Button(this);camera.setText("カメラで撮影");camera.setOnClickListener(v->capture());root.addView(camera);
        preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setMaxHeight(380);root.addView(preview);
        question=new EditText(this);question.setHint("例：予約日時を読んで。選んだカレンダーへの登録候補を作って");question.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2400)});root.addView(question);
        send=new Button(this);send.setText("画像と質問を送信（Geminiへ）");send.setOnClickListener(v->submit());root.addView(send);
        status=new TextView(this);root.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        if(state!=null){cameraId=state.getString("cameraId","");draftId=state.getString("draftId","");question.setText(state.getString("question",""));if(!cameraId.isEmpty())cameraUri=uri(cameraId);}
        if(!draftId.isEmpty())try{File f=draft();if(System.currentTimeMillis()-f.lastModified()<ImageQuestions.TTL){image=DeviceSecrets.photos().decrypt(java.nio.file.Files.readAllBytes(f.toPath()));showImage();}}catch(Exception e){status.setText("画像を再選択してください");}
        if(draftId.isEmpty())draftId=UUID.randomUUID().toString();ImageQuestions.prune(this);try{ImageQuestionCameraProvider.target(this,UUID.randomUUID().toString());}catch(Exception ignored){}setBusy(false);
    }
    private Uri uri(String id){return Uri.parse("content://"+getPackageName()+".image-camera/"+id);}
    private File draft(){File dir=new File(getCacheDir(),"image-question-drafts");dir.mkdirs();for(File f:dir.listFiles())if(System.currentTimeMillis()-f.lastModified()>ImageQuestions.TTL)f.delete();return new File(dir,draftId+".gcm");}
    private void capture(){try{
        cameraId=UUID.randomUUID().toString();File f=ImageQuestionCameraProvider.target(this,cameraId);new FileOutputStream(f).close();cameraUri=uri(cameraId);
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
        i.setClipData(ClipData.newRawUri("撮影先",cameraUri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i,702);
    }catch(Exception e){cleanupCamera();status.setText("カメラを開けません。画像を選択してください");}}
    protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code!=701&&code!=702)return;if(result!=RESULT_OK){if(code==702)cleanupCamera();return;}final Uri selected=code==702?cameraUri:data==null?null:data.getData();if(selected==null){status.setText("画像が選択されていません");return;}setBusy(true);status.setText("画像を準備しています…");new Thread(()->{try{byte[] next=ImageQuestions.normalize(ImageQuestions.readBounded(getContentResolver().openInputStream(selected)));java.nio.file.Files.write(draft().toPath(),DeviceSecrets.photos().encrypt(next));runOnUiThread(()->{image=next;showImage();status.setText("送信する画像を確認してください");setBusy(false);});}catch(Exception e){runOnUiThread(()->{status.setText("画像を読み込めません。形式・サイズを確認してください");setBusy(false);});}finally{if(code==702)cleanupCamera();}},"LokiImageSelect").start();}
    private void showImage(){preview.setImageBitmap(BitmapFactory.decodeByteArray(image,0,image.length));}
    private void setBusy(boolean value){working=value;send.setEnabled(!value&&image!=null);choose.setEnabled(!value);camera.setEnabled(!value);}
    private void submit(){if(working||image==null)return;final byte[] selected=image;final String text=question.getText().toString().trim();setBusy(true);new Thread(()->{try{ImageQuestions.enqueue(this,UUID.randomUUID().toString(),text,selected);draft().delete();runOnUiThread(()->{image=null;preview.setImageDrawable(null);status.setText("グラスへ送信待ちです。返信はスマホ・Webの共通履歴とグラスに表示します。");setBusy(false);});}catch(Exception e){runOnUiThread(()->{status.setText(e.getMessage());setBusy(false);});}},"LokiImageSend").start();}
    private void cleanupCamera(){try{if(cameraUri!=null)revokeUriPermission(cameraUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);if(!cameraId.isEmpty())ImageQuestionCameraProvider.target(this,cameraId).delete();}catch(Exception ignored){}}
    protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("cameraId",cameraId);out.putString("draftId",draftId);out.putString("question",question.getText().toString());}
}
