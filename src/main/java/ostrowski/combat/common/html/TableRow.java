package ostrowski.combat.common.html;

import java.util.ArrayList;
import java.util.List;

public class TableRow extends HtmlElement
{
   int rowIndex = 0;
   public final List<TableData> data = new ArrayList<>();

   public TableRow() {
      this(0);
   }
   public TableRow(int rowIndex) {
      setRowIndex(rowIndex);
   }
   public TableRow(int rowIndex, Object th) {
      this(rowIndex);
      addHeader(th);
   }
   public TableRow(int rowIndex, Object th, Object td1) {
      this(rowIndex, th);
      addTD(td1);
   }
   public TableRow(int rowIndex, Object th, Object td1, Object td2) {
      this(rowIndex, th, td1);
      addTD(td2);
   }
   public TableRow(int rowIndex, Object th, Object td1, Object td2, Object td3) {
      this(rowIndex, th, td1, td2);
      addTD(td3);
   }
   public TableRow(int rowIndex, Object th, Object td1, Object td2, Object td3, Object td4) {
      this(rowIndex, th, td1, td2, td3);
      addTD(td4);
   }
   public TableRow(int rowIndex, Object th, Object td1, Object td2, Object td3, Object td4, Object td5) {
      this(rowIndex, th, td1, td2, td3, td4);
      addTD(td5);
   }

   public void setRowIndex(int rowIndex) {
      this.rowIndex = rowIndex;
      if (rowIndex == -1) {
         setClassName("header-row");
      }
      else {
         setClassName("row" + (rowIndex % HtmlBuilder.MAX_HTML_ROWS));
      }
   }

   public TableRow addTD(Object td) {
      if (td instanceof TableData) {
         data.add((TableData)(td));
      }
      else {
         if (rowIndex == -1) {
            data.add(new TableHeader(td.toString()));
         }
         else {
            data.add(new TableData(td.toString()));
         }
      }
      return this;
   }

   public TableRow addHeader(Object td) {
      if (td instanceof TableHeader) {
         data.add((TableHeader)(td));
      }
      else {
         data.add(new TableHeader(td.toString()));
      }
      return this;
   }

   @Override
   protected String getElementName() { return "tr";}

   @Override
   protected String getElementContents() {
      StringBuilder sb = new StringBuilder();
      for (TableData td : data) {
         sb.append(td);
      }
      return sb.toString();
   }
}
