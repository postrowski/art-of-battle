/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DieType;

public class SpellMagicMissile extends MissileMageSpell
{
   public static final String NAME = "Magic Missile";
   public SpellMagicMissile() {
      super(NAME, new Class[] {SpellCreateEarth.class, SpellCreateForce.class, SpellThrowSpell.class},
            new MageCollege[] {MageCollege.CONJURATION, MageCollege.EARTH, MageCollege.ENERGY},
            (byte)2/*baseDamage*/, (byte)2/*damagePerPower*/,
            DieType.D4, DamageType.BLUNT, (short)30/*rangeBase*/);
   }
}
