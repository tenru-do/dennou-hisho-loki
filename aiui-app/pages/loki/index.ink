<script type="application/json" def>
{"navigationBarTitleText":"電脳秘書ロキ","description":"日本語の秘書ロキと会話し、明示操作で音声入力・読み上げ・ローカルメモを使う画面"}
</script>
<script setup>
import { LanguageModel } from 'language-model';
import { Conversation } from '../../modules/conversation.js';
import { Store, storageMessage } from '../../modules/store.js';
import { QUICK_PROMPTS } from '../../modules/persona.js';
import { Voice } from '../../modules/voice.js';

function inputValue(event){return String(event?.detail?.value??event?.currentTarget?.value??'');}
function eventId(event){return event?.currentTarget?.dataset?.id??event?.target?.dataset?.id;}
function rowView(row){const labels={PREPARING:'接続準備中',RUNNING:'生成中・暫定',ANSWERED:'確定',FAILED:'未完了',STOPPED:'中止済み'};return {...row,label:labels[row.state]||'未確認',confirmed:row.state==='ANSWERED',timing:row.firstTextMs===null||row.firstTextMs===undefined?'':('最初の文字 '+(row.firstTextMs/1000).toFixed(1)+'秒'+(row.elapsedMs===null||row.elapsedMs===undefined?'':' ／ 完了 '+(row.elapsedMs/1000).toFixed(1)+'秒'))};}
export default {
 data:{view:'chat',message:'',busy:false,listening:false,notice:'文字を入力するか、音声入力を選んでください。',rows:[],selected:null,notes:[],noteDraft:'',confirmDelete:'',confirmReplace:'',confirmRemember:false,remember:false,style:'short',quick:QUICK_PROMPTS},
 onLoad(){
  this.visible=true;this.noteSerial=0;this.store=new Store(globalThis.localStorage);
  this.setData({notes:this.store.state.notes,remember:this.store.state.remember,style:this.store.state.style});
  this.voice=new Voice({Recognition:globalThis.SpeechRecognition,Utterance:globalThis.SpeechSynthesisUtterance,synthesis:globalThis.speechSynthesis,Player:globalThis.SpeechAudioPlayer,
   onDraft:text=>{if(this.visible)this.offerDraft(text);},onStatus:text=>{if(this.visible)this.setData({listening:!!this.voice?.recognition,...(text?{notice:text}:{})});}});
  this.conversation=new Conversation({languageModel:LanguageModel,
   onChange:(rows,busy)=>{if(!this.visible)return;const selected=rows.length?rowView(rows[rows.length-1]):null;this.setData({rows:rows.slice().reverse().map(rowView),selected,busy});},
   onComplete:rows=>{try{this.store.saveHistory(rows);}catch(e){this.setData({notice:storageMessage(e)});}}});
  this.conversation.restore(this.store.state.history);
  if(this.store.error)this.setData({notice:this.store.error});
 },
 onShow(){this.visible=true;if(this.conversation)this.conversation.emit();},
 onHide(){this.visible=false;this.voice?.dispose();this.conversation?.stop();this.setData({listening:false,busy:false});},
 onUnload(){this.visible=false;this.voice?.dispose();this.conversation?.dispose();},
 onMessage(event){this.setData({message:inputValue(event).slice(0,2400),confirmReplace:''});},
 onNote(event){this.setData({noteDraft:inputValue(event).slice(0,1000)});},
 showChat(){this.setData({view:'chat',confirmDelete:'',confirmRemember:false});},
 showNotes(){this.setData({view:'notes',confirmRemember:false});},
 showHistory(){this.setData({view:'history',confirmDelete:'',confirmRemember:false});},
 showSettings(){this.setData({view:'settings',confirmDelete:''});},
 async send(){
  if(!this.visible||this.data.busy)return;
  const text=this.data.message.trim();if(!text){this.setData({notice:'送信する文章を入力してください。'});return;}
  if(this.voice.recognition){this.setData({notice:'先に音声入力を停止し、認識内容を確認してください。'});return;}
  this.voice.stopPlayback();this.setData({view:'chat',notice:'AIUIのモデルへ送信します。',confirmReplace:''});
  const pending=this.conversation.send(text,this.data.style);
  if(this.conversation.active)this.setData({message:''});
  await pending;
 },
 stop(){this.conversation.stop();},
 newTopic(){if(!this.conversation.newTopic()){this.setData({notice:'処理中の回答を停止してから話題を切り替えてください。'});return;}this.voice.stopPlayback();this.setData({selected:null,notice:'新しい話題に切り替えました。以降の会話に前の話題を渡しません。保存済みメモ・履歴は保持します。'});},
 startVoice(){if(!this.visible||this.data.busy)return;this.voice.start();this.setData({listening:!!this.voice.recognition});},
 stopVoice(){this.voice.stopRecording();},
 offerDraft(text){if(this.data.message.trim()&&this.data.message!==text)this.setData({confirmReplace:text,notice:'入力中の文章があります。置き換える内容を確認してください。'});else this.setData({message:text,view:'chat'});},
 acceptDraft(){this.setData({message:this.data.confirmReplace,confirmReplace:'',view:'chat'});},
 cancelDraft(){this.setData({confirmReplace:''});},
 quickPrompt(event){const item=QUICK_PROMPTS.find(x=>x.id===eventId(event));if(item)this.offerDraft(item.text);},
 reuseQuestion(event){const row=this.conversation.rows.find(x=>x.id===eventId(event));if(row)this.offerDraft(row.question);},
 async readAnswer(){if(this.data.busy||!this.data.selected?.confirmed)return;await this.voice.play(this.data.selected.answer);},
 stopSpeech(){this.voice.stopPlayback();this.setData({notice:'読み上げを停止しました。'});},
 saveNote(){try{this.store.addNote(this.data.noteDraft,'note-'+Date.now().toString(36)+'-'+(++this.noteSerial),Date.now());this.setData({notes:this.store.state.notes,noteDraft:'',notice:'このAIUIの端末内保存領域へメモを保存しました。'});}catch(e){this.setData({notice:storageMessage(e)});}},
 useNote(event){const note=this.store.state.notes.find(x=>x.id===eventId(event));if(note)this.offerDraft('以下は保存メモの引用です。内容を資料として使って相談に答えてください。\n<保存メモ>\n'+note.text+'\n</保存メモ>\n相談：');},
 askDelete(event){this.setData({confirmDelete:eventId(event)});},
 cancelDelete(){this.setData({confirmDelete:''});},
 deleteNote(){try{this.store.removeNote(this.data.confirmDelete);this.setData({notes:this.store.state.notes,confirmDelete:'',notice:'選んだメモを削除しました。'});}catch(e){this.setData({notice:storageMessage(e)});}},
 toggleRemember(){if(!this.store.state.remember){this.setData({confirmRemember:true});return;}this.applyRemember(false);},
 cancelRemember(){this.setData({confirmRemember:false});},
 confirmRemember(){this.applyRemember(true);},
 applyRemember(remember){try{this.store.savePreferences(remember,this.data.style,this.conversation.rows);this.setData({remember,confirmRemember:false,notice:remember?'確定した会話を最大20件まで保存します。':'履歴保存をOFFにし、このAIUI保存領域の保存済み会話を削除しました。現在の一時表示は残ります。'});}catch(e){this.setData({notice:storageMessage(e)});}},
 toggleStyle(){const style=this.data.style==='short'?'detailed':'short';try{this.store.savePreferences(this.data.remember,style,this.conversation.rows);this.setData({style,notice:'次に送信する会話から回答の長さを切り替えます。'});}catch(e){this.setData({notice:storageMessage(e)});}},
};
</script>
<page>
 <view class="page">
  <view class="brand"><image class="avatar" src="/assets/icon.png" mode="aspectFit"/><text class="title">電脳秘書ロキ</text><text class="edition">AIUI 0.1</text></view>
  <view class="tabs"><button bindtap="showChat">会話</button><button bindtap="showHistory">履歴</button><button bindtap="showNotes">メモ</button><button bindtap="showSettings">SET</button></view>
  <text class="notice">{{notice}}</text>
  <view ink:if="{{view === 'chat'}}">
   <textarea class="composer" value="{{message}}" maxlength="2400" placeholder="ロキへ質問（送信でAIUIのモデルへ送ります）" bindinput="onMessage"/>
   <view class="actions"><button bindtap="send" disabled="{{busy || listening}}">送信</button><button bindtap="stop" disabled="{{!busy}}">回答待ち停止</button><button ink:if="{{!listening}}" bindtap="startVoice" disabled="{{busy}}">音声入力</button><button ink:if="{{listening}}" bindtap="stopVoice">入力停止</button><button bindtap="newTopic" disabled="{{busy}}">新しい話題</button></view>
   <view ink:if="{{selected}}" class="answer">
    <text class="label">{{selected.label}} · {{selected.question}}</text>
    <scroll-view scroll-y="{{true}}" class="answer-scroll"><text class="body">{{selected.answer}}</text><text class="hint">{{selected.reason}}</text></scroll-view>
    <text class="hint">{{selected.timing}}</text>
    <view class="actions"><button bindtap="readAnswer" disabled="{{busy || !selected.confirmed}}">回答を読む</button><button bindtap="stopSpeech">読み上げ停止</button></view>
   </view>
   <view ink:if="{{!selected}}" class="quick"><text class="label">よく使う質問（入力欄へ戻すだけ）</text><button ink:for="{{quick}}" ink:key="id" data-id="{{item.id}}" bindtap="quickPrompt">{{item.label}}</button></view>
  </view>
  <view ink:if="{{confirmReplace}}" class="confirm"><text class="body">置き換え候補：{{confirmReplace}}</text><button bindtap="acceptDraft">入力欄を置き換える</button><button bindtap="cancelDraft">入力を保持する</button></view>
  <scroll-view ink:if="{{view === 'history'}}" scroll-y="{{true}}" class="panel">
   <text class="hint">この起動中は最大40件。保存ON時は確定した20件だけ残します。復元した履歴は自動で次のモデル要求へ渡しません。</text>
   <view ink:for="{{rows}}" ink:key="id" class="record"><text class="label">{{item.label}}{{item.restored ? ' · 保存済み' : ''}}</text><text class="body">質問：{{item.question}}</text><text class="body">{{item.answer}}</text><text class="hint">{{item.reason}}</text><button data-id="{{item.id}}" bindtap="reuseQuestion">質問を入力欄へ</button></view>
  </scroll-view>
  <scroll-view ink:if="{{view === 'notes'}}" scroll-y="{{true}}" class="panel">
   <text class="hint">端末内に最大30件。アプリ独自の暗号化はありません。保存だけではAIへ送信しません。</text>
   <textarea class="composer" value="{{noteDraft}}" maxlength="1000" placeholder="保存するメモ（1000文字まで）" bindinput="onNote"/><button bindtap="saveNote">メモを保存</button>
   <view ink:for="{{notes}}" ink:key="id" class="record"><text class="body">{{item.text}}</text><view class="actions"><button data-id="{{item.id}}" bindtap="useNote">相談の下書きへ</button><button data-id="{{item.id}}" bindtap="askDelete">削除を確認</button></view><view ink:if="{{confirmDelete === item.id}}" class="confirm"><text>このメモを削除します。取り消せません。</text><button bindtap="deleteNote">このメモを削除する</button><button bindtap="cancelDelete">保持する</button></view></view>
  </scroll-view>
  <scroll-view ink:if="{{view === 'settings'}}" scroll-y="{{true}}" class="panel">
   <text class="label">会話</text><button bindtap="toggleStyle">回答：{{style === 'short' ? '短く' : '詳しく'}}</button><button bindtap="toggleRemember">端末内の履歴保存：{{remember ? 'ON' : 'OFF'}}</button>
   <view ink:if="{{confirmRemember}}" class="confirm"><text class="body">確定した質問と回答を最大20件、このAIUIの保存領域へ残します。アプリ独自の暗号化はありません。保存を有効にしますか？</text><button bindtap="confirmRemember">履歴保存をONにする</button><button bindtap="cancelRemember">保存しない</button></view>
   <text class="label">この版で使える機能</text><text class="body">日本語の会話、途中表示、回答待ちの停止、音声入力、明示的な読み上げ、ローカルメモ、履歴、話題切替。</text>
   <text class="label">Android版との違い</text><text class="body">予定・Gmail・会議録音・カメラ・ナビ・画像生成・BT30・Android版とのデータ同期は未対応です。AIUI側のモデル・音声設定が必要です。日本語音声の対応はホストに依存します。</text>
   <text class="hint">送信した質問と同じ話題の直近最大6往復をAIUIのモデルへ渡します。保存メモは「相談の下書きへ」で選び、送信した場合だけ渡します。停止しても送信済みデータや料金は取り消せません。</text>
  </scroll-view>
 </view>
