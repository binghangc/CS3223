package simpledb.opt;

// Specifies the join algorithms available to the query planner.
// DEFAULT selects the join with the lowest estimated cost.

public enum JoinStrategy{
   DEFAULT, BLOCK_NESTED_LOOPS, SORT_MERGE, INDEX_OR_HASH
}