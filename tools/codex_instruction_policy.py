"""Binding policy for glasses -> the visible Codex conversation.

The desktop UI collector and authenticated phone transport still need wiring.
No latest-thread fallback and no executable send command are provided here.
The supplied RPC must address the existing desktop app-server, not a new server.
"""
from dataclasses import dataclass
import re
import uuid


class Unavailable(Exception):
    pass


@dataclass(frozen=True)
class VisibleThread:
    thread_id: str
    title: str
    captured_at_ms: int
    window_id: str


@dataclass(frozen=True)
class BoundInstruction:
    request_id: str
    thread_id: str
    title: str
    text: str
    created_at_ms: int

    @property
    def announcement(self):
        return f"Codexの『{self.title}』へ指示を送ります。"


def bind_visible(snapshot: VisibleThread | None, text: str, now_ms: int, rpc) -> BoundInstruction:
    """Read fresh UI identity, validate metadata, then freeze the destination.

    Caller must speak announcement before queue_bound(). A later UI change does
    not change this request's destination. A new request requires a new capture.
    """
    if snapshot is None or not snapshot.window_id:
        raise Unavailable("Codexで開いている会話を確認できません。まだ送信していません。")
    if not 0 <= now_ms-snapshot.captured_at_ms <= 5_000:
        raise Unavailable("画面の会話情報が古いため、まだ送信していません。")
    try:
        uuid.UUID(snapshot.thread_id)
    except (ValueError, TypeError, AttributeError):
        raise Unavailable("送信先の会話IDを確認できません。") from None
    text = text.strip()
    if not text or len(text) > 3000 or "\x00" in text:
        raise Unavailable("指示は1～3000文字で指定してください。")
    root = rpc("thread/read", {"threadId":snapshot.thread_id, "includeTurns":False})
    thread = root.get("thread", {})
    title = str(thread.get("name") or "").strip()
    if (thread.get("id") != snapshot.thread_id or not title or title != snapshot.title
            or len(title) > 300 or re.search(r"[\x00-\x1f]", title)):
        raise Unavailable("画面の会話とCodexの送信先を照合できません。まだ送信していません。")
    return BoundInstruction(str(uuid.uuid4()), snapshot.thread_id, title, text, now_ms)


def queue_bound(request: BoundInstruction, now_ms: int, rpc):
    """Queue a genuine user input without starting a parallel worker or turn.

    Persist request_id before calling. If the connection fails after dispatch,
    report unknown outcome and reconcile with thread/queue/list; never retry
    automatically or announce success. PC approval handling stays with Codex.
    """
    if not 0 <= now_ms-request.created_at_ms <= 60_000:
        raise Unavailable("指示の送信期限が切れました。自動では再送しません。")
    return rpc("thread/queue/add", {
        "threadId": request.thread_id,
        "clientUserMessageId": request.request_id,
        "input": [{"type":"text", "text":request.text, "text_elements":[]}],
    })
