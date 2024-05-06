package edu.npu.arktouros.emitter;

import edu.npu.arktouros.cache.AbstractCache;
import lombok.Getter;

/**
 * @author : [wangminan]
 * @description : 抽象发射器
 */
@Getter
public abstract class AbstractEmitter extends Thread{

    protected final AbstractCache inputCache;

    public AbstractEmitter(AbstractCache inputCache) {
        super();
        this.setName("emitter-thread");
        this.inputCache = inputCache;
    }
}
