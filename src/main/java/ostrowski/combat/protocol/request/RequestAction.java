/*
 * Created on May 16, 2006
 *
 */
package ostrowski.combat.protocol.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.DiceSet;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.DamageType;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Position;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.IMissileSpell;
import ostrowski.combat.common.spells.IRangedSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.mage.SpellSpiderWeb;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaCoordinates;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.SemaphoreAutoLocker;

public class RequestAction extends SyncRequest implements Enums
{
   public  int                          actorID                   = -1;
   public  int                          targetID                  = -1;
   public  RequestPosition              positionRequest           = null;
   public  RequestAttackStyle           styleRequest              = null;
   public  RequestMovement              movementRequest           = null;
   public  RequestLocation              locationSelection         = null;
   public  RequestEquipment             equipmentRequest          = null;
   public  RequestTarget                targetPriorities          = null;
   public  RequestSpellTypeSelection    spellTypeSelectionRequest = null;
   public  RequestSpellSelection        spellSelectionRequest     = null;
   public  RequestSingleTargetSelection targetSelection           = null;
   private Spell                        spell                     = null;
   private boolean                      dualGrappleAttack         = false;
   private LimbType limbType = null;

   @Override
   public RequestActionOption[] getReqOptions()
   {
      RequestActionOption[] result = new RequestActionOption[options.size()];
      for (int i = 0; i < options.size() ; i++) {
         if (options.get(i) instanceof RequestActionOption) {
            result[i] = (RequestActionOption) options.get(i);
         }
         else {
            if (options.get(i).getIntValue() == -1) {
               result[i] = new RequestActionOption("", RequestActionType.OPT_NO_ACTION, null, false);
            }
            else {
               DebugBreak.debugBreak("wrong object type in RequestAction");
            }
         }
      }
      return result;
   }

   @Override
   protected String getAllowedKeyStrokesForOption(int optionID) {
      IRequestOption ans = getRequestOptionByIntValue(optionID);
      if (ans instanceof RequestActionOption) {
         return ((RequestActionOption)ans).getValue().getAllowedKeyStrokesForOption();
      }
      return "";
   }

