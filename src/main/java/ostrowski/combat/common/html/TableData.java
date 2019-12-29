package ostrowski.combat.common.html;

public class TableData extends HtmlElement
{
   public final String  _data;
   public       boolean _isBold;
   public TableData(Object data) {
      this(data,false);
   }
   public TableData(Object data, boolean isBold) {
      _data = data.toString();
      _isBold = isBold;
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
      _isBold = isBold;
      return this;
   }

   @Override
   protected String getElementName() {
      return "td";
   }

   @Override
   protected String getElementContents() {
      if (_isBold) {
         return "<b>" + _data + "</b>";
      }
      return _data;
   }

}
