# Setup notes

> 公開版ではスマホ–グラス間の通常エンドポイントに共有トークン認証が必要です。トークンは手入力しません。スマホ側で60秒のペアリング受付を明示的に開始し、その間にグラス側の `SET` を押すと、一回限りのペアリングエンドポイントからアプリ専用領域へ直接保存されます。信頼できるLANまたは自分のテザリングで実行してください。

These notes describe the original prototype flow. They are not yet a polished installation guide.

## Requirements

- Rokid glasses with developer/ADB access
- Android phone on the same network as the glasses, or another reachable phone-to-glasses network path
- Android SDK build tools
- Gemini API key

## High-level setup

1. Build and install the phone companion app.
2. Grant required phone permissions:
   - calendar access
   - notification access for mail summaries
   - microphone permission if using phone-side speech recognition
3. Build and install the glasses app.
4. In the phone app, open `指示` and press `グラスをペアリング（60秒）`.
5. Within 60 seconds, press `SET` in the glasses app. The shared token is transferred automatically and the pairing window closes after the first successful claim.
6. Open the glasses `SET` screen and save the Gemini API key and optional custom instructions.
7. Confirm that the phone app local server is running, then use text input from:
   - Bluetooth keyboard connected to the glasses
   - the phone companion app
   - glasses-side voice input, where supported

## Gemini quota/rate limiting

The glasses app includes local pacing/cooldown logic for Gemini API 429/503 responses. If you hit 429 often:

- confirm that the Gemini API key has billing/quota available for the selected model
- wait several minutes
- reduce rapid repeated prompts
- use a lower-cost/faster Gemini model

Frequent `429` responses are expected if the key/model combination is not provisioned for sustained use. This project still needs tuning around retry intervals, long prompts, and voice-driven repeated requests.

## Network notes

The phone and glasses must be able to reach each other. In the original environment this was usually done over Wi-Fi. Outdoor use may require phone tethering or another stable local route.

## Build status

The current source snapshot is not yet a standard Gradle project. A future cleanup should add:

- Gradle wrapper
- reproducible build scripts
- debug signing instructions
- release signing instructions
