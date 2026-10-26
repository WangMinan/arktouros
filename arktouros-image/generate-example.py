"""Deterministic synthetic checkout traces; package-install.py refreshes timestamps."""
import json
import math
from collections import Counter
from pathlib import Path

# key, parent, service, operation, offset and duration in milliseconds
nodes = [
    ('gateway', '', 'gateway', 'POST /checkout', 0, 3600),
    ('auth', 'gateway', 'auth', 'auth.verify', 20, 500),
    ('session', 'auth', 'cache', 'cache.session.get', 50, 35),
    ('user', 'auth', 'user', 'user.profile', 100, 350),
    ('profile-cache', 'user', 'cache', 'cache.profile.get', 125, 30),
    ('user-db', 'user', 'database', 'db.user.select', 180, 180),
    ('order', 'gateway', 'order', 'order.create', 550, 2600),
    ('validate', 'order', 'order', 'order.validate', 580, 1450),
    ('inventory', 'validate', 'inventory', 'inventory.reserve', 610, 450),
    ('stock-cache', 'inventory', 'cache', 'cache.stock.get', 625, 40),
    ('stock-db', 'inventory', 'database', 'db.stock.update', 700, 320),
    ('pricing', 'validate', 'pricing', 'pricing.calculate', 1080, 350),
    ('price-cache', 'pricing', 'cache', 'cache.price.get', 1100, 45),
    ('coupon', 'pricing', 'coupon', 'coupon.validate', 1170, 180),
    ('coupon-db', 'coupon', 'database', 'db.coupon.select', 1210, 90),
    ('payment', 'order', 'payment', 'payment.charge', 2100, 850),
    ('risk', 'payment', 'risk', 'risk.evaluate', 2120, 190),
    ('risk-cache', 'risk', 'cache', 'cache.risk.rules', 2140, 25),
    ('history-db', 'risk', 'database', 'db.payment.history', 2180, 110),
    ('attempt-1', 'payment', 'payment', 'payment.provider.attempt1', 2350, 220),
    ('attempt-2', 'payment', 'payment', 'payment.provider.retry', 2610, 270),
    ('order-db', 'order', 'database', 'db.order.insert', 3000, 120),
    ('recommendation', 'gateway', 'recommendation', 'recommendation.related', 30, 420),
    ('recommendation-cache', 'recommendation', 'cache', 'cache.recommendation.get', 50, 35),
    ('catalog', 'recommendation', 'catalog', 'catalog.products', 110, 310),
    ('products-db', 'catalog', 'database', 'db.products.select', 150, 230),
    ('notification', 'gateway', 'notification', 'notification.enqueue', 3200, 300),
    ('queue', 'notification', 'queue', 'queue.order-events.publish', 3230, 60),
    ('audit', 'notification', 'audit', 'audit.record', 3330, 140),
    ('audit-db', 'audit', 'database', 'db.audit.insert', 3350, 85),
]
services = sorted({n[2] for n in nodes})
service_index = {name: i for i, name in enumerate(services)}


def endpoint(service):
    index = service_index[service]
    return dict(serviceName='demo-' + service, ip=f'10.20.0.{index+10}',
                port=8000+index, latency=0, type='ENDPOINT')


def generate(base=1792972800000):
    rows = []
    all_spans = []
    for i in range(30):
        trace_id = f'checkout-tree-{i:02d}'
        retry = i in (12, 24, 29)
        slow = i in (20, 29)
        order_id = f'SYNTHETIC-ORDER-{i:04d}'
        trace_spans = []
        for key, parent, service, operation, offset, duration in nodes:
            if key == 'attempt-2' and not retry:
                continue
            start = base + i*12000 + offset
            duration += i % 5
            if key == 'pricing' and slow:
                duration += 500
            error = key == 'attempt-1' and retry
            span_id = f'{trace_id}:{key}'
            tags = [dict(key='synthetic', value='true'), dict(key='order.id', value=order_id),
                    dict(key='scenario', value='slow-and-retry' if slow and retry else 'retry' if retry else 'slow' if slow else 'normal')]
            if key.startswith('attempt-'):
                tags.append(dict(key='retry.attempt', value=key[-1]))
            if error:
                tags.append(dict(key='error.message', value='Synthetic provider timeout; next attempt succeeds'))
            span = dict(type='SPAN', id=span_id, name=operation, serviceName='demo-' + service,
                        traceId=trace_id, parentSpanId=f'{trace_id}:{parent}' if parent else '', root=not parent,
                        startTime=start, endTime=-1 if error else start+duration,
                        localEndPoint=endpoint(service), remoteEndPoint=None, tags=tags)
            rows.append(span)
            trace_spans.append(span)
            rows.append(dict(type='LOG', serviceName='demo-' + service, traceId=trace_id, spanId=span_id,
                             content=f'[合成树状调用] {order_id} {operation} ' + ('模拟超时，将重试' if error else f'完成，耗时 {duration}ms'),
                             timestamp=start+(duration if not error else 220), severityText='ERROR' if error else 'WARN' if slow and key == 'pricing' else 'INFO',
                             error=error, tags=tags))
        lookup = {s['id']: s for s in trace_spans}
        assert sum(s['root'] for s in trace_spans) == 1
        assert len(lookup) == len(trace_spans)
        for span in trace_spans:
            visited = {span['id']}
            cursor = span
            while cursor['parentSpanId']:
                cursor = lookup[cursor['parentSpanId']]
                assert cursor['id'] not in visited
                visited.add(cursor['id'])
            if span['parentSpanId'] and span['endTime'] != -1:
                parent = lookup[span['parentSpanId']]
                assert parent['startTime'] <= span['startTime'] <= span['endTime'] <= parent['endTime'], span
        assert Counter(s['parentSpanId'] for s in trace_spans)[trace_id + ':gateway'] == 4
        all_spans.extend(trace_spans)
        for service in services:
            k = service_index[service]
            for metric_name, value in [('cpu_usage', round(20 + k*2 + 8*math.sin(i/3+k), 2)),
                                       ('memory_usage', round(128 + k*16 + i*1.5, 2))]:
                rows.append(dict(sourceType='METRIC', metricType='GAUGE', name=metric_name,
                                 serviceName='demo-'+service, description='Synthetic checkout workload',
                                 labels={'synthetic':'true'}, timestamp=base+i*12000, value=value))

    counts = dict(Counter(r.get('type', r.get('sourceType')) for r in rows))
    assert len(services) == 16 and len(all_spans) == 873
    assert len({s['id'] for s in all_spans}) == len(all_spans)
    assert counts == {'SPAN': 873, 'LOG': 873, 'METRIC': 960}
    assert len(rows) == 2706
    return rows


if __name__ == "__main__":
    destination = Path(__file__).parent / "examples/arktouros-demo.txt"
    destination.parent.mkdir(exist_ok=True)
    destination.write_text("".join(json.dumps(row, ensure_ascii=False) + "\n" for row in generate()), encoding="utf-8", newline="\n")
