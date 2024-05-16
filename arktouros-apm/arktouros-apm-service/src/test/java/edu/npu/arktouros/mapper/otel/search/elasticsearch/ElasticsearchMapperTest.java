package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link ElasticsearchMapper}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ElasticsearchMapperTest {

    private MockedStatic<ElasticsearchUtil> elasticsearchUtil;

    @Resource
    private ElasticsearchMapper elasticsearchMapper;

    @BeforeEach
    void beforeEach() {
        elasticsearchUtil = Mockito.mockStatic(ElasticsearchUtil.class);
    }

    @AfterEach
    void afterEach() {
        elasticsearchUtil.close();
    }

    @Test
    void testGetServiceList() {
        ServiceQueryDto queryDto = Mockito.mock(ServiceQueryDto.class);
        SearchResponse<Service> searchResponse =
                Mockito.mock(SearchResponse.class);
        HitsMetadata<Service> hits = Mockito.mock(HitsMetadata.class);
        Mockito.when(searchResponse.hits()).thenReturn(hits);
        Mockito.when(searchResponse.hits().hits()).thenReturn(new ArrayList<>());
        Mockito.when(searchResponse.hits().total()).thenReturn(null);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        Assertions.assertNotNull(elasticsearchMapper.getServiceList(queryDto));
        Mockito.when(queryDto.query()).thenReturn("123");
        Mockito.when(queryDto.namespace()).thenReturn("default");
        TotalHits totalHits = Mockito.mock(TotalHits.class);
        Mockito.when(totalHits.value()).thenReturn(1L);
        Mockito.when(searchResponse.hits().total()).thenReturn(totalHits);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        Assertions.assertNotNull(elasticsearchMapper.getServiceList(queryDto));
    }

    @Test
    void testGetServiceListFromNamespace() {
        SearchResponse<Service> searchResponse =
                Mockito.mock(SearchResponse.class);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        HitsMetadata<Service> hits = Mockito.mock(HitsMetadata.class);
        Mockito.when(searchResponse.hits()).thenReturn(hits);
        Mockito.when(searchResponse.hits().hits()).thenReturn(new ArrayList<>());
        Assertions.assertNotNull(elasticsearchMapper
                .getServiceListFromNamespace("default"));
    }

    @Test
    void testGetSpanListByServiceNames() {
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .scrollSearch(Mockito.any(), Mockito.any()))
                .thenReturn(new ArrayList<>());
        Assertions.assertNotNull(elasticsearchMapper
                .getSpanListByServiceNames(new ArrayList<>()));
        Assertions.assertNotNull(elasticsearchMapper
                .getSpanListByServiceNames(List.of("123", "456")));
    }

    @Test
    void testGetServiceByName() {
        SearchResponse<Service> searchResponse =
                Mockito.mock(SearchResponse.class);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        HitsMetadata<Service> hits = Mockito.mock(HitsMetadata.class);
        Mockito.when(searchResponse.hits()).thenReturn(hits);
        Mockito.when(searchResponse.hits().hits()).thenReturn(new ArrayList<>());
        Assertions.assertNull(elasticsearchMapper.getServiceByName("123"));
        Hit<Service> hit = Mockito.mock(Hit.class);
        Service service = Mockito.mock(Service.class);
        List<Hit<Service>> services = List.of(hit);
        Mockito.when(searchResponse.hits().hits()).thenReturn(services);
        Mockito.when(services.getFirst().source()).thenReturn(service);
        Assertions.assertNotNull(elasticsearchMapper.getServiceByName("123"));
    }

    @Test
    void testGetLogListByQuery() {
        LogQueryDto logQueryDto = Mockito.mock(LogQueryDto.class);
        SearchResponse<Service> searchResponse =
                Mockito.mock(SearchResponse.class);
        HitsMetadata<Service> hits = Mockito.mock(HitsMetadata.class);
        Mockito.when(searchResponse.hits()).thenReturn(hits);
        Mockito.when(searchResponse.hits().hits()).thenReturn(new ArrayList<>());
        Mockito.when(searchResponse.hits().total()).thenReturn(null);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        Assertions.assertNotNull(elasticsearchMapper.getLogListByQuery(logQueryDto));
        Mockito.when(logQueryDto.serviceName()).thenReturn("123");
        Mockito.when(logQueryDto.traceId()).thenReturn("traceId");
        Mockito.when(logQueryDto.keyword()).thenReturn("keyword");
        Mockito.when(logQueryDto.keywordNotIncluded()).thenReturn("keywordNotIncluded");
        Mockito.when(logQueryDto.severityText()).thenReturn("severityText");
        Mockito.when(logQueryDto.startTimestamp()).thenReturn(null);
        Mockito.when(logQueryDto.endTimestamp()).thenReturn(null);
        Assertions.assertNotNull(elasticsearchMapper.getLogListByQuery(logQueryDto));
    }

    @Test
    void testGetEndPointListByServiceName() {
        SearchResponse<Span> searchResponse =
                Mockito.mock(SearchResponse.class);
        elasticsearchUtil.when(() -> ElasticsearchUtil
                        .simpleSearch(Mockito.any(), Mockito.any()))
                .thenReturn(searchResponse);
        HitsMetadata<Span> hits = Mockito.mock(HitsMetadata.class);
        Mockito.when(searchResponse.hits()).thenReturn(hits);
        Hit<Span> hit1 = Mockito.mock(Hit.class);
        Mockito.when(searchResponse.hits().hits()).thenReturn(List.of(hit1));
        EndPointQueryDto endPointQueryDto = Mockito.mock(EndPointQueryDto.class);
        Mockito.when(endPointQueryDto.serviceName()).thenReturn("123");
        Assertions.assertNotNull(
                elasticsearchMapper.getEndPointListByServiceName(endPointQueryDto));
        Hit<Span> hit2 = Mockito.mock(Hit.class);
        Hit<Span> hit3 = Mockito.mock(Hit.class);
        Mockito.when(searchResponse.hits().hits()).thenReturn(List.of(hit1, hit2, hit3));
        Span span1 = Mockito.mock(Span.class);
        Span span2 = Mockito.mock(Span.class);
        Span span3 = Mockito.mock(Span.class);
        Mockito.when(hit1.source()).thenReturn(span1);
        Mockito.when(hit2.source()).thenReturn(span2);
        Mockito.when(hit3.source()).thenReturn(span3);
        EndPoint tmpEndPoint1 = Mockito.mock(EndPoint.class);
        EndPoint tmpEndPoint2 = Mockito.mock(EndPoint.class);
        Mockito.when(span1.getLocalEndPoint()).thenReturn(tmpEndPoint1);
        Mockito.when(span2.getLocalEndPoint()).thenReturn(tmpEndPoint1);
        Mockito.when(span3.getLocalEndPoint()).thenReturn(tmpEndPoint2);
        Assertions.assertNotNull(
                elasticsearchMapper.getEndPointListByServiceName(endPointQueryDto));
    }
}
