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
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : Json日志预处理器
 */
@Slf4j
public class JsonFilePreHandler extends Thread {
    // 阻塞队列 不需要考虑并发问题 用synchronize或者lock画蛇添足会导致线程阻塞
    private final ArrayBlockingQueue<String> inputCache;
    private final StringBuilder cacheStringBuilder = new StringBuilder();
    private final String fileType;
    private final File logDirFile;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;
    private final TraceQueueService traceQueueService;
    private final int sytelTraceAnalyzerNumber;
    private List<SytelTraceAnalyzer> traceAnalyzers;
    private ExecutorService traceAnalyzerThreadPool;
    protected static boolean needCleanWhileShutdown = false;

    public JsonFilePreHandler(ArrayBlockingQueue<String> inputCache, String fileType,
                              File logDirFile, TraceQueueService traceQueueService,
                              SinkService sinkService, ObjectMapper objectMapper,
                              int sytelTraceAnalyzerNumber) {
        this.inputCache = inputCache;
        this.fileType = fileType;
        this.logDirFile = logDirFile;
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
        this.traceQueueService = traceQueueService;
        this.sytelTraceAnalyzerNumber = sytelTraceAnalyzerNumber;
    }

    private void initSytelAnalyzers() {
        this.traceAnalyzers = new ArrayList<>();
        if (fileType.equals("sytel")) {
            // 启动一个线程池来处理sytel的数据
            ThreadFactory traceAnalyzerThreadFactory = new BasicThreadFactory.Builder()
                    .namingPattern("Trace-analyzer-%d").build();
            traceAnalyzerThreadPool = new ThreadPoolExecutor(sytelTraceAnalyzerNumber, sytelTraceAnalyzerNumber,
                    0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(sytelTraceAnalyzerNumber),
                    traceAnalyzerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
            for (int i = 0; i < sytelTraceAnalyzerNumber; i++) {
                SytelTraceAnalyzer traceAnalyzer = new SytelTraceAnalyzer(sinkService, objectMapper);
                traceAnalyzers.add(traceAnalyzer);
                traceAnalyzer.setNeedCleanWhileShutdown(needCleanWhileShutdown);
                SytelTraceAnalyzer.setQueueService(traceQueueService);
                traceAnalyzer.setName("SytelTraceAnalyzer-" + i);
                traceAnalyzer.init();
                traceAnalyzerThreadPool.submit(traceAnalyzer);
            }
        }
    }

    @Override
    public void run() {
        log.info("JsonFilePreHandler start working");
        initSytelAnalyzers();
        while (!isInterrupted()) {
            try {
                handle();
            } catch (InterruptedException | IOException e) {
                log.warn("JsonFilePreHandler interrupted during running.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void handle() throws InterruptedException, IOException {
        log.debug("Formatting input from cache.");
        String input = cacheStringBuilder.append(inputCache.take().trim()).toString();
        if (fileType.equalsIgnoreCase("sytel")) {
            extractJsonFromSytel();
            input = cacheStringBuilder.toString();
        }
        if (!input.startsWith("{")) {
            throw new IllegalArgumentException("Invalid input for json when handling: " + input);
        }
        extractJson(input);
    }

    private void extractJsonFromSytel() {
        StringBuilder result = new StringBuilder();
        boolean insideSquareBrackets = false;
        boolean insideCurlyBraces = false;

        for (int i = 0; i < cacheStringBuilder.length(); i++) {
            char currentChar = cacheStringBuilder.charAt(i);

            if (currentChar == '[' && !insideCurlyBraces) {
                // 进入方括号状态
                insideSquareBrackets = true;
            } else if (currentChar == ']' && insideSquareBrackets) {
                // 退出方括号状态
                insideSquareBrackets = false;
            } else if (currentChar == '{') {
                // 进入花括号状态
                insideCurlyBraces = true;
                result.append(currentChar);  // 保留花括号内容
            } else if (currentChar == '}' && insideCurlyBraces) {
                // 退出花括号状态
                insideCurlyBraces = false;
                result.append(currentChar);  // 保留花括号内容
            } else {
                // 如果在花括号内，保留内容；在方括号内则忽略
                if (insideCurlyBraces || !insideSquareBrackets) {
                    result.append(currentChar);
                }
            }
        }

        // 更新全局变量 input 为处理后的内容
        cacheStringBuilder.setLength(0); // 清空原内容
        cacheStringBuilder.append(result); // 追加处理后的内容
    }

    private void extractJson(String input) throws IOException {
        // 开始做大括号匹配 匹配部分扔出去 剩下的放cache里
        Stack<Character> stack = new Stack<>();
        boolean isInStrFlag = false; // 游标是否正在字符串中
        int lastPos = 0;
        int currentPos;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                isInStrFlag = !isInStrFlag;
            } else if (c == '{' && !isInStrFlag) {
                stack.push('{');
            } else if (c == '}' && !isInStrFlag) {
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

    @Override
    public void interrupt() {
        super.interrupt(); // 暂停read操作
        if (needCleanWhileShutdown) {
            traceAnalyzers.forEach(traceAnalyzer -> {
                traceAnalyzer.setNeedCleanWhileShutdown(true);
            });
            // 删除日志文件夹里的所有文件
            if (logDirFile.exists()) {
                File[] files = logDirFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            log.warn("Failed to delete file:{} from logdir:{}",
                                    file.getName(), logDirFile);
                        }
                    }
                }
            }
        }
        if (traceAnalyzerThreadPool != null) {
            traceAnalyzerThreadPool.shutdown();
        }
    }
}
