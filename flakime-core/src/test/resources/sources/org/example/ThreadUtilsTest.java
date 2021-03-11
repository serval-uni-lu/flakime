package org.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

class ThreadUtilsTest {
    @Test
    void testUnsafeThreatWithSleep() throws InterruptedException {
        final ByteArrayOutputStream stream = ThreadUtils.toStreamUnsafe("Hello ", "World");
        Thread.sleep(200);
        assertEquals("Hello World", stream.toString());
    }

    @Test
    void threadWithTimeout()
    {
        assertTimeout(ofSeconds(5), () -> {
            final ByteArrayOutputStream stream = ThreadUtils.toStreamUnsafe("Hello ", "World");
            Thread.sleep(200);
            assertEquals("Hello World", stream.toString());
        });
    }
}