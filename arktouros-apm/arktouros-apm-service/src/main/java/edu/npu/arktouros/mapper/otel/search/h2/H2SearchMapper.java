package edu.npu.arktouros.mapper.otel.search.h2;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : H2搜索Mapper
 */
public class H2SearchMapper extends SearchMapper {
    @Override
    public R getServiceList(BaseQueryDto queryDto) {
        return R.ok();
    }

    @Override
    public List<Service> getServiceListFromNamespace(String namespace) {
        return List.of();
    }

    @Override
    public List<Span> getSpanListByServiceNames(List<String> serviceNames) {
        return List.of();
    }

    @Override
    public Service getServiceByName(String serviceName) {
        return null;
    }

    @Override
    public R getLogListByQuery(LogQueryDto logQueryDto) {
        return R.ok();
    }

    @Override
    public R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto) {
        return null;
    }

    @Override
    public List<Span> getSpanListByTraceId(String traceId) {
        return List.of();
    }

    @Override
    public List<String> getMetricsNames(String serviceName, Integer metricNameLimit) {
        return List.of();
    }

    @Override
    public List<String> getMetricsValues(List<String> metricNames, Long startTimestamp, Long endTimestamp) {
        return List.of();
    }

}
