package edu.npu.arktouros.receiver.tcp.arktouros;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Locale;
import java.util.Stack;

/**
 * @author : [wangminan]
 * @description : 自有协议数据TCP接收器 netty实现
 */
@Slf4j
public class ArktourosTcpReceiver extends DataReceiver {

    private final int tcpPort;
    private final StringBuilder cacheStringBuilder;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;

    public ArktourosTcpReceiver(SinkService sinkService, int tcpPort,
                                ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.tcpPort = tcpPort;
        this.cacheStringBuilder = new StringBuilder();
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        log.info("Starting arktouros tcp receiver on port: {}", tcpPort);
        // 我也不知道这个netty是哪个依赖引进来的 反正咱有得用是好事
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        // 添加具体的handler
                        channel.pipeline().addLast(new StringDecoder());
                        channel.pipeline().addLast(new StringEncoder());
                        channel.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(
                                    ChannelHandlerContext channelHandlerContext, String inputStr)
                                    throws Exception {
                                cacheStringBuilder.append(inputStr.trim());
                                handleChannelInput();
                            }
                        });
                    }
                })
                .bind(tcpPort);
    }

    private void handleChannelInput() throws IOException {
        String input = cacheStringBuilder.toString();
        if (!input.startsWith("{")) {
            // 直接就挂了
            log.warn("Invalid input for json when handling: {}", input);
            throw new ArktourosException("Invalid input for json when handling: " + input);
        }
        // 开始做大括号匹配 匹配部分扔出去 剩下的放cache里
        Stack<Character> stack = new Stack<>();
        boolean isInStrFlag = false; // 游标是否正在字符串中
        int lastPos = 0;
        int currentPos;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                isInStrFlag = !isInStrFlag;
            } else if (c == '{' && !isInStrFlag) {
                stack.push('{');
            } else if (c == '}' && !isInStrFlag) {
                stack.pop();
                if (stack.isEmpty()) {
                    currentPos = i;
                    log.debug("Outputting formatted json to cache.");
                    persistInput(cacheStringBuilder.substring(0, currentPos - lastPos + 1));
                    cacheStringBuilder.delete(0, currentPos - lastPos + 1);
                    lastPos = currentPos + 1;
                }
            }
        }
    }

    private void persistInput(String jsonStr) throws IOException {
        log.debug("Sinking an object in json:{}", jsonStr);
        // 结构相似 都能转 所以只能用关键字试探
        // 深拷贝
        String tmpJson = jsonStr.trim();
        tmpJson = tmpJson.replaceAll("\\s*|\r|\n|\t", "")
                .toLowerCase(Locale.ROOT);
        if (tmpJson.contains("\"type\":\"log\"")) {
            Log log = objectMapper.readValue(jsonStr, Log.class);
            sinkService.sink(log);
        } else if (tmpJson.contains("\"type\":\"span\"")) {
            Span span = objectMapper.readValue(jsonStr, Span.class);
            sinkService.sink(span);
        } else if (tmpJson.contains("\"metrictype:\"gauge\"")) {
            Gauge gauge = objectMapper.readValue(jsonStr, Gauge.class);
            sinkService.sink(gauge);
        } else if (tmpJson.contains("\"metrictype:\"counter\"")) {
            Counter metric = objectMapper.readValue(jsonStr, Counter.class);
            sinkService.sink(metric);
        } else if (tmpJson.contains("\"metrictype:\"summary\"")) {
            Summary metric = objectMapper.readValue(jsonStr, Summary.class);
            sinkService.sink(metric);
        } else if (tmpJson.contains("\"metrictype:\"histogram\"")) {
            Histogram metric = objectMapper.readValue(jsonStr, Histogram.class);
            sinkService.sink(metric);
        } else {
            log.warn("Unknown json type:{}", jsonStr);
        }
    }

    @Override
    public void stop() {
        log.info("Tcp receiver shutdown. All unreceived data will be lost.");
    }
}
