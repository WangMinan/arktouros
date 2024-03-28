package edu.npu.arktouros.model.otel.trace;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 链路中的基础节点
 */
@Data
public class Span implements Source {
    private String name;
    private String id;
    private String serviceName;
    private String traceId;
    private String parentSpanId;
    private EndPoint localEndPoint;
    private EndPoint remoteEndPoint;
    private Long startTime;
    private Long endTime;
    private boolean isRoot;
    private SourceType type = SourceType.SPAN;
    private List<Tag> tags;

    @Builder
    public Span(String id, String serviceName, String name, String traceId,
                String parentSpanId, EndPoint localEndPoint, EndPoint remoteEndPoint,
                Long startTime, Long endTime, boolean isRoot, @Singular List<Tag> tags) {
        this.id = id;
        this.serviceName = serviceName;
        this.name = name;
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.localEndPoint = localEndPoint;
        this.remoteEndPoint = remoteEndPoint;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isRoot = isRoot;
        this.tags = tags;
    }
}
