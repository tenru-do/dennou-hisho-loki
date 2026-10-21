import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {Conversation,contextFor} from '../modules/conversation.js';
import {Store,storageMessage} from '../modules/store.js';
import {QUICK_PROMPTS} from '../modules/persona.js';
import {Voice} from '../modules/voice.js';
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};};
const tick=()=>new Promise(r=>setImmediate(r));
class Memory {constructor(){this.data=new Map();this.fail=false;this.writes=0;}getItem(k){return this.data.get(k)??null;}setItem(k,v){if(this.fail)throw Error('quota');this.writes++;this.data.set(k,v);}}
function model(chunks=[{done:false,value:'回答です。'},{done:true}]){
 const result={creates:[],inputs:[],destroyed:0,cancelled:0,availabilityCalls:0};
 result.api={availability:async()=>{result.availabilityCalls++;return 'available';},create:async options=>{result.creates.push(options);let i=0;return {promptStreaming:input=>{result.inputs.push(input);return {read:async()=>{const next=chunks[i++];if(next instanceof Error)throw next;return next??{done:true};},cancel:()=>result.cancelled++};},destroy:()=>result.destroyed++};}};return result;
}
function conversation(m,extra={}){return new Conversation({languageModel:m.api,wait:async()=>{},...extra});}
test('stream is provisional until completion and handles empty polls and final value',async()=>{
 const m=model([{done:false},{done:false,value:'こんにちは。'},{done:true,value:'ロキです。'}]);const observations=[];let now=0;
 const c=conversation(m,{now:()=>++now,onChange:rows=>observations.push(rows)});assert.equal(await c.send('挨拶してください'),true);
 assert.equal(c.rows[0].answer,'こんにちは。ロキです。');assert.equal(c.rows[0].state,'ANSWERED');assert.ok(c.rows[0].firstTextMs>=0);assert.equal(m.inputs.length,1);assert.equal(m.destroyed,1);
 assert.ok(observations.some(rows=>rows[0]?.state==='RUNNING'&&rows[0]?.answer==='こんにちは。'));
});
test('one request at a time; no send for blank or too long text',async()=>{
 const gate=deferred(),m=model();m.api.availability=()=>gate.promise;const c=conversation(m);const pending=c.send('一件目');
 assert.equal(await c.send('二件目'),false);assert.equal(await c.send(' '),false);gate.resolve('available');await pending;assert.equal(await c.send('x'.repeat(2401)),false);assert.equal(m.inputs.length,1);
});
test('cancel during model creation destroys late session without starting old prompt',async()=>{
 const gate=deferred(),m=model();m.api.create=()=>gate.promise;const c=conversation(m);const pending=c.send('古い質問');await tick();c.stop();let destroyed=0;
 gate.resolve({destroy(){destroyed++;},promptStreaming(){throw Error('must not start');}});await pending;assert.equal(destroyed,1);assert.equal(c.rows[0].state,'STOPPED');
});
test('late old chunk cannot replace newer answer',async()=>{
 const gate=deferred(),m=model();let seq=0;m.api.create=async()=>{const old=seq++===0;let read=0;return {destroy(){},promptStreaming:()=>({cancel(){},read:()=>old?gate.promise:Promise.resolve(read++?{done:true}:{done:false,value:'新しい回答です。'})})};};
 const c=conversation(m);const old=c.send('古い質問');await tick();c.stop();assert.equal(await c.send('新しい質問'),true);gate.resolve({done:true,value:'古い回答'});await old;
 assert.equal(c.rows[0].state,'STOPPED');assert.equal(c.rows[0].answer,'');assert.equal(c.rows[1].answer,'新しい回答です。');
});
test('timeout terminates stuck request and does not retry',async()=>{
 const gate=deferred(),m=model();m.api.availability=()=>gate.promise;let timeout;
 const c=conversation(m,{setTimer:fn=>{timeout=fn;return 1;},clearTimer:()=>{}});const pending=c.send('質問');timeout();assert.equal(c.rows[0].state,'FAILED');gate.resolve('available');await pending;assert.equal(m.creates.length,0);
});
test('network error keeps partial unconfirmed and hides raw error',async()=>{
 const m=model([{done:false,value:'途中です。'},Error('SECRET PROVIDER TOKEN')]);const c=conversation(m);await c.send('質問');assert.equal(c.rows[0].state,'FAILED');assert.equal(c.rows[0].answer,'途中です。');assert.ok(!c.rows[0].reason.includes('SECRET'));assert.equal(m.inputs.length,1);
});
test('empty or excessive answers fail instead of confirming',async()=>{
 for(const chunks of [[{done:true}],[{done:false,value:'x'.repeat(12001)}]]){const c=conversation(model(chunks));await c.send('質問');assert.equal(c.rows[0].state,'FAILED');}
});
test('host unavailable sends no prompt',async()=>{const m=model();m.api.availability=async()=> 'unavailable';const c=conversation(m);await c.send('質問');assert.equal(m.inputs.length,0);assert.equal(c.rows[0].state,'FAILED');});
test('topic and status bound the model context; restored history is not sent',async()=>{
 const m=model(),c=conversation(m);await c.send('一件目');await c.send('二件目','detailed');assert.equal(m.creates[1].initialPrompts.length,3);assert.ok(m.creates[1].initialPrompts[0].content.includes('丁寧に説明'));
 c.newTopic();await c.send('別の話題');assert.equal(m.creates[2].initialPrompts.length,1);
 assert.deepEqual(contextFor([{state:'FAILED',topic:0,question:'x',answer:'x'},{state:'ANSWERED',topic:0,restored:true,question:'secret',answer:'secret'}],0),[]);
});
test('context keeps at most six pairs within character budget',()=>{
 const rows=Array.from({length:10},(_,i)=>({state:'ANSWERED',topic:0,question:'q'+i,answer:'a'+i}));assert.equal(contextFor(rows,0).length,12);assert.equal(contextFor([{...rows[0],answer:'a'.repeat(10000)}],0).length,0);
});
test('dispose blocks later sends and cancel before availability prevents creation',async()=>{
 const gate=deferred(),m=model();m.api.availability=()=>gate.promise;const c=conversation(m);const pending=c.send('質問');c.dispose();gate.resolve('available');await pending;assert.equal(m.creates.length,0);assert.equal(await c.send('後の質問'),false);
});
test('memory history stays bounded',async()=>{const c=conversation(model());for(let i=0;i<45;i++)await c.send('質問'+i);assert.equal(c.rows.length,40);});
test('history is off by default and notes never clear unrelated storage',()=>{
 const mem=new Memory();mem.setItem('unrelated','keep');const s=new Store(mem);assert.equal(s.state.remember,false);s.saveHistory([{state:'ANSWERED'}]);assert.equal(mem.writes,1);s.addNote('保存する文章','note-1',1);assert.equal(mem.getItem('unrelated'),'keep');assert.equal(s.state.notes[0].text,'保存する文章');
});
test('saving failure keeps state and reports a safe message',()=>{
 const mem=new Memory(),s=new Store(mem);mem.fail=true;assert.throws(()=>s.addNote('keep','note-1',1));assert.equal(s.state.notes.length,0);assert.ok(!storageMessage(Error('SECRET')).includes('SECRET'));
});
test('corrupt and unavailable storage is not overwritten',()=>{
 const mem=new Memory();mem.setItem('dennou-hisho-loki.aiui.v1','corrupt');const s=new Store(mem);assert.ok(s.error);assert.throws(()=>s.addNote('keep','note-1',1));assert.equal(mem.writes,1);assert.ok(new Store(undefined).error);
});
test('concurrent storage revision rejects lost update',()=>{const mem=new Memory(),a=new Store(mem),b=new Store(mem);a.addNote('one','note-1',1);assert.throws(()=>b.addNote('two','note-2',2),/STORAGE_CONFLICT/);assert.equal(new Store(mem).state.notes.length,1);});
test('only confirmed twenty history entries persisted and off removes saved history',()=>{
 const s=new Store(new Memory());s.addNote('keep','note-1',1);const rows=Array.from({length:25},(_,i)=>({id:'turn-'+i,state:'ANSWERED',question:'q',answer:'a',at:i}));rows.push({id:'turn-fail',state:'FAILED',question:'secret',answer:'partial'});
 s.savePreferences(true,'detailed',rows);assert.equal(s.state.history.length,20);assert.ok(s.state.history.every(x=>x.restored&&x.state==='ANSWERED'));s.savePreferences(false,'short',rows);assert.equal(s.state.history.length,0);assert.equal(s.state.notes.length,1);
});
test('note limits and explicit scoped delete',()=>{
 const s=new Store(new Memory());assert.throws(()=>s.addNote('x'.repeat(1001),'long',1));for(let i=0;i<30;i++)s.addNote('note','note-'+i,i);assert.throws(()=>s.addNote('overflow','extra',1));s.removeNote('note-0');assert.equal(s.state.notes.length,29);assert.throws(()=>s.removeNote('missing'));
});
class Recognition {start(){}stop(){this.stopped=true;}abort(){this.aborted=true;}}
test('voice returns draft only, requires explicit start and ignores aborted result',()=>{
 const drafts=[];const v=new Voice({Recognition,onDraft:x=>drafts.push(x)});assert.equal(v.recognition,null);v.start();const old=v.recognition;old.onresult({results:[[{transcript:'聞き取った文章'}]]});assert.deepEqual(drafts,['聞き取った文章']);v.stopRecording(true);old.onresult({results:[[{transcript:'古い音声'}]]});assert.equal(drafts.length,1);assert.equal(old.aborted,true);
});
test('voice stop finalizes recognition without sending a model request',()=>{const v=new Voice({Recognition});v.start();const r=v.recognition;v.stopRecording();assert.equal(r.stopped,true);r.onend();assert.equal(v.recognition,null);});
test('voice stop aborts a host that never acknowledges completion',()=>{let timer;const v=new Voice({Recognition,setTimer:fn=>{timer=fn;return 1;},clearTimer:()=>{}});v.start();const r=v.recognition;v.stopRecording();timer();assert.equal(v.recognition,null);assert.equal(r.aborted,true);});
test('late TTS generation is aborted and never played after stop',async()=>{
 const gate=deferred();let played=0,aborted=0;const v=new Voice({Utterance:class{},synthesis:{synthesize:()=>gate.promise},Player:class{play(){played++;}}});const pending=v.play('回答');v.stopPlayback();gate.resolve({abort(){aborted++;},finished:Promise.resolve()});assert.equal(await pending,false);assert.equal(played,0);assert.equal(aborted,1);
});
test('TTS has a private stoppable player and bounds text',async()=>{
 let text='',stopped=0,destroyed=0,aborted=0;const v=new Voice({Utterance:class{constructor(t){text=t;}},synthesis:{synthesize:async()=>({abort(){aborted++;},finished:Promise.resolve()})},Player:class{play(){}stop(){stopped++;}destroy(){destroyed++;}}});
 assert.equal(await v.play('x'.repeat(2000)),true);assert.equal(text.length,1200);v.dispose();assert.equal(stopped,1);assert.equal(destroyed,1);assert.equal(aborted,1);
});
function page(m,storage=new Memory()){
 globalThis.localStorage=storage;
 const ink=fs.readFileSync(new URL('../pages/loki/index.ink',import.meta.url),'utf8');const code=ink.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\r?$/gm,'').replace('export default','return');
 const p=new Function('LanguageModel','Conversation','Store','QUICK_PROMPTS','Voice','storageMessage',code)(m.api,Conversation,Store,QUICK_PROMPTS,Voice,storageMessage);p.data=structuredClone(p.data);p.setData=patch=>Object.assign(p.data,patch);p.onLoad();return p;
}
test('page does not send on load or template selection and all bindings exist',()=>{
 const m=model(),p=page(m);assert.equal(m.availabilityCalls,0);p.quickPrompt({currentTarget:{dataset:{id:'plan'}}});assert.ok(p.data.message);assert.equal(m.inputs.length,0);
 const ink=fs.readFileSync(new URL('../pages/loki/index.ink',import.meta.url),'utf8');for(const match of ink.matchAll(/bind(?:tap|input)="([^"]+)"/g))assert.equal(typeof p[match[1]],'function',match[1]);p.onUnload();
});
test('page keeps typed draft unless replacement is confirmed',()=>{
 const p=page(model());p.data.message='入力途中';p.offerDraft('音声の結果');assert.equal(p.data.message,'入力途中');assert.equal(p.data.confirmReplace,'音声の結果');p.acceptDraft();assert.equal(p.data.message,'音声の結果');p.onUnload();
});
test('page keeps note input if quota fails',()=>{const mem=new Memory(),p=page(model(),mem);p.data.noteDraft='消さない文章';mem.fail=true;p.saveNote();assert.equal(p.data.noteDraft,'消さない文章');assert.ok(p.data.notice.includes('保存できません'));p.onUnload();});
test('page saving notes or using them as drafts never sends',()=>{
 const m=model(),p=page(m);p.data.noteDraft='予定の資料';p.saveNote();assert.equal(p.data.notes.length,1);p.useNote({currentTarget:{dataset:{id:p.data.notes[0].id}}});assert.ok(p.data.message.includes('<保存メモ>'));assert.equal(m.inputs.length,0);assert.equal(m.availabilityCalls,0);p.onUnload();
});
test('page hide cancels its active request without resending on show',async()=>{
 const gate=deferred(),m=model();m.api.availability=()=>gate.promise;const p=page(m);p.data.message='質問';const pending=p.send();p.onHide();gate.resolve('available');await pending;p.onShow();assert.equal(m.creates.length,0);assert.equal(p.data.rows[0].state,'STOPPED');p.onUnload();
});
test('page requires confirmation to enable history and hides model error details',async()=>{
 const p=page(model([{done:false,value:'途中'},{done:true}]));p.toggleRemember();assert.equal(p.store.state.remember,false);p.confirmRemember();assert.equal(p.store.state.remember,true);p.data.message='質問';await p.send();assert.equal(p.store.state.history.length,1);assert.equal(p.data.selected.confirmed,true);p.onUnload();
});
