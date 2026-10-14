package simpledb.plan;

import simpledb.materialize.GroupByPlan;
import simpledb.materialize.SortPlan;
import simpledb.parse.QueryData;
import simpledb.tx.Transaction;

public final class QueryPlan {
   private QueryPlan() {
   }

   public static Plan finish(Plan p, QueryData data, Transaction tx) {
      // Step 4: If grouping or aggregation is specified, add a group by plan.
      if (!data.groupFields().isEmpty() || !data.aggregationFns().isEmpty())
         p = new GroupByPlan(tx, p, data.groupFields(), data.aggregationFns());
      for (String field : data.fields())
         if (!p.schema().hasField(field))
            throw new IllegalArgumentException("Field is unknown or is not grouped: " + field);
      for (simpledb.query.Sort sort : data.sorts())
         if (!p.schema().hasField(sort.getField()))
            throw new IllegalArgumentException("Unknown ORDER BY field: " + sort.getField());
      // Step 5: If ordering is specified, add a sort plan. Otherwise leave it as normal.
      if (!data.sorts().isEmpty())
         p = new SortPlan(tx, p, data.sorts());

      // Step 6: Project on the selected field names
      return new ProjectPlan(p, data.fields());
   }
}
