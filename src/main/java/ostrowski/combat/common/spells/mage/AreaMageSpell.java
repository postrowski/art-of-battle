/*
 * Created on May 18, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.server.ArenaLocation;

public abstract class AreaMageSpell extends MageSpell implements IAreaSpell
{
   private ArenaLocation _targetLocation = null;

   @SuppressWarnings("rawtypes")
   public AreaMageSpell(String name, Class[] prerequisiteSpells, MageCollege[] colleges) {
      super(name, prerequisiteSpells, colleges);
   }

   @Override
   public void setTargetLocation(ArenaLocation targetLocation)
   {
      _targetLocation = targetLocation;
   }

   @Override
   public ArenaLocation getTargetLocation()
   {
      return _targetLocation;
   }
   @Override
   public TargetType getTargetType() {
      return TargetType.TARGET_AREA;
   }

   @Override
   public short getMaxRange() {
      return 1;
   }
   @Override
   public short getMinRange() {
      return 0;
   }
}
