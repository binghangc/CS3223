package simpledb.opt;

// Restricts the types of join that we consider in the HeurisitcQueryPlanner
// A DEFAULT join is essentially the join best chosen by the Query Planner

public enum JoinStrategy{
   DEFAULT, BLOCK_NESTED_LOOPS, SORT_MERGE, INDEX_OR_HASH
}