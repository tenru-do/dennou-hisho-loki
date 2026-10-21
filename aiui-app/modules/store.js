const KEY = 'dennou-hisho-loki.aiui.v1';
const clean = (text,limit) => typeof text === 'string' ? text.slice(0,limit) : '';
export const defaults = () => ({schema:1,revision:0,remember:false,style:'short',notes:[],history:[]});
function decode(raw) {
 if(raw===null||raw===undefined||raw==='')return defaults();
 if(typeof raw!=='string'||raw.length>400000)throw Error('STORAGE_INVALID');
 const d=JSON.parse(raw);
 if(d.schema!==1||!Number.isSafeInteger(d.revision)||d.revision<0||!Array.isArray(d.notes)||!Array.isArray(d.history))throw Error('STORAGE_INVALID');
 return {schema:1,revision:d.revision,remember:d.remember===true,style:d.style==='detailed'?'detailed':'short',
  notes:d.notes.slice(-30).filter(x=>x&&typeof x.id==='string'&&/^[a-z0-9-]{1,80}$/.test(x.id)&&typeof x.text==='string'&&x.text.trim()).map(x=>({id:x.id,text:clean(x.text,1000),at:Number.isFinite(x.at)?x.at:0})),
  history:d.remember===true?d.history.slice(-20).filter(x=>x&&typeof x.id==='string'&&/^[a-z0-9-]{1,80}$/.test(x.id)&&x.state==='ANSWERED'&&typeof x.question==='string'&&typeof x.answer==='string').map(x=>({id:x.id,state:'ANSWERED',question:clean(x.question,2400),answer:clean(x.answer,12000),at:Number.isFinite(x.at)?x.at:0,restored:true})):[]};
}
export class Store {
 constructor(storage){this.storage=storage;this.state=defaults();this.error='';try{this.state=decode(storage.getItem(KEY));}catch{this.error='保存領域を読み取れません。既存内容を上書きせず、会話は一時表示だけで使えます。';}}
 commit(next){
  if(this.error)throw Error('STORAGE_UNAVAILABLE');
  let current;try{current=decode(this.storage.getItem(KEY));}catch{throw Error('STORAGE_UNAVAILABLE');}
  if(current.revision!==this.state.revision)throw Error('STORAGE_CONFLICT');
  const value=decode(JSON.stringify({...next,schema:1,revision:current.revision+1}));
  this.storage.setItem(KEY,JSON.stringify(value));this.state=value;return value;
 }
 savePreferences(remember,style,history){return this.commit({...this.state,remember:remember===true,style,history:remember===true?history.filter(x=>x.state==='ANSWERED').slice(-20):[]});}
 saveHistory(history){if(this.state.remember)return this.savePreferences(true,this.state.style,history);}
 addNote(text,id,at){text=clean(text,1001).trim();if(!text||text.length>1000)throw Error('NOTE_LENGTH');if(this.state.notes.length>=30)throw Error('NOTE_LIMIT');return this.commit({...this.state,notes:[...this.state.notes,{id,text,at}]});}
 removeNote(id){if(!this.state.notes.some(x=>x.id===id))throw Error('NOTE_MISSING');return this.commit({...this.state,notes:this.state.notes.filter(x=>x.id!==id)});}
}
export function storageMessage(error){return ({STORAGE_CONFLICT:'別の画面で保存内容が更新されました。開き直してから保存してください。',NOTE_LENGTH:'メモは1〜1000文字で入力してください。',NOTE_LIMIT:'メモは30件までです。必要なものを確認して整理してください。',NOTE_MISSING:'対象のメモが見つかりません。'})[error?.message]||'保存できませんでした。入力は残っています。空き容量や保存機能を確認してください。';}
