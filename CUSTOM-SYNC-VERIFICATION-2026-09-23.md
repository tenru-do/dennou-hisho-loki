# Custom instruction verification — 2026-09-23

Private instruction bodies and credentials are intentionally excluded.

## Verified on connected phone and glasses

- Current instruction length: 725 characters.
- Glass-held pre-merge backups: glass 14 nonempty lines, phone 14 nonempty lines.
- Exact original lines absent from current instructions: zero for both backups.
- Authenticated PC pairing succeeded; user confirmed the new connection feedback appeared.
- Changed-content test through phone `/custom_edit`: temporary unique check line received and acknowledged by glasses (`pending=false`).
- Original 725-character content restored, then acknowledged by glasses. No temporary check line remains.
- Concurrent-edit protection uses expected-base comparison; test cleanup refuses to overwrite unrelated edits.

## Limits

- This establishes completeness relative to the saved migration backups, not versions predating those backups.
- This tests phone-side editing and the glass-to-phone acknowledgement path. Editing directly in the glasses settings UI remains unverified.
- PC pairing success does not mean instruction synchronization is complete; the UI now distinguishes those states.

## Personal Gemini integration research

Official Gemini API conversation state concerns API-created interactions. No documented interface for live bidirectional synchronization with personal Gemini Apps history/memory was located.

Google documents exporting Gemini Apps activity via Takeout (My Activity → Gemini Apps). An explicitly selected export could be imported as reference history, not executable instructions. This would not be live synchronization; export contents and supported format must be checked before implementation. Do not silently add imported conversations to custom instructions.

Sources:
- https://ai.google.dev/gemini-api/docs/interactions-overview
- https://support.google.com/gemini/answer/16920332?hl=en

No GitHub publication performed.
