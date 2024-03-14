package edu.npu.preHandler;

import edu.npu.cache.AbstractCache;

import java.util.Stack;

/**
 * @author : [wangminan]
 * @description : Otlp日志预处理器
 */
public class OtlpLogPreHandler extends AbstractPreHandler{

    private final StringBuilder cacheStringBuilder = new StringBuilder();

    public OtlpLogPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
        super(inputCache, outputCache);
    }

    @Override
    public void run() {
        while (true) {
            handle();
        }
    }

    public void handle() {
        String input =
                cacheStringBuilder.append(inputCache.get().trim()).toString();
        if (!input.startsWith("{")) {
            throw new IllegalArgumentException("Invalid input for json: " + input);
        }
        // 开始做大括号匹配 匹配部分扔出去 剩下的放cache里
        Stack<Character> stack = new Stack<>();
        boolean isInStrFlag = false; // 游标是否正在字符串中
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                isInStrFlag = !isInStrFlag;
            } else if (c == '{' && !isInStrFlag) {
                stack.push('{');
            } else if (c == '}' && !isInStrFlag) {
                stack.pop();
                if (stack.isEmpty()) {
                    outputCache.put(cacheStringBuilder.substring(0, i + 1));
                    cacheStringBuilder.delete(0, i + 1);
                }
            }
        }
    }

    public static class Factory implements PreHandlerFactory {

        @Override
        public AbstractPreHandler createPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
            return new OtlpLogPreHandler(inputCache, outputCache);
        }
    }
}
