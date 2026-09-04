package simpledb.parse;

import java.util.Scanner;

public class ParserTestActions {
   public static void main(String[] args) {
      if (args.length > 0 && args[0].equals("--self-test")) {
         runSelfTests();
         return;
      }

      Scanner sc = new Scanner(System.in);
      System.out.print("Enter an SQL statement: ");
      while (sc.hasNext()) {
         String s = sc.nextLine();
         try {
            String result = parseStatement(s);
            System.out.println("Your statement is: " + result);
         } catch (BadSyntaxException ex) {
            System.out.println("Your statement is illegal");
         }
         System.out.print("Enter an SQL statement: ");
      }
      sc.close();
   }

   private static String parseStatement(String s) {
      Parser p = new Parser(s);
      if (s.startsWith("select"))
         return p.query().toString();
      else
         return p.updateCmd().getClass().toString();
   }

   private static void runSelfTests() {
      String[][] tests = {
            {"select a from t", "select a from t"},
            {"select b from t order by a", "select b from t order by a"},
            {"select b from t order by a desc", "select b from t order by a DESC"},
            {"select sid, sname, gradyear from student order by gradyear asc, sname desc",
                  "select sid, sname, gradyear from student order by gradyear, sname DESC"}
      };

      for (String[] test : tests) {
         String actual = parseStatement(test[0]);
         if (!actual.equals(test[1]))
            throw new RuntimeException("Expected \"" + test[1] + "\" but got \"" + actual + "\"");
         System.out.println("yes: " + actual);
      }
   }
}
