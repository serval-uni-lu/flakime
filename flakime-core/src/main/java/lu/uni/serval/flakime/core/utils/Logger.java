package lu.uni.serval.flakime.core.utils;

public interface Logger {
    void info(String message);
    void warn(String message);
    void debug(String message);
    void error(String message);
}
