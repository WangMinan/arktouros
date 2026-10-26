"""Generate deterministic synthetic telemetry for the offline receiver (no production data)."""
import json
from pathlib import Path


def generate():
    rows = []
    services = ["demo-gateway", "demo-order", "demo-payment", "demo-database"]
    base = 1792972800000  # 2026-10-26 00:00 UTC; import-example.sh shifts these timestamps.
    for i in range(30):
        trace = f"demo-trace-{i:02d}"
        start = base + i * 10000
        for j, service in enumerate(services):
            span_id = f"{trace}-span-{j}"
            duration = 250 if i == 20 and j == 2 else 10 + j * 5 + i % 5
            rows.append(dict(type="SPAN", id=span_id, name=f"{service}/request", serviceName=service,
                             traceId=trace, parentSpanId="" if j == 0 else f"{trace}-span-{j-1}",
                             root=j == 0, startTime=start + j * 30,
                             endTime=-1 if i == 15 and j == 2 else start + j * 30 + duration,
                             localEndPoint=dict(serviceName=service, ip=f"10.0.0.{j+1}", port=8000+j,
                                                latency=0, type="ENDPOINT"), remoteEndPoint=None,
                             tags=[dict(key="synthetic", value="true")]))
            for name, value in [("cpu_usage", 20 + j*5 + i % 10), ("memory_usage", 128 + j*32 + i*2)]:
                rows.append(dict(sourceType="METRIC", metricType="GAUGE", name=name, serviceName=service,
                                 description="Synthetic example, not a measured production metric",
                                 labels={"synthetic": "true"}, timestamp=start, value=value))
        for service in services[:2]:
            rows.append(dict(type="LOG", serviceName=service, traceId=trace, spanId=f"{trace}-span-0",
                             content=f"[合成示例] 请求 {i:02d} " + ("模拟失败" if i == 15 else "处理完成"),
                             severityText="ERROR" if i == 15 else "INFO", error=i == 15, timestamp=start,
                             tags=[dict(key="synthetic", value="true")]))
    assert len(rows) == 420
    assert len({r["serviceName"] for r in rows}) == 4
    ids = {r["id"] for r in rows if r.get("type") == "SPAN"}
    assert len(ids) == 120
    assert all(not r["parentSpanId"] or r["parentSpanId"] in ids for r in rows if r.get("type") == "SPAN")
    return rows


if __name__ == "__main__":
    destination = Path(__file__).parent / "examples/arktouros-demo.txt"
    destination.parent.mkdir(exist_ok=True)
    destination.write_text("".join(json.dumps(row, ensure_ascii=False) + "\n" for row in generate()), encoding="utf-8", newline="\n")
