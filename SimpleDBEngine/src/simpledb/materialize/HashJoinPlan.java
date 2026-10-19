package simpledb.materialize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.record.Layout;
import simpledb.record.RID;
import simpledb.record.Schema;
import simpledb.record.TableScan;
import simpledb.tx.Transaction;

/** A GRACE hash equijoin, using the RHS as the build input. */
public class HashJoinPlan implements Plan {
   private final Transaction tx;
   private final Plan p1, p2;
   private final String fldname1, fldname2;
   private final Schema sch = new Schema();

   /**
    * Creates a join of the two plans on the specified fields.
    * Both inputs are partitioned using the same hash function. Oversized
    * partition pairs are repartitioned before building and probing a hash table.
    */
   public HashJoinPlan(Transaction tx, Plan p1, Plan p2,
                       String fldname1, String fldname2) {
      this.tx = tx;
      this.p1 = p1;
      this.p2 = p2;
      this.fldname1 = fldname1;
      this.fldname2 = fldname2;
      sch.addAll(p1.schema());
      sch.addAll(p2.schema());
   }

   public Scan open() {
      if (tx.availableBuffs() < 3)
         throw new IllegalStateException("GRACE hash join requires at least 3 available buffers");
      int buckets = tx.availableBuffs() - 1;
      int capacity = buildCapacity();
      List<TempTable> left = new ArrayList<>();
      List<TempTable> right = new ArrayList<>();
      Partition[] lhs = partition(p1.open(), p1.schema(), fldname1, buckets, 0);
      Partition[] rhs = partition(p2.open(), p2.schema(), fldname2, buckets, 0);
      for (int i = 0; i < buckets; i++)
         prepare(lhs[i], rhs[i], buckets, capacity, 1, left, right);
      return new HashJoinScan(left, right, p1.schema(), p2.schema(),
                              fldname1, fldname2, capacity);
   }

   private void prepare(Partition lhs, Partition rhs, int buckets, int capacity,
                        int depth, List<TempTable> left, List<TempTable> right) {
      if (lhs.records == 0 || rhs.records == 0)
         return;
      if (rhs.records <= capacity || depth >= 32) {
         left.add(lhs.table);
         right.add(rhs.table);
         return;
      }
      Partition[] lp = partition(lhs.table.open(), p1.schema(), fldname1, buckets, depth);
      Partition[] rp = partition(rhs.table.open(), p2.schema(), fldname2, buckets, depth);
      for (int i = 0; i < buckets; i++) {
         // Identical keys or hash codes cannot be separated by rehashing.
         // Join partitions that do not shrink in bounded RHS chunks instead.
         if (rp[i].records == rhs.records) {
            if (lp[i].records > 0) {
               left.add(lp[i].table);
               right.add(rp[i].table);
            }
         }
         else
            prepare(lp[i], rp[i], buckets, capacity, depth + 1, left, right);
      }
   }

   /** Partitions a source, taking ownership of it and closing every writer. */
   private Partition[] partition(Scan src, Schema schema, String field,
                                 int buckets, int depth) {
      Partition[] result = new Partition[buckets];
      Map<Integer, TableScan> writers = new LinkedHashMap<>(16, 0.75f, true);
      try {
         for (int i = 0; i < buckets; i++)
            result[i] = new Partition(new TempTable(tx, schema));
         // Complex input plans may already own several buffers. Remember the
         // tail of each evicted writer so reopening it does not rescan its table.
         int maxWriters = tx.availableBuffs();
         if (maxWriters < 1)
            throw new IllegalStateException("No buffer available for hash partitions");
         src.beforeFirst();
         while (src.next()) {
            int bucket = bucket(src.getVal(field).hashCode(), buckets, depth);
            Partition part = result[bucket];
            TableScan dest = writers.get(bucket);
            if (dest == null) {
               if (!writers.isEmpty()
                     && (writers.size() >= maxWriters || tx.availableBuffs() == 0)) {
                  Integer oldest = writers.keySet().iterator().next();
                  writers.remove(oldest).close();
               }
               dest = (TableScan) part.table.open();
               writers.put(bucket, dest);
               if (part.tail != null)
                  dest.moveToRid(part.tail);
            }
            dest.insert();
            for (String name : schema.fields())
               dest.setVal(name, src.getVal(name));
            part.tail = dest.getRid();
            part.records++;
         }
         return result;
      }
      finally {
         for (TableScan writer : writers.values())
            writer.close();
         src.close();
      }
   }

   private static int bucket(int hash, int buckets, int depth) {
      // Use a different seed on each pass; floorMod also handles MIN_VALUE.
      hash ^= 0x9e3779b9 * (depth + 1);
      hash ^= hash >>> 16;
      hash *= 0x85ebca6b;
      hash ^= hash >>> 13;
      hash *= 0xc2b2ae35;
      hash ^= hash >>> 16;
      return Math.floorMod(hash, buckets);
   }

   private int buildCapacity() {
      int recordsPerBlock = tx.blockSize() / new Layout(p2.schema()).slotSize();
      // Bound record payload by B-2 pages; Java collection overhead is extra.
      long capacity = (long) Math.max(1, tx.availableBuffs() - 2) * recordsPerBlock;
      return (int) Math.min(Integer.MAX_VALUE, Math.max(1, capacity));
   }

   /**
    * Estimates input reads plus partition writes and reads, assuming uniform
    * hashing. One pass over base tables costs about 3 * (B1 + B2).
    * Additional passes add two accesses per materialized block.
    */
   public int blocksAccessed() {
      int buckets = Math.max(2, tx.availableBuffs() - 1);
      long records = p2.recordsOutput();
      int passes = 1;
      long capacity = (long) buildCapacity() * buckets;
      while (records > capacity) {
         records = (records + buckets - 1) / buckets;
         passes++;
      }
      long blocks = (long) new MaterializePlan(tx, p1).blocksAccessed()
                  + new MaterializePlan(tx, p2).blocksAccessed();
      long cost = (long) p1.blocksAccessed() + p2.blocksAccessed() + 2L * passes * blocks;
      return (int) Math.min(Integer.MAX_VALUE, cost);
   }

   public int recordsOutput() {
      int maxvals = Math.max(p1.distinctValues(fldname1), p2.distinctValues(fldname2));
      if (maxvals == 0)
         return 0;
      long records = (long) p1.recordsOutput() * p2.recordsOutput() / maxvals;
      return (int) Math.min(Integer.MAX_VALUE, records);
   }

   public int distinctValues(String fldname) {
      return p1.schema().hasField(fldname)
            ? p1.distinctValues(fldname) : p2.distinctValues(fldname);
   }

   public Schema schema() {
      return sch;
   }

   private static class Partition {
      final TempTable table;
      long records;
      RID tail;

      Partition(TempTable table) {
         this.table = table;
      }
   }
}
