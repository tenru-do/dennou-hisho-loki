"""Read-only ADB process observation. Never starts/stops apps or captures content."""
import argparse, datetime, json, re, subprocess, time
from pathlib import Path

TARGETS = {"glass": ("", "com.example.rokidkeyboardbridge"),
           "phone": ("", "com.example.rokidgeminisecretary")}

def total_pss(text):
    match = re.search(r"TOTAL PSS:\s*(\d+)", text)
    if not match:
        match = re.search(r"^\s*TOTAL\s+(\d+)\s", text, re.M)
    return int(match.group(1)) if match else None

def adb_read(adb, serial, *args):
    try:
        result = subprocess.run([adb, "-s", serial, *args], capture_output=True,
                                text=True, encoding="utf-8", errors="replace", timeout=12)
        return result.stdout if result.returncode == 0 else None
    except (subprocess.TimeoutExpired, OSError):
        return None

def observe(adb, name):
    serial, package = TARGETS[name]
    pid = adb_read(adb, serial, "shell", "pidof", package)
    if pid is None:
        reachable = adb_read(adb, serial, "get-state")
        return {"state": "not_running" if reachable and reachable.strip() == "device" else "unreachable"}
    pid = pid.strip()
    if not re.fullmatch(r"\d+", pid):
        # pidof reports nonzero when a process is absent; a separate reachability check
        # is intentionally required before classifying the absence.
        return {"state": "not_running"}
    memory = adb_read(adb, serial, "shell", "dumpsys", "meminfo", pid)
    value = total_pss(memory or "")
    return {"state": "running" if value is not None else "memory_unavailable",
            "pid": int(pid), "pssKiB": value}

def summarize(samples):
    result = {}
    for name in TARGETS:
        rows = [row[name] for row in samples]
        values = [row["pssKiB"] for row in rows if row.get("pssKiB") is not None]
        pids = [row["pid"] for row in rows if "pid" in row]
        result[name] = {"observations": len(rows), "memorySamples": len(values),
                        "unavailable": sum(row["state"] != "running" for row in rows),
                        "observedPidChanges": sum(a != b for a, b in zip(pids, pids[1:])),
                        "firstPssKiB": values[0] if values else None,
                        "lastPssKiB": values[-1] if values else None,
                        "maxPssKiB": max(values) if values else None}
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", required=True)
    parser.add_argument("--glass-serial", required=True)
    parser.add_argument("--phone-serial", required=True)
    parser.add_argument("--minutes", type=float, default=15)
    parser.add_argument("--interval", type=int, default=30)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not 0 < args.minutes <= 120 or not 5 <= args.interval <= 300:
        parser.error("minutes must be (0,120], interval must be [5,300] seconds")
    TARGETS["glass"] = (args.glass_serial, TARGETS["glass"][1])
    TARGETS["phone"] = (args.phone_serial, TARGETS["phone"][1])
    samples = []
    start = time.monotonic()
    deadline = start + args.minutes * 60
    args.output.parent.mkdir(parents=True, exist_ok=True)
    try:
        while True:
            sample = {"elapsedSeconds": round(time.monotonic() - start, 1)}
            for name in TARGETS:
                sample[name] = observe(args.adb, name)
            samples.append(sample)
            report = {"startedAt": datetime.datetime.now(datetime.timezone.utc).isoformat()
                      if not samples[:-1] else report["startedAt"],
                      "elapsedSeconds": round(time.monotonic() - start, 1),
                      "scope": "read-only process observation; no workload started; no crash-free guarantee",
                      "summary": summarize(samples), "samples": samples}
            temp = args.output.with_suffix(args.output.suffix + ".tmp")
            temp.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
            temp.replace(args.output)
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                break
            time.sleep(min(args.interval, remaining))
    except KeyboardInterrupt:
        print("Stopped; completed samples remain in the output file.")
    print(json.dumps(summarize(samples), ensure_ascii=False))

if __name__ == "__main__":
    main()
