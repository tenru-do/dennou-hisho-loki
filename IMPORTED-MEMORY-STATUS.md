# Gemini reference memory — initial implementation

- Phone Operations: Gemini会話を取り込む・記憶を確認.
- Paste one user/assistant exchange; each input capped at 12,000 characters.
- Local extractive preview, user edits/confirms up to 600 characters. No AI summarization call.
- Raw pasted conversation is not persisted; only normalized exchange hash, reviewed summary, importance flag and import timestamp are stored in app-private preferences, separately from custom instructions.
- Exact normalized exchange deduplication, maximum 200 records; no silent eviction. Preview labels assistant statements unverified.
- Important flag boosts equally relevant matches; it does not cause unconditional inclusion.
- Authenticated lookup uses Japanese-compatible character bigram overlap; maximum two summaries and 1,000 summary characters per query. Glasses label these as non-instructional, unverified reference material.
- User can inspect/delete stored summaries. Import time is not treated as conversation time.
- Optional glass reference lookup now tries only the first known phone endpoint, without LAN discovery retries. Connection/read timeouts are 350/450 ms, response limited to 16 KiB with an elapsed-time guard. Failure skips external memory rather than aborting the conversation. Real disconnection timing remains unmeasured.

Validation: signed builds for phone and glasses; 8 tests passed on Android runtime covering identity boundaries, duplicate normalization, role labeling, relevance, empty query, size bound, importance ranking and three-character Japanese queries. Installed over existing apps preserving settings.

Live UI confirmed a saved user entry with the test word. Live authenticated retrieval exposed a short-query threshold defect; fixed by lowering the threshold only when the query has fewer than three bigrams. At 19:43, post-fix live retrieval returned one 10-character summary for the short test query and zero for unrelated queries. A queued 3-character phone question reached the glasses; log recorded `imported reference attached count=1` before the Gemini request. Phone history subsequently contained a new user turn and a 300-character Gemini response. This verifies retrieval-to-generation wiring, not semantic recall quality from a meaningful conversation or audible playback. No test memory was inserted/deleted; the existing user test entry was preserved.

PC pairing windows were incorrectly Activity-instance fields while the bridge server was static and survived recreation. Windows now share server lifetime, resolving immediate `pairing_closed` after a visible pairing action; live pairing succeeded after the update. Saved summaries support editing and changing importance, with expected-summary conflict protection.

Not completed: semantic retrieval evaluation with meaningful multi-turn history, audible playback verification, AI-generated summaries, per-turn delta detection within overlapping bulk histories, Takeout bulk parsing, automatic synchronization with personal Gemini, reading Gemini's pinned-chat state. Current importance flag is explicitly selected in Loki. No GitHub publication.

## Next work

1. Test a meaningful, user-approved memory through UI import, relevant/unrelated questions and deletion; do not use the existing one-word test as evidence of semantic accuracy.
2. Add optional AI summarization with explicit raw-text transmission disclosure and preview before persistence. Gemini credentials currently reside on glasses: avoid copying keys to the phone; use a bounded dedicated summary job, separate from spoken conversation and search prompts.
3. Deduplicate individual new turns before summarization; exact exchange identity currently only prevents identical full-exchange imports.
4. Test editing directly on glasses for custom-instruction synchronization; phone edit/ack/restore is already verified.
