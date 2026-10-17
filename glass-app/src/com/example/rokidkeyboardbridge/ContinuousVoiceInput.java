package com.example.rokidkeyboardbridge;

import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/** One microphone producer and one ordered recognizer. PCM exists only in bounded RAM. */
final class ContinuousVoiceInput {
    interface Source { byte[] capture(BooleanSupplier finish) throws Exception; void stop(); default boolean speaking(){return false;} }
    interface Decoder { String decode(byte[] pcm) throws Exception; }
    interface Listener { void text(String text); void state(String text); void error(String text); void ended(); }
    static final int MAX_QUEUED_CLIPS = 2;
    static final int MAX_CLIP_BYTES = 16000 * 2 * 60;
    private final Source source;
    private final Decoder decoder;
    private final Listener listener;
    private final BooleanSupplier valid;
    private final ArrayDeque<byte[]> queue = new ArrayDeque<>();
    private volatile boolean finishing, cancelled, producerDone, decoding;
    private Thread producer, consumer;
    ContinuousVoiceInput(Source source, Decoder decoder, Listener listener, BooleanSupplier valid) {
        this.source=source;this.decoder=decoder;this.listener=listener;this.valid=valid;
    }
    void start() {
        producer=new Thread(this::produce,"VoiceMicContinuous");
        consumer=new Thread(this::consume,"VoiceSttOrdered");
        consumer.start();producer.start();
    }
    boolean finishing(){return finishing;}
    boolean hasUnfinishedAudio(){synchronized(queue){return decoding||!queue.isEmpty()||source.speaking();}}
    void finish(){finishing=true;source.stop();synchronized(queue){queue.notifyAll();}}
    void cancel(){cancelled=true;finishing=true;source.stop();synchronized(queue){queue.clear();queue.notifyAll();}if(producer!=null)producer.interrupt();if(consumer!=null)consumer.interrupt();}
    private boolean invalid(){return cancelled||!valid.getAsBoolean();}
    private void produce(){
        try {
            while(!finishing&&!invalid()) {
                synchronized(queue){
                    while(queue.size()>=MAX_QUEUED_CLIPS&&!finishing&&!invalid()){
                        listener.state("音声蓄積上限・認識待ち（録音休止）");queue.wait(200);
                    }
                }
                if(finishing||invalid())break;
                listener.state("音声入力中・認識待ちも追加受付");
                byte[] pcm=source.capture(()->finishing||invalid());
                if(invalid())break;
                if(pcm==null||pcm.length==0)continue;
                if(pcm.length>MAX_CLIP_BYTES)throw new IllegalStateException("録音上限を超えました");
                synchronized(queue){queue.addLast(pcm);queue.notifyAll();}
            }
        }catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        catch(Exception e){if(!invalid())listener.error("録音エラー: "+e.getClass().getSimpleName());}
        finally{producerDone=true;source.stop();synchronized(queue){queue.notifyAll();}}
    }
    private void consume(){
        try{
            while(!invalid()){
                byte[] pcm;
                synchronized(queue){
                    while(queue.isEmpty()&&!producerDone&&!invalid())queue.wait(200);
                    if(invalid()||(queue.isEmpty()&&producerDone))break;
                    pcm=queue.removeFirst();decoding=true;queue.notifyAll();
                }
                listener.state(finishing?"最後の音声を認識中":"音声認識中・追加発話も受付中");
                try{String text;try{text=decoder.decode(pcm);}finally{decoding=false;}if(!invalid()&&text!=null&&!text.trim().isEmpty())listener.text(text.trim());}
                catch(Exception e){if(!invalid())listener.error("音声認識に失敗: "+e.getClass().getSimpleName()+"（次の発話は保持）");}
            }
        }catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        finally{cancelled=true;source.stop();synchronized(queue){queue.clear();queue.notifyAll();}listener.ended();}
    }
}