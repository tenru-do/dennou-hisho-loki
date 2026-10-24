# Navigation interaction checks

This suite runs the shipped glasses Activity entry points, real Android dialogs and key listeners under a separate, permission-free Android UID. Bridge responses, route choices, places and operation results are synthetic. It never starts the user's Loki Activity, route APIs, recording or playback. The temporary UI host is removed afterward.

Build both candidate APKs with tools/build-integration.ps1 -App glass/phone -Development, then run tools/Verify-NavigationInteractions.py --output <new-output-directory>. An authorized test phone must be connected. No configured API key is used.

The interaction suite covers registered places, walking/bicycle/car starts, shared transit choices, saved/outbound/return routes, next leg, missed-ride retry, destination/stopover/alternative changes, notification settings, landmarks, boarding/alighting/pause/resume/arrival actions, cancellation, empty/malformed responses, stale contexts, duplicate decisions, response loss, timeouts, and actual polling failure/recovery while a dialog is open. Phone native preview timeout handling is checked with real Android dialogs. Driving/walking/transit progression is also covered in the companion regression suite.

This replaces neither real-world movement checks nor provider availability testing. The synthetic dependency is in-process only: no intent, preference or HTTP endpoint enables it in the production app. All ordinary runtime instances use the actual Activity and authenticated bridge.

A test pass must match the exact APK hashes that are installed. Do not report assertion counts as end-to-end coverage, and do not call an untested live route verified.

Additional coverage (v633/v278): native phone shared-route selection/start/cancel in four modes, saved-leg decisions, delayed results, changed inputs, stale decisions after completed commits, explicit unknown commit outcome, cleanup in each preview phase, and actual Android test-host recreation. Three component tests check concurrent start claims and competing snapshot/generation invalidation. Host recreation calls the same cleanup helper used by production onDestroy; it is not a full launch/recreation of the user app. The provider seam is process-local and cannot be enabled by user settings or endpoints. Two physical devices communicating, live providers, live GPS, lock-screen behavior and audible output remain separate checks.
