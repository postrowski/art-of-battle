package ostrowski.combat.protocol.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.Rules;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.server.Arena;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.SyncRequest;

public class RequestSingleTargetSelection extends SyncRequest implements Enums
{
   public RequestSingleTargetSelection() {
      // used only for Serialization
   }

   public RequestSingleTargetSelection(final Character requestor, Arena arena, RequestAction action) {
      // Use this ctor when selecting a target for a missile weapon
      this(requestor, null, arena, action);
   }
   public RequestSingleTargetSelection(final Character requestor, Spell castingSpell, Arena arena, RequestAction action) {
      byte power = 0;
      byte adjustedStr = requestor.getAdjustedStrength();
      Weapon rangedWeapon = null;
      WeaponStyleAttackRanged rangedStyle = null;
      StringBuilder messageBuffer = new StringBuilder();
      if (castingSpell == null) {
         messageBuffer.append(requestor.getName()).append(", select a new target");
         Weapon weapon = requestor.getWeapon();
         if (weapon != null) {
            if (weapon.isMissileWeapon()) {
               rangedWeapon = weapon;
               rangedStyle = (WeaponStyleAttackRanged) weapon.getAttackStyle(0);
            }
            if (weapon.isThrowable()) {
               rangedWeapon = weapon;
               rangedStyle = weapon.getThrownAttackStyle();
            }
            if (rangedStyle != null) {
               short rangeBase = rangedStyle.getRangeBase();
               short adjustedRangeBase = (short) Math.round(rangeBase * Rules.getRangeAdjusterForAdjustedStr(adjustedStr));

               messageBuffer.append(" for your ").append(rangedWeapon.getName()).append(" which has a base range of ").append(rangeBase).append(" hexes");
               if (adjustedRangeBase != rangeBase) {
                  messageBuffer.append(" (").append(Attribute.Strength.shortName);
                  messageBuffer.append(" adjusted to ").append(adjustedRangeBase).append(")");
               }
            }
         }
      }
      else {
         power = castingSpell.getPower();
         if (castingSpell instanceof PriestSpell) {
            IRequestOption answer = action.answer();
            if (answer instanceof RequestActionOption) {
               RequestActionOption reqActOpt = (RequestActionOption) answer;
               HashMap<RequestActionType, Integer> powerPerAction = new HashMap<>();
               powerPerAction.put(RequestActionType.OPT_COMPLETE_PRIEST_SPELL_1, 1);
               powerPerAction.put(RequestActionType.OPT_COMPLETE_PRIEST_SPELL_2, 2);
               powerPerAction.put(RequestActionType.OPT_COMPLETE_PRIEST_SPELL_3, 3);
               powerPerAction.put(RequestActionType.OPT_COMPLETE_PRIEST_SPELL_4, 4);
               powerPerAction.put(RequestActionType.OPT_COMPLETE_PRIEST_SPELL_5, 5);
               powerPerAction.put(RequestActionType.OPT_CHANGE_TARGET_PRIORITIES, (int)requestor.getAffinity(((PriestSpell)castingSpell).getDeity()));
               if (powerPerAction.containsKey(reqActOpt.getValue())) {
                  power = (byte) powerPerAction.get(reqActOpt.getValue()).intValue();
               }
            }
         }

         messageBuffer.append(requestor.getName()).append(", please select the target for your ");
         if (power > 0) {
            messageBuffer.append(power).append("-point ");
         }
         messageBuffer.append(castingSpell.getName()).append(" spell");

         if (castingSpell instanceof IRangedSpell) {
            IRangedSpell rangedSpell = (IRangedSpell)castingSpell;
            Attribute attr = castingSpell.getCastingAttribute();

            short rangeBase = rangedSpell.getRangeBase();
            short adjustedRangeBase = castingSpell.getAdjustedRangeBase();

            messageBuffer.append(", which has a base range of ").append(rangeBase).append(" hexes");
            if (adjustedRangeBase != rangeBase) {
               messageBuffer.append(" (").append(attr.shortName);
               if (requestor.getRace().getBuildModifier() != 0) {
                  messageBuffer.append(" and size");
               }
               messageBuffer.append(" adjusted to ").append(adjustedRangeBase).append(")");
            }
         }
      }
      messageBuffer.append(".");
      _message = messageBuffer.toString();
      ArrayList<Character> combatants = arena.getCombatants();
      ArrayList<Character> targets =  new ArrayList<>();
      boolean targetEnemies = (castingSpell == null) || !castingSpell.isBeneficial();
      synchronized (combatants) {

         for (Character other : combatants) {
            // If this is a beneficial spell, only offer choices of allies
            // If this is a harmful spell or an attack, only offer choices of enemies
            if (targetEnemies == requestor.isEnemy(other)) {
               if (castingSpell != null) {
                  if (castingSpell.canTarget(requestor, other) == null) {
                     targets.add(other);
                  }
               }
               // Attacks (non-spells) should only attack conscious enemies.
               // Spells deal with this condition by calling castingSpell.canTarget(),
               // which considers the condition of the target
               else if (other.getCondition().isConscious()) {
                  targets.add(other);
               }
            }
         }
      }
      // sort the list of target based upon the distance from the caster:
      Comparator< ? super Character> sortOnDistance = new Comparator<>() {
         @Override
         public int compare(Character o1, Character o2) {
            short dist1 = Arena.getMinDistance(requestor, o1);
            short dist2 = Arena.getMinDistance(requestor, o2);
            if (dist1 == dist2) {
               return 0;
            }
            if (dist1 > dist2) {
               return 1;
            }
            return -1;
         }
      };
      Collections.sort(targets, sortOnDistance );

      for (Character target : targets) {
         // Don't allow a selection of someone they can't see.
         boolean hasLineOfSight = arena.hasLineOfSight(requestor, target);
         if (hasLineOfSight) {
            if (!arena.getCombatMap().isFacing(requestor, target)) {
               continue;
            }
            boolean enabled = true;
            StringBuilder description = new StringBuilder();
            if (target._uniqueID == requestor._uniqueID) {
               description.append("Self");
            }
            else {
               description.append(target.getName());
            }
            String cantTargetReason = null;
            if (castingSpell != null) {
               cantTargetReason = castingSpell.canTarget(requestor, target);
            }

            if (cantTargetReason != null) {
               if (cantTargetReason.length() > 0) {
                  description.append(" (");
                  description.append(cantTargetReason);
                  description.append(")");
               }
               enabled = false;
            }
            else {
               short distance = Arena.getMinDistance(requestor, target);
               description.append(", ");
               description.append(distance).append(" hexes away");
               if (requestor.getHoldLevel(target) != null) {
                  description.append(" (holding you)");
               }
               if (target.getHoldLevel(requestor) != null) {
                  description.append(" (you are holding)");
               }
               if (castingSpell != null) {
                  if (castingSpell instanceof IRangedSpell) {
                     RANGE range = castingSpell.getRange(distance);
                     description.append(" - ").append(range.getName());
                     if (range != RANGE.OUT_OF_RANGE) {
                        description.append(" range");
                     }
                     if (enabled && (range == RANGE.OUT_OF_RANGE)) {
                        enabled = false;
                     }
                  }
                  else if (castingSpell instanceof PriestSpell) {
                     if (power < (distance - 1)) {
                        description.append(" - ").append(RANGE.OUT_OF_RANGE.getName());
                        enabled = false;
                     }
                  }
               }
               else  if (rangedStyle != null) {
                  RANGE range = rangedStyle.getRangeForDistance(distance, adjustedStr);
                  description.append(" - ").append(range.getName());
                  if (range != RANGE.OUT_OF_RANGE) {
                     description.append(" range");
                  }
               }
            }

            addOption(target._uniqueID, description.toString(), enabled);
         }
      }
      addOption(OPT_CANCEL_ACTION, "Cancel", true/*enabled*/);
      setDefaultOption(requestor._targetID);
   }

   @Override
   public boolean isCancelable() {
      return false;
   }

}
