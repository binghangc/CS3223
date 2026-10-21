package simpledb.materialize;

import java.util.HashSet;
import java.util.Set;

import simpledb.query.Constant;
import simpledb.query.Scan;

/**
 * Computes {@code COUNT(DISTINCT field)} for each group.
 * Stores the group's unique field values in memory, so memory usage grows
 * with the number of distinct values in the current group. SimpleDB does
 * not support SQL NULL values, so every field value participates in the count.
 */
public class DistinctCountFn implements AggregationFn {
    private final String fldname;
    private final Set<Constant> seen = new HashSet<>();

    /**
     * Creates a distinct count for a single field.
     *
     * @param fldname the input field to count; must be a field name, not {@code *}
     */
    public DistinctCountFn(String fldname) {
        this.fldname = fldname;
    }

    /**
     * Starts a new group, discarding values from the previous group and
     * recording the first row's field value.
     *
     * @param s the input scan positioned at the first row of the group
     */
    public void processFirst(Scan s) {
        seen.clear();
        seen.add(s.getVal(fldname));
    }

    /**
     * Records another value in the current group. Repeated values leave
     * the count unchanged because the values are stored in a set.
     *
     * @param s the input scan positioned at the next row of the group
     */
    public void processNext(Scan s) {
        seen.add(s.getVal(fldname));
    }

    /**
     * @return the number of distinct values recorded for the current group
     */
    public Constant value() {
        return new Constant(seen.size());
    }

    /**
     * @return zero for a global count over an empty input
     */
    public Constant emptyValue() {
        return new Constant(0);
    }

    /**
     * @return the input field name used for schema validation
     */
    public String inputField() {
        return fldname;
    }

    /**
     * Returns the generated output field name. The distinct-count prefix
     * separates this result from an ordinary count of the same input field.
     *
     * @return the output field name
     */
    public String fieldName() {
        return "countdistinctof" + fldname;
    }

    /**
     * @return a fresh function for the same input field with an independent,
     *         empty set of values, allowing each scan to maintain its own state
     */
    public AggregationFn newInstance() {
        return new DistinctCountFn(fldname);
    }

    /**
     * @return the SQL expression, preserving DISTINCT when serializing queries
     *         such as stored view definitions
     */
    public String toSql() {
        return "count(distinct " + fldname + ")";
    }
}
