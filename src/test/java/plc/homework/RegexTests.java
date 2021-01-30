// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                // Provided Tests 1-4
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                // My Tests 5-12
                Arguments.of("Mixed Cases", "tEsTeMaIl@gmail.com", true),
                Arguments.of("Dot in Name", "this.shouldwork@gmail.com", true),
                Arguments.of("Single Char Name", "t@gmail.com", true),
                Arguments.of("Multiple dots in Name", "this.should.also.work@gmail.com", true),
                Arguments.of("Empty Domain (testing the *)", "test123@.com", true),
                Arguments.of("Empty Beginning (testing the +)", "@gmail.com", false),
                Arguments.of("Spaces", "thisis notgoingtowork@gmail.com", false),
                Arguments.of("Multiple dots in domain", "test@gmail.co.uk", false),
                Arguments.of("Domain Ending < 2", "test@gmail.c", false),
                Arguments.of("Domain Ending > 3", "test@gmail.coom", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                // My tests 5 - 12
                Arguments.of("16 Characters", "this1sAw3someyea", true),
                Arguments.of("18 Characters", "_this1sAw3someyea!", true),
                Arguments.of("20 Characters Max", "automobileelibomotua", true),
                Arguments.of("5 Characters", "char5", false),
                Arguments.of("21 Characters", "automobileelibomotua!", false),
                Arguments.of("Empty", "", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                Arguments.of("Empty", "[]", true),
                Arguments.of("Empty Spaces", "[ ]", true),
                Arguments.of("Trailing Comma", "[1,2,3,]", false),
                Arguments.of("Spaces", "[1, 2, 3 ]", true),
                Arguments.of("One bracket Front", "[1 2 3", false),
                Arguments.of("One bracket Back", "1 2 3]", false),
                Arguments.of("Various Spaces", "[1, 2,   3 ]", true),
                Arguments.of("Letters", "[1,2,A]", false),
                Arguments.of("Symbols", "[1,2,$]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Single Dig Decimal", "1.0", true),
                Arguments.of("No Decimal", "123", true),
                Arguments.of("Missing Right", "1.", false),
                Arguments.of("Missing Left", ".5534", false),
                Arguments.of("Leading 0's", "00005.5534", true),
                Arguments.of("Leading 0's Decimal", "0000.5534", true),
                Arguments.of("Single Leading 0", "0.5534", true),
                Arguments.of("Single Leading 0 Decimal", "05.5534", true),
                Arguments.of("+ Sign", "+10.5", true),
                Arguments.of("- Sign", "-10.5", true),
                Arguments.of("+ Sign Space", "+ 10.5", false),
                Arguments.of("- Sign Space", "- 10.5", false),
                Arguments.of("Single Trailing 0", "10.0", true),
                Arguments.of("Single Trailing 0 Decimal", "10.50", true),
                Arguments.of("Trailing 0's", "10.0000", true),
                Arguments.of("Trailing 0's Decimal", "10.50000", true),
                Arguments.of("Letter", "12X", false),
                Arguments.of("Symbol", "12$", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("String", "\"This is a string\"", true),
                Arguments.of("Not String", "This is not a string", false),
                Arguments.of("Correct Escape", "\"This is \\t legal!\"", true),
                Arguments.of("Incorrect Escape", "\"This is \\g not legal\"", false),
                Arguments.of("Only Slash", "\"This is not allowed \\ right?\"", false),
                Arguments.of("Only Start", "\"This is not legal", false),
                Arguments.of("Only End", "This is also not legal\"", false),
                Arguments.of("All Types of Characters", "\"1234 ,_+-# ABCD !!\"", true),
                Arguments.of("Characters Outside of Quotes", "\"This should\" not work", false),
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("New Line", "\"These are\ntwo lines\"", true)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
