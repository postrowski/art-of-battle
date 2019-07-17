/*
 * Created on May 18, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;

public abstract class AreaMageSpell extends MageSpell implements IAreaSpell
{
   private ArenaLocation _targetLocation = null;

   @SuppressWarnings("rawtypes")
   public AreaMageSpell(String name, Class[] prerequisiteSpells, MageCollege[] colleges) {
      super(name, prerequisiteSpells, colleges);
   }

   @Override
   public void setTargetLocation(ArenaLocation targetLocation, Arena arena)
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
   public void affectCharacterOnEntry(Character enteringCharacter) {
   }

   @Override
   public void affectCharacterOnExit(Character exitingCharacter) {
   }

   @Override
   public void affectCharacterOnRoundStart(Character characterInHex) {
   }

   @Override
   public void affectCharacterOnRoundEnd(Character characterInHex) {
   }

   @Override
   public String getImageResourceName() {
      return null;
   }

   @Override
   public void affectCharacterOnActivation(Character chr) {
      affectCharacterOnEntry(chr);
   }

   @Override
   public void affectCharacterOnDeactivation(Character chr) {
   }
}
