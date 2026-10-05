# Loki Audio Relay

Loki Audio Relay captures playback on the Android device that is currently
sending Bluetooth audio to the Rokid glasses. It works with either the main
Galaxy phone or a separate Android tablet running Android 10 or later.

## Setup

1. Install `audio-relay-app` on the Bluetooth source device.
2. Keep the source device and Galaxy bridge on the same trusted Wi-Fi LAN or
   personal hotspot.
3. In the Galaxy secretary app, open `操作` and press `音声PAIR`.
4. Within 60 seconds, open Loki Audio Relay on the source device and press
   `Galaxyを検索`.
5. Press `再生音声 ON` and approve Android's audio-sharing dialog.
6. On the glasses, press `AMB` and select `Bluetooth` or `両方`.
7. To stop capture and battery use, press `再生音声 OFF` on the source device.

When Galaxy itself is the Bluetooth source, install both the secretary bridge
and Audio Relay on Galaxy. Discovery automatically checks the same device.

## Data handling

- Captured PCM stays in memory and is discarded after recognition.
- PCM is sent only to the paired Galaxy bridge over the local network.
- The bridge token is transferred during a user-opened 60-second pairing window.
- The latest pending transcript expires after two minutes and is removed when
  acknowledged by the glasses.
- Relay transcripts are not added to the phone conversation log.
- Only the short transcript excerpt, not PCM, is sent onward to Gemini by the
  glasses when an explanation is requested.

The local bridge uses authenticated HTTP rather than TLS. Use it only on a
trusted LAN or your own hotspot. Apps that prohibit Android playback capture,
DRM media, calls, and protected playback may be silent.
