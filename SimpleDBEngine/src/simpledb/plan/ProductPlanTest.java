package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.metadata.MetadataMgr;
import simpledb.query.Expression;
import simpledb.query.Predicate;
import simpledb.query.Scan;
import simpledb.query.Term;

public class ProductPlanTest {
   public static void main(String[] args) throws Exception {
      SimpleDB db = new SimpleDB("producttest");
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

      tx.commit();
      
      Term t = new Term(new Expression("a"), "=", new Expression("c"));
      Predicate pred = new Predicate(t);

      Plan p1 = new TablePlan(tx, "temp1", mdm);
      Plan p2 = new TablePlan(tx, "temp2", mdm);
      Plan pp = new ProductPlan(p1, p2);
      

      Plan sp = new SelectPlan(pp, pred);
      
      Scan s = sp.open();
      while (s.next())
          System.out.println(s.getString("b") + ' ' + s.getString("d")); 
       s.close();
      
      tx.commit();
   }
}
