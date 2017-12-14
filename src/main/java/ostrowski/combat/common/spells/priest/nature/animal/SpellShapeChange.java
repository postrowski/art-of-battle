/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.nature.animal;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Race;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.common.things.Armor;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;

public abstract class SpellShapeChange extends ExpiringPriestSpell
{
   private Race previousRace = null;
   private Armor previousArmor = null;

   public SpellShapeChange(String name, Class<? extends IPriestGroup> group, int affinity) {
      super(name, (short)50 /*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTargetName() + " changes into the body of a " + getShapeName() + ".";
      }
      return " body of a " + getShapeName() + ".";
   }

   private String getShapeName() {
      return getShapeName(getEffectivePower());
   }
   abstract public String getShapeName(byte effectivePower);

   @Override
   public String describeSpell() {
      StringBuilder sb = new StringBuilder();
      sb.append("The '").append(getName()).append("' changes the shape of the caster into a creature.");
      sb.append(" The more power put into the spell, the more fearsome the creature:");
      Table table = new Table();
      table.addRow(new TableRow(-1, "Effective power", "Creature"));
      for (byte power=1 ; power<=8 ; power++) {
         table.addRow(new TableRow(power-1, "" + power, getShapeName(power)));
      }
      sb.append(table.toString());
      return sb.toString();
   }
   @Override
   public Boolean isCastInBattle() {
      return true;
   }


   @Override
   public void applyEffects(Arena arena) {
      previousRace = getTarget().getRace();
      previousArmor = getTarget().getArmor();
      getTarget().setArmor(Armor.NAME_NoArmor);
      getTarget().dropAllEquipment(arena);
      Race newRace = Race.getRace(getShapeName(), previousRace.getGender());
      applyEffects(arena, previousRace, newRace);
   }
   @Override
   public void removeEffects(Arena arena) {
      applyEffects(arena, getTarget().getRace(), previousRace);
      getTarget().setArmor(previousArmor.getName());
   }

   private void applyEffects(Arena arena, Race prevRace, Race newRace) {
      Character target = getTarget();
      short magePoints = target.getCondition().getMageSpellPointsAvailable();
      short priestPoints = target.getCondition().getPriestSpellPointsAvailable();
      ArenaLocation headLoc = arena.getLocation(target.getOrientation().getHeadCoordinates());
      Facing facing = target.getOrientation().getFacing();
      arena.getCombatMap().removeCharacter(target);
      byte IQ = target.getAttributeLevel(Attribute.Intelligence);
      target.setRace(newRace.getName(), newRace.getGender());
      target.setAttribute(Attribute.Intelligence, IQ, false/*containInLimits*/);
      target.getOrientation().setHeadLocation(target, headLoc, facing, arena.getCombatMap(), null/*diag*/, true/*allowTwisting*/);
      arena.getCombatMap().addCharacter(target, headLoc, null/*clientProxy*/);

      // setting the race resets the spell points, so we must preserve them manually:
      target.getCondition().setMageSpellPointsAvailable(magePoints);
      target.getCondition().setPriestSpellPointsAvailable(priestPoints);
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_SELF;
   }
}
