package edu.npu.arktouros;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Gauge;
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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : [wangminan]
 * @description : 为测试类提供springboot环境
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

    @Test
    void testMain() throws IOException {
        log.info("This is the main springboot class for test environment.");
        PropertiesProvider.init();
        sinkService.init();
        if (!sinkService.isReady()) {
            log.error("APM sink service is not ready, shutting down.");
            return;
        }
//        addTrace();
        addSpan();
    }

    @Test
    void testGetJsonLogs() throws IOException {
        for(int i = 0; i < 10; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            String traceId = UUID.randomUUID().toString();
            String spanId = UUID.randomUUID().toString();
            Span span = Span.builder()
                    .serviceName("test")
                    .name("test_file_input_" + i)
                    .id(spanId)
                    .traceId(traceId)
                    .parentSpanId("")
                    .root(false)
                    .startTime(System.currentTimeMillis())
                    .endTime(System.currentTimeMillis())
                    .localEndPoint(endPointA1)
                    .remoteEndPoint(endPointA1)
                    .build();
            stringBuilder.append(objectMapper.writeValueAsString(span));
            Log spanLog = Log.builder()
                    .serviceName("test")
                    .traceId(traceIdA)
                    .spanId(spanId)
                    .timestamp(System.currentTimeMillis())
                    .severityText("INFO")
                    .content("This is a test log.")
                    .error(false)
                    .build();
            stringBuilder.append(objectMapper.writeValueAsString(spanLog));
            Gauge gauge = Gauge.builder()
                    .name("test_gauge")
                    .description("This is a test gauge.")
                    .timestamp(System.currentTimeMillis())
                    .labels(new HashMap<>())
                    .value(1.0)
                    .build();
            gauge.setServiceName("test");
            stringBuilder.append(objectMapper.writeValueAsString(gauge));
            File file = new File(logDir + "/logs_" + i + ".txt");
            file.createNewFile();
            Files.write(file.toPath(), stringBuilder.toString().getBytes(),
                    StandardOpenOption.APPEND);
        }
    }


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

    private void addSpan() throws IOException {
        // 这里只针对一个服务加复杂的树状Span
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
}
