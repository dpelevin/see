package see.functions.service;


import com.google.common.base.Function;
import see.functions.ContextCurriedFunction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static see.functions.bool.BooleanCastHelper.fromBoolean;

public class IsDefined implements ContextCurriedFunction<Function<List<String>, BigDecimal>> {
    @Override
    public Function<List<String>, BigDecimal> apply(final Map<String, Object> context) {
        return new Function<List<String>, BigDecimal>() {
            @Override
            public BigDecimal apply(List<String> strings) {
                return fromBoolean(context.get(strings.get(0)) != null);
            }
        };
    }

    @Override
    public String toString() {
        return "isDefined";
    }
}
