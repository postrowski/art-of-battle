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
   String  _source;
   String  _text;
   boolean _popUp = false;
   final   List<String> _targets;
   private boolean      _isPublic;
   public MessageText() {
      _text    = null;
      _targets = null;
   }
   public MessageText(String source, String text, List<String> targetNames, boolean popUp, boolean isPublic) {
      _source  = source;
      _text    = text;
      _targets = targetNames;
      _popUp   = popUp;
      _isPublic= isPublic;
   }
   public String getText() {return _text; }
   public String getTextNoHTML() {
      String newText = _text.replaceAll("<br>", "\n");
      newText = newText.replaceAll("<br/>", "\n");
      newText = newText.replaceAll("<BR>", "\n");
      newText = newText.replaceAll("<BR/>", "\n");
      newText = newText.replaceAll("<HR>", "\n--------------------------------------------------------\n");
      newText = newText.replaceAll("<HR/>", "\n--------------------------------------------------------\n");
      newText = newText.replaceAll("<hr>", "\n--------------------------------------------------------\n");
      newText = newText.replaceAll("<b>", "");
      newText = newText.replaceAll("<B>", "");
      newText = newText.replaceAll("</b>", "");
      newText = newText.replaceAll("</B>", "");
      for (int i=1 ; i<6 ; i++) {
         newText = newText.replaceAll("<h"+i+">", "");
         newText = newText.replaceAll("<H"+i+">", "");
         newText = newText.replaceAll("</h"+i+">", "");
         newText = newText.replaceAll("</H"+i+">", "");
      }
      return newText;
   }
   public String getSource() {return _source; }
   public boolean isPopUp() { return _popUp; }
   public boolean isPublic() {return _isPublic;}

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_source, out);
         writeToStream(_text, out);
         writeToStream(_popUp, out);
         writeToStream(_isPublic, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _source = readString(in);
         _text = readString(in);
         _popUp = readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("MessageText: ");
      sb.append(", source:").append(_source);
      sb.append(", message: ").append(_text);
      sb.append(", popUp: ").append(_popUp);
      sb.append(", isPublic: ").append(_isPublic);
      if (_targets != null) {
         for (int i=0 ; i<_targets.size() ; i++) {
            sb.append("\n").append(i).append(": ").append(_targets.get(i));
         }
      }
      return sb.toString();
   }
}
