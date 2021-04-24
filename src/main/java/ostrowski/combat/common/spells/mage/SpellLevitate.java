/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.Spell;

public class SpellLevitate extends ExpiringMageSpell implements ICastInBattle
{
   public static final String NAME = "Levitate";
   public SpellLevitate() {
      super(NAME, (short)10, (short)10, new Class[] {SpellCreateForce.class},
            new SkillType[] {SkillType.Spellcasting_Air, SkillType.Spellcasting_Energy});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell causes the subject of the spell to float in the air."+
              " While levitating, the subject may move 1 yard per action spent." +
              " The weight limit of the spell is determined by the power put into the spell.";
   }

   @Override
   public byte getModifiedMovementPerRound(byte previousMovementRate) {
      if (!isExpired() && (previousMovementRate>0)) {
         return (byte) 1;
      }
      return previousMovementRate;
   }

   @Override
   public boolean isBeneficial() {
      return true;
   }

   @Override
   public boolean isIncompatibleWith(Spell spell) {
      if (spell.getClass() == SpellFlight.class) {
         return true;
      }
      return super.isIncompatibleWith(spell);
   }

   @Override
   public boolean takesPrecedenceOver(Spell spell) {
      if (spell.getClass() == SpellFlight.class) {
         return false;
      }
      return super.takesPrecedenceOver(spell);
   }


}
