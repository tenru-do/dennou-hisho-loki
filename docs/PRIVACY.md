# プライバシー

## 保存される情報

- グラス: Gemini APIキー、カスタム指示、連携トークン、最後に接続したスマホのローカルIP、待機時刻
- スマホ: カスタム指示、連携トークン、直近90日のAI会話ログ、90日以前の圧縮長期記憶、メール通知候補（最大20件・7日間）、ロキ・トピックと案内済み項目、歩数、市区町村名、天気・気温のキャッシュ

いずれもAndroidのアプリ専用領域へ保存します。APIキー、カスタム指示、会話ログ、カレンダー内容、メール内容をソースコードや配布リポジトリへ収集・同梱する処理はありません。

90日以前の会話は、質問と回答を短くした抽出要約としてスマホ内へ残ります。圧縮処理自体は端末内で行い、要約目的で外部APIへ送信しません。長期記憶には自動の保存期限を設けていないため、不要になった場合はスマホ側のログ削除を実行してください。

## 読み取る可能性がある情報

- カレンダー: `READ_CALENDAR` を許可した場合
- メール候補: Androidの通知アクセスを利用者が明示的に許可した場合。通知に含まれる送信者・件名・概要を読み取ります
- Gmail未読件数: Gmailのラベル読み取り権限を利用者が許可した場合。アカウントを識別して受信トレイの未読件数だけを取得し、アカウント名やメール本文はグラスへ渡しません
- 歩数: Health Connectの `READ_STEPS` を許可した場合
- 音声: `RECORD_AUDIO` を許可し、音声入力を開始した場合
- 現在地・天気・交通・ナビ: `ACCESS_COARSE_LOCATION` または `ACCESS_FINE_LOCATION` を許可した場合。ナビ中は座標、進行方向、速度、経路、道路名を扱います
- AMB周辺音声: グラスまたはスマホで `RECORD_AUDIO` を許可し、利用者が `AMB` を明示的にオンにした場合
- AMB再生音: Audio Relayで利用者がAndroidのメディア投影を明示的に開始した場合
- RokidAI表示文字: 任意の「RokidAI表示連携」アクセシビリティサービスを利用者がAndroid設定で明示的にオンにした場合。対象はRokidAI関連パッケージだけで、取得文字は一時的なHUD連動に使用し、会話ログへの保存や外部送信は行いません

## 外部送信

### 全身・顔モードと連続撮影（v447）

感情・動作の判定は通常のGemini返信に内部メタデータを付加して行います。メタデータは読み上げ前に除去します。全身・顔の反応監査はアプリ内部に時刻、感情、画像ID、補正結果、端末内秘密鍵による会話のHMAC-SHA256を記録し、各ファイル約256KiBで一世代保存します。本文を含まなくても感情や時刻は機微情報になり得ます。ハッシュは暗号化・完全な匿名化ではありません。

AMBでカメラまたは音声＋カメラを選ぶと、周囲の画像もGeminiへ送信されます。周囲の人の顔、画面や文書が映り込む可能性があります。音声のみのAMBとは送信内容が異なります。

RECの連続撮影はカメラ画像をアプリ専用の外部保存領域 `Pictures/loki-recorder` に保存します。REC自体は写真を外部APIへ送信しません。ただし、別途AMBを有効にした場合はAMBの外部送信も行われます。v447では写真本体をAndroid Keystoreの鍵によるAES-GCMで暗号化します。旧写真も起動時に移行し、失敗時は元写真を保持します。撮影時刻・保護マーカーは暗号化していません。共有ギャラリーへの自動登録はありませんが、ADBや権限を持つファイル管理アプリ等で読み取れる場合があります。

必要な写真はRECの「記録写真を復号して共有」から書き出します。書き出し用JPEGは内部キャッシュで最大1時間有効となり、期限後は読取りを拒否して次回起動・REC操作等で削除します。共有先が保存したコピーはロキから削除できません。Keystore鍵はバックアップに含まれないため、データ消去・アンインストール前に必要な写真を書き出してください。

