const fs=require('fs'),crypto=require('crypto'),tls=require('tls'),path=require('path');
const certificate=new crypto.X509Certificate(fs.readFileSync(path.join(__dirname,'phone-public-certificate.local.der')));
const fingerprint=certificate.fingerprint256;
function connect(request){return new Promise((resolve,reject)=>{
 let chunks=[],length=0,verified=false,settled=false;
 const socket=tls.connect({host:'127.0.0.1',port:18766,rejectUnauthorized:false},()=>{
  const peer=socket.getPeerCertificate();
  if(!peer.raw||new crypto.X509Certificate(peer.raw).fingerprint256!==fingerprint){socket.destroy();settled=true;reject(Error('Smartphone certificate changed; verification required.'));return;}
  verified=true;
  if(request)socket.write('GET '+request+' HTTP/1.1\r\nHost: localhost:18766\r\nConnection: close\r\n\r\n');
  else {settled=true;resolve(Buffer.alloc(0));socket.end();}
 });
 socket.setTimeout(10000,()=>{socket.destroy();if(!settled){settled=true;reject(Error('USB connection timeout'));}});
 socket.on('data',chunk=>{length+=chunk.length;if(length>524288){socket.destroy();if(!settled){settled=true;reject(Error('Web response exceeds limit'));}}else chunks.push(chunk);});
 socket.on('end',()=>{if(!settled){settled=true;if(!verified)reject(Error('TLS verification incomplete'));else resolve(Buffer.concat(chunks));}});
 socket.on('error',error=>{if(!settled){settled=true;reject(error);}});
 });}
(async()=>{
 if(process.argv.includes('--check-web')){
  for(const [route,marker] of [['/web/','chatForm'],['/web/app.js','function newRequestId']]){
   const response=await connect(route),boundary=response.indexOf('\r\n\r\n');
   if(boundary<0||boundary>8192)throw Error('Phone Web response headers invalid: '+route);
   const headers=response.subarray(0,boundary).toString('ascii'),body=response.subarray(boundary+4);
   const lengths=[...headers.matchAll(/^Content-Length:[ \t]*([0-9]+)[ \t]*\r?$/gim)];
   if(!/^HTTP\/1\.1 200 /.test(headers)||lengths.length!==1||/^Transfer-Encoding:/im.test(headers)||Number(lengths[0][1])!==body.length||!body.toString('utf8').includes(marker))throw Error('Phone Web response check failed: '+route);
   console.log('PASS certificate-verified '+route+' HTTP 200');
  }
 }else{
  await connect();console.log(crypto.createHash('sha256').update(certificate.publicKey.export({type:'spki',format:'der'})).digest('base64'));
 }
})().catch(error=>{console.error(error.message);process.exitCode=1;});
