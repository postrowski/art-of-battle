package ostrowski.combat.client;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.client.ui.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.protocol.*;
import ostrowski.combat.protocol.request.RequestAttackStyle;
import ostrowski.combat.protocol.request.RequestLocation;
import ostrowski.combat.protocol.request.RequestMovement;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.SyncRequest;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.List;

/*
 * Created on May 5, 2006
 */

/**
 * @author Paul
 *
 */
public class CharacterDisplay implements Enums, ModifyListener, IMapListener //, Listener
{
   public final CharacterWidget _charWidget;
   public final CharacterFile   _charFile            = new CharacterFile("character.data");
   // these object all implement the IUIBlock Interface:
   private final AttackBlock         _attackBlock         = new AttackBlock(this);
   private final CharInfoBlock       _charInfoBlock       = new CharInfoBlock(this);
   private final TargetPriorityBlock _targetPriorityBlock = new TargetPriorityBlock(this);
   public        ServerConnection    _serverConnection    = null;
   //private SpellsBlock         _spellsBlock         = new SpellsBlock(this);
   // these dont:
   private final ConditionBlock      _conditionBlock      = new ConditionBlock(this);
   private final Configuration       _configurationBlock  = new Configuration();
   private final ConnectionBlock     _connectionBlock     = new ConnectionBlock(this);
   private final MessagesBlock       _messagesBlock       = new MessagesBlock(this);
   private final ArenaMapBlock       _arenaMapBlock       = new ArenaMapBlock();
   private final AIBlock             _aiBlock             = new AIBlock(this);
   public        int                 _uniqueConnectionID  = -1;
   private final List<SyncRequest>   _pendingRequests     = new ArrayList<>();
   public        Shell               _shell;
   public final  List<Helper>   _uiBlocks            = new ArrayList<>();

   transient private final MouseOverCharacterInfoPopup _mouseOverCharInfoPopup = new MouseOverCharacterInfoPopup();

//   @Override
//   public void handleEvent(Event event) {
//      if ((event.widget.getDisplay() != null) &&
//          (event.widget.getDisplay().getActiveShell() != null)) {
//         if (event.type == SWT.MouseEnter) {
//            event.widget.getDisplay().getActiveShell().setCursor(new Cursor(event.display, SWT.CURSOR_SIZENS));
//         }
//      }
//   }

   public CharacterDisplay(String preferedCharName) {
      _charWidget = new CharacterWidget(preferedCharName, _charFile, this);
      _uiBlocks.add(_attackBlock);
      _uiBlocks.add(_charInfoBlock);
      _uiBlocks.add(_targetPriorityBlock);
      _uiBlocks.add(_conditionBlock);
      //_uiBlocks.add(_spellsBlock);
   }

