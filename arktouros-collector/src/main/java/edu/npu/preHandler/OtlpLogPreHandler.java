package edu.npu.preHandler;

import edu.npu.cache.AbstractCache;

import java.util.Stack;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
public class OtlpLogPreHandler extends AbstractPreHandler{

    private StringBuilder cacheStringBuilder = new StringBuilder();

    public OtlpLogPreHandler(AbstractCache inputCache, AbstractCache outputCache) {
        super(inputCache, outputCache);
    }

    @Override
    public void run() {

    }

    public void handle() {
        String input =
                cacheStringBuilder.append(inputCache.get().trim()).toString();
        if (!input.startsWith("{")) {
            throw new IllegalArgumentException("Invalid input for json: " + input);
        }
        // 开始做大括号匹配 匹配部分扔出去 剩下的放cache里
        Stack<Character> stack = new Stack<>();
        StringBuilder result = new StringBuilder();
        boolean isInStrFlag = false; // 游标是否正在字符串中
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);


        }
    }
}
