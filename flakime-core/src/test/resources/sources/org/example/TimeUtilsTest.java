package org.example;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {
    @Test
    void testGetTimeInSeconds(){
        long milliseconds = TimeUtils.getCurrentTime(TimeUnit.MILLISECONDS);
        long days = TimeUtils.getCurrentTime(TimeUnit.SECONDS);

        assertEquals(1000., (double)milliseconds/(double)days, 10.);
    }

    @Test
    void testGetTimeInDays(){
        long milliseconds = TimeUtils.getCurrentTime(TimeUnit.MILLISECONDS);
        long days = TimeUtils.getCurrentTime(TimeUnit.DAYS);

        assertEquals(8.64E7, (double)milliseconds/(double)days, 1E5);
    }
}