package edu.npu.arktouros.model.sytel;

import lombok.Data;

/**
 * @author : [wangminan]
 * @description : 用于Sytel的SpanContext
 */
@Data
public class SpanContext {
    private String traceId;
    private String spanId;
}
