package edu.npu.receiver;

import edu.npu.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
public abstract class AbstractReceiver extends Thread {
    protected final AbstractCache outputCache;

    public AbstractReceiver(AbstractCache outputCache) {
        super();
        this.setName("receiver-thread");
        this.outputCache = outputCache;
    }
}
