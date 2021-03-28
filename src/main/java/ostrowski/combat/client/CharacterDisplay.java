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
   public final  CharacterWidget     charWidget;
   public final  CharacterFile       charFile            = new CharacterFile("character.data");
   // these object all implement the IUIBlock Interface:
   private final AttackBlock         attackBlock         = new AttackBlock(this);
   private final CharInfoBlock       charInfoBlock       = new CharInfoBlock(this);
   private final TargetPriorityBlock targetPriorityBlock = new TargetPriorityBlock(this);
   public        ServerConnection    serverConnection    = null;
   //private SpellsBlock         spellsBlock         = new SpellsBlock(this);
   // these dont:
   private final ConditionBlock      conditionBlock      = new ConditionBlock(this);
   private final Configuration       configurationBlock  = new Configuration();
   private final ConnectionBlock     connectionBlock     = new ConnectionBlock(this);
   private final MessagesBlock       messagesBlock       = new MessagesBlock(this);
   private final ArenaMapBlock       arenaMapBlock       = new ArenaMapBlock();
   private final AIBlock             aiBlock             = new AIBlock(this);
   public        int                 uniqueConnectionID  = -1;
   private final List<SyncRequest>   pendingRequests     = new ArrayList<>();
   public        Shell               shell;
   public final  List<Helper>        uiBlocks            = new ArrayList<>();
   boolean inRefreshDisplay = false;


   transient private final MouseOverCharacterInfoPopup mouseOverCharInfoPopup = new MouseOverCharacterInfoPopup();

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
      charWidget = new CharacterWidget(preferedCharName, charFile, this);
      uiBlocks.add(attackBlock);
      uiBlocks.add(charInfoBlock);
      uiBlocks.add(targetPriorityBlock);
      uiBlocks.add(conditionBlock);
      //_uiBlocks.add(spellsBlock);
   }

   public void buildCharSheet(Shell shell, boolean startOnArenaPage, boolean aiOn) {
      this.shell = shell;
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
         charWidget.buildCharSheet(characterData);
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

         connectionBlock.buildBlock(page2LeftBlock);
         arenaMapBlock.buildBlock(page2MiddleBlock, 2); // arenaMapBlock uses to rows & 2 column
         arenaMapBlock.addControlGroup(page2MiddleBlock);
         aiBlock.buildBlock(page2MiddleBlock);
         targetPriorityBlock.buildBlock(page2RightBlock);
         charInfoBlock.buildBlock(page2RightBlock);
         conditionBlock.buildBlock(page2LeftBlock);
         configurationBlock.buildDisplay(page2LeftBlock, false/*isServer*/);
         //    new Label(page2Block, 0);
         attackBlock.buildBlock(page2RightBlock);

         messagesBlock.buildBlock(botBlock);

         // setup the listener mechanism so we are told about clicks on the map that change the target.
         arenaMapBlock.addListener(this);

         // disable the controls that should not be enabled until we connect.
         messagesBlock.enableControls(false/*enabledFlag*/);
      }
      {
         TabItem item = new TabItem(tabFolder, SWT.NULL);
         item.setText("Rules");
         // add the control to the TabItem
         item.setControl(new RuleComposite(tabFolder, 1, GridData.FILL_BOTH, null/*configuration*/,
                                           900/*width*/, shell.getDisplay().getSystemColor(SWT.COLOR_WHITE)));
      }
      if (aiOn) {
         aiBlock.setAI(true);
      }

      //      List<Control> tabArray = new ArrayList();
      //      connectionBlock.getTabItems(tabArray);
      //      Control[] tabList = new Control[tabArray.size()];
      //      arenaData.setTabList(tabArray.toArray(tabList));
      updateDisplayFromCharacter();
   }

   public void sendMessageText(String message) {
      MessageText msg = new MessageText(charWidget.character.getName(), message, null, false/*popUp*/, true/*isPublic*/);
      if (serverConnection != null) {
         serverConnection.sendObject(msg, "server");
      }
   }

   public void appendMessage(String string) {
      messagesBlock.appendMesage(string);
   }

   public void connectToServer(String ipAddress, int port) {
      serverConnection = new ServerConnection(this);
      serverConnection.connect(ipAddress, port);
      serverConnection.start();
   }

   public void disconnectFromServer() {
      if (serverConnection != null) {
         serverConnection.shutdown();
         serverConnection = null;
      }
   }

   public void handleDisconnect() {
      connectionBlock.handleDisconnect();
      messagesBlock.enableControls(false/*enabledFlag*/);
      CombatMap map = new CombatMap((short) 0, (short) 0, null/*diag*/);
      if (charWidget.character != null) {
         arenaMapBlock.updateMap(map, charWidget.character.uniqueID, charWidget.character.teamID,
                                 null/*availableLocs*/, charInfoBlock.getTargetUniqueID());
      }
      else {
         arenaMapBlock.updateMap(map, -1/*selfID*/, (byte) -1/*teamID*/, null/*availableLocs*/, charInfoBlock.getTargetUniqueID());
      }
      setControls(true/*enabledFlag*/, true/*visibleFlag*/);
   }

   public void handleConnect() {
      connectionBlock.handleConnect();
      messagesBlock.enableControls(true/*enabledFlag*/);
   }

   public void beginBattle(BeginBattle battleMsg) {
      // nothing to do
   }

   //   public void handleMessage(final String inputLine)
   //   {
   //      // we can't modify any UI element from another thread,
   //      // so we must use the Display.asyncExec() method:
   //      Display display = shell.getDisplay();
   //      display.asyncExec(new Runnable() {
   //         public void run() {
   //            handleMessageInUIThread(inputLine);
   //         }
   //      });
   //   }

   public void updateDisplayFromCharacter() {
      CharacterWidget.inModify = true;
      try {
         for (Helper helper : uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.updateDisplayFromCharacter(charWidget.character);
         }
         charWidget.updateCharacterFromDisplay();
         refreshDisplay();
      } finally {
         CharacterWidget.inModify = false;
      }
   }

   public void refreshDisplay() {
      if (inRefreshDisplay) {
         return;
      }
      inRefreshDisplay = true;
      try {
         for (Helper helper : uiBlocks) {
            IUIBlock block = (IUIBlock) helper;
            block.refreshDisplay(charWidget.character);
         }
         charWidget.refreshDisplay();
         shell.setText((charWidget.character == null) ? "character" : charWidget.character.getName());
         arenaMapBlock.redraw();
      } finally {
         inRefreshDisplay = false;
      }
   }

   public void updateCharacterFromDisplay() {
      for (Helper helper : uiBlocks) {
         IUIBlock block = (IUIBlock) helper;
         block.updateCharacterFromDisplay(charWidget.character);
      }
      charWidget.updateCharacterFromDisplay();
   }

   public void setControls(boolean enabledFlag, boolean visibleFlag) {
      charWidget.setControls(enabledFlag, visibleFlag);
      conditionBlock.enableControls(enabledFlag);
   }

   public void updateServerStatus(ServerStatus status) {
      List<Character> combatants = status.getCombatants();
      charInfoBlock.updateCombatants(combatants);
      targetPriorityBlock.updateCombatants(combatants);
      for (Character combatant : combatants) {
         if (combatant.uniqueID == charWidget.character.uniqueID) {
            charWidget.character.copyData(combatant);
            updateDisplayFromCharacter();
         }
         arenaMapBlock.updateCombatant(combatant);
      }
      connectionBlock.updateServerStatus(status);
   }

   public void updateMap(CombatMap map) {
      if (map != null) {
         arenaMapBlock.updateMap(map, charWidget.character.uniqueID, charWidget.character.teamID,
                                 map.getAvailablePlayerLocs(), charInfoBlock.getTargetUniqueID());
//         if ((charWidget._character._locX != -1) && (charWidget._character._locY != -1)) {
//            arenaMapBlock.updateCombatant(charWidget._character);
//         }
      }
   }

   public void updateCharacter(Character character) {
      boolean selfMoved = false;
      if (character.uniqueID == charWidget.character.uniqueID) {
         // using the compareTo method compares the locations of the characters
         if (charWidget.character.getCondition().getOrientation().getCoordinates().isEmpty() ||
             (charWidget.character.compareTo(character) != 0)) {
            selfMoved = true;
         }
         charWidget.character.copyData(character);
      }
      charInfoBlock.updateCombatant(character);
      targetPriorityBlock.updateCombatant(character);
      arenaMapBlock.updateCombatant(character);
      if (!CharacterWidget.inModify) {
         CharacterWidget.inModify = true;
         try {
            charWidget.updateCharacter(character);
            if (character.uniqueID == charWidget.character.uniqueID) {
               refreshDisplay();
            }
         } finally {
            CharacterWidget.inModify = false;
         }
      }
      if (selfMoved) {
         arenaMapBlock.recomputeVisibility(character, null/*diag*/);
      }
   }

   @Override
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }

   @Override
   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      // are we waiting on a location request?
      if (!pendingRequests.isEmpty()) {
         SyncRequest syncReq = pendingRequests.get(0);
         if (syncReq instanceof RequestMovement) {
            // If we are in the middle of a movement, don't display character information.
            return;
         }
      }
      mouseOverCharInfoPopup.onMouseMove(loc, event, angleFromCenter, normalizedDistFromCenter);
   }

   @Override
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      if (loc == null) {
         return;
      }
      // are we waiting on a location request?
      if (!pendingRequests.isEmpty()) {
         SyncRequest syncReq = pendingRequests.get(0);
         boolean rightClick = (event.button != 1);
         if (rightClick) {
            return;
         }
         if (syncReq instanceof RequestMovement) {
            RequestMovement moveReq = (RequestMovement) syncReq;
            // fill in the location into the request, and send it back.
            // If it's a valid location, then this will return true.
            if (moveReq.setOrientation(loc, angleFromCenter, normalizedDistFromCenter)) {
               serverConnection.sendObject(syncReq, "server");
               pendingRequests.remove(syncReq);
               arenaMapBlock.endHexSelection();
               return;
            }
         }
         if (syncReq instanceof RequestLocation) {
            RequestLocation locReq = (RequestLocation) syncReq;
            if (locReq.setAnswer(loc.x, loc.y)) {
               serverConnection.sendObject(syncReq, "server");
               pendingRequests.remove(syncReq);
               arenaMapBlock.endHexSelection();
               return;
            }
         }
      }
      // If this didn't match the first pendingRequest, maybe they are changing targets
      // A click on a character means 'target this character'.
      List<Character> characters = loc.getCharacters();
      for (Character target : characters) {
         // Never target yourself.
         // TODO: what about healing spells? targeting yourself should be allowed for this.
         if (!target.getName().equals(charWidget.character.getName())) {
            charInfoBlock.updateTargetFromCharacter(target);
            arenaMapBlock.updateTargetID(target.uniqueID);
            arenaMapBlock.redraw();
            break;
         }
      }
   }

   @Override
   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }

   public void setUniqueConnectionID(int id) {
      uniqueConnectionID = id;
      charWidget.character.uniqueID = uniqueConnectionID;
      Rules.setDiagComponentName("Client" + id);
   }

   public void setCharacter(Character character) {
      charWidget.character = character;
      charWidget.character.uniqueID = uniqueConnectionID;
   }

   public void requestMovement(RequestMovement moveRequest) {
      pendingRequests.add(moveRequest);
      arenaMapBlock.requestMovement(moveRequest);
   }

   public void requestLocation(RequestLocation locRequest) {
      pendingRequests.add(locRequest);
      arenaMapBlock.requestLocation(locRequest);
   }

   public boolean requestAttackStyle(RequestAttackStyle styleRequest) {
      return styleRequest.setAnswerID(attackBlock.getAttackStyle(styleRequest));
   }

   public void updateTargetPriorities(List<Character> orderedEnemies) {
      TargetPriorities targets = new TargetPriorities(orderedEnemies);
      if (serverConnection != null) {
         serverConnection.sendObject(targets, "server");
      }
      if (charWidget.ai != null) {
         charWidget.ai.updateTargetPriorities(orderedEnemies);
      }
   }

   public CombatMap getCombatMap() {
      return arenaMapBlock.getCombatMap();
   }

   public List<Character> getOrderedEnemies() {
      return targetPriorityBlock.getOrderedEnemies();
   }

   public boolean ignoreEnemy(Character enemy) {
      return targetPriorityBlock.ignoreEnemy(enemy);
   }

   public void setAIEngine(String aiEngine) {
      if (aiEngine.equalsIgnoreCase("Off")) {
         charWidget.ai = null;
      }
      else {
         charWidget.ai = new AI(charWidget.character, AI_Type.getByString(aiEngine));
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
   }

   public void updateArenaLocation(ArenaLocation arenaLoc) {
      arenaMapBlock.updateArenaLocation(arenaLoc);
   }

   public void updateMapVisibility(MapVisibility mapVisibilty) {
      arenaMapBlock.updateMapVisibility(mapVisibilty);
   }
}
