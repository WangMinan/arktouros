package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import lombok.Getter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author : [wangminan]
 * @description : 链路分析模块
 */
public class OtelTraceAnalyzer extends DataAnalyzer {

    private static BlockingQueue<ResourceSpans> queue;

    private static final int QUEUE_SIZE = 100;

    @Getter
    private static final OtelTraceAnalyzer instance = new OtelTraceAnalyzer();

    public OtelTraceAnalyzer() {
        queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    @Override
    public void run() {
        super.run();
    }

    public void handle(ResourceSpans resourceSpans) {

    }

    public void analyze() {

    }
}
