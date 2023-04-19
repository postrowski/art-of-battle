package ostrowski.combat.common.html;

public class TableHeader extends TableData
{
   public TableHeader(Object data) {
      super(data);
   }

   @Override
   protected String getElementName() {
      return "th";
   }

}
