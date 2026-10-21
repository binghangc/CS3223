package simpledb.plan;

import java.util.Collections;
import simpledb.materialize.GroupByPlan;
import simpledb.materialize.SortPlan;
import simpledb.parse.QueryData;
import simpledb.tx.Transaction;

public final class QueryPlan {
   private QueryPlan() {
   }

   public static Plan finish(Plan p, QueryData data, Transaction tx) {
      // If grouping or aggregation is specified, add a group by plan.
      if (!data.groupFields().isEmpty() || !data.aggregationFns().isEmpty())
         p = new GroupByPlan(tx, p, data.groupFields(), data.aggregationFns());
      for (String field : data.fields())
         if (!p.schema().hasField(field))
            throw new IllegalArgumentException("Field is unknown or is not grouped: " + field);
      // validate ordering fields
      for (simpledb.query.Sort sort : data.sorts()) {
         String field = sort.getField();

         if (!p.schema().hasField(sort.getField()))
            throw new IllegalArgumentException("Unknown ORDER BY field: " + sort.getField());

         if (data.isDistinct() && !data.fields().contains(field)) {
            throw new IllegalArgumentException(
                  "ORDER BY field must appear in SELECT DISTINCT: "
                        + field);
         }
      }

      if (data.isDistinct()) {
         p = new GroupByPlan(tx, p, data.fields(), Collections.emptyList());
      }

      // If ordering is specified, add a sort plan. Otherwise leave it as normal.
      if (!data.sorts().isEmpty())
         p = new SortPlan(tx, p, data.sorts());

      // Project on the selected field names
      return new ProjectPlan(p, data.fields());
   }
}
