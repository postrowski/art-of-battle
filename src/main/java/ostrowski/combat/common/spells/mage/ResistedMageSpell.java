/*
 * Created on May 18, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.spells.IResistedSpell;
import ostrowski.combat.server.Configuration;

public abstract class ResistedMageSpell extends ExpiringMageSpell implements IResistedSpell
{
   protected Attribute resistedAtt;
   protected byte      resistedActions;

   @SuppressWarnings("rawtypes")
   protected ResistedMageSpell(String name, Attribute resistedAtt, byte resistedActions, boolean expires,
                           Class[] prerequisiteSpells, MageCollege[] colleges) {
      super(name, (short)(expires ? 10 : 0)/*baseExpirationTimeInTurns*/,
                  (short)(expires ? 2 : 0)/*bonusTimeInTurnsPerPower*/,
                  prerequisiteSpells, colleges);
      this.resistedAtt = resistedAtt;
      this.resistedActions = resistedActions;
   }

   @Override
   public short getMaxRange(Character caster) {
      return 100;
   }
   /**
    * This method is called to determine if the defender can dodge, block, or parry this spell.
    * Resisted spells are Resistable, but not defendable.
    */
   @Override
   public boolean isDefendable() {
      return false;
   }

   /**
    * @param target
    * @return the targets level for the attribute resisted by this spell
    */
   @Override
   public byte getResistanceAttribute(Character target) {
      return target.getAttributeLevel(resistedAtt);
   }
   @Override
   public byte getResistanceActions() {
      return resistedActions;
   }
   @Override
   public DiceSet getResistanceDice(Character target) {
      byte resistanceAttr = getResistanceAttribute(target);
      if (Configuration.useExtendedDice()) {
         return Rules.getDice(resistanceAttr, getResistanceActions(), resistedAtt, RollType.MAGIC_RESISTANCE);
      }

      DiceSet dice = Rules.getDice(resistanceAttr, (byte)2, resistedAtt, RollType.MAGIC_RESISTANCE);
      switch (getResistanceActions()) {
         case 1: dice.addBonus(-5); break;
         case 2: break;
         case 3: dice.addBonus(5); break;
            default:
      }
      return dice;
   }

   @Override
   protected void copyDataFrom(MageSpell source) {
      super.copyDataFrom(source);

      if (source instanceof ResistedMageSpell) {
         ResistedMageSpell resistedSource = (ResistedMageSpell) source;
         resistedAtt = resistedSource.resistedAtt;
         resistedActions = resistedSource.resistedActions;
      }
   }

   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_OTHER_FIGHTING;
   }

   @Override
   public String getResistanceAttributeName(){
      return resistedAtt.shortName;
   }

}
