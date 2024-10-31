package edu.npu.arktouros.receiver.file.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.analyzer.sytel.SytelTraceAnalyzer;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author : [wangminan]
 * @description : Json日志预处理器
 */
@Slf4j
public class JsonFilePreHandler implements Runnable{
    // 阻塞队列 不需要考虑并发问题 用synchronize或者lock画蛇添足会导致线程阻塞
    private final ArrayBlockingQueue<String> inputCache;
    private final StringBuilder cacheStringBuilder = new StringBuilder();
    private final String fileType;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;
    private final SytelTraceAnalyzer sytelTraceAnalyzer;

    public JsonFilePreHandler(ArrayBlockingQueue<String> inputCache, String fileType,
                              TraceQueueService traceQueueService,
                              SinkService sinkService, ObjectMapper objectMapper) {
        this.inputCache = inputCache;
        this.fileType = fileType;
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
        this.sytelTraceAnalyzer = new SytelTraceAnalyzer(sinkService, objectMapper);
        sytelTraceAnalyzer.setQueueService(traceQueueService);
    }

    @Override
    public void run() {
        log.info("JsonFilePreHandler start working");
        while (true) {
            try {
                handle();
            } catch (InterruptedException | IOException e) {
                log.error("JsonFilePreHandler run failed", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void handle() throws InterruptedException, IOException {
        log.debug("Formatting input from cache.");
        String input =
                cacheStringBuilder.append(inputCache.take().trim()).toString();
        // 如果是sytel的格式 要把前导[]和其中的内容去掉 否则直接处理JSON
        if (fileType.equalsIgnoreCase("sytel")) {
            fixInput(input, '[', ']');
        }

        if (!input.startsWith("{")) {
            throw new IllegalArgumentException("Invalid input for json when handling: " + input);
        }

        fixInput(input, '{', '}');
    }

    private void fixInput(String input, char prefix, char suffix) throws IOException {
        // 开始做大括号匹配 匹配部分扔出去 剩下的放cache里
        Stack<Character> stack = new Stack<>();
        boolean isInStrFlag = false; // 游标是否正在字符串中
        int lastPos = 0;
        int currentPos;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                isInStrFlag = !isInStrFlag;
            } else if (c == prefix && !isInStrFlag) {
                stack.push(prefix);
            } else if (c == suffix && !isInStrFlag) {
                stack.pop();
                if (stack.isEmpty()) {
                    currentPos = i;
                    log.debug("Outputting formatted json to cache.");
                    persistInput(cacheStringBuilder.substring(0, currentPos - lastPos + 1));
                    cacheStringBuilder.delete(0, currentPos - lastPos + 1);
                    lastPos = currentPos + 1;
                }
            }
        }
    }

    private void persistInput(String substring) throws IOException {
        if (fileType.equalsIgnoreCase("arktouros")) {
            persistArktouros(substring);
        } else if (fileType.equals("otel")) {
            persistOtel(substring);
        } else if (fileType.equals("sytel")) {
            SytelTraceAnalyzer.handle(substring);
        } else {
            throw new IllegalArgumentException("Invalid file type: " + fileType);
        }
    }

    private void persistOtel(String substring) {
        throw new ArktourosException("Not implemented yet. Please use default arktouros-collector.");
    }

    private void persistArktouros(String subString) throws IOException {
        log.debug("Sinking an arktouros object in json:{}", subString);
        try {
            JsonNode jsonNode = objectMapper.readTree(subString);
            String type;
            try {
                type = jsonNode.get("type").asText().toLowerCase(Locale.ROOT);
            } catch (NullPointerException npe) {
                // 历史遗留问题
                type = jsonNode.get("sourceType").asText().toLowerCase(Locale.ROOT);
            }
            switch (type) {
                case "log":
                    Log log1 = objectMapper.readValue(subString, Log.class);
                    sinkService.sink(log1);
                    break;
                case "span":
                    Span span = objectMapper.readValue(subString, Span.class);
                    sinkService.sink(span);
                    break;
                case "metric":
                    String metricType = jsonNode.get("metricType").asText().toLowerCase(Locale.ROOT);
                    switch (metricType) {
                        case "gauge":
                            Gauge gauge = objectMapper.readValue(subString, Gauge.class);
                            sinkService.sink(gauge);
                            break;
                        case "counter":
                            Counter metric = objectMapper.readValue(subString, Counter.class);
                            sinkService.sink(metric);
                            break;
                        case "summary":
                            Summary summary = objectMapper.readValue(subString, Summary.class);
                            sinkService.sink(summary);
                            break;
                        case "histogram":
                            Histogram histogram = objectMapper.readValue(subString, Histogram.class);
                            sinkService.sink(histogram);
                            break;
                        default:
                            log.warn("Unknown metric type:{}", subString);
                    }
                    break;
                default:
                    log.warn("Unknown json type:{}", subString);
            }
        } catch (RuntimeException e) {
            log.error("Encountered an error while handling json from tcp:{}. Trying to recover.", subString);
            e.printStackTrace();
        }
    }
}
