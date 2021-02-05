package lu.uni.serval.model;

import javassist.CtMethod;

import java.io.File;
import java.util.Set;

/**
 * This class represent a Test method holding all information about the TestMEthod to be transformed
 */
public class TestMethod {
    private Set<CtMethod> ctMethods;
    private String sourceCode;
}
