package edu.npu.arktouros.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DataGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String[] SERVICE_NAMES = {
            "user-service", "order-service", "payment-service", "inventory-service",
            "notification-service", "auth-service", "shipping-service", "product-service",
            "recommendation-service", "cart-service", "review-service", "analytics-service",
            "search-service", "gateway-service", "config-service", "email-service",
            "logging-service", "monitoring-service", "cache-service", "file-service"
    };

    private static final String[] SPAN_NAMES = {
            "GET /api/users", "POST /api/orders", "GET /api/products", "POST /api/auth/login",
            "PUT /api/cart", "DELETE /api/cart", "GET /api/recommendations", "POST /api/payments",
            "GET /api/inventory", "POST /api/notifications", "GET /api/search", "PUT /api/users",
            "GET /api/reviews", "POST /api/shipping", "GET /api/config", "POST /api/analytics",
            "GET /api/files", "PUT /api/products", "DELETE /api/users", "GET /api/monitoring",
            "findUserById", "createOrder", "processPayment", "checkInventory",
            "sendNotification", "authenticate", "shipOrder", "fetchProduct",
            "generateRecommendations", "updateCart", "submitReview", "trackEvent",
            "executeSearch", "routeRequest", "loadConfiguration", "sendEmail",
            "logMessage", "checkHealth", "getCachedData", "uploadFile"
    };

    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};

    private static final String[] LOG_CONTENTS = {
            "Request processed successfully", "User authentication successful",
            "Database query completed in {}ms", "Cache miss for key: {}",
            "Connection established to remote service", "Received message from queue",
            "Failed to process request", "Invalid input parameters",
            "Database connection timeout", "Remote service unavailable",
            "Rate limit exceeded", "Permission denied for operation",
            "Resource not found", "Request validation failed",
            "Processing batch job", "Scheduled task executed"
    };

    private static final int TRACE_COUNT = 20;
    private static final int SPAN_PER_TRACE_MIN = 15;
    private static final int SPAN_PER_TRACE_MAX = 30;
    private static final int LOG_COUNT = 100;

    // 存储每个服务对应的spanName列表
    private static final Map<String, List<String>> serviceSpanMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // 初始化服务和span名称的映射关系
        initializeServiceSpanMap();

        List<Map<String, Object>> spans = generateSpans();
        List<Map<String, Object>> logs = generateLogs(spans);

        // 写入文件 - 每行一个JSON对象
        writeToFile("spans.txt", spans);
        writeToFile("logs.txt", logs);

        System.out.println("生成了 " + spans.size() + " 条 Span 记录");
        System.out.println("生成了 " + logs.size() + " 条 Log 记录");
    }

    private static void initializeServiceSpanMap() {
        // 确保每个服务有唯一的span名称
        List<String> allSpanNames = new ArrayList<>(List.of(SPAN_NAMES));
        Collections.shuffle(allSpanNames); // 随机打乱顺序

        int spansPerService = SPAN_NAMES.length / SERVICE_NAMES.length;

        for (int i = 0; i < SERVICE_NAMES.length; i++) {
            String service = SERVICE_NAMES[i];
            int startIdx = i * spansPerService;
            int endIdx = Math.min(startIdx + spansPerService, allSpanNames.size());

            if (i == SERVICE_NAMES.length - 1) {
                // 确保最后一个服务获取所有剩余的span名称
                endIdx = allSpanNames.size();
            }

            serviceSpanMap.put(service, new ArrayList<>(allSpanNames.subList(startIdx, endIdx)));
        }
    }

    private static void writeToFile(String filename, List<Map<String, Object>> records) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Map<String, Object> record : records) {
                String json = objectMapper.writeValueAsString(record);
                writer.write(json);
                writer.newLine();
            }
        }
    }

    private static List<Map<String, Object>> generateSpans() {
        List<Map<String, Object>> allSpans = new ArrayList<>();
        Map<String, List<Long>> spanNameDurations = new HashMap<>();

        for (int i = 0; i < TRACE_COUNT; i++) {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            int spanCount = ThreadLocalRandom.current().nextInt(SPAN_PER_TRACE_MIN, SPAN_PER_TRACE_MAX + 1);

            // 为每个trace创建一个根span
            String rootSpanId = UUID.randomUUID().toString().replace("-", "");
            String rootServiceName = randomSelect(SERVICE_NAMES);
            String rootSpanName = getRandomSpanNameForService(rootServiceName);

            long rootStartTime = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(3600000);
            long rootDuration = ThreadLocalRandom.current().nextLong(50, 200);
            long rootEndTime = rootStartTime + rootDuration;

            Map<String, Object> rootSpan = createSpan(
                    rootSpanId, rootServiceName, rootSpanName, traceId, "",
                    rootStartTime, rootEndTime, true
            );
            allSpans.add(rootSpan);

            // 记录span名称对应的持续时间
            spanNameDurations.computeIfAbsent(rootSpanName, k -> new ArrayList<>()).add(rootDuration);

            // 创建子span的层级结构
            List<String> currentLevelSpanIds = new ArrayList<>(Collections.singletonList(rootSpanId));
            int remainingSpans = spanCount - 1;

            // 创建多级span树
            while (remainingSpans > 0 && !currentLevelSpanIds.isEmpty()) {
                List<String> nextLevelSpanIds = new ArrayList<>();

                for (String parentId : currentLevelSpanIds) {
                    if (remainingSpans <= 0) break;

                    // 为每个父span创建1-3个子span
                    int childCount = Math.min(remainingSpans, ThreadLocalRandom.current().nextInt(1, 4));
                    remainingSpans -= childCount;

                    // 查找父span
                    Map<String, Object> parentSpan = allSpans.stream()
                            .filter(s -> s.get("id").equals(parentId))
                            .findFirst()
                            .orElseThrow();

                    long parentStartTime = (Long) parentSpan.get("startTime");
                    long parentEndTime = (Long) parentSpan.get("endTime");

                    // 创建子span
                    for (int j = 0; j < childCount; j++) {
                        String spanId = UUID.randomUUID().toString().replace("-", "");
                        String serviceName = randomSelect(SERVICE_NAMES);
                        String spanName = getRandomSpanNameForService(serviceName);

                        // 确保子span的时间范围在父span之内
                        long timeWindow = parentEndTime - parentStartTime;

                        // 修复：确保有足够的时间窗口
                        long minStartOffset = 5;
                        long startTime = parentStartTime + minStartOffset;

                        long duration;
                        if (timeWindow <= 15) { // 如果时间窗口太小
                            duration = 5; // 使用固定的小持续时间
                        } else {
                            // 确保上限始终大于下限
                            duration = ThreadLocalRandom.current().nextLong(10, Math.min(timeWindow - minStartOffset, 150));
                        }

                        long endTime = startTime + duration;

                        // 确保结束时间不超过父span的结束时间
                        if (endTime > parentEndTime) {
                            endTime = parentEndTime;
                            startTime = Math.max(parentStartTime, endTime - duration);
                        }

                        Map<String, Object> span = createSpan(
                                spanId, serviceName, spanName, traceId, parentId,
                                startTime, endTime, false
                        );
                        allSpans.add(span);
                        nextLevelSpanIds.add(spanId);

                        // 记录span名称对应的持续时间
                        spanNameDurations.computeIfAbsent(spanName, k -> new ArrayList<>()).add(duration);
                    }
                }

                currentLevelSpanIds = nextLevelSpanIds;
            }
        }

        // 添加10%的异常延迟span
        addSlowSpans(allSpans, spanNameDurations);

        return allSpans;
    }

    // 为指定服务获取随机spanName
    private static String getRandomSpanNameForService(String serviceName) {
        List<String> spanNames = serviceSpanMap.get(serviceName);
        if (spanNames == null || spanNames.isEmpty()) {
            return randomSelect(SPAN_NAMES); // 后备方案
        }
        return spanNames.get(ThreadLocalRandom.current().nextInt(spanNames.size()));
    }

    private static void addSlowSpans(List<Map<String, Object>> allSpans, Map<String, List<Long>> spanNameDurations) {
        // 计算每种span名称的平均持续时间
        Map<String, Double> avgDurations = new HashMap<>();
        spanNameDurations.forEach((name, durations) -> {
            double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0);
            avgDurations.put(name, avg);
        });

        // 选择10%的span进行延迟处理(排除已添加的span)
        int slowSpanCount = (int) Math.ceil(allSpans.size() * 0.1);
        Set<String> processedSpanIds = new HashSet<>();

        for (int i = 0; i < slowSpanCount; i++) {
            // 为慢span随机选择服务
            String serviceName = randomSelect(SERVICE_NAMES);
            // 为该服务选择一个跨度名称
            String spanName = getRandomSpanNameForService(serviceName);

            double avgDuration = avgDurations.getOrDefault(spanName, 50.0);
            long slowDuration = (long) (avgDuration * (1.5 + ThreadLocalRandom.current().nextDouble(1.0)));

            // 创建新的trace和span
            String traceId = UUID.randomUUID().toString().replace("-", "");
            String spanId = UUID.randomUUID().toString().replace("-", "");

            long startTime = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(3600000);
            long endTime = startTime + slowDuration;

            Map<String, Object> slowSpan = createSpan(
                    spanId, serviceName, spanName, traceId, "",
                    startTime, endTime, true
            );
            allSpans.add(slowSpan);
            processedSpanIds.add(spanId);

            // 添加1-3个子span
            int childCount = ThreadLocalRandom.current().nextInt(1, 4);
            for (int j = 0; j < childCount; j++) {
                String childSpanId = UUID.randomUUID().toString().replace("-", "");
                String childServiceName = randomSelect(SERVICE_NAMES);
                String childSpanName = getRandomSpanNameForService(childServiceName);

                long childStartTime = startTime + ThreadLocalRandom.current().nextLong(5, 10);
                long childDuration = ThreadLocalRandom.current().nextLong(10, 100);
                long childEndTime = childStartTime + childDuration;

                if (childEndTime > endTime) {
                    childEndTime = endTime - 2;
                    childStartTime = childEndTime - childDuration;
                }

                Map<String, Object> childSpan = createSpan(
                        childSpanId, childServiceName, childSpanName, traceId, spanId,
                        childStartTime, childEndTime, false
                );
                allSpans.add(childSpan);
            }
        }
    }

    private static List<Map<String, Object>> generateLogs(List<Map<String, Object>> spans) {
        List<Map<String, Object>> logs = new ArrayList<>(LOG_COUNT);

        // 70%的日志关联到现有span
        int linkedLogCount = (int) (LOG_COUNT * 0.7);

        // 创建关联日志
        for (int i = 0; i < linkedLogCount; i++) {
            Map<String, Object> randomSpan = spans.get(ThreadLocalRandom.current().nextInt(spans.size()));
            String traceId = (String) randomSpan.get("traceId");
            String spanId = (String) randomSpan.get("id");
            String serviceName = (String) randomSpan.get("serviceName");

            Long spanStartTime = (Long) randomSpan.get("startTime");
            Long spanEndTime = (Long) randomSpan.get("endTime");
            Long logTimestamp = spanStartTime + ThreadLocalRandom.current().nextLong(spanEndTime - spanStartTime + 1);

            boolean isError = ThreadLocalRandom.current().nextInt(10) < 2; // 20%的概率是错误日志
            String severityText = isError ? "ERROR" : randomSelect(LOG_LEVELS);

            logs.add(createLog(serviceName, traceId, spanId, logTimestamp, severityText, isError));
        }

        // 创建非关联日志
        for (int i = linkedLogCount; i < LOG_COUNT; i++) {
            String serviceName = randomSelect(SERVICE_NAMES);
            String severityText = randomSelect(LOG_LEVELS);
            boolean isError = "ERROR".equals(severityText);
            Long logTimestamp = System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(3600000);

            logs.add(createLog(serviceName, null, null, logTimestamp, severityText, isError));
        }

        return logs;
    }

    // 其余方法保持不变...
    private static Map<String, Object> createSpan(String id, String serviceName, String name,
                                                  String traceId, String parentSpanId,
                                                  long startTime, long endTime, boolean root) {
        Map<String, Object> span = new HashMap<>();
        span.put("name", name);
        span.put("id", id);
        span.put("serviceName", serviceName);
        span.put("traceId", traceId);
        span.put("parentSpanId", parentSpanId);
        span.put("startTime", startTime);
        span.put("endTime", endTime);
        span.put("root", root);
        span.put("type", "SPAN");
        span.put("tags", new ArrayList<>());

        // 创建本地和远程端点
        Map<String, Object> localEndpoint = new HashMap<>();
        localEndpoint.put("serviceName", serviceName);
        localEndpoint.put("ip", generateRandomIp());
        localEndpoint.put("port", ThreadLocalRandom.current().nextInt(8000, 9000));
        localEndpoint.put("latency", (int) (endTime - startTime));
        localEndpoint.put("type", "ENDPOINT");
        span.put("localEndPoint", localEndpoint);

        if (!root) {
            Map<String, Object> remoteEndpoint = new HashMap<>();
            remoteEndpoint.put("serviceName", serviceName);
            remoteEndpoint.put("ip", generateRandomIp());
            remoteEndpoint.put("port", ThreadLocalRandom.current().nextInt(8000, 9000));
            remoteEndpoint.put("latency", ThreadLocalRandom.current().nextInt(5, 50));
            remoteEndpoint.put("type", "ENDPOINT");
            span.put("remoteEndPoint", remoteEndpoint);
        } else {
            span.put("remoteEndPoint", null);
        }

        return span;
    }

    private static Map<String, Object> createLog(String serviceName, String traceId, String spanId,
                                                 Long timestamp, String severityText, boolean error) {
        Map<String, Object> log = new HashMap<>();
        log.put("serviceName", serviceName);
        log.put("traceId", traceId);
        log.put("spanId", spanId);
        log.put("type", "LOG");

        String content = randomSelect(LOG_CONTENTS);
        if (content.contains("{}")) {
            content = content.replace("{}", String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000)));
        }
        log.put("content", content);

        log.put("tags", new ArrayList<>());
        log.put("error", error);
        log.put("timestamp", timestamp);
        log.put("severityText", severityText);

        return log;
    }

    private static String generateRandomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.nextInt(10, 192) + "." +
                random.nextInt(0, 256) + "." +
                random.nextInt(0, 256) + "." +
                random.nextInt(1, 255);
    }

    private static <T> T randomSelect(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
}
