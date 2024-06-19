package edu.npu.arktouros.receiver.tcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Gauge;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled
public class TcpClientTest {

    @Test
    void testTcpClient() throws InterruptedException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Log log1 = Log.builder()
                .serviceName("test")
                .severityText("INFO")
                .content("Tcp connect test 123.")
                .timestamp(System.currentTimeMillis())
                .build();
        Gauge gauge = Gauge.builder()
                .name("test_tcp")
                .description("test tcp send metric")
                .labels(new HashMap<>())
                .value(1.0)
                .timestamp(System.currentTimeMillis())
                .build();
        gauge.setServiceName("test");
        Channel tcpChannel = new Bootstrap()
                // 2. 添加组件
                .group(new NioEventLoopGroup())
                // 3. 选择客户端 channel 实现
                .channel(NioSocketChannel.class)
                // 4. 添加 handler 处理器
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline().addLast(new StringEncoder());
                    }
                })
                // 5. 连接到服务器
                .connect("localhost", 50049)
                .sync()
                .channel();
        tcpChannel.writeAndFlush(mapper.writeValueAsString(log1));
        tcpChannel.writeAndFlush(mapper.writeValueAsString(gauge));
    }
}
