package lu.uni.serval.data;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represent a Test method holding all information about the TestMEthod to be transformed
 */
public class TestMethod {
    private final CtMethod ctMethod;
    private final ControlFlow controlFlow;
    private final File sourceCode;

    public TestMethod(CtMethod ctMethod, File sourceCode) throws BadBytecode {
        this.ctMethod = ctMethod;
        this.sourceCode = sourceCode;
        this.controlFlow = new ControlFlow(this.ctMethod);
    }

    public Set<Integer> getStatementLineNumbers(){
        return Arrays.stream(this.controlFlow.basicBlocks())
                .map(block -> this.ctMethod.getMethodInfo().getLineNumber(block.position())+1)
                .collect(Collectors.toSet());
    }

    public void addLocalVariableDouble(String variableName) throws CannotCompileException {
        this.ctMethod.addLocalVariable(variableName, CtClass.doubleType);
    }

    public void insertAt(int lineNumber, String payload) throws CannotCompileException {
        this.ctMethod.insertAt(lineNumber, payload);
    }

    public String getName() {
        return this.ctMethod.getName();
    }
}
