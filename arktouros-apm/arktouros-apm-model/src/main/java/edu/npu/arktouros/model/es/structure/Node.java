package edu.npu.arktouros.model.es.structure;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import edu.npu.arktouros.model.es.basic.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 对标K8S中的node 物理节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Node extends Source {
    private String ip;
    private String hostname;
    private OSType osType;
    private List<Tag> tags;
    private SourceType type = SourceType.NODE;
    private boolean status; // healthy 1; unhealthy 0

    public enum OSType {
        OS_TYPE_LINUX,
        OS_TYPE_WIN,
        OS_TYPE_SOLARIS,
        OS_TYPE_MAC,
        OS_TYPE_FREEBSD,
        OS_TYPE_OTHER
    }
}
