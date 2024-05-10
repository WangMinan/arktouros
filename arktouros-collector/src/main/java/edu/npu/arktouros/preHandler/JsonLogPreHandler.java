package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.AbstractCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

/**
 * @author : [wangminan]
 * @description : Json日志预处理器
 */
@Slf4j
public class JsonLogPreHandler extends AbstractPreHandler{

    protected final StringBuilder cacheStringBuilder = new StringBuilder();

    public JsonLogPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
        super(inputCache, outputCache);
    }

    @Override
    public void run() {
        log.info("JsonLogPreHandler start working");
        while (true) {
            handle();
        }
    }

    public void handle() {
        log.debug("Formatting input from cache.");
        String input =
                cacheStringBuilder.append(inputCache.get().trim()).toString();
        if (!input.startsWith("{")) {
            throw new IllegalArgumentException("Invalid input for json when handling: " + input);
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
                    outputCache.put(
                            cacheStringBuilder.substring(0, currentPos - lastPos + 1)
                    );
                    cacheStringBuilder.delete(0, currentPos - lastPos + 1);
                    lastPos = currentPos + 1;
                }
            }
        }
    }

    public static class Factory implements PreHandlerFactory {

        @Override
        public AbstractPreHandler createPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
            return new JsonLogPreHandler(inputCache, outputCache);
        }
    }
}
