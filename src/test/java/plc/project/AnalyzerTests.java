package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tests have been provided for a few selective parts of the AST, and are not
 * exhaustive. You should add additional tests for the remaining parts and make
 * sure to handle all of the cases defined in the specification which have not
 * been tested here.
 */
public final class AnalyzerTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testDeclarationStatement(String test, Ast.Stmt.Declaration ast, Ast.Stmt.Declaration expected) {
        Analyzer analyzer = test(ast, expected, new Scope(null));
        if (expected != null) {
            Assertions.assertEquals(expected.getVariable(), analyzer.scope.lookupVariable(expected.getName()));
        }
    }

    public static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        new Ast.Stmt.Declaration("name", Optional.of("Integer"), Optional.empty()),
                        init(new Ast.Stmt.Declaration("name", Optional.of("Integer"), Optional.empty()), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, Environment.NIL));
                        })
                ),
                Arguments.of("Initialization",
                        // LET name = 1;
                        new Ast.Stmt.Declaration("name", Optional.empty(), Optional.of(new Ast.Expr.Literal(BigInteger.ONE))),
                        init(new Ast.Stmt.Declaration("name", Optional.empty(), Optional.of(new Ast.Expr.Literal(BigInteger.ONE))), ast -> {
                            ast.setVariable(new Environment.Variable("name", "name", Environment.Type.INTEGER, Environment.NIL));
                        })
                ),
                Arguments.of("Missing Type",
                        // LET name;
                        new Ast.Stmt.Declaration("name", Optional.empty(), Optional.empty()),
                        null
                ),
                Arguments.of("Unknown Type",
                        // LET name: Unknown;
                        new Ast.Stmt.Declaration("name", Optional.of("Unknown"), Optional.empty()),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testIfStatement(String test, Ast.Stmt.If ast, Ast.Stmt.If expected) {
        test(ast, expected, new Scope(null));
    }

    public static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("Valid Condition",
                        // IF TRUE DO print(1); END
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Stmt.Expression(
                                        new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                new Ast.Expr.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        new Ast.Stmt.If(
                                init(new Ast.Expr.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                Arrays.asList(new Ast.Stmt.Expression(
                                        init(new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER))
                                        )), ast -> ast.setFunction(new Environment.Function("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL))))
                                ),
                                Arrays.asList()
                        )
                ),
                Arguments.of("Invalid Condition",
                        // IF "FALSE" DO print(1); END
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal("FALSE"),
                                Arrays.asList(new Ast.Stmt.Expression(
                                        new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                            new Ast.Expr.Literal(BigInteger.ONE)
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Invalid Statement",
                        // IF TRUE DO print(9223372036854775807); END
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal(Boolean.TRUE),
                                Arrays.asList(new Ast.Stmt.Expression(
                                        new Ast.Expr.Function(Optional.empty(), "print", Arrays.asList(
                                                new Ast.Expr.Literal(BigInteger.valueOf(Long.MAX_VALUE))
                                        ))
                                )),
                                Arrays.asList()
                        ),
                        null
                ),
                Arguments.of("Empty Statements",
                        // IF TRUE DO END
                        new Ast.Stmt.If(
                                new Ast.Expr.Literal(Boolean.TRUE),
                                Arrays.asList(),
                                Arrays.asList()
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testLiteralExpression(String test, Ast.Expr.Literal ast, Ast.Expr.Literal expected) {
        test(ast, expected, new Scope(null));
    }

    public static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean",
                        // TRUE
                        new Ast.Expr.Literal(true),
                        init(new Ast.Expr.Literal(true), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Integer Valid",
                        // 2147483647
                        new Ast.Expr.Literal(BigInteger.valueOf(Integer.MAX_VALUE)),
                        init(new Ast.Expr.Literal(BigInteger.valueOf(Integer.MAX_VALUE)), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Invalid",
                        // 9223372036854775807
                        new Ast.Expr.Literal(BigInteger.valueOf(Long.MAX_VALUE)),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testBinaryExpression(String test, Ast.Expr.Binary ast, Ast.Expr.Binary expected) {
        test(ast, expected, new Scope(null));
    }

    public static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Logical AND Valid",
                        // TRUE AND FALSE
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Literal(Boolean.TRUE),
                                new Ast.Expr.Literal(Boolean.FALSE)
                        ),
                        init(new Ast.Expr.Binary("AND",
                                init(new Ast.Expr.Literal(Boolean.TRUE), ast -> ast.setType(Environment.Type.BOOLEAN)),
                                init(new Ast.Expr.Literal(Boolean.FALSE), ast -> ast.setType(Environment.Type.BOOLEAN))
                        ), ast -> ast.setType(Environment.Type.BOOLEAN))
                ),
                Arguments.of("Logical AND Invalid",
                        // TRUE AND "FALSE"
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Literal(Boolean.TRUE),
                                new Ast.Expr.Literal("FALSE")
                        ),
                        null
                ),
                Arguments.of("String Concatenation",
                        // "Ben" + 10
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal("Ben"),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expr.Binary("+",
                                init(new Ast.Expr.Literal("Ben"), ast -> ast.setType(Environment.Type.STRING)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.STRING))
                ),
                Arguments.of("Integer Addition",
                        // 1 + 10
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigInteger.TEN)
                        ),
                        init(new Ast.Expr.Binary("+",
                                init(new Ast.Expr.Literal(BigInteger.ONE), ast -> ast.setType(Environment.Type.INTEGER)),
                                init(new Ast.Expr.Literal(BigInteger.TEN), ast -> ast.setType(Environment.Type.INTEGER))
                        ), ast -> ast.setType(Environment.Type.INTEGER))
                ),
                Arguments.of("Integer Decimal Addition",
                        // 1 + 1.0
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Literal(BigInteger.ONE),
                                new Ast.Expr.Literal(BigDecimal.ONE)
                        ),
                        null
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    public void testRequireAssignable(String test, Environment.Type target, Environment.Type type, boolean success) {
        if (success) {
            Assertions.assertDoesNotThrow(() -> Analyzer.requireAssignable(target, type));
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> Analyzer.requireAssignable(target, type));
        }
    }

    public static Stream<Arguments> testRequireAssignable() {
        return Stream.of(
                Arguments.of("Integer to Integer", Environment.Type.INTEGER, Environment.Type.INTEGER, true),
                Arguments.of("Integer to Decimal", Environment.Type.DECIMAL, Environment.Type.INTEGER, false),
                Arguments.of("Integer to Comparable", Environment.Type.COMPARABLE, Environment.Type.INTEGER,  true),
                Arguments.of("Integer to Any", Environment.Type.ANY, Environment.Type.INTEGER, true),
                Arguments.of("Any to Integer", Environment.Type.INTEGER, Environment.Type.ANY, false)
        );
    }

    /**
     * Helper function for tests. If {@param expected} is {@code null}, analysis
     * is expected to throw a {@link RuntimeException}.
     */
    private static <T extends Ast> Analyzer test(T ast, T expected, Scope scope) {
        Analyzer analyzer = new Analyzer(scope);
        if (expected != null) {
            analyzer.visit(ast);
            Assertions.assertEquals(expected, ast);
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> analyzer.visit(ast));
        }
        return analyzer;
    }

    /**
     * Runs a callback initializer on the given AST, used for setting type
     * information inline.
     */
    private static <T extends Ast> T init(T ast, Consumer<T> initializer) {
        initializer.accept(ast);
        return ast;
    }

}
