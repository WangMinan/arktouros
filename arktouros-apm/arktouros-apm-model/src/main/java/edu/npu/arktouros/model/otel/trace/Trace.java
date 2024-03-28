package edu.npu.arktouros.model.otel.trace;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 一条标准链路 trace是一个只有在搜索阶段才会用到的概念 不会被持久化
 */
@Data
public class Trace implements Source {
    private SourceType type = SourceType.TRACE;
    private List<Span> spans = new ArrayList<>();

    @Builder
    public Trace(@Singular List<Span> spans) {
        this.spans = spans;
    }
}
