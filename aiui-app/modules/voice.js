export class Voice {
 constructor({Recognition,Utterance,synthesis,Player,onDraft=()=>{},onStatus=()=>{},setTimer=setTimeout,clearTimer=clearTimeout}){Object.assign(this,{Recognition,Utterance,synthesis,Player,onDraft,onStatus,setTimer,clearTimer});this.recognition=null;this.player=null;this.task=null;this.epoch=0;this.recordEpoch=0;this.recordTimer=null;}
 start(){
  if(this.recognition)return false;
  if(typeof this.Recognition!=='function'){this.onStatus('この環境では音声入力を利用できません。文字入力を使ってください。');return false;}
  this.stopPlayback();const epoch=++this.recordEpoch;let recognition;
  try{
   recognition=new this.Recognition();this.recognition=recognition;recognition.lang='ja-JP';recognition.continuous=false;recognition.interimResults=false;
   recognition.onresult=event=>{if(this.recognition!==recognition||epoch!==this.recordEpoch)return;const index=Number.isInteger(event.resultIndex)?event.resultIndex:0;const text=event.results?.[index]?.[0]?.transcript;if(typeof text==='string'&&text.trim()){this.onDraft(text.slice(0,2400));this.onStatus(text.length>2400?'音声入力は2400文字まで入力欄へ戻しました。内容を確認してください。':'音声入力を入力欄へ戻しました。確認して送信してください。');}else this.onStatus('音声を読み取れませんでした。');};
   recognition.onerror=()=>{if(this.recognition===recognition)this.onStatus('音声入力を完了できませんでした。権限や接続を確認してください。');};
   recognition.onend=()=>{if(this.recognition===recognition){this.recognition=null;this.clearTimer(this.recordTimer);this.recordTimer=null;this.onStatus('');}};
   this.onStatus('音声入力中。停止してから内容を確認できます。');recognition.start();
   this.recordTimer=this.setTimer(()=>{if(this.recognition===recognition)this.stopRecording(true);},60000);return true;
  }catch{this.recognition=null;try{recognition?.abort();}catch{}this.onStatus('音声入力を開始できません。権限やAIUIの対応状況を確認してください。');return false;}
 }
 stopRecording(abort=false){const recognition=this.recognition;if(!recognition)return;this.clearTimer(this.recordTimer);this.recordTimer=null;if(abort){this.recordEpoch++;this.recognition=null;}try{if(abort)recognition.abort();else recognition.stop();}catch{this.recognition=null;}if(abort)this.onStatus('');else if(this.recognition===recognition)this.recordTimer=this.setTimer(()=>{if(this.recognition===recognition){this.stopRecording(true);this.onStatus('音声入力の終了を確認できないため停止しました。');}},5000);}
 stopPlayback(){this.epoch++;const task=this.task,player=this.player;this.task=null;this.player=null;try{task?.abort();}catch{}try{player?.stop();}catch{}try{player?.destroy();}catch{}}
 async play(text){
  this.stopPlayback();this.stopRecording(true);const epoch=this.epoch;
  if(!this.synthesis?.synthesize||typeof this.Utterance!=='function'||typeof this.Player!=='function'){this.onStatus('この環境では停止可能な読み上げを利用できません。');return false;}
  try{
   this.onStatus('読み上げを準備中です。');const task=await this.synthesis.synthesize(new this.Utterance(text.slice(0,1200)),{subtitles:'none'});
   if(epoch!==this.epoch){try{task.abort();}catch{}return false;}
   this.task=task;task.finished.catch(()=>{if(epoch===this.epoch){this.stopPlayback();this.onStatus('音声の生成を完了できませんでした。');}});
   this.player=new this.Player(task);this.player.play();this.onStatus('読み上げを開始しました（最大1200文字）。日本語の音声品質はAIUI側の対応に依存します。');return true;
  }catch{if(epoch===this.epoch){this.stopPlayback();this.onStatus('読み上げを開始できませんでした。AIUIの音声設定を確認してください。');}return false;}
 }
 dispose(){this.stopRecording(true);this.stopPlayback();}
}
