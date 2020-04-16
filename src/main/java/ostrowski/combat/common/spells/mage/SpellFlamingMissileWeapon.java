/*
 * Created on May 13, 2007
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.html.Table;
import ostrowski.combat.common.html.TableRow;

public class SpellFlamingMissileWeapon extends SpellFlamingWeapon
{
   public static final String NAME = "Flaming Missile Weapon";

   @SuppressWarnings("unchecked")
   public SpellFlamingMissileWeapon() {
      super(new Class[] {SpellControlFire.class, SpellControlTemperature.class, SpellCreateFire.class, SpellFlamingWeapon.class});
   }

   @Override
   public String describeSpell() {
      Table table = new Table();
      table.addRow(new TableRow(-1, "Power", "Extra Pain", "Extra wounds"));
      for (byte power = 1; power < 8; power++) {
         table.addRow(new TableRow(power-1, ""+power, ""+getPain(power), ""+getWounds(power)));
      }
      return "The '" + getName() + "' spell causes the subject's missile weapon to ignite into flames." +
             " These flames automatically ignite any missile loaded into the weapon." +
             " Any time the missile hits an opponent, and penetrates the armor (does at least 1 point of damage)," +
             " then the flames of the weapon add additional pain and wounds, based upon the power of the spell:" +
             table +
             "<br/>This spell has no effect when cast on melee weapons, or thrown weapons." +
             " Use the 'Flaming Weapon' spell for that.";
   }
}
