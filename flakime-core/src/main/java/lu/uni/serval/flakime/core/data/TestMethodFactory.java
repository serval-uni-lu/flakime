package lu.uni.serval.flakime.core.data;

import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import lu.uni.serval.flakime.core.utils.Logger;

import java.io.File;

public class TestMethodFactory {
    public static TestMethod create(Logger logger, CtMethod ctMethod, File sourceFile) {
        try {
            return new TestMethod(logger, ctMethod, sourceFile);
        } catch (BadBytecode badBytecode) {
            logger.error(String.format("Failed to create %s from %s of '%s': %s",
                    TestMethod.class.getCanonicalName(),
                    CtMethod.class.getCanonicalName(),
                    ctMethod.getLongName(),
                    badBytecode.getMessage()
            ));

            return null;
        }
    }
}
