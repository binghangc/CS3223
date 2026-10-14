package simpledb.materialize;

import simpledb.query.*;

/**
 * The interface implemented by aggregation functions.
 * Aggregation functions are used by the <i>groupby</i> operator.
 * 
 * @author Edward Sciore
 */
public interface AggregationFn {

   /**
    * Use the current record of the specified scan
    * to be the first record in the group.
    * 
    * @param s the scan to aggregate over.
    */
   void processFirst(Scan s);

   String inputField();

   AggregationFn newInstance();

   default Constant emptyValue() {
      throw new IllegalStateException("Empty " + fieldName() + " requires SQL NULL, which SimpleDB does not support");
   }

   default String toSql() {
      String name = getClass().getSimpleName().replace("Fn", "").toLowerCase();
      return name + "(" + inputField() + ")";
   }

   /**
    * Use the current record of the specified scan
    * to be the next record in the group.
    * 
    * @param s the scan to aggregate over.
    */
   void processNext(Scan s);

   /**
    * Return the name of the new aggregation field.
    * 
    * @return the name of the new aggregation field
    */
   String fieldName();

   /**
    * Return the computed aggregation value.
    * 
    * @return the computed aggregation value
    */
   Constant value();
}
