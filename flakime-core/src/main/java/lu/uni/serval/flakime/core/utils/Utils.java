package anonymised.flakime.core.utils;

import javassist.CtMethod;
import javassist.bytecode.AttributeInfo;

import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    private Utils(){

    }

    /**
     * @param m The Ctmethod instance which is assesed to be a test or not
     * @param methodFilters The test method filter (i.e. the regex identifying a test by name)
     * @param annotationFilters The test annotation filter( i.e. the regex identifying a test by its annotation)
     * @return True if the ctMethod is to be considered as a test.
     */
    public static boolean isTest(CtMethod m, NameFilter methodFilters,NameFilter annotationFilters) {
        String methodName = m.getName();

        if(m.getMethodInfo().isConstructor()){
            return false;
        }

        if(methodFilters.hasRules() && methodFilters.matches(methodName)){
            return true;
        }

        String runtimeAnnotation = "RuntimeVisibleAnnotations";
        List<AttributeInfo> ai = m.getMethodInfo().getAttributes().stream()
                .filter(attributeInfo -> attributeInfo.getName().equals(runtimeAnnotation)).collect(Collectors.toList());

        for(AttributeInfo attribute:ai){
            if(annotationFilters.matches(attribute.toString())){
                return true;
            }
        }

        return false;
    }
}
