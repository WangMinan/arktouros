package edu.npu.arktouros.service.otel.scheduled.h2;

import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.service.otel.scheduled.ScheduledJob;
import edu.npu.arktouros.service.otel.search.SearchService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : H2定时任务
 */
@Slf4j
public class H2ScheduledJob extends ScheduledJob {

    public H2ScheduledJob(SearchService searchService) {
        super(searchService);
        log.info("H2ScheduledJob initialize complete.");
    }

    @Override
    public void startJobs() {
        log.warn("Start jobs has not been implemented yet.");
    }

    @Override
    protected void rollOver() {
       log.warn("Rollover has not been implemented yet.");
    }

    @Override
    protected void simulateMetrics(Service service) {
        log.warn("Stimulate metrics has not been implemented yet.");
    }

    @Override
    protected void calculateThroughput(Service service) {
        log.warn("Calculate throughput has not been implemented yet.");
    }

    @Override
    protected void calculateResponseTime(Service service) {
        log.warn("Calculate response time has not been implemented yet.");
    }

    @Override
    protected void calculateErrorRate(Service service) {
        log.warn("Calculate error rate has not been implemented yet.");
    }
}
