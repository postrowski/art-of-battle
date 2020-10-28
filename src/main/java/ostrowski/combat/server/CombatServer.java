/*
 * Created on May 10, 2006
 *
 */
package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.DebugBreak;
import ostrowski.combat.client.CombatClient;
import ostrowski.combat.client.MessageDialog;
import ostrowski.combat.client.RequestUserInput;
import ostrowski.combat.client.ui.AutoRunBlock;
import ostrowski.combat.client.ui.CharInfoBlock;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.graphics.objects3d.Helper;
import ostrowski.util.ClientListener;
import ostrowski.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class CombatServer extends Helper implements SelectionListener, Enums, IMapListener, ModifyListener, Listener, KeyListener, ControlListener, ShellListener
{
   private static final String VERSION_NUMBER = "4.2.0";

   public static final HashMap<Thread, HashMap<String, Object>> threadStorage_ = new HashMap<>();
   public static Object getThreadStorage(String key) {
      HashMap<String, Object> threadsStorage = threadStorage_.get(Thread.currentThread());
      if (threadsStorage == null) {
         return null;
      }
      return threadsStorage.get(key);
   }

   public static void main(String[] args)
   {
      List<String> newArgs = new ArrayList<>(Arrays.asList(args));
      if (!newArgs.isEmpty()) {
         if (newArgs.get(0).equalsIgnoreCase("client")) {
            newArgs.remove(0);
            CombatClient.main(newArgs);
            return;
         }
      }
      main(newArgs);
   }
   public static void main(List<String> args) {
      AnglePairDegrees.test();
      probabilityTest();
      @SuppressWarnings("unused")
      Attribute at = Attribute.Strength;// force load of this enum
      Rules.setDiagComponentName("Server");
      CombatServer server = new CombatServer(args);
      server.execute();
      server.cleanUpOnExit();
   }

   static void probabilityTest() {
      DiceSet dice = new DiceSet("1d10±");
      double odds = dice.getOddsForTN(-31);
      //noinspection MismatchedReadAndWriteOfArray
      @SuppressWarnings("unused")
      double[] odd = new double[40];
      for (int i=0 ; i<40 ; i++) {
         odd[i] = dice.getOddsForTN(i-10);
      }
      for (int i=-30 ; i<30 ; i++) {
         double newOdds = dice.getOddsForTN(i);
         if (newOdds > odds) {
            DebugBreak.debugBreak();
         }
         odds = newOdds;
      }
      double odds1  = dice.getOddsForTN(1);
      double odds2  = dice.getOddsForTN(2);
      double odds10 = dice.getOddsForTN(10);
      double odds11 = dice.getOddsForTN(11);
      double odds0  = dice.getOddsForTN(0);
      double odds20 = dice.getOddsForTN(20);
      double odds_5 = dice.getOddsForTN(-5);
      @SuppressWarnings("unused")
      double t = odds1 + odds2 + odds10 + odds11 + odds0 + odds20 + odds_5;
      dice = null;
   }

   static final int WINDOW_WIDTH = 950;
   private Shell                            _shell;
   Diagnostics                              _diag             = null;
   public static boolean                    _isServer         = false;
   public static CombatServer               _this             = null;

   // UI elements:
   private Button                           _openButton;
   public  Button                           _usePseudoRandomNumbers;
   private Text                             _pseudoRandomNumberSeedText;
   private Combo                            _arenaName;
   private Button                           _saveButton;
   private Button                           _newMapButton;
   private Button                           _openMapButton;
   private Button                           _saveMapButton;
   private Text                             _arenaSizeXValue;
   private Text                             _arenaSizeYValue;
   public        IMapWidget _map;
   private final Browser    _fullMessages;
   private       Browser    _messages;

   private ClientListener                   _clientListener;
   private final Arena                      _arena;
   private Button[/*team*/][/*id*/]         _combatantsButtons;
   private Combo[/*team*/][/*id*/]          _combatantsName;
   private Combo[/*team*/][/*id*/]          _combatantsAI;
   private final byte                       _maxTeams         = CombatServer.MAX_TEAMS;
   private byte                             _currentTeam      = -1;
   private byte                             _currentCombatant = -1;
   private boolean                          _changingMap      = false;

   private Button                           _loadBattleButton;
   private Button                           _saveBattleButton;
   private Button                           _pausePlayButton;
   private Button                           _turnButton;
   private Button                           _roundButton;
   private Button                           _phaseButton;
   private final Object                     _pausePlayControl = new Object();
   private final Semaphore                  _lock_pausePlayControl = new Semaphore("CombatServer_pausePalyControl",
                                                                                   CombatSemaphore.CLASS_COMBATSERVER_pausePlayControl);

   public final CharacterWidget _charWidget;
   public final CharacterFile   _charFile      = new CharacterFile("Character.data");
   private final CharInfoBlock   _charInfoBlock = new CharInfoBlock(null);
   private Button                           _hideViewFromLocalPlayersButton;


   public static final byte MAX_COMBATANTS_PER_TEAM = 15;
   public static final byte MAX_TEAMS               = (byte)(TEAM_NAMES.length);

   private final HashMap<String, CombatMap> _nameToArenaMap   = null;// new HashMap<String, CombatMap>();


   private      TabFolder _tabFolder;
   public final TabFolder _tabFolderMain;
   private      TabItem   _terrainTabItem;
   private TabItem                    _triggersTabItem;
   private TabItem                    _combatantsTabItem;
   private TabItem                    _messagesTabItem;

   private TerrainInterface           _terrainInterface;
   private TriggersInterface          _triggersInterface;
   private CombatMap _originalMap = null;
   private boolean _autoStart = false;

   public static final String _REMOTE_AI_NAME = "Remote Connection";
   public static final String _INACTIVE_AI_NAME = "Off";

   private static boolean USE_CACHED_DRAWING = true;
   private static boolean ALLOW_BACKUP = false;

   public CombatServer(List<String> args)
   {
      _isServer = true;
      _this = this;
      resetMessageBuffer();
      //String propertiesFileName = "PropertiesFileName.properties";
      //DiagParameters diagParams = new DiagParameters(propertiesFileName);
      //_diag = new Diagnostics(propertiesFileName, diagParams, true/*startNewFile*/);

      Display display;
      boolean useSleak = false;
      if (useSleak) {
         DeviceData data = new DeviceData();
         data.tracking = true;
         display = new Display(data);
         Sleak sleak = new Sleak();
         sleak.open();
      }
      else {
         display = new Display();
      }
      setShell(new Shell(display));
      getShell().setText("Art of Battle"); // the window's title

      _charWidget = new CharacterWidget(null, _charFile);

      getShell().setLayout(new FillLayout());
      getShell().addKeyListener(this);
      _tabFolderMain = new TabFolder(getShell(), SWT.NONE);

      TabItem item;
      // create a TabItem
      item = new TabItem( _tabFolderMain, SWT.NULL);
      item.setText( "Arena Map");
      // create a control
      Composite arenaMap = createComposite(_tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( arenaMap );

      // create the next tab
      item = new TabItem( _tabFolderMain, SWT.NULL);
      item.setText( "Characters");
      Composite characterData = createComposite(_tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( characterData );

      // create the next tab
      item = new TabItem( _tabFolderMain, SWT.NULL);
      item.setText( "Full Messages");
      Composite fullMessageData = createComposite(_tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( fullMessageData );

      // create the next tab
      item = new TabItem( _tabFolderMain, SWT.NULL);
      item.setText( "Rules");
      // add the control to the TabItem
      item.setControl(new RuleComposite(_tabFolderMain, 1, GridData.FILL_BOTH, new Configuration(),
                                        WINDOW_WIDTH, display.getSystemColor(SWT.COLOR_WHITE)));

      Composite mainGridBlock = new Composite(arenaMap, SWT.NONE);
      mainGridBlock.setLayout(new GridLayout(1, false));
      GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.minimumHeight = 350;
      //data.horizontalAlignment = SWT.BEGINNING;
      mainGridBlock.setLayoutData(data);

      characterData.setLayout(new GridLayout(1 /*columns*/,false/*sameWidth*/));

      buildTopBlock(mainGridBlock);
      SashForm mainSash = new SashForm(mainGridBlock, SWT.VERTICAL);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      mainSash.setLayoutData(data);
      mainSash.SASH_WIDTH = 4;
      mainSash.setBackground(display.getSystemColor(SWT.COLOR_DARK_GREEN));

      buildMiddleBlock(mainSash);
      buildBottomBlock(mainSash);
      mainSash.setWeights(new int[]{2, 1});
      {
         _fullMessages = new Browser(fullMessageData, SWT.NONE | SWT.BORDER);
         _fullMessages.setText("<br><br><br><br><br>");
         data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.minimumHeight = 700;
         data.minimumWidth  = WINDOW_WIDTH;
         _fullMessages.setLayoutData(data);
         _fullMessages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      }
      _arena = new Arena(this,  Short.parseShort(_arenaSizeXValue.getText().trim()),
                         (short)(Short.parseShort(_arenaSizeYValue.getText().trim()) * 2));
      if (_map != null) {
         _map.addListener(_arena);
      }
      readNameToArenaMapFromFile();

      String arenaName = null;
      String battleName = null;
      String port = null;
      int initialTab = 0;
      for (String arg : args) {
         if (arg.equalsIgnoreCase("noCachedDrawing")) {
            USE_CACHED_DRAWING = false;
         }
         else if (arg.equalsIgnoreCase("allowBackup")) {
            ALLOW_BACKUP = true;
         }
         else {
            StringTokenizer st = new StringTokenizer(arg, "=");
            if (st.countTokens() == 2) {
               String name = st.nextToken();
               String value = st.nextToken();
               if (name.equalsIgnoreCase("arena")) {
                  arenaName = value;
               }
               if (name.equalsIgnoreCase("battle")) {
                  battleName = value;
               }
               if (name.equalsIgnoreCase("port")) {
                  port = value;
               }
               if (name.equalsIgnoreCase("tab")) {
                  try {
                     initialTab = Integer.parseInt(value);
                  } catch (NumberFormatException e) {}
               }
               if (name.equalsIgnoreCase("seed")) {
                  setPseudoRandomNumberSeed(Integer.parseInt(value));
                  _usePseudoRandomNumbers.setSelection(true);
                  _pseudoRandomNumberSeedText.setEnabled(true);
               }
            }
         }
      }
      _tabFolderMain.setSelection(initialTab);

      if (_nameToArenaMap != null) {
         if (arenaName != null) {
            // Is this name the name of an arena in our data file?
            if (_nameToArenaMap.get(arenaName.toLowerCase()) == null) {
               // if not, ignore the parameter.
               arenaName = null;
            }
         }
         if (arenaName == null) {
            TreeSet<String> keys = new TreeSet<>(_nameToArenaMap.keySet());
            Iterator<String> iter = keys.iterator();
            if (iter.hasNext()) {
               arenaName = iter.next();
            }
         }
      }
      if (arenaName != null) {
         setMap(arenaName);
         if (_arenaName != null) {
            _arenaName.setText(arenaName);
         }
      }
      else {
         updateMap(_arena);
      }
      if ((battleName != null) && (battleName.length()>0)) {
         File battleFile = new File(battleName);
         _arena.serializeFromFile(battleFile);
      }

//      List<String> mapLocNames = new ArrayList<>();
//      for (byte team=0 ; team<_maxTeams ; team++) {
//         for (byte cur=0 ; cur<MAX_COMBATANTS_PER_TEAM ; cur++) {
//            mapLocNames.add(CombatMap.getLabel(team, cur));
//         }
//      }
      _charWidget.buildCharSheet(characterData);

      getShell().pack();
      getShell().open();

      _map.setZoomToFit();
      if (port != null) {
         Configuration._serverPort = Integer.parseInt(port);
         // If the port was passed in on the command line, open it up.
         _autoStart  = true;
      }
   }


   public void execute() {
      if (_autoStart) {
         openPort(true/*startup*/);
      }

      Display display = getShell().getDisplay();
      while (!getShell().isDisposed()) {
         if (!display.readAndDispatch()) {
            for (MapWidget3D widget : _widgets) {
               widget.redraw();
            }
            display.sleep ();
         }
      }
      display.dispose ();
   }

   private void cleanUpOnExit() {
      synchronized (_pausePlayControl) {
         _lock_pausePlayControl.check();
         _pausePlayControl.notifyAll();
      }
      if (_clientListener != null) {
         _clientListener.closePort();
         _clientListener = null;
      }
      if (_arena != null) {
         _arena.removeAllCombatants();
         _arena.terminateBattle();
      }
      if (_diag != null) {
         _diag.endDiagnostics();
      }
      _diag = null;
      System.out.println("end diagnostics called.");
   }


   @SuppressWarnings("deprecation")
   @Override
   protected void finalize() throws Throwable
   {
      try {
         if (_diag != null) {
            _diag.endDiagnostics();
            _diag = null;
            System.out.println("end diagnostics called in finalize!");
         }
      }
      finally {
         super.finalize();
      }
   }


   /**
    * @param mainGridBlock
    */
   private void buildTopBlock(Composite mainGridBlock)
   {
      Composite topGridBlock = new Composite(mainGridBlock, SWT.NONE);
      topGridBlock.setLayout(new GridLayout(5, true));
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.grabExcessVerticalSpace = false;
      data.grabExcessHorizontalSpace = true;
      topGridBlock.setLayoutData(data);
      buildArenaNameBlock(topGridBlock);
      buildPseudoRandomBlock(topGridBlock);
      buildArenaSizeBlock(topGridBlock);

      _hideViewFromLocalPlayersButton = new Button(topGridBlock, SWT.CHECK);
      _hideViewFromLocalPlayersButton.setText("Hide map from local players.");
      _hideViewFromLocalPlayersButton.setSelection(false);
      _hideViewFromLocalPlayersButton.addSelectionListener(this);
   }

   /**
    * @param mainGridBlock
    */
   private void buildBottomBlock(Composite mainGridBlock)
   {
      Composite bottomGridBlock = new Composite(mainGridBlock, SWT.NONE);
      FillLayout layout = new FillLayout();
      bottomGridBlock.setLayout(layout);
      _tabFolder = new TabFolder(bottomGridBlock, SWT.NONE);


      { // Make the bottom section resize with the main window:
         GridData gdata = new GridData(GridData.FILL_BOTH);
         gdata.horizontalSpan = 1;
         gdata.grabExcessVerticalSpace = true;
         gdata.grabExcessHorizontalSpace = true;
         bottomGridBlock.setLayoutData(gdata);
      }

      // create a TabItem
      _terrainTabItem = new TabItem( _tabFolder, SWT.NULL);
      _terrainTabItem.setText( "Terrain and Walls");
      // create a control
      Composite terrainComposite = createComposite(_tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      _terrainTabItem.setControl( terrainComposite );

      // create the next tab
      _triggersTabItem = new TabItem( _tabFolder, SWT.NULL);
      _triggersTabItem.setText( "Triggers");
      Composite triggersComposite = createComposite(_tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      _triggersTabItem.setControl( triggersComposite );

      // create the next tab
      _combatantsTabItem = new TabItem( _tabFolder, SWT.NULL);
      _combatantsTabItem.setText( "Combatants");
      Composite combatantsComposite = createComposite(_tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      _combatantsTabItem.setControl( combatantsComposite );

      // create the next tab
      _messagesTabItem = new TabItem( _tabFolder, SWT.NULL);
      _messagesTabItem.setText( "Messages");
      Composite messagesComposite = createComposite(_tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      _messagesTabItem.setControl( messagesComposite );

      int selectedTabIndex = 0;
      _tabFolder.setSelection(selectedTabIndex);
      _tabFolder.addSelectionListener(this);

      // When the server starts up, disable the pan feature of the map so you can drag a paintbrush
      // with terrain over multiple hexes at once.
      _map.allowPan(false);
      _map.setMode(IMapWidget.MapMode.NONE);

      TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
      if (_map != null) {
         CombatMap combatMap = _map.getCombatMap();
         if (combatMap != null) {
            if (selectedTab != _triggersTabItem) {
               combatMap.setSelectedTrigger(null);
            }
            else {
               combatMap.setSelectedTrigger(_triggersInterface.getCurrentlySelectedTrigger());
            }
         }
      }

      GridData data;
      GridLayout grid;
      _terrainInterface = new TerrainInterface();
      _triggersInterface = new TriggersInterface();
      if (_map instanceof MapWidget3D) {
         ((MapWidget3D) _map).addGLViewListener(_terrainInterface);
      }
      _terrainInterface.buildBlock(terrainComposite);
      _triggersInterface.buildBlock(triggersComposite);
      _triggersInterface.setMap(_map);
      _terrainInterface.setMap(_map);

      {
         Composite combatantButtonsBlock = new Composite(combatantsComposite, SWT.TRAIL);
         grid = new GridLayout(_maxTeams/*columns*/, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         combatantButtonsBlock.setLayout(grid);
         data = new GridData();
         data.horizontalSpan = 3;
         combatantButtonsBlock.setLayoutData(data);
         _combatantsButtons = new Button[_maxTeams][MAX_COMBATANTS_PER_TEAM];
         _combatantsAI      = new Combo[_maxTeams][MAX_COMBATANTS_PER_TEAM];
         _combatantsName    = new Combo[_maxTeams][MAX_COMBATANTS_PER_TEAM];
         List<String> charNames = _charWidget.getCharacterFile().getCharacterNames();
         charNames.add("Random...");
         List<String> aiNames = new ArrayList<>();
         aiNames.add(_INACTIVE_AI_NAME);
         aiNames.add(_REMOTE_AI_NAME);
         aiNames.add("Local");
         for (AI_Type aiType : AI_Type.values()) {
            aiNames.add("AI - " + aiType.name);
         }
         for (int team=0 ; team<_maxTeams ; team++) {
            Group teamGroup = createGroup(combatantButtonsBlock, TEAM_NAMES[team], 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
            for (int combatant=0 ; combatant<MAX_COMBATANTS_PER_TEAM ; combatant++) {
               _combatantsButtons[team][combatant] = new Button(teamGroup, SWT.PUSH);
               _combatantsAI[team][combatant] = createCombo(teamGroup, SWT.DROP_DOWN | SWT.READ_ONLY, 1/*hSpan*/, aiNames);
               _combatantsName[team][combatant] = createCombo(teamGroup, SWT.NONE, 1/*hSpan*/, charNames);
               _combatantsButtons[team][combatant].addListener(SWT.Paint, this);
               data = new GridData();
               data.minimumWidth = 35;
               data.horizontalAlignment = SWT.CENTER;
               data.grabExcessHorizontalSpace = true;
               _combatantsButtons[team][combatant].setLayoutData(data);
               _combatantsButtons[team][combatant].addSelectionListener(this);
               // enable only the first button.
               _combatantsButtons[team][combatant].setEnabled(true);
               _combatantsAI[team][combatant].setEnabled(false);
               _combatantsName[team][combatant].setEnabled(false);
               _combatantsAI[team][combatant].setText(aiNames.get(0)); // 'Off'

               _combatantsName[team][combatant].addSelectionListener(this);
               _combatantsName[team][combatant].addModifyListener(this);
               _combatantsAI[team][combatant].addSelectionListener(this);
               _combatantsAI[team][combatant].addModifyListener(this);
            }
         }
      }
      {
         _messages = new Browser(messagesComposite, SWT.NONE | SWT.BORDER);
         _messages.setText("<br><br><br><br><br>");
         data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.minimumHeight = 175;
         data.minimumWidth  = WINDOW_WIDTH;
         data.horizontalSpan = 3;
         _messages.setLayoutData(data);
         Display display = getShell().getDisplay();
         if (display != null) {
            _messages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
         }
      }
   }

   public void addNewCharacter(String characterName) {
      for (int team=0 ; team<_maxTeams ; team++) {
         for (int combatant=0 ; combatant<_combatantsName[team].length ; combatant++) {
            _combatantsName[team][combatant].add(characterName);
         }
      }
   }
   public void removeCharacter(String characterName) {
      for (int team=0 ; team<_maxTeams ; team++) {
         for (int combatant=0 ; combatant<_combatantsName[team].length ; combatant++) {
            _combatantsName[team][combatant].remove(characterName);
         }
      }
   }

   private static final short MAP_SIZE_HEIGHT = 300;
   private static final short MAP_SIZE_WIDTH  = 600;
   /**
    * @param mainGridBlock
    */
   private void buildMiddleBlock(Composite mainGridBlock)
   {
      Composite midGridBlock = new Composite(mainGridBlock, SWT.CENTER);
      GridLayout grid = new GridLayout(2, false);
      midGridBlock.setLayout(grid);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      //GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.horizontalAlignment = SWT.BEGINNING;
      data.grabExcessVerticalSpace = false;
      data.grabExcessHorizontalSpace = true;
      midGridBlock.setLayoutData(data);

      _uses3dMap = Configuration.use3DMap();

      if (_uses3dMap) {
         _map = new MapWidget3D(midGridBlock);
      }
      else {
         _map = new MapWidget2D(midGridBlock);
      }
      _map.addListener(this);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.minimumHeight = MAP_SIZE_HEIGHT;
      data.minimumWidth  = MAP_SIZE_WIDTH;

      data.horizontalAlignment = GridData.FILL;
      _map.setLayoutData(data);
      {
         Composite midRightBlock = new Composite(midGridBlock, SWT.CENTER);
         grid = new GridLayout(1, false);
         midRightBlock.setLayout(grid);
         data = new GridData(SWT.FILL, SWT.FILL, false/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.horizontalAlignment = SWT.BEGINNING;
         midRightBlock.setLayoutData(data);
         _map.addControlGroup(midRightBlock);
         {
            Composite midRightTopBlock = new Composite(midRightBlock, SWT.CENTER);
            grid = new GridLayout(2, false);
            midRightTopBlock.setLayout(grid);
            data = new GridData();
            data.horizontalAlignment = SWT.BEGINNING;
            midRightTopBlock.setLayoutData(data);
            {
               Composite midRightTopLeftBlock = new Composite(midRightTopBlock, SWT.CENTER);
               grid = new GridLayout(1, false);
               midRightTopLeftBlock.setLayout(grid);
               data = new GridData();
               data.horizontalAlignment = SWT.BEGINNING;
               midRightTopLeftBlock.setLayoutData(data);
               addStartButton(midRightTopLeftBlock);
               @SuppressWarnings("unused")
               Label dummy = new Label(midRightTopLeftBlock, SWT.LEFT);
               dummy = new Label(midRightTopLeftBlock, SWT.LEFT);
               _pausePlayButton = createButton(midRightTopLeftBlock, "Pause", 1/*hSpan*/, null/*fontData*/, this);
               _turnButton = createButton(midRightTopLeftBlock, "Turn++", 1/*hSpan*/, null/*fontData*/, this);
               _roundButton = createButton(midRightTopLeftBlock, "Round++", 1/*hSpan*/, null/*fontData*/, this);
               _phaseButton = createButton(midRightTopLeftBlock, "Phase++", 1/*hSpan*/, null/*fontData*/, this);
               resetPlayPauseControls();

               Group group = createGroup(midRightTopLeftBlock, "Battle", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
               _loadBattleButton = createButton(group, "Load", 1/*hSpan*/, null/*fontData*/, this);
               _saveBattleButton = createButton(group, "Save", 1/*hSpan*/, null/*fontData*/, this);
            }
            _charInfoBlock.buildBlock(midRightTopBlock);
         }
         {
            Composite midRightBottomBlock = new Composite(midRightBlock, SWT.CENTER);
            grid = new GridLayout(1, false);
            midRightBottomBlock.setLayout(grid);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalAlignment = SWT.BEGINNING;
            midRightBottomBlock.setLayoutData(data);

            AutoRunBlock autoRunBlock = new AutoRunBlock(this);
            autoRunBlock.buildBlock(midRightBottomBlock);
         }
      }
   }

   private void resetPlayPauseControls() {
      _pausePlayButton.setEnabled(true);
      _pausePlayButton.setText("Pause");
      _turnButton.setEnabled(false);
      _roundButton.setEnabled(false);
      _phaseButton.setEnabled(false);
   }

   /**
    * @param topGridBlock
    */
   private void buildArenaSizeBlock(Composite topGridBlock)
   {
      Composite block  = new Composite(topGridBlock, SWT.TRAIL);
      GridLayout grid = new GridLayout(5, false);
      block.setLayout(grid);
      new Label(block, SWT.LEFT).setText("Arena size:");

      _arenaSizeXValue = new Text(block, SWT.LEFT | SWT.BORDER);
      _arenaSizeXValue.setText("20");
      _arenaSizeXValue.setSize(30, 20);
      GridData data = new GridData();
      data.minimumWidth = 30;
      data.grabExcessHorizontalSpace = true;
      _arenaSizeXValue.setLayoutData(data);
      new Label(block, SWT.CENTER).setText("x");
      _arenaSizeYValue = new Text(block, SWT.LEFT | SWT.BORDER);
      _arenaSizeYValue.setText("14");
      _arenaSizeYValue.setSize(30, 20);
      data = new GridData();
      data.minimumWidth = 30;
      data.grabExcessHorizontalSpace = true;
      _arenaSizeYValue.setLayoutData(data);

      _arenaSizeXValue.addModifyListener(this);
      _arenaSizeYValue.addModifyListener(this);
   }

   /**
    * @param topGridBlock
    */
   private void buildArenaNameBlock(Composite topGridBlock)
   {
      Composite block = new Composite(topGridBlock, SWT.CENTER);
      GridLayout grid = new GridLayout(3, false);
      block.setLayout(grid);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalAlignment = SWT.CENTER;
      data.grabExcessHorizontalSpace = true;
      block.setLayoutData(data);
//      _arenaName = new Combo(block, SWT.NONE);
//      _arenaName.addModifyListener(this);
//
//      _saveButton = new Button(block, SWT.PUSH | SWT.BORDER);
//      _saveButton.setText(" Save ");
//      _saveButton.addSelectionListener(this);

      _newMapButton = new Button(block, SWT.PUSH);
      _newMapButton.setText("New Map");
      _newMapButton.addSelectionListener(this);

      _saveMapButton = new Button(block, SWT.PUSH);
      _saveMapButton.setText("Save Map");
      _saveMapButton.addSelectionListener(this);

      _openMapButton = new Button(block, SWT.PUSH);
      _openMapButton.setText("Load Map");
      _openMapButton.addSelectionListener(this);
    }

   /**
    * @param topGridBlock
    */
   private void buildPseudoRandomBlock(Composite topGridBlock)
   {
      Composite block   = new Composite(topGridBlock, SWT.LEFT);

      GridLayout grid = new GridLayout(2, false);
      block.setLayout(grid);
      GridData data = new GridData();
      data.horizontalAlignment = SWT.BEGINNING;
      block.setLayoutData(data);

      _usePseudoRandomNumbers = new Button(block, SWT.CHECK);
      _usePseudoRandomNumbers.setText("Use pseduo-random number seed:");
      _usePseudoRandomNumbers.setSelection(false);
      _usePseudoRandomNumbers.addSelectionListener(this);

      _pseudoRandomNumberSeedText = new Text(block, SWT.LEFT | SWT.BORDER);
      // over-write the Text field with a large empty string to keep it large for the initial pack.
      _pseudoRandomNumberSeedText.setText("       ");
      _pseudoRandomNumberSeedText.setEditable(false);
      _pseudoRandomNumberSeedText.setEnabled(false);
      _pseudoRandomNumberSeedText.addModifyListener(this);
   }

   private void addStartButton(Composite parent)
   {
      _openButton = new Button(parent, SWT.LEFT);
      setOpenButtonText(true/*start*/);
      _openButton.addSelectionListener(this);
   }
   private void setOpenButtonText(boolean start) {
      Color bgColor;
      if (start) {
         _openButton.setText("Start Battle");
         bgColor = new Color(_openButton.getDisplay(), 128, 255, 128);
      } else {
         _openButton.setText("Stop Battle");
         bgColor = new Color(_openButton.getDisplay(), 255, 128, 128);
      }
      _openButton.setBackground(bgColor);
      bgColor.dispose();
   }

   public void setMap(String arenaName)
   {
      CombatMap map;
      if (_nameToArenaMap != null) {
         map = _nameToArenaMap.get(arenaName.toLowerCase());
      }
      else {
         map = new CombatMap();
         File mapFile = new File("arenas" + File.separator + arenaName + ".xml");
         if (!mapFile.exists()) {
            mapFile = new File(arenaName + ".xml");
            if (!mapFile.exists()) {
               mapFile = new File(arenaName);
               if (!mapFile.exists()) {
                  mapFile = new File("arenas" + File.separator + arenaName);
               }
            }
         }
         if (mapFile.exists() && mapFile.canRead()) {
            map.serializeFromFile(mapFile);
         }
      }
      setMap(map, true/*clearCombatants*/);
   }
   public void setMap(CombatMap map, boolean clearCombatants) {
      if (_map != null) {
         _map.endHexSelection();
      }
      if (map != null) {
         map = map.clone();
         _changingMap = true;
         _arena.setCombatMap(map, clearCombatants);
         _map.updateMap(map, (byte)-1/*selfID*/, (byte)-1/*selfTeam*/, null/*availableLocs*/, (byte)-1/*targetID*/);
         _triggersInterface.setMap(map);
         _originalMap = map.clone();
         _arenaSizeXValue.setText(String.valueOf(map.getSizeX()));
         _arenaSizeYValue.setText(String.valueOf(map.getSizeY()/2));
         _hideViewFromLocalPlayersButton.setSelection(map.isHideViewFromLocalPlayers());
         for (byte team=0 ; team<_maxTeams ; team++) {
            for (byte curCombatantIndex=0 ; curCombatantIndex<map.getStockAIName(team).length ; curCombatantIndex++) {
               boolean locExists = (map.getStartingLocation(team, curCombatantIndex) != null);
               String aiName = map.getStockAIName(team)[curCombatantIndex];
               String combatantName = map.getStockCharacters(team)[curCombatantIndex];
               if ((aiName == null) || _REMOTE_AI_NAME.equals(aiName) || _INACTIVE_AI_NAME.equals(aiName)) {
                  aiName = locExists ? _REMOTE_AI_NAME : _INACTIVE_AI_NAME;
               }
               else {
                  AI_Type aiType = AI_Type.getByString(aiName);
                  if (aiType != null) {
                     aiName = "AI - " + aiType.name;
                  }
               }
               if (combatantName == null) {
                  combatantName = "";
               }
               if (_combatantsAI != null) {
                  _combatantsAI[team][curCombatantIndex].setText(aiName);
                  _combatantsAI[team][curCombatantIndex].setEnabled(locExists);
               }
               if (_combatantsName != null) {
                  _combatantsName[team][curCombatantIndex].setText(combatantName);
                  boolean enableNameEdit = locExists && !_INACTIVE_AI_NAME.equals(aiName);
                  _combatantsName[team][curCombatantIndex].setEnabled(enableNameEdit);
               }
            }
         }
         _changingMap = false;
         resetPlayPauseControls();
      }
      updateMap(_arena);
   }
   @Override
   public void handleEvent(Event event)
   {
      int sizePerHex = 20;
      int offsetX = 6;
      int offsetY = 2;
      if (event.type == SWT.Paint) {
         for (byte team=0 ; team<_combatantsButtons.length ; team++) {
            for (byte cur=0 ; cur<_combatantsButtons[team].length ; cur++) {
               if (event.widget == _combatantsButtons[team][cur]) {
                  ArenaLocation loc = new ArenaLocation((short)0,(short)0);
                  if (_combatantsButtons[team][cur].isEnabled()) {
                     loc.setTerrain(TerrainType.GRASS);
                     loc.setLabel(CombatMap.getLabel(team, cur));
                  }
                  else {
                     loc.setTerrain(TerrainType.GRAVEL);
                     loc.setLabel(" -- ");
                  }
                  if ((_currentTeam == team) && (_currentCombatant == cur)) {
                     event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
                     event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_GRAY));
                     event.gc.fillRectangle(2, 2, event.width-4, event.height-4);
                  }

                  //loc;
                  MapWidget2D.drawHex(loc, event.gc, event.display, sizePerHex, offsetX, offsetY);
                  int[] bounds = MapWidget2D.getHexDimensions((short)0/*column*/, (short)0/*row*/, sizePerHex, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
                  int x = bounds[MapWidget2D.X_SMALLEST] + 5;
                  int y = bounds[MapWidget2D.Y_SMALLEST] + 3;
                  if (_combatantsButtons[team][cur].isEnabled()) {
                     event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_RED));
                  }
                  else {
                     event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_GRAY));
                  }
                  event.gc.drawText(loc.getLabel(), x + offsetX, y + offsetY, true/*transparent background*/);
                  return;
               }
            }
         }
      }
   }

   public void refreshSaveButton() {
      if ((_saveButton != null) && (_arenaName != null) && (_nameToArenaMap != null)) {
         CombatMap map = _nameToArenaMap.get(_arenaName.getText().toLowerCase());
         if (map == null) {
            _saveButton.setText("  Add ");
         }
         else if (_arena.getCombatMap().equals(map)) {
            _saveButton.setText("Delete");
         }
         else {
            _saveButton.setText(" Save ");
         }
      }
      if (_saveMapButton != null) {
         boolean enabled = (_originalMap == null) || (!_originalMap.equals(_arena.getCombatMap()));
         _saveMapButton.setEnabled(enabled );
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e)
   {
      try {
         if (e.widget == _openButton) {
            if (_openButton.getText().equals("Start Battle")) {
//               Shell shell = new Shell(_shell, SWT.DIALOG_TRIM | SWT.MODELESS);
//               shell.setText("test");
//               shell.setLayout(new GridLayout(2/*numColumns*/, false/*makeColumnsEqualWidth*/));
//               //shell.addFocusListener(this);
               openPort(true/*startup*/);
            } else { // _openButton.getText().equals("Stop Battle")
               onPlay();
               closePort();
            }
         }
         else if (e.widget == _usePseudoRandomNumbers) {
            _pseudoRandomNumberSeedText.setEditable(_usePseudoRandomNumbers.getSelection());
            _pseudoRandomNumberSeedText.setEnabled(_usePseudoRandomNumbers.getSelection());
            _pseudoRandomNumberSeedText.setText(String.valueOf(_pseudoRandomNumberSeed));
         }
         else if (e.widget == _saveMapButton) {
            writeArenaMapToFile(_arena.getCombatMap(), true/*overwriteExistingFile*/);
            _originalMap = _arena.getCombatMap().clone();
            refreshSaveButton();
         }
         else if (e.widget == _openMapButton) {
            FileDialog dialog = new FileDialog(getShell());
            dialog.setFilterExtensions(new String[] {"*.xml"});
            dialog.setFilterNames(new String[] {"Arena Files (*.xml)"});
            dialog.setFilterPath("Arenas");

            // Disable the current terrain and wall settings, or the mouse up
            // event will fire to the terrain interface after the load completes,
            // and the map will be edited immediately:
            _terrainInterface.disableCurrentEdits();

            String fileName = dialog.open();
            if ((fileName != null) && (fileName.length() > 0)) {
               File sourceFile = new File("Arenas" + File.separator + fileName);
               if (!sourceFile.exists()) {
                  sourceFile = new File(fileName);
               }
               if (sourceFile.exists()) {
                  if (sourceFile.canRead()) {
                     _currentMapFileName = sourceFile.getAbsolutePath();
                     CombatMap map = new CombatMap();
                     map.serializeFromFile(sourceFile);
                     setMap(map, true/*clearCombatants*/);
                     _map.setZoomToFit();
                     refreshSaveButton();
                  }
               }
            }
         }
         else if (e.widget == _newMapButton) {
            NewMapDialog dialog = new NewMapDialog(getShell());
            dialog.open();
            if (!dialog.isCanceled()) {
               CombatMap map = new CombatMap(dialog.getSizeX(), (short) (dialog.getSizeY()*2), null/*diag*/);
               map.setName(dialog.getName());
               _originalMap = map.clone();
               setMap(map, true/*clearCombatants*/);
               _currentMapFileName = null; // make sure we don't overwrite the current file
               refreshSaveButton();
            }
         }
         else if ((e.widget == _saveButton) && (_saveButton != null)) {
            if (_nameToArenaMap != null) {
               if (_saveButton.getText().equals(" Save ")) {
                  _saveButton.setText("Delete");
                  _nameToArenaMap.put(_arenaName.getText().toLowerCase(), _arena.getCombatMap());
               } else if (_saveButton.getText().equals("Delete")) {
                  _saveButton.setText("  Add ");
                  _nameToArenaMap.remove(_arenaName.getText().toLowerCase());
               } else if (_saveButton.getText().equals("  Add ")) {
                  _saveButton.setText("Delete");
                  _nameToArenaMap.put(_arenaName.getText().toLowerCase(), _arena.getCombatMap());
                  _arenaName.add(_arenaName.getText());
               }
            }
            writeNamedArenaMapToFile(_arenaName.getText(), true/*overwriteExistingFile*/);
            refreshSaveButton();
         }
         else if ((e.widget == _loadBattleButton) || (e.widget == _saveBattleButton)) {
            boolean save = (e.widget == _saveBattleButton);
            FileDialog fileDlg = new FileDialog(_loadBattleButton.getShell(), save ? SWT.SAVE : 0);
            fileDlg.setFilterExtensions(new String[] {"*.btl"});
            fileDlg.setFilterNames(new String[] {"battle files (*.btl)"});
            fileDlg.setText("Select battle file to " + (save ? "save battle to" : "load battle from"));
            String filename = fileDlg.open();
            if ((filename != null) && (filename.length()>0)) {
               File battleFile = new File(filename);
               if (save) {
                  _arena.serializeToFile(battleFile);
               }
               else {
                  _arena.serializeFromFile(battleFile);
                  setMap(_arena.getCombatMap(), false/*clearCombatants*/);
                  setOpenButtonText(false/*start*/);
                  // Open the Messages tab:
                  this._tabFolder.setSelection(3);
                  this._map.allowPan(true);
               }
            }
         }
         else if (e.widget == _pausePlayButton) {
            if (_pausePlayButton.getText().equals("Pause")) {
               _pausePlayButton.setEnabled(false);
               if (_arena != null) {
                  _arena.onPause();
               }
            }
            else {
               onPlay();
            }
         }
         else if ((e.widget == _turnButton) || (e.widget == _roundButton) || (e.widget == _phaseButton)) {
            resetPlayPauseControls();
            if (_arena != null) {
               if (e.widget == _turnButton) {
                  _arena.onTurnAdvance();
               }
               if (e.widget == _roundButton) {
                  _arena.onRoundAdvance();
               }
               if (e.widget == _phaseButton) {
                  _arena.onPhaseAdvance();
               }
            }
            synchronized (_pausePlayControl ) {
               _lock_pausePlayControl.check();
               _pausePlayControl.notifyAll();
            }
         }
         else if (e.widget == _tabFolder){
            int selectedTabIndex = _tabFolder.getSelectionIndex();
            TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
            boolean isDraggable = (selectedTab != _terrainTabItem) || (_terrainInterface.allowPan());
            _map.allowPan(isDraggable);
            _map.setMode(isDraggable ? IMapWidget.MapMode.DRAG :
                                       IMapWidget.MapMode.NONE);

            if (selectedTab != _triggersTabItem) {
               _map.getCombatMap().setSelectedTrigger(null);
               _map.redraw();
            }
            else {
               ArenaTrigger trigger = _triggersInterface.getCurrentlySelectedTrigger();
               _map.getCombatMap().setSelectedTrigger(trigger);
               if (trigger != null) {
                  _map.redraw();
               }
            }
         }
         else if (e.widget == _hideViewFromLocalPlayersButton) {
            _map.setHideViewFromLocalPlayers(_hideViewFromLocalPlayersButton.getSelection());
            refreshSaveButton();
         }
         else {
            for (byte team=0 ; team<_combatantsButtons.length ; team++) {
               for (byte cur=0 ; cur<_combatantsButtons[team].length ; cur++) {
                  byte prevCurrentTeam = _currentTeam;
                  byte prevCurrentCombatant = _currentCombatant;
                  if (e.widget == _combatantsButtons[team][cur]) {
                     ArenaLocation oldStartLoc = _arena.getCombatMap().clearStartingLocation(team, cur);
                     _currentTeam = team;
                     _currentCombatant = cur;
                     _combatantsAI[team][cur].setEnabled(false);
                     _combatantsName[team][cur].setEnabled(false);
                     _combatantsButtons[team][cur].redraw();
                     if ((_map != null) && (_map instanceof MapWidget2D)){
                        if (oldStartLoc != null) {
                           List<ArenaCoordinates> locs = new ArrayList<>();
                           locs.add(oldStartLoc);
                           ((MapWidget2D)_map).redraw(locs);
                        }
                     }
                     refreshSaveButton();
                  }
                  else if (e.widget == _combatantsAI[team][cur]) {
                     changeAI(team, cur);
                  }
                  else if (e.widget == _combatantsName[team][cur]) {
                     changeName(team, cur, true/*checkForRandom*/);
                     //refreshSaveButton();
                  }
                  else {
                     continue;
                  }
                  if ((prevCurrentTeam != -1) && (prevCurrentCombatant != -1)) {
                     _combatantsButtons[prevCurrentTeam][prevCurrentCombatant].redraw();
                  }
                  return;
               }
            }
         }
      }
      catch (Throwable ex) {
         ex.printStackTrace();
      }
   }

   private void changeName(byte team, byte cur, boolean checkForRandom)
   {
      String newName = _combatantsName[team][cur].getText();
      if (checkForRandom) {
         if (newName.equals("Random...")) {
            GenerateCharacterDialog dialog = new GenerateCharacterDialog(getShell(), null);
            int points = dialog.open();
            String race = dialog.getRace();
            String equipment = dialog.getEquipment();
            newName = "? " + race + " " + points + " " + equipment;
            _combatantsName[team][cur].setText(newName);
         }
      }
      // new AI player selected
      if (_map != null) {
         CombatMap combatMap = _map.getCombatMap();
         if (combatMap != null) {
            Combo AICombo = _combatantsAI[team][cur];
            if (AICombo != null) {
               combatMap.setStockCharacter(newName, AICombo.getText(), team, cur);
            }
         }
      }
      refreshSaveButton();
   }

   private void changeAI(byte team, byte cur) {
      // new AI engine selected

      String newAI = _combatantsAI[team][cur].getText();
      if (newAI.startsWith("AI - ")) {
         newAI = newAI.replace("AI - ", "");
      }
      Combo nameCombo = _combatantsName[team][cur];
      if (nameCombo != null) {
         if (_INACTIVE_AI_NAME.equals(newAI)) {
            nameCombo.setText("");
            nameCombo.setEnabled(false);
         }
         else {
            nameCombo.setEnabled(true);
         }
         if (_map != null) {
            CombatMap combatMap = _map.getCombatMap();
            if (combatMap != null) {
               combatMap.setStockCharacter(nameCombo.getText(), newAI, team, cur);
            }
         }
      }
      refreshSaveButton();
   }
   public void onPlay() {
      resetPlayPauseControls();
      if (_arena != null) {
         _arena.onPlay();
      }
      synchronized (_pausePlayControl ) {
         _lock_pausePlayControl.check();
         _pausePlayControl.notifyAll();
      }
   }
   public void onPause() {
      _pausePlayButton.setEnabled(true);
      _pausePlayButton.setText("Play ");
      _turnButton.setEnabled(true);
      _roundButton.setEnabled(true);
      _phaseButton.setEnabled(true);
   }
   public void waitForPlay(List<Object> waitingObjects) {
      try {
         if (!getShell().isDisposed()) {
            Display display = getShell().getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(this::onPause);
            }
         }
         synchronized (_pausePlayControl) {
            _lock_pausePlayControl.check();
            waitingObjects.add(_pausePlayControl);
            _pausePlayControl.wait();
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      finally {
         waitingObjects.remove(_pausePlayControl);
      }
   }

   public void onAutoRun(AutoRunBlock autoRunBlock) {
      openPort(false/*startup*/);
      _arena.onAutoRun(autoRunBlock);
   }

   private void openPort(boolean startup)
   {
      resetMessageBuffer();
      TabFolder bottomTabFolder = _messagesTabItem.getParent();
      bottomTabFolder.setSelection(bottomTabFolder.indexOf(_messagesTabItem));
      _terrainTabItem.getControl().setVisible(false);
      _triggersTabItem.getControl().setVisible(false);
      _combatantsTabItem.getControl().setVisible(false);

      _arenaSizeXValue.setEnabled(false);
      _arenaSizeYValue.setEnabled(false);
      setOpenButtonText(false/*start*/);

      if (startup) {
         _clientListener = new ClientListener(this);
         _clientListener.start();
         CombatServer.resetPseudoRandomNumberGenerator();
         _arena.addStockCombatants();
      }
   }
   private void closePort()
   {
      resetPlayPauseControls();

      TabFolder bottomTabFolder = _messagesTabItem.getParent();
      bottomTabFolder.setSelection(bottomTabFolder.indexOf(_terrainTabItem));
      _terrainTabItem.getControl().setVisible(true);
      _triggersTabItem.getControl().setVisible(true);
      _combatantsTabItem.getControl().setVisible(true);

      if (_clientListener != null) {
         _clientListener.closePort();
         _clientListener = null;
      }
      _arenaSizeXValue.setEnabled(true);
      _arenaSizeYValue.setEnabled(true);
      setOpenButtonText(true/*start*/);

      _arena.removeAllCombatants();
      _arena.terminateBattle();
      _arena.disconnectAllClients();
      if (_map != null) {
         _map.setZoomToFit();
         CombatMap combatMap = _map.getCombatMap();
         combatMap.removeAllCombatants();
         // reload the map, so a new battle can begin
         setMap(combatMap.getName());
         int selectedTabIndex = _tabFolder.getSelectionIndex();
         TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
         if (selectedTab != _triggersTabItem) {
            combatMap.setSelectedTrigger(null);
         }
         else {
            combatMap.setSelectedTrigger(_triggersInterface.getCurrentlySelectedTrigger());
         }
         _map.redraw();
      }
      for (MessageDialog dialog : MessageDialog._activeMessages) {
         // TODO: close this dialog
         if (!dialog._shell.isDisposed()) {
            dialog._shell.close();
         }
      }
      MessageDialog._activeMessages.clear();
      MessageDialog._topMessage = null;
      for (RequestUserInput input : Arena._activeRequestUserInputs) {
         input._shell.close();
      }
      Arena._activeRequestUserInputs.clear();
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }

   public void resetMessageBuffer() {
      if (_messages != null) {
         if (!getShell().isDisposed()) {
            Display display = getShell().getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(() -> {
                  if ((_messages != null) && (!_messages.isDisposed())) {
                     _messages.setText("<body></body>");
                  }
                  if ((_fullMessages != null) && (!_fullMessages.isDisposed())) {
                     _fullMessages.setText("<body></body>");
                  }
                  synchronized (_pendingMessage) {
                     _lock_pendingMessage.check();
                     _pendingMessage.setLength(0);
                  }
               });
            }
         }
      }
   }
   final         Semaphore     _lock_pendingMessage = new Semaphore("CombatServer_pendingMessage",
                                                                    CombatSemaphore.CLASS_COMBATSERVER_pendingMessage);
   private final StringBuilder _pendingMessage      = new StringBuilder();
//   List<String> audit = new ArrayList<>();
   public void appendMessage(String message)
   {
      if (message == null)
       {
         return;
//      audit.add(message);
      }

      final StringBuilder fullMsg = new StringBuilder();
//      fullMsg.append("<font color=\"blue\">").append(audit.size()).append("</font>");

      fullMsg.append(message);
      if (!message.endsWith(">")) {
         fullMsg.append("<br>");
      }

      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      synchronized (_pendingMessage) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_pendingMessage)) {
            boolean alreadyWaiting = (_pendingMessage.length() > 0);
            _pendingMessage.append(fullMsg);
            if (alreadyWaiting) {
//               audit.set(audit.size()-1, "pending:" + audit.get(audit.size()-1));
               return;
            }
         }
      }
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               String msg;
               synchronized (_pendingMessage) {
                  _lock_pendingMessage.check();
                  msg = _pendingMessage.toString();
                  _pendingMessage.setLength(0);
               }
               // remove all CR-LF because they terminate the javascript execution for the insert
               msg = msg.replace("\n", "");
               // escape any single quote character, since we are putting this inside a single quote
               msg = msg.replace("'", "\\'");
               StringBuilder sb = new StringBuilder();
               sb.append("document.body.insertAdjacentHTML('beforeEnd', '");
               sb.append(msg);
               sb.append("');window.scrollTo(0,document.body.scrollHeight);"); // scroll to the bottom of the window
               if ((_messages != null) && (!_messages.isDisposed())) {
                  _messages.execute(sb.toString());
                  _messages.redraw();
               }
               if ((_fullMessages != null) && (!_fullMessages.isDisposed())) {
                  _fullMessages.execute(sb.toString());
                  _fullMessages.redraw();
               }
           });
         }
      }
   }

   public Arena getArena() { return _arena;  }

   public void updateMap(final Arena arena) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               if (_map != null) {
                  if (_map.updateMap(arena.getCombatMap(), -1/*selfID*/, (byte)-1/*selfTeam*/, null/*availableLocs*/, -1/*targetID*/)) {
                     _map.setZoomToFit();
                  }
                  int selectedTabIndex = _tabFolder.getSelectionIndex();
                  TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
                  CombatMap combatMap = _map.getCombatMap();
                  if (combatMap != null) {
                     if (selectedTab != _triggersTabItem) {
                        combatMap.setSelectedTrigger(null);
                     }
                     else {
                        combatMap.setSelectedTrigger(_triggersInterface.getCurrentlySelectedTrigger());
                     }
                  }
               }
               refreshSaveButton();
            });
         }
      }
   }

   @Override
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      List<ArenaCoordinates> locationsToRedraw = new ArrayList<>();
      int selectedTabIndex = _tabFolder.getSelectionIndex();
      TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
      boolean redraw = false;
      if (selectedTab == _terrainTabItem) {
         _terrainInterface.onMouseDown(loc, event, angleFromCenter, normalizedDistFromCenter, _map, locationsToRedraw);
         redraw = true;
      }
      else if (selectedTab == _triggersTabItem) {
         _triggersInterface.onMouseDown(loc, event, angleFromCenter, normalizedDistFromCenter, _map, locationsToRedraw);
         redraw = true;
      }
      if (redraw && (_map != null) && (_map instanceof MapWidget2D)) {
         if (locationsToRedraw.isEmpty()) {
            _map.redraw();
         }
         else {
            MapWidget2D map = (MapWidget2D) _map;
            map.redraw(locationsToRedraw);
         }
      }

      // On every mouse click, the map may become modified
      refreshSaveButton();
   }
   @Override
   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }
   @Override
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      //if (loc != null) {

         List<ArenaCoordinates> locationsToRedraw = new ArrayList<>();

         int selectedTabIndex = _tabFolder.getSelectionIndex();
         TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
         boolean redraw = false;
         if (selectedTab == _terrainTabItem) {
            _terrainInterface.onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter, _map, locationsToRedraw);
            redraw = true;
         }
         else if (selectedTab == _triggersTabItem) {
            _triggersInterface.onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter, _map.getCombatMap(),
                                         locationsToRedraw);
            redraw = true;
         }
         else if (selectedTab == _combatantsTabItem) {
            if ((_currentTeam > -1) && (_currentCombatant != -1)) {
               ArenaLocation oldLoc = _arena.getCombatMap().getStartingLocation(_currentTeam, _currentCombatant);
               if (oldLoc != null) {
                  locationsToRedraw.add(oldLoc);
               }
               _arena.getCombatMap().setStartingLocation(_currentTeam, _currentCombatant, loc);
               _combatantsAI[_currentTeam][_currentCombatant].setEnabled(true);
               if (_INACTIVE_AI_NAME.equals(_combatantsAI[_currentTeam][_currentCombatant].getText())) {
                  _combatantsAI[_currentTeam][_currentCombatant].setText(_REMOTE_AI_NAME);
               }
               else if (!_REMOTE_AI_NAME.equals(_combatantsAI[_currentTeam][_currentCombatant].getText())) {
                  _combatantsName[_currentTeam][_currentCombatant].setEnabled(true);
               }
               redraw = true;
               locationsToRedraw.add(loc);
            }
         }
         if (redraw && (_map != null) && (_map instanceof MapWidget2D)) {
            if (locationsToRedraw.isEmpty()) {
               _map.redraw();
            }
            else {
               MapWidget2D map = (MapWidget2D) _map;
               map.redraw(locationsToRedraw);
            }
         }

         // On every mouse click, the map may become modified
         refreshSaveButton();
