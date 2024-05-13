package edu.npu.arktouros.emitter;

import edu.npu.arktouros.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : 抽象发射器
 */
public abstract class AbstractEmitter extends Thread {

    protected static AbstractCache inputCache;

    protected AbstractEmitter(AbstractCache inputCache) {
        super();
        this.setName("emitter-thread");
        AbstractEmitter.inputCache = inputCache;
    }

    public AbstractCache getInputCache() {
        return inputCache;
    }
}
