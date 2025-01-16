package edu.npu.arktouros.receiver.file.domain;

import lombok.Getter;

/**
 * 文件状态 未读取 正在读取 已完成读取
 */
@Getter
public enum FileStatus {
    UNREAD, READING, READ;

    public static FileStatus fromString(String status) {
        return switch (status) {
            case "UNREAD" -> UNREAD;
            case "READING" -> READING;
            case "READ" -> READ;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
}
