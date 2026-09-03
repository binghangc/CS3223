package simpledb.query;

/**
 * The class that stores a sort field and its order.
 */
public class Sort {
   private String field = null;
   private boolean isAsc = true;
   
   public Sort(String field, boolean isAsc) {
      this.field = field;
      this.isAsc = isAsc;
   }
   
   public String getField() {
	   return this.field;
   }
   
   public boolean getAsc() {
	   return this.isAsc;
   }
   
//   public boolean equals(Object obj) {
//      Constant c = (Constant) obj;
//      return (ival != null) ? ival.equals(c.ival) : sval.equals(c.sval);
//   }
//   
//   public int compareTo(Constant c) {
//      return (ival != null) ? ival.compareTo(c.ival) : sval.compareTo(c.sval);
//   }
//   
//   public int hashCode() {
//      return (ival != null) ? ival.hashCode() : sval.hashCode();
//   }
//   
   public String toString() {
      return field + (isAsc ? "" : " DESC");
   }   
}