   public void buildCharSheet(Shell shell, boolean startOnArenaPage, boolean aiOn) {
      _shell = shell;
      shell.setLayout(new FillLayout());

      TabFolder tabFolder = new TabFolder(shell, SWT.NONE);

      // create a TabItem
      {
         TabItem item = new TabItem(tabFolder, SWT.NULL);
         item.setText("Character Data");
         // create a control
         Composite characterData = Helper.createComposite(tabFolder, 1, GridData.FILL_BOTH);
         // add the control to the TabItem
         item.setControl(characterData);
         _charWidget.buildCharSheet(characterData);
      }
      // create the next tab
      {
         TabItem item = new TabItem(tabFolder, SWT.NULL);
         item.setText("Arena Map");
         SashForm mainSash = new SashForm(tabFolder, SWT.VERTICAL);
         GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         mainSash.setLayoutData(data);
         mainSash.SASH_WIDTH = 4;
         mainSash.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
//         mainSash.getDisplay().getActiveShell().setCursor(new Cursor(mainSash.getDisplay(), SWT.CURSOR_SIZENS));
//         mainSash.addListener(SWT.MouseEnter, this);
//         mainSash.addListener(SWT.MouseExit, this);

         // add the control to the TabItem
         item.setControl(mainSash);

         Composite topBlock = Helper.createComposite(mainSash, 1, GridData.FILL_BOTH/*gridDataStyle*/);
         Composite botBlock = Helper.createComposite(mainSash, 1, GridData.FILL_BOTH);
         mainSash.setWeights(new int[] { 4, 3});

         if (startOnArenaPage) {
            tabFolder.setSelection(1);
         }

         topBlock.setLayout(new GridLayout(3 /*columns*/, false/*sameWidth*/));
         data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         topBlock.setLayoutData(data);

         Composite page2LeftBlock = Helper.createComposite(topBlock, 1, GridData.FILL_HORIZONTAL);
         page2LeftBlock.setLayout(new GridLayout(1 /*columns*/, false/*sameWidth*/));
         data = new GridData(SWT.FILL, SWT.FILL, false/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.verticalAlignment = SWT.BEGINNING;
         data.horizontalAlignment = SWT.CENTER;
         page2LeftBlock.setLayoutData(data);

         Composite page2MiddleBlock = Helper.createComposite(topBlock, 1, GridData.FILL_BOTH/*gridDataStyle*/);
         page2MiddleBlock.setLayout(new GridLayout(2 /*columns*/, false/*sameWidth*/));
         data = new GridData();
         data.verticalAlignment = GridData.FILL;
         data.horizontalAlignment = GridData.FILL;
         data.grabExcessVerticalSpace = true;
         data.grabExcessHorizontalSpace = true;
         page2MiddleBlock.setLayoutData(data);

         Composite page2RightBlock = Helper.createComposite(topBlock, 1, GridData.FILL_HORIZONTAL);
         page2RightBlock.setLayout(new GridLayout(1 /*columns*/, false/*sameWidth*/));
         data = new GridData(SWT.FILL, SWT.FILL, false/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.verticalAlignment = SWT.BEGINNING;
         data.horizontalAlignment = SWT.CENTER;
         page2RightBlock.setLayoutData(data);

         _connectionBlock.buildBlock(page2LeftBlock);
         _arenaMapBlock.buildBlock(page2MiddleBlock, 2); // arenaMapBlock uses to rows & 2 column
         _arenaMapBlock.addControlGroup(page2MiddleBlock);
         _aiBlock.buildBlock(page2MiddleBlock);
         _targetPriorityBlock.buildBlock(page2RightBlock);
         _charInfoBlock.buildBlock(page2RightBlock);
         _conditionBlock.buildBlock(page2LeftBlock);
         _configurationBlock.buildDisplay(page2LeftBlock, false/*isServer*/);
         //    new Label(page2Block, 0);
         _attackBlock.buildBlock(page2RightBlock);

         _messagesBlock.buildBlock(botBlock);

         // setup the listener mechanism so we are told about clicks on the map that change the target.
         _arenaMapBlock.addListener(this);

         // disable the controls that should not be enabled until we connect.
         _messagesBlock.enableControls(false/*enabledFlag*/);
      }
      {
         TabItem item = new TabItem(tabFolder, SWT.NULL);
         item.setText("Rules");
         // add the control to the TabItem
         item.setControl(new RuleComposite(tabFolder, 1, GridData.FILL_BOTH, null/*configuration*/,
                                           900/*width*/, shell.getDisplay().getSystemColor(SWT.COLOR_WHITE)));
      }
      if (aiOn) {
         _aiBlock.setAI(true);
      }

      //      List<Control> tabArray = new ArrayList();
      //      _connectionBlock.getTabItems(tabArray);
      //      Control[] tabList = new Control[tabArray.size()];
      //      arenaData.setTabList(tabArray.toArray(tabList));
      updateDisplayFromCharacter();
   }

   public void sendMessageText(String message) {
      MessageText msg = new MessageText(_charWidget._character.getName(), message, null, false/*popUp*/, true/*isPublic*/);
      if (_serverConnection != null) {
         _serverConnection.sendObject(msg, "server");
      }
   }

   public void appendMessage(String string) {
      _messagesBlock.appendMesage(string);
   }

   public void connectToServer(String ipAddress, int port) {
      _serverConnection = new ServerConnection(this);
      _serverConnection.connect(ipAddress, port);
      _serverConnection.start();
   }

   public void disconnectFromServer() {
      if (_serverConnection != null) {
         _serverConnection.shutdown();
         _serverConnection = null;
      }
   }

   public void handleDisconnect() {
      _connectionBlock.handleDisconnect();
      _messagesBlock.enableControls(false/*enabledFlag*/);
      CombatMap map = new CombatMap((short) 0, (short) 0, null/*diag*/);
      if (_charWidget._character != null) {
         _arenaMapBlock.updateMap(map, _charWidget._character._uniqueID, _charWidget._character._teamID,
                                  null/*availableLocs*/, _charInfoBlock.getTargetUniqueID());
      }
      else {
         _arenaMapBlock.updateMap(map, -1/*selfID*/, (byte) -1/*teamID*/, null/*availableLocs*/, _charInfoBlock.getTargetUniqueID());
      }
      setControls(true/*enabledFlag*/, true/*visibleFlag*/);
   }

   public void handleConnect() {
      _connectionBlock.handleConnect();
      _messagesBlock.enableControls(true/*enabledFlag*/);
   }

   public void beginBattle(BeginBattle battleMsg) {
      // nothing to do
   }

   //   public void handleMessage(final String inputLine)
   //   {
   //      // we can't modify any UI element from another thread,
   //      // so we must use the Display.asyncExec() method:
   //      Display display = _shell.getDisplay();
   //      display.asyncExec(new Runnable() {
   //         public void run() {
   //            handleMessageInUIThread(inputLine);
   //         }
   //      });
   //   }

   public void updateDisplayFromCharacter() {
      CharacterWidget._inModify = true;
      try {
         for (Helper helper : _uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.updateDisplayFromCharacter(_charWidget._character);
         }
         _charWidget.updateCharacterFromDisplay();
         refreshDisplay();
      } finally {
         CharacterWidget._inModify = false;
      }
   }

   boolean _inRefreshDisplay = false;

   public void refreshDisplay() {
      if (_inRefreshDisplay) {
         return;
      }
      _inRefreshDisplay = true;
      try {
         for (Helper helper : _uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.refreshDisplay(_charWidget._character);
         }
         _charWidget.refreshDisplay();
         _shell.setText((_charWidget._character == null) ? "character" : _charWidget._character.getName());
         _arenaMapBlock.redraw();
      } finally {
         _inRefreshDisplay = false;
      }
   }

   public void updateCharacterFromDisplay() {
      for (Helper helper : _uiBlocks) {
         IUIBlock block = (IUIBlock) helper;
         block.updateCharacterFromDisplay(_charWidget._character);
      }
      _charWidget.updateCharacterFromDisplay();
   }

   public void setControls(boolean enabledFlag, boolean visibleFlag) {
      _charWidget.setControls(enabledFlag, visibleFlag);
      _conditionBlock.enableControls(enabledFlag);
   }

   public void updateServerStatus(ServerStatus status) {
      List<Character> combatants = status.getCombatants();
      _charInfoBlock.updateCombatants(combatants);
      _targetPriorityBlock.updateCombatants(combatants);
      for (Character combatant : combatants) {
         if (combatant._uniqueID == _charWidget._character._uniqueID) {
            _charWidget._character.copyData(combatant);
            updateDisplayFromCharacter();
         }
         _arenaMapBlock.updateCombatant(combatant);
      }
      _connectionBlock.updateServerStatus(status);
   }

   public void updateMap(CombatMap map) {
      if (map != null) {
         _arenaMapBlock.updateMap(map, _charWidget._character._uniqueID, _charWidget._character._teamID,
                                  map.getAvailablePlayerLocs(), _charInfoBlock.getTargetUniqueID());
//         if ((_charWidget._character._locX != -1) && (_charWidget._character._locY != -1)) {
//            _arenaMapBlock.updateCombatant(_charWidget._character);
//         }
      }
   }

   public void updateCharacter(Character character) {
      boolean selfMoved = false;
      if (character._uniqueID == _charWidget._character._uniqueID) {
         // using the compareTo method compares the locations of the characters
         if (_charWidget._character.getCondition().getOrientation().getCoordinates().isEmpty() ||
             (_charWidget._character.compareTo(character) != 0)) {
            selfMoved = true;
         }
         _charWidget._character.copyData(character);
      }
      _charInfoBlock.updateCombatant(character);
      _targetPriorityBlock.updateCombatant(character);
      _arenaMapBlock.updateCombatant(character);
      if (!CharacterWidget._inModify) {
         CharacterWidget._inModify = true;
         try {
            _charWidget.updateCharacter(character);
            if (character._uniqueID == _charWidget._character._uniqueID) {
               refreshDisplay();
            }
         } finally {
            CharacterWidget._inModify = false;
         }
      }
      if (selfMoved) {
         _arenaMapBlock.recomputeVisibility(character, null/*diag*/);
      }
   }

   @Override
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }

   @Override
   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      _mouseOverCharInfoPopup.onMouseMove(loc, event, angleFromCenter, normalizedDistFromCenter);
   }

