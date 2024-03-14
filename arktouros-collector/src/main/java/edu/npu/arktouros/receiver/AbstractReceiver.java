package edu.npu.arktouros.receiver;

import edu.npu.arktouros.cache.AbstractCache;

/**
 * @author : [wangminan]
 * @description : [抽象接收器]
 */
public abstract class AbstractReceiver extends Thread {
    protected final AbstractCache outputCache;

    public AbstractReceiver(AbstractCache outputCache) {
        super();
        this.setName("receiver-thread");
        this.outputCache = outputCache;
    }
}
