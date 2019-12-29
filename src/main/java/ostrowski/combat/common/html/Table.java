package ostrowski.combat.common.html;

import java.util.ArrayList;

public class Table extends HtmlElement
{
   public ArrayList<TableRow> _data = new ArrayList<>();
   public Table() {
   }

   public Table addRow(TableRow tr) {
      _data.add(tr);
      return this;
   }

   @Override
   protected String getElementName() { return "table";}

   @Override
   protected String getElementContents() {
      StringBuilder sb = new StringBuilder();
      for (TableRow tr : _data) {
         sb.append(tr);
      }
      return sb.toString();
   }
}
