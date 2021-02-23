package lu.uni.serval.flakime.core.data;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.ControlFlow;
import lu.uni.serval.flakime.core.utils.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represent a Test method holding all information about the targeted (test) method to be transformed
 */
public class TestMethod {
    private final Logger logger;
    private final CtMethod ctMethod;
    private final ControlFlow controlFlow;
    private final File sourceCodeFile;

    public ControlFlow.Block[] getBlocks() {
        return controlFlow.basicBlocks();
    }

    /**
     * TestMethod constructor
     *
     * @param logger Reference to logger
     * @param ctMethod Instance of javassist {@code CtMethod.class}
     * @param sourceCode Instance of {@code File.class} pointing the method source files
     * @throws BadBytecode Thrown if the bytecode is malformed
     */
    public TestMethod(Logger logger, CtMethod ctMethod, File sourceCode) throws BadBytecode {
        this.logger = logger;
        this.ctMethod = ctMethod;
        this.sourceCodeFile = sourceCode;
        this.controlFlow = new ControlFlow(this.ctMethod);
    }

    public CtMethod getCtMethod() {
        return ctMethod;
    }

    /**
     *
     * Helper function to extract the line number following all {@code BasicBlocks} representing an statement.
     *
     * @return The set of line number that are right after a statement.
     */
    public Set<Integer> getStatementLineNumbers(){
        return Arrays.stream(this.controlFlow.basicBlocks())
                .map(block -> this.ctMethod.getMethodInfo().getLineNumber(block.position()))
                .collect(Collectors.toSet());
    }

    /**
     * Helper function to insert a new double variable in the {@code CtMethod} context
     *
     * @param variableName Unique variable identifier (Must not overwrite existing method or global variables names)
     * @param type Class of the parameter to add
     * @throws CannotCompileException If the variable does no follow java syntax
     */
    public void addLocalVariable(String variableName,CtClass type) throws CannotCompileException {
        this.ctMethod.addLocalVariable(variableName, type);
        logger.debug(String.format("Inserted local variable '%s' to method %s",
                variableName,
                this.ctMethod.getLongName()
        ));
    }
    public void insertBefore(String payload){
        try {
            this.ctMethod.insertBefore(payload);
            logger.debug(String.format("Inserted '%s' at beginning of method %s",
                    payload,
                    this.ctMethod.getLongName()
            ));
        } catch (CannotCompileException e) {
            logger.error(String.format("Failed to insert payload before method '%s': %s",
                    this.ctMethod.getLongName(),
                    e.getMessage()
            ));
        }
    }
    /**
     * Insert a source code payload at a certain index in the {@code CtMethod} instance
     * @param lineNumber The target line to insert the payload
     * @param payload The source code to insert
     *  if the Compilation of source code fails
     */
    public void insertAt(int lineNumber, String payload) {
        try {
            this.ctMethod.insertAt(lineNumber, payload);
            logger.debug(String.format("Inserted payload at line %s in method %s",
                    lineNumber,
                    this.ctMethod.getLongName()
            ));
        } catch (CannotCompileException e) {
            logger.error(String.format("Failed to insert payload at line %d in method '%s': %s",
                    lineNumber,
                    this.ctMethod.getLongName(),
                    e.getMessage()
            ));
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
