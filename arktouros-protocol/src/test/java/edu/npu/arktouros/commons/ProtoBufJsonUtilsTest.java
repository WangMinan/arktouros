package edu.npu.arktouros.commons;

import io.opentelemetry.proto.logs.v1.ResourceLogs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * @author : [wangminan]
 * @description : {@link ProtoBufJsonUtils}
 */
@ExtendWith({MockitoExtension.class})
// 要加这个配置 不然对any的类型推断有很严格的限制
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ProtoBufJsonUtilsTest {

    /**
     * 怎么cover私有构造函数？我只演示这一遍
     */
    @Test
    void testConstructor() throws NoSuchMethodException {
        Constructor<ProtoBufJsonUtils> constructor = ProtoBufJsonUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Assertions.assertThrows(Exception.class, constructor::newInstance);
    }

    @Test
    void testToJSON() throws IOException {
        ResourceLogs resourceLogs = ResourceLogs.newBuilder().build(); // replace with a valid Message instance
        String actualJson = ProtoBufJsonUtils.toJSON(resourceLogs);
        Assertions.assertEquals("{\n}", actualJson);
    }

    @Test
    void testFromJSON() throws IOException {
        String json = "{\n}"; // replace with a valid JSON string
        ResourceLogs.Builder builder = ResourceLogs.newBuilder();
        ProtoBufJsonUtils.fromJSON(json, builder);
        ResourceLogs build = builder.build();
        Assertions.assertNotNull(build);
    }
}
