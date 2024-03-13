package edu.npu.preHandler;

import edu.npu.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : 预处理器
 */
public abstract class AbstractPreHandler extends Thread{
    protected final AbstractCache inputCache;
    protected final AbstractCache outputCache;

    public AbstractPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
        super();
        this.setName("preHandler-thread");
        this.inputCache = inputCache;
        this.outputCache = outputCache;
    }
}
