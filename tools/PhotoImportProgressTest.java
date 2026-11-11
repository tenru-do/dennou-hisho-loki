package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONObject;

public final class PhotoImportProgressTest {
    private static int checks;
    private static final long NOW = 1000000;
    private static final String BATCH = "00000000-0000-0000-0000-000000000001";
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static JSONObject request() throws Exception {
        return new JSONObject().put("id", "00000000-0000-0000-0000-000000000002")
                .put("createdAt", NOW - 1000).put("expiresAt", NOW + 599000).put("mode", "copy");
    }
    private static JSONObject row(String phase, int confirmed, int retained) throws Exception {
        return new JSONObject().put("kind", "photo").put("batchId", BATCH).put("phase", phase)
                .put("expected", 10).put("confirmed", confirmed).put("retained", retained)
                .put("startedAt", NOW - 500).put("updatedAt", NOW);
    }
    private static JSONObject workspace(JSONObject row) throws Exception {
        return new JSONObject().put("connected", true).put("transfers", new JSONArray().put(row));
    }
    public static void main(String[] args) throws Exception {
        JSONObject idle = PhotoImportProgress.view(new JSONObject(), new JSONObject(), NOW);
        check(!idle.getBoolean("active") && !idle.getBoolean("totalKnown"), "idle is not a zero-photo completion");
        check(idle.isNull("total") && idle.isNull("confirmed") && idle.isNull("remaining"), "unknown counts stay null");
        JSONObject queued = PhotoImportProgress.view(new JSONObject(), request(), NOW);
        check(queued.getString("state").equals("QUEUED") && queued.getBoolean("active"), "queued request can be observed");
        check(queued.isNull("remaining") && !queued.getString("label").contains("完了しました"), "queued is not saved");
        JSONObject accepted = request().put("acceptedAt", NOW - 100);
        check(PhotoImportProgress.view(new JSONObject(), accepted, NOW).getString("state").equals("ACCEPTED"), "accepted differs from started");
        accepted.put("acceptedAt", NOW - 40000);
        check(PhotoImportProgress.view(new JSONObject(), accepted, NOW).getString("state").equals("START_UNCONFIRMED"), "receipt alone does not prove start");
        check(PhotoImportProgress.view(new JSONObject(), request(), NOW + 600000).getString("state").equals("EXPIRED"), "request expires without success");
        check(PhotoImportProgress.view(new JSONObject(), request(), NOW - 2000).getString("state").equals("UNKNOWN"), "backward clock is unconfirmed");
        JSONObject running = PhotoImportProgress.view(workspace(row("RUNNING", 3, 2)), request(), NOW);
        check(running.getLong("total") == 10 && running.getLong("confirmed") == 3 && running.getLong("remaining") == 5, "confirmed, excluded and remaining are distinct");
        check(running.getBoolean("fresh") && running.getBoolean("active"), "fresh running transfer");
        check(running.getString("label").contains("コピー枚数には含めません"), "deleted receipt is not copied");
        JSONObject partialRow = row("COMPLETED", 3, 2).put("failed", 2).put("deferred", 1);
        JSONObject partial = PhotoImportProgress.view(workspace(partialRow), request(), NOW);
        check(partial.getString("state").equals("PARTIAL") && !partial.getBoolean("active"), "invalid completion is downgraded");
        check(partial.getLong("remaining") == 5 && partial.getLong("failed") == 2 && partial.getLong("deferred") == 1, "failure remains unconfirmed");
        JSONObject complete = PhotoImportProgress.view(workspace(row("COMPLETED", 8, 2)), request(), NOW);
        check(complete.getString("state").equals("COMPLETED") && complete.getLong("confirmed") == 8 && complete.getLong("remaining") == 0, "completion does not inflate copied count");
        JSONObject stale = PhotoImportProgress.view(workspace(row("RUNNING", 3, 0)).put("connected", false), request(), NOW);
        check(stale.getLong("confirmed") == 3 && !stale.getBoolean("fresh") && stale.getString("label").contains("前回確認"), "disconnect retains counts as stale");
        check(!PhotoImportProgress.view(workspace(row("RUNNING", 3, 0).put("updatedAt", NOW + 1)), request(), NOW).getBoolean("fresh"), "future clock never fresh");
        check(PhotoImportProgress.view(workspace(row("COMPLETED", 10, 0)), request().put("previousBatchId", BATCH), NOW).isNull("total"), "same old batch cannot complete new request");
        check(PhotoImportProgress.view(workspace(row("COMPLETED", 10, 0).put("startedAt", NOW - 1001)), request(), NOW).isNull("total"), "older batch cannot complete new request");
        JSONObject other = row("RUNNING", 1, 0).put("kind", "meeting");
        check(PhotoImportProgress.view(workspace(other), request(), NOW).isNull("total"), "meeting counts never leak into KEEP");
        JSONObject empty = row("COMPLETED", 0, 0).put("expected", 0);
        check(PhotoImportProgress.view(workspace(empty), request(), NOW).getBoolean("totalKnown"), "known empty differs from unknown");
        for (String phase : new String[]{"FAILED", "INTERRUPTED", "PARTIAL"}) {
            JSONObject result = PhotoImportProgress.view(workspace(row(phase, 3, 0)), request(), NOW);
            check(!result.getBoolean("active") && result.getLong("remaining") == 7, phase + " retains unresolved count");
        }
        JSONObject minimal = PhotoImportProgress.view(workspace(row("RUNNING", 1, 0)).put("health", "private-fixture"), request(), NOW);
        check(!minimal.has("health") && !minimal.has("request"), "endpoint only exposes transfer summary");
        System.out.println("PASS " + checks + " KEEP progress policy checks (synthetic JSON; no devices/photos/API)");
    }
}
