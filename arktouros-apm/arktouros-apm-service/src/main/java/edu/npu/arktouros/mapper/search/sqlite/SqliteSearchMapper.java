package edu.npu.arktouros.mapper.search.sqlite;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.*;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.*;
import edu.npu.arktouros.util.StandardDeviationUtil;
import edu.npu.arktouros.util.sqlite.SqliteStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

public class SqliteSearchMapper extends SearchMapper {
    private final SqliteStore store;
    private final JdbcTemplate jdbc;

    public SqliteSearchMapper(SqliteStore store) {
        this.store = store;
        this.jdbc = store.jdbc().getJdbcTemplate();
    }

    @Override
    public R getServiceList(ServiceQueryDto dto) {
        Filter filter = new Filter("service").and("namespace=?", present(dto.namespace()) ? dto.namespace() : "default");
        if (present(dto.query())) filter.and("substr(name,1,length(?))=?", dto.query(), dto.query());
        long total = count(filter);
        List<Service> services = page(filter, Service.class, "name,id", dto.pageNum(), dto.pageSize());
        for (Service service : services) {
            List<Span> latest = read(new Filter("span").and("service_name=?", service.getName()),
                    Span.class, " ORDER BY start_time DESC,id LIMIT 1");
            if (!latest.isEmpty()) {
                Span span = latest.getFirst();
                if (span.getEndTime() >= span.getStartTime()) {
                    service.setLatency((int) Math.min(Integer.MAX_VALUE, span.getEndTime() - span.getStartTime()));
                }
            }
        }
        return result(new PageResultVo<>(total, services));
    }

    @Override
    public List<Service> getServiceListFromTopologyQuery(String namespace) {
        return read(new Filter("service").and("namespace=?", namespace), Service.class, " ORDER BY name,id");
    }

    @Override
    public List<Span> getSpanListByServiceNames(List<String> names) {
        if (names.isEmpty()) return new ArrayList<>();
        return read(new Filter("span").in("service_name", names), Span.class, " ORDER BY start_time,id");
    }

    @Override
    public Service getServiceByName(String name) {
        List<Service> services = read(new Filter("service").and("name=?", name), Service.class,
                " ORDER BY namespace,id LIMIT 1");
        return services.isEmpty() ? null : services.getFirst();
    }

    @Override
    public R getLogListByQuery(LogQueryDto dto) {
        Filter filter = new Filter("log");
        if (present(dto.serviceName())) {
            if ("null".equals(dto.serviceName())) filter.and("service_name IS NULL");
            else filter.and("service_name=?", dto.serviceName());
        }
        if (present(dto.traceId())) filter.and("trace_id=?", dto.traceId());
        // shortcut: literal whitespace-separated terms, use FTS if Elasticsearch linguistic analysis is required.
        if (present(dto.keyword())) {
            for (String word : dto.keyword().strip().split("\\s+")) {
                filter.and("instr(lower(coalesce(content,'')),lower(?))>0", word);
            }
        }
        if (present(dto.keywordNotIncluded())) {
            for (String word : dto.keywordNotIncluded().strip().split("\\s+")) {
                filter.and("instr(lower(coalesce(content,'')),lower(?))=0", word);
            }
        }
        if (present(dto.severityText())) {
            filter.and("severity=?", "null".equals(dto.severityText()) ? "" : dto.severityText());
        }
        filter.time("timestamp", "timestamp", dto.startTimestamp(), dto.endTimestamp());
        return result(new PageResultVo<>(count(filter), page(filter, Log.class, "timestamp DESC,id",
                dto.pageNum(), dto.pageSize())));
    }

    @Override
    public R getEndPointListByServiceName(EndPointQueryDto dto) {
        List<EndPointTraceIdVo> endpoints = getEndPointTraceIdVos(dto);
        long offset = offset(dto.pageNum(), dto.pageSize());
        return result(endpoints.stream().skip(offset).limit(dto.pageSize()).toList()).put("total", endpoints.size());
    }

