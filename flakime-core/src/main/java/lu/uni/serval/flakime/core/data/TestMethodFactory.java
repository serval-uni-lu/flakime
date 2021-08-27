package anonymised.flakime.core.data;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import anonymised.flakime.core.utils.Logger;

import java.io.File;

public class TestMethodFactory {

    private TestMethodFactory() throws IllegalAccessException {
        throw new IllegalAccessException("TestMethodFactory should not be instantiated");
    }

    public static TestMethod create(Logger logger, CtMethod ctMethod, File sourceFile, CtClass declaringClass) {
        try {
            return new TestMethod(logger, ctMethod, sourceFile,declaringClass);
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
