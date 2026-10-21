# Android版からの対応表

| 機能 | AIUI 0.1.0 |
| --- | --- |
| ロキの日本語人格・アイコン | 引継ぎ済み |
| テキスト会話・途中表示・依頼IDと回答・中止 | 移植済み。AIUIホストのモデルを使用 |
| 短答／詳説・話題切替 | 移植済み |
| 音声入力・読み上げ | 公式APIで実装。ホストの日本語対応と実機確認が必要 |
| メモ・履歴 | AIUI側の独立保存。独自暗号化なし、履歴保存は初期OFF |
| タブレットWeb・スマホとの同期 | 未対応。既存のTLS固定を迂回して接続しない |
| Googleカレンダー・Gmail・通知・歩数 | 未対応。認証・ホスト権限・実行確認経路の設計が必要 |
| 会議録音・写真・画像比較・画像動画生成 | 未対応。Androidバックグラウンドサービスは移植されない |
| TOPIC・ナビ・BT30 | 未対応 |
| 全身表示・表情・アニメーション | 大容量資産を含めず、既存アイコンで表示 |

Android版のアプリ、データ、プロンプト保護区間は変更していません。AIUI版に秘密鍵・APIキー・実会話・写真は含みません。Android版のバックアップを取り込む機能はありません。

今回のAIXはAIUIプロジェクトとしての移植第一版です。全機能移植済み、公開審査通過済み、AIUI実機動作確認済みとは扱わないでください。

## 公式参照
- https://js.rokid.com/AIUI/guide/quickstart/quickstart?lang=en-US&version=latest
- https://github.com/yodaos-project/AIUI/blob/main/skills/aiui-dev/references/apis/ai.md
- https://github.com/yodaos-project/AIUI/blob/main/documentation/3-api/storage-api.en-US.md
- https://github.com/yodaos-project/AIUI/blob/main/documentation/1-framework/open-agent-format/agents.en-US.md

独自APIを推測せず、LanguageModelTextStream.read/cancel、SpeechRecognition、speechSynthesis.synthesizeと独立したSpeechAudioPlayerを使用します。LanguageModelTextStreamはWHATWG ReadableStreamではなく、空のread結果でも完了とは扱いません。
