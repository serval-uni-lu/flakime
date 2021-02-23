package lu.uni.serval.flakime.core.flakime.maven.utils;

import lu.uni.serval.flakime.core.utils.Logger;
import org.apache.maven.plugin.logging.Log;

public class MavenLogger implements Logger {
    private final Log logger;

    public MavenLogger(Log logger){
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        this.logger.info(message);
    }

    @Override
    public void warn(String message) {
        this.logger.warn(message);
    }

    @Override
    public void debug(String message) {
        this.logger.debug(message);
    }

    @Override
    public void error(String message) {
        this.logger.error(message);
    }
}
