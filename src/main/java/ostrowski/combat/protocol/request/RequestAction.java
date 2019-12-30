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
   public int                          _actorID                   = -1;
   public int                          _targetID                  = -1;
   public RequestPosition              _positionRequest           = null;
   public RequestAttackStyle           _styleRequest              = null;
   public RequestMovement              _movementRequest           = null;
   public RequestLocation              _locationSelection         = null;
   public RequestEquipment             _equipmentRequest          = null;
   public RequestTarget                _targetPriorities          = null;
   public RequestSpellTypeSelection    _spellTypeSelectionRequest = null;
   public RequestSpellSelection        _spellSelectionRequest     = null;
   public RequestSingleTargetSelection _targetSelection           = null;
   private Spell                       _spell                     = null;
   private boolean                     _dualGrappleAttack         = false;

   @Override
   public RequestActionOption[] getReqOptions()
   {
      RequestActionOption[] result = new RequestActionOption[_options.size()];
      for (int i=0 ; i<_options.size() ; i++) {
         if (_options.get(i) instanceof RequestActionOption) {
            result[i] = (RequestActionOption) _options.get(i);
         }
         else {
            if (_options.get(i).getIntValue() == -1) {
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

   private LimbType _limbType    = null;

   public RequestAction() {
      // c'tor used by the SerializableFactory class, when reading in a object of this class
   }
   public RequestAction(int actorID, int targetID) {
      _actorID = actorID;
      _targetID = targetID;
   }
   @Override
   public void init() {
      super.init();
      _limbType = null;
   }
   @Override
   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.setAnswerByOptionIndex(i);
         computeHand();
      }
   }
   @Override
   public synchronized void setCustAnswer(String answer)  {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.setCustAnswer(answer);
         computeHand();
      }
   }
   @Override
   public synchronized boolean setAnswerID(int answerID ) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
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
         _limbType = ((RequestAction)source)._limbType;
      }
      super.copyAnswer(source);
   }
   public LimbType getLimb() {
      if (isEquipUnequip()) {
         return _equipmentRequest.getLimb();
      }
      if (_limbType == null) {
         computeHand();
      }
      return _limbType;
   }

   private void computeHand() {
      if (_limbType == null) {
         if (_answer instanceof RequestActionOption) {
            RequestActionOption reqAct = (RequestActionOption) _answer;
            _limbType = reqAct.getLimbType();
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
         if (_spell != null) {
            return _spell.isDefendable();
         }
      }
      return false;
   }
   public boolean isAttack() {
      return (_answer != null) && (getAttackActions(false/*considerSpellAsAttack*/) > 0);
   }

   public boolean isCharge()             { return ((RequestActionOption)_answer).getValue().isCharge(); }
   public boolean isAdvance()            { return ((RequestActionOption)_answer).getValue().isAdvance(); }
   public boolean isEvasiveMove()        { return ((RequestActionOption)_answer).getValue().isEvasiveMove(); }
   public boolean isRetreat()            { return false; }
   public boolean isReadyWeapon()        { return ((RequestActionOption)_answer).getValue().isReadyWeapon(); }
   public boolean isChangePosition()     { return ((RequestActionOption)_answer).getValue().isChangePosition();}
   public boolean isEquipUnequip()       { return ((RequestActionOption)_answer).getValue().isEquipUnequip(); }
   public boolean isChangeTargets()      { return ((RequestActionOption)_answer).getValue().isChangeTargets(); }
   public boolean isBeginSpell()         { return ((RequestActionOption)_answer).getValue().isBeginSpell(); }
   public boolean isPrepareInateSpell()  { return ((RequestActionOption)_answer).getValue().isPrepareInateSpell();}
   public boolean isContinueSpell()      { return ((RequestActionOption)_answer).getValue().isContinueSpell();}
   public boolean isChannelEnergy()      { return ((RequestActionOption)_answer).getValue().isChannelEnergy(); }
   public boolean isMaintainSpell()      { return ((RequestActionOption)_answer).getValue().isMaintainSpell(); }
   public boolean isDiscardSpell()       { return ((RequestActionOption)_answer).getValue().isDiscardSpell();}
   public boolean isCompleteMageSpell()  { return ((RequestActionOption)_answer).getValue().isCompleteMageSpell(); }
   public boolean isCompletePriestSpell(){ return((RequestActionOption)_answer).getValue().isCompletePriestSpell(); }
   public boolean isCompleteSpell()      { return ((RequestActionOption)_answer).getValue().isCompleteSpell(); }
   public boolean isApplyItem()          { return ((RequestActionOption)_answer).getValue().isApplyItem(); }
   public boolean isFinalDefense()       { return ((RequestActionOption)_answer).getValue().isFinalDefense(); }
   public boolean isTargetEnemy()        { return ((RequestActionOption)_answer).getValue().isTargetEnemy(); }
   public boolean isPrepareRanged()      { return ((RequestActionOption)_answer).getValue().isPrepareRanged(); }
   public boolean isWaitForAttack()      { return ((RequestActionOption)_answer).getValue().isWaitForAttack();}
   public boolean isPickupItem(Character actor, Arena arena) {
      if (((RequestActionOption)_answer).getValue().isLocationAction()) {
         return arena.isPickupItem(actor, this);
      }
      return false;
   }
   public boolean isLocationAction(Character actor, Arena arena) {
      if (((RequestActionOption)_answer).getValue().isAnyLocationAction()) {
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
               if ((_movementRequest == null) || (!_movementRequest.hasMovesLeft())) {
                  _movementRequest = new RequestMovement();
                  Character target = arena.getCharacter(actor._targetID);
                  if (target != null) {
                     HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
                     List<Orientation> validDestinations = new ArrayList<>();
                     List<Orientation> orientationsThatCanAttack = new ArrayList<>();
                     HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient = new HashMap<>();
                     actor.getOrientation().getPossibleChargePathsToTarget(arena.getCombatMap(), actor, target,
                                                                           actor.getAvailableMovement(false/*movingEvasively*/),
                                                                           mapOrientationToNextOrientationsLeadingToChargeAttack);

                     for (List<Orientation> destOrientationList : mapOrientationToNextOrientationsLeadingToChargeAttack.values()) {
                        if (destOrientationList != null) {
                           for (Orientation orient : destOrientationList) {
                              validDestinations.add(orient);
                              List<Orientation> nextOrients = mapOrientationToNextOrientationsLeadingToChargeAttack.get(orient);
                              if (nextOrients == null) {
                                 // this orientation has no place to move forward, so it must be able to attack the target from here
                                 orientationsThatCanAttack.add(orient);
                              }
                              else {
                                 for (Orientation nextOrient : nextOrients) {
                                    mapOfFutureOrientToSourceOrient.put(nextOrient, orient);
                                 }
                              }
                           }
                        }
                     }


                     _movementRequest.setOrientations(validDestinations, mapOfFutureOrientToSourceOrient, actor);
                     //_locationRequest.addOption(0, "move");
                     _movementRequest.setMessage(actor.getName() + ", please select your destination on the arena map.");
                     nextReq = _movementRequest;
                  }
               }
            }
         }
         if (_styleRequest == null) {
            _styleRequest = actor.getRequestAttackStyle(this, arena);
         }
         nextReq = _styleRequest;
      }
      else if (isChangePosition()) {
         if (_positionRequest == null) {
            _positionRequest = actor.getRequestPosition(this);
         }
         nextReq = _positionRequest;
      }
      else if (isAdvance()) { // but not an attack! (isAttack() check above rules that out)
         byte moveAvailable = actor.getAvailableMovement(isEvasiveMove());
         // Can we continue moving? If not, return null
         if ((moveAvailable == 0) || (actor.isMoveComplete())) {
            return null;
         }
         if ((_movementRequest == null) || (!_movementRequest.hasMovesLeft())) {
            _movementRequest = new RequestMovement();
            List<Orientation> futureOrientations = new ArrayList<>();

            HashMap<Orientation, Orientation> mapOfFutureOrientToSourceOrient = new HashMap<>();
            arena.getMoveableLocations(actor, moveAvailable, arena.getCombatMap(), futureOrientations, mapOfFutureOrientToSourceOrient);
            _movementRequest.setOrientations(futureOrientations, mapOfFutureOrientToSourceOrient, actor);
            //_locationRequest.addOption(0, "move");
            _movementRequest.setMessage(actor.getName() + ", please select your destination on the arena map.");
         }
         nextReq = _movementRequest;
      }
      else if (isChangeTargets()) {
         arena.getCombatMap().recomputeKnownLocations(actor, true, true, null);
         if (_targetSelection == null) {
            // If _spell is 'null', this will use the actor's missile weapon for range assessment
            _targetSelection = new RequestSingleTargetSelection(actor, _spell, arena, this);
            nextReq = _targetSelection;
         }
         else {
            // make sure the new order is in place before we ask for the next action.
            int newTargetUniqueID = _targetSelection.getAnswerID();
            if (newTargetUniqueID != -1) {
               actor.setTarget(newTargetUniqueID);
            }
            List<Character> charactersTargetingActor = arena._battle.getCharactersAimingAtCharacter(actor);
            nextReq = actor.getActionRequest(arena, null/*delayedTarget*/, charactersTargetingActor);
            nextReq.copyDataInto(this);
            nextReq = this;
         }
         //         if (_targetPriorities == null) {
         //            _targetPriorities = actor.getTargetPrioritiesRequest(this, arena);
         //            nextReq = _targetPriorities;
         //         }
         //         else {
         //            // make sure the new order is in place before we ask for the next action.
         //            actor.setTargetPriorities(_targetPriorities.getOrderedTargetIds());
         //            List<Character> charactersTargetingActor = arena._battle.getCharactersAimingAtCharacter(actor);
         //            nextReq = actor.getActionRequest(arena, null/*delayedTarget*/, charactersTargetingActor);
         //            nextReq.copyDataInto(this);
         //            nextReq = this;
         //         }
      }
      else if (isEquipUnequip()) {
         if (_equipmentRequest == null) {
            _equipmentRequest = actor.getEquipmentRequest();
         }
         nextReq = _equipmentRequest;
      }
      else if (isBeginSpell()) {
         if (_spellTypeSelectionRequest == null) {
            _spellTypeSelectionRequest = actor.getSpellTypeSelectionRequest();
            nextReq = _spellTypeSelectionRequest;
         }
         else {
            _spellSelectionRequest = (RequestSpellSelection) _spellTypeSelectionRequest.getNextQuestion(actor, combatants, arena);
            nextReq = _spellSelectionRequest;
         }
      }
      else if (isCompleteSpell()) {
         if (_spell.getTargetType() == TargetType.TARGET_AREA) {
            if (_locationSelection == null) {
               _locationSelection = new RequestLocation(((IAreaSpell)_spell).getImageResourceName());
               ArenaCoordinates actorCoords = actor.getOrientation().getHeadCoordinates();
               CombatMap map = arena.getCombatMap();
               ArenaLocation curLoc = map.getLocation(actorCoords);
               List<ArenaCoordinates> visibleLocationsInRange = new ArrayList<>();
               short maxRange = _spell.getMaxRange(actor);
               short minRange = _spell.getMinRange(actor);
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
               _locationSelection.setCoordinates(visibleLocationsInRange);
            }
            nextReq = _locationSelection;
         }
         else if (_spell.requiresTargetToCast()) {
            if (_targetSelection == null) {
               _targetSelection = new RequestSingleTargetSelection(actor, _spell, arena, this);
            }
            nextReq = _targetSelection;
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
               _answer.setEnabled(false);
            }
            init();
            _answer = null;
            if (nextReq == _equipmentRequest) {
               _equipmentRequest           = null;
            }
            else if (nextReq == _movementRequest) {
               _movementRequest            = null;
            }
            else if (nextReq == _positionRequest) {
               _positionRequest            = null;
            }
            else if (nextReq == _styleRequest) {
               _styleRequest               = null;
            }
            else if (nextReq == _targetPriorities) {
               _targetPriorities           = null;
            }
            else if (nextReq == _targetSelection) {
               _targetSelection            = null;
            }
            else if ((nextReq == _spellTypeSelectionRequest) ||
                     (nextReq == _spellSelectionRequest)) {
               _spellTypeSelectionRequest  = null;
               _spellSelectionRequest      = null;
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

   public boolean isRangedAttack()       { return ((RequestActionOption)_answer).getValue().isRangedAttack();       }
   public boolean isGrappleAttack()      { return ((RequestActionOption)_answer).getValue().isGrappleAttack();      }
   public boolean isBreakFree()          { return ((RequestActionOption)_answer).getValue().isBreakFree();          }
   public boolean isCounterAttack()      { return ((RequestActionOption)_answer).getValue().isCounterAttack();      }
   public boolean isCounterAttackThrow() { return ((RequestActionOption)_answer).getValue().isCounterAttackThrow(); }
   public boolean isCounterAttackGrab()  { return ((RequestActionOption)_answer).getValue().isCounterAttackGrab();  }
   public byte getAttackActions(boolean considerSpellAsAttack) { return ((RequestActionOption)_answer).getValue().getAttackActions(considerSpellAsAttack); }
   public byte getActionsUsed()
   {
      return ((RequestActionOption)_answer).getValue().getActionsUsed((_equipmentRequest != null) ?
                                                                                                   _equipmentRequest.getActionsUsed() : 1);
   }
   public String getActionDescription(Character actor, Character target, Arena arena) {
      switch (((RequestActionOption)_answer).getValue()) {
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
         case OPT_ON_GAURD                  : return actor.getName() + " remains poised, on gaurd.";
         case OPT_FINAL_DEFENSE_1         :
         case OPT_FINAL_DEFENSE_2         :
         case OPT_FINAL_DEFENSE_3         :
         case OPT_FINAL_DEFENSE_4         :
         case OPT_FINAL_DEFENSE_5         : return actor.getName() + " makes a final defensive action ("+getActionsUsed()+"-actions).";
         case OPT_CHANGE_POS              : return actor.getName() + " changes "+actor.getHisHer()+" position. (" + _positionRequest.getAnswer() + ")";
         case OPT_WAIT_TO_ATTACK          : return actor.getName() + " waits for an opportunatey to attack.";
         case OPT_EQUIP_UNEQUIP           : return actor.getName() + " equips or unequips gear: " + ((_equipmentRequest == null) ? "" : _equipmentRequest.getAnswer());
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
         case OPT_COMPLETE_SPELL_3        : return actor.getName() + " casts a '"+_spell.getName() +"' spell (" + _spell.getPower()+" power points)";
         case OPT_COMPLETE_PRIEST_SPELL_1 : return actor.getName() + " casts a '"+_spell.getName() +"' spell (1 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_2 : return actor.getName() + " casts a '"+_spell.getName() +"' spell (2 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_3 : return actor.getName() + " casts a '"+_spell.getName() +"' spell (3 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_4 : return actor.getName() + " casts a '"+_spell.getName() +"' spell (4 power points)";
         case OPT_COMPLETE_PRIEST_SPELL_5 : return actor.getName() + " casts a '"+_spell.getName() +"' spell (5 power points)";
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
      if (((RequestActionOption)_answer).getValue().isAnyLocationAction()) {
         return arena.getActionDescription(actor, this);
      }

      return "";
   }
   @Override
   public void serializeFromStream(DataInputStream in)
   {
      super.serializeFromStream(in);
      try {
         _actorID       = readInt(in);
         _targetID      = readInt(in);
         byte limbVal   = readByte(in);
         _limbType      = (limbVal == -1) ? null : LimbType.getByValue(limbVal);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public void serializeToStream(DataOutputStream out)
   {
      super.serializeToStream(out);
      try {
         writeToStream(_actorID, out);
         writeToStream(_targetID, out);
         writeToStream((_limbType == null) ? ((byte)-1) : _limbType.value, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   @Override
   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         super.copyDataInto(newObj);
         if (newObj instanceof RequestAction) {
            RequestAction reqAct = (RequestAction) newObj;
            reqAct._actorID                   = _actorID;
            reqAct._equipmentRequest          = _equipmentRequest;
            reqAct._movementRequest           = _movementRequest;
            reqAct._positionRequest           = _positionRequest;
            reqAct._styleRequest              = _styleRequest;
            reqAct._targetPriorities          = _targetPriorities;
            reqAct._spellTypeSelectionRequest = _spellTypeSelectionRequest;
            reqAct._spellSelectionRequest     = _spellSelectionRequest;
            reqAct._targetSelection           = _targetSelection;
            reqAct._targetID                  = _targetID;
            reqAct._limbType                      = _limbType;
         }
      }
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder(super.toString());
      sb.append(", actorID = ").append(_actorID);
      sb.append(", equReq = ").append(_equipmentRequest);
      sb.append(", movReq = ").append(_movementRequest);
      sb.append(", posReq = ").append(_positionRequest);
      sb.append(", styReq = ").append(_styleRequest);
      sb.append(", targetPriorities = ").append(_targetPriorities);
      sb.append(", spellTypeSelectionRequest = ").append(_spellTypeSelectionRequest);
      sb.append(", spellSelectionRequest = ").append(_spellSelectionRequest);
      sb.append(", targetSelection = ").append(_targetSelection);
      sb.append(", targetID = ").append(_targetID);
      sb.append(", hand = ").append(_limbType);
      return sb.toString();
   }

   public RANGE getRange(Character attacker, short distanceInHexes) {
      byte rangedDeterminingAttribute = 0;
      if ((_styleRequest != null) && (_styleRequest.isRanged())) {
         rangedDeterminingAttribute = attacker.getAdjustedStrength();
      }
      if (_spell != null) {
         if (_spell instanceof IRangedSpell) {
            // When an attacker has a web spell ready, but they choose to attack with a weapon instead,
            // don't let the spell affect the range results. This results in the range being returned
            // as Point_blank, or similar, when a melee attack is taking place.
            if (!isAttack()) {
               if (_spell instanceof SpellSpiderWeb) {
                  SpellSpiderWeb web = (SpellSpiderWeb) _spell;
                  return web.getRange(distanceInHexes);
               }
               rangedDeterminingAttribute = attacker.getAttributeLevel(_spell.getCastingAttribute());
               rangedDeterminingAttribute += attacker.getRace().getBuildModifier();
            }
         }
      }
      WeaponStyleAttack attackingWeapon = getAttackStyle(attacker);
      if ((attackingWeapon == null) || !(attackingWeapon instanceof WeaponStyleAttackRanged)) {
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
      if (_styleRequest != null) {
         if (_styleRequest.isGrapple()) {
            return weapon.getGrappleStyle(_styleRequest.getAnswerIndex());
         }
         return weapon.getAttackStyle(_styleRequest.getAnswerIndex());
      }
      return weapon.getAttackStyle(0);
   }

   public Weapon getAttackingWeapon(Character attacker) {
      if (_styleRequest != null) {
         return attacker.getLimb(getLimb()).getWeapon(attacker);
      }
      if (_spell != null) {
         if (_spell instanceof IMissileSpell) {
            IMissileSpell missileSpell = (IMissileSpell) _spell;
            return missileSpell.getMissileWeapon();
         }
      }
      return null;
   }

   public boolean isRanged() {
      if (_styleRequest != null) {
         return _styleRequest.isRanged();
      }

      if (_spell != null) {
         return _spell instanceof IRangedSpell;
      }
      return false;
   }
   public DamageType getDamageType()
   {
      if (_styleRequest != null) {
         return _styleRequest.getDamageType();
      }

      if (_spell != null) {
         return _spell.getDamageType();
      }
      return ostrowski.combat.common.enums.DamageType.NONE;
   }
   public String getCantRetreatOrParryFromAttackTypeString()
   {
      if (isCharge()) {
         return "charge attack.";
      }
      if (_styleRequest != null) {
         if (_styleRequest.isThrown()) {
            return "thrown weapon.";
         }
         if (_styleRequest.isMissile()) {
            return "missile weapon.";
         }
      }
      if (_spell != null) {
         if (_spell instanceof IMissileSpell) {
            return _spell.getName() + " spell.";
         }
      }
      return null;
   }
   public void setSpell(Spell spell) {
      _spell = spell;
   }
   public Spell getSpell() {
      return _spell;
   }
   public WeaponStyleAttack getWeaponStyleAttack(Character attacker)
   {
      if (isCompleteSpell()) {
         if (_spell instanceof IMissileSpell) {
            IMissileSpell missileSpell = (IMissileSpell) _spell;
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
         if (_styleRequest != null) {
            return attackingWeapon.getAttackStyle(_styleRequest.getAnswerIndex());
         }
         return attackingWeapon.getAttackStyle(0);
      }
      return null;
   }

   public DiceSet getAttackDice(RANGE range) {
      if (_styleRequest != null) {
         return _styleRequest.getAttackDice();
      }
      if (isCompleteSpell()) {
         return _spell.getCastDice(getActionsUsed(), range);
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
            if (_spell instanceof PriestMissileSpell) {
               rollModifier += attacker.getAttributeLevel(Attribute.Dexterity) + ((PriestSpell)_spell).getAffinity();
            }
            rollModifier += _spell.getLevel();
         }
         if (includeWoundsAndPain) {
            rollModifier -= attacker.getWoundsAndPainPenalty();
         }
      }
      else {
         if (includeSkill) {
            rollModifier += attacker.getWeaponSkill(getLimb(), _styleRequest.getAnswerIndex(), _styleRequest.isGrapple(),
                                                    _styleRequest.isCounterAttack(),
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
      if (isCompleteSpell() && (_spell instanceof IMissileSpell)) {
         IMissileSpell missileSpell = (IMissileSpell) _spell;
         return missileSpell.getSpellDamageBase();
      }
      WeaponStyleAttack style = getWeaponStyleAttack(attacker);
      if (style != null) {
         return (byte) (style.getDamageMod() + attacker.getPhysicalDamageBase());
      }
      return 0;
   }
   public void setDualGrappleAttack() {
      _dualGrappleAttack = true;
   }
   public boolean isDualGrappleAttack() {
      return _dualGrappleAttack;
   }
}
