package edu.npu.emit;

import edu.npu.cache.AbstractCache;

public interface EmitterFactory {
    AbstractEmitter createEmitter(AbstractCache inputCache);
}
