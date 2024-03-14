package edu.npu.arktouros.commons;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

/**
 * @author : [wangminan]
 * @description : jackson配置类
 */
@Getter
public class JacksonConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static JacksonConfig instance = new JacksonConfig();

    private JacksonConfig() {
        objectMapper.getFactory()
                .setStreamReadConstraints(
                        StreamReadConstraints.builder().maxStringLength(10_000_000).build());
    }
}
