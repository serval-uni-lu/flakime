import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import lu.uni.serval.flakime.core.data.Project;
import lu.uni.serval.flakime.core.data.TestClass;
import lu.uni.serval.flakime.core.data.TestMethod;
import lu.uni.serval.flakime.core.instrumentation.FlakimeInstrumenter;
import lu.uni.serval.flakime.core.instrumentation.models.Model;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InstrumenterTest {

    @Test
    void computePayloadTest_normalOperation() throws CannotCompileException {
        String output_dir = "";
        String result = null;
        int lineNumber = 12;
        TestMethod m_testMethod = mock(TestMethod.class);
        Model m_model = mock(Model.class);
        when(m_model.getTestFlakinessProbability(m_testMethod,lineNumber,1.0)).thenReturn(0.5);
        result = FlakimeInstrumenter.computePayload(m_testMethod,m_model,lineNumber,new File(output_dir),1.0,true);
        Pattern p = Pattern.compile("^.*<\\s*0\\.5.*\\n*throw new Exception");
        assertTrue(p.matcher(result).find());
    }

    @Test
    void computePayloadTest_zeroProbability() {
        String output_dir = "";
        String result = null;
        int lineNumber = 12;
        TestMethod m_testMethod = mock(TestMethod.class);
        Model m_model = mock(Model.class);
        when(m_model.getTestFlakinessProbability(m_testMethod,lineNumber,1.0)).thenReturn(0.0);
        result = FlakimeInstrumenter.computePayload(m_testMethod,m_model,lineNumber,new File(output_dir),1.0,false);
        assertEquals("",result);

    }

}
