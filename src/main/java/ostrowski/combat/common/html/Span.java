package ostrowski.combat.common.html;

import java.util.ArrayList;
import java.util.List;

public class Span extends HtmlElement
{
   private String contents;

   public Span(String contents) {
      this.contents = contents;
   }

   @Override
   protected String getElementName() { return "span";}

   @Override
   protected String getElementContents() {
      return contents;
   }
}
