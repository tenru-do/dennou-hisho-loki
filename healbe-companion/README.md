# HEALBE直接連携・検証版

公式HEALBE SDK 1.11.0を利用するAndroid補助アプリ。公式アプリの画面読み取りとは別経路で、GoBeへBluetooth接続して同期後に当日のEnergySummaryを取得する。未ログイン状態や古い測定値を現在値として表示しない。

## 利用

スマホ上で既存HEALBEアカウントへログインする。認証情報をチャット・PCへ送らない。必要時はAndroidの付近のデバイス許可をユーザーが付与する。取得成功後、ロキの歩数欄へ収支を併記する。公式アプリを閉じた状態で実データを取得できることは、ログイン後の確認が必要。

ログイン後はBluetooth常駐サービスで約5分周期に同期する。インターネット接続がない場合は取得を試さない。Doze等により遅れるため定刻を保証しない。常駐サービスの開始が制限された場合は15分ジョブへ代替する。GoBeは画面で選択・確認し、必要時は既存PINをスマホ内で入力する。同期ごとにGoBeへ再接続し、TasksState.SYNCからOPERATIVEへの同期全体完了を待つ。同期成功時刻と実値の変化時刻は別に記録する。

## データ保護

- パスワードを独自保存・ログ出力せず、入力画面はFLAG_SECUREと自動入力・状態保存の抑制を使用。SDKのセッション管理はHEALBE SDKが担当する。
- 収支キャッシュはAndroidKeyStore/AES-GCMで暗号化し、バックアップを無効化。
- 固定URIの読み取り専用Providerは署名権限と呼出元パッケージの両方で制限。任意のファイルやアカウント情報は公開しない。
- 停止ボタンは定期実行をキャンセルし、連携を無効化して収支キャッシュを削除する。
- observedAtはGoBe接続・同期後の取得時刻。measuredAtは詳細EnergyDataの最終時刻。日次収支の同期時刻と区別し、グラスではobservedAtを「同期N分前」と明示する。

## ビルド

Android SDK 35、JDK 21、Gradle 9.3.0で `gradle -p healbe-companion :app:assembleDebug`。ロキとのProvider連携には同一署名が必要。local.properties、ビルド出力は管理対象外。

公式資料: https://healbe.com/dev/
SDK配布: https://nexus.healbe.com/repository/maven-public/
公開サンプル: https://bitbucket.org/Healbe/healbe-public-android-sdk-demo/
