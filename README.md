# 電脳秘書ロキ

Rokidグラスを、Bluetoothキーボード・音声・スマートフォンから操作できるGeminiベースの実験的AI秘書にするAndroidアプリ群です。

![電脳秘書ロキ アイコン](artwork/dennou-hisho-loki-icon.png)

> [!WARNING]
> 個人開発のアルファ版です。Rokid、Google、Gemini、HEALBE、OpenAIの公式製品ではありません。端末・Rokid OS・ネットワーク構成によっては動作しません。医療・緊急・法務・金融など、誤回答が重大な損害につながる用途には使用しないでください。

## APKのダウンロード

1. [Releases](https://github.com/tenru-do/dennou-hisho-loki/releases/latest) を開きます。
2. `Assets` から次の2ファイルをダウンロードします。
   - `DennouHishoLoki-phone-v0.9.4-alpha.apk`：Androidスマホ側
   - `DennouHishoLoki-glass-v0.9.4-alpha.apk`：Rokidグラス側
3. スマホ版はスマホでAPKを開いてインストールします。
4. グラス版は開発者ケーブルとADBを使用してインストールします。

```powershell
adb devices
adb -s <グラスのシリアル番号> install -r DennouHishoLoki-glass-v0.9.4-alpha.apk
```

Androidが警告を表示した場合は、内容を確認したうえで、このAPKを開いたアプリに限って「不明なアプリのインストール」を許可してください。APKはアルファ版用のテスト署名です。導入手順の詳細は [docs/INSTALL.md](docs/INSTALL.md) を参照してください。

## 主な機能

- Bluetoothキーボード、スマホ、音声から質問
- Gemini回答のHUD表示と読み上げ
- スマホ経由のGoogleカレンダー、メール通知、Health Connect歩数
- スマホの概算現在地とOpen-Meteoによる現在・明日・明後日の天気
- 単色緑マスコット
- 頭を上げたときのHUD表示、正面復帰後の自動消灯、回答中の画面保持
- 別途インストールした自作Zoomカメラを起動する `CAM` ショートカット、Wi-Fi操作

`CAM` はカメラ機能を内蔵しているボタンではありません。パッケージ名 `com.example.rokidzoomcamera` のZoomカメラアプリがグラスにインストールされている場合、そのアプリを起動します。未導入の場合は起動しません。

## 構成

```text
glass-app/    Rokidグラス側アプリ
phone-app/    スマホ側ブリッジ
manager-app/  Rokid管理用の小型ランチャー
artwork/      アイコン原画
docs/         導入・安全性・制限事項
```

## 必要なもの

- 対応するRokidグラスとADB導入環境
- Androidスマートフォン
- 利用者自身のGemini APIキー
- グラスとスマホが相互通信できるWi-Fiまたはテザリング

Gemini APIは無料枠・課金・モデル別クォータ・混雑の影響を受けます。無料枠では `429`、混雑時には `503` が発生することがあります。課金しても無制限にはなりません。

## 最初の設定

1. Releasesからスマホ側とグラス側のAPKをダウンロードし、インストールします。
2. スマホ側でカレンダー、マイク、通知、Health Connect、概算位置情報のうち使う権限だけを許可します。
3. スマホ側の「指示」パネルで「グラスをペアリング（60秒）」を押します。
4. 60秒以内にグラス側の「SET」を押します。共有トークンは一度だけ自動転送され、手入力や画面表示は不要です。
5. グラス側のSET画面でGemini APIキーと任意のカスタム指示を保存し、同じネットワーク上で接続を確認します。

詳細は [docs/INSTALL.md](docs/INSTALL.md)、[docs/SETUP.md](docs/SETUP.md)、ソースから作る場合は [docs/BUILDING.md](docs/BUILDING.md) を参照してください。

## プライバシー

- APIキー、個人用カスタム指示、予定、メール、会話ログ、端末識別子はリポジトリに含まれていません。
- Gemini APIキーとカスタム指示はグラスのアプリ専用領域に保存されます。
- スマホ側のカスタム指示と連携トークンはスマホのアプリ専用領域に保存されます。
- メール機能はメールボックス全体ではなく、許可したAndroid通知を利用します。
- 天気機能ではスマホの概算座標をOpen-Meteoへ送り、市区町村名・天気・気温だけを認証済みローカル接続でグラスへ渡します。
- 予定・メール・音声・質問内容は、機能に応じてローカルLAN、Google/Gemini、Android音声認識へ送られる場合があります。
- LANブリッジは共有トークンで認証しますが、HTTP通信自体は暗号化されません。信頼できるLANまたは自分のテザリングだけで利用してください。
- トークンの自動転送は、スマホで明示的に開いた60秒のペアリング時間内に最初の1台だけが取得できます。公共LANではペアリングしないでください。

削除方法と権限一覧は [docs/PRIVACY.md](docs/PRIVACY.md)、脅威と対策は [docs/SECURITY.md](docs/SECURITY.md) に記載しています。

## 現在の制限

- Google Play配布や自動セットアップには未対応です。APKは手動導入が必要です。
- 公開APKはアルファ版用のテスト署名で、正式なリリース署名ではありません。
- Rokid OS固有APIや端末差に依存します。
- 音声認識、TTS、Wi-Fi復帰は端末実装によって不安定です。
- Google Workspaceの完全なOAuth/Gmail API統合ではありません。
- Geminiの回答は正確とは限らず、予定やニュースも必ず原典を確認してください。

詳細は [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md) を参照してください。

## ライセンス

[LICENSE](LICENSE) を参照してください。第三者の商標・サービス名は各権利者に帰属します。
