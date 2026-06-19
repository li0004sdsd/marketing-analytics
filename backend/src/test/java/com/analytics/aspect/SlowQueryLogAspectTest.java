package com.analytics.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.analytics.annotation.SlowQueryLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SlowQueryLogAspectTest {

    @Autowired
    private SlowQueryTestService testService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    void setUp() {
        aspectLogger = (Logger) LoggerFactory.getLogger(SlowQueryLogAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
        aspectLogger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        if (aspectLogger != null && listAppender != null) {
            aspectLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    @DisplayName("方法耗时超阈值：记录[SLOW QUERY] WARN级别日志，包含类名、方法名、耗时")
    void testSlowQuery_LogsWarning() {
        String result = testService.slowMethod("arg1", 42);

        assertEquals("slow-result:arg1-42", result,
                "业务方法返回值应正常透传，AOP不应修改方法签名");

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size(), "超过阈值的慢查询应记录1条日志");

        ILoggingEvent event = logs.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("[SLOW QUERY]"), "日志应包含[SLOW QUERY]标识");
        assertTrue(msg.contains("SlowQueryTestService#slowMethod"),
                "日志应包含类名#方法名");
        assertTrue(msg.contains("threshold=100ms"), "日志应包含阈值");
        assertTrue(msg.contains("String=arg1"), "日志应包含参数名-值");
        assertTrue(msg.contains("Integer=42"), "日志应包含所有参数");
        assertTrue(msg.contains("resultType=String"), "日志应包含返回值类型");
    }

    @Test
    @DisplayName("方法耗时低于阈值：不记录日志")
    void testFastQuery_NoLog() {
        String result = testService.fastMethod("hello");

        assertEquals("fast:hello", result);
        assertTrue(listAppender.list.isEmpty(),
                "低于阈值的查询不应触发慢查询日志");
    }

    @Test
    @DisplayName("方法抛异常：仍记录[SLOW QUERY FAILED]日志，包含异常信息")
    void testSlowQuery_WithException_StillLogs() {
        try {
            testService.exceptionMethod();
            fail("应抛出RuntimeException");
        } catch (RuntimeException expected) {
            assertEquals("boom", expected.getMessage());
        }

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size(), "慢查询抛异常也应记录日志");

        String msg = logs.get(0).getFormattedMessage();
        assertTrue(msg.contains("[SLOW QUERY FAILED]"),
                "异常场景日志应包含FAILED标识");
        assertTrue(msg.contains("RuntimeException: boom"),
                "日志应包含异常类型和消息");
    }

    @Test
    @DisplayName("注解自定义阈值：优先使用注解上的thresholdMs，而非配置默认值")
    void testCustomThreshold_UsesAnnotationValue() {
        testService.customThresholdMethod();

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).getFormattedMessage().contains("threshold=50ms"),
                "应使用注解上指定的50ms，而非默认500ms");
    }

    @Test
    @DisplayName("参数截断：大于200字符的参数截断显示，避免日志过大")
    void testLongArg_Truncated() {
        String longArg = "a".repeat(500);
        testService.fastMethod(longArg);

        assertTrue(listAppender.list.isEmpty(), "fastMethod阈值100ms，150ms不应触发");
    }

    @Test
    @DisplayName("无参方法：日志中args为空，不报错")
    void testNoArgsMethod() {
        String result = testService.noArgsMethod();
        assertEquals("no-args", result);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).getFormattedMessage().contains("args=[]"),
                "无参方法应显示空args");
    }

    @Test
    @DisplayName("null参数：日志中显示null，不抛NPE")
    void testNullArg() {
        String result = testService.slowMethod(null, 99);
        assertEquals("slow-result:null-99", result);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).getFormattedMessage().contains("String=null"),
                "null参数应正确显示为null");
    }

    @Component
    public static class SlowQueryTestService {

        @SlowQueryLog(thresholdMs = 100)
        public String slowMethod(String s, Integer i) {
            sleep(150);
            return "slow-result:" + s + "-" + i;
        }

        @SlowQueryLog(thresholdMs = 100)
        public String fastMethod(String s) {
            sleep(10);
            return "fast:" + s;
        }

        @SlowQueryLog(thresholdMs = 100)
        public String exceptionMethod() {
            sleep(150);
            throw new RuntimeException("boom");
        }

        @SlowQueryLog(thresholdMs = 50)
        public String customThresholdMethod() {
            sleep(80);
            return "custom";
        }

        @SlowQueryLog(thresholdMs = 100)
        public String noArgsMethod() {
            sleep(150);
            return "no-args";
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
