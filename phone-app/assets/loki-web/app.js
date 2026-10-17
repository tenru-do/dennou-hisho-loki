'use strict';
const $=id=>document.getElementById(id);
const PHOTO_PAGE_SIZE=200;
let meetingSignature='', meetingCurrent=null, meetingBlob='', meetingPlayGeneration=0;
let activeTab='photos', meetingControlBusy=false, meetingUploadController=null, meetingUploadId='', meetingImportSignature='';
let token=localStorage.getItem('lokiWebToken')||sessionStorage.getItem('lokiWebToken')||'', offset=0, items=[], total=0, selected=new Set(), generation=0, blobs=[], viewing=-1, largeBlob='', zoom=1, busy=false, historySignature='', sendId='';
let generatedParent='', generatedSignature='',generatedUrls=[];
let generatedEpoch=0;
const note=text=>{$('notice').textContent=text;};
async function api(path,data,signal){
 const response=await fetch('/web/api/'+path,{method:data===undefined?'GET':'POST',headers:{'X-Loki-Web-Token':token,...(data===undefined?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:JSON.stringify(data),cache:'no-store',signal});
 if(response.status===401){token='';sessionStorage.removeItem('lokiWebToken');localStorage.removeItem('lokiWebToken');showAuth();throw Error('接続の期限が切れました。スマホでコードを発行してください');}
 if(!response.ok){let j;try{j=await response.json();}catch{}throw Error(j?.error||'処理できませんでした（'+response.status+'）');}
 return /^(image|video)\//.test(response.headers.get('Content-Type'))?response.blob():response.json();
}
function on(id,action){$(id).addEventListener('click',()=>Promise.resolve().then(action).catch(e=>note(e.message)));}
function showAuth(){const connected=!!token;if(!connected){generatedEpoch++;for(const u of generatedUrls)URL.revokeObjectURL(u);generatedUrls=[];generatedSignature='';generatedParent='';$('generatedList').replaceChildren();$('generationParent').textContent='';$('resetGenerationParent').hidden=true;}$("meetings").hidden=!connected||activeTab!=="meetings";if(!connected){if(meetingUploadController)meetingUploadController.abort();$("meetingFile").value="";$("meetingImportList").replaceChildren();$("meetingControlStatus").textContent="接続していません";$("meetingUploadStatus").textContent="";meetingImportSignature="";meetingPlayGeneration++;$("meetingAudio").pause();$("meetingAudio").removeAttribute("src");$("meetingList").replaceChildren();$("meetingText").textContent="";$("meetingPlayer").hidden=true;if(meetingBlob)URL.revokeObjectURL(meetingBlob);meetingBlob="";meetingCurrent=null;meetingSignature="";}$('pair').hidden=connected;$('console').hidden=!connected;$('logout').hidden=!connected;if(!connected){clearChatImage();generation++;blobs.forEach(URL.revokeObjectURL);blobs=[];$('grid').replaceChildren();closeViewer();$('history').replaceChildren();$('outbox').replaceChildren();historySignature='';}}
function caption(item){const m=item.metadata;return new Date(item.time).toLocaleString('ja-JP')+(m?.available?' · '+m.latitude.toFixed(5)+', '+m.longitude.toFixed(5):' · 撮影場所なし');}
function updateSelected(){for(const suffix of ['', 'Bottom']){$('selectedCount'+suffix).textContent=selected.size+'枚選択';$('saveSelected'+suffix).disabled=$('deleteSelected'+suffix).disabled=!selected.size||busy;$('selectPage'+suffix).disabled=$('clearSelection'+suffix).disabled=busy;}for(const card of $('grid').children){const checked=selected.has(card.dataset.id);card.classList.toggle('selected',checked);card.querySelector('input').checked=checked;card.querySelector('input').disabled=busy;}}
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
for(const suffix of ['', 'Bottom']){
on('selectPage'+suffix,()=>{selected=new Set(items.map(x=>x.id));updateSelected();});on('clearSelection'+suffix,()=>{selected.clear();updateSelected();});on('deleteSelected'+suffix,()=>deleteIds([...selected]));
on('saveSelected'+suffix,async()=>{if(busy)return;busy=true;updateSelected();try{for(const item of items.filter(x=>selected.has(x.id)))await saveItem(item);note('JPEGを保存しました。ブラウザのダウンロードを確認してください');}finally{busy=false;updateSelected();}});
}
on('previous',async()=>{offset=Math.max(0,offset-PHOTO_PAGE_SIZE);await loadPhotos();});on('next',async()=>{offset+=PHOTO_PAGE_SIZE;await loadPhotos();});
on('deleteAll',async()=>{const preview=await api('delete-preview');if(!preview.count)return;if(!confirm('スマホのロキ保存分 '+preview.count+'枚をすべて削除します。全ページが対象です。続けますか？'))return;await api('delete-all',{revision:preview.revision});closeViewer();offset=0;await loadPhotos();note('ロキ保存分をすべて削除しました');});
on('closeViewer',closeViewer);$('viewer').addEventListener('cancel',e=>{e.preventDefault();closeViewer();});
on('prevPhoto',()=>openViewer(viewing-1));on('nextPhoto',()=>openViewer(viewing+1));on('zoomIn',()=>{zoom=Math.min(5,zoom+.5);applyZoom();});on('zoomOut',()=>{zoom=Math.max(1,zoom-.5);applyZoom();});on('resetZoom',()=>{zoom=1;applyZoom();});$('largePhoto').addEventListener('dblclick',()=>{zoom=zoom>1?1:2;applyZoom();});
on('savePhoto',()=>viewing>=0?saveItem(items[viewing]):undefined);on('deletePhoto',()=>viewing>=0?deleteIds([items[viewing].id]):undefined);
function showTab(tab){activeTab=tab;for(const name of ['photos','chat','meetings']){$(name).hidden=name!==tab;$(name+'Tab').setAttribute('aria-pressed',String(name===tab));}}
on('photosTab',()=>showTab('photos'));
on('chatTab',async()=>{showTab('chat');await loadHistory();});
on('meetingsTab',async()=>{showTab('meetings');await refreshMeetings();});
$('message').addEventListener('input',()=>{sendId='';});
let chatImageData='', chatImageUrl='', chatImageGeneration=0, chatImagePreparing=false;
function clearChatImage(){chatImageGeneration++;chatImageData='';chatImagePreparing=false;sendId='';$('chatImage').value='';$('chatImagePreview').hidden=true;$('chatImagePreview').removeAttribute('src');$('clearChatImage').hidden=true;$('chatImageStatus').textContent='';if(chatImageUrl)URL.revokeObjectURL(chatImageUrl);chatImageUrl='';}
async function chatImageDimensions(file){
 const bytes=new Uint8Array(await file.slice(0,1048576).arrayBuffer()),d=new DataView(bytes.buffer);let width=0,height=0;
 const text=(offset,n)=>String.fromCharCode(...bytes.subarray(offset,offset+n));
 if(bytes.length>=24&&bytes[0]===137&&text(1,3)==='PNG'){width=d.getUint32(16);height=d.getUint32(20);}
 else if(bytes.length>=10&&text(0,3)==='GIF'){width=d.getUint16(6,true);height=d.getUint16(8,true);}
 else if(bytes.length>=30&&text(0,4)==='RIFF'&&text(8,4)==='WEBP'){
  const kind=text(12,4);
  if(kind==='VP8X'){width=1+bytes[24]+(bytes[25]<<8)+(bytes[26]<<16);height=1+bytes[27]+(bytes[28]<<8)+(bytes[29]<<16);}
  else if(kind==='VP8 '&&bytes[23]===157&&bytes[24]===1&&bytes[25]===42){width=d.getUint16(26,true)&16383;height=d.getUint16(28,true)&16383;}
  else if(kind==='VP8L'&&bytes[20]===47){width=1+((bytes[21]|(bytes[22]<<8))&16383);height=1+(((bytes[22]>>6)|(bytes[23]<<2)|(bytes[24]<<10))&16383);}
 }else if(bytes.length>4&&bytes[0]===255&&bytes[1]===216){
  let p=2;while(p+8<bytes.length){if(bytes[p++]!==255)break;while(bytes[p]===255)p++;const marker=bytes[p++];if(marker===217||marker===218)break;const length=d.getUint16(p);if(length<2||p+length>bytes.length)break;if([192,193,194,195,197,198,199,201,202,203,205,206,207].includes(marker)){height=d.getUint16(p+3);width=d.getUint16(p+5);break;}p+=length;}
 }
 if(!width||!height)throw Error('JPEG・PNG・WebP・GIFで保存した画像を選択してください');
 if(width*height>80000000)throw Error('画像は8000万画素以内にしてください');
 return {width,height};
}
async function prepareChatImage(file){
 clearChatImage();if(!file)return;const epoch=chatImageGeneration;chatImagePreparing=true;
 try{if(file.size>20000000||!file.type.startsWith('image/'))throw Error('画像は20MB以内の画像ファイルを選択してください');
  $('chatImageStatus').textContent='画像を準備しています…';const dimensions=await chatImageDimensions(file);const decodeRatio=Math.min(1,2048/Math.max(dimensions.width,dimensions.height));const bitmap=await createImageBitmap(file,{resizeWidth:Math.max(1,Math.round(dimensions.width*decodeRatio)),resizeHeight:Math.max(1,Math.round(dimensions.height*decodeRatio)),resizeQuality:'high'});
  try{if(bitmap.width*bitmap.height>80000000)throw Error('画像は8000万画素以内にしてください');const ratio=Math.min(1,2048/Math.max(bitmap.width,bitmap.height)),canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(bitmap.width*ratio));canvas.height=Math.max(1,Math.round(bitmap.height*ratio));const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0,canvas.width,canvas.height);
   let blob;for(let q=.9;q>=.3;q-=.1){blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',q));if(blob&&blob.size<=600000)break;}if(!blob||blob.size>600000)throw Error('必要な部分を切り取って再選択してください');const bytes=new Uint8Array(await blob.arrayBuffer());let binary='';for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));if(epoch!==chatImageGeneration||!token)return;chatImageData=btoa(binary);chatImageUrl=URL.createObjectURL(blob);$('chatImagePreview').src=chatImageUrl;$('chatImagePreview').hidden=false;$('clearChatImage').hidden=false;$('chatImageStatus').textContent='送信する画像を確認してください';
  }finally{bitmap.close();}
 }catch(e){if(epoch===chatImageGeneration){clearChatImage();note(e.message);}}finally{if(epoch===chatImageGeneration)chatImagePreparing=false;}
}
$('chatImage').addEventListener('change',()=>prepareChatImage($('chatImage').files[0]));on('clearChatImage',clearChatImage);
let composingMessage=false;
$('message').addEventListener('compositionstart',()=>{composingMessage=true;});
$('message').addEventListener('compositionend',()=>{composingMessage=false;});
$('message').addEventListener('keydown',e=>{
 if(e.key!=='Enter'||e.shiftKey||e.isComposing||composingMessage||e.keyCode===229)return;
 e.preventDefault();if(e.repeat||$('send').disabled||(!$('message').value.trim()&&!chatImageData))return;
 $('chatForm').requestSubmit();
});

function newRequestId(){
 const rng=globalThis.crypto;
 if(typeof rng?.randomUUID==='function')return rng.randomUUID();
 if(typeof rng?.getRandomValues!=='function')throw Error('このブラウザでは送信IDを作成できません。最新のブラウザで開き直してください');
 return Array.from(rng.getRandomValues(new Uint8Array(16)),v=>v.toString(16).padStart(2,'0')).join('');
}
$('chatForm').addEventListener('submit',async e=>{
 e.preventDefault();if($('send').disabled)return;const message=$('message').value.trim();const image=chatImageData;if(chatImagePreparing){note('画像の準備が終わるまでお待ちください');return;}if(!message&&!image)return;
 $('send').disabled=true;let timer;
 try{
  sendId=sendId||newRequestId();const controller=new AbortController();timer=setTimeout(()=>controller.abort(),15000);
  note('送信しています…');const receipt=await api(generatedParent?'media-generate':image?'image-question':'send',{message,requestId:sendId,...(image?{image}:{}),...(generatedParent?{parent:generatedParent}:{})},controller.signal);if(receipt.videoGenerated||receipt.generated){$('resetGenerationParent').click();$('generatedPanel').open=true;await loadGenerated();}
  if($('message').value.trim()===message){$('message').value='';sendId='';if(image===chatImageData)clearChatImage();}
  note(receipt.videoGenerated?'動画生成を受け付けました。会話を続けられます。生成動画一覧で進捗を確認できます':receipt.generated?'画像生成を受け付けました。生成画像一覧で進捗を確認できます':'グラスへ送信待ちです。返信は共通履歴に表示されます');
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



$('resetGenerationParent').onclick=()=>{generatedParent='';$('generationParent').textContent='';$('resetGenerationParent').hidden=true;sendId='';};
$('chatImage').addEventListener('change',()=>{if(generatedParent)$('resetGenerationParent').click();});
let generatedLoading=false;
async function loadGenerated(){if(!token||$('chat').hidden||generatedLoading)return;generatedLoading=true;try{const sessionToken=token;const [images,videos]=await Promise.all([api('image-generations'),api('video-generations')]);if(token!==sessionToken)return;const items=[...images.items.map(j=>({...j,mediaKind:'IMAGE'})),...videos.items.map(j=>({...j,mediaKind:'VIDEO'}))].sort((a,b)=>b.time-a.time);const signature=JSON.stringify(items);if(signature===generatedSignature)return;const list=$('generatedList');if([...list.querySelectorAll('video')].some(v=>!v.paused&&!v.ended))return;generatedSignature=signature;const epoch=++generatedEpoch;for(const v of list.querySelectorAll('video')){v.pause();v.removeAttribute('src');v.load();}for(const u of generatedUrls)URL.revokeObjectURL(u);generatedUrls=[];list.replaceChildren();
 for(const j of items){if(epoch!==generatedEpoch||token!==sessionToken)return;const video=j.mediaKind==='VIDEO',card=document.createElement('article'),label=document.createElement('p');card.dataset.id=j.id;card.dataset.mediaKind=j.mediaKind;label.textContent=(video?'動画':'画像')+' · '+j.prompt+' · '+({queued:'受付済み',submitting:'APIへ受付中…',running:'生成中… 会話を続けられます',downloading:'転送中…',saving:'保存中…',completed:'完成',paused:'結果確認を休止中'}[j.state]||'生成失敗')+(j.error?' · '+j.error:'')+(j.statusError?' · '+j.statusError:'')+(j.galleryError?' · '+j.galleryError:'');card.append(label);
 if(j.state==='completed'&&!video){const image=document.createElement('img');image.alt='生成画像';image.style.maxHeight='240px';image.style.maxWidth='100%';card.append(image);try{const blob=await api('generated-image?id='+encodeURIComponent(j.id)+'&size=thumb');if(epoch!==generatedEpoch||token!==sessionToken)return;const url=URL.createObjectURL(blob);generatedUrls.push(url);image.src=url;}catch(e){label.textContent+=' · プレビュー取得失敗';}
 const follow=document.createElement('button');follow.type='button';follow.textContent='この画像から続ける';follow.onclick=()=>{generatedParent=j.id;$('generationParent').textContent='この画像を元にします。変更内容や動きをそのまま入力してください';$('resetGenerationParent').hidden=false;sendId='';$('message').focus();};card.append(follow);const save=document.createElement('button');save.type='button';save.textContent='画像を保存';save.onclick=async()=>{try{const blob=await api('generated-image?id='+encodeURIComponent(j.id));const u=URL.createObjectURL(blob),a=document.createElement('a');a.href=u;a.download='Loki-generated-'+j.id+(j.mime==='image/png'?'.png':j.mime==='image/webp'?'.webp':'.jpg');a.click();setTimeout(()=>URL.revokeObjectURL(u),30000);}catch(e){note(e.message);}};card.append(save);}
 if(j.state==='completed'&&video){const player=document.createElement('video');player.controls=true;player.preload='none';player.playsInline=true;player.style.maxWidth='100%';player.style.maxHeight='360px';player.setAttribute('aria-label','生成動画');card.append(player);let objectUrl='';const fetchVideo=async()=>{if(objectUrl)return objectUrl;const blob=await api('generated-video?id='+encodeURIComponent(j.id));if(epoch!==generatedEpoch||sessionToken!==token)return '';objectUrl=URL.createObjectURL(blob);generatedUrls.push(objectUrl);return objectUrl;};const play=document.createElement('button');play.type='button';play.textContent='動画を再生';play.onclick=async()=>{play.disabled=true;try{const url=await fetchVideo();if(url){player.src=url;await player.play();}}catch(e){note(e.message);}finally{play.disabled=false;}};card.append(play);const save=document.createElement('button');save.type='button';save.textContent='動画を保存';save.onclick=async()=>{try{const url=await fetchVideo();if(url){const a=document.createElement('a');a.href=url;a.download='Loki-video-'+j.id+'.mp4';a.click();}}catch(e){note(e.message);}};card.append(save);}
 if(video&&j.canResume){const resume=document.createElement('button');resume.type='button';resume.textContent='結果確認を再開';resume.onclick=async()=>{try{await api('generated-video-resume',{id:j.id});generatedSignature='';await loadGenerated();}catch(e){note(e.message);}};card.append(resume);}
 if(!['queued','submitting','running','downloading','saving'].includes(j.state)){const del=document.createElement('button');del.type='button';del.textContent='ロキ内から削除';del.onclick=async()=>{if(!confirm('ロキ内の生成結果と指示を削除します。ギャラリーのコピーは残ります。'))return;try{await api(video?'generated-video-delete':'generated-delete',{id:j.id});generatedSignature='';if(generatedParent===j.id)$('resetGenerationParent').click();await loadGenerated();}catch(e){note(e.message);}};card.append(del);}list.append(card);
 }}finally{generatedLoading=false;}}
setInterval(()=>{if(!document.hidden)loadGenerated().catch(e=>note(e.message));},4000);
$('generatedPanel').addEventListener('toggle',()=>{if($('generatedPanel').open)loadGenerated().catch(e=>note(e.message));});
