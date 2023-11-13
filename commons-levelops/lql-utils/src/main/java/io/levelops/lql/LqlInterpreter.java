package io.levelops.lql;

import io.levelops.lql.eval.LqlTermEvaluator;
import io.levelops.lql.exceptions.LqlEvalException;
import io.levelops.lql.exceptions.LqlException;
import io.levelops.lql.models.LqlAst;
import io.levelops.lql.models.LqlToken;
import io.levelops.lql.models.LqlTokenType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.levelops.lql.utils.LqlParsingUtils.checkToken;
import static io.levelops.lql.utils.LqlParsingUtils.checkTokenType;

@Log4j2
public class LqlInterpreter {

    public static boolean evaluate(LqlAst ast, List<LqlTermEvaluator> termEvaluators) throws LqlEvalException {
        return evaluate(ast, Collections.emptyMap(), termEvaluators);
    }

    public static boolean evaluate(LqlAst ast, Map<LqlAst, Boolean> environment) throws LqlEvalException {
        return evaluate(ast, environment, Collections.emptyList());
    }

    public static boolean evaluate(LqlAst ast, Map<LqlAst, Boolean> environment, List<LqlTermEvaluator> termEvaluators) throws LqlEvalException {

        // TODO this is a naïve O(n^2) implementation with minor optimizations
        // Should be optimized (bubble results up the branch) if LQLs become large

        MutableInt changes = new MutableInt(0);
        do {
            changes.setValue(0);
            visit(ast, node -> {
                // already evaluated, skip
                if (node.getEval() != null) {
                    return false; // stop visiting this branch
                }

                // node is in env, take value from it
                Boolean eval = environment.get(node);
                if (eval != null) {
                    node.setEval(eval);
                    changes.increment();
                    return false; // stop visiting this branch
                }

                // logical expression, evaluate if children are evaluated
                boolean and = checkToken(node.getToken(), LqlTokenType.KEYWORD, "and");
                boolean or = checkToken(node.getToken(), LqlTokenType.KEYWORD, "or");
                boolean not = checkToken(node.getToken(), LqlTokenType.KEYWORD, "not");
                if (and || or) {
                    if (checkChildrenEvaluation(node.getChildren())) {
                        Boolean output = node.getChildren().stream()
                                .map(LqlAst::getEval)
                                .map(Boolean.class::cast)
                                .reduce(and ? Boolean::logicalAnd : Boolean::logicalOr)
                                .orElse(false);
                        node.setEval(output);
                        changes.increment();
                        return false; // stop visiting this branch;
                    }
                } else if (not) {
                    if (node.getChildren().size() == 1 && checkChildrenEvaluation(node.getChildren())) {
                        Boolean output = (Boolean) node.getChildren().get(0).getEval();
                        node.setEval(!output);
                        changes.increment();
                        return false; // stop visiting this branch;
                    }
                } else {
                    // otherwise, try to find an evaluator for that node
                    for (LqlTermEvaluator evaluator : termEvaluators) {
                        if (evaluator.accepts(node)) {
                            try {
                                node.setEval(evaluator.evaluate(node));
                            } catch (LqlException e) {
                                log.warn("Failed to evaluate AST node: {}", node, e);
                            }
                            changes.increment();
                            return false; // stop visiting this branch;
                        }
                    }
                }

                return true; // keep visiting
            });

            // continue until the root is evaluated, or no change has been made
        } while (changes.getValue() > 0 && ast.getEval() == null);

        if (ast.getEval() == null) {
            throw new LqlEvalException("Evaluation did not yield a value");
        }
        return (boolean) ast.getEval();
    }

    public static List<LqlAst> extractIdentifiers(LqlAst ast) {
        return search(ast, LqlInterpreter::isIdentifier);
    }

    public static List<LqlAst> extractTerms(LqlAst ast) {
        return search(ast, LqlInterpreter::isTerm);
    }

    /**
     * Mutate AST. USE CAREFULLY.
     * Mapper gives a identifier token value and returns a mutated value.
     */
    public static void transformIdentifiers(LqlAst ast, Function<String, String> mapper) {
        transform(ast, LqlInterpreter::isIdentifier, token -> LqlLexer.parseToken(mapper.apply(token.getRawValue())).orElse(token));
    }

    /**
     * Mutate AST. USE CAREFULLY.
     * Mapper gives a identifier token value and returns a mutated value.
     */
    public static void transformLiterals(LqlAst ast, Function<String, String> mapper) {
        transform(ast, LqlInterpreter::isLiteral, token -> LqlLexer.parseToken(mapper.apply(token.getRawValue())).orElse(token));
    }

    // region predicates
    private static boolean isIdentifier(LqlAst node) {
        return node.getToken().getType().equals(LqlTokenType.IDENTIFIER);
    }

    private static boolean isLiteral(LqlAst node) {
        return node.getToken().getType().equals(LqlTokenType.LITERAL);
    }


    private static boolean isTerm(LqlAst node) {
        return checkTokenType(node.getToken(), LqlTokenType.OPERATOR)
                || checkToken(node.getToken(), LqlTokenType.KEYWORD, "is");
    }
    // endregion

    private static List<LqlAst> search(LqlAst ast, Predicate<LqlAst> predicate) {
        List<LqlAst> matches = new ArrayList<>();
        visit(ast, node -> {
            if (predicate.test(node)) {
                matches.add(node);
            }
        });
        return matches;
    }

    private static void visit(LqlAst ast, Consumer<LqlAst> visitor) {
        visit(ast, node -> {
            visitor.accept(node);
            return true;
        });
    }

    private static void visit(LqlAst ast, Function<LqlAst, Boolean> visitor) {
        Stack<LqlAst> stack = new Stack<>();
        stack.push(ast);
        while (!stack.isEmpty()) {
            LqlAst pop = stack.pop();
            Boolean keepGoing = visitor.apply(pop);
            if (Boolean.TRUE.equals(keepGoing)) {
                pop.getChildren().forEach(stack::push);
            }
        }
    }

    private static void transform(LqlAst ast, Predicate<LqlAst> predicate, Function<LqlToken, LqlToken> mapper) {
        Stack<LqlAst> stack = new Stack<>();
        stack.push(ast);
        while (!stack.isEmpty()) {
            LqlAst pop = stack.pop();
            if (predicate.test(pop)) {
                pop.setToken(mapper.apply(pop.getToken()));
            }
            pop.getChildren().forEach(stack::push);
        }
    }

    private static boolean checkChildrenEvaluation(List<LqlAst> children) {
        return children.stream()
                .map(LqlAst::getEval)
                .allMatch(Objects::nonNull);
    }

    public static List<LqlToken> extractTokens(List<LqlAst> astList) {
        return astList.stream().map(LqlAst::getToken).collect(Collectors.toList());
    }
}
