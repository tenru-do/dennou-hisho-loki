const fs=require('fs'),path=require('path'),assert=require('assert/strict');
const {chromium}=require('C:/Users/user/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const base=path.resolve(__dirname,'../phone-app/assets/loki-web');
let checks=0,external=0;const ok=(value,message)=>{assert.ok(value,message);checks++;};
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',args:['--disable-background-networking','--disable-component-update']});
 const calls=[],errors=[];let status={state:'IDLE',active:false,totalKnown:false,total:null,confirmed:null,remaining:null,label:'取り込み状況：未実行'},fail=false,held=null,hold=false;
 let catalog=Array.from({length:1001},(_,i)=>({id:String(1793000000000+i)+'-'+String(i).padStart(36,'0'),time:1793000000000+i,metadata:{available:false}}));
 const image=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aEckAAAAASUVORK5CYII=','base64');
 try{
  const page=await browser.newPage({viewport:{width:1024,height:1366}});
  page.on('pageerror',error=>errors.push(error.message));page.on('dialog',dialog=>dialog.accept());
  await page.addInitScript(()=>sessionStorage.setItem('lokiWebToken','fixture-session'));
  await page.route('**/*',async route=>{
   const url=new URL(route.request().url());if(url.origin!=='http://keep.test'){external++;return route.abort();}
   if(url.pathname.startsWith('/web/api/')){
    const name=url.pathname.slice(9),data=route.request().method()==='POST'?route.request().postDataJSON():null;
    calls.push({name,data,offset:Number(url.searchParams.get('offset')||0),token:route.request().headers()['x-loki-web-token']});
    if(name==='photo-import-status'){
     if(hold){held=route;return;}
     if(fail)return route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({error:'合成の通信断'})});
     return route.fulfill({contentType:'application/json',body:JSON.stringify(status)});
    }
    if(name==='photo')return route.fulfill({contentType:'image/png',body:image});
    let reply={items:[],requests:[],offset:0,total:0};
    if(name==='photos'){const offset=Math.min(Number(url.searchParams.get('offset')||0),Math.max(0,Math.floor((catalog.length-1)/500)*500));reply={items:catalog.slice(offset,offset+500),offset,total:catalog.length,pageSize:500};}
    if(name==='delete'){catalog=catalog.filter(item=>!data.ids.includes(item.id));reply={deleted:data.ids.length};}
    if(name==='import'){status={state:'QUEUED',active:true,totalKnown:false,total:null,confirmed:null,remaining:null,label:'グラスへ依頼済み。対象枚数は確認待ちです'};reply={queued:true,id:'synthetic-import',mode:'copy'};}
    return route.fulfill({contentType:'application/json',body:JSON.stringify(reply)});
   }
   const name=url.pathname==='/web/'?'index.html':path.basename(url.pathname),file=path.join(base,name);
   if(!fs.existsSync(file))return route.fulfill({status:204});
   return route.fulfill({contentType:name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':'text/html',body:fs.readFileSync(file)});
  });
  const click=id=>page.evaluate(id=>document.getElementById(id).click(),id);
  const refresh=async next=>{status=next;await click('photoImportRefresh');await page.waitForFunction(label=>document.getElementById('photoImportStatus').textContent===label,next.label);};
  const count=()=>calls.filter(call=>call.name==='photo-import-status').length;
  await page.goto('http://keep.test/web/');
  await page.waitForFunction(()=>document.querySelectorAll('#grid .card').length===500);
  ok((await page.locator('#page').innerText()).includes('1〜500 / 1001枚'),'first page contains 500');
  ok(await page.locator('#previous').innerText()==='前の500枚'&&await page.locator('#next').innerText()==='次の500枚','navigation labels use 500');
  await click('next');await page.waitForFunction(()=>document.getElementById('page').textContent.includes('501〜1000'));
  ok(await page.locator('#grid .card').count()===500,'second page contains 500');
  await click('next');await page.waitForFunction(()=>document.getElementById('page').textContent.includes('1001〜1001'));
  ok(await page.locator('#grid .card').count()===1&&await page.locator('#next').isDisabled(),'last page and upper boundary');
  await click('previous');await page.waitForFunction(()=>document.getElementById('page').textContent.includes('501〜1000'));
  await click('previous');await page.waitForFunction(()=>document.getElementById('page').textContent.includes('1〜500'));
  await click('selectPage');ok((await page.locator('#selectedCountBottom').innerText()).includes('500枚'),'all 500 selectable from either toolbar');
  await click('deleteSelected');await page.waitForFunction(()=>document.getElementById('count').textContent==='501枚');
  ok(calls.find(call=>call.name==='delete').data.ids.length===500,'single page batch preserves actual cap of 500');
  ok(calls.filter(call=>call.name==='photos').some(call=>call.offset===1000),'page offsets advance by 500');
  const backend=fs.readFileSync(path.resolve(__dirname,'../phone-app/src/com/example/rokidgeminisecretary/LokiWebConsole.java'),'utf8');
  ok(/PHOTO_PAGE_SIZE\s*=\s*500/.test(backend)&&backend.includes('ids.length()>PHOTO_PAGE_SIZE'),'server page and delete cap share 500');
  ok(backend.indexOf('JSONObject session=authenticate')<backend.indexOf('path.equals("/web/api/photo-import-status")'),'progress endpoint follows authentication');
  await refresh({active:true,totalKnown:false,total:null,confirmed:null,remaining:null,label:'グラス受信済み。対象枚数は確認中です'});
  ok(await page.locator('#photoImportProgress').isHidden(),'unknown total does not render 0/0 or percentage');
  ok(await page.locator('#import').isDisabled(),'pending import cannot be requested twice');
  const running={state:'RUNNING',active:true,totalKnown:true,total:10,confirmed:3,retained:2,remaining:5,label:'保存確認3枚／対象10枚／残り未確認5枚／削除照合2枚'};
  await refresh(running);
  ok(await page.locator('#photoImportProgress').isVisible(),'known totals show progress');
  ok(await page.locator('#photoImportProgress').getAttribute('max')==='10'&&await page.locator('#photoImportProgress').getAttribute('value')==='5','meter counts verified saves and separately labelled exclusions');
  fail=true;await click('photoImportRefresh');await page.waitForFunction(()=>document.getElementById('photoImportConnection').textContent.includes('通信を確認できません'));
  ok(await page.locator('#photoImportStatus').innerText()===running.label,'communication failure retains last count');
  ok(await page.evaluate(()=>photoImportFailures===1&&photoImportTimer!==null),'failed polling backs off without concurrent work');fail=false;
  await refresh({...running,state:'PARTIAL',active:false,failed:1,label:'一部未完了：保存確認3枚／対象10枚／残り未確認5枚'});
  ok(await page.evaluate(()=>photoImportTimer===null)&&await page.locator('#import').isEnabled(),'terminal partial retains unresolved count and stops polling');
  await refresh({state:'COMPLETED',active:false,totalKnown:true,total:0,confirmed:0,retained:0,remaining:0,label:'取り込み処理完了：対象0枚'});
  ok(await page.locator('#photoImportProgress').isHidden()&&(await page.locator('#photoImportStatus').innerText()).includes('対象0枚'),'known empty completion is explicit without invalid progress denominator');
  const importsBefore=calls.filter(call=>call.name==='import').length;
  await page.evaluate(()=>{document.getElementById('import').click();document.getElementById('import').click();});
  await page.waitForFunction(()=>document.getElementById('photoImportStatus').textContent.includes('グラスへ依頼済み'));
  ok(calls.filter(call=>call.name==='import').length===importsBefore+1,'one explicit copy request only');
  ok(await page.locator('#photoImportProgress').isHidden(),'new request clears previous totals');
  await refresh(running);await click('chatTab');const beforeHidden=count();await page.waitForTimeout(5400);
  ok(count()===beforeHidden,'photo polling stops while another app tab is selected');
  await click('photosTab');await page.waitForFunction(()=>photoImportTimer!==null);
  await page.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,get:()=>true});document.dispatchEvent(new Event('visibilitychange'));});
  const beforeInvisible=count();await page.evaluate(()=>startPhotoImportPolling());await page.waitForTimeout(150);
  ok(count()===beforeInvisible&&await page.evaluate(()=>photoImportTimer===null),'hidden browser never polls');
  await page.evaluate(()=>{Object.defineProperty(document,'hidden',{configurable:true,get:()=>false});document.dispatchEvent(new Event('visibilitychange'));});
  await page.waitForFunction(()=>photoImportTimer!==null);
  let beforeBudget=count();await page.evaluate(()=>{stopPhotoImportPolling();photoImportUntil=Date.now()-1;return pollPhotoImportProgress(photoImportEpoch);});
  ok(count()===beforeBudget&&(await page.locator('#photoImportConnection').innerText()).includes('30分'),'time budget stops automatic traffic');
  beforeBudget=count();await page.evaluate(()=>{photoImportUntil=Date.now()+60000;photoImportPolls=360;return pollPhotoImportProgress(photoImportEpoch);});
  ok(count()===beforeBudget,'request-count budget also stops traffic if clock changes');
  hold=true;await click('photoImportRefresh');for(let i=0;!held&&i<50;i++)await page.waitForTimeout(10);ok(!!held,'delayed status fixture captured');
  await page.evaluate(()=>{token='new-session';document.dispatchEvent(new Event('loki-session-cleared'));});
  try{await held.fulfill({contentType:'application/json',body:JSON.stringify({...running,label:'OLD PRIVATE COUNT'})});}catch{}
  await page.waitForTimeout(80);ok(await page.locator('#photoImportStatus').innerText()==='','late old-session count cannot reappear');
  ok(calls.filter(call=>call.name==='photo-import-status').every(call=>call.token==='fixture-session'),'all progress requests authenticated');
  ok(errors.length===0,'no uncaught errors: '+errors.join(';'));ok(external===0,'no external requests');
  console.log(JSON.stringify({passed:checks,externalRequests:external,scope:'real browser UI and mocked local API; synthetic 1001 photos, no device/photo mutation'}));
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exit(1);});
