'use strict';
const $=id=>document.getElementById(id);
let meetingSignature='', meetingCurrent=null, meetingBlob='', meetingPlayGeneration=0;
let activeTab='photos', meetingControlBusy=false, meetingUploadController=null, meetingUploadId='', meetingImportSignature='';
let token=localStorage.getItem('lokiWebToken')||sessionStorage.getItem('lokiWebToken')||'', offset=0, items=[], total=0, selected=new Set(), generation=0, blobs=[], viewing=-1, largeBlob='', zoom=1, busy=false, historySignature='', sendId='';
const note=text=>{$('notice').textContent=text;};
async function api(path,data,signal){
 const response=await fetch('/web/api/'+path,{method:data===undefined?'GET':'POST',headers:{'X-Loki-Web-Token':token,...(data===undefined?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:JSON.stringify(data),cache:'no-store',signal});
 if(response.status===401){token='';sessionStorage.removeItem('lokiWebToken');localStorage.removeItem('lokiWebToken');showAuth();throw Error('接続の期限が切れました。スマホでコードを発行してください');}
 if(!response.ok){let j;try{j=await response.json();}catch{}throw Error(j?.error||'処理できませんでした（'+response.status+'）');}
 return response.headers.get('Content-Type').startsWith('image/')?response.blob():response.json();
}
function on(id,action){$(id).addEventListener('click',()=>Promise.resolve().then(action).catch(e=>note(e.message)));}
function showAuth(){const connected=!!token;$("meetings").hidden=!connected||activeTab!=="meetings";if(!connected){if(meetingUploadController)meetingUploadController.abort();$("meetingFile").value="";$("meetingImportList").replaceChildren();$("meetingControlStatus").textContent="接続していません";$("meetingUploadStatus").textContent="";meetingImportSignature="";meetingPlayGeneration++;$("meetingAudio").pause();$("meetingAudio").removeAttribute("src");$("meetingList").replaceChildren();$("meetingText").textContent="";$("meetingPlayer").hidden=true;if(meetingBlob)URL.revokeObjectURL(meetingBlob);meetingBlob="";meetingCurrent=null;meetingSignature="";}$('pair').hidden=connected;$('console').hidden=!connected;$('logout').hidden=!connected;if(!connected){generation++;blobs.forEach(URL.revokeObjectURL);blobs=[];$('grid').replaceChildren();closeViewer();$('history').replaceChildren();$('outbox').replaceChildren();historySignature='';}}
function caption(item){const m=item.metadata;return new Date(item.time).toLocaleString('ja-JP')+(m?.available?' · '+m.latitude.toFixed(5)+', '+m.longitude.toFixed(5):' · 撮影場所なし');}
function updateSelected(){$('selectedCount').textContent=selected.size+'枚選択';$('saveSelected').disabled=$('deleteSelected').disabled=!selected.size||busy;for(const card of $('grid').children){const checked=selected.has(card.dataset.id);card.classList.toggle('selected',checked);card.querySelector('input').checked=checked;}}
async function loadPhotos(){
 const epoch=++generation;const result=await api('photos?offset='+offset);if(epoch!==generation)return;
 offset=result.offset;total=result.total;items=result.items;selected.clear();blobs.forEach(URL.revokeObjectURL);blobs=[];$('grid').replaceChildren();
 $('count').textContent=total+'枚';$('page').textContent=total?(offset+1)+'〜'+(offset+items.length)+' / '+total+'枚':'写真がありません';$('previous').disabled=offset===0;$('next').disabled=offset+items.length>=total;
 const jobs=[];
 for(const [index,item] of items.entries()){
  const card=document.createElement('article');card.className='card';card.dataset.id=item.id;
  const image=document.createElement('img');image.alt='記録写真 '+new Date(item.time).toLocaleString('ja-JP');image.addEventListener('click',()=>openViewer(index).catch(e=>note(e.message)));
  const label=document.createElement('label'), check=document.createElement('input');check.type='checkbox';check.addEventListener('change',()=>{check.checked?selected.add(item.id):selected.delete(item.id);updateSelected();});label.append(check,document.createTextNode('選択'));
  const text=document.createElement('p');text.className='caption';text.textContent=caption(item);card.append(image,label,text);$('grid').append(card);
  jobs.push(async()=>{try{const blob=await api('photo?id='+encodeURIComponent(item.id)+'&size=thumb');if(epoch!==generation)return;const url=URL.createObjectURL(blob);blobs.push(url);image.src=url;}catch(e){if(epoch===generation)text.textContent='読み込み失敗 · '+e.message;}});
 }
 updateSelected();let cursor=0;await Promise.all(Array.from({length:2},async()=>{while(cursor<jobs.length&&epoch===generation)await jobs[cursor++]();}));
}
function closeViewer(){viewing=-1;if(largeBlob)URL.revokeObjectURL(largeBlob);largeBlob='';$('largePhoto').removeAttribute('src');if($('viewer').open)$('viewer').close();}
async function openViewer(index){
 if(index<0||index>=items.length)return;viewing=index;const item=items[index];$('photoInfo').textContent=caption(item);$('largePhoto').removeAttribute('src');$('prevPhoto').disabled=index===0;$('nextPhoto').disabled=index===items.length-1;if(!$('viewer').open)$('viewer').showModal();
 const blob=await api('photo?id='+encodeURIComponent(item.id));if(viewing!==index||items[index]?.id!==item.id)return;
 if(largeBlob)URL.revokeObjectURL(largeBlob);largeBlob=URL.createObjectURL(blob);$('largePhoto').src=largeBlob;zoom=1;applyZoom();
}
function applyZoom(){const image=$('largePhoto');image.classList.toggle('zoomed',zoom>1);image.style.width=zoom>1?(Math.min(image.naturalWidth||1000,$('imageViewport').clientWidth)*zoom)+'px':'';}
async function saveItem(item){const blob=await api('photo?id='+encodeURIComponent(item.id));const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='Loki-'+item.id+'.jpg';document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),30000);}
async function deleteIds(ids){if(busy||!ids.length)return;if(!confirm(ids.length+'枚をスマホのロキ保存分から削除します。よろしいですか？'))return;busy=true;updateSelected();const oldIndex=viewing;try{await api('delete',{ids});await loadPhotos();if(oldIndex>=0&&viewing>=0&&$('viewer').open){if(items.length)await openViewer(Math.min(oldIndex,items.length-1));else closeViewer();}note(ids.length+'枚を削除しました');}finally{busy=false;updateSelected();}}
async function loadHistory(){if(!token||$('chat').hidden)return;const result=await api('history');const signature=JSON.stringify(result);if(signature===historySignature)return;historySignature=signature;
 const list=$('history'),nearEnd=list.scrollHeight-list.scrollTop-list.clientHeight<100;list.replaceChildren();
 for(const entry of result.items){const div=document.createElement('div');div.className='bubble'+(entry.kind?.startsWith('ユーザー')?' user':'');const small=document.createElement('small');small.textContent=entry.kind+' · '+new Date(entry.time).toLocaleString('ja-JP');div.append(small,document.createTextNode(entry.message));list.append(div);}
 if(nearEnd)list.scrollTop=list.scrollHeight;$('outbox').replaceChildren();for(const entry of result.requests.slice(-20).reverse()){const p=document.createElement('p');const delivered=result.items.some(h=>h.kind?.startsWith('ユーザー')&&h.message===entry.message&&h.time>=entry.time);p.textContent=entry.device+' · '+(delivered?'共有履歴に反映済み':entry.status)+' · '+entry.message;$('outbox').append(p);}
}
const invitation=new URLSearchParams(location.hash.slice(1)).get('join')||'';
if(location.hash)history.replaceState(null,'',location.pathname+location.search);
$('device').value=/iPad|Tablet|Android/i.test(navigator.userAgent)?'タブレット':/Windows/i.test(navigator.userAgent)?'Windows PC':'ブラウザ';
async function connectBrowser(ticket=''){
 const button=$('pairForm').querySelector('button');button.disabled=true;note('スマホでこの端末の接続を許可してください…');
 try{const remember=$('remember').checked,result=await api('connect',{invitation:ticket,name:$('device').value,remember});const registered=result.remember===undefined?remember:result.remember;token=result.token;localStorage.removeItem('lokiWebToken');sessionStorage.removeItem('lokiWebToken');(registered?localStorage:sessionStorage).setItem('lokiWebToken',token);showAuth();await loadPhotos();note(registered?'接続しました。この端末は登録済みです':'接続しました。このタブだけで利用します');}
 catch(err){note(err.message);}finally{button.disabled=false;}
}
$('pairForm').addEventListener('submit',e=>{e.preventDefault();connectBrowser();});
on('logout',async()=>{await api('logout',{});token='';sessionStorage.removeItem('lokiWebToken');localStorage.removeItem('lokiWebToken');showAuth();note('接続を解除しました');});
on('refresh',loadPhotos);on('import',async()=>{if(!confirm('グラスの連続撮影を停止し、保存した写真をスマホへ取り込みます。続けますか？'))return;await api('import',{});note('グラスへ取り込み要求を送りました。転送中は一覧を更新してください。');});
on('selectPage',()=>{selected=new Set(items.map(x=>x.id));updateSelected();});on('clearSelection',()=>{selected.clear();updateSelected();});on('deleteSelected',()=>deleteIds([...selected]));
on('saveSelected',async()=>{if(busy)return;busy=true;updateSelected();try{for(const item of items.filter(x=>selected.has(x.id)))await saveItem(item);note('JPEGを保存しました。ブラウザのダウンロードを確認してください');}finally{busy=false;updateSelected();}});
on('previous',async()=>{offset=Math.max(0,offset-60);await loadPhotos();});on('next',async()=>{offset+=60;await loadPhotos();});
on('deleteAll',async()=>{const preview=await api('delete-preview');if(!preview.count)return;if(!confirm('スマホのロキ保存分 '+preview.count+'枚をすべて削除します。全ページが対象です。続けますか？'))return;await api('delete-all',{revision:preview.revision});closeViewer();offset=0;await loadPhotos();note('ロキ保存分をすべて削除しました');});
on('closeViewer',closeViewer);$('viewer').addEventListener('cancel',e=>{e.preventDefault();closeViewer();});
on('prevPhoto',()=>openViewer(viewing-1));on('nextPhoto',()=>openViewer(viewing+1));on('zoomIn',()=>{zoom=Math.min(5,zoom+.5);applyZoom();});on('zoomOut',()=>{zoom=Math.max(1,zoom-.5);applyZoom();});on('resetZoom',()=>{zoom=1;applyZoom();});$('largePhoto').addEventListener('dblclick',()=>{zoom=zoom>1?1:2;applyZoom();});
on('savePhoto',()=>viewing>=0?saveItem(items[viewing]):undefined);on('deletePhoto',()=>viewing>=0?deleteIds([items[viewing].id]):undefined);
function showTab(tab){activeTab=tab;for(const name of ['photos','chat','meetings']){$(name).hidden=name!==tab;$(name+'Tab').setAttribute('aria-pressed',String(name===tab));}}
on('photosTab',()=>showTab('photos'));
on('chatTab',async()=>{showTab('chat');await loadHistory();});
on('meetingsTab',async()=>{showTab('meetings');await refreshMeetings();});
$('message').addEventListener('input',()=>{sendId='';});
let composingMessage=false;
$('message').addEventListener('compositionstart',()=>{composingMessage=true;});
$('message').addEventListener('compositionend',()=>{composingMessage=false;});
$('message').addEventListener('keydown',e=>{
 if(e.key!=='Enter'||e.shiftKey||e.isComposing||composingMessage||e.keyCode===229)return;
 e.preventDefault();if(e.repeat||$('send').disabled||!$('message').value.trim())return;
 $('chatForm').requestSubmit();
});

