package org.example;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkUtilsTest {
    @Test
    void testListenForMessageFor3secondsWithNoMessage() throws IOException {
        final String message = NetworkUtils.listenForMessage(8095, 3000);
        assertTrue(message.isEmpty());
    }
}
