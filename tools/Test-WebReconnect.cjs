const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../phone-app/assets/loki-web/app.js'),'utf8');
const marker='// Connection hints never transfer credentials';
assert(source.includes(marker));
const panel={hidden:true},retry={disabled:false},events={},calls=[];let fail=false;
const window={fetch:async(url,opts={})=>{calls.push({url,...opts});if(fail)throw Error('offline');return {ok:true,status:200};},addEventListener:(name,fn)=>events[name]=fn};
vm.runInNewContext(source.slice(source.indexOf(marker)),{window,document:{getElementById:id=>id==='networkRecovery'?panel:retry},location:{pathname:'/web/'}});
(async()=>{events.offline();assert.equal(panel.hidden,false);fail=true;await assert.rejects(()=>window.fetch('/web/api',{method:'POST',body:'synthetic'}));assert.equal(calls.length,1,'failed mutation must not replay');assert.equal(panel.hidden,false);fail=false;await retry.onclick();assert.equal(calls.length,2);assert.equal(calls[1].method,'GET');assert.equal(calls[1].url,'/web/');assert.equal(panel.hidden,true);assert.equal(retry.disabled,false);console.log('PASS 7 reconnect UI / no replay checks');})();
