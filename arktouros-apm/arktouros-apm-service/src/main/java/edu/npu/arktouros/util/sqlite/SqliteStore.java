package edu.npu.arktouros.util.sqlite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.*;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SQLite owns telemetry; the existing H2 DataSource still owns the ingestion queues. */
public class SqliteStore implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    // The HTTP mapper converts nulls and numbers to strings; persistence must retain their types.
    private final ObjectMapper mapper = new ObjectMapper();

    public SqliteStore(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath().normalize();
        Files.createDirectories(absolutePath.getParent());
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setBusyTimeout(10000);
        SQLiteDataSource sqlite = new SQLiteDataSource(sqliteConfig);
        sqlite.setUrl("jdbc:sqlite:" + absolutePath);
        HikariConfig pool = new HikariConfig();
        pool.setDataSource(sqlite);
        pool.setMaximumPoolSize(4);
        pool.setMinimumIdle(1);
        pool.setPoolName("sqlite-telemetry");
        dataSource = new HikariDataSource(pool);
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        try {
            jdbc.getJdbcTemplate().execute("PRAGMA journal_mode=WAL");
            jdbc.getJdbcTemplate().execute("""
                    CREATE TABLE IF NOT EXISTS telemetry (
                        kind TEXT NOT NULL CHECK (kind IN ('service','span','log','metric')),
                        id TEXT NOT NULL,
                        payload TEXT NOT NULL CHECK (json_valid(payload)),
                        service_name TEXT AS (json_extract(payload, '$.serviceName')),
                        name TEXT AS (json_extract(payload, '$.name')),
                        namespace TEXT AS (json_extract(payload, '$.namespace')),
                        trace_id TEXT AS (json_extract(payload, '$.traceId')),
                        start_time INTEGER AS (json_extract(payload, '$.startTime')),
                        end_time INTEGER AS (json_extract(payload, '$.endTime')),
                        timestamp INTEGER AS (json_extract(payload, '$.timestamp')),
                        severity TEXT AS (json_extract(payload, '$.severityText')),
                        content TEXT AS (json_extract(payload, '$.content')),
                        metric_type TEXT AS (json_extract(payload, '$.metricType')),
                        PRIMARY KEY (kind, id)
                    )
                    """);
            for (String columns : List.of("kind,service_name,start_time", "kind,trace_id,start_time",
                    "kind,service_name,name,timestamp", "kind,timestamp", "kind,namespace,name")) {
                jdbc.getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS telemetry_"
                        + columns.replace(',', '_') + " ON telemetry (" + columns + ")");
            }
        } catch (RuntimeException e) {
            dataSource.close();
            throw e;
        }
    }

    public NamedParameterJdbcTemplate jdbc() {
        return jdbc;
    }

    public <T> T decode(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read stored " + type.getSimpleName(), e);
        }
    }

    public Metric decodeMetric(String json, String type) {
        return switch (MetricType.valueOf(type)) {
            case GAUGE -> decode(json, Gauge.class);
            case COUNTER -> decode(json, Counter.class);
            case HISTOGRAM -> decode(json, Histogram.class);
            case SUMMARY -> decode(json, Summary.class);
            default -> throw new IllegalStateException("Unsupported stored metric type: " + type);
        };
    }

    public void sink(Source source) throws IOException {
        String kind;
        String id = UUID.randomUUID().toString();
        String serviceName = null;
        switch (source) {
            case Service service -> {
                kind = "service";
                id = service.getId();
            }
            case Span span -> {
                kind = "span";
                id = span.getId();
                serviceName = span.getServiceName();
                if (span.getStartTime() == null || span.getEndTime() == null) {
                    throw new IOException("Span startTime and endTime are required");
                }
            }
            case Log log -> {
                kind = "log";
                serviceName = log.getServiceName();
            }
            case Metric metric -> {
                kind = "metric";
                serviceName = metric.getServiceName();
                if (!(metric instanceof Gauge || metric instanceof Counter
                        || metric instanceof Histogram || metric instanceof Summary)) {
                    throw new IOException("Unsupported metric: " + metric.getClass().getName());
                }
            }
            default -> throw new IOException("Unsupported source: " + source);
        }
        if (id == null || id.isBlank()) {
            throw new IOException("Service/Span id must not be empty");
        }
        Map<String, ?> document = Map.of("kind", kind, "id", id, "payload", mapper.writeValueAsString(source));
        Service service = serviceName == null || serviceName.isBlank() ? null : Service.builder()
                .name(serviceName).status(true).build();
        if (source instanceof Span span && service != null) {
            service.setStatus(span.getEndTime() != PersistentDataConstants.ERROR_SPAN_END_TIME);
            if (!service.isStatus()) {
                service.setTags(List.of(new Tag(PersistentDataConstants.LATEST_ERROR_SPAN_ID, span.getId())));
            }
        }
        String serviceJson = service == null ? null : mapper.writeValueAsString(service);
        transaction.executeWithoutResult(status -> {
            if (service != null) {
                jdbc.update("""
                        INSERT INTO telemetry (kind,id,payload) VALUES ('service',:id,:payload)
                        ON CONFLICT(kind,id) DO NOTHING
                        """, Map.of("id", service.getId(), "payload", serviceJson));
                if (source instanceof Span) {
                    jdbc.update("""
                            UPDATE telemetry SET payload = json_set(payload,
                                '$.status', json_extract(:payload,'$.status'),
                                '$.tags', json_extract(:payload,'$.tags'))
                            WHERE kind='service' AND id=:id
                            """, Map.of("id", service.getId(), "payload", serviceJson));
                }
            }
            jdbc.update("""
                    INSERT INTO telemetry (kind,id,payload) VALUES (:kind,:id,:payload)
                    ON CONFLICT(kind,id) DO UPDATE SET payload=excluded.payload
                    """, document);
        });
    }

    public void delete(String kind) {
        if (kind == null) {
            jdbc.getJdbcTemplate().update("DELETE FROM telemetry");
        } else if (List.of("log", "span", "metric").contains(kind)) {
            jdbc.update("DELETE FROM telemetry WHERE kind=:kind", Map.of("kind", kind));
        } else {
            throw new IllegalArgumentException("Unknown telemetry kind: " + kind);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
