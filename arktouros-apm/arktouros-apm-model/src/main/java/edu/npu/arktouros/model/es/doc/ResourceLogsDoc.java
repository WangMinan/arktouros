package edu.npu.arktouros.model.es.doc;

import edu.npu.arktouros.model.es.base.Resource;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.logs.v1.ResourceLogs} 在ES中的映射
 * 把enum等都简化了 只保留基本的属性 protobuf直接翻译出来的类是没办法用jackson来完成正常的序列化和反序列化的
 */
@Data
@AllArgsConstructor
@Builder
public class ResourceLogsDoc {
    private Resource resource;

    public ResourceLogsDoc(ResourceLogs resourceLogs) {

    }
}
