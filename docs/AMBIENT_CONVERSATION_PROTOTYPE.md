# AMBIENT conversation assistant prototype

This experimental branch is isolated from the current public Loki release in
Git, but its APK intentionally uses the existing package
`com.example.rokidkeyboardbridge`. Installing it with an update operation keeps
the existing app preferences, including the API key and bridge settings.

## Behavior

- Pressing `AMB` while off opens an input selector: glasses microphone,
  Bluetooth playback, or both. The last choice is remembered. Pressing `AMB`
  while active turns the selected capture mode off.
- When both sources are selected, the glasses microphone and capturable Android
  playback audio are recorded in separate, concurrent, bounded 5.5–9 second PCM
  windows. Single-source modes start only the selected recorder.
- Android playback capture is used for media being played to Bluetooth. Android
  shows a system consent screen the first time `AMB` is enabled in a session.
- Both audio sources are queued in memory, while transcription and Gemini
  requests are always processed one at a time. Captured playback is prioritized
  and matching microphone echo is suppressed for 15 seconds.
- PCM is sent to the paired phone's local `/stt` endpoint for transcription.
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

Audio is not uploaded directly to Gemini. The paired phone's Android speech
recognition service receives temporary PCM from either the glasses microphone
or capturable playback, and the resulting transcript excerpt is sent to Gemini.
Therefore this mode still performs cloud processing and must only be enabled
where recording, playback capture, and transcription are appropriate. Turning
`AMB` off stops both recorders and releases Android's playback-capture session.

## Current limitations

- Only Android playback from apps that permit audio capture can be received.
  DRM-protected media, calls, protected apps, and some vendor players may be silent.
- "Bluetooth" identifies capturable app playback routed by the glasses; it does
  not intercept the Bluetooth radio stream or bypass Android capture policy.
- Simultaneous microphone and playback capture depends on the Rokid firmware's
  audio policy and still needs real-device validation.
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
