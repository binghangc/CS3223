package simpledb.materialize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import simpledb.query.Constant;
import simpledb.query.Scan;
import simpledb.record.Schema;

/** Builds a hash table on each RHS partition and probes it with the LHS. */
public class HashJoinScan implements Scan {
   private final List<TempTable> left, right;
   private final Schema leftSchema, rightSchema;
   private final String fldname1, fldname2;
   private final int capacity;
   private final Map<Constant, List<Map<String, Constant>>> table = new HashMap<>();
   private Iterator<Map<String, Constant>> matches = Collections.emptyIterator();
   private Map<String, Constant> current;
   private Scan lhs, rhs;
   private int partition = -1;
   private boolean closed;
   private boolean rhsExhausted;

   /**
    * Takes paired partitions prepared by HashJoinPlan. At most capacity RHS
    * records are held at once. Skewed partitions use multiple build/probe passes.
    */
   public HashJoinScan(List<TempTable> left, List<TempTable> right,
                       Schema leftSchema, Schema rightSchema,
                       String fldname1, String fldname2, int capacity) {
      this.left = left;
      this.right = right;
      this.leftSchema = leftSchema;
      this.rightSchema = rightSchema;
      this.fldname1 = fldname1;
      this.fldname2 = fldname2;
      this.capacity = capacity;
   }

   public void beforeFirst() {
      if (closed)
         throw new IllegalStateException("Hash join scan is closed");
      closePartition();
      partition = -1;
      table.clear();
      matches = Collections.emptyIterator();
      current = null;
   }

   public boolean next() {
      if (closed)
         return false;
      current = null;
      try {
         while (true) {
            if (matches.hasNext()) {
               current = matches.next();
               return true;
            }
            if (lhs != null) {
               while (lhs.next()) {
                  List<Map<String, Constant>> rows = table.get(lhs.getVal(fldname1));
                  if (rows != null) {
                     matches = rows.iterator();
                     current = matches.next();
                     return true;
                  }
               }
            }
            if (!buildNextChunk())
               return false;
         }
      }
      catch (RuntimeException e) {
         close();
         throw e;
      }
   }

   private boolean buildNextChunk() {
      table.clear();
      matches = Collections.emptyIterator();
      while (true) {
         if (rhsExhausted)
            closePartition();
         if (rhs == null) {
            if (++partition >= right.size())
               return false;
            rhs = right.get(partition).open();
            lhs = left.get(partition).open();
            rhsExhausted = false;
         }
         int count = 0;
         while (count < capacity) {
            if (!rhs.next()) {
               rhsExhausted = true;
               break;
            }
            Map<String, Constant> row = new HashMap<>();
            for (String field : rightSchema.fields())
               row.put(field, rhs.getVal(field));
            Constant key = row.get(fldname2);
            table.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            count++;
         }
         if (count > 0) {
            lhs.beforeFirst();
            return true;
         }
         closePartition();
      }
   }

   public int getInt(String fldname) {
      return getVal(fldname).asInt();
   }

   public String getString(String fldname) {
      return getVal(fldname).asString();
   }

   public Constant getVal(String fldname) {
      if (current == null)
         throw new IllegalStateException("No current hash join record");
      if (leftSchema.hasField(fldname))
         return lhs.getVal(fldname);
      if (rightSchema.hasField(fldname))
         return current.get(fldname);
      throw new IllegalArgumentException("Unknown field: " + fldname);
   }

   public boolean hasField(String fldname) {
      return leftSchema.hasField(fldname) || rightSchema.hasField(fldname);
   }

   public void close() {
      closePartition();
      table.clear();
      matches = Collections.emptyIterator();
      current = null;
      closed = true;
   }

   private void closePartition() {
      if (lhs != null) {
         lhs.close();
         lhs = null;
      }
      if (rhs != null) {
         rhs.close();
         rhs = null;
      }
   }
}
