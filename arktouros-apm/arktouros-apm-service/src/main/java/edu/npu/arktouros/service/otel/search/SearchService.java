package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
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

    int getSpanCount(Service service, long startTime, long endTime);
}
