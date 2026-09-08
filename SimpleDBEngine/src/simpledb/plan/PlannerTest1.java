package simpledb.plan;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.query.Scan;

public class PlannerTest1 {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("plannertest1");
      Transaction tx = db.newTx();
      Planner planner = db.planner();
      String cmd = "create table tempT1(A int, B varchar(9))";
      planner.executeUpdate(cmd, tx);

      
      int n = 50;
      System.out.println("Inserting " + n + " random records.");
      for (int i=0; i<n; i++) {
         int a = (int) Math.round(Math.random() * 50);
         String b = "rec-" + a;
         cmd = "insert into tempT1(A,B) values(" + a + ", '" + b + "')";
         planner.executeUpdate(cmd, tx);
      }

      System.out.println("Selecting all records with A=10");
      String qry = "select B from tempT1 where A=10";
      Plan p = planner.createQueryPlan(qry, tx);
      Scan s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      // should be unordered
      System.out.println("Selecting all records with 4<A<8");
      qry = "select B from tempT1 where A>4 and A<8";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      // ordered by A ascending
      System.out.println("Selecting all records with 4<A<8 in ascending order");
      qry = "select B from tempT1 where A>4 and A<8 order by A";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      // ordered by A descending
      System.out.println("Selecting all records with 4<A<8 desc");
      qry = "select B from tempT1 where A>4 and A<8 order by A desc";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b")); 
      s.close();
      
      
      // ordered by B descending
      System.out.println("Selecting all records with 4<A<8 desc");
      qry = "select B from tempT1 where A>4 and A<8 order by A desc";
      p = planner.createQueryPlan(qry, tx);
      s = p.open();
      while (s.next())
         System.out.println(s.getString("b"));
      
      s.close();
      
      tx.commit();
   }
}

