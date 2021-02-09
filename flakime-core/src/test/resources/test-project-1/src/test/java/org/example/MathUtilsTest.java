package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MathUtilsTest {
    @Test
    void testInflateWithPositiveNumber(){
        double value = 1.;
        assertTrue(value < MathUtils.inflate(value));
    }

    @Test
    void testInflateWithNegativeNumber(){
        double value = -1.;
        assertTrue(value > MathUtils.inflate(value));
    }

    @Test
    void testInflateWithZero(){
        double value = 0.;
        assertEquals(0., MathUtils.inflate(value));
    }
}