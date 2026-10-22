package simpledb.opt;

import simpledb.metadata.MetadataMgr;
import simpledb.parse.Parser;
import simpledb.parse.QueryData;
import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * The following experiment compares three specific join strategies
 * listed in the JoinStrategy enum. They are:
 * 1. Block-Nested Only
 * 2. Sort-Merge Only
 * 3. Planner Chooses: Index or Hash
 * This is done on the same join query found in the statement variable below on four tables:
 * student, enroll, section, dept
 * The strategies are allowed one warmup round and then three runs are timed before
 * averaging to get the final timing.
 */

public class FourTableJoinBenchmarkTest {
   private static final String statement = 
      "select sname, grade, dname, sectid " +
      "from student, enroll, section, dept " +
      "where sid = studentid and sectionid = sectid and majorid = did";

   private static final JoinStrategy[] STRATEGIES = {
      JoinStrategy.BLOCK_NESTED_LOOPS, JoinStrategy.SORT_MERGE, JoinStrategy.INDEX_OR_HASH
   };

   private static final int WARMUPS = 1;
   private static final int RUNS = 3;
   
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      MetadataMgr mdm = db.mdMgr();
      Long expectedCount = null;

      // this 2D array stores all the measured runtimes across all join strategies!
      double[][] allTimings = new double[STRATEGIES.length][RUNS];

      // print out the 4-way join statement
      System.out.println("Join Statement: " + statement);

      for (int i = 0; i < STRATEGIES.length; i++) {
         JoinStrategy strategy = STRATEGIES[i];
         double[] times = allTimings[i];

         for (int run = -WARMUPS; run < RUNS; run++) {
            Transaction tx = db.newTx();

            try {
               QueryData data = new Parser(statement).query();
               Plan p = new HeuristicQueryPlanner(mdm, strategy).createPlan(data, tx);

               // this prints out the name and query plan for each join strategy
               // this is printed during the warmup round where no timing is captured yet
               if (run == -WARMUPS) {
                  System.out.println("\n=== " + strategy + " ===");
                  System.out.println(p.printQueryPlan(0));
               }

               long start = System.nanoTime();
               long rows = countRows(p);
               long elapsedNs = System.nanoTime() - start;

               // verify that all join strategies return the same row count!
               if (expectedCount == null) expectedCount = rows;
               else if (rows != expectedCount) {
                  throw new IllegalStateException(strategy + " produced " + rows + " rows, expected " + expectedCount);
               }

               if (run >= 0) times[run] = elapsedNs / 1_000_000.0;
               tx.commit();
            } catch (RuntimeException exception) {
               tx.rollback();
               throw exception;
            }
         }
      }

      System.out.println("====== Experiment 2 Join Strategy Runtimes ======");
      System.out.printf("%-21s %12s %12s %12s %12s%n",
         "Strategy", "Run 1 (ms)", "Run 2 (ms)", "Run 3 (ms)", "Average (ms)"
      );

      for (int i = 0; i < STRATEGIES.length; i++) {
         double[] times = allTimings[i];
         double average = (times[0] + times[1] + times[2]) / RUNS;
         System.out.printf("%-21s %12.3f %12.3f %12.3f %12.3f%n",
            STRATEGIES[i], times[0], times[1], times[2], average
         );
      }

      System.out.println("Matching rows (each plan): " + expectedCount);
   }

   // this counts the number of rows by counting the number of times scan.next() returned a tuple
   private static long countRows(Plan p) {
      Scan scan = p.open();
      long count = 0;
      try {
         while (scan.next()) count++;
      } finally { scan.close(); };

      return count;
   }
}
