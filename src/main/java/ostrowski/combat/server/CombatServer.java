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
import ostrowski.combat.client.ui.PseudoRandomBlock;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.*;
import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.graphics.objects3d.Helper;
import ostrowski.util.ClientListener;
import ostrowski.util.*;

import java.io.File;
import java.util.List;
import java.util.*;

public class CombatServer extends Helper implements SelectionListener, Enums, IMapListener, ModifyListener, Listener, KeyListener, ControlListener, ShellListener
{
   private static final String VERSION_NUMBER = "4.2.0";
   static final         int    WINDOW_WIDTH   = 950;
   private              Shell  shell;
   Diagnostics diag = null;
   public static boolean                  isServer              = false;
   public static CombatServer             _this                 = null;
   // UI elements:
   private       Button                   startStopBattleButton;
   public        IMapWidget               map;
   private final Browser                  fullMessages;
   private       Browser                  messages;
   private       ClientListener           clientListener;
   private final Arena                    arena;
   private       Button[/*team*/][/*id*/] combatantsButtons;
   private       Combo[/*team*/][/*id*/]  combatantsName;
   private       Combo[/*team*/][/*id*/]  combatantsAI;
   private final byte                     maxTeams              = CombatServer.MAX_TEAMS;
   private       byte                     currentTeam           = -1;
   private       byte                     currentCombatant      = -1;
   private       boolean                  changingMap           = false;
   private       Button                   loadBattleButton;
   private       Button                   saveBattleButton;
   private       Button                   pausePlayButton;
   private       Button                   turnButton;
   private       Button                   roundButton;
   private       Button                   phaseButton;
   private final Object                   pausePlayControl      = new Object();
   private final Semaphore                lock_pausePlayControl = new Semaphore("CombatServer_pausePalyControl",
                                                                                CombatSemaphore.CLASS_COMBATSERVER_pausePlayControl);
   public final  CharacterWidget          charWidget;
   public final  CharacterFile            charFile              = new CharacterFile("Character.data");
   private final CharInfoBlock            charInfoBlock         = new CharInfoBlock(null);
   private       PseudoRandomBlock        pseudoRandomBlock     = new PseudoRandomBlock(this);

   public static final List<MapWidget3D> widgets   = new ArrayList<>();
   public static boolean                 uses3dMap = false;

   public static final byte MAX_COMBATANTS_PER_TEAM = 15;
   public static final byte MAX_TEAMS               = (byte) (TEAM_NAMES.length);

   private      TabFolder tabFolder;
   public final TabFolder tabFolderMain;
   private      TabItem   mapTabItem;
   private      TabItem   terrainTabItem;
   private      TabItem   triggersTabItem;
   private      TabItem   combatantsTabItem;
   private      TabItem   messagesTabItem;

   public  MapInterface      mapInterface;
   public  TerrainInterface  terrainInterface;
   private TriggersInterface triggersInterface;
   private CombatMap         originalMap = null;
   private boolean           autoStart   = false;

   public static final String REMOTE_AI_NAME   = "Remote Connection";
   public static final String INACTIVE_AI_NAME = "Off";

   private static boolean USE_CACHED_DRAWING = true;
   private static boolean ALLOW_BACKUP = false;

   public static final HashMap<Thread, HashMap<String, Object>> threadStorage_      = new HashMap<>();
   final               Semaphore                                lock_pendingMessage = new Semaphore("CombatServer_pendingMessage",
                                                                                                    CombatSemaphore.CLASS_COMBATSERVER_pendingMessage);
   private final       StringBuilder                            pendingMessage      = new StringBuilder();

   public static boolean inModify = false;

   public static int    pseudoRandomNumberSeed     = 123;
   public static int    pseudoRandomNumberUseCount = 0;
   public static Random pseudoRandom               = null;

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

