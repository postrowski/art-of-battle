/*
 * Created on July 5, 2006
 */
package ostrowski.combat.common;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ostrowski.DebugBreak;
import ostrowski.combat.client.CharacterDisplay;
import ostrowski.combat.client.ServerConnection;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.DefenseOption;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.ICastInBattle;
import ostrowski.combat.common.spells.IExpiringSpell;
import ostrowski.combat.common.spells.IInstantaneousSpell;
import ostrowski.combat.common.spells.IMissileSpell;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.IResistedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.MageSpell;
import ostrowski.combat.common.spells.mage.SpellSpiderWeb;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.evil.SpellMassFear;
import ostrowski.combat.common.spells.priest.good.SpellCharmPerson;
import ostrowski.combat.common.spells.priest.healing.SpellCureWound;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.MissileWeapon;
import ostrowski.combat.common.things.Potion;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestActionOption;
import ostrowski.combat.protocol.request.RequestActionType;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestDefense;
import ostrowski.combat.protocol.request.RequestEquipment;
import ostrowski.combat.protocol.request.RequestGrapplingHoldMaintain;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.protocol.request.RequestPosition;
import ostrowski.combat.protocol.request.RequestSingleTargetSelection;
import ostrowski.combat.protocol.request.RequestSpellSelection;
import ostrowski.combat.protocol.request.RequestSpellTypeSelection;
import ostrowski.combat.protocol.request.RequestUseOfHeroPoint;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.Battle;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoTracker;

public class AI implements Enums
{
   Character                 _self;
   AI_Type                   _aiType;

   // This map is maintained for each AI bot, a map of each of the other characters, where
   // they are on the map. This map is only updated while the other characters are visible.
   static final private Map<Integer, Map<Integer, List<ArenaLocation>>> _mapToMapOfLocations = new HashMap<>();
   List<Character>                                                             _targets;
   ArenaLocation                                                               _initialLocation     = null;
   Orientation _locationBeforeHidingFromAttacker = null;
   // This is the unique ID of the Ally that we are trying to reach or help.
   private int                                                                 _allyID              = -1;
   private static final short CHARGE_DISTANCE = 4;

   public AI(Character self, AI_Type aiType) {
      _self = self;
      _aiType = aiType;
      _self.setAIType(_aiType);
   }

   public AI_Type getAiType() {
      return _aiType;
   }

