package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.vo.R;

public interface SearchService {
    R getServiceList(BaseQueryDto queryDto);

    R getServiceTopology(String namespace);

    R getLogList(LogQueryDto logQueryDto);

    R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto);

    R getSpanTopologyByTraceId(String traceId);

    R getMetrics(MetricQueryDto metricQueryDto);
}