   public CombatServer(List<String> args)
   {
      isServer = true;
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

      charWidget = new CharacterWidget(null, charFile);

      getShell().setLayout(new FillLayout());
      getShell().addKeyListener(this);
      tabFolderMain = new TabFolder(getShell(), SWT.NONE);

      TabItem item;
      // create a TabItem
      item = new TabItem(tabFolderMain, SWT.NULL);
      item.setText( "Arena Map");
      // create a control
      Composite arenaMap = createComposite(tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( arenaMap );

      // create the next tab
      item = new TabItem(tabFolderMain, SWT.NULL);
      item.setText( "Characters");
      Composite characterData = createComposite(tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( characterData );

      // create the next tab
      item = new TabItem(tabFolderMain, SWT.NULL);
      item.setText( "Full Messages");
      Composite fullMessageData = createComposite(tabFolderMain, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      item.setControl( fullMessageData );

      // create the next tab
      item = new TabItem(tabFolderMain, SWT.NULL);
      item.setText( "Rules");
      // add the control to the TabItem
      item.setControl(new RuleComposite(tabFolderMain, 1, GridData.FILL_BOTH, new Configuration(),
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
         fullMessages = new Browser(fullMessageData, SWT.NONE | SWT.BORDER);
         fullMessages.setText("<br><br><br><br><br>");
         data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.minimumHeight = 700;
         data.minimumWidth  = WINDOW_WIDTH;
         fullMessages.setLayoutData(data);
         fullMessages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      }
      arena = new Arena(this, mapInterface.getMapSizeX(), mapInterface.getMapSizeY());
      if (map != null) {
         map.addListener(arena);
      }

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
                  pseudoRandomBlock.setUsingPseudoRandomNumber(true);
               }
            }
         }
      }
      tabFolderMain.setSelection(initialTab);

      if (arenaName != null) {
         setMap(arenaName);
      }
      else {
         updateMap(arena);
      }
      if ((battleName != null) && (battleName.length()>0)) {
         File battleFile = new File(battleName);
         arena.serializeFromFile(battleFile);
      }

//      List<String> mapLocNames = new ArrayList<>();
//      for (byte team=0 ; team<_maxTeams ; team++) {
//         for (byte cur=0 ; cur<MAX_COMBATANTS_PER_TEAM ; cur++) {
//            mapLocNames.add(CombatMap.getLabel(team, cur));
//         }
//      }
      charWidget.buildCharSheet(characterData);

      getShell().pack();
      getShell().open();

      map.setZoomToFit();
      if (port != null) {
         Configuration.serverPort = Integer.parseInt(port);
         // If the port was passed in on the command line, open it up.
         autoStart = true;
      }
   }


   public void execute() {
      if (autoStart) {
         openPort(true/*startup*/);
      }

      Display display = getShell().getDisplay();
      while (!getShell().isDisposed()) {
         if (!display.readAndDispatch()) {
            for (MapWidget3D widget : widgets) {
               widget.redraw();
            }
            display.sleep ();
         }
      }
      display.dispose ();
   }

   private void cleanUpOnExit() {
      synchronized (pausePlayControl) {
         lock_pausePlayControl.check();
         pausePlayControl.notifyAll();
      }
      if (clientListener != null) {
         clientListener.closePort();
         clientListener = null;
      }
      if (arena != null) {
         arena.removeAllCombatants();
         arena.terminateBattle();
      }
      if (diag != null) {
         diag.endDiagnostics();
      }
      diag = null;
      System.out.println("end diagnostics called.");
   }


