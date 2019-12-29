/*
 * Created on May 10, 2006
 *
 */
package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import ostrowski.DebugBreak;
import ostrowski.combat.client.MessageDialog;
import ostrowski.combat.client.RequestUserInput;
import ostrowski.combat.client.ui.AutoRunBlock;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.SkillType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.spells.IAreaSpell;
import ostrowski.combat.common.things.Limb;
import ostrowski.combat.common.things.LimbType;
import ostrowski.combat.common.things.Shield;
import ostrowski.combat.common.things.Weapon;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.protocol.MessageText;
import ostrowski.combat.protocol.ServerStatus;
import ostrowski.combat.protocol.TargetPriorities;
import ostrowski.combat.protocol.request.RequestAction;
import ostrowski.combat.protocol.request.RequestArenaEntrance;
import ostrowski.combat.protocol.request.RequestArenaEntrance.TeamMember;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.CombatSemaphore;
import ostrowski.util.Diagnostics;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoTracker;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class Arena implements Enums, IMapListener
{
   final         Semaphore       _lock_combatants        = new Semaphore("Arena_combatants", CombatSemaphore.CLASS_ARENA_combatants);
   final         Semaphore       _lock_mapCombatantsToAI = new Semaphore("Arena_mapCombatantsToAI", CombatSemaphore.CLASS_ARENA_mapCombatantsToAI);
   final         Semaphore       _lock_proxyList         = new Semaphore("Arena_proxyList", CombatSemaphore.CLASS_ARENA_proxyList);
   final         Semaphore       _lock_locationRequests  = new Semaphore("Arena_locationRequests", CombatSemaphore.CLASS_ARENA_locationRequests);
   private final List<Character> _combatants             = new ArrayList<>();
   private final List<ClientProxy>           _proxyList           = new ArrayList<>();
   private final List<Character>             _charactersWaitingToConnect = new ArrayList<>();
   private final Map<Character, AI>          _mapCombatantToAI    = new HashMap<>();
   private final Set<Character>              _localCombatants     = new HashSet<>();
   private final Map<Character, ClientProxy> _mapCombatantToProxy = new HashMap<>();
   private final Map<ClientProxy, Character> _mapProxyToCombatant = new HashMap<>();
   public  Battle                          _battle              = null;
   private CombatServer                    _server              = null;
   private CombatMap                       _combatMap           = null;
   private CombatMap                       _autoRunMap          = null;
   private AutoRunBlock                    _autoRunBlock        = null;

   transient private final MouseOverCharacterInfoPopup _mouseOverCharInfoPopup = new MouseOverCharacterInfoPopup();
   transient private final RightClickPopupMenu _characterMenuPopup = new RightClickPopupMenu(this);

   public Arena(CombatServer server, short sizeX, short sizeY) {
      _server    = server;
      _combatMap = new CombatMap(sizeX, sizeY, server._diag);
   }
   public void setSize(short newX, short newY) {
      _combatMap.setSize(newX, newY);
   }
   public boolean addCombatant(Character combatant, byte team, short startingLocationX, short startingLocationY,
                               AI_Type aiEngineType) {
      ArenaLocation startingLocation = _combatMap.getLocation(startingLocationX, startingLocationY);
      if (startingLocation == null) {
         return false;
      }
      List<Character> chars = startingLocation.getCharacters();
      if ((chars != null) && (chars.size() > 0)) {
         return false;
      }
      if (!_combatMap.addCharacter(combatant, startingLocation, null/*clientProxy*/)) {
         return false;
      }
      combatant._teamID = team;
      if (combatant._uniqueID == -1) {
         combatant._uniqueID = ClientProxy.getNextServerID();
         while (getCharacter(combatant._uniqueID) != null) {
            combatant._uniqueID = ClientProxy.getNextServerID();
         }
      }
      return addCombatant(combatant, aiEngineType, true/*setInitiativeAndSpendActions*/);
   }
   private boolean addCombatant(Character combatant, AI_Type AIEngineType, boolean setInitiativeAndSpendActions) {
      Character existingCharacter = getCharacter(combatant._uniqueID);
      if (existingCharacter != null) {
         if (!combatant.equals(existingCharacter)) {
            DebugBreak.debugBreak();
            return false;
         }
      }

      _combatMap.updateCombatant(combatant, false/*checkTriggers*/);
      if (AIEngineType != null) {
         AI ai = new AI(combatant, AIEngineType);
         _mapCombatantToAI.put(combatant, ai);
      }
      else {
         // add as a local player
         _localCombatants.add(combatant);
      }
      addCombatant(combatant, false/*checkForAutoStart*/);
      if (setInitiativeAndSpendActions) {
         DiceSet initiativeDice = Rules.getInitiativeDieType();
         int initiativeRoll = initiativeDice.roll(false/*allowExplodes*/);
         combatant.setInitiativeActionsAndMovementForNewTurn(initiativeRoll);
         // slow the new guy down, so he doesn't show up in round 5 with 5 actions left....
         for (int i=1 ; i<_battle._roundCount ; i++) {
            combatant.endRound();
         }
      }
      return true;
   }

   final HashMap<String, Integer> _registeredNames = new HashMap<>();
   public boolean addCombatant(Character combatant, byte team, byte combatantIndexOnTeam, ClientProxy clientProxy, boolean checkForAutoStart) {
      if (combatantIndexOnTeam == -1) {
          combatantIndexOnTeam = _combatMap.getAvailableCombatantIndexOnTeam(team);
      }
      String newCombatantsName = combatant.getName();
      Integer currentDupCount = _registeredNames.get(newCombatantsName);
      boolean foundWaiting = false;
      boolean nameChanged = false;
      if (currentDupCount == null) {
         _registeredNames.put(newCombatantsName, 1);
      }
      else {
         // Look for a character with a matching _uniqueID
         for (Character waitingCharacter : _charactersWaitingToConnect) {
            if (waitingCharacter._uniqueID == combatant._uniqueID) {
               _charactersWaitingToConnect.remove(waitingCharacter);
               foundWaiting = true;
               break;
            }
         }
         if (!foundWaiting) {
            // Try looking up by name, and set the _uniqueID
            for (Character waitingCharacter : _charactersWaitingToConnect) {
               if (waitingCharacter.getName().equals(combatant.getName())) {
                  _charactersWaitingToConnect.remove(waitingCharacter);
                  waitingCharacter._uniqueID = combatant._uniqueID;
                  foundWaiting = true;
                  break;
               }
            }
            if (!foundWaiting) {
               if (currentDupCount == 1) {
                  for (Character existingCombatant : getCombatants()) {
                     if (existingCombatant.getName().equals(newCombatantsName)) {
                        existingCombatant.setName(newCombatantsName + "-1");
                        sendCharacterUpdate(existingCombatant, null);
                        break;
                     }
                  }
               }
               int curCount = currentDupCount + 1;
               _registeredNames.put(newCombatantsName, curCount);
               combatant.setName(newCombatantsName + "-" + curCount);
               nameChanged = true;
            }
         }
      }
      if (!foundWaiting) {
         if ((combatantIndexOnTeam == -1) ||
                  (!_combatMap.addCharacter(combatant, team, combatantIndexOnTeam, clientProxy))) {
            if (checkForAutoStart) {
               checkForAutoStart();
            }
            return false;
         }
      }

      // We can't send out an update until after the character exists on the map,
      // or the AI agents will try to recompute visibility, which is not knowable yet.
      if (nameChanged) {
         sendCharacterUpdate(combatant, null);
      }
      combatant._teamID = team;
      if (clientProxy != null) {
         _mapCombatantToProxy.put(combatant, clientProxy);
         _mapProxyToCombatant.put(clientProxy, combatant);
         clientProxy.setClientName(combatant.getName());
         combatant.setClientProxy(clientProxy, _combatMap, null/*diag*/);
         combatant._uniqueID = clientProxy.getClientID();
      }
      else {
         combatant._uniqueID = ClientProxy.getNextServerID();
         String aiEngineStr = _combatMap.getStockAIName(team)[combatantIndexOnTeam];
         AI_Type aiType = AI_Type.getByString(aiEngineStr);
         if (aiType == null) {
            if (CombatServer._REMOTE_AI_NAME.equals(aiEngineStr)) {
               _charactersWaitingToConnect.add(combatant);
            }
            else {
               if (!aiEngineStr.equalsIgnoreCase("Local")) {
                  DebugBreak.debugBreak("Can't find AI for " + aiEngineStr);
                  // Assume 'Local' for now...
               }
               _localCombatants.add(combatant);
            }
         }
         else {
            AI ai = new AI(combatant, aiType);
            _mapCombatantToAI.put(combatant, ai);
            if (aiType == AI_Type.GOD) {
               _combatMap.setAllLocationsAsKnownBy(combatant);
            }
         }
      }
      addCombatant(combatant, checkForAutoStart);
      return true;
   }

   private void addCombatant(Character combatant, boolean checkForAutoStart) {
      // Set the visibility without considering the facing.
      // This will allow the player to know the terrain behind them,
      // even though they will not see the objects and characters there.
      // (We can't do this until after we set the combatant's _uniqueID)
      _combatMap.recomputeKnownLocations(combatant, false/*basedOnFacing*/, false/*setVisibility*/, null/*locsToRedraw*/);

      if (!combatant.hasInitiativeAndActionsEverBeenInitialized()) {
         DiceSet initiativeDice = Rules.getInitiativeDieType();
         int initiativeRoll = initiativeDice.roll(false/*allowExplodes*/);
         combatant.setInitiativeActionsAndMovementForNewTurn(initiativeRoll);
      }

      synchronized (_combatants) {
         _lock_combatants.check();
         for (Character character : _combatants) {
            if (character.getName().equals(combatant.getName())) {
               _combatants.remove(character);
               break;
            }
         }
         _combatants.add(combatant);
      }
      sendServerStatus(null);
      _combatMap.registerAsWatcher(combatant._mapWatcher, _server._diag);
      recomputeAllTargets(combatant/*combatantSwitchingSides*/);

      if (checkForAutoStart) {
         checkForAutoStart();
      }
   }

   private void recomputeAllKnownLocations() {
      // Make sure all the AI player have selected targets.
      synchronized(_mapCombatantToAI) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_mapCombatantsToAI)) {
            TreeSet<Character> aiCombatants = new TreeSet<>(_mapCombatantToAI.keySet());
            for (Character aiCombatant : aiCombatants) {
               _combatMap.recomputeKnownLocations(aiCombatant, false/*basedOnFacing*/, false/*setVisibility*/, null/*locsToRedraw*/);
            }
         }
      }
      // Make sure all the local players have something targeted too.
      for (Character localCombatant : _localCombatants) {
         _combatMap.recomputeKnownLocations(localCombatant, false/*basedOnFacing*/, false/*setVisibility*/, null/*locsToRedraw*/);
      }
   }


   public boolean checkForAutoStart() {
      if (!_charactersWaitingToConnect.isEmpty()) {
         return false;
      }
      int combatants = _mapCombatantToProxy.size() + _mapCombatantToAI.size() + _localCombatants.size();
      if (_combatMap.getCombatantsCount() != combatants) {
         return false;
      }
      beginBattle();
      return true;
   }

   public void removeCombatant(Character combatant) {
      if (combatant != null) {
         _mapCombatantToProxy.remove(combatant);
         _mapCombatantToAI.remove(combatant);
         _localCombatants.remove(combatant);
         synchronized (_combatants) {
            _lock_combatants.check();
            _combatants.remove(combatant);
         }
         sendMessageTextToAllClients(combatant.getName() + " has exited the arena.", false/*popUp*/);
         sendServerStatus(null);
         _combatMap.unregisterAsWatcher(combatant._mapWatcher, _server._diag);
      }
   }
   public void removeCombatant(Character combatant, ClientProxy clientProxy) {
      Character localCombatant = _mapProxyToCombatant.remove(clientProxy);
      if (localCombatant != null) {
         removeCombatant(localCombatant);
         _combatMap.unregisterAsWatcher(combatant._mapWatcher, _server._diag);
      }
      else {
         removeCombatant(combatant);
      }
   }

   public void beginBattle() {
      synchronized (_combatants) {
         _lock_combatants.check();
         if (_combatants.size() <= 1) {
            return;
         }
      }
      if (_battle != null) {
         return;
      }

      recomputeAllTargets(null);

      _battle = new Battle("BattleThread", this, _server);
      if (_paused) {
         _battle.onPause();
      }
      if (_autoRunBlock != null) {
         _battle._resetMessageBufferOnStart = false;
      }
      _battle.start();
      if (_autoRunBlock != null) {
         _autoRunBlock.battleStarted();
      }

   }

   public void recomputeAllTargets(Character combatantSwitchingSides)
   {
      // Make sure all the AI player have selected targets.
      // We have to lock the _combatants list before we lock the mapCombatantsToAI lock,
      // because the call to recomputeTargetForCharacter will lock the _combatants list,
      // and this would be an order violation, resulting in a possible deadlock
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            synchronized(_mapCombatantToAI) {
               try (SemaphoreAutoTracker sat2 = new SemaphoreAutoTracker(_lock_mapCombatantsToAI)) {
                  TreeSet<Character> aiCombatants = new TreeSet<>(_mapCombatantToAI.keySet());
                  for (Character aiCombatant : aiCombatants) {
                     recomputeTargetForCharacter(aiCombatant, combatantSwitchingSides);
                  }
               }
            }
         }
      }
      // Make sure all the local players have something targeted too.
      for (Character localCombatant : _localCombatants) {
         recomputeTargetForCharacter(localCombatant, combatantSwitchingSides);
      }
   }

   public void recomputeTargetForCharacter(Character combatant, Character combatantSwitchingSides/*unused*/)
   {
      ArrayList<Character> targetPriorities = new ArrayList<>();
      ArrayList<Integer> targetPriorityIDs = new ArrayList<>();
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Character otherCombatant : _combatants) {
               if (otherCombatant._uniqueID != combatant._uniqueID) {
                  boolean sameTeam = (otherCombatant._teamID == combatant._teamID);
                  if ((!sameTeam) || (combatant._teamID == TEAM_INDEPENDENT)) {
                     targetPriorities.add(otherCombatant);
                     targetPriorityIDs.add(otherCombatant._uniqueID);
                  }
               }
            }
         }
      }
      combatant.setTargetPriorities(targetPriorityIDs);
      // Is this combatant an AI player?
      AI ai = _mapCombatantToAI.get(combatant);
      if (ai != null) {
         // If so, update the AI engine too.
         ai.updateTargetPriorities(targetPriorities);
      }
   }
   public void terminateBattle() {
      if (_battle == null) {
         return;
      }
      _battle.terminateBattle();
      _battle = null;
   }

   public boolean hasNonAICombatantsStillFighting() {
      if (_battle == null) {
         return false;
      }
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Character combatant : _combatants) {
               if (combatant.stillFighting()) {
                  // Make sure the client is still connected
                  ClientProxy proxy = _mapCombatantToProxy.get(combatant);
                  if (proxy != null) {
                     if (proxy.isAlive()) {
                        return true;
                     }
                  }
                  else if (_localCombatants.contains(combatant)) {
                     return true;
                  }
               }
            }
         }
      }
      return false;
   }

   public boolean stillFighting() {
      return stillAnyoneStillFighting() || (hasNonAICombatantsStillFighting() && hasExitTriggers());
   }
   public boolean stillAnyoneStillFighting() {
      if (_battle == null) {
         return false;
      }

      int aliveCount = 0;
      byte teamAlive = -1;
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Character combatant : _combatants) {
               if (combatant.stillFighting()) {
                  boolean stillFighting = false;
                  // Make sure the client is still connected
                  ClientProxy proxy = _mapCombatantToProxy.get(combatant);
                  if (proxy != null) {
                     if (proxy.isAlive()) {
                        stillFighting = true;
                     }
                  }
                  else if (_mapCombatantToAI.get(combatant) != null) {
                     stillFighting = true;
                  }
                  else if (_localCombatants.contains(combatant)) {
                     stillFighting = true;
                  }
                  if (stillFighting) {
                     if (++aliveCount == 1) {
                        teamAlive = combatant._teamID;
                     }
                     else {
                        if ((teamAlive == TEAM_INDEPENDENT) ||
                                 (teamAlive != combatant._teamID)  ||
                                 (combatant._teamID == TEAM_INDEPENDENT))  {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   public boolean isTeamAlive(int teamIndex) {
      if (_battle != null) {
         synchronized (_combatants) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
               for (Character combatant : _combatants) {
                  if (combatant._teamID == teamIndex) {
                     if (combatant.stillFighting()) {
                        return true;
                     }
                  }
               }
            }
         }
      }
      return false;

   }
   public List<Character> orderCombatantsByActionsAndInitiative()
   {
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (int i=0 ; i<_combatants.size(); i++) {
               Character combatantI = _combatants.get(i);
               for (int j=i+1 ; j<_combatants.size(); j++) {
                  Character combatantJ = _combatants.get(j);
                  byte actionsI = combatantI.getActionsAvailable(false/*usedForDefenseOnly*/);
                  byte actionsJ = combatantJ.getActionsAvailable(false/*usedForDefenseOnly*/);

                  if (actionsI > actionsJ) {
                     continue;
                  }
                  if (actionsI == actionsJ) {
                     byte initI = combatantI.getInitiative();
                     byte initJ = combatantJ.getInitiative();
                     if (initI > initJ) {
                        continue;
                     }
                     if (initI == initJ) {
                        if (combatantI.compareTo(combatantJ) > 0) {
                           continue;
                        }
                     }
                  }

                  _combatants.set(j, combatantI);
                  _combatants.set(i, combatantJ);
                  combatantI = combatantJ;
               }
            }
         }
      }
      return _combatants;
   }
   public List<Character> getCombatants() { return _combatants; }

   public Character getCharacter(int charID) {
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            // TODO : when I lock the _combatants lock, lock the same lock on calls to getCombatants()
            for (Character character : _combatants) {
               if (character._uniqueID == charID) {
                  return character;
               }
            }
         }
      }
      return null;
   }

   public static final int PLAYBACK_MODE_OFF    = 0;
   public static final int PLAYBACK_MODE_RECORD = 1;
   public static final int PLAYBACK_MODE_PLAY   = 2;
   public       int                    _playbackMode    = PLAYBACK_MODE_OFF;
   public final ArrayList<SyncRequest> _recordedActions = null;//new ArrayList<>();
   public       int                    _playbackIndex   = 0;

   public boolean sendObjectToCombatant(final Character combatant, SerializableObject obj) {
      ClientProxy proxy = (_mapCombatantToProxy.get(combatant));
      if (proxy != null) {
         return proxy.sendObject(obj);
      }

      if (_localCombatants.contains(combatant)) {
         if (obj instanceof SyncRequest) {
            SyncRequest req = (SyncRequest) obj;
            SyncRequest response = null;
            if (_recordedActions != null) {
               if (_recordedActions.size() > _playbackIndex) {
                  response = _recordedActions.get(_playbackIndex);
               }
               boolean allowBackup = (_recordedActions.size() > 0) && (_mapCombatantToProxy.size() == 0) && CombatServer.allowBackup();
               boolean responseFoundInPlayback = false;
               if (response != null) {
                  if (response.isSameQuestion(req)) {
                     req.copyAnswer(response);
                     stopWaitingForResponse(req);
                     _playbackIndex++;
                     responseFoundInPlayback = true;
                  }
                  else {
                     Rules.diag("Expected response to:\n" + response + "\nBut got response to:\n" + obj);
                     DebugBreak.debugBreak();
                  }
               }
               if (!responseFoundInPlayback) {
                  askLocalPlayer(req, allowBackup, combatant);
               }
            }
            else {
               askLocalPlayer(req, false/*allowBackup*/, combatant);
            }
            return true;
         }
         if (obj instanceof MessageText) {
            final MessageText msgText = (MessageText) obj;
            if (msgText.isPopUp()) {
               CombatServer._this.getShell().getDisplay().asyncExec(new Runnable() {
                  @Override
                  public void run() {
                     MessageDialog msgDlg = new MessageDialog(CombatServer._this.getShell(), SWT.ICON | SWT.MODELESS);
                     msgDlg.setTargetName(combatant.getName());
                     msgDlg.open(msgText.getText(), msgText.isPublic());
                  }
               });
            }
         }
         // other information doesn't need to be sent to local players
         return true;
      }

      AI ai = _mapCombatantToAI.get(combatant);
      if (ai == null) {
         Rules.diag("unable to find proxy to send message to:" + obj);
//         Rules.debugBreak();
         return false;
      }

      if (ai.processObject(obj, this, _combatMap, null/*display*/)) {
         return true;
      }
      DebugBreak.debugBreak("AI did not processObject");
      if (ai.processObject(obj, this, _combatMap, null/*display*/)) {
         return true;
      }

      if (obj instanceof SyncRequest) {
         // we couldn't determine the outcome using AI. prompt for help
         askLocalPlayer((SyncRequest) obj, false/*allowBackup*/, combatant);
      }
      return false;
   }
   /**
    */
   private final ArrayList<SyncRequest> _locationRequests = new ArrayList<>();
   public static final ArrayList<RequestUserInput> _activeRequestUserInputs = new ArrayList<>();

   private void askLocalPlayer(final SyncRequest req, final boolean allowBackup, final Character combatant)
   {
      if ((req instanceof RequestLocation) || (req instanceof RequestMovement)) {
         synchronized (_locationRequests) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationRequests)) {
               Rules.diag("askLocalPlayer, _locationRequest.size=" + _locationRequests.size());
               if (_locationRequests.size() > 0) {
                 // DebugBreak.debugBreak("multiple local player requests at once");
               }
               _locationRequests.add(req);
            }
         }
      }
      if (!_server.getShell().isDisposed()) {
         Display display = _server.getShell().getDisplay();
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               // Hide the hexes that the player can't see
               _server._map.recomputeVisibility(combatant, null/*diag*/);
               if (req instanceof RequestLocation) {
                  RequestLocation reqLoc = (RequestLocation) req;
                  // selectHex does its own redraw(), so we avoid calling that before calling selectHex
                  _server._map.requestLocation(reqLoc);
                  // continue hiding the hexes that the player can't see until they click on a valid movement location.
                  if (_recordedActions != null) {
                     _recordedActions.add(req);
                     _playbackIndex++;
                  }
               }
               else if (req instanceof RequestMovement) {
                  RequestMovement reqMove = (RequestMovement) req;
                  // selectHex does its own redraw(), so we avoid calling that before calling selectHex
                  _server._map.requestMovement(reqMove);
                  // continue hiding the hexes that the player can't see until they clink on a valid movement location.
                  if (_recordedActions != null) {
                     _recordedActions.add(req);
                     _playbackIndex++;
                  }
               }
               else {
                  _server._map.redraw();
                  RequestUserInput reqUI = new RequestUserInput(_server.getShell(),
                                                                SWT.ICON_QUESTION | SWT.MODELESS,
                                                                req, allowBackup,
                                                                _server._map, combatant,
                                                                Configuration.showChit());
                  reqUI.setDefault(req.getDefaultIndex());
                  reqUI.setTitle("Question from the Server");
                  _activeRequestUserInputs.add(reqUI);

                  Object answer = reqUI.open();

                  _activeRequestUserInputs.remove(reqUI);
                  while (!_activeRequestUserInputs.isEmpty()) {
                     // Set another open dialog to have focus
                     RequestUserInput next = _activeRequestUserInputs.get(0);
                     if (next._shell.isDisposed())
                     {
                        _activeRequestUserInputs.remove(next);
                     }
                     else
                     {
                        next._shell.setFocus();
                        break;
                     }
                  }

                  if (answer != null) {
                     if (answer instanceof Integer) {
                         req.setAnswerByOptionIndex(((Integer) answer));
                     }
                     else {
                         req.setCustAnswer((String) answer);
                     }
                  }
                  // Since the player has made his/her choice,
                  // stop hiding the hexes that the player can't see
                  _server._map.recomputeVisibility(null/*self*/, null/*diag*/);
                  _server.redrawMap();

                  req.set_backupSelected(reqUI.isBackupSelected());
                  stopWaitingForResponse(req);

                  if (_recordedActions != null) {
                     if (reqUI.isBackupSelected()) {
                        // remove the last question in our playback record
                        SyncRequest lastAction = _recordedActions.remove(_recordedActions.size() - 1);
                        // If the last action was a movement request, remove all the movement requests
                        // until we remove a non movement request (the RequestAction whose answer was 'move')
                        while (lastAction instanceof RequestMovement) {
                           lastAction = _recordedActions.remove(_recordedActions.size() - 1);
                        }
                        restart();
                     }
                     else {
                        _recordedActions.add(req);
                        _playbackIndex++;
                     }
                  }
               }
            }
         });

