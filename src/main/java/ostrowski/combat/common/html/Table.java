package ostrowski.combat.common.html;

import java.util.ArrayList;
import java.util.List;

public class Table extends HtmlElement
{
   public final List<TableRow> data = new ArrayList<>();
   public Table() {
   }

   public Table addRow(TableRow tr) {
      data.add(tr);
      return this;
   }

   @Override
   protected String getElementName() { return "table";}

   @Override
   protected String getElementContents() {
      StringBuilder sb = new StringBuilder();
      for (TableRow tr : data) {
         sb.append(tr);
      }
      return sb.toString();
   }
}
