package edu.npu.emit;

import edu.npu.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : 抽象发射器
 */
public abstract class AbstractEmitter extends Thread{

    protected final AbstractCache inputCache;

    public AbstractEmitter(AbstractCache inputCache) {
        super();
        this.setName("emitter-thread");
        this.inputCache = inputCache;
    }
}