   @Override
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      if (loc == null) {
         return;
      }
      // are we waiting on a location request?
      if (!_pendingRequests.isEmpty()) {
         SyncRequest syncReq = _pendingRequests.get(0);
         boolean rightClick = (event.button != 1);
         if (rightClick) {
            return;
         }
         if (syncReq instanceof RequestMovement) {
            RequestMovement moveReq = (RequestMovement) syncReq;
            // fill in the location into the request, and send it back.
            // If it's a valid location, then this will return true.
            if (moveReq.setOrientation(loc, angleFromCenter, normalizedDistFromCenter)) {
               _serverConnection.sendObject(syncReq, "server");
               _pendingRequests.remove(syncReq);
               _arenaMapBlock.endHexSelection();
               return;
            }
         }
         if (syncReq instanceof RequestLocation) {
            RequestLocation locReq = (RequestLocation) syncReq;
            if (locReq.setAnswer(loc._x, loc._y)) {
               _serverConnection.sendObject(syncReq, "server");
               _pendingRequests.remove(syncReq);
               _arenaMapBlock.endHexSelection();
               return;
            }
         }
      }
      // If this didn't match the first _pendingRequest, maybe they are changing targets
      // A click on a character means 'target this character'.
      List<Character> characters = loc.getCharacters();
      for (Character target : characters) {
         // Never target yourself.
         // TODO: what about healing spells? targeting yourself should be allowed for this.
         if (!target.getName().equals(_charWidget._character.getName())) {
            _charInfoBlock.updateTargetFromCharacter(target);
            _arenaMapBlock.updateTargetID(target._uniqueID);
            _arenaMapBlock.redraw();
            break;
         }
      }
   }

   @Override
   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }

   public void setUniqueConnectionID(int id) {
      _uniqueConnectionID = id;
      _charWidget._character._uniqueID = _uniqueConnectionID;
      Rules.setDiagComponentName("Client" + id);
   }

   public void setCharacter(Character character) {
      _charWidget._character = character;
      _charWidget._character._uniqueID = _uniqueConnectionID;
   }

   public void requestMovement(RequestMovement moveRequest) {
      _pendingRequests.add(moveRequest);
      _arenaMapBlock.requestMovement(moveRequest);
   }

   public void requestLocation(RequestLocation locRequest) {
      _pendingRequests.add(locRequest);
      _arenaMapBlock.requestLocation(locRequest);
   }

   public boolean requestAttackStyle(RequestAttackStyle styleRequest) {
      return styleRequest.setAnswerID(_attackBlock.getAttackStyle(styleRequest));
   }

   public void updateTargetPriorities(List<Character> orderedEnemies) {
      TargetPriorities targets = new TargetPriorities(orderedEnemies);
      if (_serverConnection != null) {
         _serverConnection.sendObject(targets, "server");
      }
      if (_charWidget._ai != null) {
         _charWidget._ai.updateTargetPriorities(orderedEnemies);
      }
   }

   public CombatMap getCombatMap() {
      return _arenaMapBlock.getCombatMap();
   }

   public List<Character> getOrderedEnemies() {
      return _targetPriorityBlock.getOrderedEnemies();
   }

   public boolean ignoreEnemy(Character enemy) {
      return _targetPriorityBlock.ignoreEnemy(enemy);
   }

   public void setAIEngine(String aiEngine) {
      if (aiEngine.equalsIgnoreCase("Off")) {
         _charWidget._ai = null;
      }
      else {
         _charWidget._ai = new AI(_charWidget._character, AI_Type.getByString(aiEngine));
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
   }

   public void updateArenaLocation(ArenaLocation arenaLoc) {
      _arenaMapBlock.updateArenaLocation(arenaLoc);
   }

   public void updateMapVisibility(MapVisibility mapVisibilty) {
      _arenaMapBlock.updateMapVisibility(mapVisibilty);
   }
}
