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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
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
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private Channel channel;

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
        log.info("Starting arktouros tcp receiver on port: {}", tcpPort);
        // 我也不知道这个netty是哪个依赖引进来的 反正咱有得用是好事
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // boss and worker 进一步提高性能
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel channel) {
                // 添加具体的handler
                channel.pipeline()
                        // head -> add last 加入的处理器 -> tail
                        .addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg)
                                    throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                log.debug("Tcp netty receiver receives data: {}",
                                        byteBuf.toString(Charset.defaultCharset()));
                                cacheStringBuilder.append(
                                        byteBuf.toString(Charset.defaultCharset()));
                                handleChannelInput();
                                // 如果有响应要写就放在这个位置
                                // ctx.writeAndFlush();
                            }

                            // 异常处理
                            @Override
                            public void exceptionCaught(
                                    ChannelHandlerContext ctx, Throwable cause) {
                                log.error("Error caught by tcp receiver when handling channel input. Cause: {}",
                                        Arrays.toString(cause.getStackTrace()));
                                //释放资源
                                ctx.close();
                            }
                        });
            }
        });
        ChannelFuture channelFuture = serverBootstrap.bind(tcpPort);
        log.info("Arktouros tcp receiver started on port: {}", tcpPort);
        channel = channelFuture.channel();
        // 定义channel.close之后会执行的操作 异步写法 同步的话用try catch
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("Arktouros tcp receiver start shutting down NioEventLoopGroups.");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            } else {
                log.error("Arktouros tcp receiver caught error, cause: \n{}",
                        Arrays.stream(future.cause().getStackTrace()).toList());
            }
        });
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
        channel.close();
    }
}
