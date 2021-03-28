/*
 * Created on May 11, 2006
 *
 */
package ostrowski.combat.protocol;

import ostrowski.combat.common.enums.Enums;
import ostrowski.protocol.SerializableObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class MessageText extends SerializableObject implements Enums
{
   String  source;
   String  text;
   boolean popUp = false;
   final   List<String> targets;
   private boolean      isPublic;
   public MessageText() {
      text = null;
      targets = null;
   }
   public MessageText(String source, String text, List<String> targetNames, boolean popUp, boolean isPublic) {
      this.source = source;
      this.text = text;
      targets = targetNames;
      this.popUp = popUp;
      this.isPublic = isPublic;
   }
   public String getText() {return text; }
   public static String getTextNoHTML(String text) {
      String newText = text.replaceAll("<br>", "\n")
                           .replaceAll("<br/>", "\n")
                           .replaceAll("<BR>", "\n")
                           .replaceAll("<BR/>", "\n")
                           .replaceAll("<HR>", "\n--------------------------------------------------------\n")
                           .replaceAll("<HR/>", "\n--------------------------------------------------------\n")
                           .replaceAll("<hr>", "\n--------------------------------------------------------\n")
                           .replaceAll("<b>", "")
                           .replaceAll("<B>", "")
                           .replaceAll("</b>", "")
                           .replaceAll("</B>", "");
      for (int i=1 ; i<6 ; i++) {
         newText = newText.replaceAll("<h"+i+">", "")
                          .replaceAll("<H"+i+">", "")
                          .replaceAll("</h"+i+">", "")
                          .replaceAll("</H"+i+">", "");
      }
      return newText;
   }
   public String getSource() {return source; }
   public boolean isPopUp() { return popUp; }
   public boolean isPublic() {return isPublic;}

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(source, out);
         writeToStream(text, out);
         writeToStream(popUp, out);
         writeToStream(isPublic, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         source = readString(in);
         text = readString(in);
         popUp = readBoolean(in);
         isPublic = readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MessageText: ");
      sb.append(", source:").append(source);
      sb.append(", message: ").append(text);
      sb.append(", popUp: ").append(popUp);
      sb.append(", isPublic: ").append(isPublic);
      if (targets != null) {
         for (int i = 0; i < targets.size() ; i++) {
            sb.append("\n").append(i).append(": ").append(targets.get(i));
         }
      }
      return sb.toString();
   }

}
