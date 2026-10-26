#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
docker cp examples/arktouros-demo.txt arktouros-all-in-one:/tmp/arktouros-demo.txt
docker exec arktouros-all-in-one python3 -c '
import json, os, pathlib, tempfile, time
target = pathlib.Path("/usr/local/arktouros-apm-api/input_logs/arktouros-demo.txt")
if target.exists() or target.with_name("ARKTOUROS-READ-" + target.name).exists():
    raise SystemExit("示例已导入或正在导入；为避免重复日志，本次不重复写入。")
source = pathlib.Path("/tmp/arktouros-demo.txt")
rows = [json.loads(line) for line in source.read_text(encoding="utf-8").splitlines() if line.strip()]
if not rows:
    raise SystemExit("示例文件为空，未导入。")
latest = max(row.get("timestamp", row.get("endTime", 0)) for row in rows)
offset = int(time.time()*1000) - 1000 - latest
for row in rows:
    for field in ("timestamp", "startTime", "endTime"):
        if field in row and row[field] >= 0:
            row[field] += offset
# Use the ignored prefix on the same filesystem for an atomic final rename.
fd, temporary = tempfile.mkstemp(prefix="ARKTOUROS-READ-pending-", dir=target.parent)
try:
    with os.fdopen(fd, "w", encoding="utf-8") as output:
        for row in rows:
            output.write(json.dumps(row, ensure_ascii=False) + "\n")
    os.replace(temporary, target)
finally:
    pathlib.Path(temporary).unlink(missing_ok=True)
source.unlink()
print("已提交 %d 条合成示例数据，请等待接收器导入。" % len(rows))
'