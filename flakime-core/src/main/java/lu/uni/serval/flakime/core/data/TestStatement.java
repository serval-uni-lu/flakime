package lu.uni.serval.flakime.core.data;

public class TestStatement {
    private int sourceLineNumberStart;
    private int getSourceLineNumberEnd;
    private String content;
    private TestMethod owningMethod;

    public TestStatement(int sourceLineNumberStart, int getSourceLineNumberEnd, TestMethod owningMethod) {
        this.sourceLineNumberStart = sourceLineNumberStart;
        this.getSourceLineNumberEnd = getSourceLineNumberEnd;
        this.owningMethod = owningMethod;
    }
}
