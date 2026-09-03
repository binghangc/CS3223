package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.Scan;

public class PlannerTest1 {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("plannertest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();
      String cmd = "create table T1(A int, B varchar(9))";
      planner.executeUpdate(cmd, tx);
      
      cmd = "delete from T1"; // delete all entries from previous test
      planner.executeUpdate(cmd, tx);

      
      int n = 200;
      System.out.println("Inserting " + n + " random records.");
      for (int i=0; i<n; i++) {
         int a = (int) Math.round(Math.random() * 50);
         String b = "rec-" + a;
         cmd = "insert into T1(A,B) values(" + a + ", '" + b + "')";
         planner.executeUpdate(cmd, tx);
      }

      System.out.println("Selecting all records with A=10");
      String qry = "select B from T1 where A=10";
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      System.out.println("Selecting all records with 4<A<8 asc");
      qry = "select B from T1 where A>4 and A<8 order by A";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      System.out.println("Selecting all records with 4<A<8 desc");
      qry = "select B from T1 where A>4 and A<8 order by A desc";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      tx.commit();
   }
}

