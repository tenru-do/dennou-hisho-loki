# Navigation progress — 2026-09-23 (not published)

## Phone112 input validation
- Removed the local AIza/39-character validity assumption. Google response decides credential validity; only unsafe header characters and excessive size are rejected locally.
- Clipboard boundary whitespace/BOM/zero-width characters and balanced wrapping quotes are normalized; internal characters are not silently rewritten. Tile key query is URL-encoded.
- Pure Java normalization/header-safety tests passed. This does not establish why the user's actual clipboard content failed the earlier regex; do not claim it was an incorrect key based only on that check.

## Phone111 settings feedback correction
- Settings dialog no longer dismisses on Save/Check. Shows stored-state metadata and persistent last result without showing key contents.
- Invalid new input stays in the editor with its length and does not overwrite saved credentials. Credential storage now checks commit success and UI verifies decrypt/readback after a new save.
- Input remains intentionally blank when reopened. FLAG_SECURE prevents the key-entry dialog appearing in screenshots. API connection remains unverified; do not infer that UI correction fixed route integration.

## Latest verified update (glass338 / phone109)
- Follow-up phone110: diagnostic fixed to test the valid Android identity first and classify sanitized Google error reason codes. Real requests returned 401 CREDENTIALS_MISSING. A subsequent local-only shape validation confirmed the saved value is not the expected Google API key format; no value was logged. User must enter the actual key from Show key, not a key/project name. Restrictions unchanged, verified=false, navigation integration still blocked. Phone110 rebuilt/signed/installed with settings preserved.
- Both signed APKs built and installed with adb install -r, preserving app data.
- Operation mode: 96dp map, no 1.25x magnification (scale 1.0).
- Map mode: 180dp map/navigation panel anchored to the bottom of the root; header mascot/info hidden, controls remain at top, comments occupy the space above navigation. Restoring operation mode restores the header. The bottom placement is verified on an actual device screenshot (.build-integration/loki-bottom-map-qa.png); route still says unavailable.
- PC pairing has its own one-use /pc_pair window, distinct from glasses /pair. WinForms launcher UTF-8 BOM restored for Windows PowerShell 5.1; SmokeTest passes.
- Authenticated live custom-instruction test: 725 characters before/after, same-content POST /custom_edit acknowledged by glasses, content preserved. This does NOT establish original-backup merge completeness or different-content bidirectional editing.
- Topic collection split: news/event feeds only during the existing 07/12/17/21 slots (15-minute grace window; OS scheduling may delay/miss a slot). Permission/custom changes no longer force a news fetch. Playback rebuilds current calendar, recent mail notifications and Gmail unread count; news uses same-day cache and does not fetch feeds. Gmail bodies are not fetched by the label provider. Runtime playback freshness still needs validation.
- Google Maps credential dialog added under phone Operations. Key uses Android Keystore AES-GCM, not plaintext source/APK or chat. Explicit connection check uses public sample points, tests rejection with wrong Android identity and successful requests with actual signing identity. Failed checks never disable restrictions. No actual API test yet: user key entry/result pending.
- Map Tiles cloud quotas now verified: 2000/day, 120/min; 3D root and Street View 0/day. Routes remains 25/day, 5/min; matrix 0/day.
- Seven Java regression suites passed, including 1440 schedule cases, 13 ETA cases, 11 weather-place cases, map palette/modes, instruction priority, query policy and route geometry. This is not a substitute for field tests.
- Route integration and new expression images remain unfinished. No GitHub publication.

## Display modes
- NAV now opens a menu: compact operation mode (96dp), larger map mode (126dp), and the existing stop/resume operation.
- Compact is the default; choice persists in navigation_display preferences.
- Comment updates do not change the chosen map size. GPS centering is unchanged.
- Glass APK built and signature verified; palette and size tests passed; adb install -r succeeded on connected glasses. On-device visual/interaction validation remains outstanding.

## Routes: not fixed yet
- Phone still uses the existing OSM-based estimate, not Google Routes/Map Tiles APIs.
- routeReady explicitly excludes transit and requires a destination from the Maps notification, matching cached mode, and fresh route points. Missing destination or transit therefore produces no route line intentionally; removing those guards would risk showing a wrong route.
- Need implement an explicit Loki destination/travel-mode/route selection flow with official API results; Google Maps app selected routes cannot be obtained through notification text alone.

## Cloud setup from the preceding session
- Separate project loki-maps; Routes API and Map Tiles API enabled.
- Dedicated key restricted to those two APIs and phone package/signing certificate. Secret is not included here or in app sources.
- Routes ComputeRoutes: 25/day, 5/minute. ComputeRouteMatrix: 0/day.
- Project-only JPY 1 budget alert configured; an alert is NOT a billing hard stop.
- Map Tiles conservative quotas completed in latest update; app integration and credential rejection tests pending. Existing Gemini project was not changed.
- Earlier GOOGLE-MAPS-API-PLAN.md statement that no Cloud setup has occurred is superseded by this section, not by a claim that integration is complete.

