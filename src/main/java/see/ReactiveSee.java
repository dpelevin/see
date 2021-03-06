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

package see;

import com.google.common.collect.Maps;
import see.evaluation.Evaluator;
import see.evaluation.evaluators.SimpleEvaluator;
import see.parser.config.ConfigBuilder;
import see.parser.config.GrammarConfiguration;
import see.reactive.SignalFactory;
import see.reactive.impl.OrderedSignalFactory;
import see.tree.Node;

import java.util.Map;

import static com.google.common.collect.ImmutableClassToInstanceMap.builder;
import static see.evaluation.evaluators.SimpleEvaluator.extractScope;
import static see.evaluation.evaluators.SimpleEvaluator.extractServices;

/**
 * Public API for working with reactive extensions(signals, bindings, etc.).
 * Holds {@link See} and {@link SignalFactory} instances.
 * Evaluation methods taking context work in new context.
 */
@SuppressWarnings("UnusedDeclaration")
public class ReactiveSee {
    private final See see;
    private final SignalFactory signalFactory;
    private final GrammarConfiguration config;

    /**
     * Create new instance, custom See instance and reactive factory.
     * @param configuration see configuration
     * @param factory reactive factory
     */
    public ReactiveSee(GrammarConfiguration configuration, SignalFactory factory) {
        this.see = new See(configuration);
        this.signalFactory = factory;
        this.config = configuration;
    }

    /**
     * Create new instance, See configured with supplied configuration.
     * @param config see configuration
     */
    public ReactiveSee(GrammarConfiguration config) {
        this(config, new OrderedSignalFactory());
    }

    /**
     * Create new instance, default See configuration, external ReactiveFactory.
     * @param signalFactory reactive factory
     */
    public ReactiveSee(SignalFactory signalFactory) {
        this(ConfigBuilder.defaultConfig().build(), signalFactory);
    }

    /**
     * Create new instance with default See configuration.
     */
    public ReactiveSee() {
        this(new OrderedSignalFactory());
    }

    /**
     * Get inner reactive factory.
     * @return reactive factory
     */
    public SignalFactory getReactiveFactory() {
        return signalFactory;
    }

    /**
     * Parse a single expression
     *
     * @param expression text to parse
     * @return parsed tree
     */
    public Node<Object> parseExpression(String expression) {
        return see.parseExpression(expression);
    }

    /**
     * Parse semicolon-separated list of expressions
     *
     * @param expression text to parse
     * @return parsed tree
     */
    public Node<Object> parseExpressionList(String expression) {
        return see.parseExpressionList(expression);
    }

    /**
     * Evaluate tree with supplied variables.
     * Evaluation context will contain a reference to ReactiveFactory under special key.
     *
     * @param tree tree to evaluate
     * @param context variable->value mapping
     * @param <T> return type
     * @return evaluated value
     */
    public <T> T evaluate(Node<T> tree, final Map<String, Object> context) {
        Evaluator evaluator = new SimpleEvaluator(
                extractScope(config),
                builder().putAll(extractServices(config))
                        .put(SignalFactory.class, signalFactory)
                        .build()
        );
        return evaluator.evaluate(tree, context);
    }

    /**
     * Evaluate tree with empty context.
     *
     * @param tree tree to evaluate
     * @param <T> return type
     * @return evaluated value
     */
    public <T> T evaluate(Node<T> tree) {
        return evaluate(tree, Maps.<String, Object>newHashMap());
    }

    /**
     * Parse and evaluate simple expression.
     * Equivalent to evaluate(parseExpression(expression), context).
     * @param expression expression to evaluate
     * @param context variable->value mapping
     * @return evaluated value
     */
    public Object eval(String expression, Map<String, Object> context) {
        return evaluate(parseExpression(expression), context);
    }

    /**
     * Parse and evaluate simple expression with empty context.
     * Equivalent to evaluate(parseExpression(expression), context).
     * @param expression expression to evaluate
     * @return evaluated value
     */
    public Object eval(String expression) {
        return eval(expression, Maps.<String, Object>newHashMap());
    }
}
