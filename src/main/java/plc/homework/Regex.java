// Jeremy DePoyster
// University of Florida
// COP4020 Spring 2021 Online

package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._\\-]+@[A-Za-z0-9-]*\\.[a-z]{2,3}"),
            EVEN_STRINGS = Pattern.compile("(..){5,10}"),
            INTEGER_LIST = Pattern.compile("\\[[ ]*(((\\d,[ ]*)*\\d)|)[ ]*\\]"),
            NUMBER = Pattern.compile("[+-]?0*[\\d]+(\\.\\d+)?"),
            STRING = Pattern.compile("\\\"([^\\\\]|\\\\[bnrt'\\\"\\\\])*\\\"");
}
