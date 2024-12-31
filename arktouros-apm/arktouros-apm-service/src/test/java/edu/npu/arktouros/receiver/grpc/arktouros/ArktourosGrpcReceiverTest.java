package edu.npu.arktouros.receiver.grpc.arktouros;

import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author : [wangminan]
 * @description : {@link ArktourosGrpcReceiver}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ArktourosGrpcReceiverTest {

    @Test
    void testMain() {
        SinkService sinkService = Mockito.mock(SinkService.class);
        ArktourosGrpcReceiver receiver = new ArktourosGrpcReceiver(sinkService, 50058);
        receiver.start();
        receiver.stop();
    }
}
