> 履歴資料。記載は作成時点の状態です。現行仕様は v446 の資料を参照してください。

# 全身モードの瞬き・口パク v413

対象ポーズ: 000,004,014,165,166,167。未対応ポーズでは決定稿の顔を保ちます。
元画像と体は変更せず、確認した顔の座標へCanvasで目・口の小さな図形を重ねます。約5.7秒周期の瞬き、発話状態mode2でのみ口パク。既存setMascotSpeechPresentationのanimateMouthフラグも反映します。音素・音量同期ではなく発話状態による開閉です。座り姿・立ち姿のサイズと足元基準は維持しています。
アニメーション中に画像デコードし直す必要はなく、描画用Paint/Path/RectF/口開度配列を再利用します。非表示・Activity pause時はタイマーを停止します。顔モードの描画変更はありません。
検証: 40ソースのビルドエラー0、署名検証成功。RG_glassesで264件のCanvasテスト成功(6ポーズ×4状態、身体の画素が不変、無言の口閉じ、瞬き範囲)。既存Mascotテスト6087件成功。6ポーズの描画をface-review.pngで確認しました。
更新: RG_glasses[device serial omitted]へadb install --no-incremental -rで更新、version413 / 0.9.234-fullbody-face-motion確認済み。スマホアプリのインストール変更なし(スマホ側も事前のoffscreen描画テストを実行)。
preview-*.gifはグラスCanvasテスト画像をつないだプレビューです。実会話の音声同期の実証ではありません。glass-blink-v413.mp4はグラスアプリ待機画面の録画、装着時の光学キャプチャではありません。
APK SHA256: B7FAABD82222AF30E7DF04F22B9B2555E035A90D0753559BFB0E3D3EF55A74F3

