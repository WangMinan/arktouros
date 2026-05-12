package edu.npu.arktouros;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.config.PropertiesProvider;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.sinker.SinkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : [wangminan]
 * @description : 向数据库中添加Span以供页面测试
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class TestAddSpan {

    @Resource
    private SinkService sinkService;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${receiver.file.json.logDir}")
    private String logDir;

    private static final String traceIdA = UUID.randomUUID().toString();
    private static final String traceIdB = UUID.randomUUID().toString();
    private static final String traceIdF = UUID.randomUUID().toString();

    private static final EndPoint endPointA1 = EndPoint.builder()
            .ip("127.0.0.1").port(11000).serviceName("service_a").build();
    private static final EndPoint endPointA2 = EndPoint.builder()
            .ip("127.0.0.1").port(12000).serviceName("service_a").build();
    private static final EndPoint endPointB = EndPoint.builder()
            .ip("127.0.0.1").port(11001).serviceName("service_b").build();
    private static final EndPoint endPointC = EndPoint.builder()
            .ip("127.0.0.1").port(11002).serviceName("service_c").build();
    private static final EndPoint endPointD = EndPoint.builder()
            .ip("127.0.0.1").port(11003).serviceName("service_d").build();
    private static final EndPoint endPointE = EndPoint.builder()
            .ip("127.0.0.1").port(11004).serviceName("service_e").build();
    private static final EndPoint endPointF = EndPoint.builder()
            .ip("127.0.0.1").port(11005).serviceName("service_f").build();
    private static final EndPoint endPointF2 = EndPoint.builder()
            .ip("127.0.0.1").port(11006).serviceName("service_f").build();

    @Test
    void testMain() throws IOException {
        log.info("This is the main springboot class for test environment.");
        PropertiesProvider.init();
        sinkService.init();
        if (!sinkService.isReady()) {
            log.error("APM sink service is not ready, shutting down.");
            return;
        }
        addTrace();
        addTreeSpan();
        addDuplicateCallBetweenTwoServices();
    }

    @Test
    void testGetJsonLogs() throws IOException {
        Path outputDir = Path.of(logDir);
        Files.createDirectories(outputDir);

        long baseTime = System.currentTimeMillis();

        List<Object> traceAObjects = new ArrayList<>();
        String jsonTraceIdA = UUID.randomUUID().toString();
        Span serviceARoot = buildSpan(jsonTraceIdA, "", true,
                "service_a_root", "service_a", endPointA1, null,
                baseTime, baseTime + 1_600, false);
        appendSpanWithLogs(traceAObjects, serviceARoot,
                "service_a receives request and prepares a downstream call.", false);

        Span serviceACallB = buildSpan(jsonTraceIdA, serviceARoot.getId(), false,
                "service_a_call_service_b", "service_a", endPointA2, endPointB,
                baseTime + 80, baseTime + 1_420, false);
        appendSpanWithLogs(traceAObjects, serviceACallB,
                "service_a calls service_b for order enrichment.", false);

        Span serviceBCallC = buildSpan(jsonTraceIdA, serviceACallB.getId(), false,
                "service_b_call_service_c", "service_b", endPointB, endPointC,
                baseTime + 190, baseTime + 1_180, false);
        appendSpanWithLogs(traceAObjects, serviceBCallC,
                "service_b calls service_c for inventory detail.", false);

        Span serviceCWork = buildSpan(jsonTraceIdA, serviceBCallC.getId(), false,
                "service_c_query_inventory", "service_c", endPointC, null,
                baseTime + 330, baseTime + 980, false);
        appendSpanWithLogs(traceAObjects, serviceCWork,
                "service_c completes inventory query.", false);
        writeJsonObjects(outputDir.resolve("sample-topology-service-a-b-c.json").toFile(), traceAObjects);

        List<Object> traceDObjects = new ArrayList<>();
        String jsonTraceIdB = UUID.randomUUID().toString();
        long traceBBaseTime = baseTime + 10_000;
        Span serviceDRoot = buildSpan(jsonTraceIdB, "", true,
                "service_d_root", "service_d", endPointD, endPointB,
                traceBBaseTime, traceBBaseTime + 24_000, true);
        appendSpanWithLogs(traceDObjects, serviceDRoot,
                "service_d receives a batch request and calls service_b.", true);

        Span serviceBCallE = buildSpan(jsonTraceIdB, serviceDRoot.getId(), false,
                "service_b_call_service_e", "service_b", endPointB, endPointE,
                traceBBaseTime + 180, traceBBaseTime + 22_600, true);
        appendSpanWithLogs(traceDObjects, serviceBCallE,
                "service_b performs routing and calls service_e.", true);

        Span serviceECallF = buildSpan(jsonTraceIdB, serviceBCallE.getId(), false,
                "service_e_call_service_f", "service_e", endPointE, endPointF,
                traceBBaseTime + 360, traceBBaseTime + 21_500, true);
        appendSpanWithLogs(traceDObjects, serviceECallF,
                "service_e delegates expensive aggregation to service_f.", true);

        Span serviceFRoot = buildSpan(jsonTraceIdB, serviceECallF.getId(), false,
                "service_f_aggregation_root", "service_f", endPointF, null,
                traceBBaseTime + 620, traceBBaseTime + 20_800, true);
        appendSpanWithLogs(traceDObjects, serviceFRoot,
                "service_f starts multi-layer aggregation.", true);
        appendSameNameDurationSamples(traceDObjects, jsonTraceIdB, serviceFRoot);
        appendServiceFSpanTree(traceDObjects, jsonTraceIdB, serviceFRoot);
        writeJsonObjects(outputDir.resolve("sample-topology-service-d-b-e-f.json").toFile(), traceDObjects);
    }

    private Span buildSpan(String traceId, String parentSpanId, boolean root,
                           String name, String serviceName,
                           EndPoint localEndPoint, EndPoint remoteEndPoint,
                           long startTime, long endTime, boolean longDuration) {
        EndPoint actualRemoteEndPoint = remoteEndPoint == null ? localEndPoint : remoteEndPoint;
        return Span.builder()
                .id(UUID.randomUUID().toString())
                .traceId(traceId)
                .parentSpanId(parentSpanId)
                .root(root)
                .name(name)
                .serviceName(serviceName)
                .startTime(startTime)
                .endTime(endTime)
                .localEndPoint(localEndPoint)
                .remoteEndPoint(actualRemoteEndPoint)
                .tags(buildSpanTags(longDuration))
                .build();
    }

    private void appendSpanWithLogs(List<Object> jsonObjects, Span span,
                                    String content, boolean longDuration) {
        jsonObjects.add(span);
        jsonObjects.add(Log.builder()
                .spanId(span.getId())
                .serviceName(span.getServiceName())
                .traceId(span.getTraceId())
                .severityText("INFO")
                .content(content + " span=" + span.getName())
                .tags(new ArrayList<>())
                .error(false)
                .timestamp(span.getStartTime() + 10)
                .build());
        if (longDuration) {
            jsonObjects.add(Log.builder()
                    .spanId(span.getId())
                    .serviceName(span.getServiceName())
                    .traceId(span.getTraceId())
                    .severityText("WARN")
                    .content("Long duration span detected, duration_ms="
                            + (span.getEndTime() - span.getStartTime())
                            + ", span=" + span.getName())
                    .tags(buildLongDurationTags())
                    .error(false)
                    .timestamp(span.getEndTime())
                    .build());
        }
    }

    private void appendSameNameDurationSamples(List<Object> jsonObjects, String traceId,
                                               Span parentSpan) {
        long[] durations = {120, 150, 180, 220, 260, 320, 380, 450, 520, 800, 1_200, 6_200};
        for (int i = 0; i < durations.length; i++) {
            long startTime = parentSpan.getStartTime() + 1_000 + i * 130L;
            boolean longDuration = durations[i] >= 3_000;
            Span sampleSpan = buildSpan(traceId, parentSpan.getId(), false,
                    "service_f_hot_path_rpc", "service_f", endPointF2, endPointF2,
                    startTime, startTime + durations[i], longDuration);
            appendSpanWithLogs(jsonObjects, sampleSpan,
                    "service_f same-name duration sample "
                            + (i + 1) + ", duration_ms=" + durations[i] + ".", longDuration);
        }
    }

    private void appendServiceFSpanTree(List<Object> jsonObjects, String traceId,
                                        Span serviceFRoot) {
        AtomicInteger spanCounter = new AtomicInteger(1);
        List<SpanTreeCursor> formerSpanList = new ArrayList<>();
        formerSpanList.add(new SpanTreeCursor(serviceFRoot.getId(), serviceFRoot.getStartTime()));
        for (int level = 1; level <= 5; level++) {
            List<SpanTreeCursor> nextSpanList = new ArrayList<>();
            for (SpanTreeCursor formerSpan : formerSpanList) {
                int leaves = random.nextInt(3) + 1;
                for (int leaf = 0; leaf < leaves; leaf++) {
                    boolean longDuration = random.nextDouble() > 0.65
                            || (level % 2 == 0 && leaf == 0);
                    boolean error = longDuration && random.nextDouble() > 0.78;
                    long startTime = formerSpan.startTime()
                            + 90L * level
                            + 40L * leaf
                            + random.nextInt(180);
                    long duration = longDuration
                            ? 2_500L + random.nextInt(6_500)
                            : 80L + random.nextInt(620);
                    Span currentSpan = buildSpan(traceId, formerSpan.spanId(), false,
                            getServiceFTreeSpanName(level),
                            "service_f", endPointF2, endPointF2,
                            startTime, startTime + duration, longDuration);
                    appendSpanWithLogs(jsonObjects, currentSpan,
                            "service_f handles tree layer " + level
                                    + " leaf " + leaf
                                    + " sample " + spanCounter.getAndIncrement()
                                    + ", duration_ms=" + duration + ".", longDuration);
                    if (error) {
                        jsonObjects.add(Log.builder()
                                .spanId(currentSpan.getId())
                                .serviceName(currentSpan.getServiceName())
                                .traceId(currentSpan.getTraceId())
                                .severityText("ERROR")
                                .content("service_f branch returned degraded result, span="
                                        + currentSpan.getName())
                                .tags(buildLongDurationTags())
                                .error(true)
                                .timestamp(currentSpan.getEndTime())
                                .build());
                    } else {
                        nextSpanList.add(new SpanTreeCursor(currentSpan.getId(), currentSpan.getStartTime()));
                    }
                }
            }
            formerSpanList = nextSpanList;
            if (formerSpanList.isEmpty()) {
                break;
            }
        }
    }

    private String getServiceFTreeSpanName(int level) {
        return switch (level) {
            case 1 -> "service_f_prepare_context";
            case 2 -> "service_f_load_profile";
            case 3 -> "service_f_merge_feature";
            case 4 -> "service_f_write_cache";
            case 5 -> "service_f_finalize_response";
            default -> "service_f_tree_worker";
        };
    }

    private void writeJsonObjects(File file, List<Object> jsonObjects) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Object jsonObject : jsonObjects) {
            lines.add(objectMapper.writeValueAsString(jsonObject));
        }
        String content = String.join(System.lineSeparator(), lines);
        if (content.contains("\"remoteEndPoint\":\"\"")) {
            throw new IllegalStateException("Invalid json sample, remoteEndPoint must not be serialized as empty string.");
        }
        Files.writeString(file.toPath(), content,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<Tag> buildSpanTags(boolean longDuration) {
        if (longDuration) {
            return buildLongDurationTags();
        }
        return new ArrayList<>();
    }

    private List<Tag> buildLongDurationTags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.builder()
                .key(Span.SpanTagKey.LONG_DURATION.getKey())
                .value(Span.SpanTagValue.TRUE.getValue())
                .build());
        return tags;
    }

    private record SpanTreeCursor(String spanId, long startTime) {
    }

    /**
     * 我现在要构造一个从
     * service_a --> service_b --> service_c
     * service_d --> service_b --> service_e --> service_f
     * 的服务拓扑
     */
    private void addTrace() throws IOException {
        // traceA
        // service_A
        String formerSpanId = UUID.randomUUID().toString();
        String currentSpanId = formerSpanId;
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdA)
                .root(true)
                .name("service_a_1")
                .serviceName("service_a")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointA1)
                .build());
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdA)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_a_2")
                .serviceName("service_a")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointA2)
                .remoteEndPoint(endPointB)
                .build());
        // service_b
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdA)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_b_1")
                .serviceName("service_b")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointB)
                .remoteEndPoint(endPointC)
                .build());
        // service_c
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdA)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_c_1")
                .serviceName("service_c")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointC)
                .build());
        // traceB
        // service_d
        formerSpanId = UUID.randomUUID().toString();
        currentSpanId = formerSpanId;
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdB)
                .root(true)
                .name("service_d_1")
                .serviceName("service_d")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointD)
                .remoteEndPoint(endPointB)
                .build());
        // service_b
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdB)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_b_2")
                .serviceName("service_b")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointB)
                .remoteEndPoint(endPointE)
                .build());
        // service_e
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdB)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_e_1")
                .serviceName("service_e")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointE)
                .remoteEndPoint(endPointF)
                .build());
        // service_f
        formerSpanId = currentSpanId;
        currentSpanId = UUID.randomUUID().toString();
        log.info("CurrentSpanId: {}", currentSpanId);
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdB)
                .root(false)
                .parentSpanId(formerSpanId)
                .name("service_f_1")
                .serviceName("service_f")
                .startTime(System.currentTimeMillis())
                .endTime((long) PersistentDataConstants.ERROR_SPAN_END_TIME)
                .localEndPoint(endPointF)
                .build());
    }

    /**
     * 这里只针对一个服务加复杂的树状Span
     * 对serviceF做一个随机的五层的span树
     */
    private void addTreeSpan() throws IOException {
        AtomicInteger spanCounter = new AtomicInteger(2);
        String currentSpanId = UUID.randomUUID().toString();
        sinkService.sink(Span.builder()
                .id(currentSpanId)
                .traceId(traceIdF)
                .root(true)
                .name("service_f_" + spanCounter.getAndIncrement())
                .serviceName("service_f")
                .startTime(System.currentTimeMillis())
                .endTime(System.currentTimeMillis())
                .localEndPoint(endPointF2)
                .build());
        List<String> formerSpanIdList = new ArrayList<>();
        formerSpanIdList.add(currentSpanId);
        // 我们做一个五层的Span树
        for (int i = 0; i < 5; i++) {
            List<String> tmpFormerSpanIdList = new ArrayList<>(formerSpanIdList);
            formerSpanIdList.clear();
            tmpFormerSpanIdList.forEach(formerSpanId -> {
                // random 1-3
                int leaves = (int) (Math.random() * 3) + 1;
                for (int j = 0; j < leaves; j++) {
                    String innerCurrentSpanId = UUID.randomUUID().toString();
                    boolean isError = Math.random() > 0.85;
                    try {
                        sinkService.sink(Span.builder()
                                .id(innerCurrentSpanId)
                                .traceId(traceIdF)
                                .root(false)
                                .parentSpanId(formerSpanId)
                                .name("service_f_" + spanCounter.getAndIncrement())
                                .serviceName("service_f")
                                .startTime(System.currentTimeMillis())
                                .endTime(isError ? PersistentDataConstants.ERROR_SPAN_END_TIME :
                                        System.currentTimeMillis())
                                .localEndPoint(endPointF2)
                                .remoteEndPoint(endPointF2)
                                .build());
                        if (isError) {
                            // 生成错误日志
                            sinkService.sink(Log.builder()
                                    .spanId(innerCurrentSpanId)
                                    .serviceName("service_f")
                                    .traceId(traceIdF)
                                    .severityText("ERROR")
                                    .content("Service with spanId:" + innerCurrentSpanId + " has encountered an error")
                                    .tags(new ArrayList<>())
                                    .error(true)
                                    .timestamp(System.currentTimeMillis())
                                    .build());
                        } else {
                            formerSpanIdList.add(innerCurrentSpanId);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private static final Random random = new Random();

    /**
     * 添加两个服务之间的反复调用
     * 为调用时间统计看板提供基础数据
    */
    private void addDuplicateCallBetweenTwoServices() {
        for (int i = 1; i < 100; i++) {
            String traceId = UUID.randomUUID().toString();
            String currentSpanId = UUID.randomUUID().toString();
            String spanName = "test_duplicate_call";
            long startTime = System.currentTimeMillis();

            Span currentSpan = Span.builder()
                    .id(currentSpanId)
                    // 我们默认在调用关系相同的时候span的名称也是相同的
                    .name(spanName)
                    .serviceName("service_a_duplicate")
                    .traceId(traceId)
                    .root(true)
                    .startTime(startTime)
                    .localEndPoint(endPointA1)
                    // 回调在 50-250ms 之间 均值 150ms 标准差 20ms
                    .endTime(startTime + (long) generateNormalRandom(
                                    50,
                                    200,
                                    150,
                                    20))
                    .build();
            try {
                sinkService.sink(currentSpan);
            } catch (IOException e) {
                log.error("Failed to sink span:{}", currentSpan);
            }
        }
    }

    /**
     * 在指定范围内生成一个正态分布的随机数
     * @param min 最小值
     * @param max 最大值
     * @param mean 均值
     * @param stdDev 标准差
     * @return 生成的随机数
     */
    public static double generateNormalRandom(double min,
                                              double max,
                                              double mean,
                                              double stdDev) {
        double value;
        do {
            value = mean + stdDev * random.nextGaussian();
        } while (value < min || value > max);
        return value;
    }
}
