/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.protocol.SyncRequest;

public class RequestUseOfHeroPoint extends SyncRequest
{
   public RequestUseOfHeroPoint() {
   }
   public RequestUseOfHeroPoint(Wound wound, int heroPointsLeft) {
      message = "You are about to suffer the following wound:\n" + wound.describeWound() +
                "\nYou have " + heroPointsLeft + " Hero Point left. Would you like to use a Hero Point to avoid this wound?";
      addOption(new RequestActionOption("Use Hero Point.", RequestActionType.OPT_USE_HERO_POINT, LimbType.BODY, true));
      addOption(new RequestActionOption("Do not use Hero Point, take wound.", RequestActionType.OPT_DONT_USE_HERO_POINT, LimbType.BODY, true));
   }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      if ((optionID % RequestActionOption.LIMB_MULTIPLIER) == RequestActionType.OPT_USE_HERO_POINT.ordinal()) {
         return "y";
      }
      if ((optionID % RequestActionOption.LIMB_MULTIPLIER) == RequestActionType.OPT_DONT_USE_HERO_POINT.ordinal()) {
         return "n";
      }
      return "1234567890";
   }

   public boolean isAnswerUseHeroPoint() {
      if (answer == null) {
         return false;
      }
      return (answer.getIntValue() % RequestActionOption.LIMB_MULTIPLIER) == RequestActionType.OPT_USE_HERO_POINT.ordinal();
   }

   public void setAnswerUseHeroPoint() {
      int i = 0;
      do {
         setAnswerByOptionIndex(i++);
      } while (!isAnswerUseHeroPoint());
   }
}
