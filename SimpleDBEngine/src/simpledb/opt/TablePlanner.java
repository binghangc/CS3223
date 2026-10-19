package simpledb.opt;

import java.util.Map;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.*;
import simpledb.metadata.*;
import simpledb.index.planner.*;
import simpledb.materialize.HashJoinPlan;
import simpledb.materialize.MergeJoinPlan;
import simpledb.multibuffer.MultibufferProductPlan;
import simpledb.plan.*;

/**
 * This class contains methods for planning a single table.
 * 
 * @author Edward Sciore
 */
class TablePlanner {
   private TablePlan myplan;
   private Predicate mypred;
   private Schema myschema;
   private Map<String, IndexInfo> indexes;
   private Transaction tx;

   /**
    * Creates a new table planner.
    * The specified predicate applies to the entire query.
    * The table planner is responsible for determining
    * which portion of the predicate is useful to the table,
    * and when indexes are useful.
    * 
    * @param tblname the name of the table
    * @param mypred  the query predicate
    * @param tx      the calling transaction
    */
   public TablePlanner(String tblname, Predicate mypred, Transaction tx, MetadataMgr mdm) {
      this.mypred = mypred;
      this.tx = tx;
      myplan = new TablePlan(tx, tblname, mdm);
      myschema = myplan.schema();
      indexes = mdm.getIndexInfo(tblname, tx);
   }

   /**
    * Constructs a select plan for the table.
    * The plan will use an indexselect, if possible.
    * 
    * @return a select plan for the table.
    */
   public Plan makeSelectPlan() {
      Plan p = makeIndexSelect();
      if (p == null)
         p = myplan;
      return addSelectPred(p);
   }

   /**
    * Constructs a join plan of the specified plan
    * and the table. The plan will use the best plan out of indexjoin, hashjoin, mergejoin, or productjoin.
    * The method returns null if no join is possible.
    * 
    * @param current the specified plan
    * @return a join plan of the plan and this table
    */
   public Plan makeJoinPlan(Plan current) {
      Schema currsch = current.schema();
      Predicate joinpred = mypred.joinSubPred(myschema, currsch);
      if (joinpred == null)
         return null;
      Plan indexPlan = makeIndexJoin(current, currsch);
      Plan hashPlan = makeHashJoin(current, currsch);
      Plan mergePlan = makeMergeJoin(current, currsch);
      Plan productPlan = makeProductJoin(current, currsch);
      
      Plan bestPlan = indexPlan;
      String planName = "Index Join";
      if (bestPlan == null || mergePlan.blocksAccessed() < bestPlan.blocksAccessed()) { bestPlan = mergePlan; planName = "Merge Join"; };
      if (bestPlan == null || hashPlan.blocksAccessed() < bestPlan.blocksAccessed()) { bestPlan = hashPlan; planName = "Hash Join"; };
      if (bestPlan == null || productPlan.blocksAccessed() < bestPlan.blocksAccessed()) { bestPlan = productPlan; planName = "Product Join"; };
      
      // Print statement to check which plan is used.
      System.out.println("Index cost: " + indexPlan.blocksAccessed());
      System.out.println("Hash cost: " + hashPlan.blocksAccessed());
      System.out.println("Merge cost: " + mergePlan.blocksAccessed());
      System.out.println("Product cost: " + productPlan.blocksAccessed());
      System.out.println("Using " + planName);
      
      return bestPlan;
   }

   /**
    * Constructs a product plan of the specified plan and
    * this table.
    * 
    * @param current the specified plan
    * @return a product plan of the specified plan and this table
    */
   public Plan makeProductPlan(Plan current) {
      Plan p = addSelectPred(myplan);
      return new MultibufferProductPlan(tx, current, p);
   }

   private Plan makeIndexSelect() {
      for (String fldname : indexes.keySet()) {
         Constant val = mypred.equatesWithConstant(fldname);
         if (val != null) {
            IndexInfo ii = indexes.get(fldname);
            System.out.println("Index Select on " + fldname + " used");
            return new IndexSelectPlan(myplan, ii, val);
         }
      }
      return null;
   }

   private Plan makeIndexJoin(Plan current, Schema currsch) {
      // diagnostic print statements to check if index join is running correctly
      System.out.println("Trying index join. Inner table fields: " + myschema.fields());
      System.out.println("Available indexes " + indexes.keySet());
      System.out.println("Outer fields " + currsch.fields());

      for (String fldname : indexes.keySet()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            IndexInfo ii = indexes.get(fldname);
            Plan p = new IndexJoinPlan(current, myplan, ii, outerfield);            

            p = addSelectPred(p);
            return addJoinPred(p, currsch, fldname, outerfield);
         }
      }
      return null;
   }

   private Plan makeMergeJoin(Plan current, Schema currsch) {
      for (String fldname : myschema.fields()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            Plan rhs = addSelectPred(myplan);
            Plan p = new MergeJoinPlan(tx, current, rhs, outerfield, fldname);
            return addJoinPred(p, currsch, fldname, outerfield);
         }
      }
      return null;
   }

   private Plan makeProductJoin(Plan current, Schema currsch) {
      Plan p = addSelectPred(myplan);
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      return new MultibufferProductPlan(tx, current, p, joinpred);
   }

   private Plan makeHashJoin(Plan current, Schema currsch) {
	   for (String fldname : myschema.fields()) {
		   String outerfield = mypred.equatesWithField(fldname);
		   if (outerfield != null && currsch.hasField(outerfield)) {
			   Plan rhs = addSelectPred(myplan);
			   Plan p = new HashJoinPlan(tx, current, rhs, outerfield, fldname);
			   return addJoinPred(p, currsch, fldname, outerfield);
		   }
	   }
	   return null;
   }
   
   /**
    * Add the select predicates that apply to the current schema, if any.
    */
   private Plan addSelectPred(Plan p) {
      Predicate selectpred = mypred.selectSubPred(myschema);
      if (selectpred != null)
         return new SelectPlan(p, selectpred);
      else
         return p;
   }

   /**
    * Add the select predicates that apply to the union of both schemas, but not individual schemas.
    */
   private Plan addJoinPred(Plan p, Schema currsch) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred != null)
         return new SelectPlan(p, joinpred);
      else
         return p;
   }
   
   /**
    * Add select predicates that apply to the union of both schemas, but not individual schemas.
    * Predicates where both terms were used in the join fields are excluded.
    */
   private Plan addJoinPred(Plan p, Schema currsch, String joinfield1, String joinfield2) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred != null) {
    	  joinpred.removeTerm(joinfield1, joinfield2);
    	  if (joinpred.size() == 0) return p;
    	  return new SelectPlan(p, joinpred);
      }
      else
         return p;
   }
}
