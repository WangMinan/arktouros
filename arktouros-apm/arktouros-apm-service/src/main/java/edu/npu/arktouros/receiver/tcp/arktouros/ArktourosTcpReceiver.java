package edu.npu.arktouros.receiver.tcp.arktouros;

import com.fasterxml.jackson.databind.JsonNode;
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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Stack;

/**
 * @author : [wangminan]
 * @description : 自有协议数据TCP接收器 netty实现
 */
@SuppressWarnings("CallToPrintStackTrace")
@Slf4j
public class ArktourosTcpReceiver extends DataReceiver {

    private final int tcpPort;
    private final StringBuilder cacheStringBuilder;
    private final SinkService sinkService;
    private final ObjectMapper objectMapper;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public ArktourosTcpReceiver(SinkService sinkService, int tcpPort,
                                ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.tcpPort = tcpPort;
        this.cacheStringBuilder = new StringBuilder();
        this.objectMapper = objectMapper;
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
    }

    @Override
    public void start() {
        try {
            log.info("Starting arktouros tcp receiver on port: {}", tcpPort);
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            ChannelInitializer<NioSocketChannel> channelInitializer = new ChannelInitializer<>() {
                @Override
                protected void initChannel(NioSocketChannel channel) {
                    channel.pipeline()
                            .addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    ByteBuf byteBuf = (ByteBuf) msg;
                                    log.debug("Tcp netty receiver receives data: {}", byteBuf.toString(Charset.defaultCharset()));
                                    cacheStringBuilder.append(byteBuf.toString(Charset.defaultCharset()));
                                    handleChannelInput();
                                    // 如果有响应要写就放在这个位置
                                    // ctx.writeAndFlush();
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    log.error("Error caught by tcp receiver when handling channel input.");
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                }
            };
            serverBootstrap.childHandler(channelInitializer);
            // 异步绑定端口，不阻塞当前线程
            channelFuture = serverBootstrap.bind(tcpPort);
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("Arktouros tcp receiver started on port: {}", tcpPort);
                } else {
                    log.error("Failed to start arktouros tcp receiver on port: {}", tcpPort);
                    future.cause().printStackTrace();
                }
            });
            // 等待通道关闭的异步回调
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                if (closeFuture.isSuccess()) {
                    log.info("Arktouros tcp receiver closed successfully.");
                } else {
                    log.error("Error closing arktouros tcp receiver.");
                    closeFuture.cause().printStackTrace();
                }

                // 通道关闭后，才执行资源的清理工作
                bossGroup.shutdownGracefully().addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("Boss group shutdown gracefully.");
                    } else {
                        log.error("Error shutting down boss group.");
                    }
                });
                workerGroup.shutdownGracefully().addListener(future -> {
                    if (future.isSuccess()) {
                        log.info("Worker group shutdown gracefully.");
                    } else {
                        log.error("Error shutting down worker group.");
                    }
                });
            });
        } catch (Exception e) {
            log.error("Error caught by tcp receiver when starting.");
            e.printStackTrace();
        }
    }

    private void handleChannelInput() throws IOException {
        String input = cacheStringBuilder.toString();
        String tmpInput = input.trim();
        if (!tmpInput.startsWith("{")) {
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
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            String type;
            try {
                type = jsonNode.get("type").asText().toLowerCase(Locale.ROOT);
            } catch (NullPointerException npe) {
                // 历史遗留问题
                type = jsonNode.get("sourceType").asText().toLowerCase(Locale.ROOT);
            }
            switch (type) {
                case "log":
                    Log log1 = objectMapper.readValue(jsonStr, Log.class);
                    sinkService.sink(log1);
                    break;
                case "span":
                    Span span = objectMapper.readValue(jsonStr, Span.class);
                    sinkService.sink(span);
                    break;
                case "metric":
                    String metricType = jsonNode.get("metricType").asText().toLowerCase(Locale.ROOT);
                    switch (metricType) {
                        case "gauge":
                            Gauge gauge = objectMapper.readValue(jsonStr, Gauge.class);
                            sinkService.sink(gauge);
                            break;
                        case "counter":
                            Counter metric = objectMapper.readValue(jsonStr, Counter.class);
                            sinkService.sink(metric);
                            break;
                        case "summary":
                            Summary summary = objectMapper.readValue(jsonStr, Summary.class);
                            sinkService.sink(summary);
                            break;
                        case "histogram":
                            Histogram histogram = objectMapper.readValue(jsonStr, Histogram.class);
                            sinkService.sink(histogram);
                            break;
                        default:
                            log.warn("Unknown metric type:{}", jsonStr);
                    }
                    break;
                default:
                    log.warn("Unknown json type:{}", jsonStr);
            }
        } catch (RuntimeException e) {
            log.error("Encountered an error while handling json from tcp. Trying to recover.");
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        log.info("Tcp receiver shutdown. All unreceived data will be lost. Waiting for worker groups shutting down.");
        channelFuture.channel().close();
    }
}
