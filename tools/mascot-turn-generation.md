# 高密度アングル素材の生成要件

生成方法: Codex の built-in image_gen。既存の `mascot_sheet_v6_smooth_talk.png` と `mascot_sheet_v5_extra_18.png` をキャラクター・線画スタイルの参照として使用。最初の生成結果には右端の欠けがあったため、次のプロンプトでレイアウトを修正した。採用結果が `turn-reference-raw.png`。

採用した修正プロンプト（全文）:

> Correct this sprite atlas layout and temporal ordering, preserving the same cyan lineart female head identity on opaque pure black. The current rightmost column is clipped and the temporal head scale varies. Rebuild it as an EXACT FOUR-column by EIGHT-row atlas, 32 equal square cells, canvas aspect ratio 1:2 (e.g. 1024x2048). FOUR complete heads across EVERY populated row; DO NOT clip heads at any outside edge. Each head occupies only 70% of cell width centered x50%, top of hair y8%, neck ends y90%. No gutters, text, borders, numbers or graphic decorations. First FOUR rows = indices0..15: first FIFTEEN heads are monotonic yaw35deg nose toward viewer's right to yaw0deg symmetrical front in exact 2.5degree steps read left-to-right then top-to-bottom; cell15 pure black EMPTY. Second FOUR rows = indices16..31: same FIFTEEN identical yaw poses and placements as cells0..14 but both eyes completely shut with delicate cyan lash curves, cell31 EMPTY. Mouth NEUTRAL CLOSED in all frames, no smiles, no facial-expression variation, same scale, no head roll or pitch, neck centered. Hair/outline/nose/ear/neck perfectly registered between open and closed counterparts; ONLY EYE pixels differ. Upper-left first-frame eyes aim slightly screen-right, gradually direct frontal gaze by cell14. Prioritize precise 4x8 equal cell grid and same head scale/center. Thin cyan lines, no filled skin, no big neon glow. Include all four columns in full: each far right cell has same black side margin as all others.

## パッキング

リポジトリのルートで実行:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/pack-high-density-turn.ps1
```

Windows の System.Drawing を使用し、外部の API キーや追加パッケージは不要。生成済み原画から繰り返し同じ素材を作る工程であり、AI 画像を再生成するスクリプトではない。

- 原画の 4列×8行を読み取り、開眼15ポーズ・閉眼15ポーズを対応づける。
- 頭部の高さ・中心位置を揃え、320px の顔セル15枚を 5列×3行へ配置する。
- 顔の画素から各ポーズの左右の眼・口の ROI を抽出し、対応する閉眼画像を眼だけの64×24pxセル30枚に分割する。
- `v7_high_density.png`: 1600×960、320pxセル15枚。
- `v7_high_density_eyes_closed.png`: 320×144、64×24pxセル30枚、各ポーズにつき左眼・右眼の順。
- `v7_high_density_regions.json`: 顔セル内の左右眼・口の矩形。実行時の描画座標とキャッシュ切り出しに使う。

生成プロンプトの角度は制作上の目標であり、画像から測定した実際の3D角度ではない。生成線画の形状はフレーム間で完全には一致しないため、実機サイズでの視覚確認・必要に応じた原画の修正は別途必要。
