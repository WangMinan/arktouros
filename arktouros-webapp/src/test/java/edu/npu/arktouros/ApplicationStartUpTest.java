package edu.npu.arktouros;

import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import edu.npu.arktouros.config.PropertiesProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;

public class ApplicationStartUpTest {

    private MockedStatic<PropertiesProvider> mockedPropertiesProvider;
    private MockedStatic<Server> mockedServer;

    @BeforeEach
    public void setUp() {
        mockedPropertiesProvider = Mockito.mockStatic(PropertiesProvider.class);
        mockedServer = Mockito.mockStatic(Server.class);
    }

    @AfterEach
    public void tearDown() {
        mockedPropertiesProvider.close();
        mockedServer.close();
    }

    @Test
    public void testMain() {
        // Arrange
        String[] args = new String[]{};
        ServerBuilder serverBuilder = mock(ServerBuilder.class);
        Mockito.when(serverBuilder.port(anyInt(), any(SessionProtocol.class)))
                .thenReturn(serverBuilder);
        Mockito.when(serverBuilder.serviceUnder(anyString(), any()))
                .thenReturn(serverBuilder);
        Mockito.when(serverBuilder.build()).thenReturn(mock(Server.class));
        Mockito.when(Server.builder()).thenReturn(serverBuilder);
        Mockito.when(PropertiesProvider.getProperty(anyString(), anyString()))
                .thenReturn("50052");

        ApplicationStartUp.addShutdownHook();

        Assertions.assertThrows(NullPointerException.class,
                () -> ApplicationStartUp.main(args));
    }
}
