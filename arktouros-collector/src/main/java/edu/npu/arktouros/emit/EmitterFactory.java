package edu.npu.arktouros.emit;

import edu.npu.arktouros.cache.AbstractCache;

public interface EmitterFactory {
    AbstractEmitter createEmitter(AbstractCache inputCache);
}
