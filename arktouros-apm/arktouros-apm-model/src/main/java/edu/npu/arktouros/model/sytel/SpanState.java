package edu.npu.arktouros.model.sytel;

import lombok.Getter;

@Getter
public enum SpanState {
    // 枚举
    FINISHED("FINISHED");

    private final String state;

    SpanState(String state) {
        this.state = state;
    }

    public SpanState fromValue(String state) {
        for (SpanState spanState : SpanState.values()) {
            if (spanState.state.equals(state)) {
                return spanState;
            }
        }
        return null;
    }
}
