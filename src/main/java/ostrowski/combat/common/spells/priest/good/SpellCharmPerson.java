package ostrowski.combat.common.spells.priest.good;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.spells.priest.ResistedPriestSpell;
import ostrowski.combat.server.Arena;

public class SpellCharmPerson extends ResistedPriestSpell
{
   public static final String NAME = "Charm Person";
   public SpellCharmPerson() {
   }
   public SpellCharmPerson(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, Attribute.Intelligence, (byte)3/*resistedActions*/, true/*expires*/, group, affinity);
   }
   public SpellCharmPerson(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, Attribute.Intelligence, (byte)3/*resistedActions*/, true/*expires*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " becomes an ally to " + getCasterName() + ", and is now on team " + TEAM_NAMES[_caster._teamID] + ".";
      }
      return "(currently on team " + TEAM_NAMES[_caster._teamID] + ")";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the subject of the spell to immediately regard the caster as a close friend." +
               " The subject does not forget who its previous allies are, but will do his or her best to prevent anyone from attacking the caster." +
               " This usually means stepping between his or her real allies and the caster," +
               " but the subject will fight anyone to defend the caster of the spell, if talking fails." +
               " The subject has no allegiance to the caster's allies, but the caster may be able to convince the subject that they are on the same side." +
               " If the caster behaves in any way contrary to how a close friend would behave to him," +
               " the subject gets another resistance roll to break out of the spell." +
               " If the caster actually harms the subject intentionally, the spell is immediately broken.";
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   private byte _previousTeamID;
   @Override
   public void applyEffects(Arena arena) {
      _previousTeamID = _target._teamID;
      _target._teamID = _caster._teamID;
      _target._targetID = -1;
      arena.recomputeAllTargets(_target);
   }

   @Override
   public void removeEffects(Arena arena) {
      _target._teamID = _previousTeamID;
      arena.sendMessageTextToAllClients(_target.getName() + " is back on team " + TEAM_NAMES[_previousTeamID], false/*popUp*/);
      arena.recomputeAllTargets(_target);
   }

   @Override
   public Boolean isCastInBattle() {
      return true;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_previousTeamID, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _previousTeamID = readByte(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   protected void copyDataFrom(Spell source)
   {
      super.copyDataFrom(source);
      if (source instanceof SpellCharmPerson) {
         _previousTeamID = ((SpellCharmPerson) source)._previousTeamID;
      }
   }

   @Override
   public Element getXMLObject(Document parentDoc, String newLine) {
      Element node = super.getXMLObject(parentDoc, newLine);
      node.setAttribute("previousTeamID", String.valueOf(_previousTeamID));
      return node;
   }
   @Override
   public void readFromXMLObject(NamedNodeMap namedNodeMap) {
      super.readFromXMLObject(namedNodeMap);
      Node node = namedNodeMap.getNamedItem("previousTeamID");
      if (node != null) {
         _previousTeamID  = Byte.parseByte(node.getNodeValue());
      }
   }

}
