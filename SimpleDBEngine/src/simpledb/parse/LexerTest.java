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
			String lhs = eatExpression(lex);
			String opr = eatOperator(lex);
			String rhs = eatExpression(lex);
			System.out.println(lhs + ' ' + opr + ' ' + rhs);
		}
		sc.close();
	}

	private static String eatExpression(Lexer lex) {
		if (lex.matchId())
			return lex.eatId();
		else if (lex.matchIntConstant())
			return Integer.toString(lex.eatIntConstant());
		else
			return "'" + lex.eatStringConstant() + "'";
	}

	private static String eatOperator(Lexer lex) {
		if (lex.matchOpr('<')) {
			lex.eatOpr('<');
			if (lex.matchOpr('=')) {
				lex.eatOpr('=');
				return "<=";
			}
			else if (lex.matchOpr('>')) {
				lex.eatOpr('>');
				return "<>";
			}
			return "<";
		}
		else if (lex.matchOpr('>')) {
			lex.eatOpr('>');
			if (lex.matchOpr('=')) {
				lex.eatOpr('=');
				return ">=";
			}
			return ">";
		}
		else if (lex.matchOpr('!')) {
			lex.eatOpr('!');
			lex.eatOpr('=');
			return "!=";
		}
		else {
			lex.eatOpr('=');
			return "=";
		}
	}
}
