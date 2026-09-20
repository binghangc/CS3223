package simpledb.parse;

import java.util.*;

import simpledb.materialize.*;
import simpledb.query.*;
import simpledb.record.*;

/**
 * The SimpleDB parser.
 * 
 * @author Edward Sciore
 */
public class Parser {
   private Lexer lex;

   public Parser(String s) {
      lex = new Lexer(s);
   }

   // Methods for parsing predicates, terms, expressions, constants, and fields

   public String field() {
      return lex.eatId();
   }
   
   public Sort sortField() {
	   String field = field();
	   boolean isAsc = true;
	   if (lex.matchKeyword("asc")) {
		   lex.eatKeyword("asc");
	   } else if (lex.matchKeyword("desc")) {
		   lex.eatKeyword("desc");
		   isAsc = false;
	   }
	   return new Sort(field, isAsc);
   }

   public Constant constant() {
      if (lex.matchStringConstant())
         return new Constant(lex.eatStringConstant());
      else
         return new Constant(lex.eatIntConstant());
   }

   public Expression expression() {
      if (lex.matchId())
         return new Expression(field());
      else
         return new Expression(constant());
   }

   public Term term() {
      Expression lhs = expression();
      /**
       * Supports =, <, <=, >, >=, !=, <>
       */
      String opr = "";

      if (lex.matchOpr('=')) {
         lex.eatOpr('=');
         opr += '=';
      } else if (lex.matchOpr('<')) {
         lex.eatOpr('<');
         opr += '<';
         if (lex.matchOpr('=')) {
            lex.eatOpr('=');
            opr += '=';
         } else if (lex.matchOpr('>')) {
            lex.eatOpr('>');
            opr += '>';
         }

      } else if (lex.matchOpr('>')) {
         lex.eatOpr('>');
         opr += '>';
         if (lex.matchOpr('=')) {
            lex.eatOpr('=');
            opr += '=';
         }

      } else if (lex.matchOpr('!')) {
         lex.eatOpr('!');
         opr += '!';
         if (lex.matchOpr('=')) {
            lex.eatOpr('=');
            opr += '=';
         }
      }

      if (opr.equals("") || opr.equals("!"))
         throw new BadSyntaxException();

      Expression rhs = expression();
      return new Term(lhs, opr, rhs);
   }

   public Predicate predicate() {
      Predicate pred = new Predicate(term());
      if (lex.matchKeyword("and")) {
         lex.eatKeyword("and");
         pred.conjoinWith(predicate());
      }
      return pred;
   }

   // Methods for parsing queries

   public QueryData query() {
      lex.eatKeyword("select");
      
      List<String> fields = new ArrayList<>();
      List<AggregationFn> aggfns = new ArrayList<>();
      
      selectList(fields, aggfns);
      
      lex.eatKeyword("from");
      Collection<String> tables = tableList();
      
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      
      List<String> groupfields = new ArrayList<>();
      if (lex.matchKeyword("group")) {
    	  lex.eatKeyword("group");
    	  lex.eatKeyword("by");
    	  groupfields = fieldList();
      }
      
      List<Sort> sorts = new ArrayList<Sort>();
      if (lex.matchKeyword("order")) {
    	  lex.eatKeyword("order");
    	  lex.eatKeyword("by");
    	  sorts = sortList();  
      }
      
      return new QueryData(fields, tables, pred, sorts, groupfields, aggfns);
   }
   
   private AggregationFn aggregationFn() {
	   String fn;
	   
	   if (lex.matchKeyword("sum")) {
		   lex.eatKeyword("sum");
		   fn = "sum";
	   }
	   
	   else if (lex.matchKeyword("count")) {
		   lex.eatKeyword("count");
		   fn = "count";
	   }
	   
	   else if (lex.matchKeyword("avg")) {
		   lex.eatKeyword("avg");
		   fn = "avg";
	   }
	   
	   else if (lex.matchKeyword("min")) {
		   lex.eatKeyword("min");
		   fn = "min";
	   }
	   
	   else if (lex.matchKeyword("max")) {
		   lex.eatKeyword("max");
		   fn = "max";
	   }
	   
	   else {
		   throw new BadSyntaxException();
	   }
	   
	   lex.eatDelim('(');
	   String fldname = field();
	   lex.eatDelim(')');
	   
	   if (fn.equals("sum")) {
		   return new SumFn(fldname);
	   } 
	   
	   else if (fn.equals("count")) {
		   return new CountFn(fldname);
	   }
	   
	   else if (fn.equals("avg")) {
		   return new AvgFn(fldname);
	   }
	   
	   else if (fn.equals("min")) {
		   return new MinFn(fldname);
	   }
	   
	   else if (fn.equals("max")) {
		   return new MaxFn(fldname);
	   }
	   
	   else {
		   throw new BadSyntaxException();
	   }
   }
   
