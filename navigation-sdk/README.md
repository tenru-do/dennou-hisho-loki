# Navigation SDK integration - staged, not enabled

This Android library is separate from the currently installed phone/glass APKs.
It is NOT yet referenced by the manual `tools/build-integration.ps1` build.
No SDK navigation, Cloud activation, credential change or billable request is
performed by building/testing this module.

## Implemented

- Navigation SDK 7.9.0 dependency and typed turn-by-turn receiver service.
- Immutable, session-only lane directions and SDK recommendation flags.
- Missing data is not inferred from a turn instruction. Rerouting/stopped/unknown
  states clear lanes. Data expires after five seconds using monotonic time.
- Receiver is non-exported and runs in the host app process. Attach only to the
  app's own Navigator; do not confuse its route with a Google Maps app route.
- Conservative quota policy: 10 destinations per Tokyo calendar day, 300 per
  month; each waypoint counts. Clock rollback is denied. Store reserves with a
  synchronous preferences commit and fails closed if storage is invalid.
- `SdkNavigationSession` now wires quota reservation to `Navigator.setDestinations`
  and starts guidance only after success and feed registration. These are local
  per-installation controls, not Cloud quotas or a zero-charge guarantee.
- The single-owner session rejects duplicate starts. Stop invalidates pending
  results; a delayed route success cannot restart guidance. If an SDK request
  never completes, the controller remains STOPPING and does not automatically
  issue another billable request. Host UI must report that state and handle SDK
  lifecycle recovery rather than silently retry.
- `:phone` adds a separate Gradle APK build using the existing phone sources plus
  this library. The existing manual build and manifests remain unchanged.
  The phone UI does NOT yet initialize/call this session, and there is no HUD feed
  transport yet. Including the SDK in an APK does not mean lane guidance is live.

## Before user activation

1. Validate the staged phone build's runtime/target SDK changes and add legal
   notices accessible to users. Keep the existing manual build.
2. Add explicit opt-in/setup UI, required Google terms, runtime location access,
   Navigator ownership/lifecycle, and SDK key initialization (never source keys).
3. Have the host retain the controller across UI recreation and route every SDK
   destination request through it. Do not issue requests on reconnection alone.
4. Bind and transport the feed with the SDK route identity; add HUD lane arrows
   with recommended directions distinguished. Clear immediately on stop/reroute
   and on transport timeout. Do not mix it into an unrelated Google Maps trip.
5. Obtain approval before Cloud SDK enablement/key restriction/billing changes.
6. Test real SDK navigation, missing lanes, rerouting, stop/restart, disconnect,
   background operation, and interaction with the existing map/signals.

## Build

Requires Android SDK platform 36, Java 17+ and Gradle. AGP is pinned to 8.13.2.
Use `gradle --no-daemon :assembleDebug :testDebugUnitTest :phone:assembleDebug`
from this directory with ANDROID_HOME set. This produces the AAR and a staged
phone APK (not activated or installed automatically).

Windows workspace helper: `tools/build-navigation-sdk.ps1 -Gradle <gradle.bat>`.
It mirrors the installed platform/build tools into ignored workspace build output.

2026-09-27 verification: Gradle 9.3.0 / AGP 8.13.2 resolved SDK 7.9.0,
compiled the actual typed receiver and produced the debug AAR. Eight policy/model
unit tests passed (zero failures/errors). The Windows JDK 21 ZipFS close diagnostic
remains, but command-line javac and Gradle both exit zero. No runtime receiver,
quota preferences persistence, real lane availability or HUD integration test has
been performed. This AAR does not bundle the SDK dependency; consume this Gradle
module, not just the AAR file.

Official references:
- https://developers.google.com/maps/documentation/navigation/android-sdk/tbt-feed
- https://developers.google.com/maps/documentation/navigation/android-sdk/android-studio-setup
- https://developers.google.com/maps/documentation/navigation/android-sdk/release-notes

Do not publish this stage as completed lane guidance.

## Third-stage deployment (2026-09-27)

- With explicit user approval, enabled Navigation SDK in `loki-maps` and added it
  to the existing dedicated key alongside Routes API and Map Tiles API. Verified
  the saved Android package and certificate restrictions were unchanged.
- Phone operation UI and SDK-to-HUD route/lane transport implemented. Fresh lane
  data expires after five seconds; SDK sessions do not inherit a previous Maps trip.
- Built with `-PenableNavigationSdk=true`: build succeeded, all 17 model/session
  tests passed, APK signature verified against the existing certificate.
- Installed phone and glasses APKs with `adb install -r`; both returned Success.
- Live SDK initialization, user terms flow, route acquisition, lane rendering and
  field navigation remain UNVERIFIED. Installation is not end-to-end validation.
- Commercial distribution still needs license-notice UI and release review.

## Second-stage verification (2026-09-27)

- Clean library build, 17 unit tests (zero failures/errors), and `:phone:assembleDebug`
  succeeded. The nine added tests cover quota-before-send, duplicate starts, stop
  during requests, revoked enablement, route/feed failure, duplicate completion,
  and stop exceptions. They use a fake engine, not live paid navigation.
- SDK calls in `SdkNavigationSession` compile against the actual 7.9.0 dependency.
- Staged APK: `phone/build/outputs/apk/debug/phone-debug.apk`, approximately 59 MB.
  APK signature verification passed with the existing debug certificate SHA-1
  `c876271534db14b3b17f27321ad420f2829cab6a`.
- Not installed. No SDK initialized and no destination requests made. Phone UI,
  SDK key/terms flow, HUD transport/rendering and real navigation remain pending.
- The Windows compiler workaround still emits ZipFS close diagnostics; clean
  compilation avoids stale class lookup. The build helper requests a module clean.
