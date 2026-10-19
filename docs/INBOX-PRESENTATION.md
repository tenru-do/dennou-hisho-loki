# Inbox presentation and resume (phone 213)

Read-only list/detail projections distinguish pending, registration review awaiting confirmation, expired review, completed, cancelled and registration-result-needs-check. Confirmation availability matches the existing ten-minute token window, including the original boundary and clock rollback checks. Underlying stored states, authorization tokens, calendar actions and review candidate data are unchanged. Private review/completion tokens and candidate payloads remain removed from list/detail responses. Presentation computes labels without writing the encrypted inbox.

The phone defaults to unfinished/result-needs-check cases and can switch to all cases. Details retain original request, subject, date text, conditions and saved conversation references. Closed/uncertain cases no longer show an invalid cancel action; deletion still requires explicit confirmation and does not remove calendar events.

Authenticated Web chat adds local stage filters and a title/subject/date/condition/request search. Result-needs-check cases sort first, followed by current and expired reviews. Completed/cancelled cases remain accessible via filters. Unknown backend states fail toward result-needs-check. Filtering does not fetch new external data or infer dates from vague natural language.

On both phone and Web, resuming a pending card now composes the existing explicit-ID confirmation request in the ordinary conversation input. It does not enqueue, submit or register anything. The user must press Send and then review registration content. Existing nonmatching draft text requires replacement confirmation; an unrelated draft attachment/editor source prevents resume until removed. Original card metadata stays on the authoritative phone and is reloaded by the existing glasses selection flow, rather than reconstructing dates from the displayed title. Uncertain-result cases do not offer resume/register.

Web clears cached inbox entries on disconnect. Late detail responses cannot repopulate disconnected or filtered-out rows. Counts/statuses reflect the last fetch and can be refreshed; calendar status remains authoritative for uncertain external writes.

Validation: 14 pure projection checks, two additional encrypted-store nonmutation/token-hiding checks, browser filters/search/compose guards, and isolated Android list/detail projection checks. No live Gemini request or actual calendar operation is needed for this change. These tests do not validate a new live model-generated registration conversation or human touch interaction.
