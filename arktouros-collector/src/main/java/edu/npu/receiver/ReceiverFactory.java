package edu.npu.receiver;

import edu.npu.cache.AbstractCache;

public interface ReceiverFactory {
    AbstractReceiver createReceiver(AbstractCache outputCache);
}
