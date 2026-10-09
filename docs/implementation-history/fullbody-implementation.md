> 履歴資料。記載は作成時点の状態です。現行仕様は v446 の資料を参照してください。

# 全身モード追加 実装・検証記録

2026-10-08。既存の顔モードを初期値として保持し、SETの表示設定から全身／顔を選択する追加実装。

## 実装

- 全身表示は左半分、会話は右半分、操作列は上部、時刻・残量は右下。時刻・残量は既存更新処理を利用。
- 表情ID 0〜167を決定稿28シートへ対応付け。専用FullBodyMascotViewで現在の人物領域だけをバックグラウンド読込み。重複する読込みは1件に制限し、切替中に新しい表情が届いたら最新へ追従。
- 採用済み白黒原稿を利用。グラスでは点灯部分が緑の単色表示となる。
- アニメーションの細分化は実装しない。今回は会話の既存表情IDに応じた静止ポーズ切替。
- 非表示の顔Viewは既存の可視性ライフサイクルによって描画更新を停止する。既存の顔画像キャッシュは再開互換性のため保持。顔資産の全面解放は今回追加していない。
- 全身Viewを隠すと表示Bitmapへの参照を解放。古いBitmapは描画スレッドの参照を考慮してGCに任せる。
- ナビ表示中は全身レイアウトを一時停止し、既存のナビ・表示範囲処理を優先。全身設定自体は保持。
- 顔モードへ戻す際にレイアウトと余白を復元。ナビの現在の表示状態は過去のスナップショットで上書きしない。
- 既存の作業ツリーにあった未完成の全身モード下地を利用し、画像不在の問題とナビ非表示処理を修正。既存の音声・記憶・TOPIC・スマホ側ソースは今回変更していない。

## 保存物

APK：loki-glass-v399-fullbody.apk。versionCode 399、versionName 0.9.220-fullbody。

SHA-256：0B3E5BF00DE9748F9CFCD9C81ABB75096342045402275129AEAD6120FECB8D98

MainActivity-addition.patchはこの作業開始時の未コミット状態からの差分。MainActivity.javaとAndroidManifest.xmlは実装後のコピー。FullBodyMascotView.java、FullBodyAssetTest.javaを併記。画像原本と一覧は隣のfullbody-approved-2026-10-08/に保持。

## 検証結果

- Androidソース37ファイルのコンパイル成功、errors=0。最終APKの署名検証成功。保存先コピーのSHA-256一致確認。
- MascotAnimationControllerTest：248項目 PASS。
- MascotSecondaryMotionTest：4441項目 PASS。
- MascotTurnPolicyTest：1398項目 PASS。
- PNG28シートと168領域の対応数・ヘッダ寸法を確認。寸法は1024×1536と1536×1024。
- Androidでの全168領域decode試験コードを用意し、接続スマホで未インストールAPKへのapp_process試験を試行したが、プロセスがKilledとなった。従ってAndroid実行下の全領域decode確認は未完了。

## 搭載版と未完了事項

開始時のグラス搭載版はversionCode398、0.9.219-amb-cacheとADBで確認。物理解像度480×640。途中でグラスが接続一覧から外れたため、399のインストールとグラス実機での検証は未実施。スマホのアプリも更新していない。

実機確認が必要：全身/顔の反復切替と設定保持、ナビ開始/終了、会話やTOPICとの共存、表情・靴の欠け、時刻/残量の更新、使用メモリと読込み遅延。既存機能が全て壊れていないという実機保証はまだできない。

人物領域は確認用シートの3列×2行から下端10%のID文字帯を除いて描画する暫定的な領域定義。顔/体型/足元位置には生成上の差があり、最終的な個別画像切り出し・位置合わせは後工程。

導入時は接続グラスから搭載APKをバックアップし、署名互換を確認した上でadb install -rを使う。既存のアプリデータは維持し、スマホ側は更新しない。

## 端末適用追記

グラス再接続後、搭載398版をinstalled-before-update.apkへバックアップ。候補APKと署名証明書SHA-256の一致を確認し、adb install -rで更新成功。搭載versionCode399、versionName0.9.220-fullbody、起動後PID9112を確認。確認時のクラッシュログは空。スマホ側アプリは更新していない。

グラスでも全領域decode用app_processはKilledとなり、網羅的なAndroid読込みテストは未完了。起動時のTOTAL PSSは277822KiB（約271MiB）だが、モード別の比較測定ではない。顔/全身切替、ナビ共存、装着中の表示品質・足元の欠けについての実機確認は引き続き必要。

## SET経路・全身表示の修正（405版）

SETがトークン未設定時に設定一覧へ入れない分岐を修正し、SETは常に一覧を開く。ペアリングは一覧内の項目として維持。実機のUI階層から全身モード・顔モード・ペアリングの存在を確認。一覧はスクロールで下方項目へ移動する。

実機でサブディレクトリ内の全身画像参照が失敗したため、全身画像をassets直下のloki_fullbody_000_005.png等へ配置し、APK ZipFileから読み込む方式で表示を確認。PNGシートを一時展開して人物領域を切り出し、シートはrecycleする。保持する表示Bitmapは1枚、切替時の一時シート展開は約6MiB。番号文字のない12〜17・144〜149はセル全高を利用して靴の欠けを修正。

405版（0.9.226-fullbody-frame）を署名検証後adb install --no-incremental -rで適用。全168領域のAndroid decodeテストはグラス上でPASS、最大表示Bitmap1047552bytes。全身の頭〜靴表示と顔モード復帰をスクリーンショットで確認。最後に全身モードへ戻した。装着した際の色・明るさ・見やすさ、会話とナビを含む全機能の場面検証は別途必要。

