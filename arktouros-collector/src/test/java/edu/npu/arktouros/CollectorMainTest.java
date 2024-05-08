package edu.npu.arktouros;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;

/**
 * @author : [wangminan]
 * @description : 测试主启动类
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class CollectorMainTest {

    @Test
    void testMain() throws Exception {
        CountDownLatch latch = Mockito.mock(CountDownLatch.class);
        Mockito.doThrow(new InterruptedException()).when(latch).await();
        setFinalStatic(CollectorMain.class.getDeclaredField("runningLatch"), latch);
        Assertions.assertThrows(InterruptedException.class,
                () -> CollectorMain.main(new String[]{}));
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
        Field modifiers = null;
        for (Field each : fields) {
            if ("modifiers".equals(each.getName())) {
                modifiers = each;
            }
        }
        if (modifiers != null) {
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, newValue);
        } else {
            throw new RuntimeException();
        }
    }
}
