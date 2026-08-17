package simpledb.parse;
import java.util.Scanner;

// Will successfully read in lines of text denoting an
// SQL expression of the form "id opr c" or "c opr id".
// Example: joe=1, joe <= 1, 1 == Joe

public class LexerTest {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {
			String s = sc.nextLine();
			Lexer lex = new Lexer(s);
			String x = null; String opr = null; Integer y = null;
			
			if (lex.matchId()) {
				x = lex.eatId();
			} else {
				y = lex.eatIntConstant();
			}
			
			if (lex.matchOpr('<')) {
				lex.eatOpr('<');
				opr = "<";
				if (lex.matchOpr('=')) {
					lex.eatOpr('=');
					opr += '=';
				}
			} else if (lex.matchOpr('=')) {
				lex.eatOpr('=');
				opr = "=";
				if (lex.matchOpr('=')) {
					lex.eatOpr('=');
					opr += '=';
				}
			}
			
			if (x == null) {
				x = lex.eatId();
			} else {
				y = lex.eatIntConstant();
			}
			
			System.out.println(x + ' ' + opr + ' ' + y);
		}
		sc.close();
	}
}
