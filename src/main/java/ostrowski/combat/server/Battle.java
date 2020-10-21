package ostrowski.combat.server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.*;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.spells.Spell;
import ostrowski.combat.common.spells.priest.PriestMissileSpell;
import ostrowski.combat.common.spells.priest.PriestSpell;
import ostrowski.combat.common.spells.priest.evil.SpellEnrage;
import ostrowski.combat.common.things.Hand;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.combat.common.wounds.Wound;
import ostrowski.combat.common.wounds.WoundChart;
import ostrowski.combat.common.wounds.WoundCharts;
import ostrowski.combat.protocol.BeginBattle;
import ostrowski.combat.protocol.request.*;
import ostrowski.protocol.IRequestOption;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.CombatSemaphore;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoTracker;

import java.util.*;

public class Battle extends Thread implements Enums {
   public final Arena _arena;
   int _turnCount  = 1;
   int _roundCount = 1;
   int _phaseCount = 1;
   public boolean      _startMidTurn              = false;
   public       boolean      _resetMessageBufferOnStart = true;
   public final CombatServer _combatServer;
   public       int          _turnPause                 = 1000;
   public int          _roundPause                = 1000;
   public int          _phasePause                = 1000;

   public void onPause() {
      _turnPause = _turnCount;
      _roundPause = _roundCount;
      _phasePause = _phaseCount;
   }

   public int getTimeID() {
      return (_turnCount * 100) + (_roundCount * 10) + _phaseCount;
   }

   public void onPlay() {
      _turnPause = 10000;
      _roundPause = 10000;
      _phasePause = 10000;
   }

   public void forceNewTurn() {
      _phaseCount = 1;
      _roundCount = 1;
   }

   public void onTurnAdvance() {
      _phasePause = 1;
      _roundPause = 1;
      _turnPause = _turnCount + 1;
   }

   public void onRoundAdvance() {
      _phasePause = 1;
      _roundPause = _roundCount + 1;
      _turnPause = _turnCount;
   }

   public void onPhaseAdvance() {
      _phasePause = _phaseCount + 1;
      _roundPause = _roundCount;
      _turnPause = _turnCount;
   }

   private void checkForPause() {
      /*synchronized (_waitingObjects)*/
      {
         Rules.diag("### inside lock checkForPause, of waitingList = @" + Integer.toHexString(_waitingObjects.hashCode()) + ": " + _waitingObjects);
         if (_turnCount > _turnPause) {
            _combatServer.waitForPlay(_waitingObjects);
         } else if (_turnCount == _turnPause) {
            if (_roundCount > _roundPause) {
               _combatServer.waitForPlay(_waitingObjects);
            }
            if (_roundCount == _roundPause) {
               if (_phaseCount >= _phasePause) {
                  _combatServer.waitForPlay(_waitingObjects);
               }
            }
         }
      }
   }

   public Battle(String threadName, Arena arena, CombatServer combatServer) {
      super(threadName);
      _arena = arena;
      _combatServer = combatServer;
   }

   @Override
   public void run() {
      if (!_startMidTurn) {
         // Initialize our current turn/round/phase information:
         _turnCount = 1;
         _roundCount = 1;
         _phaseCount = 1;
      }
      if (_resetMessageBufferOnStart) {
         _combatServer.resetMessageBuffer();
      }
      _combatServer.onNewBattle();
      StringBuilder sb = new StringBuilder();
      for (Character combatant : _arena.getCombatants()) {
         sb.append("<br/>");
         sb.append(combatant.getName()).append(" has entered the arena on team ").append(TEAM_NAMES[combatant._teamID]).append(".<br/>");
         sb.append(combatant.print()).append(".<br/>");
      }
      _arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);

