/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;

public class SpellWaterJet extends MissileMageSpell
{
   public static final String NAME = "Water Jet";
   public SpellWaterJet() {
      super(NAME, new Class[] {SpellCreateForce.class, SpellCreateWater.class, SpellThrowSpell.class},
            new MageCollege[] {MageCollege.CONJURATION, MageCollege.ENERGY, MageCollege.WATER},
            (byte)6/*baseDamage*/, (byte)2/*damagePerPower*/,
            DieType.D6, DamageType.BLUNT, (short)18/*rangeBase*/);
   }
}