function newRequestId(){
 const rng=globalThis.crypto;
 if(typeof rng?.randomUUID==='function')return rng.randomUUID();
 if(typeof rng?.getRandomValues!=='function')throw Error('このブラウザでは送信IDを作成できません。最新のブラウザで開き直してください');
 return Array.from(rng.getRandomValues(new Uint8Array(16)),v=>v.toString(16).padStart(2,'0')).join('');
}
$('chatForm').addEventListener('submit',async e=>{
 e.preventDefault();if($('send').disabled)return;const message=$('message').value.trim();if(!message)return;
 $('send').disabled=true;let timer;
 try{
  sendId=sendId||newRequestId();const controller=new AbortController();timer=setTimeout(()=>controller.abort(),15000);
  note('送信しています…');await api('send',{message,requestId:sendId},controller.signal);
  if($('message').value.trim()===message){$('message').value='';sendId='';}
  note('グラスへ送信待ちです。返信は共通履歴に表示されます');
 }catch(err){note(err.name==='AbortError'?'送信の確認がタイムアウトしました。接続を確認して再試行してください。同じ内容の再送は重複を防ぎます':err.message||'送信できませんでした');}
 finally{clearTimeout(timer);$('send').disabled=false;}
});
setInterval(()=>{if(!document.hidden)loadHistory().catch(e=>note(e.message));},5000);
showAuth();if(token)loadPhotos().catch(e=>{note(e.message);if(!token)connectBrowser(invitation);});else connectBrowser(invitation);


