package simpledb.materialize;


import simpledb.index.planner.IndexUpdatePlanner;
import simpledb.metadata.MetadataMgr;
import simpledb.opt.HeuristicQueryPlanner;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.plan.QueryPlanner;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;
import simpledb.plan.TablePlan;
import simpledb.plan.UpdatePlanner;

public class HashJoinPlanTest {
	public static void main(String[] args) throws Exception {
		// hash join test
		int blocksize = 128;
		int buffers = 3;
		SimpleDB db = new SimpleDB("hashjointest", blocksize, buffers);
		Transaction tx = db.newTx();
		MetadataMgr mdm = new MetadataMgr(true, tx);
		
		Schema sch1 = new Schema();
		sch1.addIntField("a");
		sch1.addStringField("b", 8);
		
		Schema sch2 = new Schema();
		sch2.addIntField("c");
		sch2.addStringField("d", 8);
		
		
		QueryPlanner qp = new HeuristicQueryPlanner(mdm);
	    UpdatePlanner up = new IndexUpdatePlanner(mdm);
	    Planner planner = new Planner(qp, up);

	    planner.executeUpdate("create table T1(a int, b varchar(8))", tx);
	    planner.executeUpdate("create table T2(c int, d varchar(8))", tx);
	    
		planner.executeUpdate("insert into T1(a, b) values "
				+ "(1, 'b1')"
				+ "(2, 'b2')"
				+ "(4, 'b4')", tx);
		
		planner.executeUpdate("insert into T2(c, d) values "
				+ "(1, 'd1')"
				+ "(3, 'd3')"
				+ "(4, 'd4')", tx);
		
		
		TablePlan t1 = new TablePlan(tx, "T1", mdm);
		TablePlan t2 = new TablePlan(tx, "T2", mdm);
		
      	Plan p = new HashJoinPlan(tx, t1, t2, "a", "c");
      	Scan s = p.open();
      	while (s.next()) {
      		System.out.println(s.getString("b"));
      	}
   }
}
