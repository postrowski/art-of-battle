/*
 * Created on May 13, 2007
 *
 */
package ostrowski.combat.common.spells.mage;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.server.Arena;

public class SpellPush extends ResistedMageSpell implements ICastInBattle
{
   public static final String NAME = "Push";
   public SpellPush() {
      super(NAME, Attribute.Health, (byte) 1/*resistedActions*/, false/*expires*/,
            new Class[] {SpellCreateForce.class},
            new MageCollege[] {MageCollege.ENERGY});
   }

   @Override
   public String describeEffects(Character defender, boolean firstTime) {
      return null;
   }
   @Override
   public String describeSpell() {
      return "The '" + getName() + "' spell pushes the subject away from the caster."+
             " A 1-point power spell pushes the subject back 2 hexes, and every additional point doubles the distance." +
             " If the subject of the spell hits a wall as he is pushed back, he will take a wound that has a pain level equal"+
             " to the power put into the spell, and one wound for every two full points of power.";
   }

   @Override
   public void applyEffects(Arena arena) {
      int distanceKnockedBack = (int) Math.pow(2, getPower());
      while (distanceKnockedBack-- > 0) {
         if (!arena.moveToFrom(getTarget(), getCaster(), getCaster().getLimbLocation(LimbType.HAND_RIGHT, arena.getCombatMap())/*retreatFromLoc*/, null/*attackFromLimb*/)) {
            // we must have hit a wall, assess damage
            Wound wound = new Wound(getPower(),
                                    Location.BODY,
                                    getName() + " spell",
                                    getPower(),//painLevel
                                    getPower()/2,//wounds
                                    0,//bleedRate
                                    0,//armPenalty
                                    0,//movePenalty
                                    0,//knockedDownDist,
                                    DamageType.GENERAL,
                                    0,//effectMask,
                                    getTarget());
            getTarget().applyWound(wound, arena);
            String message = getTargetName() + " is pushed into a wall, taking the following damage: " + wound.describeWound();
            arena.sendMessageTextToAllClients(message, false/*popUp*/);
            break;
         }
      }
      getTarget().releaseHold();
   }
}
