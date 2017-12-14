/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;

public class SpellFlameJet extends MissileMageSpell
{
   public static final String NAME = "Flame Jet";
   public SpellFlameJet() {
      super(NAME, new Class[] {SpellControlTemperature.class, SpellCreateFire.class, SpellThrowSpell.class},
            new MageCollege[] {MageCollege.CONJURATION, MageCollege.FIRE}, (byte)5/*baseDamage*/, (byte)3/*damagePerPower*/,
            DieType.D6, DamageType.FIRE, (short)10/*rangeBase*/);
   }
}
