package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.model.vo.R;

import java.util.List;

public interface SearchService {
    R getServiceList(ServiceQueryDto queryDto);

    R getServiceTopology(String namespace);

    R getLogList(LogQueryDto logQueryDto);

    R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto);

    R getSpanTopologyByTraceId(SpanTopologyQueryDto traceId);

    R getMetrics(MetricQueryDto metricQueryDto);

    R getNamespaceList(String query);

    R getAllLogLevels(String query);

    List<Service> getAllServices();

    int getTraceCount(Service service, long startTime, long endTime);

    List<SpanTreeNode> getSpanTreeInFiveMinutes(String name, String traceId, long startTime, long endTime);

    List<Span> getAllSpans(Service service, long startTime, long endTime);

    List<EndPointTraceIdVo> getEndPointTraceIdVos(EndPointQueryDto endPointQueryDto);
}
