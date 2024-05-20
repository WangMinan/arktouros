package edu.npu.arktouros.analyzer.otel.util;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Constructor;

/**
 * @author : [wangminan]
 * @description : {@link OtelAnalyzerUtil}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class OtelAnalyzerUtilTest {

    @Test
    void testConstructor() throws NoSuchMethodException {
        Constructor<OtelAnalyzerUtil> constructor =
                OtelAnalyzerUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Assertions.assertThrows(Exception.class, constructor::newInstance);
    }

    @Test
    void testConvertSpanIdWithException() {
        // 创建一个包含少于8个字节的ByteString，这样getLong()方法会抛出BufferUnderflowException
        ByteString spanId = ByteString.copyFrom(new byte[]{0, 0, 0, 0});
        String spanName = "testSpanName";
        String result = OtelAnalyzerUtil.convertSpanId(spanId, spanName);
        Assertions.assertNotNull(result);
    }
}
