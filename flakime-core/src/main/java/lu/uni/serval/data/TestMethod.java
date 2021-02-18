package lu.uni.serval.data;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represent a Test method holding all information about the targeted (test) method to be transformed
 */
public class TestMethod {
    public CtMethod getCtMethod() {
        return ctMethod;
    }

    private final CtMethod ctMethod;
    private final ControlFlow controlFlow;
    private final File sourceCodeFile;

    public ControlFlow getControlFlow() {
        return controlFlow;
    }

    /**
     * TestMethod constructor
     *
     * @param ctMethod Instance of javassist {@code CtMethod.class}
     * @param sourceCode Instance of {@code File.class} pointing the method source files
     * @throws BadBytecode Thrown if the bytecode is malformed
     */
    public TestMethod(CtMethod ctMethod, File sourceCode) throws BadBytecode {
        this.ctMethod = ctMethod;
        this.sourceCodeFile = sourceCode;
        this.controlFlow = new ControlFlow(this.ctMethod);

    }

    /**
     *
     * Helper function to extract the line number following all {@code BasicBlocks} representing an statement.
     *
     * @return The set of line number that are right after a statement.
     */
    public Set<Integer> getStatementLineNumbers(){
        return Arrays.stream(this.controlFlow.basicBlocks())
                .map(block -> this.ctMethod.getMethodInfo().getLineNumber(block.position())+1)
                .collect(Collectors.toSet());
    }

    /**
     * Helper function to insert a new double variable in the {@code CtMethod} context
     *
     * @param variableName Unique variable identifier (Must not overwrite existing method or global variables names)
     * @throws CannotCompileException If the variable does no follow java syntax
     */
    public void addLocalVariableDouble(String variableName) throws CannotCompileException {
        this.ctMethod.addLocalVariable(variableName, CtClass.doubleType);
    }

    /**
     * Insert a source code payload at a certain index in the {@code CtMethod} instance
     * @param lineNumber The target line to insert the payload
     * @param payload The source code to insert
     *  if the Compilation of source code fails
     */
    public void insertAt(int lineNumber, String payload) {

        try {
//            System.out.printf("%s line+1:%d line:%d%n",this.getName(),lineNumber,lineNumber-1);
//            this.ctMethod.getMethodInfo().getCodeAttribute().setMaxStack(5);
            this.ctMethod.insertAt(lineNumber, payload);
        } catch (CannotCompileException e) {
            int maxStack = this.ctMethod.getMethodInfo().getCodeAttribute().getMaxStack();
            System.err.printf("%s line:%d %n",this.getName(),lineNumber);
//            System.err.println("CannotCompile Maxstack: "+maxStack);
            e.printStackTrace();
        }
    }

    /**
     * @return The method simpleName
     */
    public String getName() {
        return this.ctMethod.getName();
    }

    public File getSourceCodeFile() {
        return sourceCodeFile;
    }
}
