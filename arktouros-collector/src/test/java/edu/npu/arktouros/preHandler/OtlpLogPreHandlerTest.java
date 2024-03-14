package edu.npu.arktouros.preHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import io.opentelemetry.proto.trace.v1.TracesData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : {@link OtlpLogPreHandler}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class OtlpLogPreHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testHandleJson() {
        String json = """
                {
                    "name": "wangminan",
                    "age": "{1{8"
                }
                """;
        for (int i = 0; i < json.length(); i++) {
            System.out.print(json.charAt(i) + " ");
        }
    }

    @Test
    public void testHandleResourceSpan() throws IOException {
        String traceDataStr = """
                {"resourceSpans":[{"resource":{"attributes":[{"key":"resource-attr","value":{"stringValue":"resource-attr-val-1"}}]},"scopeSpans":[{"scope":{},"spans":[{"traceId":"","spanId":"","parentSpanId":"","name":"operationA","startTimeUnixNano":"1581452772000000321","endTimeUnixNano":"1581452773000000789","droppedAttributesCount":1,"events":[{"timeUnixNano":"1581452773000000123","name":"event-with-attr","attributes":[{"key":"span-event-attr","value":{"stringValue":"span-event-attr-val"}}],"droppedAttributesCount":2},{"timeUnixNano":"1581452773000000123","name":"event","droppedAttributesCount":2}],"droppedEventsCount":1,"status":{"message":"status-cancelled","code":2}},{"traceId":"","spanId":"","parentSpanId":"","name":"operationB","startTimeUnixNano":"1581452772000000321","endTimeUnixNano":"1581452773000000789","links":[{"traceId":"","spanId":"","attributes":[{"key":"span-link-attr","value":{"stringValue":"span-link-attr-val"}}],"droppedAttributesCount":4},{"traceId":"","spanId":"","droppedAttributesCount":1}],"droppedLinksCount":3,"status":{}}]}]}]}
                """;
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(traceDataStr, builder);
        TracesData build = builder.build();
        log.info("traceDataStr: {}", build);
    }
}
