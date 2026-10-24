'use strict';
(()=>{
 const eligible=row=>['Gemini','直接回答','Assistant','トピック要約'].includes(row?.kind)&&row.time>0&&typeof row.message==='string'&&row.message.trim();
 const source=row=>({kind:row.kind,time:row.time,message:row.message,requestId:row.requestId||'',legacy:!!row.legacy});
 function line(host,value){const p=document.createElement('p');p.textContent=value;host.append(p);return p;}
 function button(host,label,action){const b=document.createElement('button');b.type='button';b.textContent=label;b.onclick=()=>Promise.resolve().then(action).catch(e=>note(e.message));host.append(b);return b;}
 function modal(title){const d=document.createElement('dialog');d.dataset.answerCorrection='true';line(d,title);button(d,'閉じる',()=>{d.close();d.remove();});document.body.append(d);d.showModal();return d;}
 function render(entry){const d=modal('元の回答と訂正');line(d,entry.source.legacy?'元の回答（更新前の保存履歴・抜粋。変更しません）':'元の回答（変更しません）');line(d,entry.source.message);line(d,'利用者の訂正（AIが確認した内容ではありません）');if(!entry.corrections.length)line(d,'まだ訂正はありません');for(const row of entry.corrections){line(d,new Date(row.at).toLocaleString('ja-JP'));line(d,row.text);}button(d,'この情報は違う・訂正を追加',()=>edit(entry));}
 async function open(row){const session=token;const entry=await api('answer-correction-view',{source:source(row)});if(session===token)render(entry);}
 function edit(entry){const session=token,d=modal('回答への訂正');line(d,'元の回答は上書きせず、自分の訂正としてスマホに保存します。AIへは送信しません。');const input=document.createElement('textarea');input.maxLength=2400;input.setAttribute('aria-label','訂正内容');d.append(input);const status=line(d,'');const changeId=newRequestId();const save=button(d,'訂正を保存',async()=>{if(!input.value.trim()){status.textContent='訂正内容を入力してください';return;}save.disabled=true;try{const saved=await api('answer-correction-save',{source:entry.source,revision:entry.revision,changeId,text:input.value});if(session!==token)return;d.close();d.remove();render(saved);}catch(e){if(session===token)status.textContent=e.message+'。入力は残しています。';}finally{if(d.isConnected)save.disabled=false;}});button(d,'最新の訂正を別画面で確認（入力は残す）',()=>open(entry.source));}
 async function library(){const session=token,data=await api('answer-corrections');if(session!==token)return;const d=modal('訂正付き回答');if(!data.items.length)line(d,'訂正付き回答はまだありません');for(const entry of data.items)button(d,'訂正 '+entry.revision+'件 / '+entry.source.message.slice(0,70),()=>render(entry));}
 function attach(row,host){if(eligible(row))button(host,'この情報は違う・訂正を見る',()=>open(row));}
 const host=document.getElementById('pendingTurns');if(host)button(host,'訂正付き回答を開く',library);
 document.addEventListener('loki-session-cleared',()=>{for(const d of document.querySelectorAll('[data-answer-correction]'))d.remove();});window.lokiAnswerCorrections={attach,library};
})();
