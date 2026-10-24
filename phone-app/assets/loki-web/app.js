'use strict';
const $=id=>document.getElementById(id);
const PHOTO_PAGE_SIZE=200;
let meetingSignature='', meetingCurrent=null, meetingBlob='', meetingPlayGeneration=0;
let activeTab='photos', meetingControlBusy=false, meetingUploadController=null, meetingUploadId='', meetingImportSignature='';
let token=localStorage.getItem('lokiWebToken')||sessionStorage.getItem('lokiWebToken')||'', offset=0, items=[], total=0, selected=new Set(), generation=0, blobs=[], viewing=-1, largeBlob='', zoom=1, busy=false, historySignature='', sendId='';
let generatedParent='', generatedSignature='',generatedUrls=[];
let generatedEpoch=0;
let recoveryNeedsConfirm=false;
let composerDraftRevision=0, generationParentRevision=0, recoveryRestoreGeneration=0;
function setComposerMessage(value){$('message').value=value;composerDraftRevision++;window.lokiConversationUi?.draftChanged();}
function composerSnapshot(){return JSON.stringify([token,$('message').value,composerDraftRevision,chatImageGeneration,generatedParent,generationParentRevision,imageReferenceDraft?.key||'',imageReferenceDraft?.prefix||'']);}
const note=text=>{$('notice').textContent=text;};
async function api(path,data,signal){
 const response=await fetch('/web/api/'+path,{method:data===undefined?'GET':'POST',headers:{'X-Loki-Web-Token':token,...(data===undefined?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:JSON.stringify(data),cache:'no-store',signal});
 if(response.status===401){token='';sessionStorage.removeItem('lokiWebToken');localStorage.removeItem('lokiWebToken');showAuth();throw Error('接続の期限が切れました。スマホでコードを発行してください');}
 if(!response.ok){let j;try{j=await response.json();}catch{}throw Error(j?.error||'処理できませんでした（'+response.status+'）');}
 return /^(image|video)\//.test(response.headers.get('Content-Type'))?response.blob():response.json();
}
function on(id,action){$(id).addEventListener('click',()=>Promise.resolve().then(action).catch(e=>note(e.message)));}
let inboxItems=[];
function showAuth(){const connected=!!token;if(!connected){clearCurrentWebTurn();clearSentImageLog();document.dispatchEvent(new Event("loki-session-cleared"));imageReferenceDraft=null;imageReferenceCurrent=null;$('operationsBody').replaceChildren();if($('recoveryList'))$('recoveryList').replaceChildren();if($('deliveryProgress'))$('deliveryProgress').textContent='';recoveryNeedsConfirm=false;$('operationsStatus').textContent='';$('workspaceBody').replaceChildren();$('workspaceStatus').textContent='';$('navigationHealthStatus').textContent='';generatedEpoch++;for(const u of generatedUrls)URL.revokeObjectURL(u);generatedUrls=[];generatedSignature='';generatedParent='';$('generatedList').replaceChildren();$('generationParent').textContent='';$('resetGenerationParent').hidden=true;}$("meetings").hidden=!connected||activeTab!=="meetings";if(!connected){if(meetingUploadController)meetingUploadController.abort();$("meetingFile").value="";$("meetingImportList").replaceChildren();$("meetingControlStatus").textContent="接続していません";$("meetingUploadStatus").textContent="";meetingImportSignature="";meetingPlayGeneration++;$("meetingAudio").pause();$("meetingAudio").removeAttribute("src");$("meetingList").replaceChildren();$("meetingText").textContent="";$("meetingPlayer").hidden=true;if(meetingBlob)URL.revokeObjectURL(meetingBlob);meetingBlob="";meetingCurrent=null;meetingSignature="";}$('pair').hidden=connected;$('console').hidden=!connected;$('logout').hidden=!connected;if(!connected){clearChatImage();generation++;blobs.forEach(URL.revokeObjectURL);blobs=[];$('grid').replaceChildren();closeViewer();$('conversationRuntime').textContent='接続していません';$('history').replaceChildren();$('outbox').replaceChildren();showImageReference(null);$('secretaryInboxList').replaceChildren();inboxItems=[];$('secretaryInboxSearch').value='';$('secretaryInboxFilter').value='active';$('secretaryInboxSummary').textContent='秘書の受信箱：接続していません';historySignature='';}}
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
let historyLoading=false;
function showConversationRuntime(runtime){
 const host=$('conversationRuntime');if(!runtime){host.textContent='グラスの状態は取得できません。スマホの接続を確認してください';return;}
 const labels={READY:'待機',WAIT:'次の送信を待っています',THINKING:'回答処理中',ERROR:'エラー',UNKNOWN:'未確認'};
 const age=runtime.stateAgeMs>=0?Math.floor(runtime.stateAgeMs/1000)+'秒前':'時刻未確認';
 host.textContent=(runtime.contactRecent?'直近のグラス通信あり':'直近15秒のグラス通信は未確認')+' ／ '+(runtime.stateFresh?'状態：':'最後の状態：')+(labels[runtime.state]||'未確認')+(runtime.message?' ／ '+runtime.message:'')+'（'+age+'）'+(runtime.state==='WAIT'&&runtime.waitMs>0?' ／ 待ち '+Math.ceil(runtime.waitMs/1000)+'秒':'')+' ／ スマホ側の送信待ち '+(runtime.queued||0)+'件';
 host.dataset.stale=String(!runtime.stateFresh);host.dataset.error=String(runtime.state==='ERROR');
}
function editQueuedText(entry){
 const existing=$('message').value.trim();if(existing&&existing!==entry.message&&!confirm('現在の入力を、この依頼文へ置き換えますか？'))return;
 if((chatImageData||generatedParent)&&!confirm('現在の添付と編集元を外して、この依頼文を戻しますか？ 元の依頼の添付は復元できません。'))return;
 if(chatImageData)clearChatImage();if(generatedParent)$('resetGenerationParent').click();
 setComposerMessage(entry.message);sendId='';recoveryNeedsConfirm=true;$('message').focus();note('入力欄へ戻しました。自動送信はしていません。前の依頼が実行済み・送信待ちの可能性があるため、履歴と送信状況を確認してから送信してください。');
}
let imageReferenceSignature='',imageReferenceCurrent=null,imageReferenceDraft=null;
function retargetReferenceDraft(current,oldPrefix,nextPrefix,limit){const next=oldPrefix&&current.startsWith(oldPrefix)?nextPrefix+current.slice(oldPrefix.length):nextPrefix;if(!nextPrefix||next.length>limit)throw Error('質問が長すぎます。本文は残しています。短くしてから選び直してください');return next;}
function showImageReference(reference){
 const data=reference&&reference.available?reference:null;imageReferenceCurrent=data;const signature=JSON.stringify(data);if(signature===imageReferenceSignature)return;imageReferenceSignature=signature;
 const panel=$('imageReferencePanel'),parts=$('imageReferenceParts');parts.replaceChildren();panel.hidden=!data;if(!data){panel.open=false;return;}
 $('imageReferenceSummary').textContent='参照可能な直近の添付：'+data.imageCount+'件 ／ '+new Date(data.at).toLocaleString('ja-JP')+' 読み取り';
 if(data.thumbnail){const preview=document.createElement('img');preview.src='data:image/jpeg;base64,'+data.thumbnail;preview.alt='参照中の写真の縮小プレビュー';preview.style.maxWidth='160px';preview.style.maxHeight='120px';$('imageReferenceSummary').append(preview);}
 const capture=document.createElement('p');capture.textContent=data.capturedAt?'撮影日時：'+new Date(data.capturedAt).toLocaleString('ja-JP'):'撮影日時：未記録（添付画像の時刻は推測しません）';parts.append(capture);
 const question=document.createElement('p');question.textContent='質問：'+data.question;parts.append(question);
 const uncertain=(data.answer||'').split(/[\r\n。]+/).filter(s=>/不鮮明|読み取れ|判読でき|判別でき|未確認|不明|記載がない|記載されていない|確認が必要|確認してください/.test(s)).slice(0,6);const caution=document.createElement('p');caution.textContent=uncertain.length?'読み取り結果にある確認事項：'+uncertain.map(s=>s.trim().slice(0,240)).join(' ／ '):'不確かな箇所の明記は見つかりません。正しさを保証する判定ではありません。';parts.append(caution);
 for(const part of data.sections||[]){const card=document.createElement('article'),text=document.createElement('p');const kind=part.kind==='video'?'動画（8場面・音声なし）':part.kind==='image'?'画像':'種類未記録';text.textContent='添付'+part.index+'・'+kind+'：'+(part.separated?part.reading:'個別の読み取り区分を確認できません。全文を確認してください。');card.append(text);
  const region=document.createElement('input');region.maxLength=120;region.placeholder='見る部分（任意）：左上の表、予約日時の欄など';region.setAttribute('aria-label','添付'+part.index+'の見る部分');card.append(region);const follow=document.createElement('button');follow.type='button';follow.textContent='添付'+part.index+'（'+kind+'）について質問・訂正';follow.onclick=()=>{if(chatImageData||generatedParent){note('送信前の添付・編集元を外してから参照を選んでください');return;}if(!data.key||imageReferenceCurrent?.key!==data.key){note('参照が更新・失効しています。最新の一覧から選び直してください');return;}const prefix='この画像の【画像'+part.index+'】（添付'+part.index+'・'+kind+'）の'+(region.value.trim()||'全体')+'について、',current=$('message').value,keep=imageReferenceDraft&&current.startsWith(imageReferenceDraft.prefix);let replacement;try{replacement=retargetReferenceDraft(current,keep?imageReferenceDraft.prefix:'',prefix,$('message').maxLength);}catch(e){note(e.message);return;}if(current.trim()&&!confirm(keep?'書いた質問を残して、選んだ添付へ参照先を変更しますか？ 自動送信はしません。':'入力中の文章を、画像への質問の書き出しへ置き換えますか？'))return;setComposerMessage(replacement);imageReferenceDraft={key:data.key,prefix};sendId='';$('message').focus();note(keep?'質問本文を残して参照先を変更しました。自動送信はしていません':'質問の書き出しを入力しました。内容を追記して送信してください。自動送信はしていません');};card.append(follow);parts.append(card);}
 const original=document.createElement('details'),heading=document.createElement('summary'),body=document.createElement('p');heading.textContent='読み取り結果の全文';body.textContent=data.answer;original.append(heading,body);parts.append(original);
}
function requestWaitHint(entry,now=Date.now()){
 if(!entry.tracked||['ANSWERED','FAILED','UNCONFIRMED','STOPPED','CANCELLED','SUPERSEDED'].includes(entry.requestState))return '';
 const stamp=Number(entry.receiptAt||entry.time),age=now-stamp;
 if(!Number.isFinite(age)||!Number.isFinite(stamp)||stamp<=0||age<0)return '';
 return ' ／ 最終受付・更新から'+Math.floor(age/1000)+'秒'+(age>=120000?'。完了未確認です。履歴と接続を確認してください（自動再送なし）':'');
}
let currentWebTurn=null,lastHistoryPollAt=0;
function clearCurrentWebTurn(){currentWebTurn=null;if($('currentTurnStatus'))$('currentTurnStatus').textContent='';}
function trackCurrentWebTurn(receipt,message){
 if(receipt.tracked===true&&/^[a-f0-9]{64}$/.test(receipt.requestKey||'')){currentWebTurn={key:receipt.requestKey,message,state:'QUEUED'};document.dispatchEvent(new CustomEvent('loki-request-sent',{detail:{key:receipt.requestKey}}));renderCurrentWebTurn([],null,false);}
 else clearCurrentWebTurn();
}
function renderCurrentWebTurn(requests,progress,fresh){
 const status=$('currentTurnStatus');if(!status||!currentWebTurn)return;
 const turn=currentWebTurn,row=(requests||[]).find(item=>item.tracked===true&&item.key===turn.key);
 if(row)turn.state=row.requestState;
 const labels={CREATED:'スマホで受付中',QUEUED:'グラスへの送信待ち',RECEIVED:'グラスで受信済み',RUNNING:'この質問への回答を処理中',ANSWERED:'この質問の回答をグラスで表示済み。ログ反映は別途確認してください',FAILED:'この質問の処理でエラー。送信状況を確認してください',UNCONFIRMED:'この質問の結果は未確認。自動再送していません',STOPPED:'この質問の回答待ちは中止済みです',CANCELLED:'この質問は送信前に取消済みです',SUPERSEDED:'編集した新しい質問へ差し替え済みです'};
 let text='直近に送ったコメント：'+turn.message+'\n'+(labels[turn.state]||'この質問の状態は未確認');
 const older=(requests||[]).filter(item=>item.tracked===true&&item.key!==turn.key&&['CREATED','QUEUED','RECEIVED','RUNNING'].includes(item.requestState)&&row&&Number(item.time)<=Number(row.time));
 if(row&&!fresh&&['CREATED','QUEUED','RECEIVED','RUNNING'].includes(turn.state))text+='\nグラスとの接続は未確認。最後に保存された状態を表示しています。';
 if(older.length)text+='\n先に送った未完了の依頼：'+older.length+'件。以前の回答が後からログに表示される場合があります。';
 if(progress?.id&&progress.id!==turn.key&&['CREATED','QUEUED','RECEIVED','RUNNING'].includes(turn.state))text+='\nグラスの進行表示は別の依頼の状態です。';
 status.textContent=text;
}
let sentImageLogEpoch=0,sentImageLogSignature='',sentImageLogLoading=false;
function clearSentImageLog(){sentImageLogEpoch++;sentImageLogSignature='';if($('sentImageLog'))$('sentImageLog').replaceChildren();}
async function loadSentImageLog(){
 if(!token||$('chat').hidden||sentImageLogLoading)return;
 sentImageLogLoading=true;const session=token,epoch=sentImageLogEpoch;
 try{const data=await api('recovery');if(token!==session||epoch!==sentImageLogEpoch)return;
 const rows=(data.items||[]).filter(row=>row.imageCount>0).slice(0,10),signature=JSON.stringify(rows);
 if(signature===sentImageLogSignature)return;
 const cards=[];
 for(const row of rows){
  if(token!==session||epoch!==sentImageLogEpoch)return;
  const card=document.createElement('article');card.className='bubble user';
  const caption=document.createElement('small');caption.textContent='送信時の添付 · '+new Date(row.at).toLocaleString('ja-JP')+' · '+row.imageCount+'件（動画は抜粋画像）';card.append(caption);
  const text=document.createElement('p');text.textContent=row.message||'画像のみの送信';card.append(text);
  try{const saved=await api('recovery?id='+encodeURIComponent(row.id));if(token!==session||epoch!==sentImageLogEpoch)return;
   const image=saved.payload?.image;
   if(typeof image==='string'&&image.length<=800000&&/^[A-Za-z0-9+/]+={0,2}$/.test(image)){const thumb=document.createElement('img');thumb.className='log-thumbnail';thumb.alt='送信時の添付の縮小プレビュー';thumb.src='data:image/jpeg;base64,'+image;card.append(thumb);}
   else throw Error('preview unavailable');
   if(row.route==='image-question'){const follow=document.createElement('button');follow.type='button';follow.textContent='この写真に追加質問';follow.onclick=()=>preparePhotoFollowUp(row.id).catch(e=>note(e.message));card.append(follow);}
  }catch(e){if(token!==session||epoch!==sentImageLogEpoch)return;const missing=document.createElement('p');missing.textContent='添付プレビューを取得できません。送信控えの期限・接続を確認してください。';card.append(missing);}
  cards.push(card);
 }
 if(token!==session||epoch!==sentImageLogEpoch)return;
 $('sentImageLog').replaceChildren(...cards);sentImageLogSignature=signature;
 }finally{sentImageLogLoading=false;}
}
async function loadHistory(){if(!token||$('chat').hidden||historyLoading)return;historyLoading=true;const session=token;
 try{const result=await api('history');if(token!==session)return;const position=window.lokiConversationReliability?.capturePosition();window.lokiConversationReliability?.beforeHistory(result);showConversationRuntime(result.runtime);showImageReference(result.imageReference);if($('deliveryProgress'))$('deliveryProgress').textContent=deliveryProgressLabel(result.progress,result.progressFresh);renderCurrentWebTurn(result.requests,result.progress,result.progressFresh);window.lokiConversationUi?.history(result);window.lokiConversationReliability?.history(result);window.lokiConversationTools?.history(result);
 const signature=JSON.stringify({items:result.items,requests:result.requests});const historyChanged=signature!==historySignature;historySignature=signature;
 const list=$('history'),previousTop=list.scrollTop,nearEnd=list.scrollHeight-list.scrollTop-list.clientHeight<100;if(historyChanged){list.replaceChildren();
 if(window.lokiConversationUi)window.lokiConversationUi.renderHistory(result.items,list);else for(const entry of result.items){const div=document.createElement('div');div.className='bubble'+(entry.kind?.startsWith('ユーザー')?' user':'');const small=document.createElement('small');small.textContent=entry.kind+' · '+new Date(entry.time).toLocaleString('ja-JP');div.append(small,document.createTextNode(entry.message));list.append(div);}
 if(nearEnd)list.scrollTop=list.scrollHeight;else list.scrollTop=previousTop;}if(historyChanged){$('outbox').replaceChildren();loadSentImageLog().catch(()=>{});}let requestIndex=0;for(const entry of result.requests.slice(-20).reverse()){
 const card=$('outbox').children[requestIndex++]||document.createElement('article'),p=card.querySelector('p')||document.createElement('p');const sameText=result.items.some(h=>h.kind?.startsWith('ユーザー')&&h.message===entry.message&&h.time>=entry.time);
 const labels={CREATED:'スマホで受付中',QUEUED:'グラスへ送信待ち',RECEIVED:'ID一致でグラスが受信',RUNNING:'ID一致で回答処理中',ANSWERED:'グラスで回答を表示済み',FAILED:'回答処理エラー（操作の成否は要確認）',UNCONFIRMED:'結果未確認（自動再実行なし）',STOPPED:'回答待ちを中止済み',CANCELLED:'送信前に取消済み',SUPERSEDED:'編集した依頼へ差し替え済み'};
 const reasons={REFERENCE_CHANGED:'画像参照が更新・失効したため未送信',SAFETY_STOP:'安全判定で停止',MODEL_ERROR:'回答処理エラー',RESTART:'再起動で結果確認できず',INTERRUPTED:'処理が中断',LOCAL_RESULT:'ローカル機能へ引き渡し。結果は履歴で確認'};
 p.textContent=entry.device+' · '+(entry.tracked?(labels[entry.requestState]||'状態未確認'):entry.status)+(entry.receiptReason?' ／ '+(reasons[entry.receiptReason]||'結果を確認してください'):'')+(!entry.tracked&&sameText?'（共有履歴に同文あり。今回の送達確定ではありません）':'')+requestWaitHint(entry)+' · '+entry.message;if(!p.parentElement)card.append(p);
 if(!card.querySelector('button')){const edit=document.createElement('button');edit.type='button';edit.textContent='入力欄へ戻す（自動送信なし）';edit.onclick=()=>editQueuedText(entry);card.append(edit);}if(!card.parentElement)$('outbox').append(card);}window.lokiConversationReliability?.restorePosition(position);
 }catch(e){if(token===session)window.lokiConversationPlus?.unavailable();if(token===session)$('conversationRuntime').textContent='スマホから状態を取得できません。接続を確認してください。自動再送はしていません';throw e;}
 finally{historyLoading=false;}
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
on('refresh',loadPhotos);on('import',async()=>{if(!confirm('グラスのKEEP撮影を停止し、原本をグラスに残してスマホへコピーします。AIへ画像は送りません。続けますか？'))return;await api('import',{});note('原本を残す取り込みを依頼しました。まだ保存完了ではありません。一覧を更新して確認してください。');});
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
on('chatTab',async()=>{showTab('chat');await Promise.all([loadHistory(),loadInboxSummary(),loadSentImageLog()]);});
on('meetingsTab',async()=>{showTab('meetings');await refreshMeetings();});
$('message').addEventListener('input',()=>{composerDraftRevision++;sendId='';if(imageReferenceDraft&&!$('message').value.startsWith(imageReferenceDraft.prefix))imageReferenceDraft=null;});
let chatImageData='', chatImageUrl='', chatImageGeneration=0, chatImagePreparing=false, chatImageCount=1, chatAttachments=[], chatAttachmentVideo=false, chatAttachmentKinds=[];
function clearChatImage(){chatImageGeneration++;chatImageData='';chatImageCount=1;chatAttachments=[];chatAttachmentVideo=false;chatAttachmentKinds=[];if($('chatAttachmentList'))$('chatAttachmentList').replaceChildren();chatImagePreparing=false;sendId='';$('chatImage').value='';$('chatImagePreview').hidden=true;if($('attachmentDraft'))$('attachmentDraft').hidden=true;$('chatImagePreview').removeAttribute('src');$('clearChatImage').hidden=true;$('chatImageStatus').textContent='';if(chatImageUrl)URL.revokeObjectURL(chatImageUrl);chatImageUrl='';}
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
   let blob;for(let q=.9;q>=.3;q-=.1){blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',q));if(blob&&blob.size<=600000)break;}if(!blob||blob.size>600000)throw Error('必要な部分を切り取って再選択してください');const bytes=new Uint8Array(await blob.arrayBuffer());let binary='';for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));if(epoch!==chatImageGeneration||!token)return;chatImageData=btoa(binary);chatImageUrl=URL.createObjectURL(blob);$('chatImagePreview').src=chatImageUrl;$('chatImagePreview').hidden=false;if($('attachmentDraft'))$('attachmentDraft').hidden=false;$('clearChatImage').hidden=false;$('chatImageStatus').textContent='送信する画像を確認してください';
  }finally{bitmap.close();}
 }catch(e){if(epoch===chatImageGeneration){clearChatImage();note(e.message);}}finally{if(epoch===chatImageGeneration)chatImagePreparing=false;}
}
async function normalizeChatAttachment(file){
 if(file.size>20000000||!file.type.startsWith('image/'))throw Error('画像は1枚20MB以内にしてください');
 const size=await chatImageDimensions(file),scale=Math.min(1,2048/Math.max(size.width,size.height));
 const bitmap=await createImageBitmap(file,{resizeWidth:Math.max(1,Math.round(size.width*scale)),resizeHeight:Math.max(1,Math.round(size.height*scale)),resizeQuality:'high'});
 try{const canvas=document.createElement('canvas');canvas.width=bitmap.width;canvas.height=bitmap.height;const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(bitmap,0,0);for(let q=.9;q>=.3;q-=.1){const blob=await new Promise(r=>canvas.toBlob(r,'image/jpeg',q));if(blob&&blob.size<=600000)return new File([blob],'attachment.jpg',{type:'image/jpeg'});}throw Error('画像が細かすぎます。範囲を切り取ってください');}finally{bitmap.close();}
}
function renderChatAttachmentList(){const list=$('chatAttachmentList');if(!list)return;list.replaceChildren();for(const [i,kind] of chatAttachmentKinds.entries()){const item=document.createElement('li');item.textContent=(i+1)+': '+(kind==='video'?'動画（8場面の抜粋・音声なし）':'画像');list.append(item);}}
async function prepareChatMedia(files){
 const list=Array.from(files);if(!list.length)return;const previous=chatAttachments.slice(),priorKinds=chatAttachmentKinds.slice();
 const kinds=priorKinds.concat(list.map(f=>f.type.startsWith('video/')?'video':'image'));
 if(kinds.length>6){dropStatus('画像・動画は合計6件までです。現在の添付は保持しています');return;}
 if(kinds.filter(k=>k==='video').length>3){dropStatus('動画は合計3本までです。現在の添付は保持しています');return;}
 const epoch=++chatImageGeneration;chatImagePreparing=true;dropStatus('添付を追加しています…');
 try{
  const qualityHints=[];const added=[];for(const file of list){const video=file.type.startsWith('video/');const prepared=video?await createMediaOverview([file],chatImageDimensions,()=>epoch!==chatImageGeneration||!token):file;const normalized=await normalizeChatAttachment(prepared);if(!video){try{const hint=await photoQualityHint(normalized);if(hint)qualityHints.push(hint);}catch{qualityHints.push("画質の目安を取得できません。画像を確認してください。");}}added.push(normalized);if(epoch!==chatImageGeneration||!token)return;}
  const combined=previous.concat(added);const sheet=combined.length===1?combined[0]:await createMediaOverview(combined,chatImageDimensions,()=>epoch!==chatImageGeneration||!token,{kinds});
  if(epoch!==chatImageGeneration||!token)return;await prepareChatImage(sheet);
  if(chatImageData){chatImageCount=combined.length;chatAttachments=combined;chatAttachmentKinds=kinds;chatAttachmentVideo=kinds.includes('video');renderChatAttachmentList();const videos=kinds.filter(k=>k==='video').length;dropStatus(combined.length+'件を添付中（画像'+(combined.length-videos)+'枚・動画'+videos+'本）。追加できます。動画は各8場面の抜粋・音声なしです'+(qualityHints.length?' 画質の目安：'+[...new Set(qualityHints)].join(' ')+' 添付を外して撮り直せます。送信は選んだ画像のまま行います。':''));}
 }catch(e){if(epoch===chatImageGeneration){dropStatus(e.message);}}finally{if(epoch===chatImageGeneration)chatImagePreparing=false;}
}
$('chatImage').addEventListener('change',()=>{if(!$('send').disabled&&!chatImagePreparing)prepareChatMedia($('chatImage').files);});on('clearChatImage',clearChatImage);
$('chatForm').addEventListener('paste',e=>{const files=Array.from(e.clipboardData?.files||[]).filter(f=>/^(image|video)\//.test(f.type));if(files.length){e.preventDefault();if($('send').disabled||chatImagePreparing){dropStatus('準備が終わってから画像を追加してください');return;}prepareChatMedia(files);}});
// Capture at the window so file-input defaults and nested widgets cannot swallow a drop.
function dropStatus(text){$('chatImageStatus').textContent=text;const status=$('chatDropStatus');if(status)status.textContent=text;note(text);}
function attachmentDragFiles(data){const files=Array.from(data?.files||[]);if(!files.length)for(const item of Array.from(data?.items||[])){if(item.kind==='file'){const file=item.getAsFile();if(file)files.push(file);}}return files;}
function dragSummary(data){return {types:Array.from(data?.types||[]).slice(0,12).map(t=>String(t).slice(0,60)),files:attachmentDragFiles(data).length,images:droppedGooglePhotoUrls(data).length};}
function recordDrop(data,stage){try{const summary={...dragSummary(data),stage};if(token)api('media-drop-status',summary).catch(()=>{});}catch{}}
window.addEventListener('dragover',e=>{e.preventDefault();if(e.dataTransfer)e.dataTransfer.dropEffect='copy';const zone=$('chatDropZone');if(zone)zone.classList.add('dragging');},true);
window.addEventListener('dragleave',e=>{if(!e.relatedTarget){const zone=$('chatDropZone');if(zone)zone.classList.remove('dragging');}},true);
window.addEventListener('drop',e=>{
 e.preventDefault();e.stopPropagation();const zone=$('chatDropZone');if(zone)zone.classList.remove('dragging');
 const data=e.dataTransfer;recordDrop(data,'received');
 if(!token){dropStatus('先にロキへ接続してください。ドロップは受信しました');return;}
 if($('send').disabled||chatImagePreparing){dropStatus('送信・画像の準備が終わってから追加してください。ドロップは受信しました');return;}
 showTab('chat');dropStatus('ドロップを受信しました。画像を確認しています…');
 const files=attachmentDragFiles(data);
 if(files.length){prepareChatMedia(files).then(()=>{if(chatImageData)dropStatus(chatImageCount+'件を添付中。内容を確認して送信してください');}).catch(err=>dropStatus(err.message));return;}
 const html=data?.getData('text/html')||'',inline=html.match(/data:image\/(?:png|jpeg|webp|gif);base64,[A-Za-z0-9+/=]+/i);
 if(inline&&inline[0].length<=800000){try{const parts=inline[0].split(','),bytes=Uint8Array.from(atob(parts[1]),c=>c.charCodeAt(0));prepareChatMedia([new File([bytes],'dropped-image',{type:parts[0].slice(5).split(';')[0]})]);return;}catch{}}
 importDroppedGooglePhotos(data).catch(err=>{dropStatus(err.name==='AbortError'?'Googleフォトからの画像取得がタイムアウトしました':err.message);recordDrop(data,'failed');});
},true);
function droppedGooglePhotoUrls(data){
 const urls=[];const add=raw=>{try{const u=new URL(raw);if(u.protocol==='https:'&&/^lh[0-9]+\.googleusercontent\.com$/.test(u.hostname)&&!u.username&&!u.password&&(!u.port||u.port==='443')&&!u.hash&&u.pathname.length>1){if(!urls.includes(u.href))urls.push(u.href);return true;}}catch{}return false;};
 const html=data?.getData('text/html')||'';if(html&&html.length<=1000000){const template=document.createElement('template');template.innerHTML=html;for(const img of template.content.querySelectorAll('img')){if(!add(img.getAttribute('src')))for(const source of (img.getAttribute('srcset')||'').split(','))if(add(source.trim().split(/\s+/)[0]))break;}
  if(!urls.length)for(const match of html.matchAll(/https:\/\/lh[0-9]+\.googleusercontent\.com\/[^\s"'<>\)]+/g))add(match[0].replaceAll('&amp;','&'));
 }
 for(const t of ['text/uri-list','text/plain','URL','text/x-moz-url'])for(const line of (data?.getData(t)||'').split(/\r?\n/))if(!line.startsWith('#'))add(line.trim());
 const download=data?.getData('DownloadURL')||'';const at=download.indexOf('https://');if(at>=0)add(download.slice(at));
 return urls;
}
if($('chatDropStatus'))$('chatDropStatus').textContent='取り込み機能 v203：準備できました';
async function importDroppedGooglePhotos(data){
 const urls=droppedGooglePhotoUrls(data);showTab('chat');
 if(!urls.length)throw Error('ドロップは届きましたが、画像データ・画像URLが含まれていません（受信形式：'+(Array.from(data?.types||[]).join('、')||'なし')+'）。写真を開いた状態で画像自体をドラッグしてください');
 if(urls.length>6)throw Error('画像は一度に6枚までです');
 const epoch=++chatImageGeneration,expectedToken=token;chatImagePreparing=true;dropStatus('Googleフォトの画像を取り込んでいます…');
 const controller=new AbortController();const timer=setTimeout(()=>controller.abort(),60000);
 try{
  const files=[];for(const url of urls){const result=await api('google-photo-import',{url},controller.signal);if(epoch!==chatImageGeneration||token!==expectedToken)return;const binary=atob(result.image),bytes=Uint8Array.from(binary,c=>c.charCodeAt(0));files.push(new File([bytes],'GooglePhoto-'+(files.length+1)+'.jpg',{type:'image/jpeg'}));}
  if(epoch!==chatImageGeneration||token!==expectedToken)return;await prepareChatMedia(files);if(chatImageData)dropStatus('Googleフォトから'+files.length+'枚を追加、合計'+chatImageCount+'件を添付中。質問を入力して送信してください');
 }catch(err){if(epoch===chatImageGeneration){$('chatImageStatus').textContent='画像を取り込めませんでした。添付済みの画像は保持しています';throw err;}}
 finally{clearTimeout(timer);if(epoch===chatImageGeneration)chatImagePreparing=false;}
}
let composingMessage=false;
$('message').addEventListener('compositionstart',()=>{composingMessage=true;});
$('message').addEventListener('compositionend',()=>{composingMessage=false;});
$('message').addEventListener('keydown',e=>{
 if(e.key!=='Enter'||e.shiftKey||e.isComposing||composingMessage||e.keyCode===229)return;
 if($('enterBehavior')?.value==='newline'&&!e.ctrlKey&&!e.metaKey)return;
 e.preventDefault();if(e.repeat||$('send').disabled||(!$('message').value.trim()&&!chatImageData))return;
 $('chatForm').requestSubmit();
});

function newRequestId(){
 const rng=globalThis.crypto;
 if(typeof rng?.randomUUID==='function')return rng.randomUUID();
 if(typeof rng?.getRandomValues!=='function')throw Error('このブラウザでは送信IDを作成できません。最新のブラウザで開き直してください');
 const bytes=rng.getRandomValues(new Uint8Array(16));bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;const hex=Array.from(bytes,v=>v.toString(16).padStart(2,'0')).join('');return hex.slice(0,8)+'-'+hex.slice(8,12)+'-'+hex.slice(12,16)+'-'+hex.slice(16,20)+'-'+hex.slice(20);
}
$('chatForm').addEventListener('submit',async e=>{
 e.preventDefault();if($('send').disabled)return;if(recoveryNeedsConfirm){if(!confirm('前の依頼が実行済み・処理中の可能性があります。履歴と送信状況を確認しましたか？ 新しい依頼として送ると予定登録・生成が重複する場合があります。'))return;recoveryNeedsConfirm=false;sendId='';}const message=$('message').value.trim();const image=chatImageData;if(chatImagePreparing){note('画像の準備が終わるまでお待ちください');return;}if(!message&&!image)return;
 $('send').disabled=true;$('chatImage').disabled=true;let timer;
 try{
  if(imageReferenceDraft&&!message.startsWith(imageReferenceDraft.prefix))imageReferenceDraft=null;
  if(imageReferenceDraft&&(image||generatedParent||!imageReferenceCurrent||imageReferenceDraft.key!==imageReferenceCurrent.key||Date.now()-imageReferenceCurrent.at>=7200000||Date.now()<imageReferenceCurrent.at))throw Error('参照が更新・失効しています。本文は残しています。最新の参照を選び直してください。まだ送信していません');
  if(generatedParent&&image)throw Error('添付を解析する場合は編集元を外してください。添付を使う生成は編集元を外して依頼してください');
  const reviewState=composerSnapshot();if(typeof window.lokiReviewSubmission==='function'&&!await window.lokiReviewSubmission({message,hasImage:!!image,imageCount:image?chatImageCount:0,imageBytes:image?Math.floor(image.length*3/4):0,parent:generatedParent}))return;if(composerSnapshot()!==reviewState)throw Error('確認中に本文・添付・編集元が変わりました。まだ送信していません');
  if(window.lokiConversationUi&&!await window.lokiConversationUi.beforeSubmit(message,!!image,!!generatedParent))return;if(composerSnapshot()!==reviewState)throw Error('送信確認中に入力が変わりました。まだ送信していません');
  sendId=sendId||newRequestId();const controller=new AbortController();timer=setTimeout(()=>controller.abort(),15000);
  note('送信しています…');const submittedState=composerSnapshot(),parent=generatedParent;const receipt=await api(parent?'media-generate':image?'image-question':'send',{message,requestId:sendId,...(imageReferenceDraft?{referenceKey:imageReferenceDraft.key}:{}),...(image?{image,imageCount:chatImageCount,kinds:chatAttachmentKinds}:{}),...(parent?{parent}:{})},controller.signal);
  trackCurrentWebTurn(receipt,message);loadHistory().catch(()=>{});if(composerSnapshot()===submittedState){setComposerMessage('');window.lokiConversationTools?.accepted();sendId='';imageReferenceDraft=null;if(image===chatImageData)clearChatImage();if(parent)$('resetGenerationParent').click();}
  if(receipt.videoGenerated||receipt.generated){$('generatedPanel').open=true;try{await loadGenerated();}catch(refreshError){note('生成は受付済みです。一覧を更新できませんでした。再送せず「生成した画像・動画」を開き直してください');return;}}
  note(receipt.duplicate?'この依頼IDは既に受付済みです。追加送信はせず、送信状況を確認してください':receipt.videoGenerated?'動画生成を受け付けました。会話を続けられます。生成動画一覧で進捗を確認できます':receipt.generated?'画像生成を受け付けました。生成画像一覧で進捗を確認できます':'グラスへ送信待ちです。返信は共通履歴に表示されます');
 }catch(err){note(err.name==='AbortError'?'送信の確認がタイムアウトしました。接続を確認して再試行してください。同じ内容の再送は重複を防ぎます':err.message||'送信できませんでした');}
 finally{clearTimeout(timer);$('send').disabled=false;$('chatImage').disabled=false;}
});
setInterval(()=>{const pending=currentWebTurn&&['CREATED','QUEUED','RECEIVED','RUNNING'].includes(currentWebTurn.state),now=Date.now();if(!document.hidden&&now-lastHistoryPollAt>=(pending?1000:5000)){lastHistoryPollAt=now;loadHistory().catch(e=>note(e.message));}},1000);
showAuth();if(token)loadPhotos().catch(e=>{note(e.message);if(!token)connectBrowser(invitation);});else connectBrowser(invitation);


async function meetingPlay(item,part,offsetMs=0){const expectedToken=token, expectedGeneration=++meetingPlayGeneration;
 meetingCurrent=item;const select=$('meetingPart');if(select.options.length!==item.parts){select.replaceChildren();for(let i=0;i<item.parts;i++){const o=document.createElement('option');o.value=i;o.textContent=Math.floor(i*10/60)+':'+String(i*10%60).padStart(2,'0');select.append(o);}}
 select.value=part;$('meetingPlayer').hidden=false;
 const response=await fetch('/web/api/meeting-audio?id='+encodeURIComponent(item.id)+'&part='+part,{headers:{'X-Loki-Web-Token':token},cache:'no-store'});if(!response.ok)throw Error('録音を取得できません');const audioBlob=await response.blob();if(!token||token!==expectedToken||expectedGeneration!==meetingPlayGeneration)return;if(meetingBlob)URL.revokeObjectURL(meetingBlob);meetingBlob=URL.createObjectURL(audioBlob);$('meetingAudio').src=meetingBlob;$('meetingPlayStatus').textContent='記録 '+new Date(item.startedAt).toLocaleString()+' · 区間 '+(part+1)+'/'+item.parts;const audio=$('meetingAudio');if(offsetMs>0){await new Promise((resolve,reject)=>{const timer=setTimeout(()=>{cleanup();reject(Error('再生位置を読み込めません'));},10000);const cleanup=()=>{clearTimeout(timer);audio.removeEventListener('loadedmetadata',ready);audio.removeEventListener('error',failed);};const failed=()=>{cleanup();reject(Error('録音を読み込めません'));};const ready=()=>{cleanup();resolve();};if(audio.readyState>=1){ready();return;}audio.addEventListener('loadedmetadata',ready,{once:true});audio.addEventListener('error',failed,{once:true});});if(token!==expectedToken||expectedGeneration!==meetingPlayGeneration)return;audio.currentTime=Math.min(offsetMs/1000,Math.max(0,audio.duration-0.01));}if(token!==expectedToken||expectedGeneration!==meetingPlayGeneration)return;await audio.play();
}
async function loadMeetings(){if(!token||activeTab!=='meetings')return;const expectedToken=token;const result=await api('meetings');if(!token||token!==expectedToken)return;const sig=JSON.stringify(result);if(sig===meetingSignature)return;meetingSignature=sig;const list=$('meetingList');list.replaceChildren();for(const item of result.items){const card=document.createElement('article');card.dataset.meetingId=item.id;const title=document.createElement('p');title.textContent=(item.title?item.title+' · ':'')+new Date(item.startedAt).toLocaleString()+' · '+Math.floor(item.durationMs/60000)+'分 · '+item.reason+' · '+item.analysisStatus;card.append(title);if(item.topics?.length){const details=document.createElement('details');const heading=document.createElement('summary');heading.textContent='TOPICコメント '+item.topics.length+'件';details.append(heading);for(const topic of item.topics){const text=document.createElement('p');text.textContent=Math.floor(topic.audioMs/60000)+'分 '+topic.text;details.append(text);}card.append(details);}const play=document.createElement('button');play.textContent='録音を再生';play.onclick=()=>meetingPlay(item,0).catch(e=>note(e.message));card.append(play);const timeline=document.createElement('button');timeline.textContent='文字起こしの区間から録音を確認';timeline.onclick=async()=>{const session=token;try{const data=await api('reliability-timeline?id='+encodeURIComponent(item.id));if(token!==session||!card.isConnected)return;const block=document.createElement('div');const hint=document.createElement('p');hint.textContent='約60秒単位の文字起こし区間です。単語・発言ごとの正確な時刻ではありません。再生ボタンを押すまで音は出しません。';block.append(hint);for(const segment of data.items||[]){const text=document.createElement('p');text.textContent=Math.floor(segment.startMs/1000)+'〜'+Math.ceil(segment.endMs/1000)+'秒 '+segment.text;block.append(text);const seek=document.createElement('button');seek.textContent='この区間の冒頭から再生';seek.onclick=()=>{if(token===session)meetingPlay(item,Math.floor(segment.startMs/10000),segment.startMs%10000).catch(e=>note(e.message));};block.append(seek);}if(!data.items?.length){const empty=document.createElement('p');empty.textContent='時刻に対応する保存済みの文字起こし区間がありません。後処理が未実施か、過去形式の記録です。';block.append(empty);}card.append(block);}catch(e){if(token===session)note(e.message);}};card.append(timeline);for(const mark of item.bookmarks||[]){const bookmark=document.createElement('button');bookmark.textContent=Math.floor(mark.audioMs/60000)+':'+String(Math.floor(mark.audioMs/1000)%60).padStart(2,'0')+' '+mark.label+' から再生';bookmark.onclick=()=>meetingPlay(item,Math.min(Math.max(0,item.parts-1),Math.floor(mark.audioMs/10000)),mark.audioMs%10000).catch(e=>note(e.message));card.append(bookmark);const editNote=document.createElement('button');editNote.textContent='しおりのメモを見る・編集';editNote.onclick=async()=>{const session=token;try{const notes=await api('bookmark-notes?id='+encodeURIComponent(item.id));if(token!==session)return;const old=notes[mark.id]||{revision:0,text:''},text=prompt('しおりメモ（800文字まで）。録音・元のしおりは変更しません。',old.text);if(text===null||token!==session)return;await api('bookmark-note',{meeting:item.id,bookmark:mark.id,revision:old.revision,text});if(token===session)note('しおりメモを保存しました');}catch(e){if(token===session)note(e.message);}};card.append(editNote);}const analyze=document.createElement('button');analyze.textContent=item.analyzing?'文字起こし・要約中':'文字起こし・AI要約（Geminiへ音声を送信）';analyze.disabled=item.analyzing;analyze.onclick=async()=>{analyze.disabled=true;try{const preview=await api('reliability-meeting-preview?id='+encodeURIComponent(item.id));if(!confirm(preview.message+'\n未処理音声：約'+Math.ceil(preview.audioMs/1000)+'秒 / '+preview.audioBytes+'バイト / 最大'+preview.maximumRequests+'回'))return;await api('meeting-analyze',{id:item.id,token:preview.token});note('保存した録音の文字起こしを開始しました。途中で止まった場合は再実行できます');}catch(e){note(e.message);}finally{await loadMeetings();}};card.append(analyze);const verify=document.createElement("button");verify.textContent="保存区間・欠損を確認";verify.onclick=async()=>{verify.disabled=true;try{const v=await api("meeting-verify",{id:item.id});note(v.message+"（"+v.checkedParts+"/"+v.totalParts+"区間）");}catch(e){note(e.message);}finally{verify.disabled=false;}};card.append(verify);if(item.summary){const summary=document.createElement('pre');summary.style.whiteSpace='pre-wrap';summary.textContent=item.summary;card.append(summary);const actions=document.createElement('button');actions.textContent='要約から保留候補を確認';actions.onclick=async()=>{actions.disabled=true;const session=token;try{const data=await api('meeting-action-candidates',{id:item.id});if(token!==session||!list.contains(card))return;const block=document.createElement('div');const hint=document.createElement('p');hint.textContent='要約の記述からの候補です。日時・担当を確認して編集できます。予定は登録しません。';block.append(hint);if(!data.items.length){const empty=document.createElement('p');empty.textContent='保留候補は見つかりませんでした。';block.append(empty);}for(const candidate of data.items){const input=document.createElement('textarea');input.value=candidate.text;input.maxLength=800;input.setAttribute('aria-label','保留する内容');block.append(input);const save=document.createElement('button');save.textContent='確認した内容を受信箱に保留';save.onclick=async()=>{if(!input.value.trim()){note('保留する内容を入力してください');return;}if(!confirm('この内容を保留保存しますか？\n'+input.value+'\n予定は登録しません。'))return;save.disabled=true;try{const result=await api('meeting-action-save',{id:item.id,sourceHash:data.sourceHash,index:candidate.index,text:input.value});if(token!==session)return;save.textContent=result.duplicate?'この候補は保存済み':'保留保存済み';save.disabled=true;input.disabled=true;loadInboxSummary();}catch(e){if(token===session){note(e.message);save.disabled=false;}}};block.append(save);}card.append(block);}catch(e){if(token===session)note(e.message);}finally{if(token===session)actions.disabled=false;}};card.append(actions);const saveSummary=document.createElement('button');saveSummary.textContent='要約を保存';saveSummary.onclick=()=>downloadText(item.summary,(item.title||'会議記録')+'-要約.txt');card.append(saveSummary);}if(item.hasTranscript||item.summary){const transcript=document.createElement('button');transcript.textContent='文字起こしを見る';transcript.onclick=async()=>{const expectedToken=token;try{const res=await fetch('/web/api/meeting-transcript?id='+encodeURIComponent(item.id),{headers:{'X-Loki-Web-Token':token},cache:'no-store'});if(!res.ok)throw Error('文字起こしを取得できません');const text=await res.text();if(token&&token===expectedToken){$('meetingText').textContent=text;downloadText(text,(item.title||'会議記録')+'-文字起こし.txt');}}catch(e){note(e.message);}};card.append(transcript);}list.append(card);}if(!result.items.length)list.textContent='転送済みの会議録音はありません。';}
$('meetingRefresh').onclick=()=>refreshMeetings().catch(e=>note(e.message));$('meetingPart').onchange=()=>{if(meetingCurrent)meetingPlay(meetingCurrent,Number($('meetingPart').value)).catch(e=>note(e.message));};$('meetingAudio').onended=()=>{if(meetingCurrent){const next=Number($('meetingPart').value)+1;if(next<meetingCurrent.parts)meetingPlay(meetingCurrent,next).catch(e=>note(e.message));}};
setInterval(()=>{if(!document.hidden&&token&&activeTab==='meetings')refreshMeetings().catch(e=>note(e.message));},5000);
function downloadText(text,name){const blob=URL.createObjectURL(new Blob([text],{type:'text/plain;charset=utf-8'}));const link=document.createElement('a');link.href=blob;link.download=name.replace(/[\\/:*?"<>|]/g,'_');link.click();setTimeout(()=>URL.revokeObjectURL(blob),1000);}
async function loadMeetingControl(){const expectedToken=token;const result=await api('meeting-control');if(!token||token!==expectedToken)return;const state=result.state,request=result.request;const connected=!!state.connected;let text=connected?(state.recording?state.status:'MTGは停止中'):'グラスの接続が確認できません。ロキとスマホの接続を確認してください';if(state.progress){const p=state.progress;text+=' · 音声取得 '+Math.floor(p.capturedMs/1000)+'秒 / 書込 '+Math.floor(p.writtenMs/1000)+'秒 / 確定 '+Math.floor(p.sealedMs/1000)+'秒'+(p.pendingMs>2000?' · 保存待ち '+Math.floor(p.pendingMs/1000)+'秒':'');}if(request.status)text+=' · 操作：'+request.status;$('meetingControlStatus').textContent=text;$('meetingStart').disabled=meetingControlBusy||!connected||!!state.recording;$('meetingStop').disabled=meetingControlBusy||!connected||!state.recording;$('meetingTransfer').disabled=meetingControlBusy||!connected||!!state.recording;}
async function meetingControl(action){if(meetingControlBusy)return;const mode=Number($('meetingMode').value);if(action==='stop'&&!confirm('録音を保存し、このMTGのTOPIC・連続写真も終了しますか？'))return;if(action==='start'&&!confirm($('meetingMode').selectedOptions[0].textContent+'を開始しますか？'+(mode>0?' TOPIC用の周囲音・カメラ画像をGeminiへ送信します。':'')))return;meetingControlBusy=true;try{await api('meeting-control',{action,mode,requestId:newRequestId()});$('meetingControlStatus').textContent='グラスへの送信待ちです。実行結果を確認中…';}finally{meetingControlBusy=false;await loadMeetingControl();}}
on('meetingStart',()=>meetingControl('start'));on('meetingStop',()=>meetingControl('stop'));on('meetingTransfer',()=>meetingControl('transfer'));
async function loadMeetingImports(){const expectedToken=token;const result=await api('meeting-import');if(!token||token!==expectedToken)return;const signature=JSON.stringify(result);if(signature===meetingImportSignature)return;meetingImportSignature=signature;const list=$('meetingImportList');list.replaceChildren();for(const item of result.items){if(item.state==='complete'||item.state==='cancelled'||item.state==='expired')continue;const card=document.createElement('article');const p=document.createElement('p');p.textContent=item.title+' · '+item.status+(item.state==='uploading'?' · '+Math.floor(item.next/item.chunks*100)+'%':'');card.append(p);if(item.state!=='converting'){if(item.next===item.chunks){const retry=document.createElement('button');retry.textContent='音声抽出を再試行';retry.onclick=async()=>{try{await api('meeting-import-finish',{id:item.id});await loadMeetingImports();}catch(e){note(e.message);}};card.append(retry);}const cancel=document.createElement('button');cancel.textContent='この読み込みを取り消す';cancel.onclick=async()=>{try{await api('meeting-import-cancel',{id:item.id});await loadMeetingImports();}catch(e){note(e.message);}};card.append(cancel);}list.append(card);}}
async function refreshMeetings(){if(!token||activeTab!=='meetings')return;await Promise.all([loadMeetingControl(),loadMeetings(),loadMeetingImports()]);}
function encodeChunk(bytes){let text='';for(let i=0;i<bytes.length;i+=8192)text+=String.fromCharCode(...bytes.subarray(i,i+8192));return btoa(text).replace(/\+/g,'-').replace(/\//g,'_');}
async function uploadMeetingFile(){if(meetingUploadController)return;const file=$('meetingFile').files[0];if(!file)throw Error('録画・録音ファイルを選択してください');if(file.size>2147483648||file.size===0)throw Error('ファイルは2GB以下にしてください');const expectedToken=token;meetingUploadController=new AbortController();const controller=meetingUploadController;$('meetingUpload').disabled=true;$('meetingUploadCancel').hidden=false;meetingUploadId='';try{const job=await api('meeting-import-begin',{name:file.name,size:file.size},controller.signal);meetingUploadId=job.id;for(let index=0;index<job.chunks;index++){if(!token||token!==expectedToken||controller.signal.aborted)throw new DOMException('転送を中止しました','AbortError');const bytes=new Uint8Array(await file.slice(index*job.chunkSize,Math.min(file.size,(index+1)*job.chunkSize)).arrayBuffer());const digest=await crypto.subtle.digest('SHA-256',bytes);const sha=Array.from(new Uint8Array(digest),n=>n.toString(16).padStart(2,'0')).join('');const packet={id:job.id,index,sha,data:encodeChunk(bytes)};let ack;for(let attempt=0;attempt<3;attempt++){try{ack=await api('meeting-import-chunk',packet,controller.signal);break;}catch(error){if(controller.signal.aborted||!token||attempt===2)throw error;}}if(ack.sha!==sha)throw Error('ファイルの転送ハッシュが一致しません');$('meetingUploadStatus').textContent='転送中：'+Math.floor((index+1)/job.chunks*100)+'%';}await api('meeting-import-finish',{id:job.id},controller.signal);$('meetingUploadStatus').textContent='転送完了。スマホで音声を抽出中です。完了後、保存した会議記録から文字起こし・AI要約を開始できます';$('meetingFile').value='';}catch(error){if(token&&token===expectedToken){$('meetingUploadStatus').textContent=error.name==='AbortError'?'転送を中止しました':error.message;if(meetingUploadId&&controller.signal.aborted){try{await api('meeting-import-cancel',{id:meetingUploadId});}catch{}}}throw error;}finally{meetingUploadController=null;$('meetingUpload').disabled=false;$('meetingUploadCancel').hidden=true;if(token&&token===expectedToken)await refreshMeetings();}}
on('meetingUpload',uploadMeetingFile);on('meetingUploadCancel',()=>{if(meetingUploadController)meetingUploadController.abort();});



$('resetGenerationParent').onclick=()=>{generationParentRevision++;generatedParent='';$('generationParent').textContent='';$('resetGenerationParent').hidden=true;sendId='';};
$('chatImage').addEventListener('change',()=>{if(generatedParent)$('resetGenerationParent').click();});
let generatedLoading=false;
async function loadGenerated(){if(!token||$('chat').hidden||generatedLoading)return;generatedLoading=true;try{const sessionToken=token;const [images,videos]=await Promise.all([api('image-generations'),api('video-generations')]);if(token!==sessionToken)return;const items=[...images.items.map(j=>({...j,mediaKind:'IMAGE'})),...videos.items.map(j=>({...j,mediaKind:'VIDEO'}))].sort((a,b)=>b.time-a.time);const signature=JSON.stringify(items);if(signature===generatedSignature)return;const list=$('generatedList');if([...list.querySelectorAll('video')].some(v=>!v.paused&&!v.ended))return;generatedSignature=signature;const epoch=++generatedEpoch;for(const v of list.querySelectorAll('video')){v.pause();v.removeAttribute('src');v.load();}for(const u of generatedUrls)URL.revokeObjectURL(u);generatedUrls=[];list.replaceChildren();
 for(const j of items){if(epoch!==generatedEpoch||token!==sessionToken)return;const video=j.mediaKind==='VIDEO',card=document.createElement('article'),label=document.createElement('p');card.dataset.id=j.id;card.dataset.mediaKind=j.mediaKind;label.textContent=(video?'動画':'画像')+' · '+j.prompt+' · '+({queued:'受付済み',submitting:'APIへ受付中…',running:'生成中… 会話を続けられます',downloading:'転送中…',saving:'保存中…',completed:'完成',paused:'結果確認を休止中'}[j.state]||'生成失敗')+(j.error?' · '+j.error:'')+(j.statusError?' · '+j.statusError:'')+(j.galleryError?' · '+j.galleryError:'');card.append(label);
 if(j.state==='completed'&&!video){const image=document.createElement('img');image.alt='生成画像';image.className='log-thumbnail';card.append(image);try{const blob=await api('generated-image?id='+encodeURIComponent(j.id)+'&size=thumb');if(epoch!==generatedEpoch||token!==sessionToken)return;const url=URL.createObjectURL(blob);generatedUrls.push(url);image.src=url;}catch(e){label.textContent+=' · プレビュー取得失敗';}
 const follow=document.createElement('button');follow.type='button';follow.textContent='この画像から続ける';follow.onclick=()=>{generationParentRevision++;generatedParent=j.id;$('generationParent').textContent='この画像を元にします。変更内容や動きをそのまま入力してください';$('resetGenerationParent').hidden=false;sendId='';$('message').focus();};card.append(follow);const save=document.createElement('button');save.type='button';save.textContent='画像を保存';save.onclick=async()=>{try{const blob=await api('generated-image?id='+encodeURIComponent(j.id));const u=URL.createObjectURL(blob),a=document.createElement('a');a.href=u;a.download='Loki-generated-'+j.id+(j.mime==='image/png'?'.png':j.mime==='image/webp'?'.webp':'.jpg');a.click();setTimeout(()=>URL.revokeObjectURL(u),30000);}catch(e){note(e.message);}};card.append(save);}
 if(j.state==='completed'&&video){const player=document.createElement('video');player.controls=true;player.preload='none';player.playsInline=true;player.className='log-thumbnail';player.setAttribute('aria-label','生成動画');card.append(player);let objectUrl='';const fetchVideo=async()=>{if(objectUrl)return objectUrl;const blob=await api('generated-video?id='+encodeURIComponent(j.id));if(epoch!==generatedEpoch||sessionToken!==token)return '';objectUrl=URL.createObjectURL(blob);generatedUrls.push(objectUrl);return objectUrl;};const play=document.createElement('button');play.type='button';play.textContent='動画を再生';play.onclick=async()=>{play.disabled=true;try{const url=await fetchVideo();if(url){player.src=url;await player.play();}}catch(e){note(e.message);}finally{play.disabled=false;}};card.append(play);const save=document.createElement('button');save.type='button';save.textContent='動画を保存';save.onclick=async()=>{try{const url=await fetchVideo();if(url){const a=document.createElement('a');a.href=url;a.download='Loki-video-'+j.id+'.mp4';a.click();}}catch(e){note(e.message);}};card.append(save);}
 if(video&&j.canResume){const resume=document.createElement('button');resume.type='button';resume.textContent='結果確認を再開';resume.onclick=async()=>{try{await api('generated-video-resume',{id:j.id});generatedSignature='';await loadGenerated();}catch(e){note(e.message);}};card.append(resume);}
 if(!['queued','submitting','running','downloading','saving'].includes(j.state)){const del=document.createElement('button');del.type='button';del.textContent='ロキ内から削除';del.onclick=async()=>{if(!confirm('ロキ内の生成結果と指示を削除します。ギャラリーのコピーは残ります。'))return;try{await api(video?'generated-video-delete':'generated-delete',{id:j.id});generatedSignature='';if(generatedParent===j.id)$('resetGenerationParent').click();await loadGenerated();}catch(e){note(e.message);}};card.append(del);}list.append(card);
 }}finally{generatedLoading=false;}}
setInterval(()=>{if(!document.hidden){loadGenerated().catch(e=>note(e.message));loadSentImageLog().catch(()=>{});}},4000);
$('generatedPanel').addEventListener('toggle',()=>{if($('generatedPanel').open)loadGenerated().catch(e=>note(e.message));});

function inboxSummary(data){const all=data.items||[],pending=all.filter(x=>x.state==='pending'),uncertain=all.filter(x=>inboxStage(x)==='needs_check'),review=all.filter(x=>inboxStage(x)==='review_wait');
 $('secretaryInboxSummary').textContent='秘書の受信箱：未完了 '+pending.length+'件'+(review.length?'・登録確認待ち '+review.length+'件':'')+(uncertain.length?'・結果要確認 '+uncertain.length+'件':'')+(pending.length?' ／ '+pending.slice(0,2).map(x=>x.title).join(' ／ '):'');}
let inboxSummaryLoading=false;
async function loadInboxSummary(){if(!token||$('chat').hidden||inboxSummaryLoading)return;inboxSummaryLoading=true;const session=token;try{const data=await api('secretary-inbox');if(token===session)inboxSummary(data);}catch(e){if(token===session)$('secretaryInboxSummary').textContent='受信箱の確認に失敗しました。展開して再確認してください';}finally{inboxSummaryLoading=false;}}
setInterval(()=>{if(!document.hidden)loadInboxSummary();},30000);
function inboxStage(item){return item.displayStage||(['pending','completed','cancelled'].includes(item.state)?item.state:'needs_check');}
function inboxLabel(item){if(item.manualCompleted)return '手動完了（予定登録とは別）';return ({pending:'保留',review_wait:'登録確認待ち',review_expired:'登録確認の期限切れ',completed:'登録完了',cancelled:'取消',needs_check:'登録結果要確認'})[inboxStage(item)]||'登録結果要確認';}
function inboxMatches(item,filter,query){const stage=inboxStage(item),closed=['completed','cancelled'].includes(stage);const match=filter==='all'||filter==='active'&&!closed||filter==='review'&&['review_wait','review_expired'].includes(stage)||filter==='uncertain'&&stage==='needs_check'||filter==='closed'&&closed;return match&&(!query||[item.title,item.subject,item.date_text,item.conditions,item.request].join(' ').toLocaleLowerCase().includes(query.toLocaleLowerCase()));}
function prepareInboxResume(card){
 if(chatImageData||generatedParent){note('送信前の添付・編集元を外してから保留案件を選んでください');return;}
 const text='受信箱の案件ID:'+card.id+' を登録して';if($('message').value.trim()&&$('message').value!==text&&!confirm('入力中の文章を、保留案件の確認依頼へ置き換えますか？'))return;
 setComposerMessage(text);sendId='';$('message').focus();note('案件を会話欄へ戻しました。送信後に登録内容を確認します。まだ送信・登録していません。');
}
function renderSecretaryInbox(){
 const host=$('secretaryInboxList');host.replaceChildren();const session=token,filter=$('secretaryInboxFilter').value,query=$('secretaryInboxSearch').value.trim();
 const rank={needs_check:0,review_wait:1,review_expired:2,pending:3,completed:4,cancelled:5};
 const items=inboxItems.filter(item=>inboxMatches(item,filter,query)).slice().sort((a,b)=>(rank[inboxStage(a)]??0)-(rank[inboxStage(b)]??0));
 for(const item of items){
  const row=document.createElement('article'),open=document.createElement('button');open.type='button';open.textContent='['+inboxLabel(item)+'] '+item.title+(item.date_text?' ／ '+item.date_text:'');
  open.onclick=async()=>{try{const {item:card}=await api('secretary-inbox?id='+encodeURIComponent(item.id));if(token!==session||!row.isConnected)return;row.replaceChildren(open);
   const detail=document.createElement('pre');detail.style.whiteSpace='pre-wrap';let reference=card.reference;try{reference=JSON.parse(reference).conversation.slice().reverse().map(r=>r.kind+'：'+r.message).join('\n\n');}catch{}
   detail.textContent='状態：'+inboxLabel(card)+'\n'+(card.subject?'対象：'+card.subject+'\n':'')+(card.date_text?'日程：'+card.date_text+'\n':'')+(card.conditions?'条件：'+card.conditions+'\n':'')+card.request+'\n\n保存時点の参照情報：\n'+reference+'\n\n日程・出典は保存時点の情報です。';row.append(detail);
   if(inboxStage(card)==='needs_check'){const warning=document.createElement('p');warning.textContent='登録結果を確認できません。カレンダーを確認してください。自動で再登録しません。';row.append(warning);}
   if(card.state==='pending'){const resume=document.createElement('button');resume.type='button';resume.textContent='会話欄で登録内容を確認する';resume.onclick=()=>prepareInboxResume(card);row.append(resume);}
   for(const [action,title] of [['complete_manual','手動完了（予定登録とは別）'],['cancel','保留を取り消す'],['delete','案件を削除']]){if((action==='cancel'||action==='complete_manual')&&card.state!=='pending')continue;const btn=document.createElement('button');btn.type='button';btn.textContent=title;btn.onclick=async()=>{if(!confirm(title+'？ 登録済みの予定は削除しません。'))return;try{await api('secretary-inbox-action',{id:card.id,revision:card.revision,action});await loadSecretaryInbox();}catch(e){note(e.message);}};row.append(btn);}
  }catch(e){note(e.message);}};row.append(open);host.append(row);
 }if(!items.length)host.textContent=inboxItems.length?'条件に合う案件はありません。表示条件を変更してください。':'保存された案件はありません。';
}
async function loadSecretaryInbox(){
 const host=$('secretaryInboxList');host.textContent='読み込み中…';const session=token;
 try{const data=await api('secretary-inbox');if(token!==session)return;inboxSummary(data);inboxItems=data.items||[];renderSecretaryInbox();}catch(e){if(token===session)host.textContent=e.message;}
}
$('secretaryInboxFilter').addEventListener('change',renderSecretaryInbox);$('secretaryInboxSearch').addEventListener('input',renderSecretaryInbox);
$('secretaryInboxRefresh').onclick=loadSecretaryInbox;
$('secretaryInboxPanel').addEventListener('toggle',()=>{if($('secretaryInboxPanel').open&&token)loadSecretaryInbox();});


let workspaceSignature='';
function calendarEditDiff(before,after){const lines=[];if(before.calendar_name!==after.calendar_name)lines.push('登録先：'+before.calendar_name+' → '+after.calendar_name);const labels={title:'予定名',start:'開始',end:'終了',location:'場所',all_day:'終日'};if(before.events.length!==after.events.length)throw Error('予定件数が変わっています');after.events.forEach((e,i)=>{for(const key of Object.keys(labels))if(String(before.events[i][key]??'')!==String(e[key]??''))lines.push('予定'+(i+1)+' '+labels[key]+'：'+before.events[i][key]+' → '+e[key]);});return lines.join('\n');}
async function loadWorkspace(){if(!token||$('chat').hidden)return;const session=token,data=await api('workspace');if(token!==session)return;$('workspaceStatus').textContent=(data.connected?'グラスと接続中':'グラスの状態は未確認・古い可能性があります')+' · '+(data.commandStatus||'');const signature=JSON.stringify([data.referenceMode,data.referenceAt,data.draftAt,data.recognition,data.inboxTitle,data.referent]);if(signature===workspaceSignature)return;const body=$('workspaceBody');if(body.contains(document.activeElement)){return;}workspaceSignature=signature;body.replaceChildren();const line=document.createElement('p');line.textContent='参照先：'+(data.referenceMode||'auto')+(data.inboxTitle?' ／ '+data.inboxTitle:'');body.append(line);
if(data.referent&&Array.isArray(data.referent.candidates)&&data.referent.candidates.length){const label=document.createElement('p');label.textContent=data.referent.candidates.length>1?'参照候補が複数あります（未確定）':'参照候補（未確定）';body.append(label);for(const candidate of data.referent.candidates.slice(0,2)){const row=document.createElement('p');row.textContent='・'+String(candidate.text||'').slice(0,100);body.append(row);}const help=document.createElement('p');help.textContent='違う場合は対象の名前を添えて訂正してください。予定登録などの同意には使いません。';body.append(help);}
const command=async value=>{try{await api('workspace-command',{...value,requestId:newRequestId()});note('グラスへ送信待ちです。反映状況を確認してください。まだ予定は登録していません。');}catch(e){note(e.message);}};
for(const [mode,label] of [['auto','自動'],['image','直近の画像を使う'],['none','画像を使わず話す']]){const b=document.createElement('button');b.type='button';b.textContent=label;b.onclick=()=>command({action:'reference',mode,referenceAt:data.referenceAt||0});body.append(b);}
const r=data.recognition;if(r){const line=document.createElement('p');line.textContent='認識：'+r.source+' / '+r.phase+' ／ '+(r.text||'認識文はまだありません');body.append(line);if(r.text){const use=document.createElement('button');use.type='button';use.textContent='認識文を会話欄で直す';use.onclick=()=>{if(chatImageData||chatImagePreparing||generatedParent){note('送信前の添付・編集元があります。添付付き依頼は送信控えから復元してください');return;}if(r.phase!=='RECOGNIZED'||!r.at||Date.now()<r.at||Date.now()-r.at>900000){note('認識文の期限切れ・未確定です。状態を更新してください');return;}if($('message').value.trim()&&!confirm('入力中の文章を認識文に置き換えますか？'))return;setComposerMessage(r.text);imageReferenceDraft=null;recoveryNeedsConfirm=true;sendId='';$('message').focus();note('会話欄へ戻しました。まだ送信していません。');};body.append(use);}}
if(data.calendarDraft){const check=document.createElement('button');check.type='button';check.textContent='保存済み候補の重複を確認（登録しません）';check.onclick=async()=>{check.disabled=true;try{const result=await api('calendar-preview',data.calendarDraft);if(body.contains(check)){const p=document.createElement('p');p.textContent=result.message;body.append(p);}}catch(e){$('notice').textContent=e.message;}finally{check.disabled=false;}};body.append(check);const note=document.createElement('p');note.textContent='予約候補の訂正。訂正後、グラスで内容を再確認します。登録はしません。終日の終了日は最終日の翌日、時刻指定はオフセット付きISO日時です。';body.append(note);const field=(label,value)=>{const l=document.createElement('label');l.textContent=label;const input=document.createElement('input');input.value=value||'';l.append(input);body.append(l);return input;};const calendar=field('カレンダー',data.calendarDraft.calendar_name),events=data.calendarDraft.events.map(e=>{const inputs={};for(const [k,label] of [['title','予定名'],['start','開始'],['end','終了'],['location','場所']])inputs[k]=field(label,e[k]);const day=document.createElement('input');day.type='checkbox';day.checked=!!e.all_day;const l=document.createElement('label');l.textContent='終日';l.append(day);body.append(l);return {inputs,day};});const b=document.createElement('button');b.type='button';b.textContent='訂正してグラスで再確認';b.onclick=()=>{const candidate={calendar_name:calendar.value,events:events.map(e=>({...Object.fromEntries(Object.entries(e.inputs).map(([k,v])=>[k,v.value])),all_day:e.day.checked}))};const diff=calendarEditDiff(data.calendarDraft,candidate);if(!diff){$('notice').textContent='変更はありません';return;}if(confirm('訂正する内容\n'+diff+'\nまだ登録しません。訂正候補を送りますか？'))command({action:'calendar_edit',baseAt:data.draftAt,candidate});};body.append(b);}}
$('workspaceRefresh').onclick=()=>{workspaceSignature='';loadWorkspace().catch(e=>note(e.message));};$('workspacePanel').addEventListener('toggle',()=>{if($('workspacePanel').open)loadWorkspace().catch(e=>note(e.message));});
setInterval(()=>{if(!document.hidden&&$('workspacePanel').open)loadWorkspace().catch(e=>note(e.message));if(!token){workspaceSignature='';$('workspaceBody').replaceChildren();$('workspaceStatus').textContent='';}},5000);
$('navigationHealthRefresh').onclick=async()=>{const session=token;try{const h=await api('navigation-health');if(token===session)$('navigationHealthStatus').textContent=h.message+' ／ 目的地：'+(h.selectedDestination||'未設定')+' ／ 現在地の経過：'+(h.locationAgeMs<0?'不明':Math.floor(h.locationAgeMs/1000)+'秒');}catch(e){note(e.message);}};
$('navigationHealthRetry').onclick=async()=>{if(!confirm('現在地と設定済みの目的地を経路APIへ送信して再取得します。目的地は維持しますが候補は変わる場合があります。続けますか？'))return;try{await api('navigation-retry',{});note('再取得を依頼しました。状態を確認してください。');}catch(e){note(e.message);}};

let operationsLoading=false;
function healthDescription(j){if(!j||!j.at)return 'まだ検査結果がありません';const label=({passed:'合格',warning:'権限などの確認が必要',failed:'確認が必要'})[j.overall]||'未確認';const names={storage:'保存領域',vault:'暗号化保存の往復',assets:'同梱ファイル（画像は代表1点）',queue:'依頼記録形式',microphone:'マイク許可',camera:'カメラ許可'};return label+' ／ v'+j.version+(j.version!==j.currentVersion?'（別バージョンの結果）':'')+(j.running?'・検査中':'')+' ／ '+new Date(j.at).toLocaleString()+'\n'+(j.checks||[]).map(x=>(names[x.id]||x.id)+'：'+(x.state==='warning'?(x.id==='camera'||x.id==='microphone'?'未許可':'未準備'):({passed:'確認済み',not_required:'対象外（このアプリは要求しません）',failed:'要確認'}[x.state]||'未確認'))).join(' ／ ');}
function transferDescription(j){const kind=j.kind==='photo'?'写真':'録音',phase=({RUNNING:'転送中',COMPLETED:'転送処理完了',PARTIAL:'一部未確認',FAILED:'転送を完了できませんでした',INTERRUPTED:'前回中断・結果確認が必要'})[j.phase]||'結果未確認';return kind+'：'+phase+' ／ スマホ保存確認 '+j.confirmed+' / '+j.expected+'件・保存未確認 '+Math.max(0,j.expected-j.confirmed-j.retained)+'件（失敗・別処理待ちを含む）'+(j.retained?'・過去削除を尊重してグラス保持 '+j.retained+'件':'')+(j.deferred?'・別の取り込み待ち '+j.deferred+'件':'')+(j.failed?'・失敗 '+j.failed+'件':'')+(j.unitsExpected?' ／ 保存区間 '+j.unitsConfirmed+' / '+j.unitsExpected:'');}
async function loadOperations(){if(!token||operationsLoading||$('chat').hidden)return;operationsLoading=true;const session=token;try{const data=await api('operations');if(token!==session)return;const body=$('operationsBody');body.replaceChildren();$('operationsStatus').textContent='未完了の会話を優先表示。保留案件と直近の端末操作。結果未確認を自動再送しません。';const line=value=>{const p=document.createElement('p');p.textContent=value;body.append(p);};if(data.connection)line(data.connection);if(data.storage){const v=data.storage;line(v.unavailable?'保存先の在庫は確認できません':'スマホのロキ：'+v.phonePhotos+'枚 / READ：'+v.readPhotos+'枚 / ギャラリー保存時確認記録：'+v.readGalleryReceipts+'枚'+(v.partial?'（一部未集計）':'')+'。最大30秒前の集計。ギャラリーの現在の存在・PC保存は未確認。グラスの残件は転送記録で別に確認します。');}if(data.diagnosticReport){const report=document.createElement('button');report.type='button';report.textContent='個人情報を除いた不具合報告';report.onclick=()=>{const pre=document.createElement('pre');pre.textContent=JSON.stringify(data.diagnosticReport,null,2);body.append(pre);const copy=document.createElement('button');copy.type='button';copy.textContent='この報告をコピー';copy.onclick=async()=>{try{await navigator.clipboard.writeText(pre.textContent);note('報告をコピーしました。自動送信はしていません');}catch(e){note('コピーできません。表示した報告を選択してコピーしてください');}};body.append(copy);note('会話本文・場所・予定名・ID・生のエラー・認証情報を含めない報告です。自動送信はしません。');};body.append(report);}const phases={OFF:'停止中',SPEAKING:'読み上げ中',RECORDING:'聞き取り中',RECOGNIZING:'音声認識中',ANALYZING:'解析・回答待ち',PAUSED:'一時待機',WAITING:'次の入力・話題待ち'};line((data.transferStateFresh?'':'前回受信の状態（現在は未確認） ／ ')+'VOICE：'+(phases[data.activity?.voice]||'状態未確認')+' ／ TOPIC：'+(phases[data.activity?.topic]||'状態未確認'));if(data.activity?.pauseReason)line('待機理由：'+({NAVIGATION:'ナビ優先',BACKGROUND:'画面が非表示',USER_TURN:'利用者の会話・入力を優先',MENU:'メニュー操作中',STOPPING:'停止処理中'}[data.activity.pauseReason]||'状態未確認'));if(data.issues?.length)line('一部の保存記録を読み取れませんでした。読み取れる診断結果を表示します。自動修復・再送はしていません。');for(const row of data.cards||[]){const card=document.createElement('article');card.dataset.phase=row.phase;const title=document.createElement('strong');title.textContent=row.label+'：'+row.title;card.append(title);const id=document.createElement('p');id.textContent='ID '+row.id.slice(0,8)+(row.at?' ／ '+new Date(row.at).toLocaleString():'');card.append(id);if(row.detail){const detail=document.createElement('p');detail.textContent=row.detail;card.append(detail);}body.append(card);}line('転送：'+(data.transferStateFresh?'最近のグラス状態':'グラスの状態が古い可能性があります'));for(const transfer of data.transfers||[])line(transferDescription(transfer));if(!data.transfers?.length)line('転送結果はまだ届いていません。スマホ側の保存数だけでは、グラスの未転送件数は判断できません。');line('スマホのローカル検査\n'+healthDescription(data.phoneHealth));line('グラスのローカル検査\n'+healthDescription(data.glassHealth));line('ローカル検査は保存・暗号化・同梱ファイル・権限などを確認します。通信・会話・実録音の成功確認とは別です。修復や再送は行いません。');}catch(e){if(token===session)$('operationsStatus').textContent='状態を取得できません。依頼の再送はしていません。';}finally{operationsLoading=false;}}
$('operationsRefresh').onclick=loadOperations;$('operationsPanel').addEventListener('toggle',()=>{if($('operationsPanel').open)loadOperations();});$('operationsCheck').onclick=async()=>{const session=token;try{await api('local-health-check',{});if(token===session){$('operationsStatus').textContent='ローカル検査を開始しました。外部送信・撮影・録音は行いません。';setTimeout(loadOperations,2000);}}catch(e){note(e.message);}};setInterval(()=>{if(!document.hidden&&$('operationsPanel').open)loadOperations();},5000);

function deliveryProgressLabel(v,fresh){
 if(!v)return '会話の進行状態はまだ届いていません';
 const phases={STOPPED:'回答待ち中止済み',IDLE:'待機中',RUNNING:'回答待ち',ANSWERED:'回答完了',FAILED:'回答未完了',UNCONFIRMED:'結果未確認'},stages={PREPARE:'会話を整理中',ANSWER:'回答待ち',REVIEW:'回答を確認中',REPAIR:'回答を調整中'};
 const phase=phases[v.phase]||'結果未確認',label=v.phase==='RUNNING'?(stages[v.stage]||phase):phase;
 return (fresh?'':'最後の状態：')+label+(!fresh?'（現在の結果は未確認）':'')+(v.id?' ／ ID '+v.id.slice(0,8):'')+(v.reason?' ／ '+v.reason:'');
}
async function validateRecoveryImage(bytes){
 const blob=new Blob([bytes],{type:'image/jpeg'});
 if(typeof createImageBitmap==='function'){const bitmap=await createImageBitmap(blob);try{if(!bitmap.width||!bitmap.height)throw Error('復元する画像を読み取れません');}finally{bitmap.close();}}
 else {const url=URL.createObjectURL(blob);try{await new Promise((resolve,reject)=>{const image=new Image();image.onload=()=>image.naturalWidth?resolve():reject(Error('復元する画像を読み取れません'));image.onerror=()=>reject(Error('復元する画像を読み取れません'));image.src=url;});}finally{URL.revokeObjectURL(url);}}
 return blob;
}
async function preparePhotoFollowUp(id){
 if(!token||chatImagePreparing||$('send').disabled)throw Error('準備・送信が終わってから写真を選んでください');
 const session=token,snapshot=composerSnapshot(),before=$('message').value;
 const selected=await api('photo-followup?id='+encodeURIComponent(id));
 if(token!==session)return;
 if(snapshot!==composerSnapshot()||chatImagePreparing||$('send').disabled)throw Error('選択中に本文・添付が変わりました。入力は残しています。写真を選び直してください');
 const applied=await restoreRecoveryPayload({...selected.payload,message:before},true);
 if(applied&&token===session){$('chatImageStatus').textContent='追加質問の写真 '+chatImageCount+'件。送信ごとに写真もAIへ再送します';note('写真を確認して「右上は？」「この文字を訳して」などを入力してください。まだ送信していません。');}
}
async function restoreRecoveryPayload(payload,followUp=false){
 if(chatImagePreparing||$('send').disabled)throw Error('準備・送信が終わってから復元してください');
 if(!payload||typeof payload.message!=='string'||payload.message.length>$('message').maxLength)throw Error('復元する本文を確認してください。現在の入力は残しています');
 for(const key of ['parent','referenceKey'])if(payload[key]!==undefined&&(typeof payload[key]!=='string'||payload[key].length>512))throw Error('復元する参照情報を確認してください');
 if(payload.parent&&payload.image||payload.referenceKey&&(payload.parent||payload.image))throw Error('復元する本文と参照先の組み合わせを確認してください');
 const session=token,snapshot=composerSnapshot(),epoch=++recoveryRestoreGeneration;
 if((chatImageData||generatedParent||(!followUp&&$('message').value.trim()))&&!confirm(followUp?'入力中の添付・編集元を選んだ写真に置き換えますか？ 本文は残します。まだ送信しません。':'入力中の文章・添付・編集元を送信控えで置き換えますか？'))return false;
 let bytes=null,blob=null,count=0,kinds=[];
 if(payload.image){if(typeof payload.image!=='string'||payload.image.length>800000)throw Error('添付が大きすぎます');count=payload.imageCount??1;if(!Number.isInteger(count)||count<1||count>6)throw Error('添付の件数を確認してください');kinds=payload.kinds??Array(count).fill('unknown');if(!Array.isArray(kinds)||kinds.length!==count||kinds.some(k=>!['image','video','unknown'].includes(k))||kinds.filter(k=>k==='video').length>3)throw Error('添付の件数・種類を確認してください');const raw=atob(payload.image);bytes=Uint8Array.from(raw,c=>c.charCodeAt(0));blob=await validateRecoveryImage(bytes);}
 else if(payload.imageCount>0)throw Error('復元する添付の内容がありません');
 if(token!==session||epoch!==recoveryRestoreGeneration||snapshot!==composerSnapshot()||chatImagePreparing||$('send').disabled)throw Error('復元の準備中に接続・入力・添付が変わりました。現在の入力は残しています。選び直してください');
 const preparedUrl=blob?URL.createObjectURL(blob):'';
 clearChatImage();$('resetGenerationParent').click();setComposerMessage(payload.message);sendId='';imageReferenceDraft=payload.referenceKey?{key:payload.referenceKey,prefix:payload.message}:null;
 if(bytes){chatImageData=payload.image;chatImageCount=count;chatAttachmentKinds=kinds.slice();chatImageUrl=preparedUrl;$('chatImagePreview').src=chatImageUrl;$('chatImagePreview').hidden=false;if($('attachmentDraft'))$('attachmentDraft').hidden=false;$('clearChatImage').hidden=false;$('chatImageStatus').textContent='送信時の添付'+chatImageCount+'件を復元。動画は抜粋画像です。追加する場合は選び直してください';}
 if(payload.parent){generatedParent=payload.parent;generationParentRevision++;$('generationParent').textContent='送信控えの編集元を復元しました';$('resetGenerationParent').hidden=false;}
 recoveryNeedsConfirm=!followUp;$('message').focus();note(followUp?'写真への追加質問を準備しました。まだ送信していません。':'送信控えを復元しました。まだ送信していません。履歴・送信状況を確認してください');return true;
}
if($('recoveryRefresh'))$('recoveryRefresh').onclick=async()=>{const session=token;try{const result=await api('recovery');if(session!==token)return;const list=$('recoveryList');list.replaceChildren();for(const row of result.items||[]){const card=document.createElement('article'),p=document.createElement('p');p.textContent=new Date(row.at).toLocaleString()+' ／ '+row.message+(row.imageCount?'（添付'+row.imageCount+'件）':'');card.append(p);const use=document.createElement('button');use.type='button';use.textContent='内容・添付を入力へ戻す（送信なし）';use.onclick=async()=>{try{const item=await api('recovery?id='+encodeURIComponent(row.id));if(session!==token)return;await restoreRecoveryPayload(item.payload);}catch(e){note(e.message);}};card.append(use);list.append(card);}if(!result.items?.length)list.textContent='保存された送信控えはありません';}catch(e){note(e.message);}};
