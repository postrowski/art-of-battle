package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;

/*
 * Created on May 4, 2006
 *
 */
public abstract class WoundChart implements Enums {

   DamageType damageType;
   // prevent default ctor
   @SuppressWarnings("unused")
   private WoundChart() {}

   public WoundChart(DamageType damageType) {
      this.damageType = damageType;
      WoundCharts.registerWithFactory(this);
   }

   abstract public Wound getWound(byte damageLevel, Character target);

}
