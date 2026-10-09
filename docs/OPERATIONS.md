# 運用・更新手順（v446）

## ビルド

リポジトリのルートで実行します。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-integration.ps1 -App glass
# スマホは必要な場合だけ別途ビルド
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-integration.ps1 -App phone
```

生成APKは `.build-integration` 以下です。署名鍵・APIキーをGitへ登録しないでください。ビルド環境と既存署名を確認してから配布します。

## 実機更新

1. `adb devices -l` で対象を確認し、以降は `adb -s <serial>` で固定する。
2. アプリの表示名、パッケージ、搭載版、署名を確認する。グラスは `com.example.rokidkeyboardbridge`。
3. 更新前APK、アプリデータ、保存写真をバックアップし、コピーのハッシュを検証する。データ取得には端末・アプリの権限が必要。
4. 新APKのパッケージ・版・署名を確認し、既存と互換であることを確認する。
5. `adb -s <serial> install -r <apk>` でデータを保持して更新する。署名不一致をアンインストールで回避しない。
6. 起動、設定保持、搭載版、インストールAPKのハッシュを確認する。検証用アプリは終了・削除する。
7. 実装テストと、装着した視覚・音声・動作の評価を別々に記録する。

## 画面と撮影

SETで顔／全身を選択。PLAYは音楽制御、SET内CTRLは本体設定と連携する明るさ調整。TOPIC内AMBは音声・カメラ・両方を選択する。

SET→RECから連続撮影を開始（5秒／10秒）・停止する。KEEPは過去2分・未来1分を保護する。CAM長押しは使わない。保存上限は実JPEG合計5GB、未保護の古い記録から削除する。保護済みで容量を使い切る場合は撮影を停止する。撮影間隔はカメラの処理状況によって変動する。

## 反応・画像の強化

`tools/FULLBODY_SCENES.md` と履歴資料 `fullbody-rules-v433.md` を参照。規則は `tools/fullbody-scene-rules.json`、生成・検証スクリプトは `tools/generate-fullbody-scenes.ps1` と `tools/test-fullbody-scenes.ps1`。通常ビルドでも規則を検証する。

全身・顔の監査JSONLは本文ではなくハッシュと反応情報を記録する。未知の反応を確認し、匿名化した回帰例を追加する。画像と感情、立位／座位、縮尺を照合し、ラベルの変更だけで不一致を隠さない。瞬き・口・移行フレームは装着評価も行う。
