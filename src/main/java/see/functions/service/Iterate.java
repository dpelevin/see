/*
 * Copyright 2011 Vasily Shiyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package see.functions.service;

import com.google.common.base.Preconditions;
import see.functions.ContextCurriedFunction;
import see.functions.Function;
import see.functions.VarArgFunction;

import java.util.List;
import java.util.Map;


/**
 * Iterate. Executes a function for every item in sequence.
 *
 * Takes three arguments: string variable name, Iterable and a function.
 * For each item in sequence it puts item into context under specified name and executes function.
 * Returns result of the last function.
 *
 * This implementation assumes that evaluator doesn't cache evaluation results,
 * i.e. each time get(int) is called on arguments, corresponding sub-tree is evaluated.
 */
public class Iterate implements ContextCurriedFunction<Function<List<Object>, Object>> {
    @Override
    public Function<List<Object>, Object> apply(Map<String, ?> context) {
        final Map<String, ? super Object> writableContext = (Map<String, ? super Object>) context;

        return new VarArgFunction<Object, Object>() {
            @Override
            public Object apply(List<Object> input) {
                Preconditions.checkArgument(input.size() == 3, "Iterate takes 3 arguments");

                String varName = (String) input.get(0);
                Iterable<?> list = (Iterable<?>) input.get(1);

                Object lastValue = null;
                for (Object item : list) {
                    writableContext.put(varName, item);
                    lastValue = input.get(2); // **WILL** re-evaluate argument
                }
                
                return lastValue;
            }
        };
    }

    @Override
    public String toString() {
        return "iterate";
    }
}