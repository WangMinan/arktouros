package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class SearchServiceImplTest {

    @Resource
    private SearchService service;

    @MockBean
    private SearchMapper searchMapper;

    @Test
    void testGetServiceList() {
        Mockito.when(searchMapper.getServiceList(any())).thenReturn(R.ok());
        ServiceQueryDto queryDto = Mockito.mock(ServiceQueryDto.class);
        Assertions.assertDoesNotThrow(() -> service.getServiceList(queryDto));
    }

    @Test
    void testGetServiceTopology() {
        Service service1 = Service.builder().name("service1").build();
        Service service2 = Service.builder().name("service2").build();
        Mockito.when(searchMapper.getServiceListFromNamespace(any()))
                .thenReturn(List.of(service1, service2));
        // 造一条3个span组成的trace
        Span father = Span.builder()
                .root(true)
                .traceId("123")
                .id("456")
                .parentSpanId(null)
                .serviceName("service1")
                .build();
        Span child1 = Span.builder()
                .traceId("123")
                .id("123")
                .parentSpanId("456")
                .root(false)
                .serviceName("service1")
                .build();
        Span child2 = Span.builder()
                .traceId("123")
                .id("789")
                .parentSpanId("456")
                .root(false)
                .serviceName("service2")
                .build();
        Mockito.when(searchMapper.getSpanListByServiceNames(any()))
                .thenReturn(List.of(father, child1, child2));
        Mockito.when(searchMapper.getServiceByName(eq("service1")))
                .thenReturn(service1);
        Mockito.when(searchMapper.getServiceByName(eq("service2")))
                .thenReturn(service2);
        Assertions.assertDoesNotThrow(() -> service.getServiceTopology("namespace"));
    }

    @Test
    void testGetLogList() {
        Mockito.when(searchMapper.getLogListByQuery(any())).thenReturn(R.ok());
        LogQueryDto queryDto = Mockito.mock(LogQueryDto.class);
        Assertions.assertDoesNotThrow(() -> service.getLogList(queryDto));
    }

    @Test
    void testGetEndPointListByServiceName() {
        Mockito.when(searchMapper.getEndPointListByServiceName(any())).thenReturn(R.ok());
        EndPointQueryDto queryDto = Mockito.mock(EndPointQueryDto.class);
        Assertions.assertDoesNotThrow(() -> service
                .getEndPointListByServiceName(queryDto));
    }

    @Test
    void testGetSpanTopologyByTraceId() {
        Mockito.when(searchMapper.getSpanListByTraceId(any())).thenReturn(List.of());
        Assertions.assertDoesNotThrow(() -> service
                .getSpanTopologyByTraceId(new SpanTopologyQueryDto("123", "test", false)));
        // 然后用一个有数据的情况
        Span father = Span.builder()
                .root(true)
                .traceId("123")
                .id("456")
                .parentSpanId(null)
                .serviceName("service1")
                .build();
        Span child1 = Span.builder()
                .traceId("123")
                .id("123")
                .parentSpanId("456")
                .root(false)
                .serviceName("service1")
                .build();
        Span child2 = Span.builder()
                .traceId("123")
                .id("789")
                .parentSpanId("456")
                .root(false)
                .serviceName("service2")
                .build();
        Mockito.when(searchMapper.getSpanListByTraceId(any()))
                .thenReturn(List.of(father, child1, child2));
        Assertions.assertDoesNotThrow(() -> service
                .getSpanTopologyByTraceId(new SpanTopologyQueryDto("123", "test", false)));
    }

    @Test
    void testGetMetricsNull() {
        Mockito.when(searchMapper.getMetricsNames(any(), any())).thenReturn(List.of());
        Assertions.assertDoesNotThrow(() ->
                service.getMetrics(Mockito.mock(MetricQueryDto.class)));
    }

    @Test
    void testGetMetrics() {
        Mockito.when(searchMapper.getMetricsNames(any(), any()))
                .thenReturn(List.of("metric1", "metric2"));
        Gauge gauge1 = Gauge.builder()
                .name("metric1")
                .value(1)
                .labels(new HashMap<>())
                .build();
        gauge1.setServiceName("service1");
        Gauge gauge2 = Gauge.builder()
                .name("metric1")
                .value(2)
                .labels(new HashMap<>())
                .build();
        gauge2.setServiceName("service1");
        Summary summary = Summary.builder()
                .labels(new HashMap<>())
                .name("metric3").build();
        summary.setServiceName("service1");
        Mockito.when(searchMapper.getMetricsValues(any(), any(), any(), any()))
                .thenReturn(List.of(gauge1, gauge2, summary));
        Assertions.assertDoesNotThrow(() ->
                service.getMetrics(Mockito.mock(MetricQueryDto.class)));
    }

    @Test
    void testGetNamespaceList() {
        Mockito.when(searchMapper.getNamespaceList(anyString())).thenReturn(R.ok());
        Assertions.assertDoesNotThrow(() -> service.getNamespaceList("default"));
    }

    @Test
    void testGetAllLogLevels() {
        Mockito.when(searchMapper.getAllLogLevels(anyString())).thenReturn(R.ok());
        Assertions.assertDoesNotThrow(() -> service.getAllLogLevels("level"));
    }
}
