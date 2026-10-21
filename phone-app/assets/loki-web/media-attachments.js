/* Local visual overview only. No network, clipboard text/URL fetching, or audio upload. */
'use strict';
async function createMediaOverview(files,dimensions,cancelled,options={}){
 if(!files.length||files.length>6)throw Error('画像は6枚まで、動画は1本を選択してください');
 const videos=files.filter(f=>f.type.startsWith('video/'));
 if(videos.length&&files.length!==1)throw Error('動画は画像と混ぜず、1本ずつ選択してください');
 const videoMode=videos.length===1,count=videoMode?8:files.length,tall=!videoMode&&options.kinds?.includes('video'),cols=tall?Math.min(3,count):2,cell=videoMode?448:tall&&count<=2?896:640,panelHeight=tall?(count<=2?1984:1420):cell,label=40;
 const canvas=document.createElement('canvas');canvas.width=cell*cols;canvas.height=64+Math.ceil(count/cols)*(panelHeight+label);
 const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.fillStyle='#000';ctx.font='22px sans-serif';ctx.fillText(videoMode?'VIDEO: sampled frames only / NO AUDIO':(tall?'ATTACHMENTS: numbered / VIDEO FRAMES ONLY / NO AUDIO':'IMAGES: read in numbered order'),12,36);
 const check=()=>{if(cancelled())throw Error('添付の準備を取り消しました');};
 const draw=(image,i,title)=>{check();const x=i%cols*cell,y=64+Math.floor(i/cols)*(panelHeight+label);ctx.fillText(title,x+10,y+28);const scale=Math.min(cell/(image.videoWidth||image.width),panelHeight/(image.videoHeight||image.height)),w=(image.videoWidth||image.width)*scale,h=(image.videoHeight||image.height)*scale;ctx.drawImage(image,x+(cell-w)/2,y+label+(panelHeight-h)/2,w,h);};
 if(videoMode){
  const file=files[0];if(file.size>50000000)throw Error('動画は50MB以内にしてください');
  const video=document.createElement('video'),url=URL.createObjectURL(file);video.preload='auto';video.muted=true;video.playsInline=true;
  const wait=(event,action)=>new Promise((resolve,reject)=>{let timer;const clean=()=>{clearTimeout(timer);video.removeEventListener(event,ok);video.removeEventListener('error',fail);};const ok=()=>{clean();resolve();},fail=()=>{clean();reject(Error('動画を読み込めません。MP4などブラウザで再生できる形式にしてください'));};video.addEventListener(event,ok,{once:true});video.addEventListener('error',fail,{once:true});timer=setTimeout(()=>{clean();reject(Error('動画の読み込みがタイムアウトしました'));},15000);try{action();}catch(e){clean();reject(e);}});
  try{
   await wait('loadeddata',()=>{video.src=url;video.load();});check();
   const duration=video.duration;if(!Number.isFinite(duration)||duration<=0||duration>120)throw Error('動画は2分以内にしてください');
   if(video.videoWidth*video.videoHeight>80000000)throw Error('動画の解像度が大きすぎます');
   for(let i=0;i<8;i++){check();const sec=Math.max(0,(duration-.05)*i/7);if(Math.abs(video.currentTime-sec)>.001)await wait('seeked',()=>{video.currentTime=sec;});draw(video,i,(i+1)+' / '+sec.toFixed(2)+'s');}
  }finally{video.pause();video.removeAttribute('src');video.load();URL.revokeObjectURL(url);}
 }else{
  for(const [i,file] of files.entries()){
   check();if(file.size>20000000||!file.type.startsWith('image/'))throw Error('画像は1枚20MB以内にしてください');
   const size=await dimensions(file),ratio=Math.min(1,1280/Math.max(size.width,size.height));check();
   const bitmap=await createImageBitmap(file,{resizeWidth:Math.max(1,Math.round(size.width*ratio)),resizeHeight:Math.max(1,Math.round(size.height*ratio)),resizeQuality:'high'});
   try{draw(bitmap,i,'IMAGE '+(i+1)+(options.kinds?.[i]==='video'?' / VIDEO: 8 FRAMES, NO AUDIO':''));}finally{bitmap.close();}
  }
 }
 check();for(let q=.9;q>=.3;q-=.1){const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/jpeg',q));if(blob&&blob.size<=600000)return blob;}
 throw Error('添付が細かすぎます。枚数や範囲を減らしてください');
}

// Local advisory only: a flat drawing can produce the same result as a blurred photo.
async function photoQualityHint(file){
 const bitmap=await createImageBitmap(file,{resizeWidth:96,resizeHeight:96,resizeQuality:'high'});
 try{const canvas=document.createElement('canvas');canvas.width=canvas.height=96;const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.drawImage(bitmap,0,0);const px=ctx.getImageData(0,0,96,96).data;const l=i=>.299*px[i*4]+.587*px[i*4+1]+.114*px[i*4+2];let brightness=0,edge=0,n=0;for(let y=1;y<95;y++)for(let x=1;x<95;x++){const i=y*96+x,v=l(i),lap=4*v-l(i-1)-l(i+1)-l(i-96)-l(i+96);brightness+=v;edge+=lap*lap;n++;}return (brightness/n<45?'暗い画像です。明るい場所での再撮影を検討してください。':'')+(edge/n<55?'細部が少ない画像です。ぼけ・ピントを確認してください（無地の画像でも表示します）。':'');}finally{bitmap.close();}
}
