# 立って手を振る姿の体格補正 v664

ユーザーが指摘した「立って手を振ると顔と手が大きく、ずんぐり見える」画像を補正し、グラスv664へ適用しました。スマホはv291を維持しています。

組み込み image_gen で標準立ち姿を参照して描き直し、頭・手・胴・脚の比率を調整しました。描画で画像を縦に引き伸ばす補正はしていません。採用した3コマを 0→1→2→1 の対応で再生し、手振り中に左右の手が入れ替わらないようにしています。生成画像の未使用の4番目のコマは再生しません。

座り側は従来の loki_body_greeting_v654.png を継続使用し、v663 APK中の画像とバイト一致を確認しました。ほかの仕草・接触・顔画像は差し替えていません。全姿勢の体格が完全一致するという保証や、全身画像443枚の個別目視を完了したという意味ではありません。

Android上で修正前・通常姿勢・修正後を同じ240×600の表示枠に描画。頭部シルエット幅／可視身長の指標は0.1559から0.1240へ約20%小さくなりました。これは画像上のしきい値測定で、身体寸法の厳密測定値ではありません。描画・関連機能997項目、既存動作14種類に成功し、手・足元の画面内収まりも確認しました。

適用前に旧APKと端末内データを非公開領域へバックアップし、署名と適用APKのSHA256を照合。初回起動前に既存417ファイルの内容一致、写真3,848枚のサイズ・更新時刻一致を確認しました。写真内容の全件再ハッシュは行っていません。

## 保存先

- [補正後の画像](C:/Users/user/Documents/Codex/2026-06-23/rokid-bluetooth-rokid-bluetooth-rokid-hirokid/feature-worktrees/ambient-conversation-assistant/glass-app/assets/loki_body_greeting_standing_v664.png)
- [実際に使用した生成指示](C:/Users/user/Documents/Codex/2026-06-23/rokid-bluetooth-rokid-bluetooth-rokid-hirokid/feature-worktrees/ambient-conversation-assistant/artwork/greeting-proportions-v664-prompt.txt)
- [Android描画の比較画像](C:/Users/user/Documents/Codex/2026-10-20/loki-maintenance-20261020/outputs/standing-v664-comparison.png)
- [検証結果](C:/Users/user/Documents/Codex/2026-10-20/loki-maintenance-20261020/outputs/body-proportions-v664.json)
