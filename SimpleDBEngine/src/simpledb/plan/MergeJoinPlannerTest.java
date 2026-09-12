package simpledb.plan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

public class MergeJoinPlannerTest {
   public static void main(String[] args) {
      String dbname = System.getProperty("java.io.tmpdir") + File.separator + "simpledb-mergejoin-test";
      deleteRecursively(new File(dbname));

      SimpleDB db = new SimpleDB(dbname);
      Transaction tx = db.newTx();
      Planner planner = db.planner();

      planner.executeUpdate("create table T1(A int, B varchar(9))", tx);
      planner.executeUpdate("create table T2(C int, D varchar(9))", tx);

      planner.executeUpdate("insert into T1(A,B) values(1, 'a1')", tx);
      planner.executeUpdate("insert into T1(A,B) values(1, 'a2')", tx);
      planner.executeUpdate("insert into T1(A,B) values(2, 'a3')", tx);
      planner.executeUpdate("insert into T1(A,B) values(4, 'a4')", tx);

      planner.executeUpdate("insert into T2(C,D) values(1, 'b1')", tx);
      planner.executeUpdate("insert into T2(C,D) values(1, 'b2')", tx);
      planner.executeUpdate("insert into T2(C,D) values(2, 'b3')", tx);
      planner.executeUpdate("insert into T2(C,D) values(3, 'b4')", tx);

      String qry = "select B,D from T1,T2 where A=C";
      PrintStream originalOut = System.out;
      ByteArrayOutputStream plannerOutput = new ByteArrayOutputStream();
      Plan p;
      System.setOut(new PrintStream(plannerOutput));
      try {
         p = planner.createQueryPlan(qry, tx);
      }
      finally {
         System.setOut(originalOut);
      }
      String planningTrace = plannerOutput.toString();
      boolean usedMergeJoin = planningTrace.contains("Using MergeJoinPlan");

      System.out.print(planningTrace);
      Scan s = p.open();

      int count = 0;
      System.out.println("Merge join planner test results:");
      while (s.next()) {
         System.out.println(s.getString("b") + " " + s.getString("d"));
         count++;
      }
      s.close();
      tx.commit();

      System.out.println("Expected merge join: true");
      System.out.println("Used merge join: " + usedMergeJoin);
      System.out.println("Expected records: 5");
      System.out.println("Actual records: " + count);
      System.out.println(usedMergeJoin && count == 5 ? "PASS" : "FAIL");
   }

   private static void deleteRecursively(File file) {
      if (!file.exists())
         return;
      if (file.isDirectory())
         for (File child : file.listFiles())
            deleteRecursively(child);
      file.delete();
   }
}
