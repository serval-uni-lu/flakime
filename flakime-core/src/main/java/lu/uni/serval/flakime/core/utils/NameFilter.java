package lu.uni.serval.flakime.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class NameFilter {
    private final Map<String, Pattern> filterMap = new HashMap<>();

    public NameFilter(Set<String> filters) {
        for(String filter: filters){
            if(filter.trim().isEmpty()){
                continue;
            }

            filterMap.put(filter, Pattern.compile(filter, Pattern.CASE_INSENSITIVE));
        }
    }

    public boolean hasRules(){
        return !filterMap.isEmpty();
    }

    public boolean matches(String name){
        if(!hasRules()){
            return true;
        }

        for(Pattern pattern: filterMap.values()){
            if(pattern.matcher(name).find()) {
                return true;
            }
        }

        return false;
    }
}
