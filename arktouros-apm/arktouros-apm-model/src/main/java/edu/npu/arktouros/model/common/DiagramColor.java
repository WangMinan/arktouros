package edu.npu.arktouros.model.common;

import lombok.Getter;

@Getter
public enum DiagramColor {

    COLOR_ERROR_RED_RGB("#FF2700"),
    COLOR_ERROR_YELLOW_RGB("#FFEE00"),
    COLOR_NORMAL_GREEN_RGB("#C7EDCC");

    private final String color;

    DiagramColor(String color) {
        this.color = color;
    }
}
