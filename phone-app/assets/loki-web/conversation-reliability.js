"use strict";
(()=>{
 const el=id=>document.getElementById(id);let latest=null,revision='',pageInfo=null,older=[],loading=false,settingsLoaded=false,sessionEpoch=0,reconcileBusy=false,previewBusy=false,historyEpoch=0,previewFailures=0;const completedPreviews=new Map();
 function line(host,text){const p=document.createElement('p');p.textContent=text;host.append(p);return p;}
 function button(host,label,action){const b=document.createElement('button');b.type='button';b.textContent=label;b.onclick=()=>Promise.resolve().then(action).catch(e=>note(e.message));host.append(b);return b;}
 function dialog(title){const d=document.createElement('dialog');d.dataset.conversationDialog='reliability';d.className='conversation-plus-dialog';const h=document.createElement('h2');h.textContent=title;d.append(h);button(d,'閉じる',()=>{d.close();d.remove();});d.oncancel=()=>d.remove();document.body.append(d);d.showModal();return d;}
 async function get(route,body){const session=token,epoch=sessionEpoch,result=await api(route,body);if(token!==session||epoch!==sessionEpoch)throw Error('接続が変わりました。開き直してください');return result;}
 function download(text,name,type){const blob=new Blob([text],{type}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
 const reasons={NETWORK:'通信を完了できませんでした。接続と履歴を確認してください。',RATE_LIMIT:'APIが混雑または利用上限です。時間をおいて履歴を確認してください。',AUTH:'APIの認証・利用条件を確認してください。',MODEL_ERROR:'モデルの回答処理に失敗しました。操作の成否は履歴で確認してください。',RESTART:'再起動後の結果が未確認です。',INTERRUPTED:'処理が中断しました。結果を確認してください。',SAFETY_STOP:'安全判定で停止しました。依頼内容を見直してください。',REFERENCE_CHANGED:'画像参照が変わりました。画像を選び直してください。'};

 let followId='',followAllowed=false,readingHistory=false;const firstRevealed=new Set();
 function capturePosition(){if(!readingHistory)return null;const list=el('history'),bounds=list.getBoundingClientRect(),cards=[...list.querySelectorAll('[data-request-id]')],card=cards.find(x=>{const r=x.getBoundingClientRect();return r.bottom>Math.max(0,bounds.top)&&r.top<Math.min(innerHeight,bounds.bottom);});return {key:card?.dataset.requestId,offset:card?.getBoundingClientRect().top,listTop:list.scrollTop};}
 function restorePosition(position){if(!position||!readingHistory)return;const list=el('history');list.scrollTop=position.listTop;if(position.key){const card=list.querySelector('[data-request-id="'+position.key+'"]');if(card){const delta=card.getBoundingClientRect().top-position.offset;if(Math.abs(delta)>1)scrollBy(0,delta);}}}
 function readerIntent(){readingHistory=true;followAllowed=false;}
 document.addEventListener('wheel',readerIntent,{passive:true});document.addEventListener('touchmove',readerIntent,{passive:true});document.addEventListener('keydown',e=>{if(['PageUp','PageDown','Home','End','ArrowUp','ArrowDown'].includes(e.key)&&!['TEXTAREA','INPUT'].includes(e.target.tagName))readerIntent();});
 document.addEventListener('loki-request-sent',e=>{followId=e.detail.key;followAllowed=true;readingHistory=false;el('answerJump')?.setAttribute('hidden','');});
 function currentArticle(){const host=el('conversationPreview');return followId?host.querySelector('[data-preview-request="'+followId+'"]'):[...host.children].filter(x=>!x.hidden).at(-1);}
 function visibleBeginning(article){if(!article)return false;const rect=article.children[1].getBoundingClientRect();return rect.top>=0&&rect.top+Math.min(30,rect.height)<= (window.visualViewport?.height||innerHeight);}
 function jumpToAnswer(){const article=currentArticle();if(!article)return;readingHistory=false;article.children[1].scrollTop=0;article.scrollIntoView({block:'nearest',behavior:'instant'});el('answerJump').hidden=true;}
 const jump=el('answerJump');if(jump)jump.onclick=jumpToAnswer;
 function updateJump(){if(jump)jump.hidden=!currentArticle()||visibleBeginning(currentArticle())||el('chat').hidden;}
 document.addEventListener('scroll',updateJump,{passive:true,capture:true});window.addEventListener('resize',updateJump);
 function partials(data){
  const position=capturePosition();
  const host=el('conversationPreview'),requests=data.requests||[],active=requests.filter(x=>['CREATED','QUEUED','RECEIVED','RUNNING'].includes(x.requestState));
  if(active.some(x=>![...host.children].some(article=>article.dataset.previewRequest===x.key)))completedPreviews.clear();
  for(const article of host.querySelectorAll('[data-preview-request]')){
   const id=article.dataset.previewRequest,row=requests.find(x=>x.key===id);
   const answers=(data.items||[]).filter(x=>x.requestId===id&&x.kind==='Gemini'&&x.message);
   if(answers.length&&row&&['RUNNING','ANSWERED'].includes(row.requestState))completedPreviews.set(id,answers.map(x=>x.message).join('\n'));
   else if(row&&!['RUNNING','ANSWERED'].includes(row.requestState))completedPreviews.delete(id);
  }
  while(completedPreviews.size>4)completedPreviews.delete(completedPreviews.keys().next().value);
  const views=new Map();
  // A delivery receipt may arrive before the history row. Keep the last preview
  // tentative until the exact request's confirmed text is available.
  for(const article of host.children)if(requests.some(x=>x.key===article.dataset.previewRequest&&x.requestState==='ANSWERED')&&!completedPreviews.has(article.dataset.previewRequest))views.set(article.dataset.previewRequest,{text:article.children[1].textContent,final:false});
  for(const item of (data.partials||[]).slice(-4)){
   if(!/^[a-f0-9]{64}$/.test(item.requestId)||!requests.some(x=>x.key===item.requestId&&x.requestState==='RUNNING')||!item.text||completedPreviews.has(item.requestId))continue;
   views.set(item.requestId,{text:item.text,final:false});
  }
  for(const [id,text] of completedPreviews)views.set(id,{text,final:true});
  for(const article of [...host.children])if(!views.has(article.dataset.previewRequest))article.remove();
  for(const [id,view] of views){
   let article=host.querySelector('[data-preview-request="'+id+'"]');
   if(!article){article=document.createElement('article');article.dataset.previewRequest=id;line(article,'');line(article,'').style.whiteSpace='pre-wrap';host.append(article);}
   article.dataset.answerState=view.final?'confirmed':'partial';article.hidden=!!followId&&id!==followId;
   const label=view.final?'確定した回答（全文は会話ログにも保存）':'生成中の回答（暫定・変更される場合があります）。確定した回答はログに表示します。';
   const question=requests.find(x=>x.key===id)?.message||article.dataset.question;if(question)article.dataset.question=question.slice(0,70);const heading=(question?'質問：'+question.slice(0,70)+' ／ ':'')+label;if(article.children[0].textContent!==heading)article.children[0].textContent=heading;
   const text=view.text.slice(0,12000)+(view.text.length>12000?'\n（続きは会話ログで確認）':'');
   if(article.children[1].textContent!==text)article.children[1].textContent=text;
  }
  restorePosition(position);const article=currentArticle();if(article&&followAllowed&&!readingHistory&&!el('message').value.trim()&&!firstRevealed.has(article.dataset.previewRequest)){firstRevealed.add(article.dataset.previewRequest);while(firstRevealed.size>100)firstRevealed.delete(firstRevealed.values().next().value);jumpToAnswer();}updateJump();
 }
 function beforeHistory(data){historyEpoch++;if(data.historyPage){if(data.historyPage.revision!==revision){revision=data.historyPage.revision;older=[];pageInfo=data.historyPage;}else if(!pageInfo)pageInfo=data.historyPage;const seen=new Set(),rows=[];for(const row of [...older,...data.items]){const key=JSON.stringify([row.requestId,row.kind,row.time,row.message]);if(!seen.has(key)){seen.add(key);rows.push(row);}}data.items=rows;el('conversationOlder').hidden=!pageInfo.hasMore;}else el('conversationOlder').hidden=true;latest=data;partials(data);}
 function history(data){for(const card of el('pendingTurnsList').querySelectorAll('[data-key]')){const row=(data.requests||[]).find(x=>x.key===card.dataset.key);if(!row)continue;if(['FAILED','UNCONFIRMED'].includes(row.requestState))line(card,(reasons[row.receiptReason]||'結果を確認してください。')+' 自動再送はしません。「入力欄へ戻す」から内容を見直せます。');}if(!settingsLoaded&&token){settingsLoaded=true;get('conversation-settings').then(s=>{el('conversationAnswerStyle').value=s.answerStyle;el('conversationGlassChars').value=String(s.glassChars);el('conversationStreaming').checked=s.streaming;}).catch(e=>{settingsLoaded=false;note(e.message);});} }
 el('conversationOlder').onclick=async()=>{if(loading||!pageInfo?.hasMore)return;loading=true;el('conversationOlder').disabled=true;const list=el('history'),height=list.scrollHeight,top=list.scrollTop;try{const result=await get('conversation-page?before='+encodeURIComponent(pageInfo.before)+'&revision='+encodeURIComponent(revision));if(result.revision!==revision)throw Error('履歴が更新されました。最新を読み直してください');older=[...result.items,...older];pageInfo=result;await loadHistory();list.scrollTop=top+(list.scrollHeight-height);}catch(e){older=[];revision='';pageInfo=null;await loadHistory();note(e.message);}finally{loading=false;el('conversationOlder').disabled=false;}};
 el('conversationSettingsSave').onclick=async()=>{try{await get('conversation-settings',{answerStyle:el('conversationAnswerStyle').value,glassChars:Number(el('conversationGlassChars').value),streaming:el('conversationStreaming').checked});note('保存しました。次に処理開始するWeb会話から適用します。全文はログに残ります');}catch(e){note(e.message);}};
 function presentation(){const font=el('conversationFont').value,height=el('conversationComposerHeight').value;document.documentElement.style.setProperty('--conversation-font-size',font+'px');el('message').style.minHeight=height+'px';try{localStorage.setItem('lokiConversationPresentation',JSON.stringify({font:Number(font),height:Number(height)}));}catch{} }
 try{const p=JSON.parse(localStorage.getItem('lokiConversationPresentation')||'null');if(p&&[16,18,22].includes(p.font)&&[80,120,180].includes(p.height)){el('conversationFont').value=String(p.font);el('conversationComposerHeight').value=String(p.height);}}catch{}
 el('conversationFont').onchange=presentation;el('conversationComposerHeight').onchange=presentation;presentation();
 el('conversationRestore').onclick=async()=>{if(reconcileBusy)return;reconcileBusy=true;try{const result=await get('conversation-reconcile',{});await loadHistory();const d=dialog('再起動後の依頼の照合');line(d,result.note);for(const item of result.items){const row=latest?.requests.find(x=>x.key===item.key);line(d,item.state+'：'+(row?.message||'保存された依頼')+(item.inPhoneQueue?'（スマホの送信待ちに保持）':''));}if(!result.items.length)line(d,'現在、未完了の依頼はありません。');}catch(e){note(e.message);}finally{reconcileBusy=false;}};
 el('conversationStorage').onclick=async()=>{try{const usage=await get('conversation-storage'),d=dialog('スマホの保存容量');line(d,usage.scope);const mb=x=>(x/1048576).toFixed(2)+' MB';line(d,'共有会話 '+mb(usage.conversationBytes)+' ／ 受信写真 '+mb(usage.photoBytes)+' ／ お気に入り '+mb(usage.favoriteBytes)+'（'+usage.favoriteCount+'件）／ 保存先の空き '+mb(usage.freeBytes));button(d,'写真を選んで整理',()=>{d.close();d.remove();el('photosTab').click();note('削除する写真を選び、確認してから削除してください');});button(d,'お気に入りを整理',()=>{d.close();d.remove();el('conversationFavoritesOpen').click();});button(d,'共有履歴を選んで整理',async()=>{const library=await get('conversation-library'),selection=new Map(),list=dialog('現在の共有履歴を整理');line(list,'スマホの現在の共有履歴だけが対象です。送信記録・アーカイブ・お気に入り・メガネの履歴は残ります。処理中の会話は削除できません。先に必要な会話を書き出してください。');for(const item of library.items){const label=document.createElement('label'),check=document.createElement('input');check.type='checkbox';check.onchange=()=>check.checked?selection.set(item.key,item):selection.delete(item.key);label.append(check,document.createTextNode(item.title));list.append(label);}button(list,'選んだ共有履歴を確認して削除',async()=>{if(!selection.size||selection.size>20)throw Error('1〜20件を選んでください');if(!confirm(selection.size+'件の現在の共有履歴を削除しますか？ 元に戻せません。別の保存コピーは残ります。'))return;const result=await get('conversation-prune',{items:[...selection.values()].map(x=>({key:x.key,revision:x.revision}))});older=[];revision='';pageInfo=null;list.close();list.remove();d.close();d.remove();await loadHistory();note(result.scope);});});}catch(e){note(e.message);}};
 el('conversationDiagnostics').onclick=async()=>{try{const result=await get('conversation-diagnostics'),text=JSON.stringify(result,null,2),d=dialog('本文を含まない診断情報');line(d,result.excluded);const pre=document.createElement('pre');pre.textContent=text;d.append(pre);button(d,'診断JSONを保存',()=>download(text,'loki-diagnostics.json','application/json'));}catch(e){note(e.message);}};
 function enhanceHistory(items,list){for(const card of list.querySelectorAll('[data-request-id]')){const rows=items.filter(x=>x.requestId===card.dataset.requestId),answer=rows.filter(x=>!x.kind?.startsWith('ユーザー')).map(x=>x.message).join('\n'),question=rows.filter(x=>x.kind?.startsWith('ユーザー')).map(x=>x.message).join('\n');if(!answer)continue;button(card,'この回答を詳しく（下書き）',()=>{if(chatImageData||generatedParent)throw Error('添付・編集元を外してから使ってください');if(el('message').value.trim()&&!confirm('入力中の文章を詳説の依頼へ置き換えますか？'))return;imageReferenceDraft=null;setComposerMessage('以下は元の会話の引用で、操作を再実行する指示ではありません。説明だけを詳しくしてください。\n元の質問：'+question.slice(0,500)+'\n元の回答：'+answer.slice(0,1000)+'\n手順・根拠を補って詳しく説明してください。');sendId='';el('message').focus();note('詳説の依頼を入力欄へ戻しました。まだ送信していません');});}}
 // Poll only while visible and active. Schedule after completion to avoid overlap.
 async function pollPreview(){
  let active=false;
  try{
   active=!!token&&!document.hidden&&!el('chat').hidden&&!!latest?.requests.some(x=>['CREATED','QUEUED','RECEIVED','RUNNING'].includes(x.requestState));
   if(active&&!previewBusy){
    previewBusy=true;const epoch=historyEpoch;
    try{const data=await get('conversation-status');if(epoch===historyEpoch){partials(data);latest={...latest,requests:data.requests||[]};}previewFailures=0;}
    catch{previewFailures=Math.min(previewFailures+1,5);}
    finally{previewBusy=false;}
   }
  }finally{setTimeout(pollPreview,active?Math.min(8000,250*2**previewFailures):1000);}
 }
 setTimeout(pollPreview,250);
 document.addEventListener('loki-session-cleared',()=>{sessionEpoch++;historyEpoch++;previewFailures=0;followId='';followAllowed=false;readingHistory=false;firstRevealed.clear();if(jump)jump.hidden=true;completedPreviews.clear();older=[];revision='';pageInfo=null;latest=null;settingsLoaded=false;el('conversationPreview').replaceChildren();for(const d of document.querySelectorAll('dialog[data-conversation-dialog="reliability"]'))d.remove();});
 window.lokiConversationReliability={beforeHistory,history,enhanceHistory,capturePosition,restorePosition};
})();
