# VOICEの全文認識と確認表示

グラス544 / スマホ199。

録音は最長60秒、発話中の無音は2.2秒まで待つ。以前の16秒上限と1秒の無音終了から変更した。無発話時は6秒で次の待機へ移る。上限に達した認識表示には「録音上限60秒」を示す。マイク入力の不検出時と認識サービスの失敗を分け、ネットワーク/認識処理の失敗だけでマイク入力設定を変更しない。

スマホの分割認識は確定した各区間を順に保持する。途中結果は更新可能な未確定部分として分離する。認識の完了待ちはPCMの長さに20秒を加えた時間（最大90秒）で管理し、固定24秒で打ち切らない。音声を最後まで渡せていない・認識がタイムアウトした場合、途中の文章を全文として会話へ送らない。認識器と音声パイプは成功・失敗とも解放し、認識要求を直列化する。

STT専用のリクエスト上限を4MiBにし、音声形式と最長60秒を確認する。他の通常リクエストの上限は従来のまま。16kHz/16bit/monoの60秒PCMは1,920,000バイト。グラスの応答待ち時間も録音長に応じて調整する。

グラスとスマホの会話入力付近に「認識した内容」を表示する。全文はタップで開き、入力欄へ戻して修正できる。送信で入力欄が空になっても認識結果の確認表示は残す。認識途中は未送信として表示し、この確認用表示はアプリのメモリ内だけに保持する。会話履歴は従来通り回答成功後に保存し、安全停止の本文は保存しない。

仕様: Androidの分割結果はonSegmentResultsで区間ごとに届き、onEndOfSegmentedSessionで終了する。EXTRA_AUDIO_SOURCEを使う分割セッションは入力パイプのクローズにより終了する。
- https://developer.android.com/reference/android/speech/RecognitionListener
- https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_SEGMENTED_SESSION

検証には、1.5秒と1.8秒の間を含む約39秒の合成日本語音声を用いた。旧版は24秒で中盤だけを返し、修正版スマホでは冒頭・中盤・末尾の目印を保持した。実機の経路・表示・マイク・入力欄に関する詳細結果は作業出力outputs/voice-stt-checkを参照する。合成音声の結果を人の会話すべての認識率や公式アプリとの同等性と解釈しない。

## Continuous capture (glass v548)

Normal VOICE now uses ContinuousVoiceInput with a separate VoicePcmCapture producer and ordered phone STT consumer. Capture continues while the preceding clip is being recognized. Recognized fragments enter the existing pendingVoiceTranscripts path; ordinary conversation instructions and request construction are unchanged.

- One microphone owner through CaptureLease; nonblocking reads allow stop/cancellation without waiting for another audio buffer.
- One recognition request at a time; FIFO delivery. A failed request does not discard later buffered clips. No automatic Gemini-audio retry on this path.
- 16kHz/16-bit mono, 60 seconds maximum per clip; 2.2 seconds trailing silence. The amplified mean-level gate is45, above the measured near-silent9–42 range. Quiet/remote speech still requires actual use testing.
- At most two queued PCM clips, plus one being captured and one being decoded. PCM content is at most7.68MB across those four slots; allocation/copying, base64 and network/UI objects add memory beyond that. No PCM files are persisted.
- When the queue is full, recording pauses with an explicit status. Clips already captured are retained; input is not unlimited.
- STOP finishes capture and drains queued clips and recognized text. The button then reads「中止」; pressing again cancels pending work. Late recognition results from an invalidated session are ignored.
- New smartphone input invalidates the prior VOICE generation; stale capture is stopped, and normal continuation can create a fresh session.

Verification artifacts: outputs/voice-stt-check in the maintenance workspace. Synthetic pipeline tests validate capture during blocked recognition, FIFO/backpressure, graceful draining and stale-result suppression. Device integration and real microphone start/stop are recorded separately; do not equate injected synthetic PCM with a spoken-user accuracy test.
While a follow-up phrase is still being spoken, buffered or decoded, recognized-text dispatch waits for that audio. This retains the existing behavior of joining pending fragments instead of prematurely sending a partial next turn. The session keeps the microphone and recognition state separately; voiceRecording alone no longer describes all pending audio.
