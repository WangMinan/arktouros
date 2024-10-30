package edu.npu.arktouros.model.sytel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author : [wangminan]
 * @description : 沈阳遥测span数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "spanId")
public class Span {
    private String spanId;
    private String parentId;
    private String operationName;
    private Long startTime;
    private Long finishTime;
    private Long duration;
    private SpanState spanState;
    private SpanContext spanContext;
}
