/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.priest.defensive;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.ExpiringPriestSpell;
import ostrowski.combat.common.spells.priest.IPriestGroup;
import ostrowski.combat.server.Arena;

/**
 * This spell increases the target Health attribute by an amount equal to twice the effective power of the spell
 */
public class SpellImprovedArmor extends ExpiringPriestSpell implements ICastInBattle
{
   public static final String NAME = "Improved Armor";
   public SpellImprovedArmor() {
      this(null, 0);
   }
   public SpellImprovedArmor(Class<? extends IPriestGroup> group, int affinity) {
      super(NAME, (short)10/*baseExpirationTimeInTurns*/, (short)5/*bonusTimeInTurnsPerPower*/, group, affinity);
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      if (firstTime) {
         return getTarget().getName() + "'s skin becomes like armor (BLD increased by " + getHealthModifier() + "), and PD increases by " + getPower() + ".";
      }
      return "(HT incresed by " + getHealthModifier() + ", PD increased by " + getPower() + ").";
   }

   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell increases the subject's Build characteristic by an amount equal to 2 times the effective power of the spell,"+
             " and PD increase by 1 point for each point of effective power in the spell." +
             " This spell may be cast on yourself or others. Multiple castings on the same subject do not accumulate," +
             " and will not accumulate with an ‘Armor’ spell either." +
             " The highest benefiting spell is the only one that has an effect when multiple armor-type spells are cast on a single subject.";
   }

   @Override
   public void applyEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) + getHealthModifier()), false/*containInLimits*/);
   }
   @Override
   public void removeEffects(Arena arena) {
      getTarget().setAttribute(Attribute.Health, (byte) (getTarget().getAttributeLevel(Attribute.Health) - getHealthModifier()), false/*containInLimits*/);
   }

   @Override
   public byte getPassiveDefenseModifier(boolean vsRangedWeapons, DamageType vsDamageType)
   {
      if (!isExpired()) {
         return getPower();
      }
      return 0;
   }

   public byte getHealthModifier() {
      return (byte) (getPower()*3);
   }
   @Override
   public boolean isBeneficial() {
      return true;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_ANYONE;
   }


   @Override
   public boolean isIncompatibleWith(Spell spell) {
      if (spell.getClass() == SpellArmor.class) {
         return true;
      }
      return super.isIncompatibleWith(spell);
   }

   @Override
   public boolean takesPrecedenceOver(Spell spell) {
      if (spell.getClass() == SpellArmor.class) {
         return true;
      }
      return super.takesPrecedenceOver(spell);
   }
}
