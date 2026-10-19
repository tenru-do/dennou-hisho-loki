# Web text request tracking (glass 565 / phone 209)

New ordinary Web text submissions carry an opaque request ID from the phone queue to the glass. Identical bodies remain distinct requests. Legacy rows keep their previous, explicitly uncertain matching display. Image/video/generation requests are outside this tracking scope.

The authenticated local receipt endpoint persists request state before acknowledging the matching queue head. Execution is granted only on the first RECEIVED-to-RUNNING transition. Repeated acknowledgements cannot remove the next request. A lost execution-permission response fails closed; it does not replay the body. On glass restart or interruption, an already dispatched request becomes UNCONFIRMED. Only receipt metadata is retried. Terminal records remove the pending body and cannot regress.

IDs are not added to Gemini prompts. There are no new Android permissions or public unauthenticated routes. Ordinary conversation content and existing local history policies are unchanged. The intent-classification continuation preserves the request ID through asynchronous replay. A different generation interrupts tracking rather than attributing its answer to the previous request.

ANSWERED means the glass displayed an answer, not that a calendar/tool operation succeeded or that Web history synchronization completed. Local tool outcomes can remain uncertain. Manually resubmitting may duplicate an already completed operation; this is not an exactly-once remote-tool guarantee.

Web input allows 2400 characters; backend normal text allows 2800, within the existing 3000-character queue envelope bound. Reserved internal command prefixes remain rejected.

Validation: host policy regressions, Web ID-status fixtures and isolated on-device queue/receipt/restart tests. Live model quality and end-to-end completion must be reported separately. The first synthetic live attempt exposed an asynchronous continuation defect and was not automatically resent; original conversation history was restored.

Authorized additional live synthetic ordinary-answer test passed: one Gemini connection, matching ID at ANSWERED on both devices, original history restored. Diagnostic helper bypassed the separate intent-classifier request to enforce a single-connection budget; this is not a full live intent-classifier regression.

## Glass 566: inbox callback ownership

Inbox list display now finishes the matching tracked request as ANSWERED after the list text is displayed. This means the list is available, not that a selected item has been registered. Selecting an item starts a new action. Empty-list replies continue to finish normally.

Both semantic and legacy inbox callbacks capture their original request ID and generation. Stale callbacks are discarded before updating the answer or continuing calendar preparation. Completion uses the captured ID rather than a mutable global secretary ID. No extra API request, new permission, or user-content logging is added.

On-device fixture coverage includes matching and different IDs, old generations, completion invalidation, actual list text display and terminal-body removal. These tests use isolated preferences and do not call Gemini or register calendar events.

## Phone 210: pending retention and waiting age

The 100-entry Web receipt ledger removes oldest terminal or legacy entries first. Tracked unresolved entries are retained. If all 100 entries are unresolved, new submissions are rejected before enqueueing; existing requests are not evicted. Previously evicted records are not reconstructed. Duplicate detection remains bounded by retained records, not an indefinite exactly-once guarantee.

Web cards show elapsed time since the latest receipt transition (or original acceptance). After two minutes without a new transition they explicitly say completion is unconfirmed, advise checking history/connectivity, and do not resubmit. This is a display hint, not a timeout, cancellation, heartbeat or proof of execution failure. The original state remains visible and late completion is still accepted. Terminal results do not get waiting warnings. Age refreshes even when history is unchanged, while preserving focus on recovery buttons.

Validation adds eight host retention checks, two Android receipt/overflow checks and seven browser timing/focus checks. Synthetic device fixtures use isolated preferences and do not access Gemini or calendars.
