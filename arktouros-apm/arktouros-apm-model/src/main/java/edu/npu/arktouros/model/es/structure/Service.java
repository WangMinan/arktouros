package edu.npu.arktouros.model.es.structure;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import edu.npu.arktouros.model.es.basic.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 对标otel的resource 表示一个服务 每个trace应该包含多个service
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Service extends Source {
    private static final String DEFAULT_NAMESPACE = "default";
    private String nodeId;
    private SourceType type = SourceType.SERVICE;
    private String nameSpace;
    // GET_XXX; POST_XXX; RPC_XXX
    private String nodeName;
    private int latency;
    private int httpStatusCode;
    private String rpcStatusCode;
    // healthy 1; unhealthy 0
    private boolean status;
    // 留作扩展
    private List<Tag> tags = new ArrayList<>();

    public Service(String name) {
        this.nameSpace = DEFAULT_NAMESPACE;
        this.name = name;
        this.id = generateServiceId();
    }

    public Service(String nameSpace, String name) {
        this.nameSpace = nameSpace;
        this.name = name;
        this.id = generateServiceId();
    }

    private String generateServiceId() {
        String fullName = "service." + nameSpace + "." + name;
        return new String(
                Base64.getEncoder().encode(fullName.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
    }
}
