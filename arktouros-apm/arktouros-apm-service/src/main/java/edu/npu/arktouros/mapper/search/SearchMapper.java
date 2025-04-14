package edu.npu.arktouros.mapper.search;

import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanNamesQueryDto;
import edu.npu.arktouros.model.dto.SpanTimesQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.model.vo.SpanTimesVo;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 抽象搜索Mapper
 */
public abstract class SearchMapper {
    public abstract R getServiceList(ServiceQueryDto queryDto);

    public abstract List<Service> getServiceListFromTopologyQuery(String namespace);

    public abstract List<Span> getSpanListByServiceNames(List<String> serviceNames);

    public abstract Service getServiceByName(String serviceName);

    public abstract R getLogListByQuery(LogQueryDto logQueryDto);

    public abstract R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto);

    public abstract List<Span> getSpanListBySpanNameAndServiceName(SpanTimesQueryDto spanTimesQueryDto);

    public abstract List<Span> getSpanListByTraceId(SpanTopologyQueryDto spanTopologyQueryDto);

    public abstract List<String> getMetricsNames(String serviceName, Integer metricNameLimit);

    public abstract List<Metric> getMetricsValues(List<String> metricNames, String serviceName,
                                                  Long startTimestamp, Long endTimestamp);

    public abstract R getNamespaceList(String query);

    public abstract R getAllLogLevels(String query);

    public abstract List<Service> getAllServices();

    public abstract int getTraceCount(Service service, long startTime, long endTime);

    public abstract List<Span> getSpanListByTraceId(String serviceName, String traceId, long startTime, long endTime);

    public abstract List<Span> getAllSpans(Service service, long startTime);

    public abstract List<EndPointTraceIdVo> getEndPointTraceIdVos(EndPointQueryDto endPointQueryDto);

    public abstract R getSpanNamesByServiceName(SpanNamesQueryDto spanNamesQueryDto);

    public abstract SpanTimesVo getSpanTimesVoBySpanName(SpanTimesQueryDto spanTimesQueryDto);

    public abstract R getTimeRange();
}
