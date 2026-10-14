package simpledb.materialize;

import simpledb.query.*;

/**
 * The <i>sum</i> aggregation function.
 * @author Edward Sciore
 */
public class SumFn implements AggregationFn {
   private String fldname;
   private int sum;
   
   /**
    * Create a sum aggregation function for the specified field.
    * @param fldname the name of the aggregated field
    */
   public SumFn(String fldname) {
      this.fldname = fldname;
   }
   
   public String inputField() { return fldname; }

   public AggregationFn newInstance() { return new SumFn(fldname); }

   /**
    * Start a new sum.
    * Since SimpleDB does not support null values,
    * the current sum is set to the field value of the first record.
    * @see simpledb.materialize.AggregationFn#processFirst(simpledb.query.Scan)
    */
   public void processFirst(Scan s) {
	   sum = s.getInt(fldname);
   }
   
   /**
    * Since SimpleDB does not support null values,
    * this method always adds the field value to the current sum.
    * @see simpledb.materialize.AggregationFn#processNext(simpledb.query.Scan)
    */
   public void processNext(Scan s) {
	   sum += s.getInt(fldname);
   }
   
   /**
    * Return the field's name, prepended by "sumof".
    * @see simpledb.materialize.AggregationFn#fieldName()
    */
   public String fieldName() {
      return "sumof" + fldname;
   }
   
   /**
    * Return the current sum.
    * @see simpledb.materialize.AggregationFn#value()
    */
   public Constant value() { 
      return new Constant(sum);
   }
}