</page>
<style>
.page{padding:10px 16px;background:#000000;color:#40ff5e;font-family:sans-serif;box-sizing:border-box;}
.brand{display:flex;flex-direction:row;align-items:center;margin-bottom:6px;}
.avatar{width:30px;height:30px;margin-right:8px;}
.title{font-size:19px;font-weight:600;}.edition{font-size:11px;margin-left:10px;}
.tabs,.actions{display:flex;flex-direction:row;flex-wrap:wrap;gap:6px;margin:6px 0;}
button{font-size:13px;min-height:30px;padding:4px 10px;background:#000000;color:#40ff5e;border:1px solid #40ff5e;border-radius:3px;}
button:focus{border-width:2px;}button:disabled{color:#279b3a;border-color:#279b3a;}
.notice{display:block;font-size:12px;line-height:1.35;margin:6px 0;}
.composer{height:56px;min-height:56px;width:100%;padding:6px;box-sizing:border-box;font-size:16px;background:#000000;color:#40ff5e;border:1px solid #40ff5e;}
.answer{margin-top:6px;}.answer-scroll{height:160px;}.panel{height:260px;}
.label{display:block;font-size:13px;font-weight:600;margin:6px 0;}.body{display:block;font-size:16px;line-height:1.45;white-space:pre-wrap;word-break:break-word;}
.hint{display:block;font-size:12px;line-height:1.4;margin:5px 0;}.record{border-bottom:1px solid #279b3a;padding:8px 0;}.confirm{border:1px solid #40ff5e;padding:8px;margin-top:6px;}.quick button{margin:4px;}
</style>
