# APKの導入

電脳秘書ロキは、Androidスマホ側のブリッジとRokidグラス側のHUDアプリを組み合わせて使用します。両方のAPKが必要です。

## 1. ダウンロード

[GitHub Releases](https://github.com/tenru-do/dennou-hisho-loki/releases/latest) を開き、`Assets` から次の2ファイルをダウンロードします。

- `DennouHishoLoki-phone-v0.9.4-alpha.apk`
- `DennouHishoLoki-glass-v0.9.4-alpha.apk`

リポジトリ画面の `Code` → `Download ZIP` はソースコードの取得です。アプリを使うだけの場合は、ReleasesのAPKを選んでください。

## 2. スマホ版

1. スマホで `DennouHishoLoki-phone-v0.9.4-alpha.apk` を開きます。
2. Androidが確認を求めた場合、このAPKを開いたファイル管理アプリまたはブラウザに限って「不明なアプリのインストール」を許可します。
3. インストール後にアプリを起動します。
4. 使用する機能に応じて、カレンダー、マイク、通知アクセス、Health Connect、概算位置情報を許可します。

使わない機能の権限を許可する必要はありません。

## 3. グラス版

Rokidグラスを開発者ケーブルでPCへ接続し、USBデバッグを許可します。Android Platform Toolsの `adb` が使える端末で次を実行します。

```powershell
adb devices
adb -s <グラスのシリアル番号> install -r DennouHishoLoki-glass-v0.9.4-alpha.apk
```

`Success` と表示されたら、グラスのアプリ一覧から「電脳秘書ロキ」を起動します。

## 4. スマホとグラスの連携

1. スマホとグラスを同じ信頼できるWi-Fiまたは自分のテザリングへ接続します。
2. スマホアプリの「指示」から「グラスをペアリング（60秒）」を押します。
3. 60秒以内にグラス側の `SET` を押します。
4. グラス側のSET画面で、自分のGemini APIキーと必要に応じてカスタム指示を保存します。

共有トークンは一度だけ自動転送されます。公共Wi-Fiではペアリングしないでください。

## CAMボタン

グラス側の `CAM` は、別途インストールした自作Zoomカメラアプリへのショートカットです。カメラ機能自体は電脳秘書ロキのAPKに含まれません。

- 対象パッケージ：`com.example.rokidzoomcamera`
- Zoomカメラがインストール済み：`CAM` で起動
- 未インストール：起動せず、エラー表示

## 注意

- 個人開発のアルファ版で、対応するRokidグラス以外での動作は保証していません。
- APKはアルファ版用のテスト署名です。
- Gemini APIキーは同梱されていません。利用者自身で用意してください。
- Gemini APIのクォータや混雑により、`429` または `503` が発生する場合があります。
- APIキー、予定、メール、会話ログなどの扱いは [PRIVACY.md](PRIVACY.md) を確認してください。
