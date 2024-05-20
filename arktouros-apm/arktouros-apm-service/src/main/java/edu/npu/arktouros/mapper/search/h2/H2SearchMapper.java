package edu.npu.arktouros.mapper.search.h2;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.otel.metric.Metric;
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
    public R getServiceList(ServiceQueryDto queryDto) {
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
        return R.ok();
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
    public List<Metric> getMetricsValues(List<String> metricNames, String serviceName, Long startTimestamp, Long endTimestamp) {
        return List.of();
    }

    @Override
    public R getNamespaceList(String query) {
        return R.ok();
    }

    @Override
    public R getAllLogLevels(String query) {
        return R.ok();
    }

}
