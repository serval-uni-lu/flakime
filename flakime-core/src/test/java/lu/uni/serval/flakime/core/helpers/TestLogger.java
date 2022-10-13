package lu.uni.serval.flakime.core.helpers;

import lu.uni.serval.flakime.core.utils.Logger;

public class TestLogger implements Logger {
    @Override
    public void info(String message) {
        System.out.printf("[INFO] %s - %s%n",this.getClass().getName(),message);
    }

    @Override
    public void warn(String message) {
        System.out.printf("[WARN] %s - %s%n",this.getClass().getName(),message);
    }

    @Override
    public void debug(String message) {
        System.out.printf("[DEBUG] %s - %s%n",this.getClass().getName(),message);
    }

    @Override
    public void error(String message) {
        System.err.printf("[ERROR] %s - %s%n",this.getClass().getName(),message);
    }
}
