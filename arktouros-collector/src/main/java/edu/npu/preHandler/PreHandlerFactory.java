package edu.npu.preHandler;

import edu.npu.cache.AbstractCache;

public interface PreHandlerFactory {
    AbstractPreHandler createPreHandler(AbstractCache inputCache, AbstractCache outputCache);
}
