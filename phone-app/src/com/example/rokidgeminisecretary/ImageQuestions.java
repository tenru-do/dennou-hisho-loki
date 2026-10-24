package com.example.rokidgeminisecretary;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

/** Explicit image questions only. Encrypted pending images expire after 30 minutes. */
final class ImageQuestions {
    static final int MAX_IMAGE=600000, MAX_SOURCE=20000000;
    static final long TTL=30*60*1000L;
    static byte[] readBounded(InputStream in)throws IOException {
        try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;
            while((n=source.read(b))!=-1){if(out.size()+n>MAX_SOURCE)throw new IOException("画像は20MB以内にしてください");out.write(b,0,n);}
            return out.toByteArray();
        }
    }
    static byte[] normalize(byte[] source)throws Exception {
        return normalize(source,MAX_IMAGE,null);
    }
    static byte[] normalize(byte[] source,int maxBytes,String[] footer)throws Exception {
        if(maxBytes<1||maxBytes>8000000)throw new IOException("invalid image budget");
        if(source.length>MAX_SOURCE)throw new IOException("画像は20MB以内にしてください");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(source,0,source.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>80000000L)throw new IOException("画像を読み取れません（最大8000万画素）");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>2048)options.inSampleSize*=2;
        Bitmap decoded=BitmapFactory.decodeByteArray(source,0,source.length,options);
        if(decoded==null)throw new IOException("画像の形式を確認してください");
        Bitmap rotated=decoded,flat=null;
        try{
            Matrix matrix=new Matrix();
            try{int o=new ExifInterface(new ByteArrayInputStream(source)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                switch(o){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;default:break;}
            }catch(IOException ignored){}
            if(!matrix.isIdentity())rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),matrix,true);
            int footerHeight=footer==null?0:Math.round(Math.max(14,rotated.getWidth()/46f)*(footer.length*1.35f+0.6f));
            flat=Bitmap.createBitmap(rotated.getWidth(),rotated.getHeight()+footerHeight,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(flat);canvas.drawColor(Color.WHITE);canvas.drawBitmap(rotated,0,0,null);
            if(footer!=null)drawFooter(canvas,rotated.getWidth(),rotated.getHeight(),footerHeight,footer);
            for(int quality=90;quality>=30;quality-=10){ByteArrayOutputStream out=new ByteArrayOutputStream();flat.compress(Bitmap.CompressFormat.JPEG,quality,out);if(out.size()<=maxBytes)return out.toByteArray();}
            throw new IOException("画像が複雑すぎます。必要な部分を切り取って再送してください");
        }finally{if(flat!=null)flat.recycle();if(rotated!=decoded)rotated.recycle();decoded.recycle();}
    }
    private static void drawFooter(Canvas canvas,int width,int top,int height,String[] lines){
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);canvas.drawRect(0,top,width,top+height,paint);
        paint.setColor(Color.WHITE);float size=Math.max(14,width/46f),pad=Math.max(4,width/100f),y=top+size*1.15f;
        for(String line:lines){paint.setTextSize(size);float measured=paint.measureText(line);if(measured>width-pad*2)paint.setTextSize(size*(width-pad*2)/measured);canvas.drawText(line,pad,y,paint);y+=size*1.35f;}
    }
    static File root(Context c){File f=new File(c.getFilesDir(),"image-questions");f.mkdirs();return f;}
    static synchronized void prune(Context c){File[] files=root(c).listFiles();if(files!=null)for(File f:files)if(System.currentTimeMillis()-f.lastModified()>TTL)f.delete();}
    static File file(Context c,String id)throws IOException{if(!id.matches("[a-f0-9-]{36}"))throw new IOException("画像IDが無効です");return new File(root(c),id+".gcm");}
    /** Reject malformed requests before recovery writes or image/video generation routing. */
    static void validateInput(String requestId,String question,byte[] jpeg)throws IOException {
        if(requestId==null||!requestId.matches("[a-zA-Z0-9-]{8,80}"))throw new IOException("送信IDが無効です。画面を開き直して送信してください");
        if(question==null||question.length()>2400)throw new IOException("画像付きの質問は2400文字以内にしてください");
        if(question.contains("__LOKI_")||question.contains("__CODEX_"))throw new IOException("内部制御用の文字列は送信できません");
        if(jpeg==null||jpeg.length==0||jpeg.length>MAX_IMAGE)throw new IOException("添付画像が空か大きすぎます。画像を選び直してください");
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,bounds);
        if(bounds.outWidth<1||bounds.outHeight<1||(long)bounds.outWidth*bounds.outHeight>80000000L)throw new IOException("添付画像を読み取れません。画像を選び直してください");
        BitmapFactory.Options preview=new BitmapFactory.Options();while(Math.max(bounds.outWidth,bounds.outHeight)/Math.max(1,preview.inSampleSize)>256)preview.inSampleSize=Math.max(1,preview.inSampleSize)*2;
        Bitmap decoded=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,preview);if(decoded==null)throw new IOException("添付画像が壊れています。画像を選び直してください");decoded.recycle();
    }
    static synchronized JSONObject enqueue(Context c,String requestId,String question,byte[] jpeg)throws Exception {return enqueue(c,requestId,question,jpeg,1,new org.json.JSONArray().put("image"));}
    static synchronized JSONObject enqueue(Context c,String requestId,String question,byte[] jpeg,int imageCount)throws Exception {return enqueue(c,requestId,question,jpeg,imageCount,null);}
    static synchronized JSONObject enqueue(Context c,String requestId,String question,byte[] jpeg,int imageCount,org.json.JSONArray sourceKinds)throws Exception {
        validateInput(requestId,question,jpeg);
        org.json.JSONArray kinds=ImageAttachmentKinds.normalize(sourceKinds,imageCount);
        if(imageCount<1||imageCount>6)throw new IOException("画像枚数が無効です（1〜6枚）");
        TransferNetwork.require(c,TransferNetwork.encodedEstimate(jpeg.length),kinds.toString().contains("video"));
        ConversationRecoveryStore.save(c,RecorderPhotoStore.hash(requestId.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"image-question",new JSONObject().put("requestId",requestId).put("message",question).put("image",Base64.encodeToString(jpeg,Base64.NO_WRAP)).put("imageCount",imageCount).put("kinds",kinds));
        if(MediaGenerationContext.video(c,question,true))return VideoGeneration.enqueue(c,requestId,question,jpeg,"");
        if(MediaGenerationContext.kind(c,question,true)==MediaGenerationIntent.Kind.IMAGE)return ImageGeneration.enqueue(c,requestId,question,jpeg,"");
        prune(c);
        if(question.trim().isEmpty())question=imageCount>1?"添付した"+imageCount+"件それぞれの内容を、番号順に教えて。画像・動画の場面一覧を別々に説明してください。":"この画像の内容を教えて。文字と日時は正確に読み取り、不鮮明な箇所は確認してください。";
        // Stable key prevents a browser retry from enqueuing another image question.
        String key=RecorderPhotoStore.hash(requestId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String id=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.US_ASCII)).toString();File f=file(c,id);
        String sha=RecorderPhotoStore.hash(jpeg);
        String command="__LOKI_IMAGE__:"+new JSONObject().put("id",id);
        if(f.exists()){JSONObject old=read(c,id);if(!old.getString("sha").equals(sha)||!old.getString("question").equals(question)||old.optInt("imageCount",1)!=imageCount||!ImageAttachmentKinds.normalize(old.optJSONArray("kinds"),imageCount).toString().equals(kinds.toString()))throw new IOException("同じ送信IDの内容が変わりました");if(!old.optBoolean("queued")&&!old.optBoolean("complete")){if(!MainActivity.queueImageCommand(c,command))throw new IOException("グラスの送信待ちが満杯です");old.put("queued",true);save(f,old);}return new JSONObject().put("queued",true).put("duplicate",true).put("complete",old.optBoolean("complete")).put("id",id);}
        File[] pending=root(c).listFiles();int waiting=0,records=0;if(pending!=null)for(File saved:pending)if(saved.getName().endsWith(".gcm")){records++;JSONObject record=new JSONObject(new String(DeviceSecrets.photos().decrypt(Files.readAllBytes(saved.toPath())),java.nio.charset.StandardCharsets.UTF_8));if(!record.optBoolean("complete"))waiting++;}
        if(waiting>=12)throw new IOException("画像の送信待ちが満杯です。解析を待ってください");
        if(records>=256)throw new IOException("画像送信の短期上限に達しました。30分ほど待ってください");
        JSONObject packet=new JSONObject().put("id",id).put("imageCount",imageCount).put("kinds",kinds).put("question",question).put("sha",sha).put("expires",System.currentTimeMillis()+TTL).put("data",Base64.encodeToString(jpeg,Base64.NO_WRAP));
        packet.put("queued",false);save(f,packet);
        if(!MainActivity.queueImageCommand(c,command)){f.delete();throw new IOException("グラスの送信待ちが満杯です");}
        packet.put("queued",true);save(f,packet);
        return new JSONObject().put("queued",true).put("id",id);
    }
    static synchronized JSONObject read(Context c,String id)throws Exception{File f=file(c,id);if(!f.isFile())throw new IOException("画像がありません。選択して再送してください");JSONObject j=new JSONObject(new String(DeviceSecrets.photos().decrypt(Files.readAllBytes(f.toPath())),java.nio.charset.StandardCharsets.UTF_8));if(j.getLong("expires")<System.currentTimeMillis()){f.delete();throw new IOException("画像の期限が切れました。選択して再送してください");}return j;}
    static void save(File f,JSONObject packet)throws Exception{File temp=new File(f.getParentFile(),f.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(DeviceSecrets.photos().encrypt(packet.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));out.getFD().sync();}Files.move(temp.toPath(),f.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);f.setLastModified(packet.getLong("expires")-TTL);}
    static synchronized void complete(Context c,String id)throws Exception{JSONObject j=read(c,id);j.remove("data");j.put("complete",true);save(file(c,id),j);}
}
