package edu.npu.arktouros.model.es.trace;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 一条标准链路
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Trace extends Source {
    private SourceType type = SourceType.TRACE;
    private List<Span> spans = new ArrayList<>();

    @Builder
    public Trace(String id, @Singular List<Span> spans) {
        this.id = id;
        this.spans = spans;
    }
}
