package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.analyzer.otel.util.OtelAnalyzerUtil;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Status;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : 链路分析模块
 */
@Component
@Slf4j
public class OtelTraceAnalyzer extends DataAnalyzer {

    @Resource
    private TraceQueueService queueService;

    public OtelTraceAnalyzer() {
        this.setName("OtelTraceAnalyzer");
    }

    @Override
    public void run() {
        log.info("OtelTraceAnalyzer start to transform data");
        while (!isInterrupted()) {
            transform();
        }
    }

    public void handle(ResourceSpans resourceSpans) {
        try {
            String resourceSpansJson = ProtoBufJsonUtils.toJSON(resourceSpans);
            TraceQueueItem logQueueItem = TraceQueueItem.builder()
                    .data(resourceSpansJson).build();
            queueService.put(logQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceSpans:{} to json", resourceSpans, e);
            throw new RuntimeException(e);
        }
    }

    public void transform() {
        TraceQueueItem item = queueService.get();
        if (item != null && StringUtils.isNotEmpty(item.getData())) {
            log.info("OtelTraceAnalyzer start to transform data");
        } else {
            log.warn("OtelTraceAnalyzer get null data from queue, continue for next.");
            return;
        }
        try {
            ResourceSpans.Builder builder = ResourceSpans.newBuilder();
            ProtoBufJsonUtils.fromJSON(item.getData(), builder);
            ResourceSpans resourceSpans = builder.build();
            Map<String, String> attributes =
                    OtelAnalyzerUtil.convertAttributesToMap(
                            resourceSpans.getResource().getAttributesList());
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                extractScopeTag(scopeSpans.getScope(), attributes);
                scopeSpans.getSpansList().forEach(otelSpan -> {
                    convertAndSinkSpan(otelSpan, attributes);
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getServiceNameFromAttributes(Map<String, String> attributes) {
        String name = null;
        name = getServiceNameFromAttributes(name, attributes, "service.name", false);
        name = getServiceNameFromAttributes(name, attributes, "faas.name", true);
        name = getServiceNameFromAttributes(name, attributes, "k8s.deployment.name", true);
        name = getServiceNameFromAttributes(name, attributes, "process.executable.name", true);

        return name;
    }

    private String getServiceNameFromAttributes(String serviceName, Map<String, String> resourceTags, String tagKey, boolean addingSource) {
        if (StringUtils.isNotEmpty(serviceName)) {
            return serviceName;
        }

        String name = resourceTags.get(tagKey);
        if (StringUtils.isNotEmpty(name)) {
            if (addingSource) {
                resourceTags.remove(tagKey);
                resourceTags.put("otlp.service.name.source", tagKey);
            }
            return name;
        }
        return "";
    }

    private void convertAndSinkSpan(io.opentelemetry.proto.trace.v1.Span otelSpan,
                                    Map<String, String> attributes) {
        Map<String, String> tags = aggregateSpanTags(otelSpan.getAttributesList(), attributes);
        tags.put("w3c.tracestate", otelSpan.getTraceState());
        Span.SpanBuilder builder = Span.builder();
        String serviceName = getServiceNameFromAttributes(tags);
        builder.name(otelSpan.getName()).serviceName(serviceName);
        if (otelSpan.getTraceId().isEmpty()) {
            throw new IllegalArgumentException("TraceId is empty in span: " + otelSpan.getName());
        }
        builder
                .traceId(OtelAnalyzerUtil.convertSpanId(otelSpan.getTraceId()));
        if (otelSpan.getSpanId().isEmpty()) {
            throw new IllegalArgumentException("SpanId is empty in span: " + otelSpan.getName());
        }
        builder.id(OtelAnalyzerUtil.convertSpanId(otelSpan.getSpanId()))
                .parentSpanId(OtelAnalyzerUtil.convertSpanId(otelSpan.getParentSpanId()))
                .startTime(TimeUnit.NANOSECONDS.toMicros(otelSpan.getStartTimeUnixNano()))
                .endTime(TimeUnit.NANOSECONDS.toMicros(otelSpan.getStartTimeUnixNano()));
        final Set<String> redundantKeys = new HashSet<>();
        builder.localEndPoint((convertEndpointFromTags(tags, serviceName,
                false, redundantKeys)));
        builder.remoteEndPoint(convertEndpointFromTags(tags, "",
                true, redundantKeys));
        for (String key : redundantKeys) {
            tags.remove(key);
        }
        populateStatus(otelSpan.getStatus(), tags);
        // TODO EMIT
    }

    private void populateStatus(Status status, Map<String, String> tags) {
        if (status.getCode() == Status.StatusCode.STATUS_CODE_ERROR) {
            tags.put("error", "true");
        } else {
            tags.remove("error");
        }

        if (status.getCode() == Status.StatusCode.STATUS_CODE_UNSET) {
            return;
        }

        tags.put("otel.status_code", status.getCode().name());
        if (StringUtils.isNotEmpty(status.getMessage())) {
            tags.put("otel.status_description", status.getMessage());
        }
    }

    private EndPoint convertEndpointFromTags(
            Map<String, String> resourceTags,
            String localServiceName, boolean isRemote, Set<String> redundantKeys) {
        EndPoint.EndPointBuilder builder = EndPoint.builder();
        String serviceName = localServiceName;
        String tmpVal;
        if (isRemote && StringUtils.isNotEmpty(tmpVal = getAndPutRedundantKey(
                resourceTags, "peer.service", redundantKeys))) {
            serviceName = tmpVal;
        } else if (isRemote &&
                StringUtils.isNotEmpty(tmpVal = getAndPutRedundantKey(
                        resourceTags, "net.peer.name", redundantKeys)) &&
                // if it's not IP, then define it as service name
                !parseIp(tmpVal)) {
            serviceName = tmpVal;
        }

        String ipKey, portKey;
        if (isRemote) {
            ipKey = "net.peer.ip";
            portKey = "net.peer.port";
        } else {
            ipKey = "net.host.ip";
            portKey = "net.host.port";
        }

        boolean ipParseSuccess = false;
        if (StringUtils.isNotEmpty(tmpVal = getAndPutRedundantKey(
                resourceTags, ipKey, redundantKeys))) {
            if (!(ipParseSuccess = parseIp(tmpVal))) {
                // if ip parse failed, use the value as service name
                serviceName = StringUtils.isEmpty(serviceName) ? tmpVal : serviceName;
            }
        }
        if (StringUtils.isNotEmpty(tmpVal = getAndPutRedundantKey(
                resourceTags, portKey, redundantKeys))) {
            builder.port(Integer.parseInt(tmpVal));
        }
        if (StringUtils.isEmpty(serviceName) && !ipParseSuccess) {
            return null;
        }

        builder.serviceName(serviceName);
        return builder.build();
    }

    private boolean parseIp(String tmpVal) {
        // 是否符合ipv4或ipv6 用正则
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4})$";
        return tmpVal.matches(ipv4Pattern) || tmpVal.matches(ipv6Pattern);
    }

    private String getAndPutRedundantKey(Map<String, String> resourceTags, String key, Set<String> redundantKeys) {
        String val = resourceTags.get(key);
        if (StringUtils.isEmpty(val)) {
            return null;
        }
        redundantKeys.add(key);
        return val;
    }

    private Map<String, String> aggregateSpanTags(List<KeyValue> spanAttrs, Map<String, String> resourceTags) {
        final HashMap<String, String> result = new HashMap<>();
        result.putAll(resourceTags);
        result.putAll(OtelAnalyzerUtil.convertAttributesToMap(spanAttrs));
        return result;
    }

    private void extractScopeTag(InstrumentationScope scope, Map<String, String> resourceTags) {
        if (scope == null) {
            return;
        }

        if (StringUtils.isNotEmpty(scope.getName())) {
            resourceTags.put("otel.library.name", scope.getName());
        }
        if (StringUtils.isNotEmpty(scope.getVersion())) {
            resourceTags.put("otel.library.version", scope.getVersion());
        }
    }

    @Override
    public void interrupt() {
        log.info("OtelTraceAnalyzer is shutting down.");
        super.interrupt();
    }

    public void sink() {

    }
}