## Other status
- User confirmed queued smartphone input works.
- Consumer Gemini conversation synchronization remains unimplemented. PC Loki client is not consumer Gemini synchronization.

## Google navigation integration (phone 113 / glasses 339, later update)
- Saved credential corrected and both real-app API success plus wrong-app rejection confirmed on phone before integration. Key material and diagnostic hashes are not stored here; temporary hash logging removed.
- Implemented Google Routes requests for WALK/BICYCLE/DRIVE/TRANSIT, encoded-polyline parsing and existing route geometry reduction; in-memory route cache only. No periodic eight-minute Google requests; destination/mode changes and existing deviation/recalculation signals trigger replacement with cooldown.
- Implemented phone-authenticated Google 2D tile bridge. API key and session token stay on phone, never sent to glasses. Native dark map styling, Google Maps attribution and tappable full viewport data credits. Google tiles bypass OSM edge processing and disk cache; memory reuse respects max-age/no-cache/no-store conservatively.
- App request ceilings: routes 20/day and 3/min; tiles 1800/day and 90/min; sessions 30/day and 2/min. These supplement Cloud quotas, not an absolute billing guarantee across other clients. Failures consume local allowance to prevent request storms.
- Never fall back to OSM tiles behind Google route data. Notification destination is still required to validate the current trip; API routes may differ from the route selected in the Google Maps app. Missing notification destination is not silently replaced with a previous trip.
- Polyline unit tests passed (official example coordinates, signed deltas, truncated/invalid input). Both APKs compile and signature verification completed before final version bump; final rebuild underway.
- Devices reconnected. Final phone 113 and glasses 339 APKs built, signatures verified, installed with settings preserved, and launched successfully. Phone package version verified. Glass transit poll confirms working phone communication but navigationActive=false; user was asked to start navigation. Actual Google tile/route rendering remains UNVERIFIED pending active navigation. No GitHub publication.
- References: https://developers.google.com/maps/documentation/tile/policies ; https://developers.google.com/maps/documentation/tile/session_tokens ; https://developers.google.com/maps/documentation/routes/compute_route_directions

### Transit walking access correction
- Live phone log confirmed a Google walking route (227 retained points); glasses confirmed nine Google tiles decoded/displayed at 17:55. Later transit notification had a walking title and departure-station detail, not a final destination. The destination-only guard hid route geometry.
- Added strict access-station extraction requiring walking instruction plus station name and departure clock. This is routeTargetKind=transit_access, not the final destination. Route geometry targets the access station in WALK mode; API walking-leg totals/arrival are excluded from final-trip metrics. Mode selection moved before route acquisition.
- Regression tests cover the observed notification shape, reject rail-as-walk, reject arrival-only text and prevent access station becoming final destination. Corrected phone installed; 18:00 route ready with 58 points in WALK mode, glass continuously reports routeReady=true. Device screenshot confirms the access route line drawn on Google tiles. Corrected glass additionally suppresses access-station name in final-destination row; station remains in next-stop row. Glass rebuilt, signature verified and installed.

### Signals and route overview (phone 114 / glasses 340)
- NAV menu now has compact, large follow, large acquired-route overview, and stop/resume. Overview is north-up, fits the acquired polyline plus current GPS with padding, and uses variable tile zoom. Access-only routes explicitly say walking-leg overview; no claim that a partial route covers the whole journey.
- Signal annotations use bounded asynchronous Overpass queries for OSM highway=traffic_signals, crossing=traffic_signals, crossing:signals=yes. Query area center is rounded to 0.002 degrees; exact GPS is not sent. 5-minute retry minimum, one-hour refresh, bounded response and node count, separate source attribution. Missing data never implies no signals; symbols do not represent signal phase or safe-to-cross status.
- Only local-area signals are queried; overview at zoom<15 suppresses signal clutter and labels that signals are visible when enlarged. Google Maps and OSM signal sources are distinguished in the map data-source dialog.
- RouteOverview tests passed short/long/zero-length routes and current-position-in-bounds. Both APKs built and signed, installed, launched. New phone log confirms live walking route generation. User asked to select NAV > large acquired-route overview for visual QA. GitHub unchanged.

### Navigation priority (glasses 341)
- Large-map active navigation pauses AMB microphone capture, processing, playback relay and voice-mode AMB supplement polling; aborts active capture/request and clears queued audio. AMB setting stays ON so leaving large navigation resumes processing. AMB results/status are suppressed while paused; button says AMB休.
- Large map controls anchored to bottom, map+guidance directly above with 24dp clearance. Ordinary layout restores top controls. Compact map reduced 96->82dp (~15%), keeping 96dp guidance row so navigation text does not lose height.
- Palette/mode-size regression passes. Final glasses 341 APK built/signature verified, installed and launched. Live log 18:21 confirms navigation ambientPaused=true. Device screenshot confirms no AMB card, bottom button row showing AMB休, map+guidance directly above buttons. Small-mode resume behavior remains to be exercised by the user.