async function meetingPlay(item,part){const expectedToken=token, expectedGeneration=++meetingPlayGeneration;
 meetingCurrent=item;const select=$('meetingPart');if(select.options.length!==item.parts){select.replaceChildren();for(let i=0;i<item.parts;i++){const o=document.createElement('option');o.value=i;o.textContent=Math.floor(i*10/60)+':'+String(i*10%60).padStart(2,'0');select.append(o);}}
 select.value=part;$('meetingPlayer').hidden=false;
 const response=await fetch('/web/api/meeting-audio?id='+encodeURIComponent(item.id)+'&part='+part,{headers:{'X-Loki-Web-Token':token},cache:'no-store'});if(!response.ok)throw Error('録音を取得できません');const audioBlob=await response.blob();if(!token||token!==expectedToken||expectedGeneration!==meetingPlayGeneration)return;if(meetingBlob)URL.revokeObjectURL(meetingBlob);meetingBlob=URL.createObjectURL(audioBlob);$('meetingAudio').src=meetingBlob;$('meetingPlayStatus').textContent='記録 '+new Date(item.startedAt).toLocaleString()+' · 区間 '+(part+1)+'/'+item.parts;await $('meetingAudio').play();
}
async function loadMeetings(){if(!token||activeTab!=='meetings')return;const expectedToken=token;const result=await api('meetings');if(!token||token!==expectedToken)return;const sig=JSON.stringify(result);if(sig===meetingSignature)return;meetingSignature=sig;const list=$('meetingList');list.replaceChildren();for(const item of result.items){const card=document.createElement('article');const title=document.createElement('p');title.textContent=(item.title?item.title+' · ':'')+new Date(item.startedAt).toLocaleString()+' · '+Math.floor(item.durationMs/60000)+'分 · '+item.reason+' · '+item.analysisStatus;card.append(title);if(item.topics?.length){const details=document.createElement('details');const heading=document.createElement('summary');heading.textContent='TOPICコメント '+item.topics.length+'件';details.append(heading);for(const topic of item.topics){const text=document.createElement('p');text.textContent=Math.floor(topic.audioMs/60000)+'分 '+topic.text;details.append(text);}card.append(details);}const play=document.createElement('button');play.textContent='録音を再生';play.onclick=()=>meetingPlay(item,0).catch(e=>note(e.message));card.append(play);const analyze=document.createElement('button');analyze.textContent=item.analyzing?'文字起こし・要約中':'文字起こし・AI要約（Geminiへ音声を送信）';analyze.disabled=item.analyzing;analyze.onclick=async()=>{analyze.disabled=true;try{await api('meeting-analyze',{id:item.id});note('保存した録音の文字起こしを開始しました。途中で止まった場合は再実行できます');}catch(e){note(e.message);}finally{await loadMeetings();}};card.append(analyze);if(item.summary){const summary=document.createElement('pre');summary.style.whiteSpace='pre-wrap';summary.textContent=item.summary;card.append(summary);const saveSummary=document.createElement('button');saveSummary.textContent='要約を保存';saveSummary.onclick=()=>downloadText(item.summary,(item.title||'会議記録')+'-要約.txt');card.append(saveSummary);}if(item.hasTranscript||item.summary){const transcript=document.createElement('button');transcript.textContent='文字起こしを見る';transcript.onclick=async()=>{const expectedToken=token;try{const res=await fetch('/web/api/meeting-transcript?id='+encodeURIComponent(item.id),{headers:{'X-Loki-Web-Token':token},cache:'no-store'});if(!res.ok)throw Error('文字起こしを取得できません');const text=await res.text();if(token&&token===expectedToken){$('meetingText').textContent=text;downloadText(text,(item.title||'会議記録')+'-文字起こし.txt');}}catch(e){note(e.message);}};card.append(transcript);}list.append(card);}if(!result.items.length)list.textContent='転送済みの会議録音はありません。';}
$('meetingRefresh').onclick=()=>refreshMeetings().catch(e=>note(e.message));$('meetingPart').onchange=()=>{if(meetingCurrent)meetingPlay(meetingCurrent,Number($('meetingPart').value)).catch(e=>note(e.message));};$('meetingAudio').onended=()=>{if(meetingCurrent){const next=Number($('meetingPart').value)+1;if(next<meetingCurrent.parts)meetingPlay(meetingCurrent,next).catch(e=>note(e.message));}};
setInterval(()=>{if(!document.hidden&&token&&activeTab==='meetings')refreshMeetings().catch(e=>note(e.message));},5000);
function downloadText(text,name){const blob=URL.createObjectURL(new Blob([text],{type:'text/plain;charset=utf-8'}));const link=document.createElement('a');link.href=blob;link.download=name.replace(/[\\/:*?"<>|]/g,'_');link.click();setTimeout(()=>URL.revokeObjectURL(blob),1000);}
async function loadMeetingControl(){const expectedToken=token;const result=await api('meeting-control');if(!token||token!==expectedToken)return;const state=result.state,request=result.request;const connected=!!state.connected;let text=connected?(state.recording?state.status:'MTGは停止中'):'グラスの接続が確認できません。ロキとスマホの接続を確認してください';if(request.status)text+=' · 操作：'+request.status;$('meetingControlStatus').textContent=text;$('meetingStart').disabled=meetingControlBusy||!connected||!!state.recording;$('meetingStop').disabled=meetingControlBusy||!connected||!state.recording;$('meetingTransfer').disabled=meetingControlBusy||!connected||!!state.recording;}
async function meetingControl(action){if(meetingControlBusy)return;const mode=Number($('meetingMode').value);if(action==='stop'&&!confirm('録音を保存し、このMTGのTOPIC・連続写真も終了しますか？'))return;if(action==='start'&&!confirm($('meetingMode').selectedOptions[0].textContent+'を開始しますか？'+(mode>0?' TOPIC用の周囲音・カメラ画像をGeminiへ送信します。':'')))return;meetingControlBusy=true;try{await api('meeting-control',{action,mode,requestId:newRequestId()});$('meetingControlStatus').textContent='グラスへの送信待ちです。実行結果を確認中…';}finally{meetingControlBusy=false;await loadMeetingControl();}}
on('meetingStart',()=>meetingControl('start'));on('meetingStop',()=>meetingControl('stop'));on('meetingTransfer',()=>meetingControl('transfer'));
async function loadMeetingImports(){const expectedToken=token;const result=await api('meeting-import');if(!token||token!==expectedToken)return;const signature=JSON.stringify(result);if(signature===meetingImportSignature)return;meetingImportSignature=signature;const list=$('meetingImportList');list.replaceChildren();for(const item of result.items){if(item.state==='complete'||item.state==='cancelled'||item.state==='expired')continue;const card=document.createElement('article');const p=document.createElement('p');p.textContent=item.title+' · '+item.status+(item.state==='uploading'?' · '+Math.floor(item.next/item.chunks*100)+'%':'');card.append(p);if(item.state!=='converting'){if(item.next===item.chunks){const retry=document.createElement('button');retry.textContent='音声抽出を再試行';retry.onclick=async()=>{try{await api('meeting-import-finish',{id:item.id});await loadMeetingImports();}catch(e){note(e.message);}};card.append(retry);}const cancel=document.createElement('button');cancel.textContent='この読み込みを取り消す';cancel.onclick=async()=>{try{await api('meeting-import-cancel',{id:item.id});await loadMeetingImports();}catch(e){note(e.message);}};card.append(cancel);}list.append(card);}}
async function refreshMeetings(){if(!token||activeTab!=='meetings')return;await Promise.all([loadMeetingControl(),loadMeetings(),loadMeetingImports()]);}
function encodeChunk(bytes){let text='';for(let i=0;i<bytes.length;i+=8192)text+=String.fromCharCode(...bytes.subarray(i,i+8192));return btoa(text).replace(/\+/g,'-').replace(/\//g,'_');}
async function uploadMeetingFile(){if(meetingUploadController)return;const file=$('meetingFile').files[0];if(!file)throw Error('録画・録音ファイルを選択してください');if(file.size>2147483648||file.size===0)throw Error('ファイルは2GB以下にしてください');const expectedToken=token;meetingUploadController=new AbortController();const controller=meetingUploadController;$('meetingUpload').disabled=true;$('meetingUploadCancel').hidden=false;meetingUploadId='';try{const job=await api('meeting-import-begin',{name:file.name,size:file.size},controller.signal);meetingUploadId=job.id;for(let index=0;index<job.chunks;index++){if(!token||token!==expectedToken||controller.signal.aborted)throw new DOMException('転送を中止しました','AbortError');const bytes=new Uint8Array(await file.slice(index*job.chunkSize,Math.min(file.size,(index+1)*job.chunkSize)).arrayBuffer());const digest=await crypto.subtle.digest('SHA-256',bytes);const sha=Array.from(new Uint8Array(digest),n=>n.toString(16).padStart(2,'0')).join('');const packet={id:job.id,index,sha,data:encodeChunk(bytes)};let ack;for(let attempt=0;attempt<3;attempt++){try{ack=await api('meeting-import-chunk',packet,controller.signal);break;}catch(error){if(controller.signal.aborted||!token||attempt===2)throw error;}}if(ack.sha!==sha)throw Error('ファイルの転送ハッシュが一致しません');$('meetingUploadStatus').textContent='転送中：'+Math.floor((index+1)/job.chunks*100)+'%';}await api('meeting-import-finish',{id:job.id},controller.signal);$('meetingUploadStatus').textContent='転送完了。スマホで音声を抽出中です。完了後、保存した会議記録から文字起こし・AI要約を開始できます';$('meetingFile').value='';}catch(error){if(token&&token===expectedToken){$('meetingUploadStatus').textContent=error.name==='AbortError'?'転送を中止しました':error.message;if(meetingUploadId&&controller.signal.aborted){try{await api('meeting-import-cancel',{id:meetingUploadId});}catch{}}}throw error;}finally{meetingUploadController=null;$('meetingUpload').disabled=false;$('meetingUploadCancel').hidden=true;if(token&&token===expectedToken)await refreshMeetings();}}
on('meetingUpload',uploadMeetingFile);on('meetingUploadCancel',()=>{if(meetingUploadController)meetingUploadController.abort();});
