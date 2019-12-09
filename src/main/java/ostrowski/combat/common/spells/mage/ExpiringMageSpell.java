/*
 * Created on Jun 8, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.combat.common.spells.IExpiringSpell;
import ostrowski.combat.server.Arena;

public abstract class ExpiringMageSpell extends MageSpell implements IExpiringSpell
{
   protected short     _duration                  = -1;
   protected short     _baseExpirationTimeInTurns = 0;
   protected short     _bonusTimeInTurnsPerPower  = 0;
   public ExpiringMageSpell(){
      this("", (short)0, (short)0, null, null);
   }
   @SuppressWarnings("rawtypes")
   public ExpiringMageSpell(String name, short baseExpirationTimeInTurns, short bonusTimeInTurnsPerPower,
                        Class[] prerequisiteSpells, MageCollege[] colleges) {
      super(name, prerequisiteSpells, colleges);
      _baseExpirationTimeInTurns = baseExpirationTimeInTurns;
      _bonusTimeInTurnsPerPower = bonusTimeInTurnsPerPower;
   }

   /**
    * Spells that have a time limit, return this limit (in Turns). Spells that have no time limit
    * (or are instantaneous) return -1.
    * @return number of Turns remaining for spell effect
    */
   @Override
   public short getDuration() {
      return _duration;
   }
   @Override
   public boolean isExpired() {
      return _duration <= 0;
   }
   @Override
   public short getBaseExpirationTimeInTurns() {
      return _baseExpirationTimeInTurns;
   }
   @Override
   public short getBonusTimeInTurnsPerPower() {
      return _bonusTimeInTurnsPerPower;
   }
   @Override
   public String describeActiveSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append(getTargetName()).append(" is under the effects of a ");
      sb.append(getPower()).append("-point power ");
      sb.append(getName()).append(" spell, which has");
      sb.append(_duration).append(" turns left.");
      return sb.toString();
   }

   /**
    * completeTurn returns 'true' if this spell expired this turn.
    * @param arena
    * @return
    */
   @Override
   public boolean completeTurn(Arena arena)
   {
      if (_duration > -1) {
         _duration--;
         if (_duration > 0) {
            return false;
         }
         removeEffects(arena);
      }
      return true;
   }
   @Override
   public void completeSpell() {
      super.completeSpell();
      _duration = (short) (_baseExpirationTimeInTurns + (getPower()*_bonusTimeInTurnsPerPower));
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_duration, out);
         writeToStream(_baseExpirationTimeInTurns, out);
         writeToStream(_bonusTimeInTurnsPerPower, out);

      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _duration                  = readShort(in);
         _baseExpirationTimeInTurns = readShort(in);
         _bonusTimeInTurnsPerPower  = readShort(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   protected void copyDataFrom(MageSpell source)
   {
      super.copyDataFrom(source);
      if (source instanceof ExpiringMageSpell) {
         _duration                  = ((ExpiringMageSpell) source)._duration;
         _baseExpirationTimeInTurns = ((ExpiringMageSpell) source)._baseExpirationTimeInTurns;
         _bonusTimeInTurnsPerPower  = ((ExpiringMageSpell) source)._bonusTimeInTurnsPerPower;
      }
   }
   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      node.setAttribute("duration", String.valueOf(_duration));
      node.setAttribute("baseExpirationTimeInTurns", String.valueOf(_baseExpirationTimeInTurns));
      node.setAttribute("bonusTimeInTurnsPerPower", String.valueOf(_bonusTimeInTurnsPerPower));
      return node;
   }
   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("duration");             if (node != null) {
         _duration  = Short.parseShort(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("baseExpirationTimeInTurns"); if (node != null) {
         _baseExpirationTimeInTurns  = Short.parseShort(node.getNodeValue());
      }
      node = namedNodeMap.getNamedItem("bonusTimeInTurnsPerPower");  if (node != null) {
         _bonusTimeInTurnsPerPower  = Short.parseShort(node.getNodeValue());
      }
   }
}
