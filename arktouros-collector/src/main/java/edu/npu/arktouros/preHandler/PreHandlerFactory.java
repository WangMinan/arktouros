package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.AbstractCache;

public interface PreHandlerFactory {
    AbstractPreHandler createPreHandler(AbstractCache inputCache, AbstractCache outputCache);
}