    @Override
    public List<EndPointTraceIdVo> getEndPointTraceIdVos(EndPointQueryDto dto) {
        List<Span> spans = read(spanFilter(dto.serviceName(), dto.startTimestamp(), dto.endTimestamp()),
                Span.class, " ORDER BY start_time,id");
        Map<EndPoint, Set<String>> endpoints = new LinkedHashMap<>();
        for (Span span : spans) {
            endpoints.computeIfAbsent(span.getLocalEndPoint(), ignored -> new LinkedHashSet<>()).add(span.getTraceId());
        }
        return endpoints.entrySet().stream().map(e -> new EndPointTraceIdVo(e.getKey(), e.getValue())).toList();
    }

    @Override
    public List<Span> getSpanListBySpanNameAndServiceName(SpanTimesQueryDto dto) {
        return read(spanFilter(dto.serviceName(), dto.startTimestamp(), dto.endTimestamp()).and("name=?", dto.spanName()),
                Span.class, " ORDER BY start_time,id");
    }

    @Override
    public List<Span> getSpanListByTraceId(SpanTopologyQueryDto dto) {
        Filter filter = new Filter("span").and("trace_id=?", dto.traceId())
                .time("start_time", "end_time", dto.startTimestamp(), dto.endTimestamp());
        if (dto.innerService()) filter.and("service_name=?", dto.serviceName());
        return read(filter, Span.class, " ORDER BY start_time,id");
    }

    @Override
    public List<String> getMetricsNames(String serviceName, Integer limit) {
        if (limit != null && limit < 0) throw new IllegalArgumentException("metricNameLimit must not be negative");
        return jdbc.queryForList("""
                SELECT DISTINCT name FROM telemetry WHERE kind='metric' AND service_name=? AND name IS NOT NULL
                ORDER BY name LIMIT ?
                """, String.class, serviceName, limit == null ? -1 : limit);
    }

    @Override
    public List<Metric> getMetricsValues(List<String> names, String serviceName, Long start, Long end) {
        if (names.isEmpty()) return new ArrayList<>();
        Filter filter = new Filter("metric").and("service_name=?", serviceName).in("name", names)
                .time("timestamp", "timestamp", start, end);
        String sql = "SELECT payload,metric_type FROM telemetry" + filter.where;
        if (start != null && end != null && end - start >= 3600000L) {
            String bucket = end - start <= 86400000L ? "strftime('%Y-%m-%d %H',timestamp/1000,'unixepoch')"
                    : end - start <= 2592000000L ? "strftime('%Y-%m-%d',timestamp/1000,'unixepoch')"
                    : "strftime('%Y',timestamp/1000,'unixepoch')";
            sql = "SELECT payload,metric_type FROM (SELECT payload,metric_type,timestamp,id,"
                    + "row_number() OVER (PARTITION BY metric_type,name," + bucket
                    + " ORDER BY timestamp,id) AS sample FROM telemetry" + filter.where + ") WHERE sample=1";
        }
        return jdbc.query(sql + " ORDER BY timestamp,id",
                (rs, row) -> store.decodeMetric(rs.getString("payload"), rs.getString("metric_type")), filter.args());
    }

    @Override
    public R getNamespaceList(String query) {
        return distinct(new Filter("service"), "namespace", query);
    }

    @Override
    public R getAllLogLevels(String query) {
        return distinct(new Filter("log"), "severity", query);
    }

    @Override
    public List<Service> getAllServices() {
        return read(new Filter("service"), Service.class, " ORDER BY name,id");
    }

    @Override
    public int getTraceCount(Service service, long start, long end) {
        Filter filter = new Filter("span").and("service_name=?", service.getName())
                .time("start_time", "start_time", start, end);
        return Math.toIntExact(jdbc.queryForObject("SELECT count(DISTINCT trace_id) FROM telemetry" + filter.where,
                Long.class, filter.args()));
    }

