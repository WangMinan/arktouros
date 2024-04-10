package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.model.otel.structure.EndPoint;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : endPoint和TraceId的整合
 */
public record EndPointTraceIdDto (
        EndPoint endPoint,
        List<String> traceIds
) {
}
