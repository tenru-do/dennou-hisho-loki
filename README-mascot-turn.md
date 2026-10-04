# MascotView: 高密度首振り・瞬き・視線先行

この差分は、前回の ValueAnimator / ハードウェア描画改修後のローカルソースを基準にする。作業ツリーには適用済み。以前の未コミット変更はこの差分に含めない。

## 動作

アングルの両端を含む15ポーズ（中割り13枚）を起動時にキャッシュ。通常の首振りでは画像全体を溶かさず、進行位置に対応する1枚を描く。閉眼・視線・口は顔とは独立した小さいキャッシュ画像で重ねる。

`ValueAnimator.ofFloat(0f, 1f)` と `AccelerateDecelerateInterpolator` を維持し、ディスプレイの Vsync に合わせて更新。全遷移は560ms。イージングの進行度0〜14%で視線が先行し、14〜86%で首を回し、最後に姿勢を落ち着かせる。中央41〜59%では、その時点のアングルに対応する目閉じセルで両眼を覆う。視線オフセットは最大で顔幅の0.9%で、32%までに通常位置へ戻す。

途中反転は現在のアングルから開始し、距離に応じた時間を確保する。元の顔から新しい素材へ入る部分と3/4へ戻る終端のみ、短い GPU クロスフェードを行う。View 切り離し時の Animator / タイマー停止も維持する。

口パク・通常の瞬きは既存の独立タイマーで更新。アングル遷移中と正面保持中にもキャッシュ済み口画像を重ね、強制瞬きと衝突させない。onDraw から Bitmap 生成・リサイズ・アニメーション開始・再描画予約を行わない。

新規アセットの欠落、寸法違い、ROIの不正がある場合は検証途中の素材を公開せず、従来のキャッシュ済みアングルへ戻す。

## ファイル

- `glass-app/src/com/example/rokidkeyboardbridge/MainActivity.java`: MascotView の統合。
- `glass-app/src/com/example/rokidkeyboardbridge/MascotTurnPolicy.java`: Android 非依存のフレーム・瞬き・視線タイミング。
- `glass-app/tests/com/example/rokidkeyboardbridge/MascotTurnPolicyTest.java`: 60Hz 時系列、全ポーズ選択、往復・途中反転などの検証。
- `glass-app/assets/v7_high_density*`: 2シート＋ROIマニフェスト。
- `tools/pack-high-density-turn.ps1`: 原画の切り出し・配置・ROI抽出を再現するスクリプト。
- `tools/turn-reference-raw.png`: 採用した生成原画。
- `tools/mascot-turn-generation.md`: 生成プロンプト・再作成方法。

## 確認範囲

glass-app の全 Java ソースのコンパイル: exit 0。MascotTurnPolicyTest: 1398 checks PASS、MascotAnimationControllerTest: 248 checks PASS、MascotSpeechEffectsTest: 1130 checks PASS（計2776）。APK のビルド・署名検証: PASS、新規2シートとROIマニフェストの収録確認: PASS。patch の正方向・逆方向適用チェック: PASS。隔離ディレクトリへ適用した結果は、PNGのSHA-256と改行を正規化した全テキストが配布物と一致。60Hzシミュレーションは端末の実測ではない。実機の60fps維持、GPUドライバの挙動、演出の自然さは未確認。今回の版はインストールしていない。

画像生成に使ったのは image_gen、スクリプトは生成済み原画の決定的なパッキング工程。画像から物理的な首の角度を実測しているわけではない。新規画像のデコード・顔・眼・口キャッシュで追加メモリは概ね13MiB、GPUテクスチャ分は別途必要。

## 別の同一基準作業ツリーへの適用

```powershell
git apply --check mascot-high-density-turn.patch
git apply mascot-high-density-turn.patch
```

バイナリ画像も patch に含める。すでに適用済みの作業ツリーへの再適用は不要。
