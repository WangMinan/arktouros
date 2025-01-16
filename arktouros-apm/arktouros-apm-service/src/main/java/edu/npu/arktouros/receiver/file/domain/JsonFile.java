package edu.npu.arktouros.receiver.file.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * @author : [wangminan]
 * @description : 接收器用的JSON文件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class JsonFile {
    private File file;
    private FileStatus status;

    public JsonFile(File file) {
        this.file = file;
        this.status = FileStatus.UNREAD;
    }
}
