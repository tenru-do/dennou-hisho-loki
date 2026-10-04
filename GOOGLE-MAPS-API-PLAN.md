# Google公式APIへの移行検討（2026-09-20）

状態: 調査・設計のみ。API有効化、課金登録、APIキー作成、API呼び出しは行っていない。

## 結論

第一候補は **Routes API + Map Tiles API（2D）**。スマホが経路データと地図タイルを取得し、グラスがGPS中心で描画する。Googleマップアプリの画面共有には戻さない。

- Routes APIは経路形状・所要時間・各区間を取得でき、公共交通の区間情報にも対応する。既存Googleマップアプリで選択した経路を読み出すAPIではない。ロキ内で目的地・移動手段・候補経路を選ぶ導線が必要になる。[経路取得](https://developers.google.com/maps/documentation/routes/compute_route_directions)、[公共交通](https://developers.google.com/maps/documentation/routes/transit-route)
- Map Tiles APIはGoogleの道路地図をタイルとして取得し、独自の描画部品で表示するためのサービス。現在のGPS中心の描画構造と組み合わせる案。Google公式のスタイル設定を使い、独自の色判定で道路を抽出する処理を置き換える。[2D地図](https://developers.google.com/maps/documentation/tile/2d-tiles-overview)
- APIの経路を表示する地図もGoogle地図にする。OSM背景にGoogle Routesの線だけを重ねる構成にしない。帰属表示・利用規約・プライバシーポリシーを満たす必要がある。[Routesの表示要件](https://developers.google.com/maps/documentation/routes/policies)

これは技術的な構成案であり、実機でのAPI通信・グラスの視認性・対象地域の各移動手段の品質は未検証。

## Navigation SDKをそのまま採用しない理由

- SDK自体はバックグラウンド位置更新と経路変更時の形状通知に対応している。[公式FAQ](https://developers.google.com/maps/documentation/navigation/android-sdk/faq)
- ただし公式ポリシーには商用アプリ向けの利用条件がある。個人用・公開実験アプリとしての適合を確認せず採用決定しない。[SDKポリシー](https://developers.google.com/maps/documentation/navigation/android-sdk/policies)
- 今回のグラス実機ではGoogle Play開発者サービスのパッケージを検出できなかった。一般スマホ向けのGoogleマップSDKがグラス上でそのまま動くとは見なさない。
- 公共交通を含む全機能を単一のSDKで対応できるとはしていない。

## 料金の目安

公式の標準従量料金、USD。2026-09-20確認。無料枠は該当SKUと請求アカウントの利用状況に左右される。

| サービス | 月の無料枠 | 最初の有料区分（1,000回あたり） |
|---|---:|---:|
| Routes: Compute Routes Essentials | 10,000回 | $5.00 |
| Routes: Compute Routes Pro | 5,000回 | $10.00 |
| Map Tiles API: 2D Map Tiles | 100,000タイル | $0.60 |
| Navigation Request（比較用） | 1,000目的地 | $25.00 |

使う機能によってRoutesのSKUが変わる。地図は1画面=1リクエストではなく複数タイルを使う。無料枠超過後なら、例えば2万タイルは$12、Essentialsの1,000回は$5。税・換算・別サービス利用分は含めない。[公式料金表](https://developers.google.com/maps/billing-and-pricing/pricing)

## 実装前に必要な確認

1. Google CloudプロジェクトとMaps用APIの利用・請求設定の明示的な承認。Geminiの残高で自動的に使えると扱わない。
2. 当面の利用量上限と、無料枠超過時に自動停止するかの選択。予算アラートだけを課金停止装置として扱わない。
3. APIキーの安全な入力・制限方式。GitHubやAPKに埋め込まず、チャットにも貼らない。アプリ制限が対象Web APIで有効か実キーで検証する。必要なら認証付きバックエンドを別途用意する。
4. 地図タイルは返却されるキャッシュ指示と規約を守る。既存OSMの7日固定保存を流用しない。帰属表示は画面端で欠けないようにする。[タイルの要件](https://developers.google.com/maps/documentation/tile/policies)

## 移行後の完了条件

- ロキで選んだ経路とグラスの線が同じrouteId/世代に属する。古い目的地・古い探索の応答を捨てる。
- 徒歩・自転車・車・公共交通を明示的に区別し、未対応を黙って徒歩へ置換しない。
- 有料道路回避などの条件を経路取得・再探索に引き継ぐ。
- 公共交通の乗換駅・最終目的地・到着時刻を同じ旅程から表示する。
- ロキ/Googleマップがスマホ前面でない状態でも地図・経路・案内が更新する。
- 現在地が常に地図内にあり、小型アイコンで進路を隠さない。枠線は不要。
- 移動・経路逸脱・再探索・通信断・GPS停止・ナビ終了を実機試験する。
- ナビ中の会話コメントを維持し、地図を縮めない。

これらを満たすまで「経路線を含めて全部解決」とはしない。
