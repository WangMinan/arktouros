package edu.npu.arktouros.receiver;

import edu.npu.arktouros.cache.AbstractCache;

public interface ReceiverFactory {
    AbstractReceiver createReceiver(AbstractCache outputCache);
}
