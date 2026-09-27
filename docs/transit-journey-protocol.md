# Transit journey extension (v1)

The phone's existing `transit` response and legacy `route` remain compatible.
Only a validated Routes TRANSIT cache adds `journey`; Navigation SDK responses
return through their existing branch and never acquire this extension.

- `version: 1`, `id`: snapshot identity; changes after successful route replacement.
- `finalDestination`, `fetchedAt`, `legCount`.
- `wholeRoute`: ordered `[latitude, longitude]` points for the full itinerary.
- `currentLegIndex`: -1 until a leg is selected.
- `positionConfirmed`: a sufficiently recent GPS sample matches the selected leg;
  this is not proof of boarding. `selectionSource` is `gps_estimate`.
- `currentLeg`: optional index, kind (ACCESS/RIDE/TRANSFER/EGRESS/WALK), API
  travelMode, route points, instruction, departureStop/arrivalStop,
  departureTime/arrivalTime (API timestamps), lineName and transitDetails.

The phone retains all legs. Only the full geometry and selected leg are sent.
GPS samples older than 30 seconds or accuracy worse than 100 metres are not
used to confirm a leg. A changed candidate requires distinct samples spanning
5 seconds. A 20-metre hysteresis reduces boundary toggles. Clock schedules alone
never advance the itinerary. Parallel/overlapping routes and missed transfers
remain limitations of GPS-only estimation and require real-world validation.

Glasses use wholeRoute for overview, currentLeg.route otherwise; changing map
mode swaps the locally cached geometry immediately. Unknown position produces
a checking message, not a fabricated boarding/turn instruction. Ride legs show
line, alighting stop and scheduled arrival (currently Japan time). Independent
Google Maps notification turns/ETA are not mixed into this API itinerary.
Empty leg geometry is not replaced by a straight line. Invalid versions and
legacy/SDK responses keep the original path.

No live service-disruption feed or guaranteed Google Maps app route matching is
provided by this extension. Unit fixtures and APK builds are not live transit tests.
