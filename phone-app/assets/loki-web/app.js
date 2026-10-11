'use strict';
const $=id=>document.getElementById(id);
let token=localStorage.getItem('lokiWebToken')||sessionStorage.getItem('lokiWebToken')||'', offset=0, items=[], total=0, selected=new Set(), generation=0, blobs=[], viewing=-1, largeBlob='', zoom=1, busy=false, historySignature='', sendId='';
const note=text=>{$('notice').textContent=text;};
async function api(path,data){
 const response=await fetch('/web/api/'+path,{method:data===undefined?'GET':'POST',headers:{'X-Loki-Web-Token':token,...(data===undefined?{}:{'Content-Type':'application/json'})},body:data===undefined?undefined:JSON.stringify(data),cache:'no-store'});
 if(response.status===401){token='';sessionStorage.removeItem('lokiWebToken');localStorage.removeItem('lokiWebToken');showAuth();throw Error('接続の期限が切れました。スマホでコードを発行してください');}
 if(!response.ok){let j;try{j=await response.json();}catch{}throw Error(j?.error||'処理できませんでした（'+response.status+'）');}
 return response.headers.get('Content-Type').startsWith('image/')?response.blob():response.json();
}
function on(id,action){$(id).addEventListener('click',()=>Promise.resolve().then(action).catch(e=>note(e.message)));}
function showAuth(){const connected=!!token;$('pair').hidden=connected;$('console').hidden=!connected;$('logout').hidden=!connected;if(!connected){generation++;blobs.forEach(URL.revokeObjectURL);blobs=[];$('grid').replaceChildren();closeViewer();$('history').replaceChildren();$('outbox').replaceChildren();historySignature='';}}
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
on('photosTab',()=>{$('photos').hidden=false;$('chat').hidden=true;$('photosTab').setAttribute('aria-pressed','true');$('chatTab').setAttribute('aria-pressed','false');});
on('chatTab',async()=>{$('photos').hidden=true;$('chat').hidden=false;$('photosTab').setAttribute('aria-pressed','false');$('chatTab').setAttribute('aria-pressed','true');await loadHistory();});
$('message').addEventListener('input',()=>{sendId='';});
$('chatForm').addEventListener('submit',async e=>{e.preventDefault();const message=$('message').value.trim();if(!message)return;$('send').disabled=true;sendId=sendId||crypto.randomUUID();try{await api('send',{message,requestId:sendId});$('message').value='';sendId='';note('グラスへ送信待ちです。返信は共通履歴に表示されます');await loadHistory();}catch(err){note(err.message);}finally{$('send').disabled=false;}});
setInterval(()=>{if(!document.hidden)loadHistory().catch(e=>note(e.message));},5000);
showAuth();if(token)loadPhotos().catch(e=>{note(e.message);if(!token)connectBrowser(invitation);});else connectBrowser(invitation);
