package edu.npu.arktouros;

import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ApplicationStartUpTest {

    private MockedStatic<PropertiesProvider> providerMockedStatic;

    @BeforeEach
    void setUp() {
        providerMockedStatic = Mockito.mockStatic(PropertiesProvider.class);
        providerMockedStatic
                .when(() ->
                        PropertiesProvider.getProperty(
                                "server.port", "50052"))
                .thenReturn("50052");
    }

    @AfterEach
    void tearDown() {
        providerMockedStatic.close();
        Mockito.clearAllCaches();
    }

    @Test
    void testMainMethod() {
        String[] args = {};
        // 运行 main 方法并验证行为
        Assertions.assertDoesNotThrow(() -> {
            ApplicationStartUp.main(args);
        });
    }
}