保存上限は実ファイル合計5GBで、古い未保護写真を削除します。KEEPで保護された写真には自動保存期限がありません。不要な保護写真は保存領域から削除するか、アプリのストレージ消去で削除してください。アンインストールや消去前に、必要な写真だけバックアップしてください。

v447の通常ビルドは `debuggable=false` です。明示的な `-Development` ビルドだけが `debuggable=true` になります。許可済みADB接続からアプリ内部データへアクセスできます。利用者向け公開APKでは別途リリース設定の見直しが必要です。APIキーや写真を含む端末バックアップは公開しないでください。

質問、カスタム指示、必要に応じて予定・メール要約・ニュース文脈はGemini APIへ送信されます。音声認識はAndroid/Googleの音声認識サービスへ送られる場合があります。各サービスの利用規約とプライバシーポリシーも確認してください。

天気機能では緯度・経度を [Open-Meteo](https://open-meteo.com/) へ送信します。ナビHUDでは現在座標・経路をスマホからグラスへローカルLAN経由で送り、経路・道路名・目的地検索のためOpenStreetMap、OSRM、Nominatim系サービスへ座標や目的地を送る場合があります。

`AMB` の音声は、文字起こし・用語抽出・説明生成のためAndroid音声認識またはGoogle/Geminiへ送られる場合があります。Audio Relayを使う場合、短い音声データが再生元端末からスマホへ証明書を検証したローカルTLSで送られます。常時録音ファイルとしてリポジトリへ保存する処理はありません。

任意のCodex通知補助スクリプトは、ローカルPC上のCodexタスク完了文または許可要求の発生を検出し、設定したスマホへ短い通知文を送ります。連携トークン・スマホIP・端末シリアルはGit対象外のローカル設定ファイルにだけ保存します。

ロキ・トピックでは主要ニュース、地域ニュース、周辺イベントの取得にGoogle News RSSを利用します。地域検索にはスマホで得た市区町村名が使われる場合があります。

v447のスマホ・グラス・Audio Relay・PC間の機能通信はTLSと共有トークン認証を使います。旧版は平文HTTPなので双方を更新する必要があります。8765番の公開証明書確認に本文・認証情報は送らず、TLS認証に失敗しても平文へ戻しません。新規ペアリングは証明書と接続コードの照合が必要です。詳細は [v447の対策と移行手順](SECURITY-v447.md) を参照してください。

## 削除方法

1. スマホ側のカスタム指示を空にして保存し、ログを削除します。この操作で直近ログと圧縮長期記憶の両方を削除します。
2. グラス側のGemini設定でAPIキー、カスタム指示、連携トークンを空にして保存します。
3. メール通知候補、ロキ・トピック、案内済み履歴を含めて完全削除する場合は、Android設定から両アプリのストレージを消去するかアンインストールします。
4. Google AI Studio側で不要なAPIキーを無効化・削除します。

公開前検査では、APIキー形式、秘密鍵、OAuth資格情報、個人メール、端末シリアル、個人用通知トピック、個人用カスタム指示を対象に検索します。


### v451のKEEP写真・位置情報
利用者が一括取り込みを実行した場合のみ、KEEP保護のないものを含む全記録写真と撮影メタデータを認証済みスマホへTLSで転送する。写真・受信途中断片・位置メタデータはスマホ内でもKeystore AES-GCMで暗号化する。スマホ内取り込み容量は500MBまでで、不足時は転送を中断しグラスの元写真を残す。新しい撮影に使う位置は既存のスマホ位置権限で得られる新しいサンプルのみ。新しい第三者サービスへの位置送信や逆ジオコーディングは行わない。
利用者がJPEG保存を選んだ場合、指定先には平文JPEGと撮影日時・取得済みGPS位置が保存される。EXIF書き込み用の一時JPEGは内部キャッシュに作り、処理終了時に削除する。異常終了で残った専用一時JPEGは1時間経過後のギャラリー起動時に削除する。スマホ側Keystore鍵もバックアップ対象外であり、データ消去・アンインストール前に必要な写真を書き出す。



## 転送後の削除と削除記録
スマホへの暗号化保存と再読込・SHA256一致を確認した写真だけ、グラスの元写真・メタデータ・KEEP印を削除する。スマホで写真を削除した場合、写真IDの削除記録をアプリ内に残して再取り込みを防ぐ。本文・位置・画像本体は削除記録に含めない。グラスv550以降は、スマホの削除済み応答を保存成功とは扱わず、グラスの元写真を保持し、保持マーカーで再取り込みを除外する。アプリデータを消去すると削除記録も失われる。

スマホの全写真削除はロキの取り込み写真が対象で、利用者がMediaStoreへ書き出したギャラリーJPEGや、PCに独立して取ったバックアップは削除しない。書き出したJPEGの日時・GPSは通常の写真として共有されるため、位置情報を含む写真の共有先は利用者が選ぶ。


## 会議録音・録画取り込みと後処理（v521／v175）
会議録音は利用者がMTGを開始したときだけ動作し、最大2時間で保存して終了します。TOPIC用の周囲音は録音マイクの直近4秒から取得し、録音全量をTOPICへ渡しません。TOPIC付きモードでは従来と同様に周囲音・指定したカメラ画像をGeminiへ送信します。「録音のみ」ではTOPICを停止します。
完成した10秒音声区間、文字起こし、AI要約はAndroid KeystoreによるAES-GCMで保存します。グラスの録音中の現在区間（最大10秒）はアプリ非公開領域の一時PCMとして平文保存され、区間確定時に暗号化して削除します。異常終了した一時PCMは次回処理時に復旧します。日時・長さ・終了理由・題名・TOPICコメント等のメタデータは非公開領域のJSONで、暗号化していません。
終了後、利用者が転送を選ぶと録音を認証済みスマホへ証明書固定TLSでコピーし、スマホのKeystore鍵で暗号化保存します。グラス側の元音声は削除しません。スマホの音声保存容量は合計2GBまでで、既存音声を自動削除して空けることはありません。会議記録には自動保存期限がありません。
PC／タブレットから選択した録画・録音は、512KiB単位の暗号化一時区間としてスマホに保存し、端末内で音声だけを取り出します。成功後は一時録画区間を削除します。失敗した入力は再試行または取消でき、未完了アップロードは次の読み込み操作時に24時間経過分を整理します。元ファイルや利用者が保存したテキストのコピーは変更・削除しません。
文字起こし・AI要約ボタンを選ぶと、その記録の音声と文字起こしをHTTPSでGeminiへ送信します。APIキーは端末側で利用し、ブラウザーへ返しません。動画の映像内容はこの処理では送信しません。認証済みWeb端末は会議記録・文字起こし・要約を閲覧でき、コピーした内容は受信端末側で管理します。
会話ログの削除は会議音声の削除ではありません。現在の会議記録画面に個別音声の削除操作はありません。完全削除には各端末のアプリデータ消去を用いますが、復号鍵も失うため必要な音声・テキストを先に書き出してください。Keystore鍵は独自バックアップに含まれず、バックアップだけで別端末へ音声を復旧できるとは限りません。

## カレンダー書込み・Codex指示連携
Googleカレンダー登録は明示された登録依頼とWRITE_CALENDAR許可により、選択した書込み可能カレンダーへ保存します。Googleとの同期はAndroidのカレンダー設定に従います。カレンダー名・予定内容は会話の文脈や登録確認に使われる場合があります。
Codex指示連携は任意のPC常駐プログラムと登録した会話を利用します。指示本文・会話ID・タイトルは連携処理に必要で、スマホの非公開領域とGit対象外のPCローカル設定・受領記録に保持します。期限とID・タイトルを検証し、送信結果不明時は自動再送しません。Codexへ渡した指示はCodexの会話とそのアカウントのアクセス権に従って処理されます。

## 明示的な画像質問（v522／v176）

撮影・選択した画像は、送信指示でグラスのGeminiへ送信します。氏名・診察予約等も送信対象です。スマホの待機画像と下書きは暗号化し30分で読取りを失効させ、次の利用時に削除します。外部カメラ撮影中の一時JPEGは専用キャッシュの平文で、戻り後に削除し、異常終了等で残ったものは期限後の次の利用時に削除します。画像を消しても解析結果は従来の会話履歴に残ります。詳細と画像からのカレンダー登録前確認は [IMAGE-QUESTIONS.md](IMAGE-QUESTIONS.md) を参照してください。

## 動画生成（2026-10-16）
明示的な動画依頼では、指示と選択した元画像を設定済みGemini／Veo APIへ送信します。ロキ自身の依頼は同梱の採用済み本人画像を参照します。動画生成のために会話履歴・位置・連絡先・無関係な写真を自動付加しません。指示・受付ID・動画区間はアプリ内で暗号化し、ギャラリーには通常のMP4コピーを保存します。動画APIには別途利用料金が発生します。詳細はVIDEO-GENERATION.md。

## 複数画像・動画の添付（グラスv553／スマホv203）

PC・タブレットのファイル選択・ドロップ・画像貼り付け、スマホの写真選択・共有で選んだ画像・動画を、送信前に端末内で処理します。画像・動画は合計6件、そのうち動画3本までです。動画は各2分・50MBまでで、8場面を抜粋した画像へ変換します。この経路では元動画・動画の音声をGeminiへ送りません。場面画像・画像内容・質問文は送信操作によってGeminiへ送られます。音声の文字起こしは別の会議記録機能です。

Googleフォトから実画像URLを含むドラッグをした場合、認証済みスマホがそのGoogle画像配信URLへ接続します。Googleにはスマホ側の接続情報が伝わります。GoogleアカウントのCookieや認証情報をPCからコピーせず、アルバム全体へのアクセス権も要求しません。サムネイルやページリンクだけでは画像データを取り込めない場合があります。

スマホの送信待ち画像と下書きはKeystoreで暗号化します。30分を超えたデータは次の選択・送信等の整理処理で削除するため、時間経過直後の物理削除を保証するものではありません。Webの準備済み添付はページ内メモリに保持し、添付解除・送信成功（送信中に入力を変更していない場合）・接続解除で破棄します。ブラウザ・OS側の一時ファイルの管理は、それぞれの実装に依存します。

ドロップ診断はスマホ内に最新1件の受信時刻・形式名・件数・処理段階だけを保存します。写真URL、HTML、ファイル名、写真や動画の内容は診断に保存しません。診断内容をGitHubへアップロードする処理はありません。


## 秘書の受信箱（グラス554／スマホ204）
明示的な保留依頼を受けたときだけ、依頼文と直近の会話の参照情報をスマホのアプリ専用領域へ保存します。会話に日程や出典URLがあれば保存対象となります。Android KeystoreのAES-GCMとAtomicFileを使用し、最大100件、不要な案件を自動削除しません。会話ログ削除とは独立しているため、受信箱から取消・削除してください。
保留の保存では新しいGemini送信や予定登録を行いません。再開して予定候補を作る際は、選択した案件の参照情報を既存のGemini経路へ送ります。認証済みスマホ・Web端末は案件の内容を閲覧・整理できます。登録前確認は10分で失効し、確定した候補のみをカレンダーへ保存します。登録成否が不明な案件は自動再送しません。詳しくは[受信箱の仕様](SECRETARY-INBOX.md)を参照してください。

### 文脈による受信箱の意図判定（555／205）
通常会話とは別に、現在の発言、直近会話の抜粋（最大8000文字）、保留案件の題名・日程・条件の短い一覧、確認候補や選択中案件の概要を設定済みGoogle Geminiへ送信します。会話や案件には個人情報が含まれ得ます。画像・音声・全長期記憶・個人用カスタム指示をこの判定に追加添付しません。API利用量・待ち時間が増えます。対象と内容の候補を示し、同意後に保存・変更・取消を反映し、意図判定だけで予定を登録しません。[詳しい仕様](SECRETARY-INBOX-SEMANTIC.md)。

### 返答前の文脈検査（グラス563）
短い続きの発言では、現在の発言（最大2400文字）、参照会話（最大6000文字）、回答案（最大6000文字）を、設定済みGoogle Geminiへ追加送信して対象の一致を検査します。内容には個人情報が含まれ得ます。原文の位置を番号で照合し、必要な場合だけ回答を1回作り直します。画像・音声は追加添付しません。検査が通信障害等で利用できなければ通常の回答を続けます。診断ログに会話内容やキーを出力しません。API利用量・待ち時間が増えます。[文脈検査の仕様](CONVERSATION-CONTEXT-REVIEW.md)を参照してください。

### 引き継ぎカード・会話付近の受信箱（564／207）
選択済みの会話参照を一時的な引用カードへ整理して、通常の会話リクエスト内で設定済みGeminiへ送ります。別の判定API送信や新しい保存先は追加しません。既存受信箱の保留件数・結果要確認件数をスマホと認証済みWebへ表示し、Webには保留題名も最大2件表示します。接続解除でWeb表示を消します。進捗通知には処理段階だけを含め、引用や会話本文は含めません。[仕様と検証](CONVERSATION-HANDOFF-PROGRESS.md)を参照してください。

### Webの処理段階と依頼文復元（スマホ208）
認証済みWeb画面へ、既存の会話履歴とともにグラス全体の処理段階・通知の古さ・送信待ち件数を返します。新しい外部サービスへの送信や権限追加はありません。状態の取得でキューを消費しません。依頼文の復元は入力欄に戻すだけで、送信は別操作です。[仕様と検証](WEB-CONVERSATION-RECOVERY.md)を参照してください。

### Web依頼IDと受信箱の完了通知（グラス565〜566／スマホ209）
認証済みWebからの通常テキスト依頼にIDを付け、スマホとグラス内で受信・処理・回答表示を照合します。IDはGeminiのプロンプトへ追加せず、新しい外部送信先やAndroid権限は追加しません。終端状態のグラス内保留レコードから依頼本文を除きます。既存会話履歴の保存方針は変更しません。結果未確認時は本文を自動再実行せず、状態通知のみ再送します。受信箱の遅延応答は元の依頼ID・会話世代と照合します。[仕様と検証](WEB-REQUEST-TRACKING.md)。

### 直近の画像読み取りパネル（グラス567／スマホ211）
画像解析済みの質問・回答・添付件数・読み取り時刻を、既存の認証済み接続でグラスからスマホへ同期し、スマホ会話画面と認証済みWebに表示します。画像本体・撮影位置・APIキーは追加同期しません。スマホ側のコピーはKeystoreのAES-GCMで暗号化し、読み取りから2時間で表示を失効、失効後の参照時に削除します。グラスの会話履歴削除とは独立した短期コピーです。追加のGemini送信や権限はありません。[仕様と検証](IMAGE-REFERENCE-PANEL.md)。

### 添付の種類・順序の引き継ぎ（グラス568／スマホ212）
画像質問と短期の読み取り参照へ、番号順の「画像／動画／種類未記録」だけを追加保持します。ファイル名・元のパス・追加の画像本体は保持せず、新しい外部送信や権限は追加しません。既存の暗号化と保存期限を引き継ぎます。[仕様](IMAGE-REFERENCE-PANEL.md)。

### TOPIC・AMBの発言直前判定（グラス569）
AMBで実際に表示した短い文章を、同文抑制のため最大8件・90秒の判定窓でメモリ内に保持します。ファイル保存や追加の外部送信は行いません。「発言判定を見る」には理由と経過時間だけを表示し、会話本文・画像・音声は表示しません。録音・KEEPの保存方針は変更しません。[仕様と検証](PROACTIVE-DELIVERY.md)。

### 録音優先の画像負荷調整（グラス570）
録音の保存待ち・端末メモリ・撮影間隔に基づき任意の画像処理を待機させ、直近の理由を既存の判定画面へ表示します。理由に写真・音声・会話内容を含めず、新しい記録や外部送信を追加しません。録音とKEEPの既存保存先・保存上限は変更しません。[仕様と検証](CONCURRENT-CAPTURE-PRIORITY.md)。

### 受信箱の確認状態・再開操作（スマホ213）
既存の暗号化受信箱から、登録確認待ち・確認期限切れ・結果要確認などの表示用状態を読み取り時に算出します。確認トークンや登録候補は一覧・詳細へ出さず、表示だけで保存内容やカレンダーを変更しません。Web検索は取得済み一覧の端末内絞り込みです。再開は入力欄への復元のみで、送信・登録は別操作です。新しい外部送信・保存先・権限は追加しません。[仕様と検証](INBOX-PRESENTATION.md)。


## Local diagnostics and transfer status (glass 574 / phone 217)

The local health check uses a temporary private cache file, an existing Android Keystore key, bundled files, permission state, and queue JSON structure. It does not create a replacement key, capture media, contact an AI or routing provider, replay commands, or repair stored data. A missing key is reported as not ready.

The operation overview projects existing receipts and inbox states. Conversation titles may include up to 80 characters of an existing request; the overview is restricted to the already authenticated phone/PC/glasses connection. Transfer diagnostics contain opaque batch IDs, counts, timestamps and acknowledgement states; they do not add raw photo/audio upload. Invalid records are reported without being rewritten. Startup check results are saved locally and shared with paired devices through the existing authenticated bridge. These checks are not proof of live AI, navigation, camera or microphone success.

Only manifest-declared permissions are checked for readiness. Undeclared permissions are reported as not_required; the phone uses the external camera intent and does not require a CAMERA permission for this check.

### Read-only process observation (phone 218 tooling)
The optional PC tool records only elapsed time, process IDs, PSS memory and availability summaries for explicitly specified devices. Device serials are command arguments, not embedded in source or saved reports. It does not collect raw logs or media, initiate workloads, or send provider requests. Operation ordering and monotonic connection freshness add no new content collection.

### Reference draft key (phone 222)
The existing authenticated reference summary includes a SHA-256 content identifier. Native saved UI state and Web in-memory drafts keep that identifier with the question prefix to check for changes before sending. The Web reception check uses existing encrypted summaries. Keys are not sent to Gemini and no image bytes or location are added.

### Queued reference identity (glass 576 / phone 223)
The opaque reference identifier travels in the existing private command queue and tracked receipt ledger, including native reference follow-ups. Glasses validate it before dispatch and around prompt preparation. It is stripped from the user text and is not added to Gemini prompts. No extra media copy, capture, external destination, or permission is introduced.

### Local reference-stop diagnostics (glass 577)
A reference-change stop is not added as a Gemini conversation reply. Only the fixed local reason code and occurrence time are retained in private preferences and shown in the glasses diagnostics. No prompt, image, location or provider error body is added to that record. Existing historical logs are not removed.

### 送信控え・明示訂正・共通進行状態（グラス580／スマホ225）

スマホの送信控えは質問文と解析へ送った添付画像（動画は抜粋画像）を、アプリ専用領域でAndroid KeystoreのAES-GCMにより暗号化し、最大10件・24時間保持します。期限切れは次の参照・保存時に削除します。元動画全体は保持しません。認証済みWebとスマホで復元でき、復元だけでは再送・生成・予定登録を実行しません。通常の会話履歴を削除しても、この独立した短期コピーは期限まで残る場合があります。端末の登録解除後はWebから取得できません。

グラスの前回質問の復元用コピーは暗号化し24時間で失効。明示された訂正は最大4件・2時間保持し、同じ話題の通常会話内でGeminiへ渡します。会話リセット・新しい話題では外します。追加の意図判定APIはこの保持機能にはありません。会話の完結性判定は端末内で行います。

共通進行状態は固定の段階・理由コード、時刻、依頼IDだけを既存の認証済み接続で共有し、本文・画像・音声を追加しません。進行状態を読むだけで処理を再送しません。詳しくは[会話の復旧](CONVERSATION-RECOVERY-v580.md)を参照してください。
