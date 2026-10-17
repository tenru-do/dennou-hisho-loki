const fs=require('fs'),crypto=require('crypto'),tls=require('tls'),path=require('path');
const cert=new crypto.X509Certificate(fs.readFileSync(path.join(__dirname,'phone-public-certificate.local.der')));
const expected=cert.fingerprint256;
const client=tls.connect({host:'127.0.0.1',port:18766,rejectUnauthorized:false},()=>{
 try{const peer=new crypto.X509Certificate(client.getPeerCertificate().raw);if(peer.fingerprint256!==expected)throw Error('Smartphone certificate changed; verification required.');
 console.log(crypto.createHash('sha256').update(cert.publicKey.export({type:'spki',format:'der'})).digest('base64'));client.end();}
 catch(e){console.error(e.message);client.destroy();process.exitCode=1;}
});client.setTimeout(10000,()=>{console.error('USB connection timeout');client.destroy();process.exitCode=1;});client.on('error',e=>{console.error(e.message);process.exitCode=1;});
