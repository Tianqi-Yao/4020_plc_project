// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        // Create empty List
        List<Token> tokens = new ArrayList<Token>();

        // LOOP START //////////////////////////////////////////////////
        while (peek(".")) {
            // Detect Whitespace
            if (peek("[ \\\b\\\n\\\r\\\t]")) {
                // CHANGE THE BELOW TO, SKIP!
                chars.advance();
                chars.skip();

            }
            else {
                tokens.add(lexToken());
            }
        }

        // END LOOP ////////////////////////////////////////////////////

        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // Identifier
        if (peek("[A-Za-z_]"))
            return lexIdentifier();

        // Number
        else if (peek("[+\\-]", "[0-9]") || peek("[0-9]"))
            return lexNumber();

        // Character
        else if (peek("'"))
            return lexCharacter();

        // String
        else if (peek("\""))
            return lexString();

        // Operator
        else
            return lexOperator();
    }

    public Token lexIdentifier() {
        while (peek("[A-Za-z_0-9]"))
            match("[A-Za-z_0-9]");
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek("[+\\-]"))
            match("[+\\-]");
        while (peek("\\d"))
            match("\\d");
        if (peek(".", "\\d")) {
            match(".");
            while (peek("\\d"))
                match("\\d");
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        if (peek("'"))
            match("'");
        // Catch escape
        if (peek("\\\\") && !peek("\\\\", "'"))
            lexEscape();
        else if (peek("[^'\\n\\r\\\\]"))   ////// CHECK THIS TO SEE IF /n ALLOWED
            match("[^'\\n\\r\\\\]");
        else
            throw new ParseException("Illegal Character", chars.index);
        if (peek("'")) {
            match("'");
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("Illegal Character", chars.index);
    }

    public Token lexString() {
        // Match starting "
        if (peek("\""))
            match("\"");
        // String contents loop
        while (peek("[^\"]")) {
            // Match legal escape
            if (peek("\\\\") && !peek("\\\\", "\""))
                lexEscape();
            // Match any other Char
            else
                match("[^\"\\n\\r\\\\]");
        }
        // Math final "
        if (peek("\""))
            match("\"");
        else
            throw new ParseException("Unterminated String", chars.index);
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        // Test escapes for legal, if not, throw exception
        if (peek("\\\\", "[bnrt'\"\\\\]"))
            match("\\\\", "[bnrt'\"\\\\]");
        else
            throw new ParseException("Illegal Escape", chars.index);
    }

    public Token lexOperator() {
        if (peek("[<>!=]", "="))
            match("[<>!=]", "=");
        else
            match(".");
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) ||
                    !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return true;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
