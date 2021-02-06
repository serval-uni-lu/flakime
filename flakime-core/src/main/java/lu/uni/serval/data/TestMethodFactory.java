package lu.uni.serval.data;

import javassist.CtMethod;
import javassist.bytecode.BadBytecode;

import java.io.File;

public class TestMethodFactory {
    public static TestMethod create(CtMethod ct, File sourceFile) {
        try {
            return new TestMethod(ct, sourceFile);
        } catch (BadBytecode badBytecode) {
            return null;
        }
    }
}
