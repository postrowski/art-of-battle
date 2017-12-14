/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;

public class SpellIceDaggers extends MissileMageSpell
{
   public static final String NAME = "Ice Daggers";
   public SpellIceDaggers() {
      super(NAME, new Class[] {SpellControlTemperature.class, SpellCreateForce.class,
                               SpellCreateWater.class, SpellThrowSpell.class, SpellWaterJet.class},
            new MageCollege[] {MageCollege.CONJURATION, MageCollege.ENERGY, MageCollege.FIRE, MageCollege.WATER},
            (byte)0/*baseDamage*/, (byte)4/*damagePerPower*/,
            DieType.D8, DamageType.IMP, (short)25/*rangeBase*/);
   }
}
