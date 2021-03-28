package ostrowski.combat.common.html;

import java.util.HashMap;
import java.util.Map.Entry;

public abstract class HtmlElement
{

   protected HashMap<String, String> attributes;
   public HtmlElement setClassName(String className) {
      setAttribute("class", className);
      return this;
   }

   public String getID() {
      return getAttributeValue("id");
   }

   public HtmlElement setID(String id) {
      setAttribute("id", id);
      return this;
   }

   public String getAttributeValue(String key) {
      if (attributes != null) {
         String value = attributes.get(key.toLowerCase());
         if (value != null) {
            return value;
         }
      }
      return "";
   }
   public HtmlElement setAttribute(String attributeName, Object attributeValue) {
      if (attributes == null) {
         attributes = new HashMap<>();
      }
      attributes.put(attributeName.toLowerCase(), attributeValue.toString());
      return this;
   }


   static int indentDepth = 0;
   static int lastWriteDepth = 0;
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
//      sb.append("\n");
//      for (int i=0 ; i<indentDepth ; i++) {
//         sb.append("\t");
//      }
      lastWriteDepth = indentDepth;
      sb.append("<").append(getElementName());
      if (attributes != null) {
         for (Entry<String, String> set : attributes.entrySet()) {
            sb.append(' ');
            sb.append(set.getKey());
            sb.append("='");
            sb.append(set.getValue());
            sb.append("'");
         }
      }
      sb.append(">");
      indentDepth++;
      sb.append(getElementContents());
      indentDepth--;
      if (lastWriteDepth != indentDepth) {
         sb.append("\n");
         sb.append("\t".repeat(Math.max(0, indentDepth)));
      }
      sb.append("</").append(getElementName()).append(">");
      // restore the last write depth
      lastWriteDepth = indentDepth;
      return sb.toString();
   }

   protected abstract String getElementContents();
   protected abstract String getElementName();
}
