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
   
   public String toString() {
      return field + (isAsc ? "" : " DESC");
   }   
}
