package edu.npu.arktouros.service.operation.sqlite;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.util.sqlite.SqliteStore;

public class SqliteOperationService extends DataOperationService {
    private final SqliteStore store;

    public SqliteOperationService(DataReceiver receiver, ScheduledJob scheduledJob, SqliteStore store) {
        super(receiver, scheduledJob);
        this.store = store;
    }

    // A single DELETE is atomic; ingestion can continue and subsequent events remain visible.
    @Override
    public void deleteAllData() {
        store.delete(null);
    }

    @Override
    public void deleteAllLogs() {
        store.delete("log");
    }

    @Override
    public void deleteAllSpans() {
        store.delete("span");
    }

    @Override
    public void deleteAllMetrics() {
        store.delete("metric");
    }
}
