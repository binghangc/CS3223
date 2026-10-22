package simpledb.plan;

import java.util.Map;

import simpledb.index.planner.IndexJoinPlan;
import simpledb.materialize.HashJoinPlan;
import simpledb.materialize.MergeJoinPlan;
import simpledb.metadata.IndexInfo;
import simpledb.metadata.MetadataMgr;
import simpledb.multibuffer.MultibufferProductPlan;
import simpledb.query.Expression;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.query.Term;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * The following experiment compares four join algorithms
 * on the same join query involving two tables. The join algos are:
 * 1. Block-Nested Join
 * 2. Sort-Merge Join
 * 3. Index Join
 * 4. GRACE Hash Join
 * The following two tables - enroll and student - are joined.
 * The strategies are allowed one warmup round and then three runs are timed before
 * averaging to get the final timing.
 */

public class TwoTableJoinBenchmarkTest {
   
   private static final String[] ALGORITHMS = {
      "Block Nested-Loops", "Sort-Merge", "Index-Based", "GRACE Hash"
   };

   private static final int WARMUP_RUNS = 1;
   private static final int MEASURED_RUNS = 3;

   /**
    * main function running the experiment
    */
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("studentdb");
      MetadataMgr mdm = db.mdMgr();
      
      // holds all the timings across all algos across three measured runs
      double[][] timings = new double[ALGORITHMS.length][MEASURED_RUNS];

      long expectedRows = -1;

      for (int algorithm = 0; algorithm < ALGORITHMS.length; algorithm++) {
         for (int run = -WARMUP_RUNS; run < MEASURED_RUNS; run++) {
            Transaction tx = db.newTx();

            try {
               // Exclude planning out of timing
               Plan plan = createJoinPlan(algorithm, tx, mdm);

               long start = System.nanoTime();
               long rows = countRows(plan);
               long elapsedTime = System.nanoTime() - start;

               if (expectedRows == -1) expectedRows = rows;
               else if (rows != expectedRows) {
                  throw new IllegalArgumentException(ALGORITHMS[algorithm]
                     + " produced " + rows + " rows, expected " + expectedRows
                  );
               }

               // compute and store the run time after warmup run
               if (run >= 0) timings[algorithm][run] = elapsedTime / 1_000_000.0;

               tx.commit();
            } catch (RuntimeException exception) {
               tx.rollback();
               throw exception;
            }
         }
      }

      System.out.println("Join on: enroll.studentid = student.sid");
      System.out.println("Matching rows: " + expectedRows);
      System.out.printf("%-21s %12s %12s %12s %12s%n",
                        "Algorithm", "Run 1 (ms)", "Run 2 (ms)",
                        "Run 3 (ms)", "Average (ms)"
      );
      
      // compute the average and print out the timings
      for (int al = 0; al < ALGORITHMS.length; al++) {
         double average = 0;
         for (double elapsed : timings[al]) average += elapsed;
         average /= MEASURED_RUNS;
         
         System.out.printf("%-21s %12.3f %12.3f %12.3f %12.3f%n",
            ALGORITHMS[al], timings[al][0], timings[al][1], 
            timings[al][2], average
         );
      }
   }

   /**
    * Creates the join plan for each respective join algorithms tested
    * in the list ALGORITHMS (expected 4)
    */
   private static Plan createJoinPlan(int algorithm, Transaction tx, MetadataMgr mdm) {
      Plan enroll = new TablePlan(tx, "enroll", mdm);
      Plan student = new TablePlan(tx, "student", mdm);

      if (algorithm == 0) { // block-nested loops
         Predicate join = new Predicate(new Term(
            new Expression("studentid"), "=", new Expression("sid")
         ));

         return new MultibufferProductPlan(tx, enroll, student, join);
      } else if (algorithm == 1) {  // sort-merge
         return new MergeJoinPlan(tx, enroll, student, "studentid", "sid");
      } else if (algorithm == 2) {  // index based (using index on sid)
         Map<String, IndexInfo> indexes = mdm.getIndexInfo("student", tx);
         IndexInfo indexOnSid = indexes.get("sid");
         if (indexOnSid == null) {
            throw new IllegalStateException(
               "An index on student.sid is required for the index join. " +
               "Available indexed fields: " + indexes.keySet() 
            );
         }

         return new IndexJoinPlan(enroll, student, indexOnSid, "studentid");
      } else if (algorithm == 3) { // GRACE Hash
         return new HashJoinPlan(tx, enroll, student, "studentid", "sid");
      } else {
         throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
      }
   }
   
   // this counts the number of rows by counting the number of times scan.next() returned a tuple
   private static long countRows(Plan plan) {
      Scan scan = plan.open();
      long count = 0;

      try {
         while (scan.next()) count++;
      } finally {
         scan.close();
      }

      return count;
   }

}
