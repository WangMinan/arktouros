package edu.npu.arktouros.analyzer.sytel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : 沈阳链路追踪遥测数据分析器
 */
@Slf4j
public class SytelTraceAnalyzer extends DataAnalyzer {

    protected static TraceQueueService queueService;

    private final SinkService sinkService;

    public SytelTraceAnalyzer(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    @Override
    public void init() {
        super.run();
        if (!queueService.isEmpty()) {
            // 打印告警 可能有otel的数据没有处理完，如果报错需要以otel格式重启处理完之后再调用沈阳格式
            log.warn("Trace data has remained in cache. Error could occur if remained data is in otel format.");
        }
        while (!isInterrupted()) {
            transform();
        }
    }

    public void setQueueService(TraceQueueService queueService) {
        SytelTraceAnalyzer.queueService = queueService;
    }

    public void transform() {

    }
}