   // return true if we want the inObj to be sent back to the server.
   @SuppressWarnings("unused")
   public boolean processObject(SerializableObject inObj, Arena arena, CombatMap map, CharacterDisplay display) {
      if ((_initialLocation == null) && (map != null)) {
         List<ArenaLocation> initialLocations = map.getLocations(_self);
         if ((initialLocations != null) && (initialLocations.size() > 0)) {
            _initialLocation = initialLocations.get(0);
         }
      }

      Integer selfID = _self._uniqueID;
      Character newSelf = ServerConnection._charactersMap.get(selfID);
      if (newSelf != null) {
         _self = newSelf;
         _self.setAIType(_aiType);
      }

      try {
         if (inObj instanceof SyncRequest) {
            boolean debug = false;
            do {
               SyncRequest req = (SyncRequest) inObj;
               if (req instanceof RequestAction) {
                  if (requestAction((RequestAction) req, map, display)) {
                     return true;
                  }
               }
               else if (req instanceof RequestAttackStyle) {
                  if (requestAttackStyle((RequestAttackStyle) req)) {
                     return true;
                  }
               }
               else if (req instanceof RequestSingleTargetSelection) {
                  if (requestSingleTargetSelection((RequestSingleTargetSelection) req, map, display)) {
                     return true;
                  }
               }
               else if (req instanceof RequestSpellTypeSelection) {
                  if (requestSpellTypeSelection((RequestSpellTypeSelection) req, map, display)) {
                     return true;
                  }
               }
               else if (req instanceof RequestSpellSelection) {
                  if (requestSpellSelection((RequestSpellSelection) req, map, display)) {
                     return true;
                  }
               }
               else if (req instanceof RequestMovement) {
                  boolean allowRanged = false;
                  Weapon myWeapon = getMyWeapon();
                  WeaponStyleAttackRanged rangedStyle = (myWeapon == null) ? null : myWeapon.getRangedStyle();
                  Character target = selectTarget(display, map, (rangedStyle != null)/*allowRangedAllack*/);
                  if (target != null) {
                     short currentDist = Arena.getMinDistance(_self, target);
                     if (rangedStyle != null) {
                        RANGE range = rangedStyle.getRangeForDistance(currentDist, _self.getAttributeLevel(Attribute.Strength));
                        if ((range != RANGE.LONG) && (range != RANGE.OUT_OF_RANGE)) {
                           allowRanged = true;
                        }
                     }
                     if (!allowRanged) {
                        Spell currentSpell = _self.getCurrentSpell(false/*eraseCurrentSpell*/);
                        if ((currentSpell != null) && !currentSpell.isBeneficial() && currentSpell.requiresTargetToCast()) {
                           if (currentSpell instanceof IRangedSpell) {
                              IRangedSpell rangedSpell = (IRangedSpell) currentSpell;
                              RANGE range = rangedSpell.getRange(currentDist);
                              if ((range != RANGE.LONG) && (range != RANGE.OUT_OF_RANGE)) {
                                 allowRanged = true;
                              }
                           }
                        }
                     }
                  }
                  if (requestMovement((RequestMovement) req, arena, map, display, target, allowRanged)) {
                     return true;
                  }
               }
               else if (req instanceof RequestLocation) {
                  // TODO: how do we target a location?
                  // Probably an AreaSpell being completed
                  RequestLocation reqLoc = (RequestLocation)req;
                  Spell currentSpell = _self.getCurrentSpell(false/*eraseCurrentSpell*/);
                  if (currentSpell instanceof IAreaSpell) {
                     byte radius = ((IAreaSpell) currentSpell).getRadiusOfAffect();
                     List<ArenaCoordinates> friendLocations = new ArrayList<>();
                     List<ArenaCoordinates> enemyLocations = new ArrayList<>();
                     for (Character character : map.getCombatants()) {
                        if (character.isEnemy(_self)) {
                           enemyLocations.addAll(character.getCoordinates());
                        }
                        else {
                           friendLocations.addAll(character.getCoordinates());
                        }
                     }

                     ArenaCoordinates bestCoord = null;
                     int bestScore = 0;
                     for (ArenaCoordinates coord : reqLoc.getSelectableCoordinates()) {
                        // Weight a friend as being 5 times more important to avoid than an enemy to hit.
                        int score =      scoreLocation(map, coord, radius, enemyLocations)
                                  - (5 * scoreLocation(map, coord, radius, friendLocations));
                        if (score > bestScore) {
                           bestCoord = coord;
                           bestScore = score;
                        }
                     }
                     if (bestCoord != null) {
                        return reqLoc.setAnswer(bestCoord._x, bestCoord._y);
                     }
                  }
                  return false;
               }
               else if (req instanceof RequestDefense) {
                  RequestDefense reqSimple = null;
                  RequestDefense reqSmart = null;
                  if (_aiType == AI_Type.SIMPLE) { // use old (simple) defense determination
                     reqSmart = new RequestDefense();
                     reqSimple = (RequestDefense) req;
                     req.copyDataInto(reqSmart);
                  }
                  else {
                     reqSimple = new RequestDefense();
                     reqSmart = (RequestDefense) req;
                     req.copyDataInto(reqSimple);
                  }
                  boolean simpleResult = requestDefenseSimple(reqSimple);
                  boolean smartResult = requestDefense(reqSmart);
                  if (reqSimple.getAnswerID() != reqSmart.getAnswerID()) {
                     StringBuilder threadLogger = (StringBuilder) CombatServer.getThreadStorage("thread logger");
                     if (threadLogger == null) {
                        threadLogger = new StringBuilder();
                     }
                     threadLogger.append(req.toString());
                     threadLogger.append(" simpleResult=").append(reqSimple.getAnswer());
                     threadLogger.append(" smartResult=").append(reqSmart.getAnswer());
                     if (false) {
                        requestDefenseSimple(reqSimple);
                        requestDefense(reqSmart);
                     }
                  }
                  if (reqSimple == req) {
                     return simpleResult;
                  }
                  return smartResult;
               }
               else if (req instanceof RequestPosition) {
                  if (requestPosition((RequestPosition) req)) {
                     return true;
                  }
               }
               else if (req instanceof RequestEquipment) {
                  if (requestEquipment((RequestEquipment) req, map, display)) {
                     return true;
                  }
               }
               else if (req instanceof RequestUseOfHeroPoint) {
                  RequestUseOfHeroPoint reqHeroPoint = (RequestUseOfHeroPoint) req;
                  reqHeroPoint.setAnswerUseHeroPoint();
                  return true;
               }
               else if (req instanceof RequestGrapplingHoldMaintain) {
                  List<RequestActionOption> priorities = new ArrayList<>();
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_HOLD_5, LimbType.BODY, true));
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_HOLD_4, LimbType.BODY, true));
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_HOLD_3, LimbType.BODY, true));
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_HOLD_2, LimbType.BODY, true));
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_HOLD_1, LimbType.BODY, true));
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_NO_ACTION, LimbType.BODY, true));
                  return selectAnswerByType(req, priorities);
               }
               debug = !debug;

               DebugBreak.debugBreak(_self.getName() + ": unable to decide what to do");
            } while (debug == true);
            return false;
         }
         if (inObj instanceof Character) {
            Character newChar = (Character) inObj;
            Integer uniqueID = newChar._uniqueID;
            if (uniqueID == _self._uniqueID) {
               _self.copyData(newChar);
            }
            else {
               if (_targets != null) {
                  for (Character target : _targets) {
                     if (target._uniqueID == uniqueID) {
                        target.copyData(newChar);
                        break;
                     }
                  }
               }
            }
            if (newChar._uniqueID != _self._uniqueID) {
               if ((_aiType== AI_Type.GOD) ||  map.canSee(newChar, _self, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
                  Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);
                  if (myVisibilityMap == null) {
                     myVisibilityMap = new HashMap<>();
                     _mapToMapOfLocations.put(_self._uniqueID, myVisibilityMap);
                  }
                  //Rules.diag("AI character " + _self.getName() + " sees " + newChar.getName() + " at " + newChar.getHeadCoordinates());
                  List<ArenaLocation> locs = map.getLocations(newChar);
                  myVisibilityMap.put(newChar._uniqueID, locs);
                  for (ArenaLocation loc : locs) {
                     loc.setKnownBy(_self._uniqueID, true);
                  }
               }
            }
            return true;
         }
         //         if (inObj instanceof MessageText ||
         //             inObj instanceof ServerStatus ||
         //             inObj instanceof BeginBattle) {
         //             return true;
         //         }
         return true;
      } catch (Throwable e) {
         System.err.println("exception in AI:");
         e.printStackTrace();
      }
      return false;
   }

   private static int scoreLocation(CombatMap map, ArenaCoordinates coord,
                                    byte radius, List<ArenaCoordinates> locations) {
      int score = 0;
      for (ArenaCoordinates enemyCoord : locations) {
         short dist = ArenaCoordinates.getDistance(enemyCoord, coord);
         if (dist <= radius) {
            score += ((radius - dist) + 1);
         }
      }
      return score;
   }

   private boolean requestSingleTargetSelection(RequestSingleTargetSelection req, CombatMap map, CharacterDisplay display) {
      Character bestTarget = null;
      List<Integer> validTargets = new ArrayList<>();
      int[] optionIds = req.getOptionIDs();
      boolean[] enabled = req.getEnableds();
      for (int index = 0 ; index < optionIds.length ; index++) {
         if (enabled[index]) {
            validTargets.add(optionIds[index]);
         }
      }
      Spell currentSpell = _self.getCurrentSpell(false/*eraseCurrentSpell*/);
      if (currentSpell != null) {
         if (currentSpell.isBeneficial()) {
            // Cast beneficial spells on ourself first:
            if (_self.isUnderSpell(currentSpell.getName()) == null) {
               for (int index = 0; index < req.getActionCount(); index++) {
                  if (req.getOptionIDs()[index] == _self._uniqueID) {
                     req.setAnswerByOptionIndex(index);
                     return true;
                  }
               }
            }
            // If we couldn't target ourselves, target any ally
            for (Character character : map.getCombatants()) {
               if (validTargets.contains(character._uniqueID)) {
                  if (!_self.isEnemy(character)) {
                     if (currentSpell.canTarget(_self, character) == null) {
                        if (currentSpell.getActiveSpellIncompatibleWith(character) == null) {
                           List<Integer> priorities = _self.getOrderedTargetPriorites();
                           priorities.add(0, character._uniqueID);
                           // if we cancel, we'll get an infinite loop:
                           //priorities.add(SyncRequest.OPT_CANCEL_ACTION);
                           return selectAnswer(req, priorities);
                        }
                     }
                  }
               }
            }
         }
         else {
            double bestChanceOfSuccess = 0;
            byte currentPain = _self.getPainPenalty(true/*accountForBersking*/);
            byte actionsUsed = _self.getActionsAvailableThisRound(false/*usedForDefenseOnly*/);

            for (Character character : map.getCombatants()) {
               if (validTargets.contains(character._uniqueID)) {
                  if (_self.isEnemy(character)) {
                     if (currentSpell.canTarget(_self, character) == null) {
                        if (currentSpell.getActiveSpellIncompatibleWith(character) == null) {
                           short minDistance = Arena.getMinDistance(_self, character);
                           short maxDistance = Arena.getMaxDistance(_self, character);
                           double oddsOfSuccess = getChanceOfSpellSuccess(currentSpell, character, actionsUsed, minDistance, currentPain);
                           if (minDistance != maxDistance) {
                              double oddsOfSuccessAtMax = getChanceOfSpellSuccess(currentSpell, character, actionsUsed, maxDistance, currentPain);
                              if (oddsOfSuccessAtMax > oddsOfSuccess) {
                                 oddsOfSuccess = oddsOfSuccessAtMax;
                              }
                           }
                           if (bestChanceOfSuccess < oddsOfSuccess) {
                              bestChanceOfSuccess = oddsOfSuccess;
                              bestTarget = character;
                              if (oddsOfSuccess >= 1.0) {
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
         if (bestTarget == null) {
            bestTarget = selectTarget(display, map, true/*allowRangedAttack*/);
         }
         if (bestTarget != null) {
            List<Integer> priorities = _self.getOrderedTargetPriorites();
            priorities.add(0, bestTarget._uniqueID);
            // if we cancel, we'll get an infinite loop:
            //priorities.add(SyncRequest.OPT_CANCEL_ACTION);
            return selectAnswer(req, priorities);
         }
      }
      return false;
   }

   private boolean requestSpellSelection(RequestSpellSelection req, CombatMap map, CharacterDisplay display) {
      Spell spell = selectSpellToCast(display, map, selectTarget(display, map, true/*allowRangedAllack*/));
      if (spell != null) {
         String[] options = req.getOptions();
         for (int index = 0; index < options.length; index++) {
            if (options[index].contains(spell.getName())) {
               req.setAnswerByOptionIndex(index);
               return true;
            }
         }
      }
      return false;
   }

   private boolean requestSpellTypeSelection(RequestSpellTypeSelection req, CombatMap map, CharacterDisplay display) {
      Spell spell = selectSpellToCast(display, map, selectTarget(display, map, true/*allowRangedAllack*/));
      if (spell != null) {
         String key = null;
         if (spell instanceof MageSpell) {
            key = RequestSpellTypeSelection.SPELL_TYPE_MAGE;
         }
         else if (spell instanceof PriestSpell) {
            PriestSpell priestSpell = (PriestSpell) spell;
            key = priestSpell.getDeity();
         }
         if (key != null) {
            String[] options = req.getOptions();
            for (int index = 0; index < options.length; index++) {
               if (options[index].contains(key)) {
                  req.setAnswerByOptionIndex(index);
                  return true;
               }
            }
         }
      }
      return false;
   }

   private boolean requestEquipment(RequestEquipment equipment, CombatMap map, CharacterDisplay display) {
      // The only time we should be here is if we are dropping our missile weapon
      // to ready our melee weapon, or readying a potion
      List<RequestActionOption> priorities = new ArrayList<>();
      // Always go to 3-action for missile attacks, unless the enemy gets too close
      // If an enemy gets too close, fire without targeting any longer.
      Weapon myWeapon = getMyWeapon();
      if ((myWeapon != null) && (myWeapon.isMissileWeapon())) {
         priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_DROP, LimbType.HAND_RIGHT, true));
         if (myWeapon.getAttackStyle(0).getHandsRequired() == 2) {
            priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_DROP, LimbType.HAND_LEFT, true));
         }
      }
      else {
         // If my right hand is cripple, and the left hand is holding a shield,
         // drop the shield so we can ready a weapon.
         Limb rightHand = _self.getLimb(LimbType.HAND_RIGHT);
         if ((rightHand != null) && (rightHand.isCrippled())) {
            Limb leftHand = _self.getLimb(LimbType.HAND_LEFT);
            if ((leftHand != null) && (!leftHand.isCrippled())) {
               Thing leftThing = leftHand.getHeldThing();
               if ((leftThing != null) && (leftThing instanceof Shield)) {
                  // Don't drop the shield, because then we wouldn't be able to
                  // pickup the weapon we probably just dropped.
                  String applyName = "drop " + leftThing.getName();
                  priorities.add(new RequestActionOption(applyName, RequestActionType.OPT_EQUIP_UNEQUIP_DROP,
                                                         leftHand._limbType, true));
               }
            }
         }
      }
      Character target = selectTarget(display, map, (myWeapon != null) && (myWeapon.getRangedStyle() != null)/*allowRangedAllack*/);
      findPotionIndexToUse(equipment, target, priorities);

      List<Thing> equip = _self.getEquipment();
      // First get a shield ready if we have one on our belt
      // unless our right hand is crippled, in which case we
      // want to ready a weapon in our left hand.
      Limb rightArm = _self.getLimb(LimbType.HAND_RIGHT);
      if ((rightArm != null) && !rightArm.isCrippled()) {
         for (Thing thing : equip) {
            if ((thing != null) && (thing.isReal())) {
               if (thing instanceof Shield) {
                  // Don't try to get a shield ready in our right hand,
                  // because we always want a weapon available.
                  RequestActionType actType = RequestActionType.getEquipUnequipReadyActionByIndex(equip.indexOf(thing));
                  priorities.add(new RequestActionOption("", actType, LimbType.HAND_LEFT, true));
               }
            }
         }
      }
      int armCount = _self.getUncrippledArmCount(true/*reduceCountForTwoHandedWeaponsHeld*/);
      // then get a weapon ready.
      Weapon bestWeapon = null;
      for (Thing thing : equip) {
         if ((thing != null) && (thing.isReal())) {
            if (thing instanceof Weapon) {
               Weapon weapon = (Weapon) thing;
               if (weapon.isOnlyTwoHanded() && (armCount < 2)) {
                  continue;
               }
               if (bestWeapon == null) {
                  bestWeapon = weapon;
                  continue;
               }
               if (_self.getBestSkillLevel(weapon) > _self.getBestSkillLevel(bestWeapon)) {
                  bestWeapon = weapon;
               }
            }
         }
      }
      if (bestWeapon != null) {
         String readyName = "ready " + bestWeapon.getName();
         for (LimbType limbType : LimbType._armTypes) {
            for (IRequestOption reqOpt :  equipment.getReqOptions()) {
               if (reqOpt.isEnabled()) {
                  if (reqOpt.getName().startsWith(readyName)) {
                     if (reqOpt instanceof RequestActionOption) {
                        RequestActionOption reqActOpt = (RequestActionOption) reqOpt;
                        if (reqActOpt.getLimbType() == limbType) {
                           priorities.add(reqActOpt);
                        }
                     }
                  }
               }
            }
         }
      }
      return selectAnswerByType(equipment, priorities);
   }

   public void assessPersonalWounds(List<Integer> outList) {
      int maxWound = 0;
      int woundCount = 0;
      boolean crippledLimb = false;
      for (Wound wound : _self.getWoundsList()) {
         if (wound.getEffectiveWounds() > 0) {
            if (wound.getWounds() > maxWound) {
               maxWound = wound.getWounds();
            }
            // If this limb is crippled, but not served (-2),
            // then a 'Healing', 'Major Healing' or 'Full Healing' potion would help up.
            if (wound.getPenaltyLimb() == -1) {
               crippledLimb = true;
            }
            woundCount++;
         }
      }
      outList.add(maxWound);
      outList.add(woundCount);
      outList.add(crippledLimb ? 1 : 0);
   }
   public boolean findPotionIndexToUse(SyncRequest action, Character target,
                                       List<RequestActionOption> requestEquipmentPriorities) {
      // berserking characters can't use potions
      if (_self.isBerserking()) {
         return false;
      }

      // do we need healing?
      List<Integer> outList = new ArrayList<>();
      assessPersonalWounds(outList);
      int maxWound = outList.remove(0);
      int woundCount = outList.remove(0);
      boolean crippledLimb = outList.remove(0) == 1;

      // If we have a target, check for non-healing beneficial potions we should use.
      // But if we don't have a target, don't use a beneficial potion, because we
      // want it's effect to last through a battle with the target
      if (target != null) {
         if (findPotionToUse(requestEquipmentPriorities, null, action)) {
            return true;
         }
      }

      // find the index of the weakest potion that will cure our maxWound.
      // should we use a full healing potion?
      if ((woundCount > 4) || crippledLimb) {
         if (findPotionToUse(requestEquipmentPriorities, Potion.POTION_FULL_HEALING, action)) {
            return true;
         }
      }
      if (maxWound >= 1) {
         if ((maxWound >= 3) || crippledLimb) {
            if ((maxWound > 4) || crippledLimb) {
               if (findPotionToUse(requestEquipmentPriorities, Potion.POTION_MAJOR_HEALING, action)) {
                  return true;
               }
            }
            if (findPotionToUse(requestEquipmentPriorities, Potion.POTION_HEALING, action)) {
               return true;
            }
         }
         if (findPotionToUse(requestEquipmentPriorities, Potion.POTION_MINOR_HEALING, action)) {
            return true;
         }
      }
      return false;
   }

   private boolean findPotionToUse(List<RequestActionOption> requestEquipmentPriorities,
                                   String potionName, SyncRequest action) {
      for (Thing thing : _self.getEquipment()) {
         if ((thing != null) && (thing.isReal())) {
            if (thing instanceof Potion) {
               Potion potion = (Potion) thing;
               if (potion.isBeneficial()) {
                  boolean usePotion = false;
                  if (potionName == null) {
                     // a null potionName means use any non-healing, beneficial Potion:
                     usePotion = !potion.isHealing();
                  }
                  else {
                     usePotion = potion.getName().equals(potionName);
                  }
                  if (usePotion) {
                     if ((requestEquipmentPriorities != null) && (action != null)) {
                        String[] options = action.getOptions();
                        IRequestOption[] optionIDs = action.getReqOptions();
                        boolean[] enableds = action.getEnableds();
                        // try to apply it first
                        String applyName = "apply " + potion.getName();
                        for (int optionIndex = 0; optionIndex < options.length; optionIndex++) {
                           if (options[optionIndex].equals(applyName)) {
                              if (enableds[optionIndex]) {
                                 requestEquipmentPriorities.add((RequestActionOption) optionIDs[optionIndex]);
                                 return true;
                              }
                           }
                        }
                        // If we couldn't directly apply it, ready it first
                        String readyName = "ready " + potion.getName();
                        for (int optionIndex = 0; optionIndex < options.length; optionIndex++) {
                           if (options[optionIndex].equals(readyName)) {
                              if (enableds[optionIndex]) {
                                 requestEquipmentPriorities.add((RequestActionOption) optionIDs[optionIndex]);
                                 return true;
                              }
                           }
                        }
                        // If we can't ready it, could be because our hands are full:
                        if (!_self.canPickup(potion)) {
                           RequestActionOption[] tryOptions = new RequestActionOption[] {
                                  new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, LimbType.HAND_RIGHT, true),
                                  new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_SHEATH, LimbType.HAND_LEFT,  true),
                                  new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_DROP,   LimbType.HAND_RIGHT, true),
                                  new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP_DROP,   LimbType.HAND_LEFT,  true)
                            };
                           for (RequestActionOption tryOption : tryOptions) {
                              for (int actionIndex = 0; actionIndex < optionIDs.length; actionIndex++) {
                                 if (optionIDs[actionIndex].getIntValue() == tryOption.getIntValue()) {
                                    if (enableds[actionIndex]) {
                                       requestEquipmentPriorities.add(tryOption);
                                       return true;
                                    }
                                 }
                              }
                           }
                        }
                        return false;
                     }
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   private Character getCharacter(int characterID) {
      if (characterID != -1) {
         if (_targets != null) {
            for (Character character : _targets) {
               if (character._uniqueID == characterID) {
                  return character;
               }
            }
         }
         // We could get here if the attacker is berserking, and therefore normal enemies aren't what's expected.
         return CombatServer._this._map.getCombatMap().getCombatantByUniqueID(characterID);
      }
      return null;
   }

   private boolean requestAction(RequestAction action, CombatMap map, CharacterDisplay display) {
      StringBuilder traceSb = new StringBuilder();
      Weapon myWeapon = getMyWeapon();
      boolean needShield = false;
      // See if we need to pickup a shield:
      byte shieldLevel = _self.getSkillLevel(SkillType.Shield, null/*useLimb*/, false, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
      // Do we know how to use a shield?
      if (shieldLevel > 0) {
         // Is our left arm able to carry a shield?
         Limb rightArm = _self.getLimb(LimbType.HAND_RIGHT);
         Limb leftArm = _self.getLimb(LimbType.HAND_LEFT);
         if ((leftArm != null) && (!leftArm.isCrippled())) {
            // Is our right hand able to carry a weapon?
            if ((rightArm != null) && (!rightArm.isCrippled())) {
               Thing heldThing = leftArm.getHeldThing();
               // Are we already carrying one?
               if (heldThing == null) {
                  // Are we carrying a two-handed weapon?
                  if ((myWeapon == null) || (!myWeapon.isOnlyTwoHanded())) {
                     needShield = true;
                  }
               }
            }
         }
      }
      Character target = selectTarget(display, map, (myWeapon != null) && (myWeapon.getRangedStyle() != null)/*allowRangedAllack*/);
      byte skillLevel = 0;
//      Limb limbUsedInAttack = null;
      if (myWeapon != null) {
//         for (Limb limb : _self.getLimbs()) {
//            if (limb.getWeapon(_self) == myWeapon) {
//               limbUsedInAttack = limb;
//               break;
//            }
//         }
         skillLevel = _self.getBestSkillLevel(myWeapon);
      }
      byte currentPain = _self.getPainPenalty(true/*accountForBerserking*/);
      byte myActionsThisRound = _self.getActionsAvailableThisRound(false/*usedForDefenseOnly*/);
      byte targetActionsThisRound = 0;
      int targetMaxRange = 0;
      traceSb.append("\n**** AI.RequestAction for ").append(_self.getName());
      traceSb.append(", pain:").append(currentPain);
      traceSb.append(", myWeapon:").append((myWeapon == null) ? "null" : myWeapon.getName());
      traceSb.append(", myActions:").append(myActionsThisRound);
      traceSb.append(", target:");
      if (target == null) {
         traceSb.append("null");
      }
      else {
         targetActionsThisRound = target.getActionsAvailableThisRound(false/*usedForDefenseOnly*/);
         if (targetActionsThisRound > 3) {
            // If I have 3 actions, but he has more, its because he's got a speed spell
            // Treat it as him having 3 actions
            traceSb.append(", targetOnSpeed");
            targetActionsThisRound = 3;
         }
         targetMaxRange = target.getWeaponMaxRange(false/*allowRanged*/, false/*onlyChargeTypes*/);
         traceSb.append(target.getName());
         traceSb.append(", targetActions:").append(targetActionsThisRound);
         traceSb.append(", targetMaxRange=").append(targetMaxRange);
      }
      traceSb.append("\n****");
      byte attackActions = 0;
      if ((target != null) && (myWeapon != null)) {
         int weaponSpeed = 0;
         for (int style = 0; style < myWeapon._attackStyles.length; style++) {
            if (!myWeapon.getAttackStyle(style).isThrown()) {
               weaponSpeed = myWeapon.getAttackStyle(style).getSpeed(_self.getAttributeLevel(Attribute.Strength));
               break;
            }
         }
         traceSb.append(" weaponspeed:").append(weaponSpeed);
         if (myWeapon.isMissileWeapon()) {
            // If we're using a missile weapon, don't consider his actions count, just attack (when ready).
            if (myActionsThisRound > 0) {
               attackActions = 1;
            }
         }
         else {
            Limb targetsRightArm = target.getLimb(LimbType.HAND_RIGHT);
            if ((targetsRightArm != null) && (targetsRightArm.getActionsNeededToReady() > 0)) {
               // since the target can't attack, consider him to have 0 actions
               targetActionsThisRound = 0;
               traceSb.append(",targetWeapNotReady");
            }
            if (targetActionsThisRound <= myActionsThisRound) {
               traceSb.append(",target <= my actions");
               // determine best action usage based on how many actions I have, and how much the enemy has
               //           3 2 1 0 <-enemy actions
               //           -------
               //   my    3|2 3 3 3
               // actions 2|- 2 2 2
               //         1|- - - 1
               if (targetActionsThisRound == myActionsThisRound) {
                  if (myActionsThisRound > 1) {
                     traceSb.append(",2-actions");
                     attackActions = 2;
                     // Should we make a 3-action attack?
                     // do this if we have 3-actions available, and our weapon
                     // has 2-actions ready time.
                     if (myActionsThisRound >= 3) {
                        if (weaponSpeed >= 2) {
                           traceSb.append(",slow weap (3 actions)");
                           attackActions = 3;
                        }
                     }
                  }
               }
               else {
                  attackActions = myActionsThisRound;
                  traceSb.append(",full act ").append(attackActions);
               }
            }
            else {
               traceSb.append(",I've less actions (").append(attackActions).append(")");
            }
            if (weaponSpeed >= 2) {
               if (attackActions < 3) {
                  if (_self.getActionsPerTurn() >= 3) {
                     double rnd = CombatServer.random();
                     if (rnd > .5) {
                        // half the time, don't attack unless we can attack with 3 actions
                        attackActions = 0;
                     }
                     traceSb.append(",slow non-full ").append(attackActions);
                     traceSb.append("(rnd=").append((attackActions * 10) / 10.0).append(")");
                  }
               }
            }
         }
         traceSb.append(",");
      }
      boolean canThrow = false;
      short curMinDist = (target == null) ? 100 : Arena.getMinDistance(_self, target);
      short curMaxDist = (target == null) ? 100 : Arena.getMaxDistance(_self, target);
      traceSb.append(" curDist=").append(curMinDist);
      if (target != null) {
         if (myWeapon != null) {
            if (map.canSee(_self, target, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
               traceSb.append(", canSee");
               //if (curMinDist > (_self.getWeaponMaxRange(false/*allowRanged*/, false/*onlyChargeTypes*/) + 1))
               {
                  //traceSb.append(", outOfRange");
                  if (myWeapon.isThrowable() && (myWeapon.getRangedStyle().getMinRange() <= curMinDist)) {
                     canThrow = true;
                     traceSb.append(", canThrow");
                  }
               }
            }
            else {
               traceSb.append(", canNotSee");
               // can't see the target, so must walk around to get to him
               List<Orientation> path = findPath(_self, target, map, true/*allowRanged*/);
               if (path != null) {
                  traceSb.append(", path");
                  curMinDist = (short) Math.max(curMinDist, path.size());
                  curMaxDist = (short) Math.max(curMaxDist, path.size());
               }
               else {
                  traceSb.append(", NoPath");
                  curMinDist = (short) 100;
                  curMaxDist = (short) 100;
               }
               traceSb.append(", curDist=").append(curMinDist);
            }
         }
         else {
            traceSb.append(", noWeap");
         }
      }
      else {
         traceSb.append(", noTarget");
      }
      boolean throwNextRound = false;
      if (canThrow) {
         // If we don't have a decent backup weapon, don't throw
         // away our current weapon.
         Weapon altWeapon = getBestAltWeapon(myWeapon);
         if (altWeapon == null) {
            traceSb.append(", NoAltWeap");
            canThrow = false;
         }
         else {
            // If the enemy is out of range to attack us,
            // throw a 3-action attack at him, if possible.
            if (myActionsThisRound > 1) {
               // When throwing weapons, always throw with 2-actions.
               attackActions = 2;
               traceSb.append(", actions(2)");
            }
            else {
               traceSb.append(", notEnoughActionsToThrow");
               canThrow = false;
               throwNextRound = true;
            }
         }
      }
      boolean myWeaponIsMissile = ((myWeapon != null) && (myWeapon.isMissileWeapon()));
      List<Character> beingAimedAtBy = new ArrayList<>();
      // If we are an animal, we have no knowledge of being targeted.
      if (!_self.isAnimal()) {
         for (Character attacker : map.getCombatants()) {
            if (attacker.getAimDuration(_self._uniqueID) > 0) {
               beingAimedAtBy.add(attacker);
            }
         }
      }
      traceSb.append(", beingAimedAtBy=[");
      if (beingAimedAtBy != null) {
         for (Character aimer : beingAimedAtBy) {
            traceSb.append(aimer.getName());
         }
      }
      traceSb.append("]");
      // Don't bother attacking if we are severely hurt.
      if (attackActions > 0) {
         // If our pain is less than 2, we can always attack
         double lowerPainLimit = 2;
         // If our pain is greater than our skill reduced by pain, never attack
         double upperPainLimit = skillLevel - _self.getWounds();
         // The higher our DEX, the less important it is to remain defensive
         upperPainLimit += Math.max((_self.getAttributeLevel(Attribute.Dexterity) - 1), 1) * attackActions;
         if (myWeaponIsMissile) {
            // If this is a missile weapon, weight the penalty based on how many actions we
            // have available. If we have a lot of actions available, then the pain doesn't mean
            // much, because we can burn that action re-readying the next shot before our pain goes down again.
            // If we are low on actions, wait for the next turn.
            upperPainLimit += (_self.getActionsAvailable(false/*usedForDefenseOnly*/) - 1) * 3;
         }
         // In between these limits, make a random choice based on a bell curve:
         double randomBellCurve = (CombatServer.random() + CombatServer.random() + CombatServer.random()) / 3.0;
         // determine our actual pain limit
         double neverAttackOverPainLimitOf = lowerPainLimit + (randomBellCurve * (upperPainLimit - lowerPainLimit));

         traceSb.append(", neverAttackOverPainLimitOf=").append((int)(neverAttackOverPainLimitOf*100)/100.0);
         if (currentPain > neverAttackOverPainLimitOf) {
            // If the penalty to attack is too high, don't attack
            attackActions = 0;
            traceSb.append(", tooMuchPain(0 actions)");
         }
      }
      if ((attackActions > 0) && (myWeapon != null)) {
         // Don't allow weapon throwing in some conditions:
         if (canThrow) {
            if (_self.getSkillLevel(SkillType.Throwing, LimbType.HAND_RIGHT, false /*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/) < 1) {
               // never throw anything if our throwing skill is 0.
               traceSb.append(", noThrowSkill");
               canThrow = false;
            }
            else {
               if (myWeapon._attackStyles.length == 1) {
                  // This weapon can only be thrown.
               }
               else {
                  Weapon altWeapon = _self.getAltWeapon();
                  if (altWeapon != null) {
                     if (altWeapon.getName().equals(myWeapon.getName())) {
                        // Our alternate weapon is the same as our primary weapon,
                        // we can throw the primary.
                     }
                     else {
                        // Do we have another significant weapon other than this one?
                        if (altWeapon.isUnarmedStyle()) {
                           traceSb.append(", NoAlt");
                           canThrow = false;
                        }
                     }
                  }
               }
            }
         }
         if (attackActions == 1) {
            traceSb.append(", actions==1");
            // don't do a 1-action attack with a weapon that needs more than 1 action to re-ready
            int fastestAttackSpeed = 10;
            for (int style = 0; style < myWeapon._attackStyles.length; style++) {
               // don't count thrown styles.
               if (!myWeapon.getAttackStyle(style).isThrown()) {
                  int speed = myWeapon.getAttackStyle(style).getSpeed(_self.getAttributeLevel(Attribute.Strength));
                  if (speed < fastestAttackSpeed) {
                     fastestAttackSpeed = speed;
                  }
               }
            }
            traceSb.append(", fastestAttackSpeed=").append(fastestAttackSpeed);
            if (fastestAttackSpeed == 2) {
               // If our fastest attack requires two actions to re-ready, don't attack with a single action
               attackActions = 0;
            }
            else if (fastestAttackSpeed == 1) {
               // If our fastest attack requires one action to re-ready, don't always attack with a single action
               // weight our current pain level, since that will reduce our attack success.
               // Also, as our skill goes up, 1-action attacks are more effective, so increase the chances
               // based upon our skill level as well.
               double rnd = CombatServer.random();
               double minLevel = ((skillLevel - currentPain) / 10.0);
               traceSb.append(", rnd=").append((int) (rnd * 100) / 100.0);
               traceSb.append(", minLevel=").append((int) (minLevel * 100) / 100.0);
               if (rnd > minLevel) {
                  attackActions = 0;
                  traceSb.append(", notGoodEnough (actions 0)");
               }
            }
         }
      }

      // Always use 3-actions for missile attacks, unless the enemy gets too close
      // If an enemy gets too close, fire without targeting any longer.
      int tooCloseForComfort = 5;
      if (target != null) {
         tooCloseForComfort = target.getMovementRate();
         // don't account for missile weapons
         if (targetMaxRange < 4) {
            tooCloseForComfort += targetMaxRange;
         }
      }
      traceSb.append(", tooCloseForComfort=").append(tooCloseForComfort);

      List<RequestActionOption> priorities = new ArrayList<>();
      // priorities: heal self (potion/spell), if needed
      //             apply beneficial potions (speed, strength, etc.)
      //             pickup weapon I dropped (unless weapon arm crippled)
      //             ready weapon (1-action)
      //             stand up
      //             Continue casting spell
      //             attack (non-advance)
      //             attack (advancing)
      //             move toward target
      //             wait for opportunity to attack
      if (!_self.isAnimal()) {
         if (findPotionIndexToUse(action, target, null/*requestEquipmentPriorities*/)) {
            traceSb.append(", potion");
            priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true));
         }
         else if ((myWeapon == null) || (!myWeapon.isReal()) || needShield) {
            determineWeaponToPickup(action, map, traceSb, myWeapon, curMinDist, tooCloseForComfort, priorities);
            // do we have uncrippled limbs that will allow us to change weapons?
            int handCount = _self.getUncrippledArmCount(true/*reduceCountForTwoHandedWeaponsHeld*/);
            if (handCount > 0) {
               byte unarmedSkill = (myWeapon == null) ? 0 : _self.getBestSkillLevel(myWeapon);
               byte unarmedDamage = (myWeapon == null) ? 0 : myWeapon.getWeaponMaxDamage(this._self);
               for (Thing equThing : _self.getEquipment()) {
                  if (equThing instanceof Weapon) {
                     Weapon equWeapon = (Weapon) equThing;
                     if ((myWeapon == null) || (!myWeapon.isReal())) {
                        if (equThing.isReal()) {
                           if (myWeapon == null) {
                              traceSb.append(", readyOnlyWeap");
                              priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true));
                              break;
                           }
                           int equSkill = _self.getBestSkillLevel(equWeapon);
                           int skillDiff = equSkill - unarmedSkill;
                           if ((equWeapon.getWeaponMaxDamage(this._self) + skillDiff) > unarmedDamage) {
                              traceSb.append(", readyBetterWeap");
                              priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true));
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      if (curMinDist <= tooCloseForComfort) {
         if (myWeaponIsMissile) {
            traceSb.append(", snapShot");
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_MISSILE);
            // If attack is't an option, then switch weapons.
            priorities.add(new RequestActionOption("", RequestActionType.OPT_EQUIP_UNEQUIP, LimbType.BODY, true));
         }
      }
      else if (canThrow || myWeaponIsMissile) {
         traceSb.append(", prep?");
         addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_PREPARE_RANGED);
      }
      if ((target != null) && (myWeapon != null)) {
         // Missile weapons should spend 3 actions aiming, thrown only need 2.
         byte aimDuration = _self.getAimDuration(target._uniqueID);
         boolean continueTargeting = (aimDuration < (myWeaponIsMissile ? 3 : 2));

         traceSb.append(", aimDuration=").append(aimDuration);
         // If we are being targeted by anyone, attack immediately
         if (myWeaponIsMissile && (!beingAimedAtBy.isEmpty())) {
            for (Character attacker : beingAimedAtBy) {
               if (attacker.getAimDuration(_self._uniqueID) > 1) {
                  traceSb.append(", ImATarget");
                  continueTargeting = false;
                  break;
               }
            }
         }
         // If the enemy gets too close, attack immediately
         if (curMinDist <= tooCloseForComfort) {
            traceSb.append(", tooClose");
            continueTargeting = false;
         }
         // If we are at the end our the current round (1-action left), continue aiming
         // so we can throw at the top of next round.
         if (throwNextRound) {
            traceSb.append(", waitThrow");
            continueTargeting = true;
         }
         if (continueTargeting) {
            traceSb.append(", targeting");
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_TARGET_ENEMY);
         }
         else {
            if (attackActions >= 1) {
               if (myWeaponIsMissile) {
                  traceSb.append(", missileAttack");
                  addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_MISSILE);
               }
//               else {
//                  if (attackActionsS >= 2) {
//                     addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_THROW_2);
//                  }
//                  addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_THROW_1);
//               }
            }
         }
      }
      addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_READY_1);

      if (_self.getPosition() != Position.STANDING) {
         traceSb.append(", notStanding");
         if (!_self.getCondition().isCollapsed()) {
            traceSb.append(", notCollasped");
            // If we can't stand, go to a kneeling position
            if (_self.canStand() || (_self.getPosition() != Position.KNEELING)) {
               traceSb.append(", changePos");
               priorities.add(new RequestActionOption("", RequestActionType.OPT_CHANGE_POS, LimbType.BODY, true));
            }
         }
      }

      priorities.add(new RequestActionOption("", RequestActionType.OPT_CONTINUE_INCANTATION, LimbType.BODY, true));
      Spell currentSpell = _self.getCurrentSpell(false/*eraseCurrentSpell*/);
      if (currentSpell != null) {
         traceSb.append(", currentSpell=").append(currentSpell.getName());
         if (!currentSpell.requiresTargetToCast() || (target != null) || (currentSpell instanceof IAreaSpell)) {
            if (currentSpell instanceof MageSpell) {
               byte desiredPower = desiredPowerForSpell(currentSpell);
               if (!currentSpell.isInate() && (currentSpell.getPower() < desiredPower)) {
                  traceSb.append(", <desiredPower of ").append(desiredPower);
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_CHANNEL_ENERGY_1, LimbType.BODY, true));
               }
               else {
                  // If this is a missile spell, don't fire it is we are too far away
                  boolean tooFar = false;
                  if (currentSpell.isDefendable()) {
                     if (target == null) {
                        tooFar = true;
                     }
                     else {
                        RANGE range = currentSpell.getRange(curMinDist);
                        traceSb.append(", MissileMageSpell rage=").append(range);
                        if (range == RANGE.OUT_OF_RANGE) {
                           traceSb.append(", OoR");
                           tooFar = true;
                        }
                        else if (range == RANGE.LONG) {
                           // If the target has any actions left to dodge this, don't shoot at
                           // long range, because they will dodge it too easily
                           if (target.getActionsAvailableThisRound(true/*usedForDefenseOnly*/) > 0) {
                              traceSb.append(", LongRange(TargetWillDodge)");
                              tooFar = true;
                           }
                        }
                        else if (range == RANGE.MEDIUM) {
                           // If the target has any 2 (or more) actions left to dodge this, and they has a shield
                           // to block with, don't shoot at medium range, because they will dodge & block it too easily
                           if (target.getActionsAvailableThisRound(true/*usedForDefenseOnly*/) > 1) {
                              for (Limb limb : target.getLimbs()) {
                                 Thing heldThing = limb.getHeldThing();
                                 if (heldThing instanceof Shield) {
                                    traceSb.append(", MediumRange(TargetWillBlock&Dodge)");
                                    tooFar = true;
                                    break;
                                 }
                              }
                           }
                        }
                     }
                     // This may be fixed now, the 'if (target == null)' was incorrectly 'if (target != null)', which
                     // would have caused tooFar to always be set to true, when we have a target:
                     //// TODO: Till I figure out how to move better, don't consider the tooFar flag,
                     //// because it causes the AI to move 0 hexes
                     tooFar = false;
                  }
                  if (currentSpell instanceof IAreaSpell) {
                     IAreaSpell areaSpell = (IAreaSpell) currentSpell;
                     if (target == null) {
                        tooFar = true;
                     }
                     else {
                        int maxDist = areaSpell.getRadiusOfAffect() + currentSpell.getMaxRange(_self);
                        short curDist = ArenaCoordinates.getDistance(_self.getHeadCoordinates(), target.getHeadCoordinates());
                        tooFar = curDist > maxDist;
                     }
                  }
                  if (tooFar) {
                     traceSb.append(", tooFar");
                     // we could be too far, or too close. If we are too far, move in
                     if (curMinDist > 2) {
                        traceSb.append(", movein");
                        priorities.add(new RequestActionOption("", RequestActionType.OPT_MOVE, LimbType.BODY, true));
                     }
                  }
                  else {
                     traceSb.append(", cast3");
                     // Always cast spells with 3 actions
                     priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_SPELL_3, LimbType.BODY, true));
                     // except innate spells can be cast at 2 actions, if we don't have 3:
                     if (currentSpell.isInate()) {
                        traceSb.append(", cast2");
                        priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_SPELL_2, LimbType.BODY, true));
                     }
                  }
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true));
               }
            }
            else if (currentSpell instanceof PriestSpell) {
               // don't cast a priest spell if we are in much pain
               traceSb.append(", priestSpell");
               PriestSpell priestSpell = (PriestSpell) currentSpell;
               RANGE range = priestSpell.getRange(curMinDist);
               Advantage divinePower = _self.getAdvantage(Advantage.DIVINE_POWER);
               int minimumCastingPower = 1;
               if (divinePower != null) {
                  switch (divinePower.getLevel()) {
                     case 0: DebugBreak.debugBreak("Divinge power of 0");
                     case 1: minimumCastingPower = 1; break;
                     case 2: minimumCastingPower = 1; break;
                     case 3: minimumCastingPower = 2; break;
                     case 4: minimumCastingPower = 3; break;
                     case 5: minimumCastingPower = 3; break;
                  }
               }
               minimumCastingPower += currentPain;
               if (priestSpell.requiresTargetToCast() || (priestSpell instanceof SpellMassFear)) {
                  // if this spell is beneficial, we must be castin on ourwselves (or an ally),
                  // so our distance to the target is irrelevant:
                  if (!priestSpell.isBeneficial()) {
                     minimumCastingPower += priestSpell.getPowerReductionForRange(curMinDist, range);
                  }
               }
               // Always cast priest spells at max power:
               if (minimumCastingPower <= 5) {
                  priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_PRIEST_SPELL_5, LimbType.BODY, true));
                  if (minimumCastingPower <= 4) {
                     priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_PRIEST_SPELL_4, LimbType.BODY, true));
                     if (minimumCastingPower <= 3) {
                        priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_PRIEST_SPELL_3, LimbType.BODY, true));
                        if (minimumCastingPower <= 2) {
                           priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_PRIEST_SPELL_2, LimbType.BODY, true));
                           if (minimumCastingPower <= 1) {
                              priorities.add(new RequestActionOption("", RequestActionType.OPT_COMPLETE_PRIEST_SPELL_1, LimbType.BODY, true));
                           }
                        }
                     }
                  }
               }
               // If we still have enough power to cast this spell, don't lose it by attacking:
               if (_self.getCondition().getPriestSpellPointsAvailable() >= currentSpell.getLevel()) {
                  if (!currentSpell.isInate()) {
                     // never attack when we have a non-inate priest spell ready:
                     attackActions = 0;
                  }
               }
            }
         }
      }
      else {
         boolean considerCastingTime = (attackActions < 2);
         if (attackActions == 2) {
            considerCastingTime = (CombatServer.random() > .5);
         }
         traceSb.append(", considerCastingTime=").append(considerCastingTime);

         List<Spell> inateSpells = _self.getRace().getInateSpells();
         for (int inateSpellIndex = 0; inateSpellIndex < inateSpells.size(); inateSpellIndex++) {
            if ((inateSpells != null) && (inateSpells.size() > 0)) {
               Spell inateSpell = inateSpells.get(0);
               double useOdds = .8;
               if (inateSpell.isDefendable()) {
                  int maxRange = inateSpell.getMaxRange(_self);
                  if (curMinDist > maxRange) {
                     useOdds = .10;
                  }
                  else {
                     if ((target.getWeapon() != null) && (target.getWeapon().getAttackStyle(0).getMaxRange() == 2)) {
                        // characters that can attack at a range of 2 can be shot even when they approach.
                        if (curMinDist == 2) {
                           useOdds = .33;
                        }
                        else {
                           useOdds = .6;
                        }
                     }
                     else {
                        if (curMinDist > 6) {
                           // they are still far enough way to use the missile spell
                           useOdds = .8;
                        }
                        else {
                           // If they are very close, don't use the inate missile spell
                           useOdds = .1;
                        }
                     }
                  }
                  // If our target is held in a spell like spiderweb,
                  // don't use another identical spell on them.
                  if (inateSpell instanceof IHolder) {
                     if (target != null) {
                        for (IHolder holder : target.getHolders()) {
                           if (holder.getClass() == inateSpell.getClass()) {
                              useOdds = 0;
                           }
                        }
                     }
                  }
               }
               else {
                  if (target != null) {
                     byte effectiveDistance = (byte) (curMinDist - target.getMovementRate() - _self.getMovementRate());
                     if (effectiveDistance < 1) {
                        effectiveDistance = 1;
                     }
                     useOdds = getChanceOfSpellSuccess(inateSpell, target, myActionsThisRound, effectiveDistance, currentPain);
                     for (Spell curSpell : target.getActiveSpells()) {
                        if (curSpell.getName() == inateSpell.getName()) {
                           useOdds = 0.0;
                           break;
                        }
                     }
                  }
               }
               traceSb.append(", inateSpell=").append(inateSpell.getName());
               traceSb.append(", useOdds=").append(useOdds);
               if (CombatServer.random() < useOdds) {
                  traceSb.append(", prepare");
                  switch (inateSpellIndex) {
                     case 0: priorities.add(new RequestActionOption("", RequestActionType.OPT_PREPARE_INITATE_SPELL_1, LimbType.BODY, true)); break;
                     case 1: priorities.add(new RequestActionOption("", RequestActionType.OPT_PREPARE_INITATE_SPELL_2, LimbType.BODY, true)); break;
                     case 2: priorities.add(new RequestActionOption("", RequestActionType.OPT_PREPARE_INITATE_SPELL_3, LimbType.BODY, true)); break;
                     case 3: priorities.add(new RequestActionOption("", RequestActionType.OPT_PREPARE_INITATE_SPELL_4, LimbType.BODY, true)); break;
                     case 4: priorities.add(new RequestActionOption("", RequestActionType.OPT_PREPARE_INITATE_SPELL_5, LimbType.BODY, true)); break;
                  }
               }
            }
         }
         Spell spell = selectSpellToCast(display, considerCastingTime, map, target);
         if (spell != null) {
            traceSb.append(", castSpell=").append(spell.getName());
            priorities.add(new RequestActionOption("", RequestActionType.OPT_BEGIN_SPELL, LimbType.BODY, true));
         }
      }

      // any time a charge option is available, use it - unless we are not attacking at all
      if (attackActions > 0) {
         priorities.add(new RequestActionOption("", RequestActionType.OPT_CHARGE_ATTACK_3, LimbType.BODY, true));
         priorities.add(new RequestActionOption("", RequestActionType.OPT_CHARGE_ATTACK_2, LimbType.BODY, true));
         priorities.add(new RequestActionOption("", RequestActionType.OPT_CHARGE_ATTACK_1, LimbType.BODY, true));
      }
      // if we prefer attacking with a charge, don't attack when we are close
      if (getDesiredDistance(target, false/*defensive*/) == CHARGE_DISTANCE) {
         // verify we are allow to charge, and not matching on the case us a large weapon (such as a halberd)
         // that has a desired range that happens to match the CHARGE_DISTANCE of 4:
         if ((myWeapon != null) && (myWeapon._attackStyles.length == 1)) {
            if (myWeapon._attackStyles[0].canCharge(_self.isMounted(), _self.getLegCount() > 3)) {
               if (!myWeapon.getName().equals(Weapon.NAME_Fangs)) {
                  // consider our expected damage.
                  byte maxDamage = myWeapon.getWeaponMaxDamage(this._self);
                  byte targetBuild = target.getBuild(myWeapon.getAttackStyle(0).getDamageType());
                  byte expectedDamage = (byte) (maxDamage - targetBuild);
                  double chance = .6;
                  // as our basic expected damage goes down, we need to increase our chance of using the charge
                  if ( expectedDamage < -5) {
                     chance = 0.8;
                     if ( expectedDamage < -10) {
                        chance = 1.0;
                     }
                  }
                  if (CombatServer.random() < chance) {
                     traceSb.append(", prefer charge");
                     attackActions = 0;
                  }
               }
            }
         }
      }

      if ((myWeapon != null) && (target != null) && (!myWeaponIsMissile) && (!myWeapon.isOnlyThrowable())) {
         int minDistance = myWeapon.getWeaponMinRange(false/*allowRanged*/, false/*onlyChargeTypes*/, _self);
         if (curMaxDist < minDistance) {
            Arena arena = CombatServer._this.getArena();
            if (arena.canRetreat(_self, arena.getLocation(target.getHeadCoordinates()))) {
               // backup so we can attack with our weapon
               priorities.add(new RequestActionOption("", RequestActionType.OPT_MOVE, LimbType.BODY, true));
            }
         }
      }
      switch (attackActions) {
         case 7:
         case 6:
         case 5:
         case 4:
            priorities.add(new RequestActionOption("", RequestActionType.OPT_CLOSE_AND_ATTACK_3, LimbType.BODY, true));
            priorities.add(new RequestActionOption("", RequestActionType.OPT_CLOSE_AND_GRAPPLE_3, LimbType.BODY, true));
         case 3:
            if (canThrow) {
               addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_THROW_3);
            }
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_MELEE_3);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_CLOSE_AND_ATTACK_2);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_GRAPPLE_3);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_CLOSE_AND_GRAPPLE_2);
         case 2:
            if (canThrow) {
               addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_THROW_2);
            }
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_MELEE_2);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_CLOSE_AND_ATTACK_1);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_GRAPPLE_2);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_CLOSE_AND_GRAPPLE_1);
         case 1:
            if (canThrow) {
               addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_THROW_1);
            }
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_MELEE_1);
            addOptionForAllHands(priorities, myWeapon, RequestActionType.OPT_ATTACK_GRAPPLE_1);
         case 0:
      }
      boolean allowAdvance = true;
      boolean targetHasMissileWeapon = false;
      // If we have missile weapon, let the enemy come to us.
      if (target != null) {
         Weapon targetsWeapon = target.getWeapon();
         targetHasMissileWeapon = (targetsWeapon != null) && (targetsWeapon.isMissileWeapon());
      }
      if ((myWeapon != null) && myWeaponIsMissile && !targetHasMissileWeapon) {
         // We have a missile weapon, and the target does not. Let them come to us
         // TODO: should we go looking for the enemy?
         // TODO: perhaps create a new AI type: sentry, and have sentries stay, while all others go scouting
         //if (_aiType == AI_Type.STATIONARY)
         {
            traceSb.append(", letClose");
            allowAdvance = false;
         }
      }
      if (allowAdvance) {
         // could we walk into the enemies range?
         if (target != null) {
            // If any movement would put us in range
            if ((curMinDist - 1) <= targetMaxRange) {
               // but are not currently in range
               if (curMinDist > targetMaxRange) {
                  // If we have only one or two actions left, don't walk into an enemy's range,
                  // unless we have more actions available than he does.
                  if (myActionsThisRound <= 2) {
                     if (targetActionsThisRound > myActionsThisRound) {
                        traceSb.append(", notEnoughActionsToClose");
                        allowAdvance = false;
                     }
                  }
                  else {
                     // If we do have 3 actions left, but so does the enemy,
                     // don't walk into his/her range
                     if (targetActionsThisRound >= 3) {
                        traceSb.append(", targetHas3Actions-DontClose");
                        allowAdvance = false;
                     }
                  }
                  // If the enemy is right next to us, but we are not facing him, turn to face
                  if ((curMinDist == 1) && !allowAdvance) {
                     traceSb.append(", close");
                     if (!map.isFacing(_self, target)) {
                        traceSb.append(", turnToFace");
                        allowAdvance = true;
                     }
                  }
               }
            }
         }
      }

      if (allowAdvance) {
         // Make sure that if we say we should move, that we have a place we actually want to move to.
         boolean allowRanged = canThrow || myWeaponIsMissile;
         traceSb.append(", advAllowed");
         if (shouldMove(map, display, target, true/*allowWander*/, allowRanged)) {
            traceSb.append(", move");
            if (!beingAimedAtBy.isEmpty()) {
               for (Character attacker : beingAimedAtBy) {
                  if (attacker.getAimDuration(_self._uniqueID) > 1) {
                     // if we're being targeted, look for a spot to move to to hide from the attacker.
                     if (findPathToHideFromAttacker(null, map) == null) {
                        // If we couldn't find a spot to move to that's hiding from our attacker, move evasively:
                        priorities.add(new RequestActionOption("", RequestActionType.OPT_MOVE_EVASIVE, LimbType.BODY, true));
                     }
                     // Weather or not we found a place to hide, there is no point looking through more of the list of attackers.
                     break;
                  }
               }
            }
            priorities.add(new RequestActionOption("", RequestActionType.OPT_MOVE, LimbType.BODY, true));
         }
      }
      // If we are being held, prefer to break out than any final defensive action:
      priorities.add(new RequestActionOption("", RequestActionType.OPT_BREAK_FREE_3, LimbType.BODY, true));
      priorities.add(new RequestActionOption("", RequestActionType.OPT_BREAK_FREE_2, LimbType.BODY, true));
      priorities.add(new RequestActionOption("", RequestActionType.OPT_BREAK_FREE_1, LimbType.BODY, true));

      priorities.add(new RequestActionOption("", RequestActionType.OPT_WAIT_TO_ATTACK, LimbType.BODY, true));

      // before we go into final defense mode, or go on gaurd, if we have a spell that needs to be maintained, maintain it
      if (currentSpell != null) {
         if ((currentSpell instanceof MageSpell) ||
              (_self.getCondition().getPriestSpellPointsAvailable() < currentSpell.getLevel())) {
            priorities.add(new RequestActionOption("", RequestActionType.OPT_MAINTAIN_SPELL, LimbType.BODY, true));
         }
      }

      priorities.add(new RequestActionOption("", RequestActionType.OPT_FINAL_DEFENSE_3, LimbType.BODY, true));
      priorities.add(new RequestActionOption("", RequestActionType.OPT_FINAL_DEFENSE_2, LimbType.BODY, true));
      priorities.add(new RequestActionOption("", RequestActionType.OPT_FINAL_DEFENSE_1, LimbType.BODY, true));
      if (curMinDist > tooCloseForComfort) {
         priorities.add(new RequestActionOption("", RequestActionType.OPT_TARGET_ENEMY, LimbType.BODY, true));
      }

      priorities.add(new RequestActionOption("", RequestActionType.OPT_ON_GAURD, LimbType.BODY, true));
      if (_self.isBerserking()) {
         priorities.add(new RequestActionOption("", RequestActionType.OPT_ATTACK_MELEE_3, LimbType.BODY, true));
         priorities.add(new RequestActionOption("", RequestActionType.OPT_ATTACK_MELEE_2, LimbType.BODY, true));
         priorities.add(new RequestActionOption("", RequestActionType.OPT_ATTACK_MELEE_1, LimbType.BODY, true));
      }
      Rules.diag(traceSb.toString());
      return selectAnswerByType(action, priorities);
   }

   private void determineWeaponToPickup(RequestAction action, CombatMap map, StringBuilder traceSb, Weapon myWeapon, short curDist, int tooCloseForComfort,
                                        List<RequestActionOption> priorities) {
      // Don't pick up a weapon if we can use it!
      Limb rightArm = _self.getLimb(LimbType.HAND_RIGHT);
      Limb leftArm = _self.getLimb(LimbType.HAND_LEFT);
      boolean rightArmEmpty = (rightArm != null) && (!rightArm.isCrippled()) && (rightArm.getHeldThing() == null);
      boolean leftArmEmpty = (leftArm != null) && (!leftArm.isCrippled()) && (leftArm.getHeldThing() == null);
      if (rightArmEmpty || leftArmEmpty) {
         // only pick up the weapon if we are better at using it than
         // our current weapon.
         for (int i = 0; i < action.getActionCount(); i++) {
            RequestActionOption answerActOpt = action.getReqOptions()[i];
            if (answerActOpt.getValue().isLocationAction()) {
               String itemName = map.getPickupItemName(_self, action, answerActOpt.getValue().getIndexOfLocationAction());
               if ((itemName == null) || (itemName.length() == 0)) {
                  continue;
               }
               traceSb.append(", floorItem:").append(itemName);
               Thing thing = Thing.getThing(itemName, null);
               if (thing instanceof Weapon) {
                  Weapon floorWeapon = (Weapon) thing;
                  if ((floorWeapon != null) && (floorWeapon.isReal())) {
                     byte floorWeaponSkillLevel = _self.getBestSkillLevel(floorWeapon);
                     if (floorWeaponSkillLevel > 0) {
                        byte myWeaponSkillLevel = (myWeapon == null) ? 0 : _self.getBestSkillLevel(myWeapon);
                        // If we've got some distance, weight more heavily toward the missile weapon on the floor:
                        if ((curDist > (2*tooCloseForComfort)) && (floorWeapon.isMissileWeapon())) {
                           traceSb.append(", floorWeaponIsMissile, faraway");
                           floorWeaponSkillLevel += 2;
                        }

                        // unarmed weapons generally do less damage, so prefer real weapons if our skill levels are close:
                        if ((myWeapon == null) || myWeapon.isUnarmedStyle()) {
                           traceSb.append(", myWeaponUnarmedStyle");
                           myWeaponSkillLevel -= 2;
                        }

                        if (floorWeaponSkillLevel > myWeaponSkillLevel) {
                           // Don't pickup a two-handed weapon if either arm is crippled.
                           if ((floorWeapon.isOnlyTwoHanded()) && (!rightArmEmpty || !leftArmEmpty)) {
                              traceSb.append(", floorWeapIs2Handed-leftCrippled");
                              continue;
                           }
                           // Don't pickup a missile weapon if our enemies are too close to use it
                           if (floorWeapon.isMissileWeapon()) {
                              traceSb.append(", floorWeapIsMissile");
                              if (curDist <= tooCloseForComfort) {
                                 traceSb.append(" - enemy is too close");
                                 continue;
                              }
                           }
                           traceSb.append(", pickupWeap");
                           priorities.add(answerActOpt);
                        }
                     }
                  }
               }
               else if (thing instanceof Potion) {
                  Potion potion = (Potion) thing;
                  if (potion.isBeneficial()) {
                     boolean pickupPotion = false;
                     if (potion.isHealing()) {
                        if (_self.getWounds() > 0) {
                           traceSb.append(", pickupHealPotion");
                           pickupPotion = true;
                        }
                     }
                     else {
                        traceSb.append(", pickupBeneficialPotion");
                        pickupPotion = true;
                     }
                     if (pickupPotion) {
                        // TODO: drop something in our current hand, so we can pick up this potion
                        //       But to do that, I need to modify the RequestEquipment method
                        priorities.add(answerActOpt);
                     }
                  }
               }
            }
         }
         // Consider picking up a shield on the floor
         // only pick up the shield if:
         //    1) we know how to use it
         //    2) we are not already holding one
         //    3) we are not using a two-handed weapon
         //    4) our right hand is not crippled
         byte shieldLevel = _self.getSkillLevel(SkillType.Shield, null/*useLimb*/, false, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
         // Do we know how to use a shield?
         if (shieldLevel > 0) {
            // Is our left arm able to carry a shield?
            if ((leftArm != null) && (!leftArm.isCrippled())) {
               // Is our right hand able to carry a weapon?
               if ((rightArm != null) && (!rightArm.isCrippled())) {
                  Thing heldThing = leftArm.getHeldThing();
                  // Are we already carrying one?
                  if (heldThing == null) {
                     // Are we carrying a two-handed weapon?
                     if ((myWeapon == null) || (!myWeapon.isOnlyTwoHanded())) {
                        // Is one available to pick up?
                        for (int i = 0; i < action.getActionCount(); i++) {
                           IRequestOption answer = action.getReqOptions()[i];
                           if (answer instanceof RequestActionOption) {
                              RequestActionOption answerActOpt = (RequestActionOption) answer;
                              if (answerActOpt.getValue().isLocationAction()) {
                                 int index = answerActOpt.getValue().getIndexOfLocationAction();
                                 String itemName = map.getPickupItemName(_self, action, index);
                                 Shield floorShield = Shield.getShield(itemName, null);
                                 if ((floorShield != null) && (floorShield.isReal())) {
                                    traceSb.append(", pickupShield");
                                    priorities.add(answerActOpt);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private double getChanceOfSpellSuccess(Spell spell, Character target, byte actionsUsed, short curDist, byte currentPain) {
      if (spell.getCaster() == null) {
         spell.setCaster(_self);
      }
      if (target.isUnderSpell(spell.getName()) != null) {
         return 0;
      }
      if (spell.getActiveSpellIncompatibleWith(target) != null) {
         return 0;
      }

      byte castingTN = spell.getTN(_self);
      byte defenseTN = 0;
      RANGE range = spell.getRange(curDist);
      if (range == RANGE.OUT_OF_RANGE) {
         if (spell instanceof IRangedSpell) {
            return 0;
         }
      }
      DiceSet castingDice = spell.getCastDice(actionsUsed, range);

      if (spell.isDefendable()) {
         defenseTN = target.getPassiveDefense(range, false/*isGrappleAttack*/, curDist);
         byte defenseActions = target.getActionsAvailableThisRound(true/*usedForDefenseOnly*/);
         if (defenseActions > 1) {
            byte minimumDamage = 0;
            if (defenseActions > 1) {
               // best defense we can offer is a 2-actions: dodge & block
               DefenseOptions defOpts = new DefenseOptions(DefenseOption.DEF_DODGE, DefenseOption.DEF_LEFT);
               defenseTN = target.getDefenseOptionTN(defOpts, minimumDamage , true/*includeWoundPenalty*/,
                                                     true/*includeHolds*/, true/*includePosition*/,
                                                     true/*includeMassiveDamagePenalty*/,
                                                     (byte) 0/*attackingWeaponsParryPenalty*/,
                                                     spell instanceof IRangedSpell/*isRangedAttack*/,
                                                     curDist, false/*isChargeAttack*/,
                                                     false/*isGrappleAttack*/, spell.getDamageType(),
                                                     false/*defenseAppliedAlready*/, range);
               // TODO: what about magic defenses?
            }
            else {
               DefenseOptions defOpts = new DefenseOptions(DefenseOption.DEF_DODGE);
               byte dodgeTN = target.getDefenseOptionTN(defOpts, minimumDamage , true/*includeWoundPenalty*/,
                                                        true/*includeHolds*/, true/*includePosition*/,
                                                        true/*includeMassiveDamagePenalty*/,
                                                        (byte) 0/*attackingWeaponsParryPenalty*/,
                                                        spell instanceof IRangedSpell/*isRangedAttack*/,
                                                        curDist, false/*isChargeAttack*/,
                                                        false/*isGrappleAttack*/, spell.getDamageType(),
                                                        false/*defenseAppliedAlready*/, range);
               defOpts = new DefenseOptions(DefenseOption.DEF_LEFT);
               byte blockTN = target.getDefenseOptionTN(defOpts, minimumDamage , true/*includeWoundPenalty*/,
                                                        true/*includeHolds*/, true/*includePosition*/,
                                                        true/*includeMassiveDamagePenalty*/,
                                                        (byte) 0/*attackingWeaponsParryPenalty*/,
                                                        spell instanceof IRangedSpell/*isRangedAttack*/,
                                                        curDist, false/*isChargeAttack*/,
                                                        false/*isGrappleAttack*/, spell.getDamageType(),
                                                        false/*defenseAppliedAlready*/, range);
               defenseTN = (byte) Math.max(dodgeTN, blockTN);
            }
            // This is already factored in:
            //defenseTN += Rules.getRangeDefenseAdjustmentPerAction(range) * defenseActions;
         }
      }
      else if (spell instanceof IResistedSpell) {
         IResistedSpell resistedSpell = (IResistedSpell) spell;
         DiceSet resistanceDice = resistedSpell.getResistanceDice(target);
         defenseTN = (byte) Math.round(resistanceDice.getAverageRoll(true/*allowExplode*/));
      }

      defenseTN += spell.getRangeTNAdjustment(curDist);

      if (spell instanceof PriestSpell) {
         PriestSpell priestSpell = (PriestSpell) spell;
         //Advantage divinePower = _self.getAdvantage(Advantage.DIVINE_POWER);
         byte priestPower = priestSpell.getPower();
         priestPower -= priestSpell.getPowerReductionForRange(curDist, range);
         priestPower -= currentPain;
         if (priestPower < 1) {
            return 0;
         }
      }
      byte highestTN = (byte) Math.max(castingTN, defenseTN);
      if (castingDice != null) {
         castingDice = castingDice.addBonus(spell.getLevel());
         return castingDice.getOddsForTN(highestTN);
      }
      return 0;
   }

   public boolean canAndShouldHealSelfWithPotion() {
      if (_self.getWounds() > 3) {
         return true;
      }
      return false;
   }

   private byte desiredPowerForSpell(Spell spell) {
      if (spell instanceof MageSpell) {
         MageSpell mageSpell = (MageSpell) spell;
         // Always cast spells with a power level equal to our MA
         DiceSet dice = Rules.getDice(_self.getAttributeLevel(Attribute.Intelligence), (byte) 3, Attribute.Intelligence);
         for (byte power = 2; power <10 ; power++) {
            byte tnAtPower = mageSpell.getTN(_self, power);
            double successOdds = dice.getOddsForTN(tnAtPower);
            int overPower = power - _self.getMagicalAptitude();

            double permittedOdds = 0.75;
            if (overPower == 0) {
               permittedOdds = 0.85;
            }
            else if (overPower == 1) {
               permittedOdds = 0.91;
            }
            else if (overPower == 2) {
               permittedOdds = 0.95;
            }
            else if (overPower >= 3) {
               permittedOdds = 0.98;
            }
            if (successOdds < permittedOdds) {
               // If this power is below the permitted odds, use the power level before this one.
               return (byte) (power - 1);
            }
         }
      }
      return 1;
   }

   private void addOptionForAllHands(List<RequestActionOption> priorities, Weapon weapon, RequestActionType option) {
      // in some cases, our best weapon may be on our head, not our hands (case: Giant Spider)
      if (weapon != null) {
         for (int set=1 ; set<=3 ; set++) {
            for (Limb limb : _self._limbs.values()) {
               if (limb._limbType.setId == set) {
                  if (!limb.isCrippled()) {
                     Weapon weap = limb.getWeapon(_self);
                     if (weapon.equals(weap)) {
                        priorities.add(new RequestActionOption("", option, limb._limbType, true));
                     }
                  }
               }
            }
         }
      }
      priorities.add(new RequestActionOption("", option, LimbType.HEAD, true));
      priorities.add(new RequestActionOption("", option, LimbType.TAIL, true));
   }

   private Weapon getBestAltWeapon(Weapon currentWeapon) {
      for (Thing thing : _self.getEquipment()) {
         if (thing instanceof Weapon) {
            Weapon altWeapon = (Weapon) thing;
            // Allow the throwing away of this weapon
            // only so long as the next available weapon
            // has at least as much skill.
            if (_self.getBestSkillLevel(altWeapon) >= _self.getBestSkillLevel(currentWeapon)) {
               return altWeapon;
            }
         }
      }
      return null;
   }


   private Weapon getMyWeapon() {
      Weapon bestWeapon = null;
      int bestWeaponScore = -1;
      for (Limb limb : _self._limbs.values()) {
         if (limb != null) {
            if (!limb.isCrippled()) {
               Weapon weap = limb.getWeapon(_self);
               if (weap != null) {
                  int minSkillToAttack = weap.getMinSkillToAttack();
                  byte weaponSkillLevel = _self.getBestSkillLevel(weap);
                  if (minSkillToAttack > 0) {
                     if (weaponSkillLevel < minSkillToAttack) {
                        continue;
                     }
                  }
                  int weaponScore = weaponSkillLevel + weap.getWeaponMaxDamage(this._self);
                  if (weap.isMissileWeapon() || weap.isThrowable()) {
                     weaponScore += 5;
                  }

                  if ((bestWeapon == null) || (weaponScore > bestWeaponScore)) {
                     bestWeapon = weap;
                     bestWeaponScore = weaponScore;
                  }
               }
            }
         }
      }
      return bestWeapon;
   }

   private boolean requestAttackStyle(RequestAttackStyle styleReq) {
      Character target = getCharacter(styleReq._targetID);
      int bestAttack = -1;
      byte bestDamage = -100;
      Limb limb = _self.getLimb(styleReq._limbType);
      Weapon myWeapon = limb.getWeapon(_self);
      // First look for non-throwing options
      boolean[] enabled = styleReq.getEnableds();
      for (int style = 0; style < enabled.length; style++) {
         if (enabled[style] && (styleReq.getOptionIDs()[style] != SyncRequest.OPT_CANCEL_ACTION)) {
            WeaponStyleAttack attackStyle = myWeapon.getAttackStyle(style);
            if (attackStyle != null) {
               byte styleDamage = attackStyle.getDamageMod();
               DamageType styleDamType = attackStyle.getDamageType();
               byte styleTime = attackStyle.getSpeed(_self.getAttributeLevel(Attribute.Strength));
               byte targetResistant = target.getArmor().getBarrier(styleDamType);
               byte skillLevel = _self.getSkillLevel(attackStyle.getSkillType(), null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, true/*adjustForHolds*/);
               styleDamage -= targetResistant;
               // for every action saved, factor in 3 points of damage
               styleDamage -= styleTime * 3;
               // favor our higher skill attacks.
               // actual damage is skill/2, for skill-bonus damage
               // however, since we are more likely to hit with high skill, use a factor of 1 instead of 1/2
               styleDamage += skillLevel;
               if ((bestAttack == -1) || (bestDamage < styleDamage)) {
                  bestAttack = style;
                  bestDamage = styleDamage;
               }
            }
         }
      }
      if (bestAttack != -1) {
         styleReq.setAnswerByOptionIndex(bestAttack);
         return true;
      }
      return false;
   }

   // This method returns true if successfully answered the RequestMovement
   private boolean requestMovement(RequestMovement reqMove, Arena arena, CombatMap map, CharacterDisplay display, Character target, boolean allowRanged) {
      if (!requestMovement2(reqMove, map, display, target, true/*allowWander*/, allowRanged)) {
         // If requestMovement2 returned false, stand still:
         Orientation selfOrientation = _self.getOrientation();
         reqMove.setOrientation(selfOrientation);
         if (!_self.hasMovedThisRound()) {
            // This should not happen:
            // Try clearing out our path cache, and forcing a recomputation of a path to our enemy.
            _pathCache = null;
            if (!requestMovement2(reqMove, map, display, target, true/*allowWander*/, allowRanged)) {
               DebugBreak.debugBreak(_self.getName() + ": AI.requestMovement2 return false, indicating stand still, so why did we ask to move?");
               // If we are stepping to see why this happend, first consider if the requestMovement2 returned
               // true when it was called with a 'null' RequestMovement:
               boolean results = requestMovement2(null, map, display, target, true/*allowWander*/, allowRanged);
               if (results || true) {
                  requestMovement2(reqMove, map, display, target, true/*allowWander*/, allowRanged);
               }
            }
         }
      }
      else {
         if (!_self.hasMovedThisRound()) {
            if (_self.getOrientation().equals(reqMove.getAnswerOrientation(false/*removeEntry*/))) {
               // This should not happen:
               // If we get here, it means the requestMovement2 said we should move when reqMove was null,
               // but returned false when reqMove was non-null.
               // Perhaps we wanted to back up, but there is no place to back up to?
               if (target != null) {
                  if (arena.canRetreat(_self, arena.getLocation(target.getHeadCoordinates()))) {
                     DebugBreak.debugBreak(_self.getName() + ": AI.requestMovement2 returned true, saying we should move, but then told us the place to move was where we already were.");
                     requestMovement2(reqMove, map, display, target, true/*allowWander*/, allowRanged);
                  }
               }
            }
         }
      }
      return true;
   }

   private boolean shouldMove(CombatMap map, CharacterDisplay display, Character target, boolean allowWander, boolean allowRanged) {
      // If we are not standing, but we could, then stand first, instead of moving.
      if (!_self.isStanding()) {
         // Check how many actions we have, cause it would take 2 to switch positions
         if (_self.getActionsAvailableThisRound(false) > 1) {
            if (_self.canStand()) {
               return false;
            }
            // If we can't stand, at least go to a kneeling position
            if (_self.getPosition() != Position.KNEELING) {
               return false;
            }
         }
      }
      if (_self.getHolders().size() > 0) {
         return false;
      }
      // If we have a weapon to ready, always ready it before we move.
      // If we don't the getAllRoutesFrom has a hard time finding a place to go,
      // because 'canAttack' will return false even when we are adjacent to the target
      for (LimbType limbType : LimbType._armTypes) {
         Limb limb = _self.getLimb(limbType);
         if ((limb != null) && (!limb.isCrippled())) {
            if (limb.canBeReadied(_self)) {
               return false;
            }
         }
      }
      if (target != null ) {
         if (!_self.hasPeripheralVision() && !map.isFacing(_self, target)) {
            return true;
         }
         if (_self.getOrientation().canAttack(_self, target, map, allowRanged, false/*onlyChargeTypes*/)) {
            Weapon myWeapon = _self.getWeapon();
            // If we have a missile weapon, consider moving to get out of the way of an enemy missile weapon attack
            boolean myWeaponIsMissile  = (myWeapon != null) && myWeapon.isMissileWeapon();
            if (!myWeaponIsMissile) {
               // should we be charging?
               if (getDesiredDistance(target, false/*defensive*/) != CHARGE_DISTANCE) {
                  // if we don't have a charge attack, and we can hit from here, attack (don't move)
                  return false;
               }
               // we do have a charge attack.
               if (Arena.getMaxDistance(_self, target) >= CHARGE_DISTANCE) {
                  // we can charge from here, don't move
                  return false;
               }
            }
            // backup into a good charge range
         }
      }
      if (_self.getMovementRate() == 0) {
         // we can't move (maybe we under the effects of a paralyze spell?), don't try
         return false;
      }
      return requestMovement2(null, map, display, target, allowWander, allowRanged);
   }

   private List<Orientation> findPathToNearbyWeaponToPickUp(CombatMap map, StringBuilder importanceFactorBuffer) {
      if (importanceFactorBuffer != null) {
         importanceFactorBuffer.setLength(0);
      }
      Limb rightHand = _self.getLimb(LimbType.HAND_RIGHT);
      Limb leftHand = _self.getLimb(LimbType.HAND_LEFT);
      int armCount = 0;
      if (rightHand != null) {
         if (rightHand.isCrippled()) {
            rightHand = null;
         }
         else {
            armCount++;
         }
      }
      if (leftHand != null) {
         if (leftHand.isCrippled()) {
            leftHand = null;
         }
         else {
            armCount++;
         }
      }
      // If both hands are crippled, we can't pickup a weapon anyway, so do nothing
      if (armCount == 0) {
         return null;
      }

      Weapon myWeapon = getMyWeapon();
      List<Skill> skills = _self.getSkillsList();
      List<SkillType> skillTypesToUse = new ArrayList<>();
      byte currentSkillLevel = 0;
      if (myWeapon != null) {
         currentSkillLevel = _self.getBestSkillLevel(myWeapon);
      }
      boolean shieldNeeded = false;
      for (Skill skill : skills) {
         if (skill.getLevel() > 0) {
            SkillType skillType = skill.getType();
            if ((skillType == SkillType.BlowGun) || (skillType == SkillType.Bow) || (skillType == SkillType.Crossbow)
                || (skillType == SkillType.Sling) || (skillType == SkillType.StaffSling) || (skillType == SkillType.Throwing)
                || (skillType == SkillType.Aikido) || (skillType == SkillType.Boxing) || (skillType == SkillType.Brawling)
                || (skillType == SkillType.Wrestling) || (skillType == SkillType.Karate)) {
               continue;
            }
            if (skillType == SkillType.Shield) {
               if (armCount > 1) {
                  if (leftHand.getHeldThing() == null) {
                     Thing righthandThing = rightHand.getHeldThing();
                     if ((!(righthandThing instanceof Weapon)) || !((Weapon) righthandThing).isOnlyTwoHanded()) {
                        skillTypesToUse.add(skillType);
                        shieldNeeded = true;
                     }
                  }
               }
            }
            else {
               // ignore skills that require two hands, when we don't have both
               if ((armCount >= 2) || (skill.getArmUseCount() != Skill.ArmsUsed.Both)) {
                  int skillDiff = skill.getLevel() - currentSkillLevel;
                  if (skillDiff > 2) {
                     skillTypesToUse.add(skillType);
                  }
               }
            }
         }
      }
      if (skillTypesToUse.isEmpty()) {
         // If we already have a weapon that we have at (close to) our highest skill level,
         // use it, and don't go looking for other weapons or shields.
         return null;
      }
      Rules.diag(_self.getName() + " wants to find an item that uses one of these skills:" + skillTypesToUse);

      Map<Orientation, Orientation> bestFromMap = new HashMap<>();
      Orientation startOrientation = _self.getOrientation();
      Orientation destinationOrientation = Arena.getAllRoutesFrom(startOrientation, map, 10/*maxMovement*/, _self.getMovementRate(),
                                                                  !_self.hasMovedThisRound(), _self, bestFromMap, null/*target*/, null/*toLoc*/,
                                                                  false/*allowRanged*/, false/*onlyChargeTypes*/, skillTypesToUse/*itemsToPickupUsingSkill*/,
                                                                  (_aiType == AI_Type.GOD)/*considerUnknownLocations*/);
      List<Orientation> route = convertBestFromMapToBestRouteTo(startOrientation, bestFromMap, destinationOrientation);
      if (route != null) {
         int routeLength = route.size();
         if (routeLength > 0) {
            Orientation finalDest = route.get(routeLength - 1);
            int maxWeaponSkillDiff = 0;
            for (ArenaCoordinates coor : finalDest.getCoordinates()) {
               ArenaLocation loc = map.getLocation(coor);
               synchronized (loc) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(loc._lock_this))
                  {
                     for (Object obj : loc.getThings()) {
                        if (obj instanceof Weapon) {
                           Weapon weap = (Weapon) obj;
                           int weaponSkillDiff = _self.getBestSkillLevel(weap) - currentSkillLevel;
                           if (weaponSkillDiff > maxWeaponSkillDiff) {
                              maxWeaponSkillDiff = weaponSkillDiff;
                              Rules.diag(_self.getName() + " sees a " + weap.getName() + " at " + loc._x + ", " + loc._y);

                           }
                        }
                        if (obj instanceof Shield) {
                           if (shieldNeeded) {
                              int weaponSkillDiff = _self.getSkillLevel(SkillType.Shield, null/*useLimb*/, false/*sizeAdj*/, true/*adjustForEncumbrance*/,
                                                                        true/*adjustForHolds*/);
                              if (weaponSkillDiff > maxWeaponSkillDiff) {
                                 maxWeaponSkillDiff = weaponSkillDiff;
                                 Rules.diag(_self.getName() + " sees a " + ((Shield) obj).getName() + " at " + loc._x + ", " + loc._y);
                              }
                           }
                        }
                     }
                  }
               }
            }
            if (importanceFactorBuffer != null) {
               // if our current weapon is a punch, kick or headbutt, double the weight of the found weapon:
               if ((myWeapon == null) || (!myWeapon.isReal())) {
                  maxWeaponSkillDiff *= 2;
               }
               importanceFactorBuffer.append(maxWeaponSkillDiff);
            }
         }
      }
      return route;
   }

   private Orientation findPathToHideFromAttacker(RequestMovement reqMove, CombatMap map) {
      // animals are not smart enough to hide from missile weapons
      if (_self.isAnimal()) {
         return null;
      }
      // Berserkers can not hide from their targets
      if (_self.isBerserking()) {
         return null;
      }
      // If this is true, then the character will always back up one hex, because it will think
      // it can hide behind it own current position.
      boolean blockedByAnyStandingCharacter = false;
      List<Orientation> possibleOrientations;
      if (reqMove != null) {
         possibleOrientations = reqMove.getOrientations();
      }
      else {
         possibleOrientations = new ArrayList<>();
         Map<Orientation, Orientation> mapOfFutureOrientToSourceOrient = new HashMap<>();
         Arena arena = CombatServer._this.getArena();
         arena.getMoveableLocations(_self, _self.getMovementRate(), arena.getCombatMap(), possibleOrientations, mapOfFutureOrientToSourceOrient);
      }
      for (Character attacker : map.getCombatants()) {
         if (attacker.getAimDuration(_self._uniqueID) > 0) {
            for (Orientation orientation : possibleOrientations) {
               if (!map.couldSee(attacker.getOrientation(), orientation, true/*considerFacing*/, blockedByAnyStandingCharacter)) {
                  _locationBeforeHidingFromAttacker = _self.getOrientation();
                  return orientation;
               }
            }
         }
      }
      return null;
   }

   // This method return true if we should actually move, false if we should remain still.
   private boolean requestMovement2(RequestMovement reqMove, CombatMap map, CharacterDisplay display, Character target, boolean allowWander, boolean allowRanged) {
      Orientation desiredOrientation = null;
      // First check to see if we need to look for any available weapons that we could use.
      StringBuilder importanceFactorBuilder = new StringBuilder();
      List<Orientation> routeToWeapon = findPathToNearbyWeaponToPickUp(map, importanceFactorBuilder);
      boolean retreiveWeapon = false;
      if ((routeToWeapon != null) && (routeToWeapon.size() > 0)) {
         int importanceFactor = Integer.parseInt(importanceFactorBuilder.toString());
         if (target == null) {
            // If we don't have an enemy target, go get the weapon
            retreiveWeapon = true;
         }
         // TODO: factor in how close we are to the enemy, and how close we are to the better weapon.
         else if (importanceFactor >= 4) {
            // Even if we have an enemy targeted, go get this better weapon.
            retreiveWeapon = true;
         }
         if (retreiveWeapon) {
            desiredOrientation = routeToWeapon.get(0);
            if (desiredOrientation.equals(_self.getOrientation())) {
               if ((routeToWeapon.size() == 1)) {
                  // If we are already standing at the weapons location, don't move, just pick it up.
                  return false;
               }
               desiredOrientation = routeToWeapon.get(1);
            }
         }
      }
      if (!retreiveWeapon) {
         Orientation hidingOrientation = findPathToHideFromAttacker(reqMove, map);
         if (hidingOrientation != null) {
            if (reqMove != null) {
               reqMove.setOrientation(hidingOrientation);
            }
            return true;
         }
         short desiredDistance = getDesiredDistance(target, false);
         if (target == null) {
            target = findAllyThatCanSeeAnEnemy(display, map);
            if (target == null) {
               if (allowWander && wander(reqMove, map)) {
                  return true;
               }
               target = findClosestAlly(map);
               if (target == null) {
                  return false;
               }
            }
         }
         Orientation selfOrient = _self.getOrientation();
         Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);
         if (myVisibilityMap == null) {
            // we don't know where anyone is.
            if (allowWander && wander(reqMove, map)) {
               return true;
            }
            return false;
         }
         List<ArenaLocation> targetCoordinates = myVisibilityMap.get(target._uniqueID);
         if (targetCoordinates == null) {
            // we don't know where the target is.
            if (allowWander && wander(reqMove, map)) {
               return true;
            }
            if (_locationBeforeHidingFromAttacker != null) {
               // move back to our pre-hide position
               if ((reqMove != null) && (reqMove.setOrientation(_locationBeforeHidingFromAttacker))) {
                  _locationBeforeHidingFromAttacker = null;
               }
               return true;
            }
            return false;
         }
         int currentMinDistance = Arena.getMinDistance(_self, target);
         // The smarter we are, the more likely we should be able to find a strategic spot behind our enemy:

         Battle battle = CombatServer._this.getArena()._battle;
         Random pseudoRandom = new Random((_self._uniqueID * 10000) + battle.getTimeID() + CombatServer._pseudoRandomNumberSeed);
         double randomForThisCharacterAndTime = pseudoRandom.nextDouble();

         boolean smartMove = (randomForThisCharacterAndTime > ((_self.getAttributeLevel(Attribute.Intelligence) + 3.0) / 10.0)) && !_self.isBerserking();
         if (smartMove || (desiredDistance > currentMinDistance)) {
            if ((reqMove == null) && (desiredDistance > currentMinDistance)) {
               return true;
            }
            // If we can move to an orientation that can attack our target, move there
            if (reqMove != null) {
               List<Orientation> validOrientationList = new ArrayList<>();
               List<Orientation> behindOrientationList = new ArrayList<>();
               // Start out only considering the desired distance, but as we look for possible
               // locations, if we can't get to the desired distance, consider closer and closer
               // distances until we find one.
               int considerDistance = desiredDistance;
               while (validOrientationList.isEmpty() && behindOrientationList.isEmpty() && (considerDistance > 1)) {
                  for (Orientation possibleOrientation : reqMove.getOrientations()) {
                     boolean isFacing = false;
                     for (ArenaLocation targetLocation : targetCoordinates) {
                        if (map.isFacing(possibleOrientation, targetLocation)) {
                           isFacing = true;
                           break;
                        }
                     }
                     if (isFacing) {
                        int distance = Arena.getDistance(possibleOrientation.getHeadCoordinates(), target);
                        // If we need to run away, consider all movement options
                        boolean runAway = ((considerDistance > CHARGE_DISTANCE) && (distance >= considerDistance));
                        if ((distance == considerDistance) || runAway) {
                           // ignore locations that are not a real move from our current location
                           boolean sameLoc = possibleOrientation.getFacing() == _self.getFacing();
                           if (!sameLoc) {
                              sameLoc = possibleOrientation.getHeadCoordinates().equals(_self.getHeadCoordinates());
                           }
                           if (!sameLoc) {
                              boolean behind = !(map.isFacing(target, possibleOrientation.getHeadCoordinates()));
                              if (behind) {
                                 behindOrientationList.add(possibleOrientation);
                              }
                              else {
                                 validOrientationList.add(possibleOrientation);
                              }
                           }
                        }
                     }
                  }
                  considerDistance--;
               }
               List<List<Orientation>> orientationPrefences = new ArrayList<>();
               // prefer to end up in a location that our enemy is not facing:
               orientationPrefences.add(behindOrientationList);
               orientationPrefences.add(validOrientationList);

               List<Orientation> safestOrientationList = new ArrayList<>();
               if (smartMove) {
                  while ((safestOrientationList.size() == 0) && (orientationPrefences.size() > 0)) {
                     safestOrientationList = orientationPrefences.remove(0);
                  }
               }
               else {
                  // if we are not being smart, lump all moves into a single list to choose from:
                  while (orientationPrefences.size() > 0) {
                     safestOrientationList.addAll(orientationPrefences.remove(0));
                  }
               }

               if (safestOrientationList.size() > 0) {
                  // randomly select one of the possible valid orientations
                  int selectedIndex = (int) (safestOrientationList.size() * pseudoRandom.nextDouble());
                  Orientation selectedOrientation = safestOrientationList.get(selectedIndex);
                  // fill in the location into the request, and send it back.
                  if (reqMove.setOrientation(selectedOrientation)) {
                     return true;
                  }
               }
            }
         }
         // If we are not yet facing our target, face him/her now, before we start to move
         boolean isFacing = false;
         for (ArenaLocation targetLocation : targetCoordinates) {
            if (map.isFacing(selfOrient, targetLocation)) {
               isFacing = true;
               break;
            }
         }
         if (!isFacing) {
            if (reqMove == null) {
               return true;
            }
            for (int testMovement = 1; testMovement < 4; testMovement++) {
               List<Orientation> validOrientationList = new ArrayList<>();
               List<Orientation> orientationList = reqMove.getOrientations();
               for (int i = 0; i < orientationList.size(); i++) {
                  Orientation possibleOrientation = orientationList.get(i);
                  if (possibleOrientation.getHeadCoordinates().sameCoordinates(selfOrient.getHeadCoordinates())) {
                     if (selfOrient.getCostToEnter(possibleOrientation, _self, map) == testMovement) {
                        isFacing = false;
                        for (ArenaLocation targetLocation : targetCoordinates) {
                           if (map.isFacing(possibleOrientation, targetLocation)) {
                              isFacing = true;
                              break;
                           }
                        }
                        if (isFacing) {
                           validOrientationList.add(possibleOrientation);
                        }
                     }
                  }
               }
               if (validOrientationList.size() > 0) {
                  // randomly select one of the possible valid orientations
                  int selectedIndex = (int) (validOrientationList.size() * CombatServer.random());
                  Orientation selectedOrientation = validOrientationList.get(selectedIndex);
                  // fill in the location into the request, and send it back.
                  if (reqMove.setOrientation(selectedOrientation)) {
                     return true;
                  }
               }
            }
         }
         List<Orientation> path = findPath(_self, target, map, allowRanged);
         if ((path == null) || path.isEmpty()) {
            if (_self.getOrientation().canAttack(_self, target, map, allowRanged, false/*onlyChargeTypes*/)) {
               // If we don't need to move, we can attack from here.
               return false;
            }
            // perhaps we couldn't attack because we have no weapon:
            Weapon myWeapon = getMyWeapon();
            if (myWeapon == null) {
               // we need to equip a weapon before we move.
               List<Thing> equip = _self.getEquipment();
               for (Thing thing : equip) {
                  if (thing instanceof Weapon) {
                     return false;
                  }
               }
            }
            boolean repeatSearch = false;
// TODO: MOVEMENT ISSUE: Rules.debugBreak();
            if (repeatSearch) {
               path = findPath(_self, target, map, allowRanged);
            }
            // pick a random location to walk to (1 hex away), with preference on moving advancing forward.
            List<Orientation> possibleAdvances = _self.getOrientation().getPossibleAdvanceOrientations(map, true/*blockByCharacters*/);
            possibleAdvances = removeOrientationThatMatch(possibleAdvances, _self.getOrientation());
            if ((possibleAdvances == null) || (possibleAdvances.size() == 0)) {
               // if we can't go forward, consider going backwards:
               possibleAdvances = _self.getOrientation().getPossibleFutureOrientations(map);
               possibleAdvances = removeOrientationThatMatch(possibleAdvances, _self.getOrientation());
               if ((possibleAdvances == null) || (possibleAdvances.size() == 0)) {
                  return false;
               }
            }
            // randomly pick one of the possible orientations
            path = new ArrayList<>();
            path.add(possibleAdvances.get((int) (possibleAdvances.size() * CombatServer.random())));
            Rules.diag(_self.getName() + " is randomly wandering to :" + path.get(0));
         }
         if (path.size() == 0) {
            // If we don't need to move, we can attack from here.
            return false;
         }
         // Face the first destination towards the enemy
         desiredOrientation = path.get(0x0);
         if (_self.getOrientation().equals(desiredOrientation)) {
            if (path.size() == 1) {
               // If we are at the only Orientation in the path,
               // then we don't need to move, we can attack from here.
               return false;
            }
            desiredOrientation = path.get(1);
         }

         short curMinDist = Arena.getMinDistance(_self, target);

         // if the enemy is within 5 hexes of us, don't worry about where our allies are.
         // Also, if we are an animal, don't worry about our allies
         if ((curMinDist > 5) && !_self.isAnimal()) {
            if (isOutInFrontOfAllies(map, target)) {
               // Returns true is there are allies at least 3 hexes behind me, and no one between me and my target.
               // In this case, we should wait for them to catch up to me.
               return false;
            }
         }

         // base our desired distance on self & target's weapon's reaches:
         short curMaxDist = Arena.getMaxDistance(_self, target);
         if (curMinDist == desiredDistance) {
            // we sometimes need to move, without approaching, to get around an obstacle
            short newDist = Arena.getDistance(desiredOrientation.getHeadCoordinates(), target);
            if (newDist < curMinDist) {
               return false;
            }
         }
         if (curMaxDist < desiredDistance) {
            // We need to backup.
            if (reqMove == null) {
               return true;
            }
            // We are trying to backup (or run away.)
            // Look for the point that is closest to our desired distance away from our enemy:
            // By default, unless we find a better location, use our current location as the desired location.
            desiredOrientation = _self.getOrientation();
            short bestDistDelta = (short) Math.abs(desiredDistance - curMaxDist);
            boolean desiredIsFacingTarget = map.isFacing(desiredOrientation, target.getHeadCoordinates());

            // Find the farthest point in path that is an option to move to in the move request:
            for (Orientation validOrientation : reqMove.getOrientations()) {
               short validDist = Arena.getDistance(validOrientation.getHeadCoordinates(), target);
               // How close are we to our desired distance?
               short distDelta = (short) (desiredDistance - validDist);
               if (distDelta < 0) {
                  // this is farther away than we want to be, there must be an answer between us and this location
                  continue;
               }
               boolean validIsFacing = map.isFacing(validOrientation, target.getHeadCoordinates());
               if ((distDelta == 0) && validIsFacing) {
                  // If we found the desired distance, and we are facing the enemy, this is perfect.
                  desiredOrientation = validOrientation;
                  desiredIsFacingTarget = true;
                  break;
               }
               if (distDelta < bestDistDelta) {
                  bestDistDelta = distDelta;
                  desiredOrientation = validOrientation;
                  desiredIsFacingTarget = validIsFacing;
               }
               else if ((distDelta == bestDistDelta) && !desiredIsFacingTarget) {
                  // Both these valid destination are equally far away from the target.
                  // but the current desired is not facing our target. If the new orientation is facing, use it.
                  if (validIsFacing) {
                     desiredOrientation = validOrientation;
                     desiredIsFacingTarget = true;
                  }
               }
            }
         }
         else {
            if (reqMove != null) {
               int attempts = 3;
               // Find the farthest point in path that is an option to move to in the move request:
               for (Orientation pathOrientation : path) {
                  short newDist = Arena.getDistance(pathOrientation.getHeadCoordinates(), target);
                  if ((newDist != curMinDist) && (newDist < desiredDistance)) {
                     // don't consider this move, because it puts us closer than we want to be.
                     break;
                  }
                  boolean pathEntryFound = false;
                  for (Orientation validOrientation : reqMove.getOrientations()) {
                     if (pathOrientation.equals(validOrientation)) {
                        desiredOrientation = validOrientation;
                        pathEntryFound = true;
                        break;
                     }
                  }
                  if (!pathEntryFound) {
                     if (attempts-- <= 0) {
                        // If we couldn't find this entry, we won't be able to find the next one, so stop looking
                        break;
                     }
                  }
               }
            }
         }
      }

      if (desiredOrientation == null) {
         // should we wander around?
         if (!allowWander) {
            // Nobody has seen any enemies. For now, stand still:
            return false;
         }
         // spin in a circle, so we can see what is around us, maybe we'll see an enemy:
         desiredOrientation = _self.getOrientation().clone();
         Facing newFacing = desiredOrientation.getFacing().turn(1);
         desiredOrientation.setHeadLocation(_self, map.getLocation(desiredOrientation.getHeadCoordinates()), newFacing, map, null/*diag*/, false/*allowTwisting*/);
      }

      // ReqLoc can be null if we are just trying to see if we should move.
      // In this case, we want to indicate that yes, we should move
      if (reqMove == null) {
         return true;
      }

      for (Orientation validOrientation : reqMove.getOrientations()) {
         if (desiredOrientation.equals(validOrientation)) {
            // fill in the location into the request, and send it back.
            if (reqMove.setOrientation(validOrientation)) {
               return true;
            }
         }
      }

      // don't move.
      return false;
   }

   private boolean isOutInFrontOfAllies(CombatMap map, Character target) {
      // if we could move forward without engaging the enemy, but we have allies behind us,
      // and no allies in front of us, don't advance - wait for the allies to advance to be with us.
      // ignoring range attacking allies (spellcasters or archers)
      byte facingToEnemy = ArenaCoordinates.getFacingToward(_self.getHeadCoordinates(), target.getHeadCoordinates());
      int alliesInFrontCount = 0;
      int alliesInBackCount = 0;
      int alliesWithMeCount = 0;
      for (Character ally : map.getCombatants()) {
         // ignore fallen combatants
         if (!ally.getCondition().isConscious() || ally.getCondition().isCollapsed()) {
            continue;
         }
         // ignore enemies
         if (ally.isEnemy(_self)) {
            continue;
         }
         // Ignore ourselves
         if (ally._uniqueID == _self._uniqueID) {
            continue;
         }
         // Ignore allies that don't use melee weapons:
         Weapon weap = ally.getWeapon();
         if ((weap != null) && (weap.isMissileWeapon() || weap.isThrowable())) {
            continue;
         }

         // Ignore wizards (but not priests, who must be close to use their magic)
         Advantage magicApptitude = ally.getAdvantage(Advantage.MAGICAL_APTITUDE);
         if ((magicApptitude != null) && (magicApptitude.getLevel() > 1)) {
            continue;
         }

         // Ignore allies that we can't even see.
         if (!map.canSee(_self.getOrientation(), ally, false, false, -1 /*markAsKnownByCharacterUniqueID*/)) {
            continue;
         }

         // Ignore allies that are very close to us
         if (ArenaCoordinates.getDistance(_self.getHeadCoordinates(), ally.getHeadCoordinates()) <3) {
            alliesWithMeCount++;
         }
         else {

            byte facingToAlly = ArenaCoordinates.getFacingToward(_self.getHeadCoordinates(), ally.getHeadCoordinates());
            // If this ally is within 1 facing change of our enemy, he is in front of us.
            if ((facingToEnemy == facingToAlly) ||
                (facingToEnemy == ((facingToAlly+1)%6)) ||
                (facingToEnemy == ((facingToAlly+5)%6))
                ) {
               alliesInFrontCount++;
            }
            else {
               if ((facingToEnemy == ((facingToAlly+3)%6)) ||
                   (facingToEnemy == ((facingToAlly+2)%6)) ||
                   (facingToEnemy == ((facingToAlly+4)%6))
                  ) {
                  alliesInBackCount++;
               }
            }
         }
      }
      // If the majority of allies are at least 2 hexes behind us, return true to indicate we should wait for them instead of advancing more
      return (alliesInBackCount > (alliesInFrontCount + alliesWithMeCount));
   }

   private boolean wander(RequestMovement reqMove, CombatMap map) {
      if (reqMove == null) {
         return true;
      }
      Orientation selectedOrientation = null;
      // prefer to wander forward, if possible
      List<Orientation> possibleMoves = _self.getOrientation().getPossibleAdvanceOrientations(map, true/*blockByCharacters*/);
      // If we couldn't continue forward, consider all moves, including those that mean turning around or backing up
      if ((possibleMoves == null) || (possibleMoves.isEmpty())) {
         possibleMoves = _self.getOrientation().getPossibleFutureOrientations(map);
      }

      // The problem with wandering towards allies, is if the ally moves away,
      // this AI character may hover around the last known spot for too long.
      boolean wanderTowardsAllies = true;
      if (wanderTowardsAllies)
      {
         // Find our closest ally
         Character closestAlly = findClosestAlly(map);
         if (closestAlly !=  null) {

            ArenaCoordinates closestAllyLoc = closestAlly.getHeadCoordinates();
            int allyScore = map.getLOSPath(_self.getHeadCoordinates(), closestAllyLoc, true).size();
            if (allyScore < 5) {
               allyScore = 5;
            }
            if (map.isFacing(_self, closestAlly)) {
               allyScore -= 3;
            }

            List<Orientation> seededMoves = new ArrayList<>();
            // we now have the closest ally that we can see, or at least has a line of sight to us.
            // Favor the orientations that put us closer to this ally.
            for (Orientation move : possibleMoves) {
               int moveScore = map.getLOSPath(move.getHeadCoordinates(), closestAllyLoc, true).size();
               if (moveScore < 5) {
                  moveScore = 5;
               }
               if (map.canSee(move, closestAlly, false, false, -1 /*markAsKnownByCharacterUniqueID*/)) {
                  if (map.isFacing(move, closestAllyLoc)) {
                     moveScore -= 3;
                  }
                  if (moveScore < allyScore) {
                     for (int i=0 ; i< (allyScore - moveScore) ; i++) {
                        seededMoves.add(move);
                     }
                  }
               }
            }
            possibleMoves.addAll(seededMoves);
         }
      }

      // Now just pick a random one of these orientations:
      if ((possibleMoves != null) && (!possibleMoves.isEmpty())) {
         selectedOrientation = possibleMoves.get((int) (CombatServer.random() * possibleMoves.size()));
         if (reqMove.setOrientation(selectedOrientation)) {
            return true;
         }
      }
      return false;
   }

   private static List<Orientation> removeOrientationThatMatch(List<Orientation> possibleAdvances, Orientation orientation) {
      if (possibleAdvances == null) {
         return null;
      }
      List<Orientation> validOrientations = new ArrayList<>();
      for (Orientation possibleAdvance : possibleAdvances) {
         if (!possibleAdvance.getHeadCoordinates().equals(orientation.getHeadCoordinates())) {
            validOrientations.add(possibleAdvance);
         }
      }
      return validOrientations;
   }

   private Character findAllyThatCanSeeAnEnemy(CharacterDisplay display, CombatMap map) {
      // Find all our allies that we can see, and if any of them have targets, move toward the ally
      List<Character> allies = new ArrayList<>();
      List<Character> alliesVisible = new ArrayList<>();
      List<Character> enemies = new ArrayList<>();

      Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);

      Character previousAlly = null;
      for (Character combatant : map.getCombatants()) {
         if (!combatant.isEnemy(_self)) {
            // Ignore ourselves from this list
            if (combatant._uniqueID != _self._uniqueID) {
               // Ignore allies that are only 1 hex away from us, since we can not advance towards them anyway
               if (ArenaCoordinates.getDistance(combatant.getHeadCoordinates(), _self.getHeadCoordinates()) > 1) {
                  allies.add(combatant);
                  // can we see this ally?
                  if (map.canSee(_self, combatant, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
                     alliesVisible.add(combatant);
                     // If we can see this ally, they should be in the visibility map
                     // but sometimes they aren't, so check that now:
                     if (!myVisibilityMap.containsKey(combatant._uniqueID)) {
                        List<ArenaCoordinates> coords = combatant.getCoordinates();
                        List<ArenaLocation> locs = new ArrayList<>();
                        for (ArenaCoordinates coord : coords) {
                           locs.add(map.getLocation(coord));
                        }
                        myVisibilityMap.put(combatant._uniqueID, locs);
                     }
                  }
                  if (combatant._uniqueID == _allyID) {
                     previousAlly = combatant;
                  }
               }
            }
         }
         else {
            enemies.add(combatant);
         }
      }
      // If we can't see anyone, don't bother check who can see who else:
      if (alliesVisible.isEmpty()) {
         StringBuilder sb = new StringBuilder();
         sb.append(_self.getName()).append(" can not see any allies or enemies.");
         if (previousAlly != null) {
            sb.append(" Will move torward previous location of ").append(previousAlly.getName());
         }
         Rules.diag(sb.toString());
         return previousAlly;
      }

      List<Character> alliesThatKnowAboutAnEnemy = new ArrayList<>();
      List<Character> alliesThatDontKnowAboutAnEnemy = new ArrayList<>();
      List<Character> alliesThatLearnedOfAnEnemy = new ArrayList<>();
      // Break the allies down into two groups: those that know where enemies are, and those that don't.
      for (Character ally : allies) {
         // Does this ally know where an enemy is?
         Map<Integer, List<ArenaLocation>> allysVisibilityMap = _mapToMapOfLocations.get(ally._uniqueID);
         if (allysVisibilityMap != null) {
            boolean knowsAboutAnEnemy = false;
            for (Character enemy : enemies) {
               if (allysVisibilityMap.get(enemy._uniqueID) != null) {
                  alliesThatKnowAboutAnEnemy.add(ally);
                  Rules.diag(ally.getName() + " can see enemy " + enemy.getName());
                  knowsAboutAnEnemy = true;
                  break;
               }
            }
            if (!knowsAboutAnEnemy) {
               alliesThatDontKnowAboutAnEnemy.add(ally);
            }
         }
      }
      do {
         for (Character allyThatLearnedOfAnEnemy : alliesThatLearnedOfAnEnemy) {
            alliesThatDontKnowAboutAnEnemy.remove(allyThatLearnedOfAnEnemy);
            alliesThatKnowAboutAnEnemy.add(allyThatLearnedOfAnEnemy);
         }
         alliesThatLearnedOfAnEnemy.clear();

         Character closestAllyWithTarget = null;
         short closestDistance = -1;
         for (Character allyThatKnowsAboutAnEnemy : alliesThatKnowAboutAnEnemy) {
            if (alliesVisible.contains(allyThatKnowsAboutAnEnemy)) {
               if (closestAllyWithTarget == null) {
                  closestAllyWithTarget = allyThatKnowsAboutAnEnemy;
                  closestDistance = Arena.getMinDistance(allyThatKnowsAboutAnEnemy, _self);
               }
               else {
                  short distance = Arena.getMinDistance(allyThatKnowsAboutAnEnemy, _self);
                  if ((closestAllyWithTarget == null) || (distance < closestDistance)) {
                     closestAllyWithTarget = allyThatKnowsAboutAnEnemy;
                     closestDistance = distance;
                  }
               }
            }
         }
         // This should cause us to spin around, looking for an enemy
         if (closestDistance > 0) {
            Rules.diag(_self.getName() + " can see ally " + closestAllyWithTarget.getName() + ", who can see either enemies, or an ally that can.");
            // Move directly to this ally, until we can see his target.
            _allyID = closestAllyWithTarget._uniqueID;
            return closestAllyWithTarget;
         }
         for (Character allyThatDoesntKnowAboutAnEnemy : alliesThatDontKnowAboutAnEnemy) {
            for (Character allyThatKnowAboutAnEnemy : alliesThatKnowAboutAnEnemy) {
               if (map.canSee(allyThatDoesntKnowAboutAnEnemy, allyThatKnowAboutAnEnemy, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
                  Rules.diag(allyThatDoesntKnowAboutAnEnemy.getName() + " can see " + allyThatKnowAboutAnEnemy.getName());
                  alliesThatLearnedOfAnEnemy.add(allyThatDoesntKnowAboutAnEnemy);
                  break;
               }
            }
         }
      } while (!alliesThatLearnedOfAnEnemy.isEmpty());
      return null;
   }

   private Character findClosestAlly(CombatMap map) {
      // Find all our allies that we can see, and if any of them have targets, move toward the ally
      Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);
      Character closestAlly = null;
      short shortestDistance = 1000;
      Spell charmSpell = _self.isUnderSpell(SpellCharmPerson.NAME);
      int charmerID = -1;
      if (charmSpell != null) {
         charmerID = charmSpell.getCaster()._uniqueID;
      }
      for (Character combatant : map.getCombatants()) {
         if (!combatant.isEnemy(_self)) {
            // Ignore ourselves from this list
            if (combatant._uniqueID != _self._uniqueID) {
               if (combatant._teamID == _self._teamID) {
                  List<ArenaLocation> allyCoord = myVisibilityMap.get(combatant._uniqueID);
                  if ((allyCoord != null) && (allyCoord.size() > 0)) {
                     // Ignore allies that are only 1 hex away from us, since we can not advance towards them anyway
                     short distance = ArenaCoordinates.getDistance(allyCoord.get(0), _self.getHeadCoordinates());
                     if (distance == 0) {
                        // If we are standing at the last known location for this character,
                        // then we either see him/her from here, or we've lost track of them.
                        myVisibilityMap.remove(combatant._uniqueID);
                     }
                     else {
                        // If we are charmed, always move toward the charmer, unless we don't know where he is.
                        if (charmerID == combatant._uniqueID) {
                           return combatant;
                        }
                        if ((closestAlly == null) || (distance < shortestDistance)) {
                           shortestDistance = distance;
                           closestAlly = combatant;
                        }
                     }
                  }
               }
            }
         }
      }
      return closestAlly;
   }

   private boolean requestDefense(RequestDefense defense) {
      Character attacker = getCharacter(defense.getAttackerID());
      short curMinDist = Arena.getMinDistance(_self, attacker);
      // Don't put more into the defense than the attacker put into the attack.
      StringBuilder sb = new StringBuilder();
      sb.append("attackActions = ").append(defense.getAttackActions());
      if (defense.isRangedAttack()) {
         sb.append("(ranged)");
      }
      byte pd = _self.getPassiveDefense(defense.getRange(), false/*isGrappleAttack*/, curMinDist);
      double expectedDamage = defense.getExpectedDamage() - _self.getBuild(defense.getDamageType());

      // SpiderWeb spells MUST be defended against as if they do damage:
      Spell spell = defense.getSpell();
      if (spell != null) {
         if (spell instanceof SpellSpiderWeb) {
            expectedDamage = 15;
         }
      }
      DiceSet toHitDice = defense.getExpectedToHitDice();
      expectedDamage = Math.round(expectedDamage * 10) / 10.0;
      // either base the defense actions on the attack actions,
      // or adjust the defense actions on the expected hit roll, but not both:
      //      double adjExpectedDamage = expectedDamage;
      //      if (adjExpectedHitRoll > 10)
      //         adjExpectedDamage += (adjExpectedHitRoll - 10) / 2;
      sb.append(" expectedDamage = ").append(expectedDamage);
      sb.append(" attackDice = ").append(toHitDice);
      sb.append("\n");

      Limb leftArm = _self.getLimb(LimbType.HAND_LEFT);
      Limb rightArm = _self.getLimb(LimbType.HAND_RIGHT);

      // weight the effectiveness of defenses by how quick they are.
      int penaltyRightArm = 4 * ((rightArm == null) ? 0 : rightArm.getDefenseTime(_self.getAttributeLevel(Attribute.Strength), _self));
      int penaltyLeftArm = 4 * ((leftArm == null) ? 0 : leftArm.getDefenseTime(_self.getAttributeLevel(Attribute.Strength), _self));

      short desiredDistance = getDesiredDistance(attacker, true);
      boolean preferRetreat = (curMinDist < desiredDistance) || (_self.getPainPenalty(true/*accountForBerserking*/) > 6);
      // unless we want to retreat, assess a 3-point penalty to retreats, because it moves us out of range.
      // If we do want to retreat, give a +5 bonus in favor of retreating
      int penaltyRetreat = preferRetreat ? -5 : 3;
      if (penaltyRetreat > 0) {
         // If we are penalizing ourselves for retreating, but we are already too far away to attack,
         // ignore the retreat penalty.
         if (curMinDist > desiredDistance) {
            penaltyRetreat = 0;
         }
      }
      // prefer non-spell defenses over spell defense, since it uses spell points
      int penaltySpell = 1;

      sb.append(defense);
      sb.append("\ncurDist=").append(curMinDist)
        .append(", desiredDistance=").append(desiredDistance)
        .append(", penaltyRightArm=").append(penaltyRightArm)
        .append(", penaltyLeftArm=").append(penaltyLeftArm)
        .append(", penaltyRetreat=").append(penaltyRetreat)
        .append("\n");

      int[] optionIDs = defense.getOptionIDs();
      DefenseOptions[] defOptions = new DefenseOptions[optionIDs.length];
      for (int i=0 ; i<optionIDs.length ; i++) {
         defOptions[i] = new DefenseOptions(optionIDs[i]);
      }
      String[] options = defense.getOptions();
      boolean[] enabled = defense.getEnableds();
      Map<Integer, Integer> bestAdjustedTNPerAction = new HashMap<>();
      Map<Integer, Integer> bestActualTNPerAction = new HashMap<>();
      Map<Integer, DefenseOptions> bestOptionPerAction = new HashMap<>();
      for (int i = 0; i < options.length; i++) {
         if (enabled[i]) {
            int index = options[i].indexOf("(TN=");
            if (index != -1) {
               String tn = options[i].substring(index + "(TN=".length()).replace(')', ' ').trim();
               int tnVal = Integer.valueOf(tn).intValue();
               int adjTnVal = tnVal + 0;
               int action = defOptions[i].getDefenseActionsUsed();
               if (defOptions[i].contains(DefenseOption.DEF_RIGHT)) {
                  adjTnVal -= penaltyRightArm;
               }
               if (defOptions[i].contains(DefenseOption.DEF_LEFT)) {
                  adjTnVal -= penaltyLeftArm;
               }
               if (defOptions[i].contains(DefenseOption.DEF_RETREAT)) {
                  adjTnVal -= penaltyRetreat;
               }
               adjTnVal -= penaltySpell * defOptions[i].getDefenseMagicPointsUsed();
               Integer bestAdjustedTN = bestAdjustedTNPerAction.get(action);
               if ((bestAdjustedTN == null) || (adjTnVal > bestAdjustedTN)) {
                  bestAdjustedTNPerAction.put(action, adjTnVal);
                  bestActualTNPerAction.put(action, tnVal);
                  bestOptionPerAction.put(action, defOptions[i]);
               }
            }
         }
      }
      bestActualTNPerAction.put(0, (int) pd);
      int bestDefAction = 0;
      int extraSkillRequired = 0;
      // only care about damage that cause a wound:
      if (expectedDamage < 5) {
         extraSkillRequired = (int) ((5 - expectedDamage) * 2);
         expectedDamage = 5;
      }
      double safeRateIncreaseNeeded = .40;
      double safeThreshold = .75;
      double carefulRateIncreaseNeeded = .25;
      double carefulThreshold = .50;
      if (expectedDamage <= 5) {
         // weak attack
         safeRateIncreaseNeeded = .45;
         safeThreshold = .50;
         carefulRateIncreaseNeeded = .30;
         carefulThreshold = .25;
      }
      else if (expectedDamage <= 10) {
         // mild attack
         safeRateIncreaseNeeded = .40;
         safeThreshold = .65;
         carefulRateIncreaseNeeded = .25;
         carefulThreshold = .40;
      }
      else if (expectedDamage <= 15) {
         // normal attack
         safeRateIncreaseNeeded = .30;
         safeThreshold = .70;
         carefulRateIncreaseNeeded = .15;
         carefulThreshold = .55;
      }
      else if (expectedDamage <= 20) {
         // strong attack
         safeRateIncreaseNeeded = .18;
         safeThreshold = .77;
         carefulRateIncreaseNeeded = .07;
         carefulThreshold = .64;
      }
      else {
         // deadly attack
         safeRateIncreaseNeeded = .07;
         safeThreshold = .85;
         carefulRateIncreaseNeeded = .04;
         carefulThreshold = .70;
      }

      //      if (_aiType != AI_Type.DEFENSIVE) {
      //         safeRateIncreaseNeeded = Math.round(safeRateIncreaseNeeded * 150) / 100.0;
      //         safeThreshold = Math.round(100 - ((1 - safeThreshold) * 150)) / 100.0;
      //         carefulRateIncreaseNeeded = Math.round(carefulRateIncreaseNeeded * 150) / 100.0;
      //         carefulThreshold = Math.round(100 - ((1 - carefulThreshold) * 150)) / 100.0;
      //      }
      //      if (_aiType == AI_Type.OFFENSIVE) {
      //         safeRateIncreaseNeeded = Math.round(safeRateIncreaseNeeded * 150) / 100.0;
      //         safeThreshold = Math.round(100 - ((1 - safeThreshold) * 150)) / 100.0;
      //         carefulRateIncreaseNeeded = Math.round(carefulRateIncreaseNeeded * 150) / 100.0;
      //         carefulThreshold = Math.round(100 - ((1 - carefulThreshold) * 150)) / 100.0;
      //      }

      // If we are in final-defense mode, always defend as best we can:
      boolean pureDefenseMode = (_self.getActionsAvailable(true/*usedForDefenseOnly*/) != _self.getActionsAvailable(false/*usedForDefenseOnly*/));
      if (!pureDefenseMode) {
         if (_self.getPainPenalty(true/*accountForBerseking*/) > 5) {
            pureDefenseMode = true;
         }
      }
      if (pureDefenseMode) {
         safeRateIncreaseNeeded = .01;
         safeThreshold = .99;
         carefulRateIncreaseNeeded = .01;
         carefulThreshold = .99;
      }
      sb.append("expectedDamage = ").append(expectedDamage);
      sb.append(" safeRateIncreaseNeeded = ").append(safeRateIncreaseNeeded);
      sb.append(" safeThreshold = ").append(safeThreshold);
      sb.append(" carefulRateIncreaseNeeded = ").append(carefulRateIncreaseNeeded);
      sb.append(" carefulThreshold = ").append(carefulThreshold);
      sb.append("\n");

      sb.append("action\ttn\todds\tdam@odds");
      sb.append("\n");
      for (int defActions = 5; defActions >= 0; defActions--) {
         Integer tnHigh = bestActualTNPerAction.get(defActions);
         Integer tnLower = bestActualTNPerAction.get(defActions - 1);
         if ((tnHigh != null) && (tnLower != null)) {
            if (toHitDice == null) {
               toHitDice = Rules.getDice(attacker.getAttributeLevel(Attribute.Dexterity), defense.getAttackActions(), Attribute.Dexterity);
            }
            double oddsDefSuccessHigh = Math.round((1.0 - toHitDice.getOddsForTN(tnHigh + extraSkillRequired)) * 100) / 100.0;
            double oddsDefSuccessLower = Math.round((1.0 - toHitDice.getOddsForTN(tnLower + extraSkillRequired)) * 100) / 100.0;
            double expectDamageAtLowerOdds = Math.round((expectedDamage * (1 - oddsDefSuccessLower)) * 100) / 100.0;
            double expectDamageAtHigherOdds = Math.round((expectedDamage * (1 - oddsDefSuccessHigh)) * 100) / 100.0;
            sb.append(defActions).append("\t").append(tnHigh).append("(").append(tnHigh + extraSkillRequired).append(")\t").append(oddsDefSuccessHigh).append("\t").append(
                                                                                                                                                                        expectDamageAtHigherOdds);
            sb.append("\n");
            sb.append(defActions - 1).append("\t").append(tnLower).append("(").append(tnLower + extraSkillRequired).append(")\t").append(oddsDefSuccessLower).append(
                                                                                                                                                                  expectDamageAtLowerOdds);
            sb.append("\n");

            if (oddsDefSuccessLower > safeThreshold) {
               // The next lower defense still gives us a good safety margin, use it
               continue;
            }
            double rateIncreaseNeeded = safeRateIncreaseNeeded;
            if (oddsDefSuccessLower > carefulThreshold) {
               rateIncreaseNeeded = carefulRateIncreaseNeeded;
            }
            // As our chance of successfully defending gets smaller, consider less restrictions
            if (oddsDefSuccessLower < 25) {
               rateIncreaseNeeded /= 2;
            }
            if (oddsDefSuccessLower < 10) {
               // If we are going to get hit, at least try to reduce the skilled-bonus damage we'll take:
               if ((tnHigh - tnLower) > 1) {
                  rateIncreaseNeeded = -1.0;
               }
            }
            if ((oddsDefSuccessHigh - oddsDefSuccessLower) > rateIncreaseNeeded) {
               // If the odd of defending successfully at the next lower defense is significantly lower
               // than our current higher defense level, don't reduce to that.
               sb.append("\n (oddsDefSuccessHigh - oddsDefSuccessLower)=").append(oddsDefSuccessHigh - oddsDefSuccessLower);
               sb.append("< rateIncreaseNeeded= ").append(rateIncreaseNeeded);
               bestDefAction = defActions;
               break;
            }
         }
      }
      sb.append("\n bestDefAction = ").append(bestDefAction);

      StringBuilder threadLogger = (StringBuilder) CombatServer.getThreadStorage("thread logger");
      if (threadLogger != null) {
         threadLogger.append(sb.toString()).append("\n");
      }
      Rules.diag(sb.toString());
      List<Integer> priorities = new ArrayList<>();
      priorities.add(bestOptionPerAction.get(bestDefAction).getIntValue());
      return selectAnswer(defense, priorities);
   }

   private boolean requestDefenseSimple(RequestDefense defense) {
      Character attacker = getCharacter(defense.getAttackerID());
      short curMinDist = Arena.getMinDistance(_self, attacker);
      // Don't put more into the defense than the attacker put into the attack.
      StringBuilder sb = new StringBuilder();
      byte defenseActions = defense.getAttackActions();
      sb.append("attackActions = ").append(defense.getAttackActions());
      if (defense.isRangedAttack()) {
         defenseActions++;
         sb.append("(ranged)");
      }
      double expectedDamage = defense.getExpectedDamage() - _self.getBuild(defense.getDamageType());
      sb.append(" expectedDamage = ").append(expectedDamage);
      sb.append(", defending against a ");
      if (expectedDamage < 0) {
         // don't both defending against weak attacks
         defenseActions--;
         if (expectedDamage < -10) {
            // don't both defending against weak attacks
            defenseActions--;
            sb.append("very ");
            if (expectedDamage < -20) {
               // don't both defending against weak attacks
               defenseActions--;
               sb.append("very ");
            }
         }
         sb.append("weak ");
      }
      else if (expectedDamage > 10) {
         // If the damage would be excessive, be extra-defensive
         defenseActions++;
         if (expectedDamage > 20) {
            // If the damage would be excessive, be extra-defensive
            defenseActions++;
            sb.append("very ");
         }
         sb.append("strong ");
      }
      byte pd = _self.getPassiveDefense(defense.getRange(), false/*isGrappleAttack*/, curMinDist);
      if (pd > (defense.getExpectedToHitRoll() + 5)) {
         // Don't defend too hard against attacks that probably won't hit anyway.
         defenseActions--;
         if (pd > (defense.getExpectedToHitRoll() + 10)) {
            defenseActions--;
            sb.append("very ");
         }
         sb.append("poor ");
      }
      sb.append("attack ");
      List<DefenseOptions> priorities = new ArrayList<>();

      if (defenseActions < 0) {
         defenseActions = 0;
      }
      if (defenseActions > 3) {
         defenseActions = 3;
      }
      DefenseOption primaryDef   = DefenseOption.DEF_DODGE;
      DefenseOption secondaryDef = DefenseOption.DEF_RIGHT;
      DefenseOption tertiaryDef  = DefenseOption.DEF_LEFT;
      boolean rangedAttack = defense.isRangedAttack();
      boolean grappleAttack = defense.isGrapple();
      boolean chargeAttack = defense.isChargeAttack();
      Limb leftArm = _self.getLimb(LimbType.HAND_LEFT);
      Limb rightArm = _self.getLimb(LimbType.HAND_RIGHT);

      short distance = ArenaCoordinates.getDistance(_self.getHeadCoordinates(), attacker.getHeadCoordinates());
      byte leftTN = (leftArm == null) ? 0 : ((Hand) leftArm).getDefenseTN(_self, rangedAttack, distance, chargeAttack,
                                                                          grappleAttack, defense.getDamageType());
      byte rightTN = (rightArm == null) ? 0 : ((Hand) rightArm).getDefenseTN(_self, rangedAttack, distance, chargeAttack,
                                                                             grappleAttack, defense.getDamageType());
      byte dodgeTN = Rules.getDodgeLevel(_self.getAttributeLevel(Attribute.Nimbleness));
      byte magicTN = 0;
      IInstantaneousSpell bestDefSpell = null;
      if (defense.isRangedAttack()) {
         bestDefSpell = _self._bestDefensiveSpell_ranged;
      }
      else {
         bestDefSpell = _self._bestDefensiveSpell_melee;
      }
      if (bestDefSpell != null) {
         if (bestDefSpell instanceof PriestSpell) {
            magicTN = _self.getAffinity(((PriestSpell) bestDefSpell).getDeity());
         }
         else {
            magicTN = bestDefSpell.getLevel();
         }
      }

      // weight the effectiveness of defenses by how quick they are.
      rightTN -= 4 * ((rightArm == null) ? 0 : rightArm.getDefenseTime(_self.getAttributeLevel(Attribute.Strength), _self));
      leftTN -= 4 * ((leftArm == null) ? 0 : leftArm.getDefenseTime(_self.getAttributeLevel(Attribute.Strength), _self));

      primaryDef = pickBest(dodgeTN, leftTN, rightTN, magicTN);
      if (primaryDef == DefenseOption.DEF_LEFT) {
         leftTN = -1;
      }
      if (primaryDef == DefenseOption.DEF_RIGHT) {
         rightTN = -1;
      }
      secondaryDef = pickBest(dodgeTN, leftTN, rightTN, magicTN);
      if ((primaryDef == DefenseOption.DEF_DODGE) && (secondaryDef == DefenseOption.DEF_DODGE)) {
         dodgeTN = -1;
      }
      if (secondaryDef == DefenseOption.DEF_LEFT) {
         leftTN = -1;
      }
      if (secondaryDef == DefenseOption.DEF_RIGHT) {
         rightTN = -1;
      }
      tertiaryDef = pickBest(dodgeTN, leftTN, rightTN, magicTN);

      sb.append(", primary def = ");
      if (primaryDef == DefenseOption.DEF_DODGE) {
         sb.append("dodge");
      }
      if (primaryDef == DefenseOption.DEF_RIGHT) {
         sb.append("right");
      }
      if (primaryDef == DefenseOption.DEF_LEFT) {
         sb.append("left");
      }
      if (primaryDef == DefenseOption.DEF_MAGIC_1) {
         sb.append("magic");
      }
      sb.append(", secondary def = ");
      if (secondaryDef == DefenseOption.DEF_DODGE) {
         sb.append("dodge");
      }
      if (secondaryDef == DefenseOption.DEF_RIGHT) {
         sb.append("right");
      }
      if (secondaryDef == DefenseOption.DEF_LEFT) {
         sb.append("left");
      }
      if (secondaryDef == DefenseOption.DEF_MAGIC_1) {
         sb.append("magic");
      }
      sb.append(", tertiary def = ");
      if (tertiaryDef == DefenseOption.DEF_DODGE) {
         sb.append("dodge");
      }
      if (tertiaryDef == DefenseOption.DEF_RIGHT) {
         sb.append("right");
      }
      if (tertiaryDef == DefenseOption.DEF_LEFT) {
         sb.append("left");
      }
      if (tertiaryDef == DefenseOption.DEF_MAGIC_1) {
         sb.append("magic");
      }
      sb.append(". final defenseActions = ").append(defenseActions);
      Rules.diag(sb.toString());

      List<DefenseOption> preferredDefenses = new ArrayList<>();
      // add the primary defense first:
      preferredDefenses.add(primaryDef);
      preferredDefenses.add(secondaryDef);
      preferredDefenses.add(tertiaryDef);

      short desiredDistance = getDesiredDistance(attacker, true);
      boolean prefereRetreat = (curMinDist < desiredDistance) || (_self.getPainPenalty(true/*accountForBerserking*/) > 6);

      int defActionsRemaining = defenseActions;
      while (defActionsRemaining > 0) {
         DefenseOptions defensesToUse = new DefenseOptions();
         int adjDefActionsRemaining = defActionsRemaining;
         if (prefereRetreat && (defActionsRemaining > 1)) {
            defensesToUse.add(DefenseOption.DEF_RETREAT);
            adjDefActionsRemaining -= 2;
         }
         defensesToUse = getDefenseAction(preferredDefenses, defensesToUse, adjDefActionsRemaining);
         priorities.add(defensesToUse);

         if (defensesToUse.contains(DefenseOption.DEF_RETREAT)) {
            // also get one with a dodge instead of a retreat, in case retreat is not an option:
            DefenseOptions altDefensesToUse = defensesToUse.clone();
            altDefensesToUse.remove(DefenseOption.DEF_RETREAT);
            altDefensesToUse.add(DefenseOption.DEF_DODGE);
            priorities.add(altDefensesToUse);
         }
         defActionsRemaining--;
      }

      //      // put a marker in
      //      priorities.add(Integer.valueOf(-1234));

      //      // If our pain penalty is too high, we won't be attacking anyway, so retreat
      //      if (prefereRetreat) {
      //         sb.append("favoring retreats ");
      //         // favor retreats
      //         switch (defenseActions) {
      //            case 7:
      //               if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //                  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_5));
      //                  if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_4 | DefenseOption.DEF_RIGHT));
      //                  if (secondaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_4 | DefenseOption.DEF_LEFT));
      //               }
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_LEFT | DefenseOption.DEF_MAGIC_3));
      //            case 6:
      //               if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //                  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_4));
      //                  if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_RIGHT));
      //                  if (secondaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_LEFT));
      //               }
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_LEFT | DefenseOption.DEF_MAGIC_2));
      //            case 5:
      //               if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //                  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_3));
      //                  if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_RIGHT));
      //                  if (secondaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_LEFT));
      //               }
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_LEFT | DefenseOption.DEF_MAGIC_1));
      //            case 4:
      //               if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //                  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_2));
      //                  if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_RIGHT));
      //                  if (secondaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_LEFT));
      //               }
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_LEFT));
      //            case 3:
      //               if (primaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1));
      //               if (primaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //               if (primaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1));
      //            case 2:
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT));
      //         }
      //         // fall into the next switch, which will re-add the same entries, but that doesn't matter
      //         // because if they are available, they will be picked up from this order first.
      //         // Also, if they are ignored from the top set, they will be ignored in the bottom set.
      //      }
      //      // if block is an option, always use it.
      //
      //      // make sure our adjusted defense actions is still in range
      //      if (defenseActions > 4) defenseActions = 4;
      //      if (defenseActions < 0) defenseActions = 0;
      //      switch (defenseActions) {
      //         case 4:
      //            if (primaryDef == DefenseOption.DEF_RIGHT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_1));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_LEFT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_1));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_DODGE) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_2));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT | DefenseOption.DEF_MAGIC_1));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //               priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_4));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_MAGIC_3));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_3 | DefenseOption.DEF_LEFT));
      //            }
      //            priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_2));
      //            priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT | DefenseOption.DEF_MAGIC_1));
      //            priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT));
      //         case 3:
      //            if (primaryDef == DefenseOption.DEF_RIGHT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_LEFT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_DODGE) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_MAGIC_1));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT | DefenseOption.DEF_RIGHT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_3));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_MAGIC_2));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_2 | DefenseOption.DEF_LEFT));
      //            }
      //         case 2:
      //            if (primaryDef == DefenseOption.DEF_RIGHT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_LEFT) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_LEFT | DefenseOption.DEF_RIGHT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_DODGE) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_DODGE));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_RETREAT));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE | DefenseOption.DEF_LEFT));
      //            }
      //            else if (primaryDef == DefenseOption.DEF_MAGIC_1) {
      //               if (secondaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_2));
      //               if (secondaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_DODGE));
      //               if (secondaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_RIGHT));
      //               if (secondaryDef == DefenseOption.DEF_LEFT)  priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1 | DefenseOption.DEF_LEFT));
      //            }
      //         case 1:
      //            if (primaryDef == DefenseOption.DEF_RIGHT) priorities.add(Integer.valueOf(DefenseOption.DEF_RIGHT));
      //            else if (primaryDef == DefenseOption.DEF_LEFT) priorities.add(Integer.valueOf(DefenseOption.DEF_LEFT));
      //            else if (primaryDef == DefenseOption.DEF_DODGE) priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE));
      //            else if (primaryDef == DefenseOption.DEF_MAGIC_1) priorities.add(Integer.valueOf(DefenseOption.DEF_MAGIC_1));
      //         case 0:
      //      }
      //      if (defenseActions > 0) {
      //         // make sure we didn't miss any:
      //         priorities.add(Integer.valueOf(DefenseOption.DEF_DODGE));
      //         priorities.add(Integer.valueOf(DefenseOption.DEF_RIGHT));
      //         priorities.add(Integer.valueOf(DefenseOption.DEF_LEFT));
      //      }
      priorities.add(new DefenseOptions(DefenseOption.DEF_PD));
      List<Integer> byIds = new ArrayList<>();
      for (DefenseOptions opt : priorities) {
         byIds.add(opt.getIntValue());
      }
      return selectAnswer(defense, byIds);
   }

   private static DefenseOptions getDefenseAction(List<DefenseOption> preferredDefenses,
                                                  DefenseOptions baseDefensesToUse,
                                                  int defActionsRemaining) {
      for (DefenseOption thisDef : preferredDefenses) {
         if (thisDef.getActionsUsed() <= defActionsRemaining) {
            defActionsRemaining -= thisDef.getActionsUsed();
            if (thisDef == DefenseOption.DEF_DODGE) {
               if (baseDefensesToUse.contains(DefenseOption.DEF_DODGE)) {
                  // a 2-action dodge is a retreat:
                  baseDefensesToUse.remove(DefenseOption.DEF_DODGE);
                  baseDefensesToUse.add(DefenseOption.DEF_RETREAT);
               }
               else if (baseDefensesToUse.contains(DefenseOption.DEF_RETREAT)) {
                  // If we already have a retreat, don't add the dodge
               }
               else {
                  // we have neither a dodge nor a retreat yet, add the dodge
                  baseDefensesToUse.add(DefenseOption.DEF_DODGE);
               }
            }
            else {
               if (thisDef == DefenseOption.DEF_MAGIC_1) {
                  // Add one more action to the magic defense
                  int magicCount = Math.min(5, baseDefensesToUse.getDefenseMagicPointsUsed() + 1);
                  baseDefensesToUse.remove(DefenseOption.DEF_MAGIC_5, DefenseOption.DEF_MAGIC_4, DefenseOption.DEF_MAGIC_3, DefenseOption.DEF_MAGIC_2, DefenseOption.DEF_MAGIC_1);
                  if (magicCount == 5) {
                     baseDefensesToUse.add(DefenseOption.DEF_MAGIC_5);
                  }
                  else if (magicCount == 4) {
                     baseDefensesToUse.add(DefenseOption.DEF_MAGIC_4);
                  }
                  else if (magicCount == 3) {
                     baseDefensesToUse.add(DefenseOption.DEF_MAGIC_3);
                  }
                  else if (magicCount == 2) {
                     baseDefensesToUse.add(DefenseOption.DEF_MAGIC_2);
                  }
                  else if (magicCount == 1) {
                     baseDefensesToUse.add(DefenseOption.DEF_MAGIC_1);
                  }
               }
               else {
                  baseDefensesToUse.add(thisDef);
               }
            }
         }
      }
      return baseDefensesToUse;
   }

   private static DefenseOption pickBest(byte dodgeTN, byte leftTN, byte rightTN, byte magicTN) {
      if ((dodgeTN > leftTN) && (dodgeTN > rightTN) && (dodgeTN > magicTN)) {
         return DefenseOption.DEF_DODGE;
      }
      if ((leftTN > rightTN) && (leftTN > magicTN)) {
         return DefenseOption.DEF_LEFT;
      }
      if (rightTN > magicTN) {
         return DefenseOption.DEF_RIGHT;
      }
      return DefenseOption.DEF_MAGIC_1;
   }

   private boolean selectAnswerByType(SyncRequest request, List<RequestActionOption> priorities) {
      int availableActionCount = request.getActionCount();
      IRequestOption[] options = request.getReqOptions();
      boolean[] enableds = request.getEnableds();
      for (RequestActionOption preferedActOpt : priorities) {
         // Check for exact matches.
         for (int availableAction = 0; availableAction < availableActionCount; availableAction++) {
            if (enableds[availableAction]) {
               if (options[availableAction].getIntValue() == preferedActOpt.getIntValue()) {
                  request.setAnswerByOptionIndex(availableAction);
                  return true;
               }
            }
         }
         // Unless the priority declares the hand to use, allow other limbs to do the job
         if (!preferedActOpt.getLimbType().isHand()) {
            for (int availableAction = 0; availableAction < availableActionCount; availableAction++) {
               if (enableds[availableAction]) {
                  if (options[availableAction] instanceof RequestActionOption) {
                     RequestActionOption reqActOpt = (RequestActionOption) options[availableAction];
                     if (reqActOpt.getValue() == preferedActOpt.getValue()) {
                        request.setAnswerByOptionIndex(availableAction);
                        return true;
                     }
                  }
               }
            }
         }
      }
      DebugBreak.debugBreak(_self.getName() + ": desired answer not found");
      return false;
   }

   private boolean selectAnswer(SyncRequest request, List<Integer> priorities) {
      int availableActionCount = request.getActionCount();
      int[] optionIds = request.getOptionIDs();
      boolean[] enableds = request.getEnableds();
      for (int desiredAction = 0; desiredAction < priorities.size(); desiredAction++) {
         for (int availableAction = 0; availableAction < availableActionCount; availableAction++) {
            if (enableds[availableAction]) {
               if (optionIds[availableAction] == (priorities.get(desiredAction)).intValue()) {
                  request.setAnswerByOptionIndex(availableAction);
                  return true;
               }
            }
         }
      }
      DebugBreak.debugBreak(_self.getName() + ": desired answer not found");
      return false;
   }

   private boolean requestPosition(RequestPosition position) {
      List<RequestActionOption> priorities = new ArrayList<>();
      // always go to a standing position, but we need to transition through other to get there:
      priorities.add(new RequestActionOption("", RequestActionType.OPT_CHANGE_POS_STAND, LimbType.BODY, true));
      // we have to sit or kneel before we can stand
      priorities.add(new RequestActionOption("", RequestActionType.OPT_CHANGE_POS_SIT, LimbType.BODY, true));
      priorities.add(new RequestActionOption("", RequestActionType.OPT_CHANGE_POS_KNEEL, LimbType.BODY, true));
      // and we have to get off our back before we can kneel
      return selectAnswerByType(position, priorities);
   }

   private List<Orientation> _pathCache = null;

   private List<Orientation> findPath(Character fromChar, Character toChar, CombatMap map, boolean allowRanged) {
      ArenaLocation fromLoc = map.getLocations(fromChar).get(0);
      ArenaLocation toLoc = map.getLocations(toChar).get(0);
      boolean canSeeDirectly = true;
      if (!map.canSee(fromChar, toChar, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
         Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);
         if (myVisibilityMap != null) {
            List<ArenaLocation> targetLocs = myVisibilityMap.get(toChar._uniqueID);
            if ((targetLocs != null) && (!targetLocs.isEmpty())) {
               toLoc = targetLocs.get(0);
               if (toLoc != null) {
                  if (fromChar.isInCoordinates(toLoc)) {
                     // If we are standing at the last known location for this character,
                     // then we either see him/her from here, or we've lost track of them.
                     myVisibilityMap.remove(toChar._uniqueID);
                  }
               }
            }
         }
         canSeeDirectly = false;
      }

      if (_pathCache != null) {
         if ((_pathCache.size() == 0) ||
             ((_pathCache.size() == 1) && (_pathCache.get(0).equals(fromChar.getOrientation())))) {
            _pathCache = null;
         }
      }
      if (_pathCache != null) {
         int newStart = -1;
         int newEnd = -1;
         // the path returned starts at the farthest point away (where we can attack our target)
         // and ends at our current location
         Orientation curOrient = fromChar.getOrientation();
         for (int cacheIndex = 0; cacheIndex < _pathCache.size(); cacheIndex++) {
            Orientation orient = _pathCache.get(cacheIndex);
            if (orient.getPosition() != curOrient.getPosition()) {
               break;
            }
            if (orient.equals(curOrient)) {
               newStart = cacheIndex;
            }
            else {
               if (curOrient.getCostToEnter(orient, fromChar, map) > 99) {
                  // We can no longer travel this path. Perhaps someone is in the way now?
                  _pathCache = null;
                  break;
               }
               if (!curOrient.getPossibleFutureOrientations(map).contains(orient)) {
                  // the next movement defined is not attainable from our current orientation.
                  break;
               }
               // otherwise move our current orient along this path, so we can use it
               // to test the next orientation.
               curOrient = orient;
            }
            boolean couldSee = map.couldSee(orient, toChar, true/*considerFacing*/, true/*blockedByAn yStandingCharacter*/);
            if (couldSee && orient.canAttack(fromChar, toChar, map, allowRanged, false/*onlyChargeTypes*/)) {
               newEnd = cacheIndex;
               break;
            }
         }
         if (newEnd != -1) {
            Rules.diag("cached path = " + _pathCache + "\n start=" + newStart + ", end=" + newEnd);
            // If the toChar has moved closer, but we can reach him at a point on the existing path,
            // make the new point where we can reach him the end of the path.
            while (_pathCache.size() > (newEnd + 1)) {
               _pathCache.remove(newEnd + 1);
            }
            // If we are not at the beginning of the path, make it the beginning.
            while (newStart-- > 0) {
               if (_pathCache.isEmpty()) {
                  DebugBreak.debugBreak();
               }
               else {
                  _pathCache.remove(0);
               }
            }
            return _pathCache;
         }
      }
      Character mover = fromChar;
      byte movementRate = mover.getMovementRate();
      if (map.hasLineOfSight(fromLoc, toLoc, true/*blockedByAnyStandingCharacter*/)) {
         // This is the line-of-sight path
         List<ArenaLocation> LOSpath = map.getLOSPath(fromLoc, toLoc, true/*trimPath*/);
         // Make sure this path doesn't have any slow spots, or characters blocking.
         // If it does, consider other paths
         for (int i = 0; i < (LOSpath.size() - 1); i++) {
            ArenaLocation loc0 = LOSpath.get(i);
            ArenaLocation loc1 = LOSpath.get(i + 1);
            if (loc1.sameCoordinates(toLoc)) {
               break;
            }
            if (canSeeDirectly && toChar.isInCoordinates(loc1)) {
               break;
            }
            if ((loc1.getCharacters().size() > 0) || (loc1.costToEnter(loc0, mover) > 1)) {
               LOSpath = null;
               break;
            }
         }
         if (LOSpath != null) {
            //            Orientation startOrientation = fromChar.getOrientation();
            //            List<Orientation> orientationPath = new ArrayList<Orientation>();
            //            boolean nextOrientFound = false;
            //            for (ArenaLocation location : LOSpath) {
            //               nextOrientFound = false;
            //               for (Orientation nextOrientation : startOrientation.getPossibleFutureOrientations(map)) {
            //                  if (nextOrientation.getHeadLocation().equals(location)) {
            //                     orientationPath.add(startOrientation);
            //                     startOrientation = nextOrientation;
            //                     nextOrientFound = true;
            //                     break;
            //                  }
            //               }
            //               if (!nextOrientFound) {
            //                  break;
            //               }
            //            }
            //            if (nextOrientFound) {
            //               return orientationPath;
            //            }
            if ((LOSpath != null) && (LOSpath.size() > 1)) {
               if (LOSpath.get(LOSpath.size() - 1).sameCoordinates(toLoc)) {
                  LOSpath.remove(LOSpath.size() - 1);
               }
            }
            List<Orientation> orientationPath = travelLOSPath(fromChar.getOrientation(), LOSpath, map);
            if (orientationPath != null) {
               _pathCache = orientationPath;
               return _pathCache;
            }
         }
      }
      Character target = (canSeeDirectly || (_aiType == AI_Type.GOD)) ? toChar : null;
      ArenaLocation targetLoc = (target == null) ? toLoc : null;
      _pathCache = getBestRouteTo(fromChar.getOrientation(), target, targetLoc, map, movementRate, allowRanged);
      return _pathCache;
   }

   private List<Orientation> travelLOSPath(Orientation startOrientation, List<ArenaLocation> LOSpath, CombatMap map) {
      List<Orientation> orientationPath = new ArrayList<>();
      if (LOSpath.isEmpty()) {
         return orientationPath;
      }

      ArenaLocation location = LOSpath.remove(0);
      if (location.sameCoordinates(startOrientation.getHeadCoordinates())) {
         if (LOSpath.size() == 0) {
            return orientationPath;
         }
         location = LOSpath.remove(0);
      }
      Facing firstDir = ArenaCoordinates.getFacingToLocation(startOrientation.getHeadCoordinates(), location);
      Facing curDir = startOrientation.getFacing();
      if (curDir.turn(3) == firstDir) {
         Orientation newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) 1/*dirDelta*/, map);
         if (newLoc == null) {
            DebugBreak.debugBreak("can't rotate orientation 1");
            newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) 1/*dirDelta*/, map);
         }
         else {
            orientationPath.add(newLoc);
         }
         newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) 2/*dirDelta*/, map);
         if (newLoc == null) {
            // If the size if greater than 1, it's normal not to be able to turn 2 hexes at once.
            if (startOrientation.getCoordinates().size() == 1) {
               DebugBreak.debugBreak("can't rotate orientation 2");
               newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) 2/*dirDelta*/, map);
            }
         }
         else {
            orientationPath.add(newLoc);
         }
      }
      else if (curDir.turn(2)== firstDir) {
         Orientation newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) 1/*dirDelta*/, map);
         if (newLoc == null) {
            DebugBreak.debugBreak("can't rotate orientation 1");
         }
         else {
            orientationPath.add(newLoc);
         }
      }
      else if (curDir.turn(4) == firstDir) {
         Orientation newLoc = startOrientation.rotateAboutPoint((byte) 0/*pointIndex*/, (byte) -1/*dirDelta*/, map);
         if (newLoc == null) {
            DebugBreak.debugBreak("can't rotate orientation -1");
         }
         else {
            orientationPath.add(newLoc);
         }
      }
      for (Orientation nextOrientation : startOrientation.getPossibleFutureOrientations(map)) {
         if (nextOrientation.getHeadCoordinates().sameCoordinates(location)) {
            orientationPath.add(nextOrientation);
            List<Orientation> futurePath = travelLOSPath(nextOrientation, LOSpath, map);
            if (futurePath != null) {
               orientationPath.addAll(futurePath);
               return orientationPath;
            }
         }
      }
      return null;
   }

   private List<Orientation> getBestRouteTo(Orientation startOrientation, Character target, ArenaCoordinates toLoc, CombatMap map, byte movementRate,
                                                 boolean allowRanged) {
      Map<Orientation, Orientation> bestFromMap = new HashMap<>();
      Orientation destinationOrientation = Arena.getAllRoutesFrom(startOrientation, map, 256/*maxMovement*/, movementRate, !_self.hasMovedThisRound(), _self,
                                                                  bestFromMap, target, toLoc, allowRanged, false/*onlyChargeTypes*/,
                                                                  null/*itemsToPickupUsingSkill*/, _aiType == AI_Type.GOD/*considerUnknownLocations*/);
      return convertBestFromMapToBestRouteTo(startOrientation, bestFromMap, destinationOrientation);
   }

   private static List<Orientation> convertBestFromMapToBestRouteTo(Orientation startOrientation, Map<Orientation, Orientation> bestFromMap,
                                                                  Orientation destinationOrientation) {
      if (destinationOrientation == null) {
         return null;
      }
      List<Orientation> path = new ArrayList<>();
      path.add(destinationOrientation);
      if (destinationOrientation.equals(startOrientation)) {
         return path;
      }
      while (true) {
         destinationOrientation = bestFromMap.get(destinationOrientation);
         if (destinationOrientation == null) {
            return null;
         }
         path.add(0, destinationOrientation);
         if (destinationOrientation.equals(startOrientation)) {
            return path;
         }
      }
   }

   private short getDesiredDistance(Character target, boolean defensive) {
      // If the enemy has a 2-hex weapon, and I have a 1-hex weapon: get in right next to him
      // If the enemy has a 1-hex weapon, and I have a 1-hex weapon: get 2 hexes away.
      // If the enemy has a 2-hex weapon, and I have a 2-hex weapon: get 3 hexes away.
      // If the enemy has a 1-hex weapon, and I have a 2-hex weapon: get 2 hexes away.
      if (target == null) {
         return 1;
      }

      Weapon myWeapon = getMyWeapon();
      if ((myWeapon != null) && (myWeapon._attackStyles.length == 1)) {
         if (myWeapon._attackStyles[0].canCharge(_self.isMounted(), _self.getLegCount() > 3)) {
            if (!myWeapon.getName().equals(Weapon.NAME_Fangs)) {
               // The only weapon we have is a charge weapon, so use the distance to charge from:
               return CHARGE_DISTANCE;
            }
         }
      }
      short selfMaxRange = _self.getWeaponMaxRange(false/*allowRanged*/, false/*onlyChargeTypes*/);
      short targetMaxRangeMelee = target.getWeaponMaxRange(false/*allowRanged*/, false/*onlyChargeTypes*/);
      short targetMaxRangeRanged = target.getWeaponMaxRange(true/*allowRanged*/, false/*onlyChargeTypes*/);
      // If the target has a ranged weapon, close on them quickly
      if (targetMaxRangeMelee != targetMaxRangeRanged) {
         return selfMaxRange;
      }
      if (selfMaxRange != targetMaxRangeMelee) {
         if (selfMaxRange < targetMaxRangeMelee) {
            // we have a longer reach then they do, sit at our max range, which is out of their range
            return selfMaxRange;
         }
         // They have a longer reach than I do. Wait for a chance to close in
         // If the enemy can't act this round, its safe to close in.
         if (target.getActionsAvailableThisRound(false/*usedForDefenseOnly*/) == 0) {
            return selfMaxRange;
         }
      }
      // we have the same reach or less
      return (short) (defensive ? targetMaxRangeMelee+1 : selfMaxRange);
   }

   public void updateTargetPriorities(List<Character> orderedEnemies, CharacterDisplay display) {
      _targets = orderedEnemies;
   }

   private Character selectTarget(CharacterDisplay display, CombatMap map, boolean allowRangedAllack) {
      Map<Integer, List<ArenaLocation>> myVisibilityMap = _mapToMapOfLocations.get(_self._uniqueID);
      if (myVisibilityMap == null) {
         myVisibilityMap = new HashMap<>();
         _mapToMapOfLocations.put(_self._uniqueID, myVisibilityMap);
      }
      boolean canAttackUnseenTargets = (_aiType == AI_Type.GOD);
      Character target = null;
      if ((_targets == null) && (display != null)) {
         _targets = display.getOrderedEnemies();
      }
      boolean updateList = false;
      short lowestWeight = 2000;
      if ((_targets != null) && (_targets.size() > 0)) {
         if (_targets.size() == 1) {
            lowestWeight = getTargetWeight(myVisibilityMap, canAttackUnseenTargets, _targets.get(0), map, allowRangedAllack);
         }
         else {
            for (int i = 0; i < (_targets.size() - 1); i++) {
               Character combatantI = _targets.get(i);
               if (combatantI != null) {
                  // ignore ourselves
                  if (combatantI._uniqueID == _self._uniqueID) {
                     continue;
                  }
                  short targetIWeight = getTargetWeight(myVisibilityMap, canAttackUnseenTargets, combatantI, map, allowRangedAllack);
                  if (targetIWeight < lowestWeight) {
                     lowestWeight = targetIWeight;
                  }
                  for (int j = i + 1; j < _targets.size(); j++) {
                     Character combatantJ = _targets.get(j);
                     if (combatantJ != null) {
                        // ignore ourselves
                        if (combatantI._uniqueID == _self._uniqueID) {
                           continue;
                        }
                        // ignore combatants that are not fighting
                        if (!combatantJ.stillFighting()) {
                           continue;
                        }

                        if (!canAttackUnseenTargets) {
                           // If we haven't seen this enemy, don't raise his/her priority
                           if (myVisibilityMap.get(combatantJ._uniqueID) == null) {
                              continue;
                           }
                        }
                        boolean swapChars = false;
                        // If I is not fighting, but J is, move J up in priority
                        if (combatantI.stillFighting()) {
                           // If we are aiming at someone, that we already
                           // have spent time aiming at, they are highest priority
                           if (_self.getAimDuration(combatantJ._uniqueID) > 0) {
                              swapChars = true;
                           }
                           else {
                              short targetJWeight = getTargetWeight(myVisibilityMap, canAttackUnseenTargets, combatantJ, map, allowRangedAllack);
                              if (targetIWeight > targetJWeight) {
                                 swapChars = true;
                                 if (targetJWeight < lowestWeight) {
                                    lowestWeight = targetJWeight;
                                 }
                              }
                           }
                        }
                        else {
                           swapChars = true;
                        }
                        if (swapChars) {
                           _targets.set(i, combatantJ);
                           _targets.set(j, combatantI);
                           updateList = true;
                           combatantI = combatantJ;
                        }
                     }
                  }
               }
            }
         }
         if (lowestWeight < 1000) {
            // If our best target isn't good enough to attack, don't attack
            target = _targets.get(0);
         }
      }
      if (updateList) {
         List<Integer> orderedTargetIds = new ArrayList<>();
         for (int i = 0; i < (_targets.size() - 1); i++) {
            orderedTargetIds.add(_targets.get(i)._uniqueID);
         }
         _self.setTargetPriorities(orderedTargetIds);
         // send a request back to the server changing our target priorities,
         if (display != null) {
            display.updateTargetPriorities(_targets);
         }
      }
      if (!canAttackUnseenTargets) {
         // If we haven't seen this enemy, don't attack him
         if ((target != null) && (myVisibilityMap.get(target._uniqueID) == null)) {
            target = null;
         }
      }
      return target;
   }

   /**
    * @param myTargetLocations
    * @param canAttackUnseenTargets
    * @param target
    * @return
    */
   private short getTargetWeight(Map<Integer, List<ArenaLocation>> myTargetLocations,
                                 boolean canAttackUnseenTargets, Character target,
                                 CombatMap map, boolean allowRangedAllack) {
      // ignore our teammates
      if (target._teamID == _self._teamID) {
         // Unless we are independent, in which case, attack anyone
         if (_self._teamID != TEAM_INDEPENDENT) {
            return 2000;
         }
      }

      if (!target.stillFighting()) {
         // don't attack characters that are unconscious or dead.
         return 1000;
      }
      boolean targetVisible = true;
      List<ArenaLocation> targetLocs = myTargetLocations.get(target._uniqueID);
      if (targetLocs == null) {
         // we don't know where this character is
         if (!canAttackUnseenTargets) {
            return 1000;
         }
         // GOD AIs always know where a target is:
         targetLocs = map.getLocations(target);
         myTargetLocations.put(target._uniqueID, targetLocs);
         targetVisible = false;
      }
      ArenaCoordinates targetCoord = targetLocs.get(0);
      // Is the target where we last saw him?
      if ((targetCoord == null) || !target.isInCoordinates(targetCoord)) {
         targetVisible = false;
         if (canAttackUnseenTargets) {
            targetCoord = target.getHeadCoordinates();
         }
      }
      if ((targetCoord == null) && !canAttackUnseenTargets) {
         // we don't know where this character is
         return 1000;
      }
      short targetWeight = Arena.getMinDistance(target, _self);
      // If we are aiming at someone, that we already
      // have spent time aiming at, they are highest priority
      if (_self.getAimDuration(target._uniqueID) > 0) {
         targetWeight = 0;
      }

      if (allowRangedAllack ) {
         if (targetVisible) {
            ArenaLocation fromLoc = map.getLocation(_self.getHeadCoordinates());
            // make sure we have a line of sight to this target
            targetVisible = map.hasLineOfSight(fromLoc, targetCoord, true/*blockedByAnyStandingCharacter*/);
            if (!targetVisible) {
               // If we can't see the target, prefer to find someone else that we can see
               targetWeight += 20;
            }
         }
      }
      if (!targetVisible) {
         if (canAttackUnseenTargets) {
            // If we can attack this target, because we can't see him,
            // lower his/her priority by adding an addition range to him.
            targetWeight += 10;
         }
         else {
            targetWeight += 30;
         }
      }

      if (target.getCondition().isCollapsed()) {
         // If the target has collapsed, and another target is readily available,
         // select the next target, because the collapsed target is not a threat.
         targetWeight += 3;
         if (target.getPainPenalty(true/*accountForBerserking*/) > 15) {
            // The target is not getting up soon:
            targetWeight += 5;
         }
      }
      // Reduce threat level of combatants that are held by magic spells, such as spider webs
      int magicHeldLevel = 0;
      for (IHolder holder : target.getHolders())
      {
         if (holder instanceof Spell) {
            Spell holdingSpell = (Spell) holder;
            int duration = -1;
            if (holdingSpell instanceof IExpiringSpell) {
               duration = ((IExpiringSpell) holdingSpell).getDuration();
            }
            // If the spell doesn't expire, or wont expire for another 10 turns, count this as a hold.
            if ((duration == -1) || (duration > 10)) {
               magicHeldLevel += holder.getHoldingLevel();
            }
         }
      }
      // If they are not held by at least 10 points, we should try to take advantage of their hold,
      // so don't reduce the target priority. But if they are held at 10 or more, consider them less
      // of a threat, and deal with other enemies.
      if (magicHeldLevel > 10) {
         targetWeight += magicHeldLevel;
      }

      // If this target is aiming a missile weapon at us, we should be targeting him.
      // Animals are not able to make this distinction
      if (!_self.isAnimal()) {
          boolean targetHoldingMissileWeapon = false;
          for (Limb limb : target.getLimbs()) {
              Thing heldThing = limb.getHeldThing();
              if ((heldThing != null) && (heldThing instanceof MissileWeapon)) {
                  targetHoldingMissileWeapon = true;
                  break;
              }
          }
         if (targetHoldingMissileWeapon) {
            if (target._targetID == -1) {
               // If he is not aiming at anyone yet, consider him a close threat
               // but keep his/her distance in the equation, so if two or more enemies are targeting us,
               // we close on the closest one.
               if (targetWeight > 4) {
                  // never increase the targetWeight, in case we are very close to the enemy
                  targetWeight = (short) (4 + (targetWeight / 5));
               }
            }
            if (target.getAimDuration(_self._uniqueID) > 0) {
               // If he is aiming at us, consider him as much of a threat as someone in our face.
               if (targetWeight > 1) {
                  // never increase the targetWeight, in case we are very close to the enemy
                  targetWeight = (short) (1 + (targetWeight / 5));
               }
            }
         }
      }

      return targetWeight;
   }

   public void removeCachedDataOnArenaExit() {
      _mapToMapOfLocations.clear();
      _targets = null;
   }

   private Spell selectSpellToCast(CharacterDisplay display, CombatMap map, Character target) {
      Spell spell = selectSpellToCast(display, true/*considerCastingTime*/, map, target);
      if (spell != null) {
         return spell;
      }
      return selectSpellToCast(display, false, map, target);
   }

   private Spell selectSpellToCast(CharacterDisplay display, boolean considerCastingTime, CombatMap map, Character target) {

      if ((_self.getCondition().getMageSpellPointsAvailable() == 0) &&
          (_self.getCondition().getPriestSpellPointsAvailable() == 0)) {
         return null;
      }

      short distance = 25;
      short maxTime = 10;
      if (target != null) {
         distance = Arena.getMinDistance(target, _self);
         // If my target is focused on me, make sure he can't reach me before I complete this spell.
         if (target._targetID == _self._uniqueID) {
            short targetMoveRate = target.getMovementRate();
            if (targetMoveRate != 0) {
               maxTime = (short) ((distance / targetMoveRate) + 1);
            }
         }
      }
      List<Spell> spells = _self.getSpells();
      List<Spell> validSpells = new ArrayList<>();
      if (spells.isEmpty()) {
         return null;
      }

      byte expectedDamage = 0;
      Weapon myWeap = getMyWeapon();
      if (myWeap != null) {
         WeaponStyleAttack style = myWeap.getAttackStyle(0);
         expectedDamage = style.getDamage(_self.getAdjustedStrength());
         expectedDamage += style.getVarianceDie().getAverageRoll(true/*allowExplodes*/);
         byte maxBuild = -100;
         for (Character character : map.getCombatants()) {
            if (_self.isEnemy(character)) {
               byte build = character.getBuild(style.getDamageType());
               if (build > maxBuild) {
                  maxBuild = build;
               }
            }
         }
         expectedDamage -= maxBuild;
      }

      byte currentPain = _self.getPainPenalty(true/*accountForBerserking*/);

      // do we need healing?
      List<Integer> outList = new ArrayList<>();
      assessPersonalWounds(outList);
      int maxWound = outList.remove(0);
      int woundCount = outList.remove(0);
      //boolean crippledLimb = outList.remove(0) == 1;

      Advantage divinePower = _self.getAdvantage(Advantage.DIVINE_POWER);
      for (Spell spell : spells) {
         if (spell.getCaster() == null) {
            spell.setCaster(_self);
         }
         if (spell instanceof ICastInBattle) {
            byte time = spell.getIncantationTime();
            if (spell instanceof MageSpell) {
               time += desiredPowerForSpell(spell);
            }
            else {
               time++;
            }
            if (!considerCastingTime || (time <= maxTime)) {
               if (spell.isBeneficial()) {
                  int weight = 0;
                  if (spell instanceof ostrowski.combat.common.spells.mage.SpellSpeed) {
                     weight = 4;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.mage.SpellArmor) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.mage.SpellBlur) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.mage.SpellFireball) {
                     if ((myWeap != null) && myWeap.getName().contains("Fireball spell")) {
                        weight = -1;
                     }
                     else {
                        weight = 1;
                     }
                  }
                  else if ( (spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingWeapon) &&
                           !(spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingMissileWeapon)) {
                     if ((myWeap != null) && myWeap.getName().contains("Fireball spell")) {
                        weight = -1;
                     }
                     else {
                        weight = 1;
                     }
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.mage.SpellStrength) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellSpeed) {
                     weight = 4;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellDexterity) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellStrength) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellIncreaseDamage) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.SpellSummonBeing) {
                     weight = ((PriestSpell)spell).getAffinity();
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellArmor) {
                     weight = 1;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellImprovedArmor) {
                     weight = 2;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellMagicShield) {
                     weight = 2;
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellMissileShield) {
                     weight = 1;
                  }
                  else  {
                     if (spell instanceof ostrowski.combat.common.spells.priest.healing.SpellCureWound) {
                        SpellCureWound cureSpell = (SpellCureWound) spell;
                        byte woundReduction = cureSpell.getWoundReduction(divinePower.getLevel());
                        if (spell instanceof ostrowski.combat.common.spells.priest.healing.SpellHeal) {
                           if (woundCount >= 3) {
                              weight = woundCount;
                           }
                        }
                        else if (spell instanceof ostrowski.combat.common.spells.priest.healing.SpellCureSeriousWound) {
                           if (maxWound >= woundReduction) {
                              weight = woundReduction;
                           }
                        }
                        else if (spell instanceof ostrowski.combat.common.spells.priest.healing.SpellCureWound) {
                           if (maxWound >= woundReduction) {
                              weight = woundReduction;
                           }
                        }
                     }
                  }
                  if (weight <= 0) {
                     continue;
                  }
                  if (spell instanceof PriestSpell) {
                     // In too much pain to cast a priest spell
                     if (currentPain  > 3) {
                        continue;
                     }
                     if (_self.getCondition().getPriestSpellPointsAvailable() < ((PriestSpell)spell).getAffinity()) {
                        // we don't have enough spell points to even cast a 1-power version:
                        continue;
                     }
                  }

                  if (spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingWeapon) {
                     if (myWeap == null) {
                        continue;
                     }
                     if (myWeap.getName().contains("Fireball spell")) {
                        continue;
                     }
                     boolean isForMissiles = (spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingMissileWeapon);
                     if (isForMissiles != myWeap.isMissileWeapon()) {
                        continue;
                     }
                  }
                  if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellMissileShield) {
                     boolean enemiesWithMissileWeapons = false;
                     for (Character combatant : map.getCombatants()) {
                        if (combatant.isEnemy(_self)) {
                           Weapon enemyWeap = combatant.getWeapon();
                           if ((enemyWeap != null) && (enemyWeap.isMissileWeapon())) {
                              enemiesWithMissileWeapons = true;
                              break;
                           }
                        }
                     }
                     if (!enemiesWithMissileWeapons) {
                        continue;
                     }
                  }
                  if (  (spell instanceof ostrowski.combat.common.spells.mage.SpellStrength)
                     || (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellStrength)
                     || (spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingWeapon)
                     || (spell instanceof ostrowski.combat.common.spells.mage.SpellFlamingMissileWeapon)
                     || (spell instanceof ostrowski.combat.common.spells.priest.offensive.SpellIncreaseDamage)) {
                     // without a weapon to attack with, these spells do me no good.
                     if (myWeap == null) {
                        continue;
                     }
                     // If you already expect to do at least 10 points of damage,
                     // adding to your strength or damage is not a significant bonus.
                     if (expectedDamage > 10) {
                        continue;
                     }
                  }

                  if (_self.isUnderSpell(spell.getName()) != null) {
                     continue;
                  }
                  if (spell.getActiveSpellIncompatibleWith(_self) != null) {
                     continue;
                  }

                  if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellMagicShield) {
                     Limb leftHand = _self.getLimb(LimbType.HAND_LEFT);
                     if ((leftHand == null) || (leftHand.isCrippled())) {
                        // we can't hold a shield
                        continue;
                     }
                     Thing heldThing = leftHand.getHeldThing();
                     if ((heldThing != null) && (heldThing.isReal())) {
                        // we are holding something already
                        continue;
                     }
                     Skill shieldSkill = _self.getSkill(SkillType.Shield);
                     if ((shieldSkill == null) || (shieldSkill.getLevel() < 2)) {
                        // we don't know how to use a shield
                        continue;
                     }
                     Limb rightHand = _self.getLimb(LimbType.HAND_RIGHT);
                     if ((rightHand != null) && !rightHand.isCrippled()) {
                        heldThing = rightHand.getHeldThing();
                        if ((heldThing != null) && (heldThing.isReal())) {
                           if (heldThing instanceof Weapon) {
                              Weapon weap = (Weapon) heldThing;
                              if (weap.isOnlyTwoHanded()) {
                                 // we have a two-handed weapon:
                                 continue;
                              }
                           }
                        }
                     }
                  }
                  if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellArmor) {
                     for (Spell validSpell : validSpells) {
                        if (validSpell instanceof ostrowski.combat.common.spells.priest.defensive.SpellImprovedArmor) {
                           weight = 0;
                           break;
                        }
                     }
                     if (weight == 0) {
                        continue;
                     }
                  }
                  else if (spell instanceof ostrowski.combat.common.spells.priest.defensive.SpellImprovedArmor) {
                     for (int i=0 ; i<validSpells.size() ; i++) {
                        Spell validSpell = validSpells.get(i);
                        if (validSpell instanceof ostrowski.combat.common.spells.priest.defensive.SpellArmor) {
                           validSpells.remove(i);
                           i--;
                        }
                     }
                  }

                  while (weight-- > 0) {
                     validSpells.add(spell);
                  }
               }
               else {
                  if (target != null) {
                     if ((spell instanceof IMissileSpell) || (spell instanceof IAreaSpell)) {
                        if (target._targetID == _self._uniqueID) {
                           short movementDist = (short) (target.getMovementRate() * time);
                           short distanceAtCastTime = (short) (distance - movementDist);
                           if (distanceAtCastTime > 1) {
                              spell.setCaster(_self);
                              RANGE range = spell.getRange(distanceAtCastTime);
                              // If they will be in point-blank range, increase the odds that we use this spell:
                              if (range == RANGE.POINT_BLANK) {
                                 validSpells.add(spell);
                                 validSpells.add(spell);
                                 validSpells.add(spell);
                              }
                              else if (range == RANGE.SHORT) {
                                 validSpells.add(spell);
                                 validSpells.add(spell);
                              }
                              else if (range == RANGE.MEDIUM) {
                                 validSpells.add(spell);
                              }
                           }
                        }
                        else {
                           validSpells.add(spell);
                        }
                     }
                     else if (spell instanceof ostrowski.combat.common.spells.mage.SpellFireball) {
                        validSpells.add(spell);
                     }
                     else if (spell instanceof SpellSpiderWeb) {
                        if (target != null) {
                           boolean alreadyInWeb = false;
                           for (IHolder holder : target.getHolders()) {
                              if (holder instanceof SpellSpiderWeb) {
                                 alreadyInWeb = true;
                              }
                           }
                           if (!alreadyInWeb) {
                              validSpells.add(spell);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      if (!validSpells.isEmpty()) {
         int randomIndex = (int) (validSpells.size() * CombatServer.random());
         return validSpells.get(randomIndex);
      }
      return null;
   }

}