   public RequestAction() {
      // c'tor used by the SerializableFactory class, when reading in a object of this class
   }
   public RequestAction(int actorID, int targetID) {
      this.actorID = actorID;
      this.targetID = targetID;
   }
   @Override
   public void init() {
      super.init();
      limbType = null;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.setAnswerByOptionIndex(i);
         computeHand();
      }
   }
   @Override
   public synchronized void setCustAnswer(String answer)  {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.setCustAnswer(answer);
         computeHand();
      }
   }
   @Override
   public synchronized boolean setAnswerID(int answerID ) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         if (!super.setAnswerID(answerID)) {
            return false;
         }
         computeHand();
         return true;
      }
   }
   @Override
   public void copyAnswer(SyncRequest source)
   {
      if (source instanceof RequestAction) {
         limbType = ((RequestAction)source).limbType;
      }
      super.copyAnswer(source);
   }
   public LimbType getLimb() {
      if (isEquipUnequip()) {
         return equipmentRequest.getLimb();
      }
      if (limbType == null) {
         computeHand();
      }
      return limbType;
   }

   private void computeHand() {
      if (limbType == null) {
         if (answer instanceof RequestActionOption) {
            RequestActionOption reqAct = (RequestActionOption) answer;
            limbType = reqAct.getLimbType();
         }
      }
   }

   @Override
   public boolean isCancelable() { return false;}

   public boolean isDefendable() {
      if (isAttack()) {
         return true;
      }
      if (isCompleteSpell()) {
         if (spell != null) {
            return spell.isDefendable();
         }
      }
      return false;
   }
   public boolean isAttack() {
      return (answer != null) && (getAttackActions(false/*considerSpellAsAttack*/) > 0);
   }

   public boolean isCharge()             { return (answer != null) && ((RequestActionOption) answer).getValue().isCharge(); }
   public boolean isAdvance()            { return (answer != null) && ((RequestActionOption) answer).getValue().isAdvance(); }
   public boolean isEvasiveMove()        { return (answer != null) && ((RequestActionOption) answer).getValue().isEvasiveMove(); }
   public boolean isRetreat()            { return false; }
   public boolean isReadyWeapon()        { return (answer != null) && ((RequestActionOption) answer).getValue().isReadyWeapon(); }
   public boolean isChangePosition()     { return (answer != null) && ((RequestActionOption) answer).getValue().isChangePosition();}
   public boolean isEquipUnequip()       { return (answer != null) && ((RequestActionOption) answer).getValue().isEquipUnequip(); }
   public boolean isChangeTargets()      { return (answer != null) && ((RequestActionOption) answer).getValue().isChangeTargets(); }
   public boolean isBeginSpell()         { return (answer != null) && ((RequestActionOption) answer).getValue().isBeginSpell(); }
   public boolean isPrepareInateSpell()  { return (answer != null) && ((RequestActionOption) answer).getValue().isPrepareInateSpell();}
   public boolean isContinueSpell()      { return (answer != null) && ((RequestActionOption) answer).getValue().isContinueSpell();}
   public boolean isChannelEnergy()      { return (answer != null) && ((RequestActionOption) answer).getValue().isChannelEnergy(); }
   public boolean isMaintainSpell()      { return (answer != null) && ((RequestActionOption) answer).getValue().isMaintainSpell(); }
   public boolean isDiscardSpell()       { return (answer != null) && ((RequestActionOption) answer).getValue().isDiscardSpell();}
   public boolean isCompleteMageSpell()  { return (answer != null) && ((RequestActionOption) answer).getValue().isCompleteMageSpell(); }
   public boolean isCompletePriestSpell(){ return (answer != null) && ((RequestActionOption) answer).getValue().isCompletePriestSpell(); }
   public boolean isCompleteSpell()      { return (answer != null) && ((RequestActionOption) answer).getValue().isCompleteSpell(); }
   public boolean isApplyItem()          { return (answer != null) && ((RequestActionOption) answer).getValue().isApplyItem(); }
   public boolean isFinalDefense()       { return (answer != null) && ((RequestActionOption) answer).getValue().isFinalDefense(); }
   public boolean isTargetEnemy()        { return (answer != null) && ((RequestActionOption) answer).getValue().isTargetEnemy(); }
   public boolean isPrepareRanged()      { return (answer != null) && ((RequestActionOption) answer).getValue().isPrepareRanged(); }
   public boolean isWaitForAttack()      { return (answer != null) && ((RequestActionOption) answer).getValue().isWaitForAttack();}
   public boolean isPickupItem(Character actor, Arena arena) {
      if ((answer != null) && ((RequestActionOption) answer).getValue().isLocationAction()) {
         return arena.isPickupItem(actor, this);
      }
      return false;
   }
   public boolean isLocationAction(Character actor, Arena arena) {
      if ((answer != null) && ((RequestActionOption) answer).getValue().isAnyLocationAction()) {
         return arena.isLocationAction(actor, this);
      }
      return false;
   }
   public SyncRequest getNextQuestion(Character actor, List<Character> combatants, Arena arena) {
      SyncRequest nextReq = null;
      actor.setMovingEvasively(false);
      if (isAttack()) {
         if (isCharge()) {
            byte moveAvailable = actor.getAvailableMovement(false/*movingEvasively*/);
            // Can we continue moving? If not, return null
            if ((moveAvailable > 0) && (!actor.isMoveComplete())) {
               if ((movementRequest == null) || (!movementRequest.hasMovesLeft())) {
                  movementRequest = new RequestMovement();
                  Character target = arena.getCharacter(actor.targetID);
                  if (target != null) {
                     HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
                     List<Orientation> validDestinations = new ArrayList<>();
                     HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient = new HashMap<>();
                     actor.getOrientation().getPossibleChargePathsToTarget(arena.getCombatMap(), actor, target,
                                                                           actor.getAvailableMovement(false/*movingEvasively*/),
                                                                           mapOrientationToNextOrientationsLeadingToChargeAttack);

                     for (List<Orientation> destOrientationList : mapOrientationToNextOrientationsLeadingToChargeAttack.values()) {
                        if (destOrientationList != null) {
                           for (Orientation orient : destOrientationList) {
                              validDestinations.add(orient);
                              List<Orientation> nextOrients = mapOrientationToNextOrientationsLeadingToChargeAttack.get(orient);
                              if (nextOrients != null) {
                                 // this orientation has no place to move forward, so it must be able to attack the target from here
                                 for (Orientation nextOrient : nextOrients) {
                                    mapOfFutureOrientToSourceOrient.put(nextOrient, orient);
                                 }
                              }
                           }
                        }
                     }


                     movementRequest.setOrientations(validDestinations, mapOfFutureOrientToSourceOrient, actor);
                     //_locationRequest.addOption(0, "move");
                     movementRequest.setMessage(actor.getName() + ", please select your destination on the arena map.");
                     nextReq = movementRequest;
                  }
               }
            }
         }
         if (styleRequest == null) {
            styleRequest = actor.getRequestAttackStyle(this, arena);
         }
         nextReq = styleRequest;
      }
      else if (isChangePosition()) {
         if (positionRequest == null) {
            positionRequest = actor.getRequestPosition(this);
         }
         nextReq = positionRequest;
      }
      else if (isAdvance()) { // but not an attack! (isAttack() check above rules that out)
         byte moveAvailable = actor.getAvailableMovement(isEvasiveMove());
         // Can we continue moving? If not, return null
         if ((moveAvailable == 0) || (actor.isMoveComplete())) {
            return null;
         }
         if ((movementRequest == null) || (!movementRequest.hasMovesLeft())) {
            movementRequest = new RequestMovement();
            List<Orientation> futureOrientations = new ArrayList<>();

            HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient = new HashMap<>();
            arena.getMoveableLocations(actor, moveAvailable, arena.getCombatMap(), futureOrientations, mapOfFutureOrientToSourceOrient);
            movementRequest.setOrientations(futureOrientations, mapOfFutureOrientToSourceOrient, actor);
            //_locationRequest.addOption(0, "move");
            movementRequest.setMessage(actor.getName() + ", please select your destination on the arena map.");
         }
         nextReq = movementRequest;
      }
      else if (isChangeTargets()) {
         arena.getCombatMap().recomputeKnownLocations(actor, true, true, null);
         if (targetSelection == null) {
            // If spell is 'null', this will use the actor's missile weapon for range assessment
            targetSelection = new RequestSingleTargetSelection(actor, spell, arena, this);
            nextReq = targetSelection;
         }
         else {
            // make sure the new order is in place before we ask for the next action.
            int newTargetUniqueID = targetSelection.getAnswerID();
            if (newTargetUniqueID != -1) {
               actor.setTarget(newTargetUniqueID);
            }
            List<Character> charactersTargetingActor = arena.battle.getCharactersAimingAtCharacter(actor);
            nextReq = actor.getActionRequest(arena, null/*delayedTarget*/, charactersTargetingActor);
            nextReq.copyDataInto(this);
            nextReq = this;
         }
         //         if (targetPriorities == null) {
         //            targetPriorities = actor.getTargetPrioritiesRequest(this, arena);
         //            nextReq = targetPriorities;
         //         }
         //         else {
         //            // make sure the new order is in place before we ask for the next action.
         //            actor.setTargetPriorities(targetPriorities.getOrderedTargetIds());
         //            List<Character> charactersTargetingActor = arena.battle.getCharactersAimingAtCharacter(actor);
         //            nextReq = actor.getActionRequest(arena, null/*delayedTarget*/, charactersTargetingActor);
         //            nextReq.copyDataInto(this);
         //            nextReq = this;
         //         }
      }
      else if (isEquipUnequip()) {
         if (equipmentRequest == null) {
            equipmentRequest = actor.getEquipmentRequest();
         }
         nextReq = equipmentRequest;
      }
      else if (isBeginSpell()) {
         if (spellTypeSelectionRequest == null) {
            spellTypeSelectionRequest = actor.getSpellTypeSelectionRequest();
            nextReq = spellTypeSelectionRequest;
         }
         else {
            spellSelectionRequest = (RequestSpellSelection) spellTypeSelectionRequest.getNextQuestion(actor, combatants, arena);
            nextReq = spellSelectionRequest;
         }
      }
      else if (isCompleteSpell()) {
         if (spell.getTargetType() == TargetType.TARGET_AREA) {
            if (locationSelection == null) {
               locationSelection = new RequestLocation(((IAreaSpell) spell).getImageResourceName());
               ArenaCoordinates actorCoords = actor.getOrientation().getHeadCoordinates();
               CombatMap map = arena.getCombatMap();
               ArenaLocation curLoc = map.getLocation(actorCoords);
               List<ArenaCoordinates> visibleLocationsInRange = new ArrayList<>();
               short maxRange = spell.getMaxRange(actor);
               short minRange = spell.getMinRange(actor);
               for (short col = 0 ; col<map.getSizeX() ; col++) {
                  for (short row = (short) (col%2) ; row<map.getSizeY() ; row += 2) {
                     ArenaCoordinates mapLoc = map.getLocationQuick(col, row);
                     if (mapLoc != null) {
                        ArenaLocation curMapLoc = map.getLocation(mapLoc);
                        short dist = ArenaCoordinates.getDistance(mapLoc, curLoc);
                        if ((dist <= maxRange) && (dist >= minRange)) {
                           if (map.canSeeLocation(actor.getOrientation(), curLoc, curMapLoc,
                                                  true/*considerFacing*/, false/*blockedByAnyStandingCharacter*/,
                                                  -1/*markAsKnownByCharacterUniqueID*/)) {
                              visibleLocationsInRange.add(mapLoc);
                           }
                        }
                     }
                  }
               }
               locationSelection.setCoordinates(visibleLocationsInRange);
            }
            nextReq = locationSelection;
         }
         else if (spell.requiresTargetToCast()) {
            if (targetSelection == null) {
               targetSelection = new RequestSingleTargetSelection(actor, spell, arena, this);
            }
            nextReq = targetSelection;
         }
      }
      if (nextReq != null) {
         if (nextReq.isCancelable()) {
            nextReq.addOption(new RequestActionOption("Cancel action",  RequestActionType.OPT_CANCEL_ACTION, LimbType.BODY, true/*enabled*/));
            //nextReq.addOption(OPT_CANCEL_ACTION, "Cancel action", true/*enabled*/);
         }
         if (nextReq.isCancel()) {
            if (nextReq.getEnabledCount(true/*includeCancelAction*/) == 1) {
               // If the action we took have only a cancel sub action,
               // then the action itself is invalid, disallow it.
               answer.setEnabled(false);
            }
            init();
            answer = null;
            if (nextReq == equipmentRequest) {
               equipmentRequest = null;
            }
            else if (nextReq == movementRequest) {
               movementRequest = null;
            }
            else if (nextReq == positionRequest) {
               positionRequest = null;
            }
            else if (nextReq == styleRequest) {
               styleRequest = null;
            }
            else if (nextReq == targetPriorities) {
               targetPriorities = null;
            }
            else if (nextReq == targetSelection) {
               targetSelection = null;
            }
            else if ((nextReq == spellTypeSelectionRequest) ||
                     (nextReq == spellSelectionRequest)) {
               spellTypeSelectionRequest = null;
               spellSelectionRequest = null;
            }
            return this;
         }
         // If this has already been answered, don't report it as a new question
         if (nextReq.isAnswered()) {
            if (nextReq instanceof RequestMovement) {
               RequestMovement move = (RequestMovement) nextReq;
               if (move.hasMovesLeft()) {
                  return nextReq;
               }
            }
            return null;
         }
      }
      return nextReq;
   }

   public boolean isRangedAttack()       { return (answer != null) && ((RequestActionOption) answer).getValue().isRangedAttack();       }
   public boolean isGrappleAttack()      { return (answer != null) && ((RequestActionOption) answer).getValue().isGrappleAttack();      }
   public boolean isBreakFree()          { return (answer != null) && ((RequestActionOption) answer).getValue().isBreakFree();          }
   public boolean isCounterAttack()      { return (answer != null) && ((RequestActionOption) answer).getValue().isCounterAttack();      }
   public boolean isCounterAttackThrow() { return (answer != null) && ((RequestActionOption) answer).getValue().isCounterAttackThrow(); }
   public boolean isCounterAttackGrab()  { return (answer != null) && ((RequestActionOption) answer).getValue().isCounterAttackGrab();  }
   public byte getAttackActions(boolean considerSpellAsAttack) { return answer == null ? 0 : ((RequestActionOption) answer).getValue().getAttackActions(considerSpellAsAttack); }
   public byte getActionsUsed()
   {
      return (answer == null) ? 0 : ((RequestActionOption) answer).getValue()
                                                                  .getActionsUsed((equipmentRequest != null) ?
                                                                                   equipmentRequest.getActionsUsed() : 1);
   }
   public String getActionDescription(Character actor, Character target, Arena arena) {
      switch (((RequestActionOption) answer).getValue()) {
         case OPT_NO_ACTION               : return actor.getName() + " does nothing.";
         case OPT_MOVE                    : return actor.getName() + ((actor.getPosition() == Position.STANDING) ? " moves." : " crawls.");
         case OPT_MOVE_EVASIVE            : return actor.getName() + " moves evasively.";
         case OPT_CLOSE_AND_ATTACK_1      :
         case OPT_CLOSE_AND_ATTACK_2      :
         case OPT_CLOSE_AND_ATTACK_3      : return actor.getName() + " advances and attacks ("+getAttackActions(false/*considerSpellAsAttack*/)+"-actions).";
         case OPT_CHARGE_ATTACK_1         :
         case OPT_CHARGE_ATTACK_2         :
         case OPT_CHARGE_ATTACK_3         : return actor.getName() + " charges to attack ("+getAttackActions(false/*considerSpellAsAttack*/)+"-actions).";
         case OPT_ATTACK_MELEE_1          :
         case OPT_ATTACK_MELEE_2          :
         case OPT_ATTACK_MELEE_3          : return actor.getName() + " attacks ("+getActionsUsed()+"-actions).";
         case OPT_ATTACK_THROW_1          :
         case OPT_ATTACK_THROW_2          :
         case OPT_ATTACK_THROW_3          : return actor.getName() + " throws "+actor.getHisHer()+" " + actor.getLimb(getLimb()).getHeldThingName() + " (" + getActionsUsed() + "-actions).";
         case OPT_ATTACK_MISSILE          : return actor.getName() + " fires "+actor.getHisHer()+" " + actor.getLimb(getLimb()).getHeldThingName() + ".";
         case OPT_PREPARE_RANGED          : return actor.getName() + " prepares ranged weapon. (" + ((Hand)actor.getLimb(getLimb())).getWeaponPrepareState() + ")";
         case OPT_TARGET_ENEMY            : return actor.getName() + " targets " + target.getName() + ".";
         case OPT_READY_1                 :
         case OPT_READY_2                 :
         case OPT_READY_3                 :
         case OPT_READY_4                 :
         case OPT_READY_5                 : return actor.getName() + " readies "+actor.getHisHer()+" " + actor.getLimb(getLimb()).getHeldThingName() + "("+getActionsUsed()+"-actions).";
         case OPT_ON_GAURD                : return actor.getName() + " remains poised, on gaurd.";
         case OPT_FINAL_DEFENSE_1         :
         case OPT_FINAL_DEFENSE_2         :
         case OPT_FINAL_DEFENSE_3         :
         case OPT_FINAL_DEFENSE_4         :
         case OPT_FINAL_DEFENSE_5         : return actor.getName() + " makes a final defensive action ("+getActionsUsed()+"-actions).";
         case OPT_CHANGE_POS              : return actor.getName() + " changes " + actor.getHisHer() + " position. (" + positionRequest.getAnswer() + ")";
         case OPT_WAIT_TO_ATTACK          : return actor.getName() + " waits for an opportunatey to attack.";
         case OPT_EQUIP_UNEQUIP           : return actor.getName() + " equips or unequips gear: " + ((equipmentRequest == null) ? "" : equipmentRequest.getAnswer());
         case OPT_CHANGE_TARGET_PRIORITIES: return actor.getName() + " changes targets.";
         case OPT_BEGIN_SPELL             : return actor.getName() + " begins a '"+actor.getCurrentSpellName() +"' spell";
         case OPT_CONTINUE_INCANTATION    : return actor.getName() + " continues casting a '"+actor.getCurrentSpellName() +"' spell";
         case OPT_CHANNEL_ENERGY_1        :
         case OPT_CHANNEL_ENERGY_2        :
         case OPT_CHANNEL_ENERGY_3        :
         case OPT_CHANNEL_ENERGY_4        :
         case OPT_CHANNEL_ENERGY_5        : return actor.getName() + " channels "+getActionsUsed()+" power into a '"+actor.getCurrentSpellName() +"' spell";
         case OPT_MAINTAIN_SPELL          : return actor.getName() + " maintains a '"+actor.getCurrentSpellName() +"' spell";
         case OPT_DISCARD_SPELL           : return actor.getName() + " discards a spell";
         case OPT_COMPLETE_SPELL_1        :
         case OPT_COMPLETE_SPELL_2        :
         case OPT_COMPLETE_SPELL_3        : return actor.getName() + " casts a '" + spell.getName() + "' spell (" + spell.getPower() + " power points)";
         case OPT_COMPLETE_PRIEST_SPELL_1 : return actor.getName() + " casts a '" + spell.getName() + "' spell (1 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_2 : return actor.getName() + " casts a '" + spell.getName() + "' spell (2 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_3 : return actor.getName() + " casts a '" + spell.getName() + "' spell (3 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_4 : return actor.getName() + " casts a '" + spell.getName() + "' spell (4 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_5 : return actor.getName() + " casts a '" + spell.getName() + "' spell (5 power points)";
         case OPT_APPLY_ITEM              : return actor.getName() + " applys an item: " + actor.getLimb(getLimb()).getHeldThingName();
         case OPT_COUNTER_ATTACK_GRAB_1   :
         case OPT_COUNTER_ATTACK_GRAB_2   :
         case OPT_COUNTER_ATTACK_GRAB_3   : return actor.getName() + " counter-attacks (grab, "+getActionsUsed()+"-actions).";
         case OPT_COUNTER_ATTACK_THROW_1  :
         case OPT_COUNTER_ATTACK_THROW_2  :
         case OPT_COUNTER_ATTACK_THROW_3  : return actor.getName() + " counter-attacks (throw, "+getActionsUsed()+"-actions).";
         case OPT_ATTACK_GRAPPLE_1        :
         case OPT_ATTACK_GRAPPLE_2        :
         case OPT_ATTACK_GRAPPLE_3        : return actor.getName() + " grapples ("+getActionsUsed()+"-actions).";
         case OPT_CLOSE_AND_GRAPPLE_1     :
         case OPT_CLOSE_AND_GRAPPLE_2     :
         case OPT_CLOSE_AND_GRAPPLE_3     : return actor.getName() + " closes and grapples ("+getActionsUsed()+"-actions).";
         case OPT_BREAK_FREE_1            :
         case OPT_BREAK_FREE_2            :
         case OPT_BREAK_FREE_3            : return actor.getName() + " tries to break free ("+getActionsUsed()+"-actions).";
         case OPT_PREPARE_INITATE_SPELL_1 : return actor.getName() + " prepares inate spell ("+actor.getRace().getInateSpells().get(0).getName() + ", power = "+actor.getRace().getInateSpells().get(0).getPower() + ").";
         case OPT_PREPARE_INITATE_SPELL_2 : return actor.getName() + " prepares inate spell ("+actor.getRace().getInateSpells().get(1).getName() + ", power = "+actor.getRace().getInateSpells().get(1).getPower() + ").";
         case OPT_PREPARE_INITATE_SPELL_3 : return actor.getName() + " prepares inate spell ("+actor.getRace().getInateSpells().get(2).getName() + ", power = "+actor.getRace().getInateSpells().get(2).getPower() + ").";
         case OPT_PREPARE_INITATE_SPELL_4 : return actor.getName() + " prepares inate spell ("+actor.getRace().getInateSpells().get(3).getName() + ", power = "+actor.getRace().getInateSpells().get(3).getPower() + ").";
         case OPT_PREPARE_INITATE_SPELL_5 : return actor.getName() + " prepares inate spell ("+actor.getRace().getInateSpells().get(4).getName() + ", power = "+actor.getRace().getInateSpells().get(4).getPower() + ").";
      }
      if (((RequestActionOption) answer).getValue().isAnyLocationAction()) {
         return arena.getActionDescription(actor, this);
      }

      return "";
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         actorID = readInt(in);
         targetID = readInt(in);
         byte limbVal   = readByte(in);
         limbType = (limbVal == -1) ? null : LimbType.getByValue(limbVal);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(actorID, out);
         writeToStream(targetID, out);
         writeToStream((limbType == null) ? ((byte)-1) : limbType.value, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestAction) {
            RequestAction reqAct = (RequestAction) newObj;
            reqAct.actorID = actorID;
            reqAct.equipmentRequest = equipmentRequest;
            reqAct.movementRequest = movementRequest;
            reqAct.positionRequest = positionRequest;
            reqAct.styleRequest = styleRequest;
            reqAct.targetPriorities = targetPriorities;
            reqAct.spellTypeSelectionRequest = spellTypeSelectionRequest;
            reqAct.spellSelectionRequest = spellSelectionRequest;
            reqAct.targetSelection = targetSelection;
            reqAct.targetID = targetID;
            reqAct.limbType = limbType;
         }
      }
   }

   @Override
   public String toString()
   {
      return super.toString() + ", actorID = " + actorID +
             ", equReq = " + equipmentRequest +
             ", movReq = " + movementRequest +
             ", posReq = " + positionRequest +
             ", styReq = " + styleRequest +
             ", targetPriorities = " + targetPriorities +
             ", spellTypeSelectionRequest = " + spellTypeSelectionRequest +
             ", spellSelectionRequest = " + spellSelectionRequest +
             ", targetSelection = " + targetSelection +
             ", targetID = " + targetID +
             ", hand = " + limbType;
   }

   public RANGE getRange(Character attacker, short distanceInHexes) {
      byte rangedDeterminingAttribute = 0;
      if ((styleRequest != null) && (styleRequest.isRanged())) {
         rangedDeterminingAttribute = attacker.getAdjustedStrength();
      }
      if (spell != null) {
         if (spell instanceof IRangedSpell) {
            // When an attacker has a web spell ready, but they choose to attack with a weapon instead,
            // don't let the spell affect the range results. This results in the range being returned
            // as Point_blank, or similar, when a melee attack is taking place.
            if (!isAttack()) {
               if (spell instanceof SpellSpiderWeb) {
                  SpellSpiderWeb web = (SpellSpiderWeb) spell;
                  return web.getRange(distanceInHexes);
               }
               rangedDeterminingAttribute = attacker.getAttributeLevel(spell.getCastingAttribute());
               rangedDeterminingAttribute += attacker.getRace().getBuildModifier();
            }
         }
      }
      WeaponStyleAttack attackingWeapon = getAttackStyle(attacker);
      if (!(attackingWeapon instanceof WeaponStyleAttackRanged)) {
         return RANGE.OUT_OF_RANGE;
      }
      WeaponStyleAttackRanged rangedAttack = (WeaponStyleAttackRanged) attackingWeapon;
      return rangedAttack.getRangeForDistance(distanceInHexes, rangedDeterminingAttribute);
   }

   public WeaponStyleAttack getAttackStyle(Character attacker) {
      Weapon weapon = getAttackingWeapon(attacker);
      if (weapon == null) {
         return null;
      }
      if (styleRequest != null) {
         if (styleRequest.isGrapple()) {
            return weapon.getGrappleStyle(styleRequest.getAnswerIndex());
         }
         return weapon.getAttackStyle(styleRequest.getAnswerIndex());
      }
      return weapon.getAttackStyle(0);
   }

   public Weapon getAttackingWeapon(Character attacker) {
      if (styleRequest != null) {
         return attacker.getLimb(getLimb()).getWeapon(attacker);
      }
      if (spell != null) {
         if (spell instanceof IMissileSpell) {
            IMissileSpell missileSpell = (IMissileSpell) spell;
            return missileSpell.getMissileWeapon();
         }
      }
      return null;
   }

   public boolean isRanged() {
      if (styleRequest != null) {
         return styleRequest.isRanged();
      }

      if (spell != null) {
         return spell instanceof IRangedSpell;
      }
      return false;
   }
   public DamageType getDamageType()
   {
      if (styleRequest != null) {
         return styleRequest.getDamageType();
      }

      if (spell != null) {
         return spell.getDamageType();
      }
      return ostrowski.combat.common.enums.DamageType.NONE;
   }
   public String getCantRetreatOrParryFromAttackTypeString()
   {
      if (isCharge()) {
         return "charge attack.";
      }
      if (styleRequest != null) {
         if (styleRequest.isThrown()) {
            return "thrown weapon.";
         }
         if (styleRequest.isMissile()) {
            return "missile weapon.";
         }
      }
      if (spell != null) {
         if (spell instanceof IMissileSpell) {
            return spell.getName() + " spell.";
         }
      }
      return null;
   }
   public void setSpell(Spell spell) {
      this.spell = spell;
   }
   public Spell getSpell() {
      return spell;
   }
   public WeaponStyleAttack getWeaponStyleAttack(Character attacker)
   {
      if (isCompleteSpell()) {
         if (spell instanceof IMissileSpell) {
            IMissileSpell missileSpell = (IMissileSpell) spell;
            return missileSpell.getMissileWeapon().getAttackStyle(0);
         }
      }
      LimbType limb = getLimb();
      Limb attackersLimb = attacker.getLimb(limb);
      if (attackersLimb == null) {
         return null;
      }
      Weapon attackingWeapon = attackersLimb.getWeapon(attacker);
      if (attackingWeapon != null) {
         if (styleRequest != null) {
            return attackingWeapon.getAttackStyle(styleRequest.getAnswerIndex());
         }
         return attackingWeapon.getAttackStyle(0);
      }
      return null;
   }

   public DiceSet getAttackDice(RANGE range) {
      if (styleRequest != null) {
         return styleRequest.getAttackDice();
      }
      if (isCompleteSpell()) {
         return spell.getCastDice(getActionsUsed(), range);
      }
      return null;
   }
   public DiceSet getAttackDice(Character attacker, boolean includeSkill, boolean includeWoundsAndPain, RANGE range) {
      DiceSet attackDice = getAttackDice(range);
      if (attackDice == null) {
         return null;
      }
      byte rollModifier = 0;
      if (isCompleteSpell()) {
         if (includeSkill) {
            if (spell instanceof PriestMissileSpell) {
               rollModifier += attacker.getAttributeLevel(Attribute.Dexterity) + ((PriestSpell) spell).getAffinity();
            }
            rollModifier += attacker.getSpellLevel(spell.getName());
         }
         if (includeWoundsAndPain) {
            rollModifier -= attacker.getWoundsAndPainPenalty();
         }
      }
      else {
         if (includeSkill) {
            rollModifier += attacker.getWeaponSkill(getLimb(), styleRequest.getAnswerIndex(), styleRequest.isGrapple(),
                                                    styleRequest.isCounterAttack(),
                                                    false/*accountForHandPenalty*/, true/*adjustForHoldPenalties*/);
         }
         // Even without pain & wounds, an off-hand may suffer penalties
         boolean includePain = includeWoundsAndPain && !attacker.isBerserking();
         rollModifier -= attacker.getPenaltyToUseArm(attacker.getLimb(getLimb()), includeWoundsAndPain/*includeWounds*/, includePain);
      }
      return attackDice.addBonus(rollModifier);
   }
   public double getExpectedAttackRoll(Character attacker, boolean includeSkill, boolean includeWoundsAndPain, RANGE range)
   {
      DiceSet attackDice = getAttackDice(attacker, includeSkill, includeWoundsAndPain, range);
      if (attackDice != null) {
         return attackDice.getAverageRoll(true/*allowExplodes*/);
      }
      return 0;
   }
   public byte getMinimumDamage(Character attacker)
   {
      if (isCompleteSpell() && (spell instanceof IMissileSpell)) {
         IMissileSpell missileSpell = (IMissileSpell) spell;
         return missileSpell.getSpellDamageBase();
      }
      WeaponStyleAttack style = getWeaponStyleAttack(attacker);
      if (style != null) {
         return (byte) (style.getDamageMod() + attacker.getPhysicalDamageBase());
      }
      return 0;
   }
   public void setDualGrappleAttack() {
      dualGrappleAttack = true;
   }
   public boolean isDualGrappleAttack() {
      return dualGrappleAttack;
   }
}
