package edu.npu.arktouros.service.sinker;

import edu.npu.arktouros.model.otel.Source;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
public abstract class SinkService {
    // 是否准备完成
    protected boolean ready = false;

    public void init() {
        this.setReady(true);
    }


    public abstract void sink(Source source) throws IOException;
}