      _arena.sendMessageTextToAllClients("<HR/>Battle Begins! <HR/>", false/*popUp*/);
      _arena.sendMessageTextToAllClients("Pseudo-random number seed = " + CombatServer.getPseudoRandomNumberSeed() + "<br/>", false/*popUp*/);
      _arena.sendMessageTextToAllClients("server version " + CombatServer.getVersionNumber() + "<br/>", false/*popUp*/);
      BeginBattle battleMsg = new BeginBattle();
      _arena.sendEventToAllClients(battleMsg);
      _arena.clearStartingPointLabels();
      while (_arena.stillFighting()) {
         try {
            executeTurn(_startMidTurn);
            _startMidTurn = false;
         } catch (BattleTerminatedException e) {
            _arena.sendMessageTextToAllClients("<HR/>Battle terminated! <HR/>", false/*popUp*/);
            _arena._characterGenerated = false;
            return;
         } catch (Throwable e) {
            _arena.sendMessageTextToAllClients(e.toString(), false/*popUp*/);
            e.printStackTrace();
         }
      }
      _arena.sendMessageTextToAllClients("<HR/>Battle Complete! <HR/>", false/*popUp*/);
      _arena.onBattleComplete(this);
      // reset for the next battle:
      CombatServer.resetPseudoRandomNumberGenerator();
   }

   private       boolean      _terminateBattle = false;
   private final List<Object> _waitingObjects  = new ArrayList<>();

   public void terminateBattle() {
      _terminateBattle = true;
      /*synchronized (_waitingObjects)*/
      {
         Rules.diag("### inside lock terminateBattle, of waitingList = @" + Integer.toHexString(_waitingObjects.hashCode()) + ": " + _waitingObjects);
         while (_waitingObjects.size() > 0) {
            Object element = _waitingObjects.remove(0);
            synchronized (element) {
               Rules.diag("### about to notifyAll @" + Integer.toHexString(element.hashCode()) + ": " + element);
               element.notifyAll();
            }
         }
      }
      synchronized (_waitingToAttack) {
         _lock_waitingToAttack.check();
         _waitingToAttack.clear();
      }
      synchronized (_aimingCharacters) {
         _lock_aimingCharacters.check();
         _aimingCharacters.clear();
      }
      // reset for the next battle:
      CombatServer.resetPseudoRandomNumberGenerator();
   }

   private void executeTurn(boolean startMidTurn) throws BattleTerminatedException {
      checkForPause();
      _arena.sendMessageTextToAllClients("<HR/><H3>Turn " + _turnCount + ":</H3>", false/*popUp*/);
      if (!startMidTurn) {
         _turnCount++;
         resolvePainAndInitiative();
         _roundCount = 1;
      }

      // First let everyone with the highest actions go first.
      // When there is a tie, let those with the higher initiative go first.
      List<Character> combatants = _arena.orderCombatantsByActionsAndInitiative();
      List<Character> activeCombatants = new ArrayList<>();
      do {
         activeCombatants.clear();
         synchronized (combatants) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_arena._lock_combatants)) {
               for (Character combatant : combatants) {
                  if (combatant.stillFighting()) {
                     if (combatant.getActionsAvailable(false/*usedForDefenseOnly*/) > 0) {
                        activeCombatants.add(combatant);
                     }
                  }
               }
            }
         }
         if (activeCombatants.size() == 0) {
            // The turn ends when no fighting combatants have any actions available.
            return;
         }
      } while (executeRound(activeCombatants, combatants) && _arena.stillFighting());
      synchronized (combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_arena._lock_combatants)) {
            for (Character combatant : combatants) {
               combatant.completeTurn(_arena);
            }
         }
      }
   }

   // return 'true' if any actions remains to be spent by any character
   private boolean executeRound(List<Character> activeCombatants, List<Character> allCombatants) throws BattleTerminatedException {

      _arena.getCombatMap().onNewRound(this);

      // Put all the combatants into a List of Lists. Characters in the same inner List
      // will act at the same time as each other. The outer list will be gone through
      // in order to determine who acts when.
      // combatants with higher available actions will appear higher in the outer list.
      // combatants with higher initiative will appear higher in the outer list.
      // combatants with the same available actions and initiative will appear in the same inner list,
      // which is contained by the outer list.
      List<List<Character>> outerList = new ArrayList<>();
      for (Character combatant : activeCombatants) {
         if (combatant.stillFighting()) {
            byte actionsChar = combatant.getActionsAvailable(false/*usedForDefenseOnly*/);
            if (actionsChar > 0) {
               boolean inserted = false;
               byte initChar = combatant.getInitiative();
               for (int i = 0; i < outerList.size(); i++) {
                  List<Character> innerList = outerList.get(i);
                  Character charComp = innerList.get(0);
                  byte actionsComp = charComp.getActionsAvailable(false/*usedForDefenseOnly*/);
                  byte initComp = charComp.getInitiative();
                  if ((actionsComp < actionsChar) || ((actionsComp == actionsChar) && (initComp < initChar))) {
                     // insert before
                     List<Character> newInnerList = new ArrayList<>();
                     newInnerList.add(combatant);
                     outerList.add(i, newInnerList);

                     inserted = true;
                     break;
                  }
                  if ((actionsComp == actionsChar) && (initComp == initChar)) {
                     // insert into
                     innerList.add(combatant);
                     inserted = true;
                     break;
                  }
               }
               if (!inserted) {
                  List<Character> newInnerList = new ArrayList<>();
                  newInnerList.add(combatant);
                  outerList.add(newInnerList);
               }
            }
         }
      }
      checkForPause();
      // At this point, all the combatants that are still fighting are in an ordered list of lists.
      // now we go through the outer list, asking each combatant in the inner list to choose their
      // action, then we resolve their action.
      StringBuilder sb = new StringBuilder();
      sb.append("<H4>Round ").append(_roundCount++).append(":</H4>");
      synchronized (allCombatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_arena._lock_combatants)) {
            for (Character combatant : allCombatants) {
               if (combatant.stillFighting()) {
                  sb.append("<br/>(").append(combatant.getName());
                  sb.append(": actions=");
                  int defendableActions = combatant.getActionsAvailable(true/*usedForDefenseOnly*/);
                  sb.append(defendableActions);
                  if ((defendableActions > 0) && (combatant.getActionsAvailable(false/*usedForDefenseOnly*/) == 0)) {
                     sb.append(" (def only)");
                  }
                  List<String> weapons = new ArrayList<>();
                  boolean printHands = false;
                  for (LimbType limbType : combatant.getRace().getLimbSet()) {
                     Limb limb = combatant.getLimb(limbType);
                     if (limb != null) {
                        Weapon weap = limb.getWeapon(combatant);
                        if (weap != null) {
                           if (!weap.isReal()) {
                              Limb otherLimb = combatant.getLimb(limbType.getPairedType());
                              if (otherLimb != null) {
                                 Weapon otherWeap = otherLimb.getWeapon(combatant);
                                 if ((otherWeap != null) && (otherWeap.isOnlyTwoHanded())) {
                                    break;
                                 }
                              }
                           }
                           if (weapons.contains(weap.getName())) {
                              printHands = true;
                           } else {
                              weapons.add(weap.getName());
                           }
                        }
                     }
                  }
                  if (weapons.size() > 0) {
                     for (LimbType limbType : combatant.getRace().getLimbSet()) {
                        Limb limb = combatant.getLimb(limbType);
                        if (limb != null) {
                           Weapon weap = limb.getWeapon(combatant);
                           if (weap != null) {
                              byte weaponState = limb.getActionsNeededToReady();
                              if ((limb instanceof Hand) || (weaponState != 0)) {
                                 if (!weap.isReal()) {
                                    Limb otherHand = combatant.getLimb(limbType.getPairedType());
                                    if (otherHand != null) {
                                       Weapon otherWeap = otherHand.getWeapon(combatant);
                                       if ((otherWeap != null) && (otherWeap.isOnlyTwoHanded())) {
                                          break;
                                       }
                                    }
                                 }
                                 sb.append(", ").append(weap.getName());
                                 if (printHands) {
                                    sb.append("{").append(limb.getName()).append("}");
                                 }
                                 sb.append("=");
                                 if (weaponState == 0) {
                                    //WeaponStyleAttackRanged rangedStyle = hand.getRangedStyle();
                                    //if ((rangedStyle != null) && (hand.getPreparedState() >= 0))
                                    //   sb.append(rangedStyle.getPreparationStepName(heldThing.getName(), hand.getPreparedState()));
                                    //else
                                    sb.append("ready");
                                 } else {
                                    sb.append(weaponState).append(" actions");
                                 }
                              }
                           }
                        }
                     }
                  }
                  sb.append(") ");
               }
            }
         }
      }
      _arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);

      _phaseCount = 1;
      while (outerList.size() > 0) {
         List<Character> innerList = outerList.remove(0);
         if (innerList.size() > 0) {
            checkForPause();
            _phaseCount++;
            Character actingChar = innerList.get(0);
            sb.setLength(0);
            sb.append("<b>actions=").append(actingChar.getActionsAvailable(false/*usedForDefenseOnly*/));
            sb.append(", initiative=").append(actingChar.getInitiative()).append(":</b><br/>");
            _arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
            Map<Character, RequestAction> results = new HashMap<>();
            if (selectActions(innerList, results, allCombatants)) {
               Map<Character, List<Wound>> wounds = new HashMap<>();
               Map<Character, List<Spell>> spells = new HashMap<>();
               applyActions(results, wounds, spells);
               // Now that all simultaneous attacks have been resolved, apply any wound damage
               if (wounds.size() > 0) {
                  applyWounds(wounds);
               }
               if (spells.size() > 0) {
                  applySpells(spells);
               }
            }
            CombatServer._this._map.applyAnimations();
         }
      }
      boolean charactersWithActionsRemaining = false;
      synchronized (allCombatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_arena._lock_combatants)) {
            for (Character combatant : allCombatants) {
               if (combatant.endRound()) {
                  charactersWithActionsRemaining = true;
               }
            }
         }
      }
      _arena.getCombatMap().onEndRound(this);

      return charactersWithActionsRemaining;
   }

   private void applyActions(Map<Character, RequestAction> results,
                             Map<Character, List<Wound>> wounds,
                             Map<Character, List<Spell>> spells)
           throws BattleTerminatedException {
      TreeSet<Character> actors = new TreeSet<>(results.keySet());
      for (Character actor : actors) {
         RequestAction action = results.get(actor);
         if (!action.isTargetEnemy()) {
            clearAimingCharacterMap(actor);
         }
         if (action.isWaitForAttack()) {
            addWaitingForAttack(actor);
         } else {
            clearWaitingForAttackMap(actor);
         }
         Spell currentSpell = actor.getCurrentSpell(false/*eraseCurrentSpell*/);
         if (currentSpell != null) {
            if (currentSpell instanceof PriestSpell) {
               if (action.isAttack() || action.isTargetEnemy()) {
                  _arena.sendMessageTextToAllClients(actor.getName() + "'s action terminates his " + currentSpell.getName() + " spell.", false);
                  actor.getCurrentSpell(true/*eraseCurrentSpell*/);
               }
            }
         }
         int targetID = action._targetID;
         if (action.isCompleteSpell()) {
            if (action._targetSelection != null) {
               targetID = action._targetSelection.getAnswerID();
            }
         }
         // All movement actions have already been applied.
         if (action.isBreakFree()) {
            boolean found = false;
            for (IHolder holder : actor.getHolders()) {
               if (actor.getPlacedIntoHoldThisTurn(holder)) {
                  continue;
               }
               RequestGrapplingHoldMaintain grappleMaintain = holder.getGrapplingHoldMaintain(actor, action, _arena);
               if (holder instanceof Character) {
                  sendRequestToCombatant((Character) holder, grappleMaintain);
               }
               holder.applyHoldMaintenance(grappleMaintain, _arena);
               if (resolveBreakFree(actor, holder, action, grappleMaintain)) {
                  // If the break free succeeded, then the actor is no longer held by the holder
                  actor.setHoldLevel(holder, null);
               }
               found = true;
               break;
            }
            if (!found) {
               DebugBreak.debugBreak();
            }
         }
         Character target = _arena.getCharacter(targetID);
         boolean spellResolved = false;
         if (action.isCompleteSpell()) {
            if (currentSpell != null) {
               if (!currentSpell.affectsMultipleTargets()) {
                  if (target != null) {
                     if (currentSpell.canTarget(actor, target) == null) {
                        // beneficial spells only affect allies,
                        // and non beneficial spells only affect enemies.
                        if (currentSpell.isBeneficial() != actor.isEnemy(target)) {
                           if (action.isDefendable()) {
                              resolveDefendableAction(actor, action, target, wounds, spells);
                           } else {
                              currentSpell.setTarget(target);
                              currentSpell.completeSpell();
                              currentSpell.resolveSpell(action, null/*defense*/, spells, wounds, this);
                           }
                           spellResolved = true;
                        }
                     }
                  }
               }
               else { // currentSpell.affectsMultipleTargets()
                  for (Character combatant : _arena.getCombatants()) {
                     if (currentSpell.canTarget(actor, combatant) == null) {
                        // beneficial spells only affect allies,
                        // and non beneficial spells only affect enemies.
                        if (currentSpell.isBeneficial() != actor.isEnemy(combatant)) {
                           if (action.isDefendable()) {
                              resolveDefendableAction(actor, action, combatant/*target*/, wounds, spells);
                           } else {
                              currentSpell.setTarget(combatant);
                              currentSpell.completeSpell();
                              currentSpell.resolveSpell(action, null/*defense*/, spells, wounds, this);
                           }
                           spellResolved = true;
                        }
                     }
                  }
               }
            }
            if (spellResolved) {
               actor.getCurrentSpell(true/*eraseCurrentSpell*/);
            }
         }
         if (!spellResolved) {
            if (target != null) {
               if (action.isDefendable()) {
                  resolveDefendableAction(actor, action, target, wounds, spells);
                  spellResolved = action.isCompleteSpell();
               }
               // TODO: isRetreat() ALWAYS returns false
               else if (action.isRetreat()) {
                  ArenaLocation attackFromLocation = actor.getAttackFromLocation(action, _arena.getCombatMap());
                  _arena.moveToFrom(actor, null, attackFromLocation, null/*attackFromLimb*/);
               } else if (action.isTargetEnemy()) {
                  addAimingCharacter(actor, target);
               }
            }
            if (action.isCompleteSpell()) {
               // Have we already dealt with this spell?
               if (!spellResolved) {
                  Spell spell = actor.getCurrentSpell(true/*eraseCurrentSpell*/);
                  spell.setTarget(target);
                  if (spell instanceof IAreaSpell) {
                     ArenaCoordinates coord = action._locationSelection.getAnswerCoordinates();
                     ((IAreaSpell) spell).setTargetLocation(_arena.getLocation(coord), _arena);
                  }
                  spell.completeSpell();
                  spell.resolveSpell(action, null/*defense*/, spells, wounds, this);
               }
            }
         }
         if (action.isPickupItem(actor, _arena)) {
            int itemIndex = action.getAnswerIndex();
            IRequestOption answer = action.answer();
            if (answer instanceof RequestActionOption) {
               RequestActionOption reqActOpt = (RequestActionOption) answer;
               itemIndex = reqActOpt.getValue().getIndexOfLocationAction();
            }
            Object thing = _arena.pickupItem(actor, action, itemIndex, _combatServer._diag);
            if (!actor.pickupObject(thing)) {
               actor.getLimbLocation(action.getLimb(), _arena.getCombatMap()).addThing(thing);
               // The ArenaLocation is a monitoredObject, it will report
               // the change to any watcher of the location.
               //else
               //   _arena.sendServerStatus(null, false/*fullMap*/, false/*recomputeVisibility*/);
            }
         } else {
            // add the original locations of the target to the list of locations
            // that must be redrawn
            Collection<ArenaCoordinates> locationsToRedraw = new ArrayList<>(actor.getCoordinates());
            _arena.sendCharacterUpdate(actor, locationsToRedraw);
         }
         if (action.isLocationAction(actor, _arena)) {
            _arena.applyAction(actor, action);
         }

         // Attacks have already been described, don't describe them again.
         if (!action.isDefendable()) {
            _arena.sendMessageTextToAllClients(action.getActionDescription(actor, target, _arena), false/*popUp*/);
         }
      }
   }

   private void resolveDefendableAction(Character actor, RequestAction action, Character target,
                                        Map<Character, List<Wound>> wounds,
                                        Map<Character, List<Spell>> spells)
           throws BattleTerminatedException {
      // add the original locations of the target to the list of locations
      // that must be redrawn
      Collection<ArenaCoordinates> targetsLocationsToRedraw = new ArrayList<>(target.getCoordinates());

      // If this is a PriestMissileSpell, the effective power has not been computed yet,
      // so there is no way to set the estimated damage, so the AI can't defend appropriately!
      if (action.isCompletePriestSpell()) {
         Spell spell = action.getSpell();
         if (spell instanceof PriestMissileSpell) {
            spell.setTarget(target);
            short minDistanceInHexes = Arena.getMinDistance(actor, target);
            RANGE range = spell.getRange(minDistanceInHexes);
            spell.getCastingPower(action, minDistanceInHexes, range, this, null);
         }
      }
      RequestDefense defense = target.getDefenseRequest(0, actor, action, _arena, false/*forCounterAttack*/);
      sendRequestToCombatant(target, defense);
      target.applyDefense(defense, _arena);
      // all movement actions occur as the action is received, so we don't need to do this here.
      //if (action.isAdvance()) {
      //   _arena.moveToFrom(actor, target, false/*isRetreat*/);
      //}
      Orientation originalOrientation = target.getOrientation();
      if (defense.isRetreat()) {
         ArenaLocation attackFromLocation = actor.getAttackFromLocation(action, _arena.getCombatMap());
         _arena.moveToFrom(target, actor, attackFromLocation/*retreatFromLocation*/, null/*attackFromLimb*/);
      }
      //               if (action.isChannelEnergy()) {
      //               }
      boolean actionSuccessful;
      if (action.isCompleteSpell()) {
         Spell spell = actor.getCurrentSpell(true/*eraseCurrentSpell*/);
         spell.setTarget(target);
         actionSuccessful = spell.resolveSpell(action, defense, spells, wounds, this);
      } else {
         actionSuccessful = resolveAttack(actor, action, target, defense, wounds);
      }
      // If this is a successful grapple attack, and the defender retreated,
      // then the defender must return to his original (pre-retreat) location.
      if (actionSuccessful) {
         if (action.isGrappleAttack() || action.isDualGrappleAttack()) {
            if (defense.isRetreat()) {
               _arena.moveCharacter(target, originalOrientation);
            }
            if (Arena.getMinDistance(actor, target) > 1) {
               // if the actor is still not next to the target, move the actor forward:
               _arena.moveToFrom(actor, target, null, null/*attackFromLimb*/);
            }
         }
      } else {
         if (defense.getDefenseOptions().isCounterAttack()) {
            // Apply the counter-attack
            resolveCounterAttack(target, defense, actor, wounds, targetsLocationsToRedraw);
         }
      }
      // add the new current locations of the target to the list of locations
      // that must be redrawn
      targetsLocationsToRedraw.addAll(target.getCoordinates());

      _arena.sendCharacterUpdate(target, targetsLocationsToRedraw);
   }

   private void resolveCounterAttack(Character counterAttacker, RequestDefense defense, Character counterAttackTarget,
                                     Map<Character, List<Wound>> wounds,
                                     Collection<ArenaCoordinates> targetsLocationsToRedraw) throws BattleTerminatedException {
      int actionsUsed = RequestDefense.getDefenseCounterActions(defense.getAnswerID());


      RequestAction counterAttack = new RequestAction(counterAttacker._uniqueID, counterAttackTarget._uniqueID);
      LimbType limbType = LimbType.HAND_RIGHT;
      DefenseOptions defOpts = defense.getDefenseOptions();
      if (defOpts.isCounterAttackGrab()) {
         // This is a grab
         counterAttack.addOption(new RequestActionOption("Counter attack grab (1-action)", RequestActionType.OPT_COUNTER_ATTACK_GRAB_1, limbType, (actionsUsed == 1)/*enabled*/));
         counterAttack.addOption(new RequestActionOption("Counter attack grab (2-action)", RequestActionType.OPT_COUNTER_ATTACK_GRAB_2, limbType, (actionsUsed == 2)/*enabled*/));
         counterAttack.addOption(new RequestActionOption("Counter attack grab (3-action)", RequestActionType.OPT_COUNTER_ATTACK_GRAB_3, limbType, (actionsUsed == 3)/*enabled*/));
         counterAttack.setAnswerByOptionIndex(actionsUsed - 1);
      } else if (defOpts.isCounterAttackThrow()) {
         // This is a throw
         counterAttack.addOption(new RequestActionOption("Counter attack throw (1-action)", RequestActionType.OPT_COUNTER_ATTACK_THROW_1, limbType, (actionsUsed == 1)/*enabled*/));
         counterAttack.addOption(new RequestActionOption("Counter attack throw (2-action)", RequestActionType.OPT_COUNTER_ATTACK_THROW_2, limbType, (actionsUsed == 2)/*enabled*/));
         counterAttack.addOption(new RequestActionOption("Counter attack throw (3-action)", RequestActionType.OPT_COUNTER_ATTACK_THROW_3, limbType, (actionsUsed == 3)/*enabled*/));
         counterAttack.setAnswerByOptionIndex(actionsUsed - 1);
         targetsLocationsToRedraw.addAll(counterAttackTarget.getCoordinates());
      }

      RequestDefense counterDefense = counterAttackTarget.getDefenseRequest(0, counterAttacker, counterAttack, _arena, true/*forCounterAttack*/);
      sendRequestToCombatant(counterAttackTarget, counterDefense);

      counterAttack._styleRequest = counterAttacker.getRequestAttackStyle(counterAttack, _arena);
      counterAttack._styleRequest.setAnswerByOptionIndex(actionsUsed - 1);
      //sendRequestToCombatant(counterAttacker, counterAttack._styleRequest);

      resolveAttack(counterAttacker, counterAttack, counterAttackTarget, counterDefense, wounds);

      counterAttackTarget.applyDefense(counterDefense, _arena);

      DefenseOptions counterDefOpts = counterDefense.getDefenseOptions();
      int actionsCount = counterDefOpts.getDefenseCounterDefenseActions();
      byte nim = counterAttackTarget.getAttributeLevel(Attribute.Nimbleness);
      byte enc = Rules.getEncumbranceLevel(counterAttackTarget);
      byte TN = (byte) ((nim - enc - 10) + (actionsCount * 5));
      boolean success;
      if (actionsCount == 0) {
         // Auto-success
         success = true;
      } else {
         //
         success = (TN > 7);
      }
   }

   private boolean resolveBreakFree(Character actor, IHolder holder, RequestAction action, RequestGrapplingHoldMaintain grappleMaintain) {
      int TN = grappleMaintain.getTn();
      byte str = actor.getAttributeLevel(Attribute.Strength);
      byte nim = actor.getAttributeLevel(Attribute.Nimbleness);
      Attribute bestAttr = str > nim ? Attribute.Strength : Attribute.Nimbleness;
      DiceSet dice = Rules.getDice((byte) Math.max(str, nim), action.getActionsUsed(), bestAttr/*attribute*/, RollType.BREAK_FREE);
      String rollMessage = actor.getName() + ", your " + bestAttr.name() + " will be used to break free (" +
                           action.getActionsUsed() + " actions), roll to attempt the escape.";
      int roll = dice.roll(true/*allowExplodes*/, actor, RollType.BREAK_FREE, rollMessage);
      int wrestlingSkill = 0;
      int aikidoSkill = 0;
      int brawlingSkill = 0;
      if (!(holder instanceof Spell)) {
         wrestlingSkill = actor.getSkillLevel(SkillType.Wrestling, null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
         aikidoSkill = actor.getSkillLevel(SkillType.Aikido, null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
         brawlingSkill = actor.getSkillLevel(SkillType.Brawling, null, false/*sizeAdjust*/, true/*adjustForEncumbrance*/, false/*adjustForHolds*/);
      }
      int skill = Math.max(wrestlingSkill, Math.max(aikidoSkill, brawlingSkill));
      int skillPenalty = 0; // TODO: ??
      int positionAdjustment = 0;// TODO: ??
      int actorPainAndWounds = actor.getWoundsAndPainPenalty();

      StringBuilder sb = new StringBuilder();
      sb.append(actor.getName()).append(" tries to break free from ").append(holder.getName()).append(":");
      sb.append(holder.getName()).append(" uses ").append(grappleMaintain.getActionsUsed()).append(" actions to maintain the hold:");
      sb.append(grappleMaintain.getTnExplanation());

      dice = actor.adjustDieRoll(dice, RollType.BREAK_FREE, holder/*target*/);
      sb.append(actor.getName()).append(" rolls ").append(dice);
      sb.append(" (").append(action.getActionsUsed()).append("-actions ");
      if (str > nim) {
         sb.append("STR of ").append(str);
      } else {
         sb.append("NIM of ").append(nim);
      }
      sb.append("), rolling ").append(dice.getLastDieRoll());

      sb.append("<table border=1><tr><td>").append(roll).append("</td><td>dice roll</td></tr>");
      sb.append("<tr><td>+").append(skill).append("</td><td>grappling skill");
      if (skill != 0) {
         sb.append(" (");
         if (skill == wrestlingSkill) {
            sb.append(SkillType.Wrestling.getName());
         } else if (skill == aikidoSkill) {
            sb.append(SkillType.Aikido.getName());
         } else {// (skill == brawlingSkill)
            sb.append(SkillType.Brawling.getName());
         }
         sb.append(")");
      }
      sb.append("</td></tr>");
      if (skillPenalty != 0) {
         sb.append("<tr><td>-").append(skillPenalty).append("</td><td>skill penalty for weapon style</td></tr>");
         skill -= skillPenalty;
      }
      if (positionAdjustment != 0) {
         sb.append("<tr><td>").append(positionAdjustment).append("</td><td>penalty for ").append(actor.getPositionName()).append("</td></tr>");
         skill += positionAdjustment;
      }
      if (actorPainAndWounds != 0) {
         sb.append("<tr><td>").append(-actorPainAndWounds).append("</td><td>pain and wounds</td></tr>");
         skill -= actorPainAndWounds;
      }
      byte result = (byte) (roll + skill);
      sb.append("<tr><td><b>").append(result).append("</b></td>");
      sb.append("<td>final roll</td></tr>");
      sb.append("<tr><td>").append(TN).append("</b></td>");
      sb.append("<td>holders TN</td></tr>");

      boolean escape = (result >= TN);
      if (dice.lastRollRolledAllOnes()) {
         sb.append("<tr><td colspan=2>automatic failure (all 1s)</td></tr>");
         escape = false;
      } else if (escape) {
         sb.append("<tr><td colspan=2>successful escape</td></tr>");
      } else {
         sb.append("<tr><td colspan=2>unsuccessful escape</td></tr>");
      }
      sb.append("</table>");

      _arena.sendMessageTextToAllClients(sb.toString(), false/*popUp*/);
      return escape;
   }

   private void sendRequestToCombatant(Character combatant, SyncRequest request) throws BattleTerminatedException {
      _arena.sendObjectToCombatant(combatant, request);
      /*synchronized (_waitingObjects)*/
      {
         Rules.diag("### inside lock applyActions, of waitingList = @" + Integer.toHexString(_waitingObjects.hashCode()) + ": " + _waitingObjects);
         synchronized (request) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(request._lockThis)) {
               if (!request.isAnswered()) {
                  if (_terminateBattle) {
                     throw new BattleTerminatedException();
                  }
                  try {
                     Rules.diag("### about to wait in applyActions, for response in @" + Integer.toHexString(request.hashCode()) + ": " + request);
                     _waitingObjects.add(request);
                     request.wait();
                  } catch (InterruptedException e) {
                     // shouldn't get here
                  } finally {
                     Rules.diag("### done waiting in applyActions, for response in @" + Integer.toHexString(request.hashCode()) + ": " + request);
                     _waitingObjects.remove(request);
                  }
               }
            }
         }
      }
   }

   public void applySpells(Map<Character, List<Spell>> spells) throws BattleTerminatedException {
      TreeSet<Character> hitTargets = new TreeSet<>(spells.keySet());
      for (Character target : hitTargets) {
         // add the current locations of the target to the list of locations
         // that must be redrawn
         Collection<ArenaCoordinates> locationsToRedraw = new ArrayList<>(target.getCoordinates());

         boolean targetConsciousBeforeWound = target.getCondition().isConscious();
         List<Spell> spellsList = spells.get(target);
         for (Spell spell : spellsList) {
            spell.applySpell(target, _arena);
         }
         boolean targetConsciousAfterWound = target.getCondition().isConscious() && target.getCondition().isAlive();
         if (targetConsciousBeforeWound && !targetConsciousAfterWound &&
             (target.getWounds() >= Rules.getUnconsciousWoundLevel(target.getAttributeLevel(Attribute.Toughness)))) {
            _arena.sendMessageTextToAllClients(target.getName() + " falls unconscious due to " + target.getHisHer() +
                                               " wounds.", false/*popUp*/);
         }
         // add the new locations of the target to the list of locations
         // that must be redrawn
         locationsToRedraw.addAll(target.getCoordinates());

         _arena.sendCharacterUpdate(target, locationsToRedraw);
      }
   }

   public void applyWounds(Map<Character, List<Wound>> wounds) {
      Set<Character> hitTargets = new TreeSet<>(wounds.keySet());
      for (Character target : hitTargets) {
         boolean targetConsciousBeforeWound = target.getCondition().isConscious();
         List<Wound> woundsList = wounds.get(target);
         for (Wound wound : woundsList) {
            target.applyWound(wound, _arena);
         }
         boolean targetConsciousAfterWound = target.getCondition().isConscious() && target.getCondition().isAlive();
         if (targetConsciousBeforeWound && !targetConsciousAfterWound &&
             (target.getWounds() >= Rules.getUnconsciousWoundLevel(target.getAttributeLevel(Attribute.Toughness)))) {
            _arena.sendMessageTextToAllClients(target.getName() + " falls unconscious due to " + target.getHisHer() +
                                               " wounds.", false/*popUp*/);
         }
         // add the locations of the target to the list of locations
         // that must be redrawn
         Collection<ArenaCoordinates> locationsToRedraw = new ArrayList<>(target.getCoordinates());

         _arena.sendCharacterUpdate(target, locationsToRedraw);
      }
   }

   private boolean resolveAttack(Character attacker, RequestAction attack, Character defender, RequestDefense defense,
                                 Map<Character, List<Wound>> wounds) throws BattleTerminatedException {
      int attackStyle = attack._styleRequest.getAnswerIndex();
      boolean grappleAttack = attack.isGrappleAttack() || attack.isCounterAttackGrab();
      Weapon attackingWeapon = attacker.getLimb(attack.getLimb()).getWeapon(attacker);
      WeaponStyleAttack attackMode;
      if (attack.isCounterAttack()) {
         attackMode = attackingWeapon.getCounterAttackStyle(attackStyle);
      } else if (attack.isGrappleAttack()) {
         attackMode = attackingWeapon.getGrappleStyle(attackStyle);
      } else {
         attackMode = attackingWeapon.getAttackStyle(attackStyle);
      }

      byte skillPenalty = attackMode.getSkillPenalty();
      byte grapplePenalty = attacker.getPenaltyForBeingHeld();
      byte positionAdjustment = attacker.getPositionAdjustmentForAttack();
      StringBuilder terrainExplanation = new StringBuilder();
      byte terrainAdjustment = attacker.getTerrainAdjustmentForAttack(terrainExplanation, _arena.getCombatMap());

      byte attackActions = (byte) Math.min(attack.getAttackActions(false/*considerSpellAsAttack*/), attackMode.getMaxAttackActions());
      int aimActions = attacker.getAimDuration(defender._uniqueID);
      if (aimActions > 0) {
         attackActions += (byte) (Math.min(aimActions - 1, attackMode.getMaxAimBonus()));
      }

      boolean isThrowWeapon = attackMode.isThrown();
      short minDistanceInHexes = Arena.getMinDistance(attacker, defender);
      RANGE range = RANGE.OUT_OF_RANGE;
      if (attackMode instanceof WeaponStyleAttackRanged) {
         WeaponStyleAttackRanged rangedAttack = (WeaponStyleAttackRanged) attackMode;
         range = rangedAttack.getRangeForDistance(minDistanceInHexes, attacker.getAdjustedStrength());
      }

      StringBuilder sb = new StringBuilder();
      sb.append("<span style=\"color:red\">");
      sb.append(attacker.getName());
      if (grappleAttack) {
         if (attack.isAdvance()) {
            sb.append(" advances to grab ");
         } else {
            sb.append(" grabs ");
         }
         sb.append(defender.getName()).append(".");
      } else if (attack.isRanged()) {
         if (isThrowWeapon) {
            sb.append(" throws ").append(attacker.getHisHer()).append(" ").append(attackingWeapon.getName());
         } else {
            sb.append(" fires ").append(attacker.getHisHer()).append(" ").append(attackingWeapon.getName());
         }
         if (attackMode.getMaxAttackActions() > 1) {
            sb.append(" (").append(Math.min(attack.getAttackActions(false/*considerSpellAsAttack*/), attackMode.getMaxAttackActions())).append(" actions)");
         }
         sb.append(" at ").append(defender.getName());
         sb.append(" after aiming for ").append(aimActions).append(" rounds (effectively making a ");
         sb.append(attackActions).append("-action attack.)");
      } else {
         if (attack.isAdvance()) {
            if (attack.isCharge()) {
               sb.append(" charges to attack ");
            } else {
               sb.append(" advances to attack ");
            }
         } else {
            sb.append(" attacks ");
         }
         sb.append(defender.getName());
         sb.append(" with ").append(attackActions).append(" actions, using ");
         sb.append(attacker.getHisHer()).append(" ").append(attackingWeapon.getName());
         sb.append(", with an attack style of  ").append(attackMode.getName()).append(".");
      }
      sb.append(":<br/></span>");

      byte finalTN = resolveDefenses(defender, defense, attackMode.getParryPenalty(), minDistanceInHexes, range, attack, sb);

      sb.append("<br/>");
      DiceSet dice = null;
      if (Configuration.useExtendedDice()) {
         dice = attack._styleRequest.getAttackDice();
      } else if (Configuration.useSimpleDice()) {
         dice = new DiceSet(0/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 1/*d10*/, 0/*d12*/, 0/*d20*/, 0/*dBell*/, 1.0/*multiplier*/);
      } else if (Configuration.useBellCurveDice()) {
         dice = new DiceSet(0/*d1*/, 0/*d4*/, 0/*d6*/, 0/*d8*/, 0/*d10*/, 0/*d12*/, 0/*d20*/, 1/*dBell*/, 1.0/*multiplier*/);
      } else {
         DebugBreak.debugBreak("unknown dice configuration type");
      }

      dice = attacker.adjustDieRoll(dice, RollType.ATTACK_TO_HIT, defender/*target*/);
      sb.append(attacker.getName()).append(" rolls ").append(dice);
      String rollMessage = attacker.getName() + ", roll to attack " + defender.getName();
      int roll = dice.roll(true/*allowExplodes*/, attacker, RollType.ATTACK_TO_HIT, rollMessage);
      sb.append(", rolling ").append(dice.getLastDieRoll());
      byte adjSkill = attacker.getAdjustedWeaponSkill(attack.getLimb(), attackStyle, attack.isGrappleAttack(),
                                                      attack.isCounterAttack(), false/*accountForHandPenalty*/, false/*adjustForHolds*/);
      SkillType skillType = attacker.getWeaponSkillType(attack.getLimb(), attackStyle,
                                                        attack.isGrappleAttack(), attack.isCounterAttack());
      sb.append("<table border=1><tr><td>").append(roll).append("</td><td>dice roll</td></tr>");
      if (Configuration.useSimpleDice() || Configuration.useBellCurveDice()) {
         sb.append("<tr><td>+").append(adjSkill).append("</td><td>Adj. skill</td></tr>");
         byte actionAdjustment = Rules.getTNBonusForActions(attackActions);
         adjSkill += actionAdjustment;
         sb.append("<tr><td>");
         if (actionAdjustment >= 0) {
            sb.append("+");
         }
         sb.append(actionAdjustment).append("</td><td>").append(attackActions).append("-action attack</td></tr>");
      } else {
         sb.append("<tr><td>+").append(adjSkill).append("</td><td>skill</td></tr>");
      }

      if (skillPenalty != 0) {
         sb.append("<tr><td>-").append(skillPenalty).append("</td><td>skill penalty for weapon style: ").append(attackMode.getName()).append("</td></tr>");
         adjSkill -= skillPenalty;
      }
      if (grapplePenalty != 0) {
         sb.append("<tr><td>-").append(grapplePenalty).append("</td><td>penalty for being held</td></tr>");
         adjSkill -= grapplePenalty;
      }
      if (positionAdjustment != 0) {
         sb.append("<tr><td>").append(positionAdjustment).append("</td><td>penalty for ").append(attacker.getPositionName()).append("</td></tr>");
         adjSkill += positionAdjustment;
      }
      if (terrainAdjustment != 0) {
         sb.append("<tr><td>").append(terrainAdjustment).append("</td><td>penalty for terrain: ").append(terrainExplanation).append("</td></tr>");
         adjSkill += terrainAdjustment;
      }
      byte result = (byte) (roll + adjSkill);
      // Adjustments for size are now handled in the skill and Passive Defense values directly.
      //      byte sizeDiff = (byte) (attacker.getRace().getBonusToHit() + defender.getRace().getBonusToBeHit());
      //      if (sizeDiff != 0) {
      //         if (attacker.getRace().getBonusToHit() != 0) {
      //            sb.append("<tr><td>");
      //            if (attacker.getRace().getBonusToHit() > 0)
      //               sb.append("+");
      //            sb.append(attacker.getRace().getBonusToHit()).append("</td>");
      //            sb.append("<td>attacker is a ").append(attacker.getRace().getName()).append("</td></tr>");
      //         }
      //         if (defender.getRace().getBonusToBeHit() != 0) {
      //            sb.append("<tr><td>");
      //            if (defender.getRace().getBonusToBeHit() > 0)
      //               sb.append("+");
      //            sb.append(defender.getRace().getBonusToBeHit()).append("</td>");
      //            sb.append("<td>defender is a ").append(defender.getRace().getName()).append("</td></tr>");
      //         }
      //         result += sizeDiff;
      //      }
      Limb attackHand = attacker.getLimb(attack.getLimb());
      byte offHandPenalty = attacker.getHandPenalties(attackHand._limbType, skillType);
      if (offHandPenalty > 0) {
         sb.append("<tr><td>-").append(offHandPenalty).append("</td>");
         sb.append("<td>using ").append(attackHand.getName()).append("</td></tr>");
         result -= offHandPenalty;
      }
      byte penalty = attacker.getPenaltyToUseArm(attackHand, true/*includeWounds*/, !attacker.isBerserking()/*includePain*/);
      penalty -= offHandPenalty;
      if (penalty > 0) {
         sb.append("<tr><td>-").append(penalty).append("</td>");
         sb.append("<td>due to pain and wounds.</td></tr>");
         result -= penalty;
      }
      sb.append("<tr><td><b>").append(result).append("</b></td>");
      sb.append("<td>final attack roll</td></tr>");
      sb.append("<tr><td>").append(finalTN).append("</b></td>");
      sb.append("<td>defenders TN</td></tr>");

      byte bonusDamage = 0;
      byte holdLevel = 0;
      boolean hit = (result >= finalTN);
      if (dice.lastRollRolledAllOnes()) {
         sb.append("<tr><td colspan=2>automatic miss - all 1s rolled!</td></tr>");
         hit = false;
      } else {
         if (hit) {
            bonusDamage = Rules.getDamageBonusForSkillExcess((byte) (result - finalTN));
            holdLevel = Rules.getHoldLevelForSkillExcess((byte) (result - finalTN));
            if (grappleAttack) {
               sb.append("<tr><td><b>").append(holdLevel).append("</b></td>");
               sb.append("<td>target grabbed, hold level</td></tr>");
            } else if (attack.isCounterAttackThrow()) {
               sb.append("<tr><td><b>").append(holdLevel).append("</b></td>");
               sb.append("<td>target throw attempt success margin</td></tr>");
            } else {
               sb.append("<tr><td><b>").append(bonusDamage).append("</b></td>");
               sb.append("<td>target hit, bonus damage</td></tr>");
            }
         } else {
            sb.append("<tr><td colspan=2>target missed</td></tr>");
         }
      }
      sb.append("</table>");
      if (hit) {
         int sizeDifference = attacker.getRace().getBuildModifier() - defender.getRace().getBuildModifier();
         boolean animalBite = false;
         if (!grappleAttack) {
            if (attackingWeapon.getName().equals(Weapon.NAME_Fangs)) {
               if (attacker.hasAdvantage(Race.PROPERTIES_ANIMAL)) {
                  sb.append("Because the attacker is an animal with fangs, this attack doubles as a grab, at a level of ");
                  sb.append(holdLevel).append(".<br/>");
                  animalBite = true;
               }
            }
         }
         if (grappleAttack || animalBite) {
            Byte currentHoldLevel = defender.getHoldLevel(attacker);
            byte holdModifier = (byte) (sizeDifference / 2);
            byte newHoldLevel = (byte) (holdLevel + holdModifier);
            if (holdModifier != 0) {
               sb.append(" Due to the size difference between the attacker (").append(attacker.getRace().getBuildModifier()).append(") and defender (").append(
                       defender.getRace().getBuildModifier()).append(
                       ") the hold is modified by ");
               if (holdModifier > 0) {
                  sb.append("+");
               }
               sb.append(holdModifier);
               if (newHoldLevel < 0) {
                  sb.append(", which negates the hold completely.<br/>");
                  hit = false;
               } else {
                  sb.append(" to a new level of ").append(newHoldLevel).append(".<br/>");
               }
            }
            if (newHoldLevel >= 0) {
               if ((currentHoldLevel != null) && (currentHoldLevel >= newHoldLevel)) {
                  sb.append("New hold level is less than or equal to the current hold level of ").append(currentHoldLevel);
                  sb.append(" so this grab is ineffective.<br/>");
                  hit = false;
               } else {
                  // Set this as a dual grapple attack, so we know if a retreat must alter the location of the attacker
                  attack.setDualGrappleAttack();
                  defender.setHoldLevel(attacker, newHoldLevel);
                  ArenaLocation attackingLimbLoc = attacker.getLimbLocation(attack.getLimb(), _arena.getCombatMap());
                  if (Arena.getShortestDistance(attackingLimbLoc, defender.getOrientation()) > 1) {
                     // Try to move the smaller character until the defender is adjacent to the attacking limb that grabbed him/her.
                     if (attacker.getBuildBase() >= defender.getBuildBase()) {
                        // Move the defender closer to the attacker:
                        for (Orientation newOrientation : defender.getOrientation().getPossibleFutureOrientations(_arena.getCombatMap())) {
                           if (Arena.getShortestDistance(attackingLimbLoc, newOrientation) == 1) {
                              _arena.moveCharacter(defender, newOrientation);
                              break;
                           }
                        }
                     } else {
                        // Move the attacker closer to the defender:
                        for (Orientation newOrientation : attacker.getOrientation().getPossibleFutureOrientations(_arena.getCombatMap())) {
                           attackingLimbLoc = newOrientation.getLimbLocation(attack.getLimb(), _arena.getCombatMap());
                           if (Arena.getShortestDistance(attackingLimbLoc, newOrientation) == 1) {
                              _arena.moveCharacter(attacker, newOrientation);
                              break;
                           }
                        }
                     }
                  }
                  if (holdModifier != 0) {
                     sb.append("<br/>");
                  }
               }
            }
         } else if (attack.isCounterAttackThrow()) {
            // Throw the opponent.
            DiceSet throwDistDice = new DiceSet(1, 1, 0, 0, 0, 0, 0, 0, 1.0);
            rollMessage = attacker.getName() + ", roll to see how far you've thrown " + defender.getName();
            int throwDist = throwDistDice.roll(true/*allowExplodes*/, attacker, RollType.ATTACK_TO_HIT, rollMessage);
            sb.append(defender.getName()).append(" is thrown ").append(throwDistDice).append(" hexes, rolling ").append(throwDistDice.getLastDieRoll());
            CombatMap map = _arena.getCombatMap();
            while (throwDist-- > 0) {
               List<Orientation> possibleMoves = defender.getOrientation().getPossibleAdvanceOrientations(map, true);
               if (possibleMoves.isEmpty()) {
                  // we must have hit a wall, assess damage
                  Wound wound = new Wound((byte) 5,
                                          Location.BODY,
                                          "Counter-attack Throw",
                                          3,//painLevel
                                          1,//wounds
                                          0,//bleedRate
                                          0,//armPenalty
                                          0,//movePenalty
                                          0,//knockedDownDist,
                                          DamageType.GENERAL,
                                          0,//effectMask,
                                          defender);
                  defender.applyWound(wound, _arena);
                  sb.append(defender.getName()).append(" is thrown into a wall, taking the following damage: ").append(wound.describeWound());
                  break;
               }
               //The first element in this list is the most forward-moving
               Orientation newOrientation = possibleMoves.get(0);
               _arena.moveCharacter(defender, newOrientation);
            }

            DiceSet sideUpDice = new DiceSet(0, 1, 0, 0, 0, 0, 0, 0, 1.0);
            rollMessage = attacker.getName() + ", roll to determine if " + defender.getName() +
                          " is facing up or down after the throw. 1 or 2 = face up, 3 or 4 = face down.";
            int sideUp = sideUpDice.roll(false/*allowExplodes*/, attacker, RollType.ATTACK_TO_HIT, rollMessage);
            sb.append(" To determine facing, a d4 is rolled, rolling ").append(sideUpDice.getLastDieRoll());
            if (sideUp > 2) {
               sb.append(", so the defender will be face-down.");
               defender.getCondition().setPosition(Position.PRONE_FRONT, _arena.getCombatMap(), defender);
            } else {
               sb.append(", so the defender will be face-up.");
               defender.getCondition().setPosition(Position.PRONE_BACK, _arena.getCombatMap(), defender);
            }
            /* Defensive Grab:
             *   When attacked, the Aikido practitioner may counter-attack using this grab.
             *   This grab follows the same rules as a normal grab, such as a wrestler would do.
             *   Unlike a wrestler's grab, this grab always starts out as an arm grab,
             *   meaning that the held attacker cannot use their weapon arm for any purpose
             *   until they break free, or the holder switches to another hold.
             */

            /* Defensive Throw:
             *   When attacked, the Aikido practitioner may counter-attack using this throw.
             *   If successful, this counter-attack causes the assailant to be thrown a few hexes
             *   (use a d4 to determine) in a direction chosen by the Aikido practitioner.
             *   The Aikido practitioner must declare the direction before the distance die is rolled.
             *      The thrower rolls his counter attack against which the assailant may dodge or retreat,
             *      but their PD does not add into their defense TN. Furthermore, the assailant must
             *      subtract their Enc level from their defense TN.
             *
             *   The thrower rolls his counter-attack against a TN of 7. If successful,
             *   the opponent may defend against the throw, using any unspent actions for this round,
             *   however, defense against a throw is handled in a different manner:
             *   The assailant (who is being thrown) declares the number of actions he is allocating to the defense,
             *   and rolls a battle die (d10±) to which he or she adds their NIM attribute,
             *   minus their current encumbrance level.
             *   If the assailant defended using 1 action, they subtract 5 from this roll. If they used 3 actions,
             *   they add 5 to this roll. Note that since the assailant attacked this round, it will be impossible
             *   for them to still have 3 actions available for defense, unless they are under the influence of
             *   some type of magic giving them more than 3 actions per round. If the assailant’s total roll results
             *   a number equal to or higher than the difference between the Aikido practitioner’s counter-attack roll
             *   and his counter-attack TN (of 7), the defense is successful. If the assailant has no actions left to
             *   be spent on the defense, and the counter-attack is successful, the assailant is thrown automatically.
             *   If the assailant makes his/her NIM roll, they are moved, but only half the distance (rounded down).
             *   If the assailant fails their NIM roll, they are thrown the full distance, and are on the ground
             *   (GM randomly decides face up or down). If the assailant fails their roll, and they are thrown into
             *   a wall, or other obstacle, the GM may assess damage as he/she sees fit. The GM should determine facing,
             *   taking into account the NIM roll. If the NIM roll is very good (over 5 points more than needed),
             *   the assailant may choose his own facing.
             */
         }
         if (!attack.isCounterAttackThrow() && !grappleAttack) {
            byte baseDamage = attackMode.getDamage(attacker.getPhysicalDamageBase());
            DiceSet damageDie = attackMode.getVarianceDie();
            String damageExplanation = String.valueOf(baseDamage);
            String damageDieExplanation = damageDie.toString();
            if (damageDieExplanation.startsWith("+") || damageDieExplanation.isEmpty()) {
               damageExplanation += " " + damageDieExplanation;
            } else {
               damageExplanation += " + " + damageDieExplanation;
            }
            resolveDamage(attacker, defender, attackingWeapon.getName(), damageExplanation, baseDamage,
                          bonusDamage, damageDie, attackMode.getDamageType(),
                          attackingWeapon.getSpecialDamageModifier(),
                          attackingWeapon.getSpecialDamageModifierExplanation(), sb, wounds,
                          (byte) (result - finalTN), attack.isCharge());
         }
      }
      String message = sb.toString();
      if (!message.isEmpty()) {
         _arena.sendMessageTextToAllClients(message, false/*popUp*/);
         _arena.sendMessageTextToParticipants(message, attacker, defender);
      }

      if (isThrowWeapon) {
         attacker.getLimb(attack.getLimb()).setHeldThing(null, attacker);
         attacker.updateWeapons();
         if (attackingWeapon.getSize() > 0) {
            ArenaLocation dropSpot = null;
            if (hit) {
               List<Wound> woundsOnDefender = wounds.get(defender);
               // woundsOnDefender can be null if the hit was too small to do any effective damage
               if ((woundsOnDefender != null) && (woundsOnDefender.size() > 0)) {
                  dropSpot = defender.getLimbLocation(woundsOnDefender.get(woundsOnDefender.size() - 1).getLimb(), _arena.getCombatMap());
               }
            }
            if (dropSpot == null) {
               dropSpot = _arena.getCombatMap().getLocation(defender.getHeadCoordinates());
            }
            if (dropSpot != null) {
               dropSpot.addThing(attackingWeapon);
            }
         }
      }
      attacker.clearAimDuration();
      return hit;
   }


   /**
    * @param defender
    * @param defense
    * @param attackParryPenalty
    * @param distance
    * @param range
    * @param sb
    * @return
    */
   public byte resolveDefenses(Character defender, RequestDefense defense, byte attackParryPenalty, short distance,
                               RANGE range, RequestAction attack, StringBuilder sb) {
      // When we get here, the defense has already been applied to the defender, so their weapon may look
      // unready. Make sure this doesn't interfere with the defense resolution.
      byte baseTN = defender.getDefenseTN(defense, false/*includeWoundPenalty*/, false/*includeHolds*/,
                                          false/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                          (byte) 0/*parryPenalty*/, true, distance, RANGE.OUT_OF_RANGE);
      byte holdTN = defender.getDefenseTN(defense, false/*includeWoundPenalty*/, true/*includeHolds*/,
                                          false/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                          (byte) 0/*parryPenalty*/, true, distance, RANGE.OUT_OF_RANGE);
      byte postnTN = defender.getDefenseTN(defense, false/*includeWoundPenalty*/, true/*includeHolds*/,
                                           true/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                           (byte) 0/*parryPenalty*/, true, distance, RANGE.OUT_OF_RANGE);
      byte rangeTN = defender.getDefenseTN(defense, false/*includeWoundPenalty*/, true/*includeHolds*/,
                                           true/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                           (byte) 0/*parryPenalty*/, true, distance, range);
      byte parryTN = defender.getDefenseTN(defense, false/*includeWoundPenalty*/, true/*includeHolds*/,
                                           true/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                           attackParryPenalty, true, distance, range);
      byte woundTN = defender.getDefenseTN(defense, true/*includeWoundPenalty*/, true/*includeHolds*/,
                                           true/*includePosition*/, false/*includeMassiveDamagePenalty*/,
                                           attackParryPenalty, true, distance, range);
      byte finalTN = defender.getDefenseTN(defense, true/*includeWoundPenalty*/, true/*includeHolds*/,
                                           true/*includePosition*/, true/*includeMassiveDamagePenalty*/,
                                           attackParryPenalty, true, distance, range);
      byte holdAdjustment = (byte) (holdTN - baseTN);
      byte postnAdjustment = (byte) (postnTN - holdTN);
      byte rangeAdjustment = (byte) (rangeTN - postnTN);
      byte parryPenalty = (byte) (parryTN - rangeTN);
      byte woundPenalty = (byte) (woundTN - parryTN);
      byte massivePenalty = (byte) (finalTN - woundTN);
      byte tnBonusForMovement = 0;
      if (range != RANGE.OUT_OF_RANGE) {
         if (defender.hasMovedLastAction()) {
            tnBonusForMovement = Rules.getTNBonusForMovement(range, defender.isMovingEvasively());
            finalTN += tnBonusForMovement;
         }
      }

      sb.append(defender.getName()).append(" ").append(defense.getDefenseName(true/*tensePast*/, defender, attack));
      if (range != RANGE.OUT_OF_RANGE) {
         sb.append(" at ").append(range.getName()).append(" range");
      }
      sb.append(" for a TN of ").append(finalTN);
      if (attack.isCounterAttack()) {
         sb.append(". Because this is a defense against a counter attack, PD doesn't help.");
      }

      if (baseTN != finalTN) {
         sb.append("<table border=1><tr>");
         sb.append("<td>").append(baseTN).append("</td>");
         sb.append("<td>").append(defense.getDefenseName(false/*tensePast*/, defender, attack)).append("</td>");
         sb.append("</tr>");
         if (parryPenalty != 0) {
            sb.append("<tr>");
            if (parryPenalty > 0) {
               sb.append("+");
            }
            sb.append("<td>").append(parryPenalty).append("</td>");
            sb.append("<td>").append("attacking weapon parry penalty").append("</td>");
            sb.append("</tr>");
         }
         if (woundPenalty != 0) {
            sb.append("<tr><td>");
            if (woundPenalty > 0) {
               sb.append("+");
            }
            sb.append(woundPenalty).append("</td>");
            sb.append("<td>").append("wounds penalty").append("</td>");
            sb.append("</tr>");
         }
         if (massivePenalty != 0) {
            sb.append("<tr>");
            if (massivePenalty > 0) {
               sb.append("+");
            }
            sb.append("<td>").append(massivePenalty).append("</td>");
            sb.append("<td>").append("massive damage penalties").append("</td>");
            sb.append("</tr>");
         }
         if (holdAdjustment != 0) {
            sb.append("<tr>");
            if (holdAdjustment > 0) {
               sb.append("+");
            }
            sb.append("<td>").append(holdAdjustment).append("</td>");
            sb.append("<td>").append("being held</td>");
            sb.append("</tr>");
         }
         if (postnAdjustment != 0) {
            sb.append("<tr>");
            if (postnAdjustment > 0) {
               sb.append("+");
            }
            sb.append("<td>").append(postnAdjustment).append("</td>");
            sb.append("<td>").append("position penalties (").append(defender.getPositionName()).append(")</td>");
            sb.append("</tr>");
         }
         if (range != RANGE.OUT_OF_RANGE) {
            if (rangeAdjustment != 0) {
               sb.append("<tr><td>");
               if (rangeAdjustment > 0) {
                  sb.append("+");
               }
               sb.append(rangeAdjustment);
               sb.append("</td><td>range ");
               if (rangeAdjustment > 0) {
                  sb.append("bonus");
               } else {
                  sb.append("penalty");
               }
               sb.append(" for ").append(range.getName()).append(" range.</td></tr>");
            }
            sb.append("<tr><td>");
            if (tnBonusForMovement >= 0) {
               sb.append("+");
            }
            sb.append(tnBonusForMovement).append("</td>");
            if (defender.isMovingEvasively()) {
               sb.append("<td>moving evasively</td></tr>");
            } else if (defender.hasMovedLastAction()) {
               sb.append("<td>moving</td></tr>");
            } else {
               sb.append("<td>not moving</td></tr>");
            }
         }
         sb.append("<tr>");
         sb.append("<td>").append(finalTN).append("</td>");
         sb.append("<td>").append("<b>Final Defense TN</b>").append("</td>");
         sb.append("</tr>");
         sb.append("</table>");
      }
      return finalTN;
   }

   public void resolveDamage(Character attacker, Character defender, String attackingWeaponName, String damageExplanation,
                             int baseDamage, byte bonusDamage, DiceSet damageDie, DamageType damageType,
                             SpecialDamage specialDamageModifier, String specialDamageModifierExplanation,
                             StringBuilder sb, Map<Character, List<Wound>> wounds, byte attackSuccessRollOverTN,
                             boolean isCharge) throws BattleTerminatedException {

      String rollMessage = attacker.getName() + ", roll to damage against " + defender.getName();
      if (Configuration.rollDice()) {
         // If we are rolling dice, then we need to send the attack message out now,
         // followed by the damage roll.
         rollMessage = sb.toString();
         if (!rollMessage.isEmpty()) {
            _arena.sendMessageTextToAllClients(rollMessage, false/*popUp*/);
            // To the parties involved, send the message as a popup message:
            // No need to send the message to the attacker, they will see the message as they roll damage dice.
            _arena.sendMessageTextToParticipants(rollMessage, defender, null);
         }
         sb.setLength(0);
         sb.append(attacker.getName()).append(" successfully attacked ").append(defender.getName()).append(".<br/>");
      }

      String damageTypeString = damageType.fullname;
      damageDie = attacker.adjustDieRoll(damageDie, RollType.DAMAGE_ATTACK, defender/*target*/);
      int damageRoll = damageDie.roll(true/*allowExplodes*/, attacker, RollType.DAMAGE_ATTACK, rollMessage);
      sb.append(attacker.getName()).append("'s ").append(attackingWeaponName);
      sb.append(" does ").append(damageExplanation);
      sb.append(" ").append(damageTypeString).append(".<br/>");
      sb.append(attacker.getName()).append(" rolls ");
      sb.append(damageDie.getLastDieRoll());
      sb.append(" for damage, resulting in ").append(damageRoll);

      sb.append(". Adding in ").append(attacker.getHisHer()).append(" base damage of ").append(baseDamage);
      if (bonusDamage > 0) {
         sb.append(" and bonus damage of ").append(bonusDamage);
      }
      byte totalDamage = (byte) (damageRoll + baseDamage + bonusDamage);
      sb.append(", results in ").append(totalDamage).append(" total points of ");
      sb.append(damageTypeString).append(" damage.<br/>");

      if (isCharge) {
         // fangs (bites) don't get charge bonus to damage:
         if (!attackingWeaponName.equals(Weapon.NAME_Fangs)) {
            byte speed = attacker.getMovementRate();
            byte speedDamage = Rules.getChargeDamageForSpeed(speed);
            totalDamage += speedDamage;
            sb.append(" Since the attacker was charging at a speed of ").append(speed).append(" ");
            sb.append(attacker.getHeShe()).append(" does an extra ").append(speedDamage).append(" points of damage, raising the total damage to ");
            sb.append(totalDamage).append(" total points of ").append(damageTypeString).append(" damage.<br/>");
         }
      }
      byte reducedDamage = totalDamage;
//      if ((specialDamageModifier.getBits() & SpecialDamage.MOD_NO_BUILD) != 0) {
//         if ((specialDamageModifier.getBits() & SpecialDamage.MOD_NO_ARMOR) != 0) {
//            sb.append(specialDamageModifierExplanation).append(" build and armor have no effect ");
//            if (reducedDamage < 0) {
//               sb.append(", but this attack does no damage.");
//               return;
//            }
//         }
//         else { // (...NO_BUILD != 0) && (...NO_ARMOR == 0)
//            byte defenderBuild = defender.getArmor().getBarrier(damageType);
//            sb.append(specialDamageModifierExplanation).append(" build has no effect, but armor does. ");
//            sb.append(defender.getName()).append(" has a armor barrier of ").append(defenderBuild);
//            reducedDamage -= defenderBuild;
//            if (reducedDamage < 0) {
//               sb.append(", which blocks all damage.");
//            }
//         }
//      }
//      else {
      byte defenderBuild;
      if ((specialDamageModifier.getBits() & SpecialDamage.MOD_NO_ARMOR) != 0) {
         sb.append(specialDamageModifierExplanation).append(" armor has no effect ");
         defenderBuild = defender.getBuildBase();
         sb.append(defender.getName()).append(" has a base build of ").append(defenderBuild);
      } else {
         defenderBuild = defender.getBuild(damageType);
         sb.append(defender.getName()).append(" has a build vs. ").append(damageTypeString);
         sb.append(" of ").append(defenderBuild);
      }
      reducedDamage -= defenderBuild;
      if (reducedDamage < 0) {
         sb.append(", which blocks all damage.");
      }
//      }
      StringBuilder sbWoundModExplanation = new StringBuilder();
      Wound wound;
      if (reducedDamage < 0) {
         // If the damage done is less than zero, some spells may still cause an effect (Shocking Grasp, for instance)
         wound = attacker.modifyWoundFromAttack(null/*wound*/, defender, attackingWeaponName, sbWoundModExplanation);
         if (wound == null) {
            return;
         }
         sb.append("<br/>Although the damage done is below zero, ").append(attacker.getName());
         sb.append(" touched the defender, and spells in effect cause the following wound:");
         sb.append(sbWoundModExplanation).append("<br/>");
      } else {
         sb.append(", reducing the damage to ").append(reducedDamage).append("<br/>");
         byte maxDamageForTNSuccess = Rules.getMaxAppliedDamageForTNSuccess(attackSuccessRollOverTN, reducedDamage);
         if (reducedDamage > maxDamageForTNSuccess) {
            sb.append("However, since the attack only succeeded by ").append(attackSuccessRollOverTN);
            sb.append(" points, the maximum damage allowed is ").append(maxDamageForTNSuccess).append(".<br/>");
            reducedDamage = maxDamageForTNSuccess;
         }
         wound = WoundCharts.getWound(reducedDamage, damageType, defender, sbWoundModExplanation);
         wound.setSpecialDamageModifier(specialDamageModifier);
         wound = attacker.modifyWoundFromAttack(wound, defender, attackingWeaponName, sbWoundModExplanation);
         if (wound.isCrippling() || wound.isKnockOut()) {
            Advantage adv = defender.getAdvantage(Advantage.HERO_POINTS);
            if ((adv != null) && (adv.getLevel() > 0)) {
               RequestUseOfHeroPoint reqHeroPoints = new RequestUseOfHeroPoint(wound, adv.getLevel());
               _arena.sendObjectToCombatant(defender, reqHeroPoints);
               synchronized (reqHeroPoints) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(reqHeroPoints._lockThis)) {
                     if (!reqHeroPoints.isAnswered()) {
                        if (_terminateBattle) {
                           throw new BattleTerminatedException();
                        }
                        try {
                           Rules.diag("### about to wait in applyActions, for response in @" + Integer.toHexString(reqHeroPoints.hashCode()) + ": "
                                      + reqHeroPoints);
                           _waitingObjects.add(reqHeroPoints);
                           reqHeroPoints.wait();
                        } catch (InterruptedException e) {
                        } finally {
                           Rules.diag("### done waiting in applyActions, for response in @" + Integer.toHexString(reqHeroPoints.hashCode()) + ": " + reqHeroPoints);
                           _waitingObjects.remove(reqHeroPoints);
                        }
                     }
                     if (reqHeroPoints.isAnswerUseHeroPoint()) {
                        sb.append(defender.getName()).append(" applies a Hero Point to avoid the wound.");
                        adv.setLevel((byte) (adv.getLevel() - 1));
                        boolean preamble = true;
                        for (IHolder holder : defender.getHolders()) {
                           if (preamble) {
                              sb.append(" Applying the Hero Point removed");
                           } else {
                              sb.append(" and");
                           }
                           sb.append(" the level-").append(defender.getHoldLevel(holder))
                             .append(" hold from ").append(holder.getName());
                           defender.setHoldLevel(holder, (byte) 0);
                           preamble = false;
                        }
                        return;
                     }
                  }
               }
            }
         }
         defender.placeWound(wound);

         sb.append(defender.getName()).append(" suffers the following wound:<br/>");
         sb.append(wound.describeWound()).append("<br/>");
         if (specialDamageModifierExplanation.length() > 0) {
            sb.append("Wound modified by: ").append(specialDamageModifierExplanation);
         }
         if (sbWoundModExplanation.length() > 0) {
            if (specialDamageModifierExplanation.length() > 0) {
               sb.append("<br/>and also modified by: ");
            } else {
               sb.append("Wound modified by: ");
            }
            sb.append(sbWoundModExplanation);
         }

      }
      byte knockBack = wound.getKnockedBackDist();
      byte holdStrength = defender.getPenaltyForBeingHeld();
      Byte holdingStrength = defender.getHoldingLevel();
      if (holdingStrength != null) {
         holdStrength += holdingStrength;
      }
      if (holdStrength >= (5 * knockBack)) {
         knockBack = 0;
      } else {
         // release any holds he has, or is held by:
         defender.releaseHold();
         for (IHolder holder : defender.getHolders()) {
            defender.setHoldLevel(holder, null);
         }
      }
      while (knockBack-- > 0) {
         if (!_arena.moveToFrom(defender, attacker, attacker.getLimbLocation(LimbType.HEAD, _arena.getCombatMap())/*retreatFromLoc*/, null/*attackFromLimb*/)) {
            // If moveToFrom returns false, we can't be knocked back any further, so stop trying.
            break;
         }
      }

      List<Wound> woundsList = wounds.computeIfAbsent(defender, k -> new ArrayList<>());
      woundsList.add(wound);
   }

   private boolean selectActions(List<Character> activeCombatants, Map<Character,
           RequestAction> results, List<Character> allCombatants)
           throws BattleTerminatedException {
      // First, ask each character to declare their action for this round, but don't wait for the response yet.
      List<SyncRequest> resultsQueue = new ArrayList<>();
      List<SyncRequest> waitingList = new ArrayList<>();
      List<Character> actedCombatants = new ArrayList<>();
      for (Character actor : activeCombatants) {
         Rules.diag("Battle:selectActions for actor " + actor.getName());
         actedCombatants.add(actor);
         // A character that defended earlier this round, and is now out of actions
         // must skip his/her action phase for this round.
         // Also, if a character has already attacked because he was waiting
         // for an opportunity to attack, and he got the opportunity.
         if (actor.stillFighting()
             && (actor.getActionsAvailableThisRound(false/*usedForDefenseOnly*/) > 0)
             && (!actor.getCondition().hasAttackedThisRound())) {
            Rules.diag("Battle:selectActions for actor " + actor.getName() + " is still fighting");
            // remove the actor from this list so he doesn't
            // continue to wait, unless he re-asks to wait more.
            clearWaitingForAttackMap(actor);

            List<Character> charactersTargetingActor = getCharactersAimingAtCharacter(actor);
            Rules.diag("Battle:selectActions for actor " + actor.getName() + ", aimed at by: " + charactersTargetingActor);
            RequestAction actReq = actor.getActionRequest(_arena, null/*delayedTarget*/, charactersTargetingActor);
            Rules.diag("Battle:selectActions for actor " + actor.getName() + ", actReq= " + actReq);
            // If there is only one option, don't ask, just do it.
            if (actReq.selectSingleEnabledEntry(false/*ignoreCancel*/)) {
               results.put(actor, actReq);
               synchronized (resultsQueue) {
                  resultsQueue.add(actReq);
                  resultsQueue.notifyAll();
               }
               Rules.diag("Battle:selectActions for actor " + actor.getName() + " - single action found: " + actReq.getAnswer());
            } else {
               // More than one entry is available to select:
               synchronized (resultsQueue) {
                  Rules.diag("setting results Queue to " + resultsQueue);
               }
               actReq.setResultsQueue(resultsQueue);
               if (_arena.sendObjectToCombatant(actor, actReq)) {
                  results.put(actor, actReq);
                  synchronized (actReq) {
                     try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(actReq._lockThis)) {
                        if (actReq.isAnswered()) {
                           Rules.diag("Battle:selectActions for actor " + actor.getName() + " - action answered: " + actReq.getAnswer());
                           synchronized (resultsQueue) {
                              resultsQueue.add(actReq);
                              resultsQueue.notifyAll();
                           }
                        } else {
                           Rules.diag("Battle:selectActions for actor " + actor.getName() + " - will wait for response.");
                           waitingList.add(actReq);
                        }
                     }
                  }
               }
            }
         } else {
            Rules.diag("Battle:selectActions for actor " + actor.getName() + " is no longer fighting");
         }
      }
      // This can happen if everyone in the list is KO'ed or dead.
      if (waitingList.size() == 0) {
         synchronized (resultsQueue) {
            if (resultsQueue.size() == 0) {
               return false;
            }
         }
      }

      // Secondly, wait for every response, and if an action requires more information
      // (such as a movement or attack style), get the subsequent action(s).
      HashMap<Character, Integer> movementTracker = new HashMap<>();
      /*synchronized (_waitingObjects)*/
      {
         Rules.diag("### inside lock selectActions, of waitingList = @" + Integer.toHexString(_waitingObjects.hashCode()) + ": " + _waitingObjects);
         synchronized (resultsQueue) {
            Rules.diag("### inside lock of resultsQueue (=" + resultsQueue + "), waitingList.size() = " + waitingList.size());
            while ((waitingList.size() + resultsQueue.size()) > 0) {
               if (resultsQueue.size() == 0) {
                  Rules.diag("### inside lock of resultsQueue==0");
                  if (_terminateBattle) {
                     throw new BattleTerminatedException();
                  }
                  try {
                     Rules.diag("### about to wait in selectActions for response in @" + Integer.toHexString(resultsQueue.hashCode()) + ": " + resultsQueue);
                     _waitingObjects.add(resultsQueue);
                     resultsQueue.wait();
                  } catch (InterruptedException e) {
                     e.printStackTrace();
                  } finally {
                     Rules.diag("### done waiting in selectActions for response in @" + Integer.toHexString(resultsQueue.hashCode()) + ": " + resultsQueue);
                     _waitingObjects.remove(resultsQueue);
                  }
               }
               if (resultsQueue.size() > 0) {
                  Rules.diag("### inside lock of resultsQueue>0");
                  // We get a response from someone
                  SyncRequest obj = resultsQueue.remove(0);
                  if (obj != null) {
                     waitingList.remove(obj);
                     if (obj.is_backupSelected()) {
                        throw new BattleTerminatedException();
                     }
                     // Who sent the response?
                     for (Character actor : actedCombatants) {
                        RequestAction actReq = results.get(actor);
                        if (actReq != null) {
                           if ((obj == actReq)
                               || (obj == actReq._equipmentRequest)
                               || (obj == actReq._movementRequest)
                               || (obj == actReq._locationSelection)
                               || (obj == actReq._positionRequest)
                               || (obj == actReq._styleRequest)
                               || (obj == actReq._targetPriorities)
                               || (obj == actReq._spellTypeSelectionRequest)
                               || ((actReq._spellTypeSelectionRequest != null) && (obj == actReq._spellTypeSelectionRequest._spellSelectionRequest))
                               || (obj == actReq._targetSelection)) {
                              if ((obj == actReq._movementRequest) || (actReq.isAttack() && actReq.isAdvance() && (obj != actReq._styleRequest))) {
                                 if (obj == actReq._movementRequest) {
                                    RequestMovement moveReq = actReq._movementRequest;

                                    Orientation destOrientation = moveReq.getAnswerOrientation(true/*removeEntry*/);
                                    if (destOrientation != null) {
                                       StringBuilder sb = new StringBuilder(actor.getName());
                                       sb.append(" is moving to ");
                                       ArenaCoordinates headCoord = destOrientation.getHeadCoordinates();
                                       sb.append(headCoord._x).append(", ").append(headCoord._y);
                                       Rules.diag(sb.toString());

                                       Integer moved = movementTracker.get(actor);
                                       if (moved == null) {
                                          moved = 0;
                                       }

                                       if (destOrientation.equals(actor.getOrientation())) {
                                          // they clicked on their current location.
                                          // This indicates they are done moving.
                                          actor.setMoveComplete();
                                          // don't increment the 'moved' variable,
                                          // since this wasn't really a move.
                                       } else {
                                          // Look for any limb that changed location. If any one changed,
                                          // mark it as a single movement, regardless of how far it moved,
                                          // or how many limbs moved.
                                          for (LimbType limbType : LimbType.values()) {
                                             ArenaCoordinates destLimbCoord = destOrientation.getLimbCoordinates(limbType);
                                             if (destLimbCoord != null) {
                                                ArenaCoordinates sourceLimbCoord = actor.getOrientation().getLimbCoordinates(limbType);
                                                if (!destLimbCoord.sameCoordinates(sourceLimbCoord)) {
                                                   moved = moved + 1;
                                                   break;
                                                }
                                             }
                                          }
                                          _arena.applyMovement(actor, destOrientation, moveReq);
                                          //if (moveReq.hasMovesLeft()) {
                                          //   resultsQueue.add(actReq);
                                          //}
                                       }
                                       movementTracker.put(actor, moved);
                                    }
                                 } else { // (actReq.isAttack() && actReq.isAdvance()) == true
                                    Character target = _arena.getCharacter(actReq._targetID);
                                    // move forward such that the attacking weapon is in range of the target
                                    Limb limb = actor.getLimb(actReq.getLimb());
                                    if (actReq.isCharge()) {
                                       HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
                                       if (actor.getOrientation().getPossibleChargePathsToTarget(_arena.getCombatMap(), actor, target,
                                                                                                 actor.getAvailableMovement(false/*movingEvasively*/),
                                                                                                 mapOrientationToNextOrientationsLeadingToChargeAttack)) {
                                          // starting from our current orientation, move forward through valid orientations to charge-attack:
                                          Orientation nextOrientationInPath = actor.getOrientation();
                                          while (nextOrientationInPath != null) {
                                             List<Orientation> possibleMoves = mapOrientationToNextOrientationsLeadingToChargeAttack.get(nextOrientationInPath);
                                             if ((possibleMoves != null) && (possibleMoves.size() > 0)) {
                                                //int randomIndex = (int) (CombatServer.random() * possibleMoves.size());
                                                // always use the first option (index 0), since that should be straight forward,
                                                // unless straight forward would not get us to where the target is.
                                                int randomIndex = 0;
                                                nextOrientationInPath = possibleMoves.get(randomIndex);
                                                _arena.moveCharacter(actor, nextOrientationInPath);
                                                checkForAimLossDueToMovement(actor);
                                                checkForWaitingCharactersToAttack(results, allCombatants, resultsQueue, waitingList, actedCombatants, actor);
                                                try {
                                                   Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                   e.printStackTrace();
                                                }
                                             } else {
                                                break;
                                             }
                                          }
                                       }
                                    } else {
                                       _arena.moveToFrom(actor, target, null/*retreatFromLoc*/, limb/*attackFromLimb*/);
                                    }
                                 }
                                 checkForAimLossDueToMovement(actor);
                                 checkForWaitingCharactersToAttack(results, allCombatants, resultsQueue, waitingList, actedCombatants, actor);
                              } else if (obj == actReq._targetPriorities) {
                                 actor.applyAction(actReq, _arena);
                              }

                              getNextQuestionForAction(allCombatants, resultsQueue, waitingList, actor, actReq);
                              // We found the character that made the request, stop looking
                              break;
                           }
                        }
                     }
                  }
               }
            }
            Rules.diag("exiting lock of " + resultsQueue);
         }
      }
      // display character moves
      TreeSet<Character> movedCharacters = new TreeSet<>(movementTracker.keySet());
      for (Character mover : movedCharacters) {
         Integer movement = movementTracker.get(mover);
         if (mover.getPosition() == Position.STANDING) {
            _arena.sendMessageTextToAllClients(mover.getName() + " moves " + movement + " hexes.", false/*popUp*/);
         } else {
            _arena.sendMessageTextToAllClients(mover.getName() + " crawls " + movement + " hexes.", false/*popUp*/);
         }
      }

      // apply all the actions to each actor
      for (Character actor : actedCombatants) {
         RequestAction actReq = results.get(actor);
         if (actReq != null && actReq.isAnswered()) {
            actor.applyAction(actReq, _arena);
         }
      }
      return true;
   }

   private void checkForWaitingCharactersToAttack(Map<Character, RequestAction> results, List<Character> allCombatants,
                                                  List<SyncRequest> resultsQueue, List<SyncRequest> waitingList,
                                                  List<Character> actedCombatants, Character actor) {
      List<Character> newAttackers = getAttackerWaitingToAttack(actor);
      for (Character attacker : newAttackers) {
         // If we waited for an opportunity on our last round, and someone
         // moved into range in the next round, we will have no actions left to act
         if (attacker.getActionsAvailable(false) == 0) {
            continue;
         }
         RequestAction delayedAttackReq = attacker.getActionRequest(_arena, actor, null);
         if (delayedAttackReq == null) {
            DebugBreak.debugBreak();
            delayedAttackReq = attacker.getActionRequest(_arena, actor, null);
            if (delayedAttackReq == null) {
               continue;
            }
         }
         if (delayedAttackReq.getActionCount() == 0) {
            // The attacker has no viable way to attack
            continue;
         }
         if (delayedAttackReq.getActionCount() > 1) {
            // The attacker has multiple ways to attack, ask which one they want
            // AND WAIT FOR THE RESPONSE HERE.
            //delayedAttackReq.setResultsQueue(resultsQueue);
            if (_arena.sendObjectToCombatant(attacker, delayedAttackReq)) {
               results.put(attacker, delayedAttackReq);
               synchronized (delayedAttackReq) {
                  try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(delayedAttackReq._lockThis)) {
                     while (!delayedAttackReq.isAnswered()) {
                        try {
                           Rules.diag("### about to wait for delayedAttackReq response in @"
                                      + Integer.toHexString(delayedAttackReq.hashCode()) + ": " + delayedAttackReq);
                           _waitingObjects.add(delayedAttackReq);
                           delayedAttackReq.wait(1000);
                        } catch (InterruptedException e) {
                           e.printStackTrace();
                        } finally {
                           Rules.diag("### done waiting for delayedAttackReq response in @"
                                      + Integer.toHexString(delayedAttackReq.hashCode()) + ": " + delayedAttackReq);
                           _waitingObjects.remove(delayedAttackReq);
                        }
                     }
                  }
               }
            }
         } else // if (delayedAttackReq.getActionCount() == 1)
         {
            // The attacker has only one way to attack, just do it
            delayedAttackReq.setAnswerByOptionIndex(0);
         }
         actedCombatants.add(attacker);
         if (delayedAttackReq.isAttack()) {
            // The waiter is choosing to attack the mover don't allow the
            // mover to move away, until after the attack is resolved.
            // TODO: If the mover didn't defend, allow them to continue moving after the attack is resolved.
            actor.setMoveComplete();
            // The delayed attack needs to have a style type.
            getNextQuestionForAction(allCombatants, resultsQueue, waitingList, attacker, delayedAttackReq);
         }
         if (!delayedAttackReq.isWaitForAttack()) {
            clearWaitingForAttackMap(attacker);
         }
      }
   }

   /**
    * @param allCombatants
    * @param resultsQueue
    * @param waitingList
    * @param actor
    * @param actReq
    */
   private void getNextQuestionForAction(List<Character> allCombatants, List<SyncRequest> resultsQueue, List<SyncRequest> waitingList,
                                         Character actor, RequestAction actReq) {
      // now, any action that needs another question must be asked
      SyncRequest req = actReq.getNextQuestion(actor, allCombatants, _arena);
      if (req != null) {
         if (req.getActionCount() > 1) {
            req.setResultsQueue(resultsQueue);
            boolean sendToCombatant = true;
            // If this is a movement request, and we have not yet
            // followed the complete path that has already been specified,
            // then don't re-ask the user, just accept the current path.
            if (req instanceof RequestMovement) {
               RequestMovement moveReq = (RequestMovement) req;
               if (moveReq.hasMovesLeft()) {
                  sendToCombatant = false;
               }
            }
            if (sendToCombatant) {
               _arena.sendObjectToCombatant(actor, req);
            }
            if (req.isAnswered()) {
               synchronized (resultsQueue) {
                  resultsQueue.add(req);
                  resultsQueue.notifyAll();
               }
            } else {
               waitingList.add(req);
            }
         } else {
            if (req.getActionCount() == 0) {
               req.addOption(new RequestActionOption("no action", RequestActionType.OPT_NO_ACTION, LimbType.BODY, true));
               //req.addOption(ACTION_NONE, "no action", true);
            }
            req.setAnswerByOptionIndex(0);
            synchronized (resultsQueue) {
               resultsQueue.add(req);
               resultsQueue.notifyAll();
            }
         }
      }
   }

   /**
    * @param actor
    * @return
    */
   private List<Character> getAttackerWaitingToAttack(Character actor) {
      List<Character> newAttackers = new ArrayList<>();
      synchronized (_waitingToAttack) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_waitingToAttack)) {
            for (Character attacker : _waitingToAttack) {
               if (attacker.stillFighting()) {
                  List<Integer> orderedTargets = attacker.getOrderedTargetPriorites();
                  for (Integer uniqueId : orderedTargets) {
                     if (uniqueId == actor._uniqueID) {
                        if (attacker.getOrientation().canAttack(attacker, actor, _arena.getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                           newAttackers.add(attacker);
                        }
                     }
                  }
               }
            }
         }
      }
      return newAttackers;
   }

   /**
    * @param actor
    */
   private void checkForAimLossDueToMovement(Character actor) {
      // If the mover moved behind something, blocking the
      // view of someone aiming at him, the targeting is lost.
      // It's also possible that the mover moved between an aimer and a target,
      // so we have to check all aimers, against each of their targets:
      synchronized (_aimingCharacters) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_aimingCharacters)) {
            TreeSet<Character> aimers = new TreeSet<>(_aimingCharacters.keySet());
            for (Character aimer : aimers) {
               Character target = _aimingCharacters.get(aimer);
               if (!_arena.hasLineOfSight(aimer, target)) {
                  if (aimer.getAimDuration(target._uniqueID) > 0) {
                     clearAimingCharacterMap(aimer);
                     _arena.sendMessageTextToAllClients(aimer.getName() + " has lost sight of " + target.getName() + ", and so losses " + aimer.getHisHer()
                                                        + " targeting bonus.", false/*popUp*/);
                     aimer.clearAimDuration();
                  }
               }
            }
         }
      }
   }

   //   private boolean waitForResponses(ArrayList waitingList)
   //   {
   //      while (waitingList.size() > 0) {
   //         SyncRequest req = (SyncRequest) waitingList.remove(0);
   //         if (req != null) {
   //            synchronized (req) {
   //               if (req.getAnswerID() == -1) {
   //                  try {
   //                     req.wait();
   //                  } catch (InterruptedException e) {
   //                     return false;
   //                  }
   //               }
   //            }
   //         }
   //      }
   //      return true;
   //   }

   public final HashMap<Integer, Byte> _berserkingCharactersOriginalTeamID = new HashMap<>();

   private void resolvePainAndInitiative() {
      StringBuilder events = new StringBuilder();
      TreeSet<Character> sortedCombatant = new TreeSet<>(Character.nameComparator);
      sortedCombatant.addAll(_arena.getCombatants());
      int notFightingCount = 0;
      for (Character combatant : sortedCombatant) {
         if (!combatant.getCondition().isAlive() || !combatant.getCondition().isConscious()) {
            if (combatant.isAIPlayer()) {
               notFightingCount++;
            }
         }
      }
      for (Character combatant : sortedCombatant) {
         boolean isAI = combatant.isAIPlayer();
         boolean showDeadOrUnconscious = ((notFightingCount < 5) && !isAI);
         if (!combatant.getCondition().isAlive()) {
            if (showDeadOrUnconscious) {
               events.append(combatant.getName()).append(" is dead.<br/>");
            }
         } else if (!combatant.getCondition().isConscious()) {
            if (showDeadOrUnconscious) {
               events.append(combatant.getName()).append(" is unconscious.<br/>");
            }
         } else {
            DiceSet initiativeDice = Rules.getInitiativeDieType();
            DiceSet painDice = null;
            byte painReduction = 0;
            byte woundReduction = 0;
            byte painLevel = combatant.getPainPenalty(false/*accountForBerserking*/);
            byte reducedPainLevel;
            boolean hasWounds = false;
            if (combatant.isRegenerative()) {
               // If the combatant isn't regenerative, don't worry about wounds
               hasWounds = (combatant.getWounds() > 0) || (combatant.getCondition().getPenaltyMove() > 0);
            }

            if ((painLevel > 0) || hasWounds) {
               painDice = Rules.getPainReductionDice(combatant.getAttributeLevel(Attribute.Toughness));
               painDice = combatant.adjustDieRoll(painDice, RollType.PAIN_RECOVERY, null/*target*/);
               String rollMessage = combatant.getName() + ", roll to reduce your pain level from " + painLevel;
               painReduction = (byte) (painDice.roll(true/*allowExplodes*/, combatant,
                                                     RollType.PAIN_RECOVERY, rollMessage));
               if (painReduction < 0) {
                  painReduction = 0;
               }
               if (hasWounds && (painReduction >= 10)) {
                  if (combatant.isRegenerative()) {
                     woundReduction = (byte) (painReduction / 10);
                     combatant.regenerateWound(woundReduction);
                  }
               }
            }
            initiativeDice = combatant.adjustDieRoll(initiativeDice, RollType.INITIATIVE, null/*target*/);
            String rollMessage = combatant.getName() + ", roll for initiative.";
            int initiativeRoll = initiativeDice.roll(false/*allowExplodes*/, combatant,
                                                     RollType.INITIATIVE, rollMessage);
            combatant.reducePain(painReduction);
            reducedPainLevel = combatant.getPainPenalty(false/*accountForBerserking*/);
            combatant.setInitiativeActionsAndMovementForNewTurn(initiativeRoll);
            Wound magicalBurn = combatant.getNewTurnBurnWound();
            if (magicalBurn != null) {
               // apply this directly to the condition, so it can't affect the spell itself.
               combatant.getCondition().applyWound(magicalBurn, _arena, combatant);
            }
            _arena.sendEventToAllClients(combatant);
            if (painDice != null) {
               events.append(combatant.getName()).append(" rolls ").append(painDice);
               events.append(" for ");
               if (painLevel > 0) {
                  events.append("pain");
                  if (hasWounds) {
                     events.append(" and ");
                  }
               }
               if (hasWounds) {
                  events.append("wounds");
               }
               events.append(", rolling ").append(painDice.getLastDieRoll());
               if (painReduction > 0) {
                  events.append(", reducing pain from ").append(painLevel);
                  events.append(" to ").append(reducedPainLevel);
               } else if (reducedPainLevel > 0) {
                  events.append(", leaving ").append(combatant.getHisHer()).append(" pain at ").append(reducedPainLevel);
               }
               if (woundReduction > 0) {
                  if (painReduction > 0) {
                     events.append(" and ");
                  } else {
                     events.append(", ");
                  }
                  events.append("regenerating ").append(woundReduction).append(" wound");
                  if (woundReduction > 1) {
                     events.append("s");
                  }
                  events.append("!");
               }
               if (combatant.getWounds() > 0) {
                  events.append(" (wounds = ").append(combatant.getWounds()).append(").");
               }
               events.append("<br/>");
               if (magicalBurn != null) {
                  events.append(combatant.getName()).append(" is holding an over-powered spell, causing ");
                  if (reducedPainLevel > 0) {
                     events.append("an additional ");
                  }
                  events.append(magicalBurn.getPain()).append(" points of pain");
                  if (reducedPainLevel > 0) {
                     events.append(", bring ").append(combatant.getHisHer()).append(" pain total to ")
                           .append(combatant.getPainPenalty(false/*accountForBerserking*/));
                  }
                  events.append(".<br/>");
               }
               if (combatant.getPainPenalty(true/*accountForBerserking*/) >= Rules.getCollapsePainLevel(combatant.getAttributeLevel(Attribute.Toughness))) {
                  events.append(combatant.getName()).append(" collapses from the pain.<br/>");
                  combatant.collapseFromPain(_arena.getCombatMap());
               }
            } else {
               if (combatant.getWounds() > 0) {
                  events.append(combatant.getName()).append(" has ");
                  events.append(combatant.getWounds()).append(" wounds.");
               }
               StringBuilder limbWounds = new StringBuilder();
               for (Limb limb : combatant.getLimbs()) {
                  if (limb.isSevered()) {
                     if (limbWounds.length() > 0) {
                        limbWounds.append(", ");
                     }
                     limbWounds.append(limb.getName()).append(": severed");
                  }
                  if (limb.isCrippled()) {
                     if (limbWounds.length() > 0) {
                        limbWounds.append(", ");
                     }
                     limbWounds.append(limb.getName()).append(": crippled");
                  }
                  if (limb.getWoundPenalty() > 0) {
                     if (limbWounds.length() > 0) {
                        limbWounds.append(", ");
                     }
                     limbWounds.append(limb.getName()).append(": penalty of ").append(limb.getWoundPenalty());
                  }
               }
               if (limbWounds.length() > 0) {
                  if (combatant.getWounds() > 0) {
                     events.append(" and the following limb penalties: ").append(limbWounds);
                  } else {
                     events.append(combatant.getName()).append(" has the following limb penalties: ").append(limbWounds);
                  }
               }
               if ((combatant.getWounds() > 0) || (limbWounds.length() > 0)) {
                  events.append(".<br/>");
               }
            }
            Set<IHolder> holders = combatant.getHolders();
            if ((holders != null) && (holders.size() > 0)) {
               events.append(combatant.getName());
               events.append(" is held by ");
               boolean first = true;
               for (IHolder holder : holders) {
                  if (holder instanceof Character) {
                     Character holdingChar = (Character) holder;
                     if (!holdingChar.getCondition().isConscious() || !holdingChar.getCondition().isAlive()) {
                        holdingChar.releaseHold();
                        continue;
                     }
                  }
                  if (!first) {
                     events.append(", ");
                  }
                  events.append(holder.getName()).append(" at level ").append(combatant.getHoldLevel(holder));
                  first = false;
               }
               events.append("<br/>");
            }
            byte newPain = combatant.getPainPenalty(false/*accountForBerserking*/);
            if (combatant.isBerserking()) {
               boolean recoveryAttempted = false;
               if (combatant.isUnderSpell(SpellEnrage.NAME) == null) {
                  Character target = _arena.getCharacter(combatant._targetID);
                  if ((target == null) || (!target.stillFighting())) {
                     recoveryAttempted = true;
                     byte iq = combatant.getAttributeLevel(Attribute.Intelligence);
                     DiceSet berserkSaveDice = Rules.getDice(iq, (byte) 1/*actions*/, Attribute.Intelligence, RollType.BERSERK_RECOVERY);
                     berserkSaveDice = combatant.adjustDieRoll(berserkSaveDice, RollType.BERSERK_RECOVERY, null/*target*/);
                     rollMessage = combatant.getName() + ", roll end your berserking state.";
                     int diceRoll = berserkSaveDice.roll(true/*allowExplodes*/, combatant, RollType.BERSERK_RECOVERY, rollMessage);
                     events.append(combatant.getName()).append("'s target (");
                     events.append(target != null ? target.getName() : "").append(") is no longer fighting, so ");
                     events.append(combatant.getHeShe()).append(" has a chance that he recovers from being berserk.<br/>");
                     events.append(combatant.getName()).append(" rolls 1-action IQ (").append(berserkSaveDice);
                     events.append("), rolling ").append(berserkSaveDice.getLastDieRoll());
                     events.append(" = ").append(diceRoll);
                     boolean anyTeam = (berserkSaveDice.getDiceCount() == (diceRoll - berserkSaveDice.getDiceCount(DieType.D1)));
                     if ((diceRoll >= (newPain + 3)) && !anyTeam) {
                        events.append(", which is equal to, or above, the new pain level of ").append(newPain);
                        events.append(" +3 (").append(newPain + 3).append("), so ").append(combatant.getName());
                        events.append(" recovers from berserkness!<br/>");
                        combatant.setIsBerserking(false);
                        Byte originalTeamID = _berserkingCharactersOriginalTeamID.remove(combatant._uniqueID);
                        if (originalTeamID != null) {
                           combatant._teamID = originalTeamID;
                        }
                     } else {
                        if (anyTeam) {
                           events.append(", which is all '1's, so he will attack the near person, even if on the same team!<br/>");
                           if (combatant._teamID != TEAM_INDEPENDENT) {
                              _berserkingCharactersOriginalTeamID.put(combatant._uniqueID, combatant._teamID);
                              combatant._teamID = TEAM_INDEPENDENT;
                           }
                        } else {
                           events.append(", which is below the new pain level of ").append(newPain);
                           events.append(" +3 (").append(newPain + 3).append("), so ").append(combatant.getName()).append(" remains berserk.<br/>");
                        }

                        Character newTarget = _arena.getNearestTarget(combatant, anyTeam);
                        if (newTarget != null) {
                           events.append("The next nearest target is ").append(newTarget.getName());
                           events.append(", so ").append(combatant.getName());
                           events.append(" is now targeting ").append(newTarget.getName());
                           events.append(".<br/>");

                           combatant._targetID = newTarget._uniqueID;
                        }
                     }
                  }
               }
               if (!recoveryAttempted) {
                  events.append(combatant.getName()).append(" is still berserking.<br/>");
               }
            }
            events.append(combatant.getName()).append(" rolls ").append(initiativeDice.getLastDieRoll());
            events.append(" on ").append(initiativeDice);
            events.append(" for initiative, resulting in an initiative of ").append(combatant.getInitiative());
            events.append(combatant.describeActiveSpells());
            events.append("<br/>");
         }
      }
      _arena.sendMessageTextToAllClients(events.toString(), false/*popUp*/);
   }

   final HashSet<Character> _waitingToAttack = new HashSet<>();

   private void addWaitingForAttack(Character waitingAttacker) {
      synchronized (_waitingToAttack) {
         _lock_waitingToAttack.check();
         _waitingToAttack.add(waitingAttacker);
      }
   }

   private void clearWaitingForAttackMap(Character waitingAttacker) {
      synchronized (_waitingToAttack) {
         _lock_waitingToAttack.check();
         _waitingToAttack.remove(waitingAttacker);
      }
   }

   final HashMap<Character, Character> _aimingCharacters      = new HashMap<>();
   final Semaphore                     _lock_aimingCharacters = new Semaphore("Battle_aimingCharacters", CombatSemaphore.CLASS_BATTLE_aimingCharacters);
   final Semaphore                     _lock_waitingToAttack  = new Semaphore("Battle_waitingToAttack", CombatSemaphore.CLASS_BATTLE_waitingToAttack);

   private void addAimingCharacter(Character aimer, Character target) {
      synchronized (_aimingCharacters) {
         _lock_aimingCharacters.check();
         _aimingCharacters.put(aimer, target);
      }
   }

   private void clearAimingCharacterMap(Character aimer) {
      synchronized (_aimingCharacters) {
         _lock_aimingCharacters.check();
         _aimingCharacters.remove(aimer);
      }
   }

   public List<Character> getCharactersAimingAtCharacter(Character target) {
      List<Character> results = new ArrayList<>();
      if (target != null) {
         synchronized (_aimingCharacters) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_aimingCharacters)) {
               TreeSet<Character> aimers = new TreeSet<>(_aimingCharacters.keySet());
               for (Iterator<Character> iter = aimers.iterator(); iter.hasNext(); ) {
                  Character aimer = iter.next();
                  if (aimer.stillFighting()) {
                     Character aimersTarget = _aimingCharacters.get(aimer);
                     if ((aimersTarget != null) && (aimersTarget._uniqueID == target._uniqueID)) {
                        results.add(aimer);
                     }
                  } else {
                     iter.remove();
                  }
               }
            }
         }
      }
      return results;
   }

   public Element getXmlObject(Document parentDoc) {
      Element mainElement = parentDoc.createElement("Battle");
      mainElement.setAttribute("turnCount", String.valueOf(_turnCount));
      mainElement.setAttribute("roundCount", String.valueOf(_roundCount));
      mainElement.setAttribute("phaseCount", String.valueOf(_phaseCount));
      return mainElement;
   }

   public boolean serializeFromXmlObject(Node doc) {
      if (!doc.getNodeName().equals("Battle")) {
         return false;
      }
      NamedNodeMap namedNodeMap = doc.getAttributes();
      if (namedNodeMap == null) {
         return false;
      }
      _turnCount = Integer.parseInt(namedNodeMap.getNamedItem("turnCount").getNodeValue());
      _roundCount = Integer.parseInt(namedNodeMap.getNamedItem("roundCount").getNodeValue());
      _phaseCount = Integer.parseInt(namedNodeMap.getNamedItem("phaseCount").getNodeValue());
      return true;
   }
}
