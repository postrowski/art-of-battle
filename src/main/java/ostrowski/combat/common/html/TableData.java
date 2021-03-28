package ostrowski.combat.common.html;

public class TableData extends HtmlElement
{
   public final String  data;
   public       boolean isBold;
   public TableData(Object data) {
      this(data,false);
   }
   public TableData(Object data, boolean isBold) {
      this.data = data.toString();
      this.isBold = isBold;
   }

   public TableData setAlignLeft() {
      setAttribute("class", "alignLeft");
      return this;
   }
   public TableData setRowSpan(int span) {
      setAttribute("rowspan", span);
      return this;
   }
   public TableData setColSpan(int span) {
      setAttribute("colspan", span);
      return this;
   }
   public TableData setBold() {
      return setBold(true);
   }
   public TableData setBold(boolean isBold) {
      this.isBold = isBold;
      return this;
   }

   @Override
   protected String getElementName() {
      return "td";
   }

   @Override
   protected String getElementContents() {
      if (isBold) {
         return "<b>" + data + "</b>";
      }
      return data;
   }

}
