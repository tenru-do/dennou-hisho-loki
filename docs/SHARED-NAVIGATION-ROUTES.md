# グラス・スマホ共通の公共交通経路（v525 / v178）

## 乗車地点からの開始修正（v529 / v183）

未設定の乗車地点までの移動は徒歩とする。自転車はナビ欄で明示して選択した場合のみ利用する。NAVITIMEの旧access_modeによる最寄り駅への自動付け替えは行わず、実際の検索開始地点から候補を取得する。

選択経路の最初の乗車地点から70m以内で、30秒以内・精度70m以内の位置があるときはBOARDING（乗車地点付近・乗車待ち）から開始し、同じ地点への自転車・徒歩経路を新規取得しない。バス乗場も対象。選択した公共交通区間の形状・候補IDを保持する。付近への到着は乗車済みの証拠にはしない。徒歩・自転車で乗車地点へ接近した場合も、ジオフェンスは乗車待ちへ進めるだけとする。

実際に乗った際は「乗車した（電車・バス）」でTRANSIT_LEGへ進める。乗車済みのあと、選択した降車地点への接近は既存の降車判定を利用する。候補にはデータ提供元の車種に基づく[バス]／[鉄道]を表示し、APIが返さないバス路線・ダイヤを生成しない。

TransitBoardingStartTestの13項目、MultimodalJourneyTestの40項目、共通候補とHUDの回帰テストPASS。東京駅丸の内南口での実測、実際のバス候補・時刻表の取得、端末上の操作は未確認。ソース・ビルドの検証と実機反映を分ける。

グラスのNAVIで目的地と公共交通を選ぶと、スマホのロキが取得した最大3件の候補をグラスに表示する。スマホのロキのMAPナビ開始も同じ候補ストアを使用する。同じ目的地名・座標・移動手段で、3分以内かつ検索開始位置から200m以内なら同一の候補IDと表示を返す。移動・期限切れ時は再検索する。候補はメモリ内のみで、端末再起動では破棄する。

選択時は候補ID、目的地名、座標、移動手段、有効期限を照合する。期限切れや別の検索で上書きされたIDは拒否する。候補一覧だけでナビは開始しない。選択したJSONのコピーを案内へ渡し、乗車時に別APIで別ルートを再検索しない。アクセス区間と降車後の道路経路は既存設定に従って生成する。経路ペイロードにはselectedRouteIdとselectedRouteLabelを付け、選択候補との照合を可能にする。

スマホはロキの選択経路画面を表示する。Googleマップは利用者が別途開く方式とし、ロキの選択経路をGoogleマップへ強制指定したと誤認させない。GoogleマップのDirections URLに指定できる目的地・移動手段等と、ロキのプロバイダーが返す候補IDは別の仕組みである。

NAVITIME利用時は同一検索の複数itemsをそれぞれ変換し、shape_transitには対応するsummary.noを渡す。追加候補と形状取得にも既存の利用回数制限を適用する。平均所要時間に基づく候補はその旨を表示し、実際の列車の運行・遅延を検証したとは扱わない。

最終目的地は施設名を維持し、途中の乗車駅・降車駅はsegmentDestinationとして分離する。位置未確認でもAPIが返した区間駅数と降車予定時刻は表示する。API未提供の駅数や現在位置からの残駅数は作らない。残駅数の移動中実証はこの変更のPCテストでは確認していない。

確認: NavigationRouteChoicesTest、MultimodalJourneyTest、JourneyHudTest、NavigationOverviewPayloadTest、TransitPolylineTest。実機での候補選択・表示一致・移動中の追従は別途確認が必要。

参照:
- https://developers.google.com/maps/documentation/routes/alternative-routes
- https://developers.google.com/maps/documentation/urls/get-started#directions
- https://api-sdk.navitime.co.jp/api/specs/api_guide/route_transit.html
- https://api-sdk.navitime.co.jp/api/specs/api_guide/shape_transit.html
