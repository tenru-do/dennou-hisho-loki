import {systemPrompt} from './persona.js';
const terminal = new Set(['ANSWERED','FAILED','STOPPED']);
export const isTerminal = state => terminal.has(state);
export function contextFor(rows,topic){
 const selected=[];let budget=10000;
 for(const r of rows.slice().reverse()){
  if(r.state!=='ANSWERED'||r.topic!==topic||r.restored||!r.answer)continue;
  const n=r.question.length+r.answer.length;if(n>budget)break;
  selected.unshift({role:'user',content:r.question},{role:'assistant',content:r.answer});budget-=n;if(selected.length>=12)break;
 }
 return selected;
}
export class Conversation {
 constructor({languageModel,onChange=()=>{},onComplete=()=>{},now=()=>globalThis.performance?.now?.()??Date.now(),wallNow=()=>Date.now(),wait=ms=>new Promise(r=>setTimeout(r,ms)),setTimer=setTimeout,clearTimer=clearTimeout,timeout=90000}){
  Object.assign(this,{languageModel,onChange,onComplete,now,wallNow,wait,setTimer,clearTimer,timeout});this.rows=[];this.active=null;this.topic=0;this.serial=0;this.disposed=false;
 }
 emit(){this.onChange(this.rows.map(r=>({...r})),!!this.active);}
 restore(rows){if(!this.rows.length)this.rows=rows.map(r=>({...r,restored:true,topic:-1}));this.emit();}
 release(job){try{job.stream?.cancel();}catch{}try{job.session?.destroy();}catch{}if(job.timer!==undefined)this.clearTimer(job.timer);}
 stop(state='STOPPED',reason='回答待ちを中止しました。送信済みデータや利用料金は取り消せません。'){
  const job=this.active;if(!job)return;this.active=null;job.row.state=state;job.row.reason=reason;this.release(job);this.emit();
 }
 newTopic(){if(this.active)return false;this.topic++;return true;}
 dispose(){this.disposed=true;this.stop();}
 async send(question,style='short'){
  question=typeof question==='string'?question.trim():'';
  if(this.disposed||this.active||!question||question.length>2400)return false;
  const row={id:'turn-'+this.wallNow().toString(36)+'-'+(++this.serial),question,answer:'',state:'PREPARING',topic:this.topic,at:this.wallNow(),firstTextMs:null,elapsedMs:null,reason:''};
  const prompts=[{role:'system',content:systemPrompt(style)},...contextFor(this.rows,this.topic)];
  const job={row,start:this.now()};this.active=job;this.rows=[...this.rows.slice(-39),row];
  job.timer=this.setTimer(()=>{if(this.active===job)this.stop('FAILED','回答待ちが長いため停止しました。自動で再送していません。');},this.timeout);this.emit();
  const current=()=>this.active===job&&!this.disposed;
  try{
   const availability=await this.languageModel.availability();if(!current())return false;
   if(availability!=='available')throw Error('MODEL_UNAVAILABLE');
   const session=await this.languageModel.create({initialPrompts:prompts});
   if(!current()){try{session.destroy();}catch{}return false;}
   job.session=session;row.state='RUNNING';job.stream=session.promptStreaming(question);this.emit();
   while(current()){
    const chunk=await job.stream.read();if(!current())return false;
    if(chunk.value!==undefined){
     if(typeof chunk.value!=='string')throw Error('INVALID_CHUNK');
     if(row.answer.length+chunk.value.length>12000)throw Error('OUTPUT_LIMIT');
     row.answer+=chunk.value;
     if(row.answer.trim()&&row.firstTextMs===null)row.firstTextMs=Math.max(0,Math.round(this.now()-job.start));
     this.emit();
    }
    if(chunk.done){
     if(!row.answer.trim())throw Error('EMPTY_ANSWER');
     row.state='ANSWERED';row.elapsedMs=Math.max(0,Math.round(this.now()-job.start));this.active=null;this.release(job);this.emit();this.onComplete(this.rows.map(r=>({...r})));return true;
    }
    if(!chunk.value)await this.wait(80);
   }
  }catch(error){
   if(!current())return false;
   const reason=error?.message==='MODEL_UNAVAILABLE'?'AIUIのモデルを利用できません。AIUI側のモデル設定と接続を確認してください。':error?.message==='OUTPUT_LIMIT'?'表示上限に達したため停止しました。途中の文章は未確定です。':'回答を完了できませんでした。途中の文章は未確定です。接続やAIUIのモデル設定を確認してください。自動で再送していません。';
   this.stop('FAILED',reason);
  }
  return false;
 }
}
