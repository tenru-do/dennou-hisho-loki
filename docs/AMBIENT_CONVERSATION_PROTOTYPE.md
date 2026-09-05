# AMBIENT conversation assistant prototype

Source-device setup is described in [AUDIO_RELAY.md](AUDIO_RELAY.md).

This experimental branch is isolated from the current public Loki release in
Git, but its APK intentionally uses the existing package
`com.example.rokidkeyboardbridge`. Installing it with an update operation keeps
the existing app preferences, including the API key and bridge settings.

## Behavior

- Pressing `AMB` while off opens an input selector: glasses microphone,
  Bluetooth playback, or both. The last choice is remembered. Pressing `AMB`
  while active turns the selected capture mode off.
- When both sources are selected, the glasses microphone and playback transcripts
  from an audio-source Android device are handled concurrently. Gemini requests
  are still processed one at a time.
- The tested Rokid OS exposes its Bluetooth A2DP input to third-party applications
  as a silenced `AudioRecord`. Bluetooth playback is therefore captured before
  transmission by the separate `audio-relay-app` on the actual source device.
- The source device may be the main Galaxy phone or a separate Android tablet.
  Install and enable Loki Audio Relay on whichever device is currently sending
  Bluetooth audio to the glasses.
- Audio Relay captures bounded in-memory PCM windows with Android playback
  capture, sends them over the authenticated local bridge to the Galaxy speech
  recognizer, and discards them after each request.
- The Galaxy bridge stores only the latest pending transcript for at most two
  minutes. The glasses acknowledge and remove it after receipt.
- At most 500 transcript characters are sent to Gemini Flash Lite.
- Gemini returns up to two specialized terms with short Japanese explanations.
- Results are displayed silently for 12 seconds; ambient mode does not use TTS.
- Audio, raw transcripts, and ambient results are not written to app logs or history.
  The in-memory audio queue is capped at four items and items expire after 30 seconds.
- Gemini calls are limited to one per 60 seconds. HTTP failures back off for at
  least 90 seconds.
- Listening pauses while the normal assistant, VOICE, TTS, Loki Topic, or a
  pending phone command is active, and while the app is in the background.
- Listening also pauses at 42 C battery temperature or at 15% battery when not
  charging.

## Privacy note

Audio is not uploaded directly to Gemini. The paired Galaxy's Android speech
recognition service receives temporary PCM from either the glasses microphone
or Audio Relay, and the resulting transcript excerpt is sent to Gemini. Audio
Relay does not write PCM or transcripts to files or conversation logs. Therefore
this mode still performs cloud processing and must only be enabled where
recording, playback capture, and transcription are appropriate. `AMB OFF` stops
processing on the glasses; use `再生音声 OFF` on the source device to release its
playback-capture session and stop battery use.

## Current limitations

- Audio Relay requires Android 10 or later and must be installed on the device
  that is actually sending Bluetooth audio (Galaxy or tablet).
- The Relay device, Galaxy bridge, and glasses must be mutually reachable on a
  trusted Wi-Fi LAN or personal hotspot; Bluetooth audio alone does not provide
  the IP path used for transcription.
- Android playback capture can receive only apps that permit
  capture; DRM-protected media, calls, protected apps, and some players may be silent.
- Android displays a system audio-sharing consent prompt when Relay is started.
  Newer Android versions require approval for each new capture session.
- Simultaneous outside-microphone and Relay playback handling still needs
  real-device validation.
- The paired phone app, bridge token, microphone permission, network access,
  and a Gemini API key are required.
- Explanations are model-generated summaries, not a dedicated web fact-check.
- Device heat, recognition accuracy, rate limits, and HUD timing still require
  real-device validation before this can be merged into the public release.

## Recovery

The public source and signed APKs for `v0.9.7-alpha` remain available on GitHub.
They restore the application executable, but deliberately do not contain API
keys, custom instructions, conversation history, Google data, or other private
runtime state. Back up those private settings separately when needed.

The prototype and `v0.9.7-alpha` APKs use the same signing certificate. To keep
app data, never uninstall or clear storage when rolling back. Use an Android
debug bridge downgrade update such as `adb install -r -d <old-apk>` because the
prototype has a higher version code. Android's normal package installer may
reject a lower version even though its signature matches.
