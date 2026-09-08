package simpledb.multibuffer;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.plan.*;
import simpledb.query.Expression;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.query.Term;

public class MultibufferProductPlanTest {
   public static void main(String[] args) throws Exception {
      SimpleDB db = new SimpleDB("multiBufferProductPlanTest");
      Transaction tx = db.newTx();
      MetadataMgr mdm = db.mdMgr();
      Planner planner = db.planner();
      
      String cmd = "create table temp1(A int, B varchar(9))";
      planner.executeUpdate(cmd, tx);
      
      cmd = "create table temp2(C int, D varchar(9))";
      planner.executeUpdate(cmd, tx);
      
      
      int n = 3;
      System.out.println("Inserting " + n + " records into temp1.");
      for (int i=0; i<n; i++) {
         int a = i;
         String b = "aaa" + a;
         cmd = "insert into temp1(A,B) values(" + a + ", '" + b + "')";
         planner.executeUpdate(cmd, tx);
      }
      System.out.println("Inserting " + n + " records into temp2.");
      for (int i=0; i<n; i++) {
          int a = i;
          String b = "ccc" + a;
          cmd = "insert into temp2(C,D) values(" + a + ", '" + b + "')";
          planner.executeUpdate(cmd, tx);
       }
      
      cmd = "select b, d from temp1, temp2";
      System.out.println("Select");
      Plan p = planner.createQueryPlan(cmd, tx);
      Scan s2 = p.open();
      
      while (s2.next())
          System.out.println(s2.getString("b") + ' ' + s2.getString("d")); 
       s2.close();
      
      tx.commit();
      
      Term t = new Term(new Expression("a"), "=", new Expression("c"));
      Predicate pred = new Predicate(t);

      Plan p1 = new TablePlan(tx, "temp1", mdm);
      Plan p2 = new TablePlan(tx, "temp2", mdm);
      System.out.println(p1.schema().hasField("a"));
      System.out.println(p1.schema().hasField("b"));
      System.out.println(p2.schema().hasField("c"));
      Plan pp = new MultibufferProductPlan(tx, p1, p2);
      
      System.out.println(pp.schema().hasField("a"));
      System.out.println(pp.schema().hasField("c"));
      
      Scan s = pp.open();
      while (s.next())
          System.out.println(s.getString("b") + ' ' + s.getString("d")); 
       s.close();
      
      tx.commit();
   }
}
