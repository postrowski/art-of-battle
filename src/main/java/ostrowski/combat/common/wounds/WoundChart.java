package ostrowski.combat.common.wounds;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.html.HtmlBuilder;
import ostrowski.combat.common.html.TableRow;
import ostrowski.combat.server.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/*
 * Created on May 4, 2006
 *
 */
public abstract class WoundChart implements Enums {

   DamageType _damageType;
   // prevent default ctor
   @SuppressWarnings("unused")
   private WoundChart() {}

   public WoundChart(DamageType damageType) {
      _damageType = damageType;
      WoundCharts.registerWithFactory(this);
   }

   abstract public Wound getWound(byte damageLevel, Character target);

}
