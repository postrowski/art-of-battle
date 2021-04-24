/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;
import ostrowski.combat.common.enums.SkillType;

public class SpellLightning extends MissileMageSpell
{
   public static final String NAME = "Lightning";
   public SpellLightning() {
      super(NAME, new Class[] {SpellControlCharge.class, SpellCreateElectricity.class, SpellThrowSpell.class},
            new SkillType[] {SkillType.Spellcasting_Conjuration, SkillType.Spellcasting_Energy},
            (byte)-10/*baseDamage*/, (byte)5/*damagePerPower*/,
            DieType.D20, DamageType.ELECTRIC, (short)10/*rangeBase*/);
   }
}

