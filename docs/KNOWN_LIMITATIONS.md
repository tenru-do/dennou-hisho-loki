# Known limitations

This project is still an experimental prototype and needs more tuning before it can be considered stable.

It should be treated as an alpha developer preview, not as a finished or supported product.

See also:

- `../RELEASE_NOTES.md`

## Gemini API quota and billing

The glasses app uses the Gemini API directly from the app. If the API key has no billing/quota available, or if prompts are sent too quickly, Gemini may return frequent `429` errors.

In practice, users should expect to need:

- a valid Gemini API key
- billing/quota enabled where required by the selected Gemini model
- conservative pacing between requests
- a lower-cost/faster model for daily use
- additional retry and cooldown tuning

The app includes local cooldown handling for `429` and `503`, but this is only a mitigation. It does not remove Gemini-side rate limits.

## Still needs tuning

Areas that still need refinement:

- voice input reliability
- phone-to-glasses network discovery
- outdoor/tethering behavior
- Gemini model selection
- long prompt handling
- proactive AI behavior
- calendar search ranking
- mail notification summarization
- Loki Topic news/event selection and station inference
- ambient microphone/playback capture reliability and battery use
- navigation notification parsing, road-name completeness, and route refresh
- Gradle/reproducible build setup

Use this repository as a research prototype, not as a finished product.

## Not guaranteed across environments

The original prototype was tuned in one developer environment. It may not work correctly on other Rokid glasses, other Rokid OS versions, other Android phones, or other Wi-Fi/tethering setups.

Known environment-dependent areas include:

- ADB install/debug access
- Rokid launcher and input behavior
- phone-to-glasses local network reachability
- Android speech recognition behavior
- Android calendar provider sync state
- notification listener behavior for mail summaries
- Gemini model availability, billing state, and quota
- Android media-projection/playback-capture support and source-app DRM policy
- Google Maps notification wording and OpenStreetMap road/intersection coverage
- destination remaining time is minute-based and depends on a fresh Google Maps arrival clock or usable GPS/route progress; moving-route behavior still needs testing across devices

## Google Workspace scope

This project does not currently provide full Google Workspace API integration.

Calendar lookup reads events exposed through the Android Calendar Provider on the companion phone. Mail summary support reads recent Android notifications when notification access is granted. If the Gmail label permission is granted, the app can also read the inbox unread count, but not message subjects or bodies through that provider.

It does not guarantee complete Gmail search, complete Workspace search, Drive access, server-side OAuth integration, or cross-device consistency.

## No production-use guarantee

Do not use this project for safety-critical, medical, legal, financial, emergency, or production workflows.

AI responses may be wrong, incomplete, delayed, or unavailable. Calendar/mail results may be incomplete or mis-ranked. Network and Gemini API errors are expected during experimentation.
