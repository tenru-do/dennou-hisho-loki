> 履歴資料。記載は作成時点の状態です。現行仕様は v446 の資料を参照してください。

# 全身モード 状態表示と読み上げ口パク v416

右下の時計/バッテリーの上にWi-Fi/NET/WAIT秒数と既存statusを表示。Geminiリクエスト中は処理中、API402受信時は前払い残高不足。右側会話欄の下余白を確保し、顔モードへ戻すと追加欄を隠します。顔モード側レイアウトと表情口パク設定は変更なし。
全身モードはmode2で読み上げ内容に関わらず口を開閉。既存顔モードのnarration抑制animateMouth=falseが渡っても、全身の口パクは動きます。口パク未対応の表情は発話中だけ対応画像へ置換(座り姿は167、立ち姿は000)、静止時の表情選択は維持。発話中の表情の種類は、この対応範囲によって一時的に制限されます。音素同期ではありません。
検証: 43ソースビルドエラー0、署名検証成功。全168表情の発話時に対応ポーズが選ばれるテスト成功。RG_glasses CanvasでanimateMouth=false/mode2対silent/mode0の差100px、身体部分不変を確認。既存Mascot6087チェック成功。実機で右下状態表示とWAIT19sを確認、glass-status-v416.png保存。実際の状況読み上げ中の音声同期は未確認。
402: グラスのRokidKeyboardAIログがGemini HTTP402 RESOURCE_EXHAUSTED 'Your prepayment credits are depleted' を明示。Google公式billingドキュメントも残高0で402となると説明 https://ai.google.dev/gemini-api/docs/billing#prepay 。AI Studio側の残高対応が必要で、本更新では課金/残高を変更していません。API成功時に残高不足表示フラグは解除。
RG_glasses[device serial omitted]へadb install --no-incremental -r、version416 / 0.9.237-fullbody-status-speech確認。スマホアプリ更新なし。
APK SHA256: 78628F79C2F24CC9C68BD3944DED8EA3A6ED3068770C67E52B6589CD7F6BC8B2

