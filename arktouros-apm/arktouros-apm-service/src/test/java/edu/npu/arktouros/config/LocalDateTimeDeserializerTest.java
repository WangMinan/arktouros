package edu.npu.arktouros.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@ExtendWith({MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class LocalDateTimeDeserializerTest {

    private final LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer();

    @Test
    void testDeserialize() throws Exception {
        JsonParser jsonParser = Mockito.mock(JsonParser.class);
        DeserializationContext deserializationContext = Mockito.mock(DeserializationContext.class);
        Mockito.when(jsonParser.getText()).thenReturn("2022-01-01T00:00:00");
        LocalDateTime result = deserializer.deserialize(jsonParser, deserializationContext);
        Assertions.assertEquals(
                LocalDateTime.of(2022, 1, 1, 0, 0, 0),
                result);
        Mockito.when(jsonParser.getText()).thenReturn("2022-01-01ABC00:00:00");
        Assertions.assertThrows(RuntimeException.class,
                () -> deserializer.deserialize(jsonParser, deserializationContext));
    }

    @Test
    void testHandledType() {
        Class<?> result = deserializer.handledType();
        Assertions.assertEquals(LocalDateTime.class, result);
    }
}