   private boolean isAggregationFn() {
	   return lex.matchKeyword("sum") || 
			  lex.matchKeyword("count") ||
			  lex.matchKeyword("avg") ||
			  lex.matchKeyword("min") ||
			  lex.matchKeyword("max");
   }
   
   private void selectList(List<String> fields, List<AggregationFn> aggfns) {
	   if (lex.matchId()) fields.add(field());
	   else if (isAggregationFn()) aggfns.add(aggregationFn());
	   else throw new BadSyntaxException();
	   
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         selectList(fields, aggfns);
      }
   }

   private Collection<String> tableList() {
      Collection<String> L = new ArrayList<String>();
      L.add(lex.eatId());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(tableList());
      }
      return L;
   }
   
   private List<Sort> sortList() {
	   List<Sort> L = new ArrayList<Sort>();
	   
	   L.add(sortField());

	   while(lex.matchDelim(',')) {
		   lex.eatDelim(',');
		   L.add(sortField());
	   }

	   return L;
   }

   // Methods for parsing the various update commands

   public Object updateCmd() {
      if (lex.matchKeyword("insert"))
         return insert();
      else if (lex.matchKeyword("delete"))
         return delete();
      else if (lex.matchKeyword("update"))
         return modify();
      else
         return create();
   }

   private Object create() {
      lex.eatKeyword("create");
      if (lex.matchKeyword("table"))
         return createTable();
      else if (lex.matchKeyword("view"))
         return createView();
      else
         return createIndex();
   }

   // Method for parsing delete commands

   public DeleteData delete() {
      lex.eatKeyword("delete");
      lex.eatKeyword("from");
      String tblname = lex.eatId();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new DeleteData(tblname, pred);
   }

   // Methods for parsing insert commands

   public InsertData insert() {
      lex.eatKeyword("insert");
      lex.eatKeyword("into");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      List<String> flds = fieldList();
      lex.eatDelim(')');
      lex.eatKeyword("values");
      lex.eatDelim('(');
      List<Constant> vals = constList();
      lex.eatDelim(')');
      return new InsertData(tblname, flds, vals);
   }

   private List<String> fieldList() {
      List<String> L = new ArrayList<String>();
      L.add(field());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(fieldList());
      }
      return L;
   }

   private List<Constant> constList() {
      List<Constant> L = new ArrayList<Constant>();
      L.add(constant());
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         L.addAll(constList());
      }
      return L;
   }

   // Method for parsing modify commands

   public ModifyData modify() {
      lex.eatKeyword("update");
      String tblname = lex.eatId();
      lex.eatKeyword("set");
      String fldname = field();
      lex.eatDelim('=');
      Expression newval = expression();
      Predicate pred = new Predicate();
      if (lex.matchKeyword("where")) {
         lex.eatKeyword("where");
         pred = predicate();
      }
      return new ModifyData(tblname, fldname, newval, pred);
   }

   // Method for parsing create table commands

   public CreateTableData createTable() {
      lex.eatKeyword("table");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      Schema sch = fieldDefs();
      lex.eatDelim(')');
      return new CreateTableData(tblname, sch);
   }

   private Schema fieldDefs() {
      Schema schema = fieldDef();
      if (lex.matchDelim(',')) {
         lex.eatDelim(',');
         Schema schema2 = fieldDefs();
         schema.addAll(schema2);
      }
      return schema;
   }

   private Schema fieldDef() {
      String fldname = field();
      return fieldType(fldname);
   }

   private Schema fieldType(String fldname) {
      Schema schema = new Schema();
      if (lex.matchKeyword("int")) {
         lex.eatKeyword("int");
         schema.addIntField(fldname);
      } else {
         lex.eatKeyword("varchar");
         lex.eatDelim('(');
         int strLen = lex.eatIntConstant();
         lex.eatDelim(')');
         schema.addStringField(fldname, strLen);
      }
      return schema;
   }

   // Method for parsing create view commands

   public CreateViewData createView() {
      lex.eatKeyword("view");
      String viewname = lex.eatId();
      lex.eatKeyword("as");
      QueryData qd = query();
      return new CreateViewData(viewname, qd);
   }

   // Method for parsing create index commands

   public CreateIndexData createIndex() {
      lex.eatKeyword("index");
      String idxname = lex.eatId();
      lex.eatKeyword("on");
      String tblname = lex.eatId();
      lex.eatDelim('(');
      String fldname = field();
      lex.eatDelim(')');
      lex.eatKeyword("using");
      if (lex.matchKeyword("hash")) {
         lex.eatKeyword("hash");
         return new CreateIndexData(idxname, tblname, fldname, "hash");
      } else if (lex.matchKeyword("btree")) {
         lex.eatKeyword("btree");
         return new CreateIndexData(idxname, tblname, fldname, "btree");
      } else {
         throw new BadSyntaxException();
      }
   }
}
