package edu.npu.arktouros.service.sinker.sqlite;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.service.sinker.SinkService;
import edu.npu.arktouros.util.sqlite.SqliteStore;
import org.springframework.dao.DataAccessException;

import java.io.IOException;

public class SqliteSinkService extends SinkService {
    private final SqliteStore store;

    public SqliteSinkService(SqliteStore store) {
        this.store = store;
    }

    @Override
    public void sink(Source source) throws IOException {
        try {
            store.sink(source);
        } catch (DataAccessException e) {
            throw new IOException("Failed to persist telemetry in SQLite", e);
        }
    }
}
