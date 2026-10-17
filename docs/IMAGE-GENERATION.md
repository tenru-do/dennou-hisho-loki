# ロキの画像生成・編集

グラス v535／スマホ v191で追加。通常会話・画像解析・TOPICとは別の非同期処理としてスマホで実行する。

## 使い方

- グラス／会話欄：「猫の絵を描いて」「猫の画像を生成して」。
- 直前の完成画像を修正：「背景を青く」「もう少しリアルに」。共有会話の直近30分の完成画像が対象。
- グラス：「目の前のものを撮ってイラストにして」で明示的に撮影して編集できる。撮影画像をGeminiへ送る。
- スマホ：会話のそばの「画像生成・生成画像一覧」。新規生成、プレビュー、続けて編集、ギャラリー保存。添付画像を使う場合は会話欄で画像を添付して「この写真をイラストにして」などと送る。
- PC／タブレット：会話欄の「画像を生成・編集する」を選んで送信。自然な生成指示でも認識する。「画像生成・生成画像一覧」で進捗・原寸保存・この画像を編集を操作する。
- 明示的に編集元を選んだ指示は、その画像を使う。受理後は選択を解除し、次の通常の追加修正で古い画像を固定し続けない。
- グラスの完成プレビューは縮小表示。原寸はスマホとWebで確認する。「生成画像を見せて」で直近の完成画像を再表示できる。

## 保存・失敗時

原寸と指示は `files/generated-images/` に端末固有AES-GCMで保存。原寸を `Pictures/Loki/Generated/` に保存し、SHA-256で読み戻しを検証してから公開する。生成画像一覧の削除はロキ内の画像・指示だけを削除し、ギャラリーのコピーは残す。スマホの画像は長押しで削除を選択できる。

生成は一度に1件。原寸8MB・1600万画素まで、アプリ内100件／約256MBの上限。上限に達したら一覧から整理する。API失敗・利用制限・権限／課金設定の問題を一覧に表示する。

同じWeb/APIの送信IDは同じジョブを返し、再送で生成を繰り返さない。タイムアウトやアプリ終了後に自動再生成しない。保存途中のMediaStore行は再利用し、受領記録欠落でも重複コピーを増やさない。ユーザーがギャラリーから明示削除したコピーは自動で戻さない。

## API・認証

スマホの既存Gemini APIキーを使用。生成APIへ送るのは生成指示と選択した編集元画像であり、会話の記憶・位置情報・連絡先を付加しない。画像モデルは `gemini-3.1-flash-image`。404のときだけ `gemini-2.5-flash-image` を試す。429／403／通信失敗の自動連続再試行はしない。API料金と利用制限はGoogle側の設定に従う。

- ペアリング済みグラス：`POST /image_generate`、`GET /image_generation?id=...`。既存のブリッジ認証が必要。
- Web：`/web/api/image-generate`、`image-generations`、`generated-image`、`generated-delete`。既存Webセッション認証・同一オリジン制限を使用。WebトークンでブリッジAPIは呼べない。
- `__LOKI_GENERATED__:` は完了通知であり、生成を再実行する指示ではない。

[Google公式・画像生成API](https://ai.google.dev/gemini-api/docs/generate-content/image-generation)

実機検証の私用記録は作業出力の `outputs/image-generation/`。実際の生成・選択画像の編集・グラス依頼・プレビュー・暗号化とギャラリー一致・キーボード・保存復旧・認証拒否を確認。検証用画像は合成／生成画像で、実際のカメラ周囲の画像はテスト送信していない。

## v192 character context
Explicit requests for this app's Loki character include a standard adult secretary appearance description. No conversation history or unrelated reference image is attached. Mythology and unrelated prompts stay unchanged. Safety blocks are never automatically retried. This is textual appearance context, not a guaranteed match to existing artwork.


## Registered identity (v193)
Self-portrait and own-appearance requests use the actual approved full-body ID 4 and face ID 0 crops, with the shared self profile and registered image inventory. See LOKI-SELF-PROFILE.md. These are app-bundled character images, not user photographs. Unrelated image requests do not include them.
