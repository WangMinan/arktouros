package edu.npu.preHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author : [wangminan]
 * @description : {@link OtlpLogPreHandler}
 */
// 引入junit
@ExtendWith(MockitoExtension.class)
public class OtlpLogPreHandlerTest {

    @Test
    public void testHandleJson() {
        String json = "{\\\"key\\\": \\\"{value\"}";
        for (int i = 0; i < json.length(); i++) {
            System.out.println(json.charAt(i));
        }
    }
}
