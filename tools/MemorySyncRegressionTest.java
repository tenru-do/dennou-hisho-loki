package com.example.rokidkeyboardbridge;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import com.example.rokidgeminisecretary.DynamicMemorySync;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.json.*;

/** Android app_process test; isolated file store and fake preferences, no production data. */
public final class MemorySyncRegressionTest {
    private static void check(boolean value,String label) { if(!value) throw new AssertionError(label); }
    private static JSONObject operation(JSONObject entry,String action,String content) throws Exception {
        return new JSONObject().put("id","op-1").put("action",action).put("expected",entry)
                .put("topic",entry.getString("topic")).put("content",content);
    }
    public static void main(String[] args) throws Exception {
        File directory=new File(args[0]); check(directory.mkdirs(),"isolated directory");
        Map<String,Object> values=new HashMap<>();
        final SharedPreferences[] preferences=new SharedPreferences[1];
        preferences[0]=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)-> {
            if(m.getName().equals("edit")) {
                Map<String,Object> staged=new HashMap<>();
                return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,em,ea)-> {
                    if(em.getName().startsWith("put")) { staged.put((String)ea[0],ea[1]); return e; }
                    if(em.getName().equals("remove")) { staged.put((String)ea[0],null); return e; }
                    if(em.getName().equals("commit")) { for(String k:staged.keySet()) { if(staged.get(k)==null) values.remove(k); else values.put(k,staged.get(k)); } return true; }
                    throw new UnsupportedOperationException(em.getName());
                });
            }
            if(m.getName().startsWith("get")) return values.containsKey(a[0])?values.get(a[0]):a[1];
            throw new UnsupportedOperationException(m.getName());
        });
        ContextWrapper context=new ContextWrapper(null) {
            @Override public File getFilesDir() { return directory; }
            @Override public SharedPreferences getSharedPreferences(String name,int mode) { return preferences[0]; }
        };
        MemoryManager glass=new MemoryManager(context);
        JSONObject entry=glass.save("好み","無糖"); glass.save("予定","散歩");
        DynamicMemorySync phone=new DynamicMemorySync(context); phone.refresh();
        JSONObject request=phone.poll(), reply=glass.exchange(request);
        check(phone.accept(reply).getBoolean("ok") && phone.state().getJSONArray("entries").length()==2,"initial snapshot");
        phone.queue(entry,"edit","好み","微糖");
        check(phone.state().getJSONArray("entries").getJSONObject(1).getString("content").equals("無糖"),"no optimistic overwrite");
        check(new DynamicMemorySync(context).poll().getJSONObject("operation")!=null,"outbox survives recreation");
        try { phone.queue(entry,"delete","",""); throw new AssertionError("second operation accepted"); } catch(IllegalStateException expected) { }
        request=phone.poll(); reply=glass.exchange(request);
        JSONObject replay=glass.exchange(request);
        check(reply.getBoolean("ok") && replay.getBoolean("ok") && glass.getAll().length()==2,"idempotent edit retry preserves others");
        phone.accept(reply); check(!phone.state().getBoolean("pending"),"ack clears outbox");
        check(glass.getAll().getJSONObject(1).getString("id").equals(entry.getString("id"))
                && glass.getAll().getJSONObject(1).getString("timestamp").equals(entry.getString("timestamp")),"ID timestamp retained");
        JSONObject conflict=glass.exchange(new JSONObject().put("operation",operation(entry,"delete","")));
        check(!conflict.getBoolean("ok") && glass.getAll().length()==2,"stale delete rejected");
        JSONObject edited=glass.getAll().getJSONObject(1);
        phone.queue(edited,"delete","","");
        check(phone.accept(reply).optBoolean("ignored") && phone.state().getBoolean("pending"),"old ack cannot clear new delete");
        request=phone.poll(); reply=glass.exchange(request);
        check(glass.exchange(request).getBoolean("ok"),"idempotent delete retry");
        phone.accept(reply); check(glass.getAll().length()==1 && phone.state().getJSONArray("entries").length()==1,"delete synchronizes both");
        phone.refresh(); request=phone.poll(); phone.refresh();
        check(phone.accept(glass.exchange(request)).optBoolean("ignored"),"out-of-order refresh ignored");
        phone.accept(glass.exchange(phone.poll()));
        try { phone.queue(edited,"edit","","bad"); throw new AssertionError("empty topic accepted"); } catch(IllegalArgumentException expected) { }
        // Different MemoryManager instances must not lose concurrent Gemini saves.
        Thread first=new Thread(()-> { try { for(int i=0;i<20;i++) new MemoryManager(context).save("a","a"+i); } catch(Exception e) { throw new RuntimeException(e); } });
        Thread second=new Thread(()-> { try { for(int i=0;i<20;i++) new MemoryManager(context).save("b","b"+i); } catch(Exception e) { throw new RuntimeException(e); } });
        first.start(); second.start(); first.join(); second.join(); check(glass.getAll().length()==41,"shared lock preserves concurrent writes");
        check(new File(directory,"memories.json").delete() && directory.delete(),"cleanup isolated store");
        System.out.println("PASS: snapshot, durable outbox, edit/delete, conflict, retries, stale acknowledgements, validation, concurrent writes");
    }
}
