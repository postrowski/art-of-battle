package ostrowski.combat.common.html;

import java.util.HashMap;
import java.util.Map.Entry;

public abstract class HtmlElement
{

   protected HashMap<String, String> _attributes;
   public HtmlElement setClassName(String className) {
      setAttribute("class", className);
      return this;
   }
   public String getClassName() {
      return getAttributeValue("class");
   }
   public String getID() {
      return getAttributeValue("id");
   }

   public HtmlElement setID(String id) {
      setAttribute("id", id);
      return this;
   }

   public String getAttributeValue(String key) {
      if (_attributes != null) {
         String value = _attributes.get(key.toLowerCase());
         if (value != null) {
            return value;
         }
      }
      return "";
   }
   public HtmlElement setAttribute(String attributeName, Object attributeValue) {
      if (_attributes == null) {
         _attributes = new HashMap<>();
      }
      _attributes.put(attributeName.toLowerCase(), attributeValue.toString());
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
      if (_attributes != null) {
         for (Entry<String, String> set : _attributes.entrySet()) {
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
         for (int i=0 ; i<indentDepth ; i++) {
            sb.append("\t");
         }
      }
      sb.append("</").append(getElementName()).append(">");
      // restore the last write depth
      lastWriteDepth = indentDepth;
      return sb.toString();
   }

   protected abstract String getElementContents();
   protected abstract String getElementName();
}