//         synchronized (waitLock) {
//            try {
//               waitLock.wait();
//            } catch (InterruptedException e) {
//               e.printStackTrace();
//            }
//         }
      }
   }

   private static void stopWaitingForResponse(SyncRequest req)
   {
      List<SyncRequest> resultsQueue = req.getResultsQueue();
      if (resultsQueue != null) {
         synchronized (resultsQueue) {
            resultsQueue.add(req);
            resultsQueue.notifyAll();
         }
      }
      else {
         synchronized(req) {
            req.notifyAll();
         }
      }
   }

   public void sendMessageTextToParticipants(String message, Character char1, Character char2) {
      ArrayList<Character> participants = new ArrayList<>();
      participants.add(char1);
      if (char1 != char2) {
         participants.add(char2);
      }
      sendMessageTextToParticipants(message, participants);
   }
   public void sendMessageTextToParticipants(String message, ArrayList<Character> participants) {
      // send this message to each of the involved parties (attacker & defender)
      // but the message content is currently in HTML, and we can't display that
      // in a popup yet, so for now we manually convert it out of HTML:
//      message = "<span style=\"color:red\">300 priest casts a 4-power 'Bless' spell on Darryl, the Orc Lord:<br/></span>The target, Darryl, the Orc Lord is 1 hexes away.<br/><table border=1></table>\n"
//+" <table>\n"
//+"   <tr class='row0'>\n"
//+"      <td>4</td>\n"
//+"      <td>base power</td>\n"
//+"   </tr>\n"
//+"   <tr class='row0'>\n"
//+"      <td>4</td>\n"
//+"      <td>Effective power</td>\n"
//+"   </tr>\n"
//+"</table><br/><table border=1><tr><td colspan=2><b>spell succeeded</b></td></tr></table><br/>Darryl, the Orc Lord is now blessed, and all rolls will be at a +4";

      message = message.replaceAll("[\n\t]", "");
      message = message.replaceAll("<br/>", "\n");
      message = message.replaceAll("<span[^>]*>", "");
      message = message.replaceAll("</span>", "");
      message = message.replaceAll(" *<table[^>]*>(<tr>)*</table>", ""); // empty table case
      message = message.replaceAll(" *<table[^>]*>", "");
      message = message.replaceAll(" *<tr[^>]*>", "\n");
      message = message.replaceAll(" *<td[^>]*>", "\t");
      message = message.replaceAll("<b>", "");
      message = message.replaceAll("</b>", "");
      message = message.replaceAll("</td>", "");
      message = message.replaceAll("</tr>", "");
      message = message.replaceAll("</table>", "\n");
      ArrayList<ClientProxy> proxiesSentTo = new ArrayList<>();
      for (Character participant : participants) {
         if ((participant != null) && (participant.getAIType() != null)) {
            ClientProxy proxy = _mapCombatantToProxy.get(participant);
            if (proxiesSentTo.contains(proxy)) {
               // This prevents the same message from being sent to the two participants that are both on the server.
               continue;
            }
            proxiesSentTo.add(proxy);
         }
         sendMessageTextToClient(message, participant, true/*popup*/);
      }
   }
   public void sendMessageTextToAllClients(String message, boolean popUp) {
      MessageText msgText = new MessageText("Server", message, null, popUp, true/*isPublic*/);
      sendEventToAllClients(msgText);
      _server.appendMessage(message);
   }
   public void sendMessageTextToClient(String message, Character target, boolean popUp)
   {
      MessageText msgText = new MessageText("Server", message, null, popUp, false/*isPublic*/);
      sendObjectToCombatant(target, msgText);
   }
   public void sendEventToAllClients(SerializableObject serObj)
   {
      Rules.diag(serObj.toString());
      synchronized(_proxyList) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_proxyList)) {
            for (ClientProxy proxy : _proxyList) {
               proxy.sendObject(serObj);
            }
         }
      }
      synchronized(_mapCombatantToAI) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_mapCombatantsToAI)) {
//         TreeSet<Character> aiCombatants = new TreeSet<Character>(_mapCombatantToAI.keySet());
            for (AI ai : _mapCombatantToAI.values()) {
               if (ai == null) {
                  DebugBreak.debugBreak();
               }
               else {
                  if (!ai.processObject(serObj, this, _combatMap, null/*display*/)) {
                     DebugBreak.debugBreak();
                     ai.processObject(serObj, this, _combatMap, null/*display*/);
                  }
               }
            }
         }
      }
      if (serObj instanceof MessageText) {
         final MessageText msgText = (MessageText) serObj;
         if (msgText.isPopUp()) {
            CombatServer._this.getShell().getDisplay().asyncExec(new Runnable() {
               @Override
               public void run() {
                  MessageDialog msgDlg = new MessageDialog(CombatServer._this.getShell(), SWT.ICON | SWT.MODELESS);
                  msgDlg.open(msgText.getText(), msgText.isPublic());
               }
            });
         }
      }
      // events sent to all clients never need to be sent to _localCombatants
   }
   public void disconnectClient(ClientProxy disconnectingProxy)
   {
      synchronized(_proxyList) {
         _lock_proxyList.check();
         _proxyList.remove(disconnectingProxy);
      }
      Character combatant = _mapProxyToCombatant.remove(disconnectingProxy);
      if (combatant != null) {
         _charactersWaitingToConnect.add(combatant);
         _mapCombatantToProxy.remove(combatant);
//         _combatMap.removeCharacter(combatant);
//         synchronized (_combatants) {
//            _lock_combatants.check();
//            _combatants.remove(combatant);
//         }
         sendServerStatus(null);
         sendMessageTextToAllClients(combatant.getName() + " has disconnected.", false/*popUp*/);
         return;
      }
      sendMessageTextToAllClients("An annonymous client has disconnected.", false/*popUp*/);
   }

   public void disconnectAllClients() {
      synchronized(_proxyList) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_proxyList)) {
            while (_proxyList.size() < 0) {
               ClientProxy disconnectingProxy = _proxyList.remove(0);
               disconnectClient(disconnectingProxy);
               disconnectingProxy.shutdown();
            }
         }
      }

   }
   public void connectClient(ClientProxy proxy)
   {
      proxy._arena = this;
      synchronized(_proxyList) {
         _lock_proxyList.check();
         _proxyList.add(proxy);
      }
      proxy.sendObject(_combatMap);
      sendServerStatus(proxy);

      Map<Byte, List<TeamMember>> availableCombatantNamesByTeams = null;
      if ((_battle._turnCount == 1 ) &&
          (_battle._roundCount == 1 ) &&
          (_battle._phaseCount == 1 )) {
         // battle is not yet in progress.
         availableCombatantNamesByTeams = _combatMap.getRemoteTeamMembersByTeams();
      }
      // Add the people who are those in the _charactersWaitingToConnect list.
      availableCombatantNamesByTeams = new HashMap<>();
      for (Character combatant : _charactersWaitingToConnect) {
         byte team = combatant._teamID;
         List<TeamMember> members = availableCombatantNamesByTeams.computeIfAbsent(team, k -> new ArrayList<>());
         members.add(new TeamMember(team, combatant.getName(), combatant, (byte)-1/*teamPosition*/, true/*available*/));
      }
      proxy.sendObject(new RequestArenaEntrance(availableCombatantNamesByTeams));
   }

   public void sendServerStatus(ClientProxy target) {
      ServerStatus status = new ServerStatus(_combatMap, _combatants, _charactersWaitingToConnect);
      if (target == null) {
         sendEventToAllClients(status);
      }
      else {
         target.sendObject(status);
      }
      _server.updateMap(this);
      _server.updateCombatants(status.getCombatants());
   }
   public void sendCharacterUpdate(Character newData, Collection<ArenaCoordinates> locationsToRedraw) {
      sendEventToAllClients(newData);
      if ((_battle != null) && (_battle._combatServer != null) && (_battle._combatServer._map != null)) {
         _battle._combatServer._map.updateCombatant(newData, false/*redraw*/);
      }
      if (locationsToRedraw == null) {
         int mapSelfID = _server._map.getSelfId();
         if ((mapSelfID == -1) || (mapSelfID == newData._uniqueID)) {
            _server.redrawMap();
         }
         else {
            locationsToRedraw = null;
         }
      }
      else {
         _server.redrawMap(locationsToRedraw);
      }
   }
   public boolean moveToFrom(Character mover, Character toChar, ArenaLocation retreatFromLocation, Limb attackFromLimb) {
      Orientation newOrientation = null;
      if (retreatFromLocation != null) {
         newOrientation = getRetreatMoveOrientation(mover, retreatFromLocation);
      }
      else {
         newOrientation = getAdvancementMoveOrientation(mover, toChar, attackFromLimb);
      }
      if (newOrientation == null) {
         return false;
      }
      return moveCharacter(mover, newOrientation);
   }
   public boolean moveCharacter(Character mover, Orientation newOrientation) {

      Collection<ArenaCoordinates> locs = new ArrayList<>();
      locs.addAll(mover.getCoordinates());
      locs.addAll(newOrientation.getCoordinates());

      // leave the old locations
      // TODO: optimize this; the sendCharcterUpdate calls _combatMap.updateCombatant(), which scans all the
      //       ArenaLocations for existence of the character in previous location, and removes them. This
      //       step is repetitive, since we are doing that already with the call to _combatMap.removeCharacter().
      _combatMap.removeCharacter(mover);
      mover.setOrientation(newOrientation, null/*diag*/);

      sendCharacterUpdate(mover, locs);
      _combatMap.recomputeKnownLocations(mover, true/*basedOnFacing*/, false/*setVisibility*/, null/*locsToRedraw*/);

      return true;
   }
   public boolean canRetreat(Character mover, ArenaLocation attackFromLocation) {
      return (getRetreatMoveOrientation(mover, attackFromLocation) != null);
   }
   public boolean canCharacterMove(Character mover, Orientation newOrientation) {
      Orientation curOrient = mover.getOrientation();
      List<ArenaLocation> curLocs = getLocations(curOrient.getCoordinates());
      List<ArenaLocation> newLocs = getLocations(newOrientation.getCoordinates());
      for (int i=0 ; i<curLocs.size() ; i++) {
         if (!ArenaLocation.canMoveBetween(curLocs.get(i), newLocs.get(i), true/*blockByCharacters*/)) {
//         if (!newLocs.get(i).canEnter(curLocs.get(i), true/*blockByCharacters*/)) {
            return false;
         }
      }
      return true;
   }
   public List<ArenaLocation> canCharacterMove(Character mover, Facing direction) {
      List<ArenaLocation> newLocs = new ArrayList<>();
      for (ArenaCoordinates coord : mover.getCoordinates()) {
         short newX = (short) (coord._x + direction.moveX);
         short newY = (short) (coord._y + direction.moveY);
         ArenaLocation toLoc = _combatMap.getLocation(newX, newY);
         ArenaLocation fromLoc = _combatMap.getLocation(coord);
         if (!ArenaLocation.canMoveBetween(fromLoc, toLoc, true/*blockByCharacters*/)) {
            return null;
         }
         newLocs.add(toLoc);
      }
      return newLocs;
   }
   public boolean applyMovement(Character mover, Orientation destination, RequestMovement moveReq) {
//      if (getDistance(destination, mover) > 1)
//         return false;
      Orientation curOrientation = mover.getOrientation();
      if (destination.equals(curOrientation)) {
         mover.setMoveComplete();
         return true;
      }
      if (!canCharacterMove(mover, destination)) {
         return false;
      }
      byte costToEnter = curOrientation.getCostToEnter(destination, mover, getCombatMap());
      // If we haven't moved at all this round, we can always move 1 hex, regardless of entry cost.
      if ((mover.getAvailableMovement(false) >= costToEnter) ||
          (!mover.hasMovedThisRound() && (costToEnter < 100))) {
         mover.applyMovementCost(costToEnter);
         // A moving character releases any holds he/she has:
         if (mover.getHoldTarget() != null) {
            sendMessageTextToAllClients(mover.getName() + " releases " + mover.getHisHer()
                                        + " hold of " + mover.getHoldTarget().getName() + " due to movement.", false/*popup*/);
            mover.releaseHold();
         }

         // leave the old locations
         _combatMap.removeCharacter(mover);
         mover.setOrientation(destination, _server._diag);
         // enter the new locations
         for (ArenaLocation destLocation : getLocations(destination.getCoordinates())) {
            destLocation.addThing(mover);
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
            boolean moverMustStop = false;
            for (ArenaTrigger trigger : _combatMap.getTriggers()) {
               if (trigger.isTriggerAtLocation(destLocation, mover, _combatMap)) {
                  if (trigger.trigger(mover)) {
                     moverMustStop = true;
                  }
               }
            }
            for (IAreaSpell spell : destLocation.getActiveSpells()) {
               spell.affectCharacterOnEntry(mover);
            }
            if (moverMustStop) {
               mover.setMoveComplete();
               moveReq.forceEndOfMovement();
            }
         }
         Collection<ArenaCoordinates> locs = new ArrayList<>();
         locs.addAll(curOrientation.getCoordinates());
         locs.addAll(destination.getCoordinates());
         int mapSelfID = _server._map.getSelfId();
         if ((mapSelfID == -1) || (mapSelfID == mover._uniqueID)) {
            // force a redraw of everything
            locs = null;
         }
         _combatMap.recomputeKnownLocations(mover, true/*basedOnFacing*/, false/*setVisibility*/, locs);
         sendCharacterUpdate(mover, locs);
         return true;
      }
      return false;
   }

   public void getMoveableLocations(Character character, byte movementAllowance, CombatMap map,
                                    List<Orientation> futureOrientations,
                                    Map<Orientation, Orientation> mapOfFutureOrientToSourceOrient) {
      futureOrientations.add(character.getOrientation());
      getFutureMoves(futureOrientations, character, character.getOrientation(), movementAllowance, map,
                     !character.hasMovedThisRound()/*firstMoveOfRound*/, mapOfFutureOrientToSourceOrient, false/*considerUnknownLocations*/);
   }
   public void getFutureMoves(List<Orientation> validOrientations, Character character, Orientation curOrientation,
                              byte movementAllowance, CombatMap map, boolean firstMoveOfRound,
                              Map<Orientation, Orientation> mapOfFutureOrientToSourceOrient, boolean considerUnknownLocations) {
      if (mapOfFutureOrientToSourceOrient == null) {
         mapOfFutureOrientToSourceOrient = new HashMap<>();
      }
      getAllRoutesFrom(curOrientation, map, movementAllowance, movementAllowance, firstMoveOfRound,
                       character, mapOfFutureOrientToSourceOrient, null/*target*/, null/*toLoc*/, false/*allowRanged*/,
                       false/*onlyChargeTypes*/, null/*itemsToPickupUsingSkill*/, considerUnknownLocations);
      validOrientations.addAll(mapOfFutureOrientToSourceOrient.keySet());
//      ArrayList<Orientation> possibleOrientations = curOrientation.getPossibleFutureOrientations(map);
//      for (Orientation orient : possibleOrientations) {
//         if (orient.equals(curOrientation)) {
//            if (!validOrientations.contains(orient)) {
//               validOrientations.add(orient);
//            }
//            continue;
//         }
//         byte costToEnter = curOrientation.getCostToEnter(orient, character);
//         // firstMoveOfRound will be true if this is the first move for this character this turn.
//         // In this condition, we never check the cost to enter, because any movement is allowed.
//         if ((firstMoveOfRound && (costToEnter < 100)) || (costToEnter <= movementAllowance)) {
//            if (!validOrientations.contains(orient)) {
//               validOrientations.add(orient);
//               if (mapOfFutureOrientToSourceOrient != null)
//                  mapOfFutureOrientToSourceOrient.put(orient, curOrientation);
//            }
//            // TODO: This recursion block should be inside the above 'if (validOrientations.contains(orient))' block
//            //       However, doing this causes many locations to be non-accessible, because you could reach orient 2
//            //       by a more convoluted path after traversing orient 1.
//            //       Therefore, for now, we just always traverse all paths, even those that already have been entered
//            //       into the validOrientations list.
//            byte remainingMovement = (byte)(movementAllowance - costToEnter);
//            if (remainingMovement > 0)
//               getFutureMoves(validOrientations, character, orient, remainingMovement, map, false/*firstMoveOfRound*/, mapOfFutureOrientToSourceOrient);
//         }
//      }
   }
   /**This returns the closest Orientation that the 'mover' character can reach to attack 'target'.
    * If target is null, this method returns null.
    * The bestFromMap HashMap will be filled with Orientations mapped to the Orientation that is the
    * closest Orientation need to reach before you can move to the key Orientation.
    */
   static public Orientation getAllRoutesFrom(Orientation startOrientation, CombatMap map,
                                              int maxMovement, byte movePerRound, boolean firstMoveOfRound,
                                              Character mover, Map<Orientation, Orientation> bestFromMap,
                                              Character target, ArenaCoordinates toLoc, boolean allowRanged,
                                              boolean onlyChargeTypes,
                                              List<SkillType> itemsToPickupUsingSkill, boolean considerUnknownLocations) {
      // movesInRound is an ArrayList of [HashMap that key a location, with a value of where it comes from].
      // The index in the movesInRound list is how many movement points it will take to get there.
      ArrayList<HashMap<Orientation, Orientation >> movesInRound = new ArrayList<>();
      HashMap<Orientation, Orientation > routeFromThisRound = new HashMap<>();
      // With 0 movement points spent, we start out at startOrientation
      routeFromThisRound.put(startOrientation, null);
      movesInRound.add(routeFromThisRound);
      int maxWeaponRange = 0;
      int initialDistance = -1;
      int handsAvailable = 0;
      // If we are going to try to figure out which weapons we can pick up,
      // we need to know if we have 2 hand available to pickup 2-handed weapons.
      if ((itemsToPickupUsingSkill != null) && (!itemsToPickupUsingSkill.isEmpty())) {
         Limb rightHand = mover.getLimb(LimbType.HAND_RIGHT);
         Limb leftHand = mover.getLimb(LimbType.HAND_LEFT);
         if ((rightHand != null) && (!rightHand.isCrippled())) {
            handsAvailable++;
         }
         if ((leftHand != null) && (!leftHand.isCrippled())) {
            handsAvailable++;
         }
      }

      boolean moverIsStanding = true;
      ArenaCoordinates targetHeadCoord = toLoc;
      if ((target != null) || (toLoc != null)) {
         maxWeaponRange = mover.getMaxWeaponRange(allowRanged, onlyChargeTypes);
         if (target != null) {
            targetHeadCoord = target.getHeadCoordinates();
            // larger targets allow us to possibly reach them when we are not yet at their head
            int targetSize = target.getOrientation().getCoordinates().size();
            maxWeaponRange += (targetSize -1);
         }
         // If we are larger than one hex, we also may not need to be so close:
         int selfSize = mover.getOrientation().getCoordinates().size();
         maxWeaponRange += (selfSize -1);
         initialDistance = ArenaCoordinates.getDistance(startOrientation.getHeadCoordinates(), targetHeadCoord);
         moverIsStanding = mover.isStanding();
      }

      //Orientation testOrientation = mover.getOrientation();

      boolean unexploredMovesExists = false;
      int devianceAllowance=0;
      do {
         if ((target != null) || (toLoc != null)) {
            Rules.diag("checking paths for devianceAllowance of " + devianceAllowance);
         }
         int currentMove = 0;
         boolean isFirstTurnOfRound = true;
         HashSet<String> visitedHeadLocation = new HashSet<>();

         unexploredMovesExists = false;
         // so long as there are movement available, that costs us less than 'movementRate' to reach,
         // keep looking for possible moves
         while (currentMove < movesInRound.size()) {
            routeFromThisRound = movesInRound.get(currentMove);
            if (routeFromThisRound != null) {
               // Sort this list, so it is not ordered by ID, which is non-deterministic, and therefore not
               // reproducible when we go to playback a scenario
               TreeSet<Orientation> sortedLocations = new TreeSet<> (routeFromThisRound.keySet());
               ArrayList<Orientation> moveDestinationsPossibleThisRound = new ArrayList<>(sortedLocations);
               // Now, iterate over all the possible Orientations that start took us 'currentMove' movement points to reach
               while (moveDestinationsPossibleThisRound.size() > 0) {
                  // randomly choose a location to move from. This causes a more natural movement when used by AI.
                  // (rather than always going to the left then up, for instance)
                  int randomLocationIndex = (int) Math.floor(CombatServer.random() * moveDestinationsPossibleThisRound.size());
                  Orientation destOrientation = moveDestinationsPossibleThisRound.remove(randomLocationIndex);
                  // If we already found a route to this location, use that, because it must be at least as fast.
                  if (bestFromMap.containsKey(destOrientation)) {
                     continue;
                  }
                  // If we already found a route to this location, even if the facing is different,
                  // don't enter, unless this is the first turn of this round.
                  StringBuilder destOrientKey = new StringBuilder();
                  destOrientKey.append(destOrientation.getHeadCoordinates()._x).append(',').append(destOrientation.getHeadCoordinates()._y);
                  if (isFirstTurnOfRound) {
                     destOrientKey.append('-').append(destOrientation.getFacing());
                  }
                  String destKey = destOrientKey.toString();
                  if (visitedHeadLocation.contains(destKey)) {
                     routeFromThisRound.remove(destOrientation);
                     continue;
                  }

                  // targetHeadCoord will be non-null if we are looking for either a target or a 'to Location' (toLoc)
                  if (targetHeadCoord != null) {
                     // make certain that this new location is within the devience allowance for this iterator.
                     // If its not, don't remove it from the routeFromThisRound,
                     // and don't put it into the bestFromMap (or the visitedHeadLocations list)
                     int destDistance = ArenaCoordinates.getDistance(destOrientation.getHeadCoordinates(), targetHeadCoord);
                     if (moverIsStanding) {
                        if ((destDistance + currentMove) > (initialDistance + devianceAllowance)) {
                           unexploredMovesExists = true;
                           continue;
                        }
                     }
                     else {
                        // If we are not standing, we should expect that it will take twice as many movement points to reach our target
                        if ((destDistance + (currentMove / 2)) > (initialDistance + devianceAllowance)) {
                           unexploredMovesExists = true;
                           continue;
                        }
                     }
                  }
                  visitedHeadLocation.add(destKey);
                  // Put the route to this new destOrientation into the bestFromMap.
                  Orientation fromOrient = routeFromThisRound.remove(destOrientation);
                  if (fromOrient == destOrientation) {
                     DebugBreak.debugBreak();
                  }
                  bestFromMap.put(destOrientation, fromOrient );

                  // If we are looking to attack a particular target or location, see if we can attack from this orientation:
                  if (targetHeadCoord != null) {
                     // Once we found the destination, stop looking
                     // Are we close enough that we could attack the target?
                     // First consider only the base ranges that could exclude an attack
                     // If the ranges are OK, then look more closely by calling 'canAttack',
                     // which is a little expensive to call for each orientation.
                     ArenaCoordinates destHeadCoords = destOrientation.getHeadCoordinates();
                     if (ArenaCoordinates.getDistance(destHeadCoords, targetHeadCoord) <= maxWeaponRange) {
                        boolean destinationValid = false;
                        if (target == null) {
                           ArenaLocation destHeadLoc = map.getLocation(destHeadCoords);
                           if (map.hasLineOfSight(destHeadLoc, targetHeadCoord, true/*blockByAnyStandingCharacter*/)) {
                              if (map.isFacing(mover, targetHeadCoord)) {
                                 destinationValid = true;
                              }
                           }
                        }
                        else {
                           if (destOrientation.canAttack(mover, target, map, allowRanged, onlyChargeTypes)) {
                              if (map.couldSee(destOrientation, target, true/*considerFacing*/, true/*blockByAnyStandingCharacter*/)) {
                                 destinationValid = true;
                              }
                           }
                        }
                        if (destinationValid) {
                           CombatServer._this._map.setRouteMap(null, null/*path*/, false/*allowRedraw*/);
                           return destOrientation;
                        }
// If we are trying to reach a destination, we must actually walk there, we don't care about weather or not our weapon can reach it
//                        // If we are trying to get to a location, see if our weapon could reach it from this destination:
//                        if (toLoc != null) {
//                           CombatServer._this._map.setRouteMap(null, null/*path*/, false/*allowRedraw*/);
//                           return destOrientation;
//                        }
                     }
                  }
                  if (toLoc != null) {
                     if (toLoc.sameCoordinates(destOrientation.getHeadCoordinates())) {
                        CombatServer._this._map.setRouteMap(null, null/*path*/, false/*allowRedraw*/);
                        return destOrientation;
                     }
                  }
                  if (itemsToPickupUsingSkill != null) {
                     List<ArenaLocation> destLocations = map.getLocations(destOrientation.getCoordinates());
                     for (ArenaLocation destLoc : destLocations) {
                        synchronized (destLoc) {
                           try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(destLoc._lock_this)) {
                              for (Object obj : destLoc.getThings()) {
                                 if (obj instanceof Weapon) {
                                    Weapon weap = (Weapon) obj;
                                    //if (mover.canPickup(weap)) {
                                    for (WeaponStyleAttack style : weap._attackStyles) {
                                       if (style._handsRequired <= handsAvailable) {
                                          if (itemsToPickupUsingSkill.contains(style.getSkillType())) {
                                             return destOrientation;
                                          }
                                       }
                                    }
                                    //}
                                 }
                                 else if (obj instanceof Shield) {
                                    //if (mover.canPickup(obj)) {
                                    if (itemsToPickupUsingSkill.contains(SkillType.Shield)) {
                                       return destOrientation;
                                    }
                                    //}
                                 }
                              }
                           }
                        }
                     }
                  }
                  // If we have exceeded our max movement, don't consider anymore future destinations,
                  // but don't return until we have finished moving everything from moveDestinationsPossibleThisRound
                  // into bestFromMap.
                  if (currentMove >= maxMovement) {
                     continue;
                  }

                  // Now find out where we can go from this new location.
                  ArrayList<Orientation> testLocs;
                  // If we haven't moved more than 2 moves so far, consider moves that include backing up
                  // If we have moved than 2 moves so far, we should be able trim down our search set by
                  // only considering advancing moves.
                  if (currentMove > 2) {
                     testLocs = destOrientation.getPossibleAdvanceOrientations(map, false/*blockByCharacters*/);
                  }
                  else {
                     testLocs = destOrientation.getPossibleFutureOrientations(map);
                  }
                  for (Orientation futureOrient : testLocs) {
                     if (futureOrient != null) {
                        Orientation bestFromOrientation = bestFromMap.get(futureOrient);
                        // If we've already found a route here, that route must be faster
                        if (bestFromOrientation == null) {
                           // If we don't know about the future location, don't consider moving there.
                           if (!considerUnknownLocations) {
                              ArenaCoordinates headCoord = futureOrient.getHeadCoordinates();
                              ArenaLocation headLoc = map.getLocation(headCoord);
                              if (!headLoc.isKnownBy(mover._uniqueID)) {
                                 continue;
                              }
                           }

                           byte costToEnter = destOrientation.getCostToEnter(futureOrient, mover, map);
                           if (costToEnter >= 100) {
                              // ignore Orientations we can't enter (perhaps another character or a wall is in the way)
                              continue;
                           }
                           int newPathLength = currentMove + costToEnter;
                           // Can we enter this future Orientation within this round?
                           if ((newPathLength <= maxMovement) || firstMoveOfRound) {
                              if (costToEnter > 1) {
                                 int movesLeftThisRound = movePerRound - (currentMove % movePerRound);
                                 if (movesLeftThisRound < costToEnter) {
                                    if (firstMoveOfRound) {
                                       // If this is the first move of the round, then we ignore the cost of
                                       // moving to this location, and instead use spend our entire movePerRound
                                       newPathLength = currentMove + movePerRound;
                                    }
                                    else {
                                       // Otherwise, add the cost of waiting here until the next round begins:
                                       newPathLength += movesLeftThisRound;
                                    }
                                 }
                              }
                              // make sure our movesInRound Array is large enough to hold this future round:
                              while (movesInRound.size() <= newPathLength) {
                                 movesInRound.add(new HashMap<Orientation, Orientation>());
                              }
                              // Put it in the slot for the movement points it would cost us to reach.
                              (movesInRound.get(newPathLength)).put(futureOrient, destOrientation);
                           }
                        }
                     }
                  }
               }
            }
            currentMove++;
            firstMoveOfRound = (currentMove % movePerRound) == 0;
            isFirstTurnOfRound = (currentMove <= movePerRound);
         }
         devianceAllowance++;
         //if ((target != null) || (toLoc != null)) {
            CombatServer._this._map.setRouteMap(bestFromMap, null/*path*/, false/*allowRedraw*/);
         //}
         if ((devianceAllowance%5) == 0) {
            if (devianceAllowance == 5) {
               // If we are having trouble finding a route, it might be that our isKnownBy data is out of date:
               map.recomputeKnownLocations(mover, true/*basedOnFacing*/, false/*setVisibility*/, null/*locsToRedraw*/);
            }
            else {
               if (moverIsStanding && (devianceAllowance == 10)) {
                  if (!CombatServer._this.getShell().isDisposed()) {
                     Display display = CombatServer._this.getShell().getDisplay();
                     final Character movingCharacter = mover;
                     final int selfId = CombatServer._this._map.getSelfId();
                     if (movingCharacter._uniqueID != selfId) {
                        display.asyncExec(new Runnable() {
                           @Override
                           public void run() {
                              CombatServer._this._map.recomputeVisibility(movingCharacter, null/*diag*/);
                           }
                        });
                        // wait until the UIThread has set its _self to this mover
                        int maxWait = 5000; // 5 seconds
                        try {
                           while ((movingCharacter._uniqueID != CombatServer._this._map.getSelfId()) && (maxWait >= 0)) {
                              Thread.sleep(100);
                              maxWait -= 100;
                           }
                        } catch (InterruptedException e) {
                           e.printStackTrace();
                        }

// TODO: MOVEMENT ISSUE: Rules.debugBreak();
                        // after the debugBreak, restore the previous view settings
                        display.asyncExec(new Runnable() {
                           @Override
                           public void run() {
                              Character previousSelf = null;
                              if (selfId != -1) {
                                 previousSelf = CombatServer._this._map.getCombatMap().getCombatantByUniqueID(selfId);
                              }

                              CombatServer._this._map.recomputeVisibility(previousSelf, null/*diag*/);
                           }
                        });
                     }
                  }
               }
            }
         }
      } while (unexploredMovesExists);
      CombatServer._this._map.setRouteMap(null, null/*path*/, false/*allowRedraw*/);
      return null;
   }

   public static byte getFacingChangeNeededToFace(Facing charFacing, ArenaCoordinates fromCoord, ArenaCoordinates toCoord) {
      Facing moveFacing = ArenaCoordinates.getFacingToLocation(fromCoord, toCoord);
      if (moveFacing != null) {
         for (byte turn=-2 ; turn<=3 ; turn++) {
            if (((charFacing.value+6+turn)%6) == moveFacing.value) {
               return turn;
            }
         }
      }
      DebugBreak.debugBreak();
      moveFacing = ArenaCoordinates.getFacingToLocation(fromCoord, toCoord);
      moveFacing = moveFacing.turnRight();
      throw new IllegalArgumentException();
   }
   /**
    * This method return the distance from the head of char1, to the closest point on char2
    * @param char1
    * @param char2
    * @return
    */
   public static short getMinDistance(Character char1, Character char2) {
      // If either character is holding the other, consider the distance to be zero.
      if (char1.getHoldLevel(char2) != null) {
         return 0;
      }
      if (char2.getHoldLevel(char1) != null) {
         return 0;
      }
      ArenaCoordinates headCoord = char1.getHeadCoordinates();
      return getShortestDistance(headCoord, char2.getOrientation());
   }

   /**
    * This method return the distance from the head of char1, to the farthest point on char2
    * @param char1
    * @param char2
    * @return
    */
   public static short getMaxDistance(Character char1, Character char2) {
      // If either character is holding the other, consider the distance to be zero.
      if (char1.getHoldLevel(char2) != null) {
         return 0;
      }
      if (char2.getHoldLevel(char1) != null) {
         return 0;
      }
      ArenaCoordinates headCoord = char1.getHeadCoordinates();
      return getFarthestDistance(headCoord, char2.getOrientation());
   }
   /**
    * This returns the distance from the head of the character to the location, in hexes
    * @param coord
    * @param character
    * @return
    */
   public static short getDistance(ArenaCoordinates coord, Character character) {
      return ArenaCoordinates.getDistance(coord, character.getHeadCoordinates());
   }
   /**
    * This returns the distance from the closest location within the orientation to the location, in hexes
    * @param loc
    * @param orientation
    * @return
    */
   public static short getShortestDistance(ArenaCoordinates loc, Orientation orientation) {
      short minDistance = -1;
      for (ArenaCoordinates orientCoord : orientation.getCoordinates()) {
         short thisDist = ArenaCoordinates.getDistance(loc, orientCoord);
         if ((minDistance == -1) || (thisDist < minDistance)) {
            minDistance = thisDist;
         }
      }
      return minDistance;
   }
   /**
    * This returns the distance from the farthest location within the orientation to the location, in hexes
    * @param loc
    * @param orientation
    * @return
    */
   public static short getFarthestDistance(ArenaCoordinates loc, Orientation orientation) {
      short maxDistance = -1;
      for (ArenaCoordinates orientCoord : orientation.getCoordinates()) {
         short thisDist = ArenaCoordinates.getDistance(loc, orientCoord);
         if ((maxDistance == -1) || (thisDist > maxDistance)) {
            maxDistance = thisDist;
         }
      }
      return maxDistance;
   }
   public boolean serializeFromString(String inputLine) {
      return _combatMap.serializeFromString(inputLine);
   }
   public String serializeToString() {
      return _combatMap.serializeToString();
   }
   public void setName(String newName) {
      _combatMap.setName(newName);
   }
   public String getName() {
      return _combatMap.getName();
   }
   public void dropThing(Object thing, short xLoc, short yLoc) {
      _combatMap.dropThing(thing, xLoc, yLoc);
      // Since the ArenaLocation is a monitoredObject, it will report
      // the change to any watcher of the location.
      //sendServerStatus(null, false/*fullMap*/, false/*recomputeVisibility*/);
   }
   public CombatMap getCombatMap() {
      return _combatMap;
   }
   public void setCombatMap(CombatMap combatMap, boolean clearCombatants) {
      if (_combatMap != null) {
         _combatMap.clearItems();
      }
      if (clearCombatants) {
         synchronized (_combatants) {
            _lock_combatants.check();
            _combatants.clear();
         }
         synchronized(_mapCombatantToAI) {
            _lock_mapCombatantsToAI.check();
            _mapCombatantToAI.clear();
         }
         _localCombatants.clear();
      }
      _combatMap = combatMap;
      if (clearCombatants) {
         _combatMap.clearCharacterViewedHistory();
      }
   }
   boolean _characterGenerated = false;
   public void addStockCombatants() {
      _combatMap.clearCharacterViewedHistory();
      _characterGenerated = false;
      for (byte team=0 ; team<_combatMap.getTeamCount() ; team++) {
         String[] stockAIName = _combatMap.getStockAIName(team);
         String[] stockCharacters = _combatMap.getStockCharacters(team);
         for (byte cur=0 ; cur<stockCharacters.length; cur++) {
            if ((stockAIName[cur] != null) && (stockAIName[cur].length() > 0) && (!stockAIName[cur].equalsIgnoreCase("Off"))) {
               if ((stockCharacters[cur] != null) && (stockCharacters[cur].length() > 0)) {
                  Character stockCharacter = null;
                  if (stockCharacters[cur].startsWith("? ")) {
                     if (!_characterGenerated) {
                        _characterGenerated = true;
                        if (!_server._usePseudoRandomNumbers.getSelection()) {
                           CombatServer.generateNewPseudoRandomNumberSeed();
                        }
                     }
                     stockCharacter = CharacterGenerator.generateRandomCharacter(stockCharacters[cur], this, true/*printCharacter*/);
                  }
                  else {
                     stockCharacter = _server._charFile.getCharacter(stockCharacters[cur]);
                  }
                  if (stockCharacter != null) {
                     addCombatant(stockCharacter, team, cur/*combatantIndexOnTeam*/, null, true/*checkForAutoStart*/);
                  }
               }
            }
         }
      }
   }
   public void removeAllCombatants() {
      _registeredNames.clear();
       _mapCombatantToProxy.clear();
       synchronized(_mapCombatantToAI) {
          try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_mapCombatantsToAI)) {
             for (AI ai : _mapCombatantToAI.values()) {
                ai.removeCachedDataOnArenaExit();
             }
             _mapCombatantToAI.clear();
          }
       }
       _localCombatants.clear();
       synchronized (_combatants) {
          _lock_combatants.check();
          _combatants.clear();
       }
       if (_combatMap != null) {
          _combatMap.removeAllCombatants();
       }
   }
   public void handleTargetPriorities(TargetPriorities priorities, ClientProxy proxy)
   {
      Character combatant = _mapProxyToCombatant.get(proxy);
      // The combatant may have not yet entered the arena
      if (combatant != null) {
         combatant.setTargetPriorities(priorities.getOrderedEnemyIdsList());
      }
   }

   public ArrayList<Character> getEnemies(Character attacker, boolean includeFallenFoes) {
      ArrayList<Character> enemies = new ArrayList<>();
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Character enemy : _combatants) {
               // Don't bother attacking an enemy that is unconscious or dead.
               if (includeFallenFoes || enemy.stillFighting()) {
                  if ((enemy._teamID == TEAM_INDEPENDENT) ||
                           (enemy._teamID != attacker._teamID)) {
                     if (enemy._uniqueID != attacker._uniqueID) {
                        enemies.add(enemy);
                     }
                  }
               }
            }
         }
      }
      return enemies;
   }
   public Character getBestTarget(Character attacker, boolean attackerIsAdvancing)
   {
      ArrayList<Integer> orderedTargets = attacker.getOrderedTargetPriorites();
      if (orderedTargets == null) {
         return null;
      }
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Integer uniqueId : orderedTargets) {
               if (uniqueId != attacker._uniqueID) {
                  for (Character enemy : _combatants) {
                     if (enemy._uniqueID == uniqueId) {
                        // Don't bother attacking an enemy that is unconscious or dead.
                        if (enemy.stillFighting()) {
                           if ((enemy._teamID == TEAM_INDEPENDENT) ||
                                    (enemy._teamID != attacker._teamID)) {
                              // Don't allow an attack on someone you can't see,
                              // even if they advance first!
                              if (_combatMap.canSee(attacker, enemy, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
                                 if (canAttack(attacker, enemy)) {
                                    return enemy;
                                 }
                                 if (attackerIsAdvancing) {
                                    // what happens when we advance?
                                    if (getAdvancementMoveOrientation(attacker, enemy, null/*attackFromLimb*/) != null) {
                                       return enemy;
                                    }
                                    if (canAttack(attacker, enemy, true/*withAdvance*/, true/*withCharge*/)) {
                                       return enemy;
                                    }
                                 }
                              }
                           }
                        }
                        // If we found the enemy matching the uniqueId were looking for, stop looking.
                        break;
                     }
                  }
               }
            }
         }
      }
      return null;
   }
   public Character getNearestTarget(Character attacker, boolean anyTeam)
   {
      Character nearestTarget = null;
      int nearestTargetdistance = 1000;
      synchronized (_combatants) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_combatants)) {
            for (Character enemy : _combatants) {
               // Don't bother attacking an enemy that is unconscious or dead.
               if (enemy.stillFighting()) {
                  if ((enemy._teamID == TEAM_INDEPENDENT) ||
                           (enemy._teamID != attacker._teamID) || anyTeam) {
                     // Don't attack yourself
                     if (attacker._uniqueID != enemy._uniqueID) {
                        // Don't allow an attack on someone you can't see
                        if (_combatMap.canSee(attacker, enemy, false/*considerFacing*/, false/*blockedByAnyStandingCharacter*/)) {
                           int dist = getMinDistance(attacker, enemy);
                           if (dist < nearestTargetdistance) {
                              nearestTargetdistance = dist;
                              nearestTarget = enemy;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
      return nearestTarget;
   }

   public boolean hasLineOfSight(Character fromChar, Character toChar) {
      if (fromChar._uniqueID == toChar._uniqueID) {
         return true;
      }
      return _combatMap.countCharactersBetween(fromChar, toChar, true/*onlyCountStandingCharacters*/) == 0;
   }

   private Orientation getAdvancementMoveOrientation(Character attacker, Character defender, Limb attackFromLimb) {
      // no need to clone this, since movedAttacker is never modified
      //Character movedAttacker = (Character) attacker.clone();
      Character movedAttacker = attacker;
      Orientation curOrientation = attacker.getOrientation();
      ArrayList<Orientation> possibleMoves = curOrientation.getPossibleAdvanceOrientations(getCombatMap(), true/*blockByCharacters*/);
      for (Orientation orient : possibleMoves) {
         if (canCharacterMove(attacker, orient)) {
            if (attackFromLimb != null) {
               if (orient.canLimbAttack(movedAttacker, defender, attackFromLimb, getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                  return orient;
               }
            }
            else {
               if (orient.canAttack(movedAttacker, defender, getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                  return orient;
               }
            }
         }
      }
      return null;
   }
   private Orientation getRetreatMoveOrientation(Character character, ArenaLocation attackFromLocation) {
      Orientation curOrientation = character.getOrientation();
      List<Orientation> possibleMoves = new ArrayList<>();
      List<Orientation> distancingMoves = new ArrayList<>();
      byte movementAllowed = character.getAvailableMovement(false/*movingEvasively*/);
      if (movementAllowed > 0) {
         // retreats only need one movement, because if we haven't moved at all, then we can make any single hex
         // movement with just a single movement point, and using only one point restricts the result set nicely.
         movementAllowed = 1;
         getFutureMoves(possibleMoves, character, curOrientation, movementAllowed, _combatMap,
                        true/*firstMoveOfRound*/, null/*mapOfFutureOrientToSourceOrient*/, true/*considerUnknownLocations*/);
      }

      short curDistanceFromLocation = getShortestDistance(attackFromLocation, curOrientation);
      for (Orientation orient : possibleMoves) {
         short distanceFromLocation = getShortestDistance(attackFromLocation, orient);
         if (curDistanceFromLocation < distanceFromLocation) {
            distancingMoves.add(orient);
         }
      }
      if (distancingMoves.size() == 3) {
         // Move the first element to the last element,
         // so that we check for the move in the middle first.
         // The order of the orientations always starts with the NOON move, and proceeds clockwise.
         // If the NOON move is the middle move away from attackFromLocation, don't re-order this.
         ArenaCoordinates headCoord = character.getHeadCoordinates();
         ArenaLocation forwardLoc = ArenaLocation.getForwardMovement(headCoord, Facing._6_OCLOCK, getCombatMap());
         if ((forwardLoc != null) && !forwardLoc.sameCoordinates(attackFromLocation)) {
            distancingMoves.add(distancingMoves.remove(0));
         }
      }
      for (Orientation orientation : distancingMoves) {
         if (canCharacterMove(character, orientation)) {
            return orientation;
         }
      }
      return null;
   }

   public boolean canAttack(Character attacker, Character defender) {
      return canAttack(attacker, defender, false/*withAdvance*/, false/*withCharge*/);
   }
   public boolean canAttack(Character attacker, Character defender, boolean withAdvance, boolean withCharge) {
      if (!_combatMap.isFacing(attacker, defender) && !attacker.hasPeripheralVision()) {
         if (withAdvance) {
            // check to see if turning 1 hex will allow us to attack the target
            for (Orientation advancedOrientation : attacker.getOrientation().getPossibleAdvanceOrientations(_combatMap, true/*blockByCharacters*/)) {
               if (advancedOrientation.getHeadCoordinates().sameCoordinates(attacker.getHeadCoordinates())) {
                  if (advancedOrientation.canAttack(attacker, defender, getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
                     return true;
                  }
               }
            }
         }
         return false;
      }

      Orientation attackingOrientation = attacker.getOrientation();
      if (attackingOrientation.canAttack(attacker, defender, getCombatMap(), true/*allowRanged*/, false/*onlyChargeTypes*/)) {
         return true;
      }
      if (withAdvance) {
         for (Orientation advancedOrientation : attacker.getOrientation().getPossibleAdvanceOrientations(_combatMap, true/*blockByCharacters*/)) {
            if (advancedOrientation.canAttack(attacker, defender, getCombatMap(), false/*allowRanged*/, false/*onlyChargeTypes*/)) {
               return true;
            }
         }
      }
      if (withCharge) {
         HashMap<Orientation, List<Orientation>> mapOrientationToNextOrientationsLeadingToChargeAttack = new HashMap<>();
         attackingOrientation.getPossibleChargePathsToTarget(getCombatMap(), attacker, defender,
                                                             attacker.getAvailableMovement(false/*movingEvasively*/),
                                                             mapOrientationToNextOrientationsLeadingToChargeAttack);
         return mapOrientationToNextOrientationsLeadingToChargeAttack.size() >= 1;

      }
      return false;
   }
   public void clearStartingPointLabels() {
      _combatMap.clearStartingPointLabels();
   }

   public boolean _paused = false;
   public void onPause() {
      if (_battle != null) {
         _battle.onPause();
      }
      _paused = true;
   }
   public void onPlay()  {
      if (_battle != null) {
         _battle.onPlay();
      }
      _paused = false;
   }
   public void onAutoRun(AutoRunBlock autoRunBlock)  {
      _combatMap.setAllCombatantsAsAI();
      _autoRunMap = _combatMap.clone();
      _autoRunBlock = autoRunBlock;
      _paused = false;
      removeAllCombatants();
      terminateBattle();
      // re-initialize the psudeo-random number generator:
      CombatServer.resetPseudoRandomNumberGenerator();

      addStockCombatants();
   }
   public void onTurnAdvance()  { if (_battle != null) {
      _battle.onTurnAdvance();
   } }
   public void onRoundAdvance() { if (_battle != null) {
      _battle.onRoundAdvance();
   }}
   public void onPhaseAdvance() { if (_battle != null) {
      _battle.onPhaseAdvance();
   }}
   public void addLocationActions(RequestAction actionReq, Character actor) {
      _combatMap.addLocationActions(actionReq, actor);
   }
   public String getActionDescription(Character actor, RequestAction actionReq) {
      return _combatMap.getActionDescription(actor, actionReq);
   }
   public boolean isPickupItem(Character actor, RequestAction actionReq) {
      return _combatMap.isPickupItem(actor, actionReq);
   }
   public Object pickupItem(Character actor, RequestAction actionReq, int itemIndex, Diagnostics diag) {
      return _combatMap.pickupItem(actor, actionReq, itemIndex, diag);
   }
   public boolean isLocationAction(Character actor, RequestAction actionReq) {
      return _combatMap.isLocationAction(actor, actionReq);
   }
   public boolean applyAction(Character actor, RequestAction actionReq) throws BattleTerminatedException {
      return _combatMap.applyAction(actor, actionReq);
   }

   // IMapListener methods:
   @Override
   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
   }
   @Override
   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      _mouseOverCharInfoPopup.onMouseMove(loc, event, angleFromCenter, normalizedDistFromCenter);
   }
   @Override
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      Rules.diag("Arena:onMouseDown (" + event.x + "," + event.y + "), event.button=" + event.button);
   }
   @Override
   public void onRightMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      _characterMenuPopup.onRightMouseDown(loc, event, angleFromCenter, normalizedDistFromCenter);
      Rules.diag("Arena:onRightMouseDown (" + event.x + "," + event.y + "), event.button=" + event.button);
   }
   @Override
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      Rules.diag("Arena:onMouseUp (" + event.x + "," + event.y + "), event.button=" + event.button);
      // If this is a right-click (or middle-click), ignore it.
      if (event.button != 1) {
         return;
      }
      if (loc == null) {
         return;
      }
      // are we waiting on a location request?
      synchronized (_locationRequests) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_locationRequests)) {
            if (!_locationRequests.isEmpty()) {
               for (SyncRequest syncReq : _locationRequests) {
                  if (syncReq instanceof RequestMovement) {
                     RequestMovement moveReq = (RequestMovement) syncReq;
                     // fill in the location into the request, and send it back.
                     // If it's a valid location, then this will return true.
                     if (moveReq.setOrientation(loc, angleFromCenter, normalizedDistFromCenter)) {
                        completeMove(syncReq);
                        return;
                     }
                  }
                  if (syncReq instanceof RequestLocation) {
                     RequestLocation locReq = (RequestLocation) syncReq;
                     // fill in the location into the request, and send it back.
                     // If it's a valid location, then this will return true.
                     if (locReq.setAnswer(loc._x, loc._y)) {
                        stopWaitingForResponse(syncReq);
                        _server._map.endHexSelection();
                        _locationRequests.remove(syncReq);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   public void completeMove(SyncRequest syncReq) {
      stopWaitingForResponse(syncReq);
      _locationRequests.remove(syncReq);
      RequestMovement nextMoveReq = null;
      for (SyncRequest syncRequest : _locationRequests) {
         if (syncRequest instanceof RequestMovement) {
            nextMoveReq = (RequestMovement) syncRequest;
            break;
         }
      }
      if (nextMoveReq == null) {
         _server._map.endHexSelection();
         // Stop hiding the hexes that the player can't see
         _server._map.recomputeVisibility(null/*self*/, null/*diag*/);
      }
      else {
         _server._map.requestMovement(nextMoveReq);
         _server._map.recomputeVisibility(getCharacter(nextMoveReq.getActorID()), null/*diag*/);
      }
   }

   public void restart()   {
      // We are in the main Thread when we get here, since this can only occur from a UI response
      // Stop the battle thread.
      terminateBattle();
      // then we need to reset all combatants
      removeAllCombatants();
      // re-initialize the psudeo-random number generator:
      CombatServer.resetPseudoRandomNumberGenerator();
      // set this index back to zero, so we know to start using the first response
      _playbackIndex = 0;
      // Adding the stock combatants will auto-launch the battle if complete.
      addStockCombatants();
   }
   public boolean hasExitTriggers() {
      return _combatMap.hasExitTriggers();
   }
   public ArenaLocation getLocation(ArenaCoordinates coord) {
      return _combatMap.getLocation(coord);
   }
   public List<ArenaLocation> getLocations(List<ArenaCoordinates> headCoord) {
      return _combatMap.getLocations(headCoord);
   }
   public boolean serializeToFile(File battleFile) {
      if (battleFile != null) {
         try {
            if (!battleFile.exists()) {
               if (!battleFile.createNewFile()) {
                  return false;
               }
            }
            if (battleFile.canWrite()) {
               Document arenaDoc = getXmlObject("\n  ");

               try (FileOutputStream fos = new FileOutputStream(battleFile)) {
                  DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
                  DOMImplementationLS impl = (DOMImplementationLS) reg.getDOMImplementation("LS");
                  LSSerializer serializer = impl.createLSSerializer();
                  LSOutput lso = impl.createLSOutput();
                  lso.setByteStream(fos);
                  serializer.write(arenaDoc, lso);
               } catch (IOException |ClassNotFoundException |InstantiationException |IllegalAccessException |ClassCastException e) {
                  e.printStackTrace();
               }
               return true;
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      return false;
   }
   public boolean serializeFromFile(File battleFile) {
      if ((battleFile != null) && battleFile.canRead()) {
         if (battleFile.getAbsolutePath().endsWith(".btl")) {
            Document arenaDoc = Character.parseXmlFile(battleFile, false/*validating*/);
            if (arenaDoc != null) {
               return serializeFromXmlObject(arenaDoc.getDocumentElement());
            }
         }
         return false;
      }
      return false;
   }
   private Document getXmlObject(String newLine) {
      // Create a builder factory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(true/*validating*/);

      // Create the builder and parse the file
      Document arenaDoc = null;
      try {
         DocumentBuilder builder = factory.newDocumentBuilder();
         arenaDoc = builder.newDocument();
         Element mainElement = arenaDoc.createElement("ArenaSave");
         mainElement.setAttribute("Name", getName());
         mainElement.setAttribute("PseudoRandomNumberSeed", String.valueOf(CombatServer.getPseudoRandomNumberSeed()));
         mainElement.setAttribute("PseudoRandomNumberUseCount", String.valueOf(CombatServer.getPseudoRandomNumberUseCount()));
         arenaDoc.appendChild(mainElement);

         ArrayList<Integer> uniqueIDs = new ArrayList<>();
         mainElement.appendChild(arenaDoc.createTextNode(newLine));
         mainElement.appendChild(_battle.getXmlObject(arenaDoc));
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         Node curMap = arenaDoc.createElement("CurrentMap");
         mainElement.appendChild(curMap);
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         Node mapList = arenaDoc.createElement("StoredMaps");
         mainElement.appendChild(mapList);
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         Node localCharactersList = arenaDoc.createElement("localCharacters");
         mainElement.appendChild(localCharactersList);
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         Node remoteCharactersList = arenaDoc.createElement("remoteCharacters");
         mainElement.appendChild(remoteCharactersList);
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         Node aiCharactersList = arenaDoc.createElement("aiCharacters");
         mainElement.appendChild(aiCharactersList);
         mainElement.appendChild(arenaDoc.createTextNode(newLine));

         for (Character localChar : _localCombatants) {
            localCharactersList.appendChild(arenaDoc.createTextNode(newLine + "  "));
            localCharactersList.appendChild(localChar.getXmlObject(arenaDoc, true/*includeConditionData*/, newLine + "  "));
            if (localChar.stillFighting()) {
               uniqueIDs.add(localChar._uniqueID);
            }
         }
         localCharactersList.appendChild(arenaDoc.createTextNode(newLine));
         for (Character remoteChar : _mapCombatantToProxy.keySet()) {
            remoteCharactersList.appendChild(arenaDoc.createTextNode(newLine + "  "));
            remoteCharactersList.appendChild(remoteChar.getXmlObject(arenaDoc, true/*includeConditionData*/, newLine + "  "));
            if (remoteChar.stillFighting()) {
               uniqueIDs.add(remoteChar._uniqueID);
            }
         }
         remoteCharactersList.appendChild(arenaDoc.createTextNode(newLine));
         synchronized(_mapCombatantToAI) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_mapCombatantsToAI)) {
               for (Character aiChar : _mapCombatantToAI.keySet()) {
                  aiCharactersList.appendChild(arenaDoc.createTextNode(newLine + "  "));
                  Element aiCharXml = aiChar.getXmlObject(arenaDoc, true/*includeConditionData*/, newLine + "  ");
                  aiCharXml.setAttribute("aiType", _mapCombatantToAI.get(aiChar).getAiType().name);
                  aiCharactersList.appendChild(aiCharXml);
                  if (aiChar.stillFighting()) {
                     uniqueIDs.add(aiChar._uniqueID);
                  }
               }
            }
         }
         aiCharactersList.appendChild(arenaDoc.createTextNode(newLine));
         // now that we have the complete list of characters we care about, we can include the maps
         curMap.appendChild(arenaDoc.createTextNode(newLine + "  "));
         curMap.appendChild(_combatMap.getXmlObject(arenaDoc, uniqueIDs/*includeKnownByUniqueIDInfo*/, newLine + "  "));
         curMap.appendChild(arenaDoc.createTextNode(newLine));
         for (CombatMap storedMap : ArenaEvent._savedMaps.values()) {
            if (storedMap.getSizeX() > 0) {
               mapList.appendChild(arenaDoc.createTextNode(newLine + "  "));
               mapList.appendChild(storedMap.getXmlObject(arenaDoc, uniqueIDs/*includeKnownByUniqueIDInfo*/, newLine + "  "));
            }
         }
      } catch (ParserConfigurationException e) {
         e.printStackTrace();
      }
      return arenaDoc;
   }
   private boolean serializeFromXmlObject(Node mainElement) {
      NamedNodeMap namedNodeMap = mainElement.getAttributes();
      if (namedNodeMap == null) {
         return false;
      }
      String pseudoRandomNumberSeed = namedNodeMap.getNamedItem("PseudoRandomNumberSeed").getNodeValue();
      CombatServer.setPseudoRandomNumberSeed(Integer.parseInt(pseudoRandomNumberSeed));
      String pseudoRandomNumberUseCountStr = namedNodeMap.getNamedItem("PseudoRandomNumberUseCount").getNodeValue();
      int pseudoRandomNumberUseCount = Integer.parseInt(pseudoRandomNumberUseCountStr);
      while (pseudoRandomNumberUseCount-- > 0) {
         CombatServer.random();
      }
      terminateBattle();
      removeAllCombatants();
      _battle = new Battle("BattleThread", this, _server);

      NodeList children = mainElement.getChildNodes();
      ArenaEvent._savedMaps = new HashMap<>();
      for (int index=0 ; index<children.getLength() ; index++) {
         Node child = children.item(index);
         String name = child.getNodeName();
         if (_battle.serializeFromXmlObject(child)) {
         }
         else if ((name.equals("localCharacters")) ||
                  (name.equals("remoteCharacters")) ||
                  (name.equals("aiCharacters"))) {
            NodeList grandChildren = child.getChildNodes();
            for (int i=0 ; i<grandChildren.getLength() ; i++) {
               Node grandChild = grandChildren.item(i);
               Character character = new Character();
               if (character.serializeFromXmlObject(grandChild)) {
                  if (name.equals("localCharacters")) {
                     addCombatant(character, null/*AIEngineType*/, false/*setInitiativeAndSpendActions*/);
                  }
                  else if (name.equals("remoteCharacters")) {
                     // TODO: how do we reconnect to a remote player?
                     addCombatant(character, null/*AIEngineType*/, false/*setInitiativeAndSpendActions*/);
                  }
                  else if (name.equals("aiCharacters")) {
                     String aiType = AI_Type.NORM.name;
                     NamedNodeMap attributes = child.getAttributes();
                     if (attributes != null) {
                        Node aiTypeNode = attributes.getNamedItem("aiType");
                        if (aiTypeNode != null) {
                           aiType = aiTypeNode.getNodeValue();
                        }
                     }
                     addCombatant(character, AI_Type.getByString(aiType), false/*setInitiativeAndSpendActions*/);
                  }
               }
            }
         }
         else if (name.equals("CurrentMap")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i=0 ; i<grandChildren.getLength() ; i++) {
               Node grandChild = grandChildren.item(i);
               _combatMap.serializeFromXmlObject(grandChild);
            }
         }
         else if (name.equals("StoredMaps")) {
            NodeList grandChildren = child.getChildNodes();
            for (int i=0 ; i<grandChildren.getLength() ; i++) {
               Node grandChild = grandChildren.item(i);
               CombatMap map = new CombatMap();
               map.serializeFromXmlObject(grandChild);
               ArenaEvent._savedMaps.put(map.getName(), map);
            }
         }
      }
      int highestUniqueID = 0;
      List<Character> combatants = getCombatants();
      for (Character character : combatants) {
         character.setCasterAndTargetFromIDs(combatants);
         if (character._uniqueID > highestUniqueID) {
            highestUniqueID = character._uniqueID;
         }
      }
      // make sure that any new characters don't get created with an already used uniqueID;
      ClientProxy.setNextServerID(highestUniqueID);

      recomputeAllTargets(null);
      recomputeAllKnownLocations();

      if (_paused) {
         _battle.onPause();
      }
      _battle._startMidTurn = true;
      _battle.start();
      return false;
   }
   public void onBattleComplete(Battle battle) {
      if (_autoRunBlock != null) {
         int teamAlive = -1;
         for (int t=0 ; t<TEAM_NAMES.length ; t++) {
            if (isTeamAlive(t)) {
               if (teamAlive != -1) {
                  teamAlive = -1;
                  DebugBreak.debugBreak();
               }
               else {
                  teamAlive = t;
               }
            }
         }

         if (_autoRunBlock.battleEnded(teamAlive)) {
            _combatMap = _autoRunMap.clone();
            CombatServer._this.getShell().getDisplay().asyncExec(new Runnable() {
               @Override
               public void run() {
                  restart();
               }
            });
         }
         else {
            _autoRunBlock = null;
            _autoRunMap = null;
         }
      }
   }

   public boolean doLocalPlayersExist() {
      return (_localCombatants != null) && !_localCombatants.isEmpty();
   }

   public boolean isRemotelyControlled(Character combatant) {
      return _charactersWaitingToConnect.contains(combatant) ||
               _mapProxyToCombatant.containsValue(combatant);
   }
   public AI_Type getAiType(Character combatant) {
      if (!_localCombatants.contains(combatant)) {
         AI ai = _mapCombatantToAI.get(combatant);
         if (ai != null) {
            return ai.getAiType();
         }
      }
      return null;
   }
   public void setControl(Character combatant, boolean localControl, AI_Type AIEngineType) {
      if (localControl) {
         _charactersWaitingToConnect.remove(combatant);
         // make sure we don't duplicate this combatant by removing it first (no harm if not in list)
         _localCombatants.remove(combatant);
         if (AIEngineType == null) {
            _mapCombatantToAI.remove(combatant);
            _localCombatants.add(combatant);
         }
         else {
            _mapCombatantToAI.put(combatant, new AI(combatant, AIEngineType));
         }

         ClientProxy clientProxy = _mapCombatantToProxy.remove(combatant);
         if (clientProxy != null) {
            _mapProxyToCombatant.remove(clientProxy);
         }
      }
      else {
         // changing to remote connection
         // make sure we don't duplicate this combatant by removing it first (no harm if not in list)
         _charactersWaitingToConnect.remove(combatant);
         _charactersWaitingToConnect.add(combatant);
         _mapCombatantToAI.remove(combatant);
         _localCombatants.remove(combatant);
      }
   }

}

