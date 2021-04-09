package lu.uni.serval.flakime.core.data;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.analysis.ControlFlow;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import lu.uni.serval.flakime.core.utils.Logger;

/**
 * This class represent a Test method holding all information about the targeted
 * (test) method to be transformed
 */
public class TestMethod {
    private final Logger logger;
    private final CtMethod ctMethod;
    private final ControlFlow controlFlow;
    private final File sourceCodeFile;
    private final CtClass declaringClass;
    private final Set<Integer> statementLineNumbers;

    public ControlFlow.Block[] getBlocks() {
        return controlFlow.basicBlocks();
    }

    /**
     * TestMethod constructor
     *
     * @param logger     Reference to logger
     * @param ctMethod   Instance of javassist {@code CtMethod.class}
     * @param sourceCode Instance of {@code File.class} pointing the method source
     *                   files
     * @param declaringClass The declaringClass fo this test mehtod
     * @throws BadBytecode Thrown if the bytecode is malformed
     */
    public TestMethod(Logger logger, CtMethod ctMethod, File sourceCode, CtClass declaringClass) throws BadBytecode {
        this.logger = logger;
        this.ctMethod = ctMethod;
        this.sourceCodeFile = sourceCode;
        this.declaringClass = declaringClass;
        this.controlFlow = new ControlFlow(this.ctMethod);
        this.statementLineNumbers = Arrays.stream(this.controlFlow.basicBlocks())
                .map(block -> this.ctMethod.getMethodInfo().getLineNumber(block.position()))
                .collect(Collectors.toSet());
    }

    public CtMethod getCtMethod() {
        return ctMethod;
    }

    /**
     *
     * Helper function to extract the line number following all {@code BasicBlocks}
     * representing an statement.
     *
     * @return The set of line number that are right after a statement.
     */
    public Set<Integer> getStatementLineNumbers() {
        return this.statementLineNumbers;
    }

    /**
     * Helper function to insert a new double variable in the {@code CtMethod}
     * context
     *
     * @param variableName Unique variable identifier (Must not overwrite existing
     *                     method or global variables names)
     * @param type         Class of the parameter to add
     * @throws CannotCompileException If the variable does no follow java syntax
     */
    public void addLocalVariable(String variableName, CtClass type) throws CannotCompileException {
        this.ctMethod.addLocalVariable(variableName, type);
        logger.debug(
                String.format("Inserted local variable '%s' to method %s", variableName, this.ctMethod.getLongName()));
    }

    public void insertBefore(String payload) {
        try {
            this.ctMethod.insertBefore(payload);
            logger.debug(
                    String.format("Inserted '%s' at beginning of method %s", payload, this.ctMethod.getLongName()));
        } catch (CannotCompileException e) {
            logger.error(String.format("Failed to insert payload before method '%s': %s", this.ctMethod.getLongName(),
                    e.getMessage()));
        }
    }

    /**
     * Insert a source code payload at a certain index in the {@code CtMethod}
     * instance
     * 
     * @param lineNumber The target line to insert the payload
     * @param payload    The source code to insert
     *
     *
     */
    public void insertAt(int lineNumber, String payload) {

        try {
            logger.debug(String.format("[%s][lineNumber: %d]", this.getName(), lineNumber));
            insertAt(lineNumber, true, payload);
            logger.debug(
                    String.format("Inserted payload at line %s in method %s", lineNumber, this.ctMethod.getLongName()));

        } catch (CannotCompileException e) {

            logger.error(String.format("Failed to insert payload at line %d in method '%s': %s", lineNumber,
                    this.ctMethod.getLongName(), e.getMessage()));

        }
    }

    private void insertAt(int lineNum, boolean modify, String src) throws CannotCompileException {
        CodeAttribute ca = ctMethod.getMethodInfo().getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        LineNumberAttribute ainfo = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag);
        if (ainfo == null)
            throw new CannotCompileException("no line number info");

        LineNumberAttribute.Pc pc = ainfo.toNearPc(lineNum);
        int index = pc.index;
        if (!modify)
            return;

        CtClass cc = declaringClass;
        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(cc);
        try {
            jv.recordLocalVariables(ca, index);
            jv.recordParams(ctMethod.getParameterTypes(), Modifier.isStatic(ctMethod.getModifiers()));
            jv.setMaxLocals(ca.getMaxLocals());
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int locals = b.getMaxLocals();
            int stack = b.getMaxStack();
            ca.setMaxLocals(locals);

            /*
             * We assume that there is no values in the operand stack at the position where
             * the bytecode is inserted.
             */


            /*
             * Added by us
             */
            int currentStackHeight = ca.getMaxStack();
            ca.setMaxStack(stack + currentStackHeight);

            index = iterator.insertAt(index, b.get());
            iterator.insert(b.getExceptionTable(), index);
            ctMethod.getMethodInfo().rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        } catch (NotFoundException | CompileError | BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * @return The method simpleName
     */
    public String getName() {
        return this.ctMethod.getName();
    }

    /**
     * Return the method fully qualified name
     *
     * @return method fully qualified name
     */
    public String getLongName() {
        return this.ctMethod.getLongName();
    }

    public File getSourceCodeFile() {
        return sourceCodeFile;
    }
}