//      }
   }

   @Override
   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      int selectedTabIndex = _tabFolder.getSelectionIndex();
      TabItem selectedTab = _tabFolder.getItem(selectedTabIndex);
      if (selectedTab == _terrainTabItem) {
         _terrainInterface.onMouseDrag(loc, event, angleFromCenter, normalizedDistFromCenter, _map);
      }
      else {
         onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter);
      }
   }
   public void readNameToArenaMapFromFile() {
      if (_nameToArenaMap == null) {
         return;
      }
      File sourceFile = new File("arena.data");
      TreeSet<String> arenaNames = new TreeSet<>((o1, o2) -> o1.toLowerCase().compareTo(o2.toLowerCase()));
      if (sourceFile.exists() && sourceFile.canRead()) {
         try (FileReader fileReader = new FileReader(sourceFile);
              BufferedReader input = new BufferedReader(fileReader)) {
            String inputLine;
            while ((inputLine = input.readLine()) != null) {
               CombatMap newCombatMap = new CombatMap();
               if (newCombatMap.serializeFromString(inputLine)) {
                  if (_nameToArenaMap != null) {
                     _nameToArenaMap.put(newCombatMap.getName().toLowerCase(), newCombatMap);
                  }
                  arenaNames.add(newCombatMap.getName());
               }
               writeNamedArenaMapToFile(newCombatMap.getName(), false/*overwriteExistingFiles*/);
            }
            sourceFile.delete();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      else {
         File sourceDir = new File("Arenas");
         if (sourceDir.exists() && sourceDir.isDirectory()) {
            File[] mapFiles = sourceDir.listFiles();
            for (File mapFile : mapFiles) {
               CombatMap newMap = new CombatMap();
               if (newMap.serializeFromFile(mapFile)) {
                  if (_nameToArenaMap != null) {
                     _nameToArenaMap.put(newMap.getName().toLowerCase(), newMap);
                  }
                  arenaNames.add(newMap.getName());
               }
            }
         }
      }
      if (_arenaName != null) {
         for (String name : arenaNames) {
            _arenaName.add(name);
         }
      }
   }

   public void writeNamedArenaMapToFile(String arenaName, boolean overwriteExistingFile) {
      if (_nameToArenaMap != null) {
         CombatMap map = _nameToArenaMap.get(arenaName.toLowerCase());
         writeArenaMapToFile(map, overwriteExistingFile);
      }
   }

   String _currentMapFileName = null;
   public void writeArenaMapToFile(CombatMap map, boolean overwriteExistingFile) {
      File sourceDir = new File("Arenas");
      if (sourceDir.exists() && sourceDir.isFile()) {
         sourceDir.delete();
      }
      if (!sourceDir.exists()) {
         sourceDir.mkdirs();
      }
      File sourceFile;
      if (_currentMapFileName == null) {
         sourceFile = new File("Arenas" + File.separator + map.getName().toLowerCase() + ".xml");
      }
      else {
         sourceFile = new File(_currentMapFileName);
      }

      try {
         if (sourceFile.exists()) {
            if (!overwriteExistingFile) {
               return;
            }
            sourceFile.delete();
         }

         sourceFile.createNewFile();
         if (sourceFile.exists() && sourceFile.canWrite()) {
            map.serializeToFile(sourceFile);
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }

   public static boolean _inModify = false;
   @Override
   public void modifyText(ModifyEvent e) {
      if (!_inModify) {
         _inModify = true;
         try {
            if (e.widget == _pseudoRandomNumberSeedText) {
               String value = _pseudoRandomNumberSeedText.getText();
               StringBuilder validatedValue = new StringBuilder();
               for (int i=0 ; i<value.length() ; i++) {
                  if (java.lang.Character.isDigit(value.charAt(i))) {
                     validatedValue.append(value.charAt(i));
                  }
               }
               int val = 0;
               if (validatedValue.length() > 0) {
                  val = Integer.parseInt(validatedValue.toString());
               }
               setPseudoRandomNumberSeed(val);
            }
            else if ((e.widget == _arenaSizeXValue) || (e.widget == _arenaSizeYValue)) {
               if (!_changingMap) {
                  _arena.setSize(Short.parseShort(_arenaSizeXValue.getText()),
                                 (short)(Short.parseShort(_arenaSizeYValue.getText())*2));
                  updateMap(_arena);
                  redrawMap();
               }
            }
            else if (e.widget == _arenaName) {
               if (_nameToArenaMap != null) {
                  CombatMap newCombatMap = _nameToArenaMap.get(_arenaName.getText().toLowerCase());
                  if (newCombatMap != null) {
                     setMap(_arenaName.getText());
                     _saveButton.setText("Delete");
                  }
                  else {
                     _saveButton.setText("  Add ");
                     _arena.setName(_arenaName.getText());
                  }
               }
            }
            else {
               for (byte team=0 ; (team<_combatantsButtons.length) ; team++) {
                  for (byte cur=0 ; (cur<_combatantsButtons[team].length) ; cur++) {
                     if (e.widget == _combatantsAI[team][cur]) {
                         changeAI(team, cur);
                         return;
                     }
                     if (e.widget == _combatantsName[team][cur]) {
                        changeName(team, cur, false/*checkForRandom*/);
                        return;
                     }
                  }
               }
            }
         }
         finally {
            _inModify = false;
         }
      }
   }
   public void redrawMap() {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               if (_map != null) {
                  _map.redraw();
               }
            });
         }
      }
   }
   public void redrawMap(final Collection<ArenaCoordinates> locationsToRedraw) {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               if (_map != null) {
                  if (_map instanceof MapWidget2D) {
                     ((MapWidget2D)_map).redraw(locationsToRedraw);
                  }
                  else if (_map instanceof MapWidget3D) {
                     _map.redraw();
                  }
               }
            });
         }
      }
   }

   public void updateCombatants(final List<Character> combatants)
   {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> _charInfoBlock.updateCombatants(combatants));
         }
      }
   }


   public static int    _pseudoRandomNumberSeed = 123;
   public static int    _pseudoRandomNumberUseCount = 0;
   public static Random _pseudoRandom = null;
   public static double random() {
//      if (!_isServer)
//         Rules.debugBreak();

      if (_pseudoRandom == null) {
         _pseudoRandom = new Random(_pseudoRandomNumberSeed);
         for (int i=0 ; i <50 ; i++) {
            _pseudoRandom.nextDouble();
         }

         _pseudoRandomNumberUseCount = 0;
         CharacterGenerator.NAMES_LIST_MALE.clear();
         CharacterGenerator.NAMES_LIST_FEMALE.clear();
      }
      _pseudoRandomNumberUseCount++;
      return _pseudoRandom.nextDouble();
   }

   public void onNewBattle() {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            final Object synchObject = 0;
            display.asyncExec(() -> {
               if (_usePseudoRandomNumbers.getSelection()) {
                  if (!_pseudoRandomNumberSeedText.getText().equals(String.valueOf(_pseudoRandomNumberSeed))) {
                     _pseudoRandomNumberSeedText.setText(String.valueOf(_pseudoRandomNumberSeed));
                  }
               }
               else {
                  if (!_arena._characterGenerated) {
                     generateNewPseudoRandomNumberSeed();
                  }
               }
               // Wait for a 1/4 second so the other thread will have a chance to wait for the synchObject lock.
               try {
                  Thread.sleep(250);
               } catch (InterruptedException e) {
               }
               synchronized(synchObject) {
                  synchObject.notifyAll();
               }
            });
            try {
               synchronized(synchObject) {
                  synchObject.wait();
               }
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      }
      synchronized (_pausePlayControl) {
         _lock_pausePlayControl.check();
      }
   }
   public static final int MAX_PSEUDO_RANDOM_VALUE = 1000;
   public static void generateNewPseudoRandomNumberSeed() {
      setPseudoRandomNumberSeed((int) (System.currentTimeMillis() / 100));
   }
   public static void setPseudoRandomNumberSeed(int newValue)
   {
      int clippedValue = (newValue % MAX_PSEUDO_RANDOM_VALUE);
      // For every new battle, always create a new Random object, so if you run the same seed twice,
      // it doesn't just continue the same sequences.
      _pseudoRandom = null;
      if (_pseudoRandomNumberSeed == clippedValue) {
         return;
      }
      _pseudoRandomNumberSeed = clippedValue;
      if (!_inModify) {
         if (_this != null) {
            _this._pseudoRandomNumberSeedText.setText(String.valueOf(_pseudoRandomNumberSeed));
         }
      }
   }
   public static int getPseudoRandomNumberSeed() {
      return _pseudoRandomNumberSeed;
   }
   public static int getPseudoRandomNumberUseCount() {
      return _pseudoRandomNumberUseCount;
   }

   public static void resetPseudoRandomNumberGenerator() {
      _pseudoRandom = null;
      CharacterGenerator.NAMES_LIST_MALE.clear();
      CharacterGenerator.NAMES_LIST_FEMALE.clear();
   }
   public static boolean usesCachedDrawing() {
      return USE_CACHED_DRAWING ;
   }
   public static boolean allowBackup() {
      return ALLOW_BACKUP;
   }

   public static String getVersionNumber() {
      return VERSION_NUMBER;
   }

   public void setShell(Shell _shell) {
      this._shell = _shell;
      this._shell.addControlListener(this);
      this._shell.addShellListener(this);
   }

   public Shell getShell() {
      return _shell;
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
   }
   @Override
   public void keyReleased(KeyEvent arg0) {
      _map.keyReleased(arg0);
   }
   public static final List<MapWidget3D> _widgets = new ArrayList<>();

   public static boolean _uses3dMap = false;
   public static void registerMapWidget3D(MapWidget3D mapWidget3D) {
      _widgets.add(mapWidget3D);
   }
   @Override
   public void controlMoved(ControlEvent arg0) {
      redrawMap();
   }
   @Override
   public void controlResized(ControlEvent arg0) {
      redrawMap();
   }
   @Override
   public void shellActivated(ShellEvent arg0) {
      redrawMap();
   }
   @Override
   public void shellClosed(ShellEvent arg0) {
   }
   @Override
   public void shellDeactivated(ShellEvent arg0) {
   }
   @Override
   public void shellDeiconified(ShellEvent arg0) {
   }
   @Override
   public void shellIconified(ShellEvent arg0) {
   }
}
