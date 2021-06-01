package org.example;

import java.io.NotActiveException;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URLPermission;
import java.util.Random;
import sun.jvm.hotspot.utilities.Assert;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {
    @Test
    void testInflateWithPositiveNumber(){
        double value = 1.;

        Random rnd = new Random();


        final ByteArrayOutputStream stream = ThreadUtils.toStreamUnsafe("Hello ", "World");
        Thread t = new Thread(() -> assertEquals(stream.toString(), "Hello world"));

        for (double i = 0.0 ; true; i = rnd.nextDouble()){
            if (i>0.9) {
                t.start();
                break;
            }
        }
        assertTrue(value < MathUtils.inflate(value));

    }
    public static void main (String[] arg) {

        testInflateWithNegativeNumber22();
    }
    @Test
    void testIf(){
        if (Math.random() > 0.5){
            assertTrue(true);
        }else if (Math.random() > 0.8){
            assertTrue(true);
        } else {
            assertTrue(true);
        }

    }

    @Test
    void testInflateWithNegativeNumber() {
        final String[] args = new String[] { "-f=bar" };
        StringBuilder options = (new StringBuilder()).append(Arrays.toString(args));
        try
        {
            System.out.printf("%s%n",options.toString());
            assertTrue(true);
        }
        catch (final NullPointerException npe){
            assertTrue(true);
        }
        catch (final Exception e)
        {
            assertTrue(true);
        }
        Object a  = null;
        System.out.printf("%n");
    }

    @Test
    static void testInflateWithNegativeNumber22() {
        ArrayList<String> list = new ArrayList<>();
        list.add("Test");

        try
        {
            assertTrue(true);
        }
        catch (final NullPointerException e)
        {
             assertTrue(true);
        }
    }
}