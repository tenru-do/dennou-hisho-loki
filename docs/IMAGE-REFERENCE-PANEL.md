# Recent attachment reading panel (glass 567 / phone 211)

The phone conversation area and authenticated Web chat show the latest successfully analyzed attachment's question, reading time, attachment count, per-image reading sections and original answer. This is an available recent reading, not a declaration that every message uses it, a current camera feed, or proof of calendar registration. Video attachments remain sampled frames without audio. Numbering refers to the original numbered attachment sheet.

The glasses asynchronously copy question, answer, count and reading time to the paired phone over the existing authenticated bridge. Image bytes, capture location and API keys are not included. Failed local transfers retry at most once per minute in a separate worker, without blocking command polling or repeating model analysis. Changes to the referenced image during transfer are detected. Phone stores the copy using Android Keystore-backed AES-GCM and AtomicFile; older reading timestamps do not overwrite newer readings. This adds no Gemini request or Android permission.

The display expires after two hours from reading time; an expired encrypted copy is removed when inspected. It is a separate short-lived copy, so clearing conversation history on glasses does not immediately erase an already copied phone reading. If pairing is lost, the phone may show its last synchronized reading until expiry. Web disconnect clears rendered content. Never interpret this panel as authoritative live context selection.

For multiple attachments, only existing ordered 【画像N】 headings are separated. Missing headings are marked unconfirmed and the full original answer remains accessible. Duplicate or out-of-order headings are not silently interpreted. No extra AI extraction or invented field values are added.

Followup/correction buttons put an explicit numbered question prefix into the input field; they do not submit, change records or register calendar events. Existing draft text requires replacement confirmation. An unrelated draft attachment must be removed first. The phone panel is compact and opens details on demand to preserve space for conversation and keyboard.

Validation uses synthetic text only: 12 host parser/expiry checks, browser selection/escaping/clearing tests, and isolated on-device encryption/order tests. This does not revalidate visual model reading accuracy or guarantee that all responses contain numbered headings.

## Attachment kinds (glass 568 / phone 212)

New phone/Web submissions preserve a positional array of image/video/unknown types alongside the count. This array is encrypted in the pending image question and copied with the successful reading into the reference summary. A single glasses camera capture is image. Legacy records lacking types remain unknown, including mixed legacy sheets; no type is inferred from reading prose. Filenames, device paths and new media bytes are not added.

Count must match the array, at most six attachments and three videos are accepted, and arbitrary type strings are rejected before enqueueing. Reusing a request ID with altered types is rejected rather than relabelling or duplicating the queued request. Existing duplicates with identical metadata remain idempotent.

Phone and Web show the type per numbered attachment; video labels explicitly say eight sampled scenes and no audio. Followup drafts retain both the original sheet index and the source kind. Missing reading sections remain unconfirmed; type metadata does not prove analysis completeness. Existing model instructions and image processing are unchanged.

Additional checks cover order preservation, unknown legacy types, malformed arrays and video limits in both apps; Web payload serialization, typed followup drafts and video limitations; native encrypted packet readback, duplicate enqueue prevention and rejection of relabelled/mismatched metadata. Synthetic packet bytes in transport fixtures are not a visual-analysis accuracy test.
