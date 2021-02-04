package plc.project;

public class CharStreamTest {
    public static void main(String args[]) {
        Lexer lexer = new Lexer("0123210a0b1c2");
       
        System.out.println("first char 0?  " + lexer.peek("0"));
        System.out.println("first char 1?  " + lexer.peek("1"));
        System.out.println("first char digit?  " + lexer.peek("\\d"));
        System.out.println("initial chars 0 through 3?  " + lexer.peek("0", "1", "2", "3"));
        System.out.println("initial chars 0 through 4?  " + lexer.peek("0", "1", "2", "3", "4"));
        System.out.println("initial chars 0 through 0123210?  " + lexer.peek("0", "1", "2", "3", "2", "1", "0"));
        
        
        lexer = new Lexer("== 5;");
        System.out.println("test for ==, pass?  " + lexer.peek("=", "=") );
        if ( lexer.match("=","=") ) {
           // chars has private access in Lexer
           // so we can't actually perform this method call in
           // this sample test class, but you have the idea...
            Token x = lexer.chars.emit(Token.Type.OPERATOR);
            System.out.println(x.getIndex());
            System.out.println(x.getLiteral());
            System.out.println(x.getType());
        }
        System.out.println("test for ' ', pass?  " + lexer.peek(" ") );
        if (lexer.match(" ")) {
            Token x = lexer.chars.emit(Token.Type.STRING);
            System.out.println(x.getIndex());
            System.out.println(x.getLiteral());
            System.out.println(x.getType());
        }

        System.out.println("current char digit?  " + lexer.peek("\\d") );
        System.out.println("current char digit?  " + lexer.peek("[0-9]") );
        System.out.println("current char not digit?  " + lexer.peek("[^0-9]") );
    }
}