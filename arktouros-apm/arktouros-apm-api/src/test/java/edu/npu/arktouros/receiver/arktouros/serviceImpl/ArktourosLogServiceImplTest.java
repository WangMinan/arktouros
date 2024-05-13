package edu.npu.arktouros.receiver.arktouros.serviceImpl;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.proto.collector.v1.LogRequest;
import edu.npu.arktouros.proto.collector.v1.LogResponse;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link ArktourosLogServiceImpl} 我宣布下面的这些注解是SpringBoot条件下做UT的起手式
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
// 要加这个配置 不然对any的类型推断有很严格的限制
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ArktourosLogServiceImplTest {

    @Test
    void testExport() throws IOException {
        SinkService sinkService = Mockito.mock(SinkService.class);
        // sinkService.sink的时候啥也不需要执行
        Mockito.doNothing().when(sinkService).sink(any(Source.class));
        ArktourosLogServiceImpl arktourosLogService = new ArktourosLogServiceImpl(sinkService);
        StreamObserver<LogResponse> responseObserver = Mockito.mock(StreamObserver.class);
        Mockito.doNothing().when(responseObserver).onNext(any());
        // 这里的参数是随便写的，因为我们在sinkService.sink的时候啥也不需要执行
        Assertions.assertDoesNotThrow(() ->
                arktourosLogService.export(LogRequest.newBuilder().build(), responseObserver));
    }

    @Test
    void testExportError() throws IOException {
        SinkService sinkService = Mockito.mock(SinkService.class);
        // sinkService.sink的时候啥也不需要执行
        Mockito.doThrow(new IOException()).when(sinkService).sink(any(Source.class));
        ArktourosLogServiceImpl arktourosLogService = new ArktourosLogServiceImpl(sinkService);
        StreamObserver<LogResponse> responseObserver = Mockito.mock(StreamObserver.class);
        Mockito.doNothing().when(responseObserver).onNext(any());
        // 这里的参数是随便写的，因为我们在sinkService.sink的时候啥也不需要执行
        Assertions.assertDoesNotThrow(() ->
                arktourosLogService.export(LogRequest.newBuilder().build(), responseObserver));
    }
}
