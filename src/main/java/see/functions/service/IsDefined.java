package see.functions.service;


import com.google.common.base.Function;
import see.functions.ContextCurriedFunction;

import java.util.List;
import java.util.Map;

public class IsDefined implements ContextCurriedFunction<Function<List<String>, Boolean>> {
    @Override
    public Function<List<String>, Boolean> apply(final Map<String, Object> context) {
        return new Function<List<String>, Boolean>() {
            @Override
            public Boolean apply(List<String> strings) {
                return context.get(strings.get(0)) != null;
            }
        };
    }

    @Override
    public String toString() {
        return "isDefined";
    }
}