/*
 * Created on Oct 23, 2006
 *
 */
package ostrowski.combat.common.things;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Race;

public class Head extends Limb
{
   public Head() {
   }
   public Head(LimbType id, Race racialBase) {
      super(id, racialBase);
   }
   @Override
   public Weapon getWeapon(Character character)
   {
      if (character.hasAdvantage(Race.PROPERTIES_STURGEBREAK)) {
         return Weapon.getWeapon(Weapon.NAME_SturgeBeak, character.getRace());
      }
      if (character.hasAdvantage(Race.PROPERTIES_FANGS)) {
         return Weapon.getWeapon(Weapon.NAME_Fangs, character.getRace());
      }
      if (character.hasAdvantage(Race.PROPERTIES_TUSKS)) {
         return Weapon.getWeapon(Weapon.NAME_Tusks, character.getRace());
      }
      if (character.hasAdvantage(Race.PROPERTIES_HORNS)) {
         return Weapon.getWeapon(Weapon.NAME_HornGore, character.getRace());
      }
      return Weapon.getWeapon(Weapon.NAME_HeadButt, character.getRace());
   }

   //static Weapon _headButt = Weapon.getWeapon(Weapon.NAME_HeadButt, character.getRace());

   @Override
   public Head clone() {
      return new Head(_limbType, getRacialBase());
   }

}
