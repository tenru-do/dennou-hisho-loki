package com.example.rokidkeyboardbridge;
import android.content.Context;import org.json.*;import java.io.*;import java.nio.*;import java.nio.file.*;
final class MeetingAudioArchive implements AutoCloseable {
    static final long MAX_BYTES=230400000L;static final int PART_BYTES=320000;
    final File folder;final JSONObject meta;private RandomAccessFile pcm;private File partial;
    private int size,index;private long total,lastSync;private final PhotoVaultCipher cipher;
    MeetingAudioArchive(Context c)throws Exception {
        recover(c);
        folder=new File(root(c),System.currentTimeMillis()+"-"+java.util.UUID.randomUUID());if(!folder.mkdirs())throw new IOException("録音フォルダを作成できません");
        cipher=DeviceSecrets.photos();meta=new JSONObject().put("id",folder.getName()).put("startedAt",System.currentTimeMillis()).put("state","recording").put("parts",new JSONArray());save();
    }
    static File root(Context c){File f=new File(c.getFilesDir(),"meeting-audio");f.mkdirs();return f;}
    static void recover(Context c)throws Exception {
        File[] folders=root(c).listFiles();if(folders==null)return;PhotoVaultCipher vault=DeviceSecrets.photos();
        for(File f:folders){File manifest=new File(f,"session.json");if(!manifest.exists())continue;
            JSONObject m=new JSONObject(new String(Files.readAllBytes(manifest.toPath()),java.nio.charset.StandardCharsets.UTF_8));if("stopped".equals(m.optString("state")))continue;
            File partial=new File(f,"current.part");File[] sealed=f.listFiles((dir,name)->name.matches("[0-9]{4}\\.gcm"));java.util.Arrays.sort(sealed,(a,b)->a.getName().compareTo(b.getName()));
            if(partial.exists()){byte[] raw=Files.readAllBytes(partial.toPath());if(raw.length>PART_BYTES)throw new IOException("invalid partial audio");if(raw.length>0){File target=new File(f,String.format(java.util.Locale.US,"%04d.gcm",sealed.length));try(FileOutputStream out=new FileOutputStream(target)){out.write(vault.encrypt(wav(java.util.Arrays.copyOf(raw,raw.length-raw.length%2))));out.getFD().sync();}}partial.delete();sealed=f.listFiles((dir,name)->name.matches("[0-9]{4}\\.gcm"));java.util.Arrays.sort(sealed,(a,b)->a.getName().compareTo(b.getName()));}
            JSONArray parts=new JSONArray();long bytes=0;for(File part:sealed){byte[] w=vault.decrypt(Files.readAllBytes(part.toPath()));parts.put(new JSONObject().put("file",part.getName()).put("bytes",w.length-44).put("sha",hash(w)));bytes+=w.length-44;}
            m.put("parts",parts).put("pcmBytes",bytes).put("durationMs",bytes/32).put("state","stopped").put("reason","前回中断・保存済み音声を復旧");File temp=new File(f,"session.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(m.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}Files.move(temp.toPath(),manifest.toPath(),StandardCopyOption.REPLACE_EXISTING);
        }
    }
    static byte[] wav(byte[] data){ByteBuffer b=ByteBuffer.allocate(44+data.length).order(ByteOrder.LITTLE_ENDIAN);b.put(new byte[]{'R','I','F','F'}).putInt(36+data.length).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(16000).putInt(32000).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(data.length).put(data);return b.array();}
    void append(byte[] b)throws Exception {
        int at=0;while(at<b.length){if(pcm==null){partial=new File(folder,"current.part");pcm=new RandomAccessFile(partial,"rw");size=0;}
            int n=Math.min(b.length-at,PART_BYTES-size);pcm.write(b,at,n);size+=n;total+=n;at+=n;
            long now=System.currentTimeMillis();if(now-lastSync>2000){pcm.getFD().sync();lastSync=now;}
            if(size==PART_BYTES)seal();}
    }
    private void seal()throws Exception {
        if(pcm==null)return;pcm.getFD().sync();pcm.close();pcm=null;
        byte[] raw=Files.readAllBytes(partial.toPath());byte[] audio=wav(raw);
        String name=String.format(java.util.Locale.US,"%04d.gcm",index++);
        File temp=new File(folder,name+".tmp"),target=new File(folder,name);
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(cipher.encrypt(audio));out.getFD().sync();}
        Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);
        meta.getJSONArray("parts").put(new JSONObject().put("file",name).put("bytes",raw.length).put("sha",hash(audio)));
        save();if(!partial.delete())throw new IOException("録音の作業ファイルを消去できません");
    }
    static String hash(byte[] bytes)throws Exception {StringBuilder s=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(java.util.Locale.US,"%02x",b));return s.toString();}
    private void save()throws Exception {meta.put("pcmBytes",total).put("durationMs",total/32);File temp=new File(folder,"session.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(meta.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}Files.move(temp.toPath(),new File(folder,"session.json").toPath(),StandardCopyOption.REPLACE_EXISTING);}
    void finish(String reason)throws Exception{seal();meta.put("state","stopped").put("reason",reason);save();}
    void topic(org.json.JSONObject item)throws Exception {JSONArray items=meta.optJSONArray("topics");if(items==null)items=new JSONArray();if(items.length()<500){items.put(item);meta.put("topics",items);save();}}
    public void close()throws IOException{if(pcm!=null){pcm.getFD().sync();pcm.close();pcm=null;}}
}
