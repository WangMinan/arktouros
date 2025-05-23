package edu.npu.arktouros.service.scheduled.h2;

import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.service.search.SearchService;
import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : H2定时任务
 */
@Slf4j
public class H2ScheduledJob extends ScheduledJob {

    public H2ScheduledJob(SearchService searchService, SinkService sinkService) {
        super(searchService, sinkService);
        log.info("H2ScheduledJob initialize complete.");
    }

    @Override
    public void start() {
        log.warn("Start jobs has not been implemented yet.");
    }

    @Override
    public void stop() {
        log.warn("Stop has not been implemented yet.");
    }

    @Override
    protected void rollover() {
       log.warn("Rollover has not been implemented yet.");
    }

    @Override
    protected void simulateMetrics() {
        log.warn("Stimulate metrics has not been implemented yet.");
    }

    @Override
    protected void calculateThroughput() {
        log.warn("Calculate throughput has not been implemented yet.");
    }

    @Override
    protected void calculateResponseTime() {
        log.warn("Calculate response time has not been implemented yet.");
    }

    @Override
    protected void calculateErrorRate() {
        log.warn("Calculate error rate has not been implemented yet.");
    }
}
