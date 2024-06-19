package edu.npu.arktouros.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Metric;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;

/**
 * @author : [wangminan]
 * @description : {@link JacksonConfig}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class JacksonConfigTest {

    @Resource
    private ObjectMapper objectMapper;

    @Test
    void testObjectMapper() throws JsonProcessingException {
        Assertions.assertNotNull(objectMapper);
        String str = """
                {
                  "serviceName": null,
                  "traceId": null,
                  "spanId": null,
                  "content": "",
                  "tags": [
                    {
                      "key": "k8s.resource.name",
                      "value": "events"
                    },
                    {
                      "key": "event.domain",
                      "value": "k8s"
                    },
                    {
                      "key": "event.name",
                      "value": "otel-collector-cluster-opentelemetry-collector-59cc664645-kjcc6.17c57569b0e7fa50"
                    }
                  ],
                  "error": false,
                  "timestamp": "0",
                  "severityText": "",
                  "type": "LOG"
                }
                """;
        Log logValue = objectMapper.readValue(str, Log.class);
        Assertions.assertNotNull(logValue);
        Assertions.assertDoesNotThrow(() -> objectMapper.writeValueAsString(logValue));
    }

    @Test
    void testMetric() throws JsonProcessingException {
        Gauge gauge = Gauge.builder()
                .name("test")
                .value(1.0)
                .timestamp(System.currentTimeMillis())
                .labels(new HashMap<>())
                .build();
        String str = objectMapper.writeValueAsString(gauge);
        Metric metric = objectMapper.readValue(str, Metric.class);
        System.out.println(metric);
        Assertions.assertNotNull(metric);
    }
}
