// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

// Revision to add Parse Exception Index method parseExIndex(boolean has)
// Current as of 2/24/21

package plc.project;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and
 * {@link #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * This method returns the correct index for the Parse Exception thrown
     */
    private int parseExIndex(boolean has) {
        if (has)
            return tokens.get(0).getIndex();
        return tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<Ast.Field>();
        List<Ast.Method> methods = new ArrayList<Ast.Method>();

        if (tokens.has(0) && peek(Token.Type.IDENTIFIER)) {
            while (peek(Token.Type.IDENTIFIER)) {

                // Try field
                if (peek("LET")) {
                    while (peek("LET")) {
                        fields.add(parseField());
                        if (tokens.has(0) && (!peek("LET") && !peek("DEF")))
                            throw new ParseException("not let or def" + " INDEX:" + parseExIndex(true),
                                    tokens.get(0).getIndex());
                    }
                }
                if (peek("DEF")) {
                    while (peek("DEF")) {
                        methods.add(parseMethod());
                        if (tokens.has(0) && !peek("DEF"))
                            throw new ParseException("not def" + " INDEX:" + tokens.get(0).getIndex(),
                                    tokens.get(0).getIndex());
                    }
                }
            }
        }

        if (!tokens.has(0))
            return new Ast.Source(fields, methods);
        else
            throw new ParseException("illegal id" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the next
     * tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        match("LET");
        String name = "";
        String typeName = "";

        // Get identifier
        if (peek(Token.Type.IDENTIFIER)) {
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            if (tokens.has(0))
                throw new ParseException("no identifier" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("no identifier" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        if (peek(":")) {
            match(":");
        } else {
            if (tokens.has(0))
                throw new ParseException("No : Operator" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("No : Operator" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        // Type
        if (peek(Token.Type.IDENTIFIER)) {
            typeName = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            if (tokens.has(0))
                throw new ParseException("No Type" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("No Type" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        if (peek("=")) {
            match("=");
            Ast.Expr value = parseExpression();
            if (peek(";")) {
                match(";");
                return new Ast.Field(name, typeName, Optional.of(value));
            } else {
                if (tokens.has(0))
                    throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no  ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        } else {
            if (peek(";")) {
                match(";");
                return new Ast.Field(name, typeName, Optional.empty());
            } else {
                if (tokens.has(0))
                    throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no  ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the next
     * tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        match("DEF");
        String name = "";
        String returnTypeName = "";
        List<String> parameterTypeNames = new ArrayList<String>();

        // Get identifier
        if (peek(Token.Type.IDENTIFIER)) {
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            if (tokens.has(0))
                throw new ParseException("no identifier" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("no identifier" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        // Check (
        if (peek("("))
            match("(");
        else {
            if (tokens.has(0))
                throw new ParseException("no (" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no  (" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        List<String> parameters = new ArrayList<String>();

        while (peek(Token.Type.IDENTIFIER)) {
            // Need to catch NON-IDENTIFIERS in here
            parameters.add(tokens.get(0).getLiteral());
            match(Token.Type.IDENTIFIER);

            if (peek(":")) {
                match(":");
            } else {
                if (tokens.has(0))
                    throw new ParseException("No : Operator" + " INDEX:" + tokens.get(0).getIndex(),
                            tokens.get(0).getIndex());
                else
                    throw new ParseException("No : Operator" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }

            if (peek(Token.Type.IDENTIFIER)) {
                parameterTypeNames.add(tokens.get(0).getLiteral());
                match(Token.Type.IDENTIFIER);
            } else {
                if (tokens.has(0))
                    throw new ParseException("No Type" + " INDEX:" + tokens.get(0).getIndex(),
                            tokens.get(0).getIndex());
                else
                    throw new ParseException("No Type" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }

            if (peek(",")) {
                match(",");
                if (peek(")"))
                    throw new ParseException("trailing comma" + " INDEX:" + tokens.get(0).getIndex(),
                            tokens.get(0).getIndex());
            } else {
                if (!peek(")")) {
                    if (tokens.has(0))
                        throw new ParseException("no , before )" + " INDEX:" + tokens.get(0).getIndex(),
                                tokens.get(0).getIndex());
                    else
                        throw new ParseException("no , before )" + " INDEX:" + (parseExIndex(false)),
                                parseExIndex(false));
                }
            }
        }

        // Check )
        if (peek(")"))
            match(")");
        else {
            if (tokens.has(0))
                throw new ParseException("no )" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no )" + " INDEX:" + (parseExIndex(false)), tokens.get(-1).getIndex());
        }

        if (peek(":")) {
            match(":");

            // Type
            if (peek(Token.Type.IDENTIFIER)) {
                returnTypeName = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            } else {
                if (tokens.has(0))
                    throw new ParseException("No Type" + " INDEX:" + tokens.get(0).getIndex(),
                            tokens.get(0).getIndex());
                else
                    throw new ParseException("No Type" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        }

        if (peek("DO"))
            match("DO");
        else {
            if (tokens.has(0))
                throw new ParseException("no DO" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no DO" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        List<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();

        while (!peek("END")) {
            statements.add(parseStatement());
        }

        if (peek("END")) {
            match("END");
            if (returnTypeName.equals("")) {
                return new Ast.Method(name, parameters, parameterTypeNames, Optional.empty(), statements);
            }
            else {
                return new Ast.Method(name, parameters, parameterTypeNames, Optional.of(returnTypeName), statements);
            }
        } else {
            if (tokens.has(0))
                throw new ParseException("no END" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no END" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method. If
     * the next tokens do not start a declaration, if, while, or return statement,
     * then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {

        // Assignment (LET)
        if (peek("LET"))
            return parseDeclarationStatement();

        // IF
        else if (peek("IF"))
            return parseIfStatement();

        // FOR
        else if (peek("FOR"))
            return parseForStatement();

        // WHILE
        else if (peek("WHILE"))
            return parseWhileStatement();

        // RETURN
        else if (peek("RETURN"))
            return parseReturnStatement();

        // EXPRESSION ('=' expression)?
        else {
            Ast.Expr current = parseExpression();
            if (peek("=")) {
                match("=");
                Ast.Expr value = parseExpression();
                if (peek(";")) {
                    match(";");
                    return new Ast.Stmt.Assignment(current, value);
                } else {
                    if (tokens.has(0))
                        throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(),
                                tokens.get(0).getIndex());
                    else
                        throw new ParseException("no ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
                }
            } else {
                if (peek(";")) {
                    match(";");
                    return new Ast.Stmt.Expression(current);
                } else {
                    if (tokens.has(0))
                        throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(),
                                tokens.get(0).getIndex());
                    else
                        throw new ParseException("no ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
                }
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a declaration statement, aka
     * {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        String name = "";
        String typeName = "";

        // Get identifier
        if (peek(Token.Type.IDENTIFIER)) {
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            if (tokens.has(0))
                throw new ParseException("no id" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no id" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        if (peek(":")) {
            match(":");

            // Type
            if (peek(Token.Type.IDENTIFIER)) {
                typeName = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            } else {
                if (tokens.has(0))
                    throw new ParseException("No Type" + " INDEX:" + tokens.get(0).getIndex(),
                            tokens.get(0).getIndex());
                else
                    throw new ParseException("No Type" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        }

        if (peek("=")) {
            match("=");
            Ast.Expr value = parseExpression();
            if (peek(";")) {
                match(";");
                if (typeName.equals(""))
                    return new Ast.Stmt.Declaration(name, Optional.empty(), Optional.of(value));
                else
                    return new Ast.Stmt.Declaration(name, Optional.of(typeName), Optional.of(value));
            } else {
                if (tokens.has(0))
                    throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        } else {
            if (peek(";")) {
                match(";");
                if (typeName.equals(""))
                    return new Ast.Stmt.Declaration(name, Optional.empty(), Optional.empty());
                else
                    return new Ast.Stmt.Declaration(name, Optional.of(typeName), Optional.empty());
            } else {
                if (tokens.has(0))
                    throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method should
     * only be called if the next tokens start an if statement, aka {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        match("IF");

        Ast.Expr condition = parseExpression();

        if (peek("DO")) {
            match("DO");

            List<Ast.Stmt> thenStatements = new ArrayList<Ast.Stmt>();
            List<Ast.Stmt> elseStatements = new ArrayList<Ast.Stmt>();

            while (!peek("ELSE") && !peek("END"))
                thenStatements.add(parseStatement());

            if (peek("ELSE")) {
                match("ELSE");

                while (!peek("END"))
                    elseStatements.add(parseStatement());
            }
            if (peek("END")) {
                match("END");
                return new Ast.Stmt.If(condition, thenStatements, elseStatements);
            } else {
                if (tokens.has(0))
                    throw new ParseException("no END" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no END" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        } else {
            if (tokens.has(0))
                throw new ParseException("no DO" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no DO" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

    }

    /**
     * Parses a for statement from the {@code statement} rule. This method should
     * only be called if the next tokens start a for statement, aka {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        match("FOR");
        String name = "";

        // Get identifier
        if (peek(Token.Type.IDENTIFIER)) {
            name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        } else {
            if (tokens.has(0))
                throw new ParseException("no identifier" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("no identifier" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        if (peek("IN"))
            match("IN");
        else {
            if (tokens.has(0))
                throw new ParseException("no IN" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no IN" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        Ast.Expr value = parseExpression();
        if (peek("DO"))
            match("DO");
        else {
            if (tokens.has(0))
                throw new ParseException("no DO" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no DO" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        List<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();

        while (!peek("END")) {
            statements.add(parseStatement());
        }

        if (peek("END")) {
            match("END");
            return new Ast.Stmt.For(name, value, statements);
        } else {
            if (tokens.has(0))
                throw new ParseException("no END" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no END" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method should
     * only be called if the next tokens start a while statement, aka {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        match("WHILE");

        Ast.Expr condition = parseExpression();

        if (peek("DO"))
            match("DO");
        else {
            if (tokens.has(0))
                throw new ParseException("no DO" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no DO" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

        List<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();

        while (!peek("END"))
            statements.add(parseStatement());

        if (peek("END")) {
            match("END");
            return new Ast.Stmt.While(condition, statements);
        } else {
            if (tokens.has(0))
                throw new ParseException("no END" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no END" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

    }

    /**
     * Parses a return statement from the {@code statement} rule. This method should
     * only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        match("RETURN");

        Ast.Expr value = parseExpression();

        if (peek(";")) {
            match(";");
            return new Ast.Stmt.Return(value);
        } else {
            if (tokens.has(0))
                throw new ParseException("no ;" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            else
                throw new ParseException("no ;" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }

    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {

        Ast.Expr left = parseEqualityExpression();

        while (peek("AND") || peek("OR")) {
            String operator = tokens.get(0).getLiteral();

            match(Token.Type.IDENTIFIER);

            Ast.Expr right = parseEqualityExpression();

            if (!peek("AND") && !peek("OR"))
                return new Ast.Expr.Binary(operator, left, right);
            else
                left = new Ast.Expr.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {

        Ast.Expr left = parseAdditiveExpression();
        Ast.Expr right;

        // Break these peeks into a modular format later
        while (peek("<") || peek("<=") || peek(">") || peek(">=") || peek("==") || peek("!=")) {
            String operator = tokens.get(0).getLiteral();

            match(Token.Type.OPERATOR);

            right = parseAdditiveExpression();

            if (!peek("<") && !peek("<=") && !peek(">") && !peek(">=") && !peek("==") && !peek("!="))
                return new Ast.Expr.Binary(operator, left, right);
            else
                left = new Ast.Expr.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {

        Ast.Expr left = parseMultiplicativeExpression();

        while (peek("+") || peek("-")) {
            String operator = tokens.get(0).getLiteral();

            match(Token.Type.OPERATOR);

            Ast.Expr right = parseAdditiveExpression();

            if (!peek("+") && !peek("-"))
                return new Ast.Expr.Binary(operator, left, right);
            else
                left = new Ast.Expr.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {

        Ast.Expr left = parseSecondaryExpression();

        while (peek("*") || peek("/")) {
            String operator = tokens.get(0).getLiteral();

            match(Token.Type.OPERATOR);

            Ast.Expr right = parseAdditiveExpression();

            if (!peek("*") && !peek("/"))
                return new Ast.Expr.Binary(operator, left, right);
            else
                left = new Ast.Expr.Binary(operator, left, right);
        }

        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {

        Ast.Expr left = parsePrimaryExpression();
        if (!peek(".")) {
            return left;
        } else {

            String name = "";
            while (peek(".")) {

                match(".");

                if (peek(Token.Type.IDENTIFIER)) {
                    name = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                } else {
                    if (tokens.has(0))
                        throw new ParseException("no id" + " INDEX:" + tokens.get(0).getIndex(),
                                tokens.get(0).getIndex());
                    else
                        throw new ParseException("no id" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
                }

                // Catch (
                if (peek("(")) {
                    match("(");

                    List<Ast.Expr> arguments = new ArrayList<Ast.Expr>();

                    while (!peek(")")) {
                        arguments.add(parseExpression());
                        if (peek(","))
                            match(",");
                    }
                    // Check )
                    match(")");
                    if (!peek("."))
                        return new Ast.Expr.Function(Optional.of(left), name, arguments);
                    else
                        left = new Ast.Expr.Function(Optional.of(left), name, arguments);
                } else {
                    if (!peek(".")) {
                        return new Ast.Expr.Access(Optional.of(left), name);
                    } else
                        left = new Ast.Expr.Access(Optional.of(left), name);
                }
            }
        }
        return null;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule for
     * expressions and includes literal values, grouping, variables, and functions.
     * It may be helpful to break these up into other methods but is not strictly
     * necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {

        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expr.Literal(null);
        }

        else if (peek("TRUE")) {
            match("TRUE");
            Boolean t = true;
            return new Ast.Expr.Literal(t);
        }

        else if (peek("FALSE")) {
            match("FALSE");
            Boolean f = false;
            return new Ast.Expr.Literal(f);
        }

        else if (peek(Token.Type.INTEGER)) {
            BigInteger value = new BigInteger(tokens.get(0).getLiteral());
            // System.out.println(value);
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(value);
        }

        else if (peek(Token.Type.DECIMAL)) {
            BigDecimal value = new BigDecimal(tokens.get(0).getLiteral());
            // System.out.println(value);
            match(Token.Type.INTEGER);
            return new Ast.Expr.Literal(value);
        }

        else if (peek(Token.Type.CHARACTER)) {
            // No escapes
            if (tokens.get(0).getLiteral().length() < 4) {
                Character c = tokens.get(0).getLiteral().charAt(1);
                // System.out.println(c);
                match(Token.Type.CHARACTER);
                return new Ast.Expr.Literal(c);
            }
            // Escape
            else {
                String temp = tokens.get(0).getLiteral();
                // BREAK THIS INTO MODULAR METHOD: ////////////////////////////
                temp = temp.replace("\\b", "\b");
                temp = temp.replace("\\n", "\n");
                temp = temp.replace("\\r", "\r");
                temp = temp.replace("\\t", "\t");
                if (temp.equals("'\\\"'"))
                    temp = "'\"'";
                if (temp.equals("'\\\\'"))
                    temp = "'\\'";
                if (temp.equals("'\\\''"))
                    temp = "'\''";
                Character c = temp.charAt(1);
                match(Token.Type.CHARACTER);
                return new Ast.Expr.Literal(c);
            }
        }

        else if (peek(Token.Type.STRING)) {
            String temp = tokens.get(0).getLiteral();
            // BREAK THIS INTO MODULAR METHOD: //////////////////////////
            temp = temp.replace("\\b", "\b");
            temp = temp.replace("\\n", "\n");
            temp = temp.replace("\\r", "\r");
            temp = temp.replace("\\t", "\t");
            temp = temp.replace("\\\"", "\"");
            temp = temp.replace("\\\\", "\\");
            temp = temp.replace("\\\'", "\'");
            temp = temp.substring(1, temp.length() - 1);
            match(Token.Type.STRING);
            return new Ast.Expr.Literal(temp);
        }

        else if (peek("(")) {
            match("(");
            Ast.Expr.Group g = new Ast.Expr.Group(parseExpression());
            if (peek(")")) {
                match(")");
                return g;
            } else {
                if (tokens.has(0))
                    throw new ParseException("no )" + " INDEX:" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                else
                    throw new ParseException("no )" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
            }
        }

        else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            // Check (
            if (peek("(")) {
                match("(");

                List<Ast.Expr> arguments = new ArrayList<Ast.Expr>();

                while (!peek(")")) {
                    arguments.add(parseExpression());
                    if (peek(",")) {
                        match(",");
                        if (peek(")"))
                            throw new ParseException("trailing comma" + " INDEX:" + tokens.get(0).getIndex(),
                                    tokens.get(0).getIndex());
                    }
                }

                // Check )
                match(")");
                return new Ast.Expr.Function(Optional.empty(), name, arguments);
            }

            else
                return new Ast.Expr.Access(Optional.empty(), name);
        } else {
            if (tokens.has(0))
                throw new ParseException("invalid primary" + " INDEX:" + tokens.get(0).getIndex(),
                        tokens.get(0).getIndex());
            else
                throw new ParseException("invalid primary" + " INDEX:" + (parseExIndex(false)), parseExIndex(false));
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's type
     * is the same, or a {@link String}, which matches if the token's literal is the
     * same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i))
                return false;
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType())
                    return false;
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral()))
                    return false;
            } else
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
        }

        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true and
     * advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++)
                tokens.advance();
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }
    }
}
