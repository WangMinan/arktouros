package edu.npu.arktouros.emitter;

import edu.npu.arktouros.cache.AbstractCache;

public interface EmitterFactory {
    AbstractEmitter createEmitter(AbstractCache inputCache);
}