    @Override
    public List<Span> getSpanListByTraceId(String serviceName, String traceId, long start, long end) {
        return read(spanFilter(serviceName, start, end).and("trace_id=?", traceId), Span.class, " ORDER BY start_time,id");
    }

    @Override
    public List<Span> getAllSpans(Service service, long startTime) {
        return read(spanFilter(service.getName(), startTime, null), Span.class, " ORDER BY start_time,id");
    }

    @Override
    public R getSpanNamesByServiceName(SpanNamesQueryDto dto) {
        return distinct(spanFilter(dto.serviceName(), dto.startTimestamp(), dto.endTimestamp()), "name", null);
    }

    @Override
    public SpanTimesVo getSpanTimesVoBySpanName(SpanTimesQueryDto dto) {
        List<Span> spans = getSpanListBySpanNameAndServiceName(dto);
        StandardDeviationUtil.markLongDurationSpansBatch(this, spans);
        SpanTimesVo result = new SpanTimesVo();
        spans.forEach(result::addSpan);
        return result;
    }

    @Override
    public R getTimeRange() {
        return result(jdbc.queryForObject("SELECT min(start_time),max(end_time) FROM telemetry WHERE kind='span'",
                (rs, row) -> {
                    long min = rs.getLong(1);
                    Long start = rs.wasNull() ? null : min;
                    long max = rs.getLong(2);
                    return new NamespaceTopoTimeRangeVo(start, rs.wasNull() ? null : max);
                }));
    }

    private R distinct(Filter filter, String field, String prefix) {
        filter.and(field + " IS NOT NULL");
        if (present(prefix)) filter.and("substr(" + field + ",1,length(?))=?", prefix, prefix);
        return result(jdbc.queryForList("SELECT DISTINCT " + field + " FROM telemetry" + filter.where
                + " ORDER BY " + field, String.class, filter.args()));
    }

    private <T> List<T> read(Filter filter, Class<T> type, String suffix) {
        return jdbc.query("SELECT payload FROM telemetry" + filter.where + suffix,
                (rs, row) -> store.decode(rs.getString(1), type), filter.args());
    }

    private <T> List<T> page(Filter filter, Class<T> type, String order, int page, int size) {
        long offset = offset(page, size);
        filter.values.add(size);
        filter.values.add(offset);
        return read(filter, type, " ORDER BY " + order + " LIMIT ? OFFSET ?");
    }

    private long count(Filter filter) {
        return jdbc.queryForObject("SELECT count(*) FROM telemetry" + filter.where, Long.class, filter.args());
    }

    private static long offset(int page, int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("pageNum and pageSize must be positive");
        return (long) (page - 1) * size;
    }

    private static Filter spanFilter(String serviceName, Long start, Long end) {
        return new Filter("span").and("service_name=?", serviceName).time("start_time", "end_time", start, end);
    }

    private static boolean present(String value) {
        return value != null && !value.isEmpty();
    }

    private static R result(Object value) {
        return R.ok().put("result", value);
    }

    private static class Filter {
        private final StringBuilder where = new StringBuilder(" WHERE kind=?");
        private final List<Object> values = new ArrayList<>();

        Filter(String kind) {
            values.add(kind);
        }

        Filter and(String condition, Object... args) {
            where.append(" AND ").append(condition);
            values.addAll(Arrays.asList(args));
            return this;
        }

        Filter time(String startColumn, String endColumn, Long start, Long end) {
            if (start != null) and(startColumn + ">=?", start);
            if (end != null) and(endColumn + "<=?", end);
            return this;
        }

        Filter in(String field, List<String> items) {
            return and(field + " IN (" + String.join(",", Collections.nCopies(items.size(), "?")) + ")", items.toArray());
        }

        Object[] args() {
            return values.toArray();
        }
    }
}