最新APK: loki-glass-v405-fullbody-frame.apk。証跡: glass-fullbody-v405.png、glass-face-v405.png。前の版の記録と未完了事項はこの追記で更新。

## 全身の常時点灯と拡大（406版）

前の405版は全身の常時点灯を実装していなかった。406版では保存された全身選択中にHUDを隠す要求・減光を抑え、FLAG_KEEP_SCREEN_ONを維持。起動時および全身選択時もHUDを表示する。顔選択中は各条件がfalseとなり、従来の自動消灯・明るさの処理を適用する。OS設定やスマホには変更しない。アプリが前面にある間の点灯維持を対象とし、電源操作や別アプリへの移動を強制的に阻止するものではない。

全身Bitmapから黒い外周余白を検出して除き、頭・手・靴の輪郭へ2ピクセル余白を付けて表示。人物比率を保持して表示枠へ収める。画像を新規生成せず、採用原本を表示時に切り出す。

406版をビルド・署名検証して更新インストール。操作せず待った後のスクリーンショットで全身・時計・操作列が残ることを確認し、端末Wakefulness=Awake、display suspend blocker保持を確認。頭上の空白が縮まり、靴まで収まる表示を確認。証跡glass-fullbody-v406-idle.png。

全168領域のAndroid読込みPASS、最大保持Bitmap862368bytes。顔の既存アニメーション関連6087項目PASS。切替時の一時PNGシート約6MiBと画素走査用配列等は別途必要であり、上記値はアプリ総使用メモリではない。

最新APKはloki-glass-v406-fullbody-always-on.apk（versionCode406、0.9.227-fullbody-always-on）。装着時の見やすさと長時間の点灯はユーザー確認が必要。

## 2026-10-08 v410: conversation poses and seated presentation
Installed on RG_glasses serial [device serial omitted] using adb install --no-incremental -r; version 410 / 0.9.231-fullbody-posture. Phone application not installed or changed in this step.
SET -> 全身姿勢：立ち姿／座り姿 selects and persists posture. Seated assets already present: 128-145,160,161,165-167. Neutral/listening seated posture uses 165; selected response expressions map to seated variants 166/167, while existing seated expressions are retained.
Whole-image rotation/scaling removed. Standing listening uses pose4; responses retain the existing expression selector. Neutral standing idle uses the existing six blink frames. Intermediate transition frames remain future work.
Corrected landscape-sheet 162-167 crop to exclude neighboring numeric labels while keeping the body and heels. Adopted original images are unchanged.
Validation: signed build 38 sources, zero errors; full-body policy test includes all168 expression mappings and seated pose membership; existing mascot regression tests 6087 checks passed. SET posture selection, seated screenshot, face mode return and full-body return verified through device UI. Final setting is seated full-body. Real conversation timing/visual quality still requires wearer evaluation; screenshot is application output, not optical capture.
APK SHA256: 6A0EAFE9C2759FDCE6CD6C2DB1D39C9B94AA178453B29890252793A575739B7D
Evidence: glass-seated-v410.png, glass-face-v410.png. Latest source copies and APK in this directory.

## 2026-10-08 v411: automatic standing/seated posture
Supersedes v410 posture selection. Removed fixed standing/seated UI and stopped reading full_body_seated. Full-body mode automatically uses seated165 for neutral rest, standing4 for listening, seated166 for expressions3/6/8 and seated167 for7/15; other expressions retain their original poses. Existing seated assets stay seated unless listening takes priority.
Rendering uses a normalized standing-height reference: standing height1.0, seated height0.76, shared bottom/floor baseline. This prevents fitting seated figures to the full standing display height. The approved source sheets vary in anatomy; 0.76 is an initial posture proportion, not a measured guarantee of identical head/bone size across every generated pose. Further asset calibration/intermediate frames remain possible.
Build: 38 source files, zero errors; APK signature verified. FullBodyMotionPolicyTest passed all168 expression/state routing and normalized-size checks. Existing portrait regression tests6087 checks passed. adb install --no-incremental -r succeeded on RG_glasses[device serial omitted]; installed version411 / 0.9.232-fullbody-auto-posture. Phone unchanged. Device screenshot glass-auto-posture-v411.png confirms reduced seated height with aligned feet; SET no longer offers fixed posture. Final setting full-body mode. Automatic conversation transitions validated by pure policy tests; wearer conversation timing/appearance has not been established by screenshot evidence.
APK SHA256: 14EE69340D6155CBB4FB1DA2D8F380F3C51AD609AE70E59C053727BEFCE70DD7

最新v412: 全168表情の切り出し矩形を個別修正。詳細は ../fullbody-crop-audit/README.md。APK SHA256 F6EF93A42C2C0DE2393D87785DE3AD6279C458E25E0D52BB36B6795237C190A2

最新v413: 通常6ポーズの瞬きと発話状態に応じた口パク。詳細・最新APK ../fullbody-face-motion/README.md。SHA256 B7FAABD82222AF30E7DF04F22B9B2555E035A90D0753559BFB0E3D3EF55A74F3

最新v414: 立ち座り/腕の中間コマ。詳細・最新APK ../fullbody-transitions/README.md。SHA256 90D26690F5BFA371F2EB68BB5A57B512648245D080AE91039BA1B4E02D62712A

最新v415: 正面座りと脚の組み替え。詳細・最新APK ../fullbody-seated-front/README.md。SHA256 4C9CD94C1799F3171F8673B6FDEE002D57BFF0087EA7005F7F1306CA20328814

