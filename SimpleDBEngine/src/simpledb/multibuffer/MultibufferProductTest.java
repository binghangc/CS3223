package simpledb.multibuffer;

import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.materialize.TempTable;
import simpledb.metadata.MetadataMgr;
import simpledb.plan.*;
import simpledb.query.Expression;
import simpledb.query.Predicate;
import simpledb.query.ProductScan;
import simpledb.query.Scan;
import simpledb.query.Term;
import simpledb.query.UpdateScan;
import simpledb.record.Layout;
import simpledb.record.Schema;
import simpledb.record.TableScan;

public class MultibufferProductTest {
	public static void main(String[] args) throws Exception {
		SimpleDB db = new SimpleDB("multibufferproducttest");
		Transaction tx = db.newTx();

		Schema sch1 = new Schema();
		sch1.addIntField("A");
		sch1.addStringField("B", 9);
		Layout layout1 = new Layout(sch1);
		TableScan ts1 = new TableScan(tx, "T1", layout1);

		ts1.beforeFirst();
		int n = 3;
		System.out.println("Inserting " + n + " records into T1.");
		for (int i = 0; i < n; i++) {
			ts1.insert();
			ts1.setInt("A", i);
			ts1.setString("B", "bbb" + i);
		}
		ts1.close();


		Schema sch2 = new Schema();
		sch2.addIntField("C");
		sch2.addStringField("D", 9);
		TempTable tt = new TempTable(tx, sch2);
		UpdateScan s = tt.open();
		for (int i = 0; i < n; i++) {
			s.insert();
			s.setInt("C", i);
			if (i % 2 == 0) s.setString("D", "bbb" + i);
			else s.setString("D", "ddd" + i);
		}
		s.close();


		System.out.println("Selecting product of T1 and T2");
		Scan s1 = new TableScan(tx, "T1", layout1);		
		Scan s2 = new MultibufferProductScan(tx, s1, tt.tableName(), tt.getLayout());
		while (s2.next())
			System.out.println(s2.getString("B") + ' ' + s2.getString("D"));
		System.out.println();
		s2.close();
		
		
		System.out.println("Selecting join of T1 and T2 on A=C");
		s1 = new TableScan(tx, "T1", layout1); // unpinned by s.close()
		Predicate pred = new Predicate(new Term(new Expression("A"), "=", new Expression("C")));
		s2 = new MultibufferProductScan(tx, s1, tt.tableName(), tt.getLayout(), pred);
		while (s2.next())
			System.out.println(s2.getString("B") + ' ' + s2.getString("D"));
		System.out.println();
		s2.close();

		
		
		System.out.println("Selecting join of T1 and T2 on A=C and B=D");
		s1 = new TableScan(tx, "T1", layout1); // unpinned by s.close()
		pred.conjoinWith(new Predicate(new Term(new Expression("B"), "=", new Expression("D"))));
		s2 = new MultibufferProductScan(tx, s1, tt.tableName(), tt.getLayout(), pred);
		while (s2.next())
			System.out.println(s2.getString("B") + ' ' + s2.getString("D"));
		System.out.println();
		s2.close();
		
		
		
		
		
		sch2 = new Schema();
		sch2.addIntField("C");
		sch2.addStringField("D", 9);
		tt = new TempTable(tx, sch2);
		s = tt.open();
		for (int i = 0; i < n; i++) {
			s.insert();
			s.setInt("C", i+3);
			s.setString("D", "bbb" + i+3);
		}
		s.close();

		
		s1 = new TableScan(tx, "T1", layout1); // unpinned by s.close()
		
		System.out.println("Selecting join of T1 and T2 on A=C with 0 match");
		pred = new Predicate(new Term(new Expression("A"), "=", new Expression("C")));
		s2 = new MultibufferProductScan(tx, s1, tt.tableName(), tt.getLayout(), pred);
		while (s2.next())
			System.out.println(s2.getString("B") + ' ' + s2.getString("D"));
		System.out.println();
		s2.close();
		
	

		tx.rollback();
	}
}
