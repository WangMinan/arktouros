package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : 预处理器
 */
public abstract class AbstractPreHandler extends Thread{
    // 所有preHandler共享一个inputCache和outputCache
    protected static AbstractCache inputCache;
    protected static AbstractCache outputCache;

    public AbstractPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
        super();
        this.setName("preHandler-thread");
        AbstractPreHandler.inputCache = inputCache;
        AbstractPreHandler.outputCache = outputCache;
    }

    @Override
    public void run() {
        super.run();
    }
}