   @SuppressWarnings("deprecation")
   @Override
   protected void finalize() throws Throwable
   {
      try {
         if (diag != null) {
            diag.endDiagnostics();
            diag = null;
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
   }

   /**
    * @param mainGridBlock
    */
   private void buildBottomBlock(Composite mainGridBlock)
   {
      Composite bottomGridBlock = new Composite(mainGridBlock, SWT.NONE);
      FillLayout layout = new FillLayout();
      bottomGridBlock.setLayout(layout);
      tabFolder = new TabFolder(bottomGridBlock, SWT.NONE);


      { // Make the bottom section resize with the main window:
         GridData gdata = new GridData(GridData.FILL_BOTH);
         gdata.horizontalSpan = 1;
         gdata.grabExcessVerticalSpace = true;
         gdata.grabExcessHorizontalSpace = true;
         bottomGridBlock.setLayoutData(gdata);
      }

      // create a TabItem
      mapTabItem = new TabItem(tabFolder, SWT.NULL);
      mapTabItem.setText("Map layout");
      // create a control
      Composite mapComposite = createComposite(tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      mapTabItem.setControl(mapComposite);

      // create a TabItem
      terrainTabItem = new TabItem(tabFolder, SWT.NULL);
      terrainTabItem.setText("Terrain and Walls");
      // create a control
      Composite terrainComposite = createComposite(tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      terrainTabItem.setControl(terrainComposite);

      // create the next tab
      triggersTabItem = new TabItem(tabFolder, SWT.NULL);
      triggersTabItem.setText("Triggers");
      Composite triggersComposite = createComposite(tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      triggersTabItem.setControl(triggersComposite);

      // create the next tab
      combatantsTabItem = new TabItem(tabFolder, SWT.NULL);
      combatantsTabItem.setText("Combatants");
      Composite combatantsComposite = createComposite(tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      combatantsTabItem.setControl(combatantsComposite);

      // create the next tab
      messagesTabItem = new TabItem(tabFolder, SWT.NULL);
      messagesTabItem.setText("Messages");
      Composite messagesComposite = createComposite(tabFolder, 1, GridData.FILL_BOTH);
      // add the control to the TabItem
      messagesTabItem.setControl(messagesComposite);

      int selectedTabIndex = 0;
      tabFolder.setSelection(selectedTabIndex);
      tabFolder.addSelectionListener(this);

      // When the server starts up, disable the pan feature of the map so you can drag a paintbrush
      // with terrain over multiple hexes at once.
      map.allowPan(false);
      map.setMode(IMapWidget.MapMode.NONE);

      TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
      if (map != null) {
         CombatMap combatMap = map.getCombatMap();
         if (combatMap != null) {
            if (selectedTab != triggersTabItem) {
               combatMap.setSelectedTrigger(null);
            }
            else {
               combatMap.setSelectedTrigger(triggersInterface.getCurrentlySelectedTrigger());
            }
         }
      }

      GridData data;
      GridLayout grid;
      mapInterface = new MapInterface(this);
      terrainInterface = new TerrainInterface();
      triggersInterface = new TriggersInterface();
      if (map instanceof MapWidget3D) {
         ((MapWidget3D) map).addGLViewListener(terrainInterface);
      }
      mapInterface.buildBlock(mapComposite);
      terrainInterface.buildBlock(terrainComposite);
      triggersInterface.buildBlock(triggersComposite);
      triggersInterface.setMap(map);
      terrainInterface.setMap(map);

      {
         Composite combatantButtonsBlock = new Composite(combatantsComposite, SWT.TRAIL);
         grid = new GridLayout(maxTeams/*columns*/, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         combatantButtonsBlock.setLayout(grid);
         data = new GridData();
         data.horizontalSpan = 3;
         combatantButtonsBlock.setLayoutData(data);
         combatantsButtons = new Button[maxTeams][MAX_COMBATANTS_PER_TEAM];
         combatantsAI = new Combo[maxTeams][MAX_COMBATANTS_PER_TEAM];
         combatantsName = new Combo[maxTeams][MAX_COMBATANTS_PER_TEAM];
         List<String> charNames = charWidget.getCharacterFile().getCharacterNames();
         charNames.add("Random...");
         List<String> aiNames = new ArrayList<>();
         aiNames.add(INACTIVE_AI_NAME);
         aiNames.add(REMOTE_AI_NAME);
         aiNames.add("Local");
         for (AI_Type aiType : AI_Type.values()) {
            aiNames.add("AI - " + aiType.name);
         }
         for (int team = 0; team < maxTeams; team++) {
            Group teamGroup = createGroup(combatantButtonsBlock, TEAM_NAMES[team], 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
            for (int combatant=0 ; combatant<MAX_COMBATANTS_PER_TEAM ; combatant++) {
               combatantsButtons[team][combatant] = new Button(teamGroup, SWT.PUSH);
               combatantsAI[team][combatant] = createCombo(teamGroup, SWT.DROP_DOWN | SWT.READ_ONLY, 1/*hSpan*/, aiNames);
               combatantsName[team][combatant] = createCombo(teamGroup, SWT.NONE, 1/*hSpan*/, charNames);
               combatantsButtons[team][combatant].addListener(SWT.Paint, this);
               data = new GridData();
               data.minimumWidth = 35;
               data.horizontalAlignment = SWT.CENTER;
               data.grabExcessHorizontalSpace = true;
               combatantsButtons[team][combatant].setLayoutData(data);
               combatantsButtons[team][combatant].addSelectionListener(this);
               // enable only the first button.
               combatantsButtons[team][combatant].setEnabled(true);
               combatantsAI[team][combatant].setEnabled(false);
               combatantsName[team][combatant].setEnabled(false);
               combatantsAI[team][combatant].setText(aiNames.get(0)); // 'Off'

               combatantsName[team][combatant].addSelectionListener(this);
               combatantsName[team][combatant].addModifyListener(this);
               combatantsAI[team][combatant].addSelectionListener(this);
               combatantsAI[team][combatant].addModifyListener(this);
            }
         }
      }
      {
         messages = new Browser(messagesComposite, SWT.NONE | SWT.BORDER);
         messages.setText("<br><br><br><br><br>");
         data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.minimumHeight = 175;
         data.minimumWidth  = WINDOW_WIDTH;
         data.horizontalSpan = 3;
         messages.setLayoutData(data);
         Display display = getShell().getDisplay();
         if (display != null) {
            messages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
         }
      }
   }

   public void addNewCharacter(String characterName) {
      for (int team = 0; team < maxTeams; team++) {
         for (int combatant = 0; combatant < combatantsName[team].length ; combatant++) {
            combatantsName[team][combatant].add(characterName);
         }
      }
   }
   public void removeCharacter(String characterName) {
      for (int team = 0; team < maxTeams; team++) {
         for (int combatant = 0; combatant < combatantsName[team].length ; combatant++) {
            combatantsName[team][combatant].remove(characterName);
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

      uses3dMap = Configuration.use3DMap();

      if (uses3dMap) {
         map = new MapWidget3D(midGridBlock);
      }
      else {
         map = new MapWidget2D(midGridBlock);
      }
      map.addListener(this);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.minimumHeight = MAP_SIZE_HEIGHT;
      data.minimumWidth  = MAP_SIZE_WIDTH;

      data.horizontalAlignment = GridData.FILL;
      map.setLayoutData(data);
      {
         Composite midRightBlock = new Composite(midGridBlock, SWT.CENTER);
         grid = new GridLayout(1, false);
         midRightBlock.setLayout(grid);
         data = new GridData(SWT.FILL, SWT.FILL, false/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
         data.horizontalAlignment = SWT.BEGINNING;
         midRightBlock.setLayoutData(data);
         map.addControlGroup(midRightBlock);
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
               pausePlayButton = createButton(midRightTopLeftBlock, "Pause", 1/*hSpan*/, null/*fontData*/, this);
               turnButton = createButton(midRightTopLeftBlock, "Turn++", 1/*hSpan*/, null/*fontData*/, this);
               roundButton = createButton(midRightTopLeftBlock, "Round++", 1/*hSpan*/, null/*fontData*/, this);
               phaseButton = createButton(midRightTopLeftBlock, "Phase++", 1/*hSpan*/, null/*fontData*/, this);
               resetPlayPauseControls();

               Group group = createGroup(midRightTopLeftBlock, "Battle", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
               loadBattleButton = createButton(group, "Load", 1/*hSpan*/, null/*fontData*/, this);
               saveBattleButton = createButton(group, "Save", 1/*hSpan*/, null/*fontData*/, this);
            }
            charInfoBlock.buildBlock(midRightTopBlock);
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

            pseudoRandomBlock.buildBlock(midRightBottomBlock);
         }
      }
   }

   private void resetPlayPauseControls() {
      pausePlayButton.setEnabled(true);
      pausePlayButton.setText("Pause");
      turnButton.setEnabled(false);
      roundButton.setEnabled(false);
      phaseButton.setEnabled(false);
   }

   /**
    * @param parent
    */
   private void addStartButton(Composite parent)
   {
      startStopBattleButton = new Button(parent, SWT.LEFT);
      setOpenButtonText(true/*start*/);
      startStopBattleButton.addSelectionListener(this);
   }
   private void setOpenButtonText(boolean start) {
      Color bgColor;
      if (start) {
         startStopBattleButton.setText("Start Battle");
         bgColor = new Color(startStopBattleButton.getDisplay(), 128, 255, 128);
      } else {
         startStopBattleButton.setText("Stop Battle");
         bgColor = new Color(startStopBattleButton.getDisplay(), 255, 128, 128);
      }
      startStopBattleButton.setBackground(bgColor);
      bgColor.dispose();
   }

   public void setMap(String arenaName)
   {
      CombatMap map;
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
      setMap(map, true/*clearCombatants*/);
   }
   public void setMap(CombatMap map, boolean clearCombatants) {
      if (this.map != null) {
         this.map.endHexSelection();
      }
      if (map != null) {
         map = map.clone();
         changingMap = true;
         arena.setCombatMap(map, clearCombatants);
         this.map.updateMap(map, (byte)-1/*selfID*/, (byte)-1/*selfTeam*/, null/*availableLocs*/, (byte)-1/*targetID*/);
         triggersInterface.setMap(map);
         originalMap = map.clone();
         mapInterface.setMap(map);
         for (byte team = 0; team < maxTeams; team++) {
            for (byte curCombatantIndex=0 ; curCombatantIndex<map.getStockAIName(team).length ; curCombatantIndex++) {
               boolean locExists = (map.getStartingLocation(team, curCombatantIndex) != null);
               String aiName = map.getStockAIName(team)[curCombatantIndex];
               String combatantName = map.getStockCharacters(team)[curCombatantIndex];
               if ((aiName == null) || REMOTE_AI_NAME.equals(aiName) || INACTIVE_AI_NAME.equals(aiName)) {
                  aiName = locExists ? REMOTE_AI_NAME : INACTIVE_AI_NAME;
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
               if (combatantsAI != null) {
                  combatantsAI[team][curCombatantIndex].setText(aiName);
                  combatantsAI[team][curCombatantIndex].setEnabled(locExists);
               }
               if (combatantsName != null) {
                  combatantsName[team][curCombatantIndex].setText(combatantName);
                  boolean enableNameEdit = locExists && !INACTIVE_AI_NAME.equals(aiName);
                  combatantsName[team][curCombatantIndex].setEnabled(enableNameEdit);
               }
            }
         }
         changingMap = false;
         resetPlayPauseControls();
      }
      updateMap(arena);
   }
   @Override
   public void handleEvent(Event event)
   {
      int sizePerHex = 20;
      int offsetX = 6;
      int offsetY = 2;
      if (event.type == SWT.Paint) {
         for (byte team = 0; team < combatantsButtons.length ; team++) {
            for (byte cur = 0; cur < combatantsButtons[team].length ; cur++) {
               if (event.widget == combatantsButtons[team][cur]) {
                  ArenaLocation loc = new ArenaLocation((short)0,(short)0);
                  if (combatantsButtons[team][cur].isEnabled()) {
                     loc.setTerrain(TerrainType.GRASS);
                     loc.setLabel(CombatMap.getLabel(team, cur));
                  }
                  else {
                     loc.setTerrain(TerrainType.GRAVEL);
                     loc.setLabel(" -- ");
                  }
                  if ((currentTeam == team) && (currentCombatant == cur)) {
                     event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
                     event.gc.setForeground(event.display.getSystemColor(SWT.COLOR_GRAY));
                     event.gc.fillRectangle(2, 2, event.width-4, event.height-4);
                  }

                  //loc;
                  MapWidget2D.drawHex(loc, event.gc, event.display, sizePerHex, offsetX, offsetY);
                  int[] bounds = MapWidget2D.getHexDimensions((short)0/*column*/, (short)0/*row*/, sizePerHex, 0/*offsetX*/, 0/*offsetY*/, true/*cacheResults*/);
                  int x = bounds[MapWidget2D.X_SMALLEST] + 5;
                  int y = bounds[MapWidget2D.Y_SMALLEST] + 3;
                  if (combatantsButtons[team][cur].isEnabled()) {
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

   @Override
   public void widgetSelected(SelectionEvent e)
   {
      try {
         if (e.widget == startStopBattleButton) {
            if (startStopBattleButton.getText().equals("Start Battle")) {
//               Shell shell = new Shell(shell, SWT.DIALOG_TRIM | SWT.MODELESS);
//               shell.setText("test");
//               shell.setLayout(new GridLayout(2/*numColumns*/, false/*makeColumnsEqualWidth*/));
//               //shell.addFocusListener(this);
               openPort(true/*startup*/);
            } else { // openButton.getText().equals("Stop Battle")
               onPlay();
               closePort();
            }
         }
         else if ((e.widget == loadBattleButton) || (e.widget == saveBattleButton)) {
            boolean save = (e.widget == saveBattleButton);
            FileDialog fileDlg = new FileDialog(loadBattleButton.getShell(), save ? SWT.SAVE : 0);
            fileDlg.setFilterExtensions(new String[] {"*.btl"});
            fileDlg.setFilterNames(new String[] {"battle files (*.btl)"});
            fileDlg.setText("Select battle file to " + (save ? "save battle to" : "load battle from"));
            String filename = fileDlg.open();
            if ((filename != null) && (filename.length()>0)) {
               File battleFile = new File(filename);
               if (save) {
                  arena.serializeToFile(battleFile);
               }
               else {
                  arena.serializeFromFile(battleFile);
                  setMap(arena.getCombatMap(), false/*clearCombatants*/);
                  setOpenButtonText(false/*start*/);
                  // Open the Messages tab:
                  this.tabFolder.setSelection(3);
                  this.map.allowPan(true);
               }
            }
         }
         else if (e.widget == pausePlayButton) {
            if (pausePlayButton.getText().equals("Pause")) {
               pausePlayButton.setEnabled(false);
               if (arena != null) {
                  arena.onPause();
               }
            }
            else {
               onPlay();
            }
         }
         else if ((e.widget == turnButton) || (e.widget == roundButton) || (e.widget == phaseButton)) {
            resetPlayPauseControls();
            if (arena != null) {
               if (e.widget == turnButton) {
                  arena.onTurnAdvance();
               }
               if (e.widget == roundButton) {
                  arena.onRoundAdvance();
               }
               if (e.widget == phaseButton) {
                  arena.onPhaseAdvance();
               }
            }
            synchronized (pausePlayControl) {
               lock_pausePlayControl.check();
               pausePlayControl.notifyAll();
            }
         }
         else if (e.widget == tabFolder){
            int selectedTabIndex = tabFolder.getSelectionIndex();
            TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
            boolean isDraggable = (selectedTab != terrainTabItem) || (terrainInterface.allowPan());
            map.allowPan(isDraggable);
            map.setMode(isDraggable ? IMapWidget.MapMode.DRAG :
                        IMapWidget.MapMode.NONE);

            if (selectedTab != triggersTabItem) {
               map.getCombatMap().setSelectedTrigger(null);
               map.redraw();
            }
            else {
               ArenaTrigger trigger = triggersInterface.getCurrentlySelectedTrigger();
               map.getCombatMap().setSelectedTrigger(trigger);
               if (trigger != null) {
                  map.redraw();
               }
            }
         }
         else {
            for (byte team = 0; team < combatantsButtons.length ; team++) {
               for (byte cur = 0; cur < combatantsButtons[team].length ; cur++) {
                  byte prevCurrentTeam = currentTeam;
                  byte prevCurrentCombatant = currentCombatant;
                  if (e.widget == combatantsButtons[team][cur]) {
                     ArenaLocation oldStartLoc = arena.getCombatMap().clearStartingLocation(team, cur);
                     currentTeam = team;
                     currentCombatant = cur;
                     combatantsAI[team][cur].setEnabled(false);
                     combatantsName[team][cur].setEnabled(false);
                     combatantsButtons[team][cur].redraw();
                     if ((map != null) && (map instanceof MapWidget2D)){
                        if (oldStartLoc != null) {
                           List<ArenaCoordinates> locs = new ArrayList<>();
                           locs.add(oldStartLoc);
                           ((MapWidget2D) map).redraw(locs);
                        }
                     }
                     mapInterface.refreshSaveButton();
                  }
                  else if (e.widget == combatantsAI[team][cur]) {
                     changeAI(team, cur);
                  }
                  else if (e.widget == combatantsName[team][cur]) {
                     changeName(team, cur, true/*checkForRandom*/);
                     //refreshSaveButton();
                  }
                  else {
                     continue;
                  }
                  if ((prevCurrentTeam != -1) && (prevCurrentCombatant != -1)) {
                     combatantsButtons[prevCurrentTeam][prevCurrentCombatant].redraw();
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
      String newName = combatantsName[team][cur].getText();
      if (checkForRandom) {
         if (newName.equals("Random...")) {
            GenerateCharacterDialog dialog = new GenerateCharacterDialog(getShell(), null);
            int points = dialog.open();
            String race = dialog.getRace();
            String equipment = dialog.getEquipment();
            newName = "? " + race + " " + points + " " + equipment;
            combatantsName[team][cur].setText(newName);
         }
      }
      // new AI player selected
      if (map != null) {
         CombatMap combatMap = map.getCombatMap();
         if (combatMap != null) {
            Combo AICombo = combatantsAI[team][cur];
            if (AICombo != null) {
               combatMap.setStockCharacter(newName, AICombo.getText(), team, cur);
            }
         }
      }
      mapInterface.refreshSaveButton();
   }

   private void changeAI(byte team, byte cur) {
      // new AI engine selected

      String newAI = combatantsAI[team][cur].getText();
      if (newAI.startsWith("AI - ")) {
         newAI = newAI.replace("AI - ", "");
      }
      Combo nameCombo = combatantsName[team][cur];
      if (nameCombo != null) {
         if (INACTIVE_AI_NAME.equals(newAI)) {
            nameCombo.setText("");
            nameCombo.setEnabled(false);
         }
         else {
            nameCombo.setEnabled(true);
         }
         if (map != null) {
            CombatMap combatMap = map.getCombatMap();
            if (combatMap != null) {
               combatMap.setStockCharacter(nameCombo.getText(), newAI, team, cur);
            }
         }
      }
      mapInterface.refreshSaveButton();
   }
   public void onPlay() {
      resetPlayPauseControls();
      if (arena != null) {
         arena.onPlay();
      }
      synchronized (pausePlayControl) {
         lock_pausePlayControl.check();
         pausePlayControl.notifyAll();
      }
   }
   public void onPause() {
      pausePlayButton.setEnabled(true);
      pausePlayButton.setText("Play ");
      turnButton.setEnabled(true);
      roundButton.setEnabled(true);
      phaseButton.setEnabled(true);
   }
   public void waitForPlay(List<Object> waitingObjects) {
      try {
         if (!getShell().isDisposed()) {
            Display display = getShell().getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(this::onPause);
            }
         }
         synchronized (pausePlayControl) {
            lock_pausePlayControl.check();
            waitingObjects.add(pausePlayControl);
            pausePlayControl.wait();
         }
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      finally {
         waitingObjects.remove(pausePlayControl);
      }
   }

   public void onAutoRun(AutoRunBlock autoRunBlock) {
      openPort(false/*startup*/);
      arena.onAutoRun(autoRunBlock);
   }

   private void openPort(boolean startup)
   {
      resetMessageBuffer();
      TabFolder bottomTabFolder = messagesTabItem.getParent();
      bottomTabFolder.setSelection(bottomTabFolder.indexOf(messagesTabItem));
      mapTabItem.getControl().setVisible(false);
      terrainTabItem.getControl().setVisible(false);
      triggersTabItem.getControl().setVisible(false);
      combatantsTabItem.getControl().setVisible(false);

      mapInterface.openPort();
      setOpenButtonText(false/*start*/);

      if (startup) {
         clientListener = new ClientListener(this);
         clientListener.start();
         CombatServer.resetPseudoRandomNumberGenerator();
         arena.addStockCombatants();
      }
   }
   private void closePort()
   {
      resetPlayPauseControls();

      TabFolder bottomTabFolder = messagesTabItem.getParent();
      bottomTabFolder.setSelection(bottomTabFolder.indexOf(terrainTabItem));
      mapTabItem.getControl().setVisible(true);
      terrainTabItem.getControl().setVisible(true);
      triggersTabItem.getControl().setVisible(true);
      combatantsTabItem.getControl().setVisible(true);

      if (clientListener != null) {
         clientListener.closePort();
         clientListener = null;
      }
      mapInterface.closePort();
      setOpenButtonText(true/*start*/);

      arena.removeAllCombatants();
      arena.terminateBattle();
      arena.disconnectAllClients();
      if (map != null) {
         map.setZoomToFit();
         CombatMap combatMap = map.getCombatMap();
         combatMap.removeAllCombatants();
         // reload the map, so a new battle can begin
         setMap(combatMap.getName());
         int selectedTabIndex = tabFolder.getSelectionIndex();
         TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
         if (selectedTab != triggersTabItem) {
            combatMap.setSelectedTrigger(null);
         }
         else {
            combatMap.setSelectedTrigger(triggersInterface.getCurrentlySelectedTrigger());
         }
         map.redraw();
      }
      for (MessageDialog dialog : MessageDialog.ACTIVE_MESSAGES) {
         // TODO: close this dialog
         if (!dialog.shell.isDisposed()) {
            dialog.shell.close();
         }
      }
      MessageDialog.ACTIVE_MESSAGES.clear();
      MessageDialog.topMessage = null;
      for (RequestUserInput input : Arena.activeRequestUserInputs) {
         input.shell.close();
      }
      Arena.activeRequestUserInputs.clear();
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }

   public void resetMessageBuffer() {
      if (messages != null) {
         if (!getShell().isDisposed()) {
            Display display = getShell().getDisplay();
            if (!display.isDisposed()) {
               display.asyncExec(() -> {
                  if ((messages != null) && (!messages.isDisposed())) {
                     messages.setText("<body></body>");
                  }
                  if ((fullMessages != null) && (!fullMessages.isDisposed())) {
                     fullMessages.setText("<body></body>");
                  }
                  synchronized (pendingMessage) {
                     lock_pendingMessage.check();
                     pendingMessage.setLength(0);
                  }
               });
            }
         }
      }
   }
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
      synchronized (pendingMessage) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_pendingMessage)) {
            boolean alreadyWaiting = (pendingMessage.length() > 0);
            pendingMessage.append(fullMsg);
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
               synchronized (pendingMessage) {
                  lock_pendingMessage.check();
                  msg = pendingMessage.toString();
                  pendingMessage.setLength(0);
               }
               // remove all CR-LF because they terminate the javascript execution for the insert
               msg = msg.replace("\n", "");
               // escape any single quote character, since we are putting this inside a single quote
               msg = msg.replace("'", "\\'");
               StringBuilder sb = new StringBuilder();
               sb.append("document.body.insertAdjacentHTML('beforeEnd', '");
               sb.append(msg);
               sb.append("');window.scrollTo(0,document.body.scrollHeight);"); // scroll to the bottom of the window
               if ((messages != null) && (!messages.isDisposed())) {
                  messages.execute(sb.toString());
                  messages.redraw();
               }
               if ((fullMessages != null) && (!fullMessages.isDisposed())) {
                  fullMessages.execute(sb.toString());
                  fullMessages.redraw();
               }
           });
         }
      }
   }

   public Arena getArena() { return arena;  }

   public void updateMap(final Arena arena) {
      // we can't modify any UI element from another thread,
      // so we must use the Display.asyncExec() method:
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               if (map != null) {
                  if (map.updateMap(arena.getCombatMap(), -1/*selfID*/, (byte)-1/*selfTeam*/, null/*availableLocs*/, -1/*targetID*/)) {
                     map.setZoomToFit();
                  }
                  int selectedTabIndex = tabFolder.getSelectionIndex();
                  TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
                  CombatMap combatMap = map.getCombatMap();
                  if (combatMap != null) {
                     if (selectedTab != triggersTabItem) {
                        combatMap.setSelectedTrigger(null);
                     }
                     else {
                        combatMap.setSelectedTrigger(triggersInterface.getCurrentlySelectedTrigger());
                     }
                  }
               }
               mapInterface.refreshSaveButton();
            });
         }
      }
   }

   @Override
   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter)
   {
      List<ArenaCoordinates> locationsToRedraw = new ArrayList<>();
      int selectedTabIndex = tabFolder.getSelectionIndex();
      TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
      boolean redraw = false;
      if (selectedTab == terrainTabItem) {
         terrainInterface.onMouseDown(loc, event, angleFromCenter, normalizedDistFromCenter, map, locationsToRedraw);
         redraw = true;
      }
      else if (selectedTab == triggersTabItem) {
         triggersInterface.onMouseDown(loc, event, angleFromCenter, normalizedDistFromCenter, map, locationsToRedraw);
         redraw = true;
      }
      if (redraw && (map != null) && (map instanceof MapWidget2D)) {
         if (locationsToRedraw.isEmpty()) {
            map.redraw();
         }
         else {
            MapWidget2D map = (MapWidget2D) this.map;
            map.redraw(locationsToRedraw);
         }
      }

      // On every mouse click, the map may become modified
      mapInterface.refreshSaveButton();
   }
   @Override
   public void onMouseMove(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
   }
   @Override
   public void onMouseUp(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      //if (loc != null) {

         List<ArenaCoordinates> locationsToRedraw = new ArrayList<>();

         int selectedTabIndex = tabFolder.getSelectionIndex();
         TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
         boolean redraw = false;
         if (selectedTab == terrainTabItem) {
            terrainInterface.onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter, map, locationsToRedraw);
            redraw = true;
         }
         else if (selectedTab == triggersTabItem) {
            triggersInterface.onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter, map.getCombatMap(),
                                        locationsToRedraw);
            redraw = true;
         }
         else if (selectedTab == combatantsTabItem) {
            if ((currentTeam > -1) && (currentCombatant != -1)) {
               ArenaLocation oldLoc = arena.getCombatMap().getStartingLocation(currentTeam, currentCombatant);
               if (oldLoc != null) {
                  locationsToRedraw.add(oldLoc);
               }
               arena.getCombatMap().setStartingLocation(currentTeam, currentCombatant, loc);
               combatantsAI[currentTeam][currentCombatant].setEnabled(true);
               if (INACTIVE_AI_NAME.equals(combatantsAI[currentTeam][currentCombatant].getText())) {
                  combatantsAI[currentTeam][currentCombatant].setText(REMOTE_AI_NAME);
               }
               else if (!REMOTE_AI_NAME.equals(combatantsAI[currentTeam][currentCombatant].getText())) {
                  combatantsName[currentTeam][currentCombatant].setEnabled(true);
               }
               redraw = true;
               locationsToRedraw.add(loc);
            }
         }
         if (redraw && (map != null) && (map instanceof MapWidget2D)) {
            if (locationsToRedraw.isEmpty()) {
               map.redraw();
            }
            else {
               MapWidget2D map = (MapWidget2D) this.map;
               map.redraw(locationsToRedraw);
            }
         }

         // On every mouse click, the map may become modified
         mapInterface.refreshSaveButton();
//      }
   }

   @Override
   public void onMouseDrag(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {
      int selectedTabIndex = tabFolder.getSelectionIndex();
      TabItem selectedTab = tabFolder.getItem(selectedTabIndex);
      if (selectedTab == terrainTabItem) {
         terrainInterface.onMouseDrag(loc, event, angleFromCenter, normalizedDistFromCenter, map);
      }
      else {
         onMouseUp(loc, event, angleFromCenter, normalizedDistFromCenter);
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (!inModify) {
         inModify = true;
         try {
            for (byte team = 0; (team < combatantsButtons.length) ; team++) {
               for (byte cur = 0; (cur < combatantsButtons[team].length) ; cur++) {
                  if (e.widget == combatantsAI[team][cur]) {
                      changeAI(team, cur);
                      return;
                  }
                  if (e.widget == combatantsName[team][cur]) {
                     changeName(team, cur, false/*checkForRandom*/);
                     return;
                  }
               }
            }
         }
         finally {
            inModify = false;
         }
      }
   }
   public void redrawMap() {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            display.asyncExec(() -> {
               if (map != null) {
                  map.redraw();
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
               if (map != null) {
                  if (map instanceof MapWidget2D) {
                     ((MapWidget2D) map).redraw(locationsToRedraw);
                  }
                  else if (map instanceof MapWidget3D) {
                     map.redraw();
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
            display.asyncExec(() -> charInfoBlock.updateCombatants(combatants));
         }
      }
   }

   public static double random() {
//      if (!_isServer)
//         Rules.debugBreak();

      if (pseudoRandom == null) {
         pseudoRandom = new Random(pseudoRandomNumberSeed);
         for (int i=0 ; i <50 ; i++) {
            pseudoRandom.nextDouble();
         }

         pseudoRandomNumberUseCount = 0;
         CharacterGenerator.NAMES_LIST_MALE.clear();
         CharacterGenerator.NAMES_LIST_FEMALE.clear();
      }
      pseudoRandomNumberUseCount++;
      return pseudoRandom.nextDouble();
   }

   public void onNewBattle() {
      if (!getShell().isDisposed()) {
         Display display = getShell().getDisplay();
         if (!display.isDisposed()) {
            final Object synchObject = 0;
            display.asyncExec(() -> {
               if (pseudoRandomBlock.isUsingPseudoRandomNumber()) {
                  pseudoRandomBlock.setSeedText(pseudoRandomNumberSeed);
               }
               else {
                  if (!arena.characterGenerated) {
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
      synchronized (pausePlayControl) {
         lock_pausePlayControl.check();
      }
   }
   public static final int MAX_PSEUDO_RANDOM_VALUE = 1000;
   public static void generateNewPseudoRandomNumberSeed() {
      setPseudoRandomNumberSeed((int) (System.currentTimeMillis() / 100));
   }
   public boolean isUsingPseudoRandomNumbers() {
      return pseudoRandomBlock.isUsingPseudoRandomNumber();
   }
   public static void setPseudoRandomNumberSeed(int newValue)
   {
      int clippedValue = (newValue % MAX_PSEUDO_RANDOM_VALUE);
      // For every new battle, always create a new Random object, so if you run the same seed twice,
      // it doesn't just continue the same sequences.
      pseudoRandom = null;
      if (pseudoRandomNumberSeed == clippedValue) {
         return;
      }
      pseudoRandomNumberSeed = clippedValue;
      if (!inModify) {
         if (_this != null) {
            _this.pseudoRandomBlock.setSeedText(pseudoRandomNumberSeed);
         }
      }
   }
   public static int getPseudoRandomNumberSeed() {
      return pseudoRandomNumberSeed;
   }
   public static int getPseudoRandomNumberUseCount() {
      return pseudoRandomNumberUseCount;
   }

   public static void resetPseudoRandomNumberGenerator() {
      pseudoRandom = null;
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

   public void setShell(Shell shell) {
      this.shell = shell;
      this.shell.addControlListener(this);
      this.shell.addShellListener(this);
   }

   public Shell getShell() {
      return shell;
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
   }
   @Override
   public void keyReleased(KeyEvent arg0) {
      map.keyReleased(arg0);
   }

   public static void registerMapWidget3D(MapWidget3D mapWidget3D) {
      widgets.add(mapWidget3D);
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

   public void setMapSize(short x, short y) {
      if (!changingMap) {
         arena.setSize(x, y);
         updateMap(arena);
         redrawMap();
      }
   }
}
