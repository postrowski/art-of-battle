package ostrowski.combat.common;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ostrowski.combat.common.enums.AI_Type;
import ostrowski.combat.common.enums.Enums;
import ostrowski.combat.common.enums.Facing;
import ostrowski.combat.common.enums.TerrainType;
import ostrowski.combat.common.orientations.Orientation;
import ostrowski.combat.common.things.Thing;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.ArenaLocation;
import ostrowski.combat.server.CombatServer;
import ostrowski.protocol.ObjectChanged;

public class RightClickPopupMenu
{
   private interface IActOnTextResults {
      void doAction(String results);
   }

   private ArenaLocation _currentMouseLoc = null;
   private final Arena _arena;
   private Menu _popupMenu;
   private Menu _moveToPopupMenu;
   private MenuItem _characterPopupMenu;

   private final Map<AI_Type, MenuItem> _controlMenuSubItem = new HashMap<>();
   private MenuItem _aiLocal;
   private MenuItem _aiRemote;
   private final Map<String, MenuItem> _teamMenuSubItem = new HashMap<>();
   private MenuItem _removeItems;
   private MenuItem _addCharacter;
   private final Map<TerrainType, MenuItem> _setTerrain = new HashMap<>();
   private ArenaLocation _moveFromLocation = null;

   public RightClickPopupMenu(Arena arena) {
      this._arena = arena;
   }

   public void onRightMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter) {

      if (_currentMouseLoc != loc) {
         //Rules.diag("onMouseMove (" + event.x + "," + event.y + ")");
         _currentMouseLoc = loc;
         Shell shell = event.display.getActiveShell();
         if (shell == null) {
            return;
         }

         if ((_popupMenu == null) || _popupMenu.isDisposed()) {
            createMainMenu(shell);
         }
         if ((_moveToPopupMenu == null) || _moveToPopupMenu.isDisposed()) {
            createMoveToMenu(shell);
         }

         _popupMenu.setVisible(false);
         _moveToPopupMenu.setVisible(false);
      }
      if (loc != null) {
         List<Character> characters = loc.getCharacters();
         List<Object> things = loc.getThings();
         boolean characterPresent = !characters.isEmpty();
         this._characterPopupMenu.setEnabled(characterPresent);
         _addCharacter.setEnabled(!characterPresent);
         if (characterPresent) {
            Character character = loc.getCharacters().get(0);
            // Select the character's current team
            for (String teamName : Enums.TEAM_NAMES) {
               boolean selected = Enums.TEAM_NAMES[character._teamID].equals(teamName);
               _teamMenuSubItem.get(teamName).setSelection(selected);
            }

            // Select the character's current AI/remote/local Control
            _aiRemote.setSelection(false);
            _aiLocal.setSelection(false);
            for (AI_Type aiType : AI_Type.values()) {
               _controlMenuSubItem.get(aiType).setSelection(false);
            }
            if (this._arena.isRemotelyControlled(character)) {
               _aiRemote.setSelection(true);
            }
            else {
               AI_Type aiType = this._arena.getAiType(character);
               if (aiType == null) {
                  _aiLocal.setSelection(true);
               }
               else {
                  _controlMenuSubItem.get(aiType).setSelection(true);
               }
            }
         }
         // Select the current location's Terrain
         for (TerrainType terrainType : TerrainType.values()) {
            boolean selected = _currentMouseLoc.getTerrain() == terrainType;
            _setTerrain.get(terrainType).setSelection(selected);
         }
         // enable/disable the 'remove all items' menu item.
         _removeItems.setEnabled(things.size() > characters.size());
         _currentMouseLoc = loc;
      }
      Point cursorLocation = Display.getCurrent().getCursorLocation();
      if (_moveFromLocation == null) {
         _popupMenu.setLocation(cursorLocation.x, cursorLocation.y);
      }
      else {
         _moveToPopupMenu.setLocation(cursorLocation.x, cursorLocation.y);
      }

      _moveToPopupMenu.setVisible(_moveFromLocation != null);
      _popupMenu.setVisible(_moveFromLocation == null);
   }

   private void createMainMenu(Shell shell) {
      _popupMenu = new Menu(shell);
      {
         _characterPopupMenu = createMenu(_popupMenu, "Character", null, SWT.CASCADE);
         Menu subMenuCharacter = new Menu(_characterPopupMenu);
         _characterPopupMenu.setMenu(subMenuCharacter);
         {
            MenuItem changeControlMenuItem = createMenu(subMenuCharacter, "Change Control", null, SWT.CASCADE);
            Menu subMenuChangeControl = new Menu(changeControlMenuItem);
            changeControlMenuItem.setMenu(subMenuChangeControl);
            _aiRemote = createMenu(subMenuChangeControl, "Remote Connection", e -> changeToRemoteConnection(), SWT.CHECK);
            _aiLocal = createMenu(subMenuChangeControl, "Local Control", e -> changeControl(null), SWT.CHECK);
            for (AI_Type ai : AI_Type.values()) {
               _controlMenuSubItem.put(ai, createMenu(subMenuChangeControl, ai.name(),
                                                      e -> changeControl(ai), SWT.CHECK));
            }
         }
         {
            MenuItem changeTeamMenuItem = createMenu(subMenuCharacter, "Change Team", null, SWT.CASCADE);
            Menu subMenuChangeTeam = new Menu(changeTeamMenuItem);
            changeTeamMenuItem.setMenu(subMenuChangeTeam);
            for (String teamName : Enums.TEAM_NAMES) {
               _teamMenuSubItem.put(teamName, createMenu(subMenuChangeTeam, teamName,
                                                         e -> changeTeam(teamName), SWT.CHECK));
            }
         }

         createMenu(subMenuCharacter, "Relocate", e -> _moveFromLocation = _currentMouseLoc, SWT.NONE);
         createMenu(subMenuCharacter, "Load into editor", e -> editCharacter(true), SWT.NONE);
         createMenu(subMenuCharacter, "Save from editor", e -> editCharacter(false), SWT.NONE);
         createMenu(subMenuCharacter, "Edit Condition", e -> editCharacterCondition(), SWT.NONE);
         createMenu(subMenuCharacter, "Remove", e -> removeCharacter(), SWT.NONE);
      }
      createMenu(_popupMenu, "Add item", e -> addItem(), SWT.NONE);
      _addCharacter = createMenu(_popupMenu, "Add character", e -> addCharacter(), SWT.NONE);
      _removeItems = createMenu(_popupMenu, "Remove all items", e -> addRemoveItems(), SWT.NONE);
      {
         MenuItem _terrainMenuItem = createMenu(_popupMenu, "Terrain", null, SWT.CASCADE);
         Menu subMenuEditTerrain = new Menu(_terrainMenuItem);
         _terrainMenuItem.setMenu(subMenuEditTerrain);
         for (TerrainType terrainType : TerrainType.values()) {
            _setTerrain.put(terrainType, createMenu(subMenuEditTerrain, terrainType.name(),
                                                    e -> setTerrain(terrainType), SWT.CHECK));
         }
      }
   }
   private void createMoveToMenu(Shell shell) {
      _moveToPopupMenu = new Menu(shell);
      createMenu(_moveToPopupMenu, "Relocate to here, facing 12 O'Clock", e -> completeMove(Facing.NOON), SWT.NONE);
      createMenu(_moveToPopupMenu, "Relocate to here, facing  2 O'Clock", e -> completeMove(Facing._2_OCLOCK), SWT.NONE);
      createMenu(_moveToPopupMenu, "Relocate to here, facing  4 O'Clock", e -> completeMove(Facing._4_OCLOCK), SWT.NONE);
      createMenu(_moveToPopupMenu, "Relocate to here, facing  6 O'Clock", e -> completeMove(Facing._6_OCLOCK), SWT.NONE);
      createMenu(_moveToPopupMenu, "Relocate to here, facing  8 O'Clock", e -> completeMove(Facing._8_OCLOCK), SWT.NONE);
      createMenu(_moveToPopupMenu, "Relocate to here, facing 10 O'Clock", e -> completeMove(Facing._10_OCLOCK), SWT.NONE);
      createMenu(_moveToPopupMenu, "Cancel Relocation", e -> _moveFromLocation = null, SWT.NONE);
   }

   private void completeMove(Facing facing) {
      Character character = getCharacter();
      if (character != null) {
         Orientation orient = character.getOrientation();
         if (orient.setHeadLocation(character, _currentMouseLoc, facing, _arena.getCombatMap(), null, true)) {
            _moveFromLocation = null;
         }
      }
   }

   private void setTerrain(TerrainType terrainType) {
      _currentMouseLoc.setTerrain(terrainType);
   }

   static private MenuItem createMenu(Menu parent, String text, Listener listener, int style) {
      MenuItem item = new MenuItem(parent, style);
      item.setText(text);
      if (listener != null) {
         item.addListener(SWT.Selection, listener);
      }
      return item;
   }

   private void addRemoveItems() {
      // be sure we don't remove any characters
      for (Object thing : new ArrayList<>(_currentMouseLoc.getThings())) {
         if (!(thing instanceof Character)) {
            _currentMouseLoc.remove(thing);
         }
      }
   }

   private void addItem() {
      openDialog(Display.getCurrent(), "Add Item", "Item to add:", "", false /*multiLine*/,
                 results -> {
                    Thing thing = Thing.getThing(results, Race.getRace(Race.NAME_Human, Race.Gender.MALE));
                    if (thing != null) {
                       _currentMouseLoc.addThing(thing);
                    }
                 });
   }

   private void changeTeam(String newTeamName) {
      Character character = getCharacter();
      if (character != null) {
         byte team = (byte) 0;
         for (String teamName : Enums.TEAM_NAMES) {
            if (teamName.equals(newTeamName)) {
               Character originalCharacter = character.clone();
               character._teamID = team;
               ObjectChanged changeNotif = new ObjectChanged(originalCharacter, character);
               character.notifyWatchers(originalCharacter, character, changeNotif, null/*skipList*/, null/*diag*/);
               return;
            }
            team++;
         }
         this._arena.recomputeAllTargets(character);
      }
   }

   private void addCharacter() {
      Character character = CombatServer._this._charWidget._character.clone();
      byte team = 0;
      this._arena.addCombatant(character, team, this._currentMouseLoc._x, this._currentMouseLoc._y, null);
      this._currentMouseLoc.addThing(character);
   }

   private void editCharacter(boolean loadIntoEditor) {
      Character character = getCharacter();
      if (character != null) {
         if (loadIntoEditor) {
            // set the edit page to this character
            CombatServer._this._charWidget._character.copyData(character);
            CombatServer._this._charWidget.updateDisplayFromCharacter();
         }
         else {
            // set this character to the edit page
            Character originalCharacter = character.clone();
            character.copyData(CombatServer._this._charWidget._character);

            ObjectChanged changeNotif = new ObjectChanged(originalCharacter, character);
            character.notifyWatchers(originalCharacter, character, changeNotif, null/*skipList*/, null/*diag*/);
         }
      }
   }

   private void editCharacterCondition() {
      Character character = getCharacter();
      if (character != null) {
         Character originalCharacter = character.clone();
         // Create a builder factory
         final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(true/*validating*/);

         // Create the builder and parse the file
         try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document charConditionDoc = builder.newDocument();
            Element mainElement = originalCharacter.getCondition().getXMLObject(charConditionDoc, "\n");

            DOMImplementationLS domImplLS = (DOMImplementationLS) charConditionDoc.getImplementation();
            LSSerializer serializer = domImplLS.createLSSerializer();
            String xmlStr = serializer.writeToString(mainElement);

            // Open a dialog to edit the condition
            openDialog(Display.getCurrent(), "Edit Condition", "Player condition:", xmlStr, true /*multiLine*/,
                       results -> {
                          InputSource src = new InputSource(new StringReader(results));
                          try {
                             Document doc = factory.newDocumentBuilder().parse(src);
                             character.getCondition().serializeFromXmlObject(doc.getFirstChild());

                             ObjectChanged changeNotif = new ObjectChanged(originalCharacter, character);
                             character.notifyWatchers(originalCharacter, character, changeNotif, null/*skipList*/, null/*diag*/);
                          } catch (SAXException | IOException | ParserConfigurationException e) {
                             e.printStackTrace();
                          }
                       });

         } catch (ParserConfigurationException e) {
            e.printStackTrace();
         }
      }
   }

   private void removeCharacter() {
      Character character = getCharacter();
      if (character != null) {
         this._arena.removeCombatant(character);
         this._currentMouseLoc.remove(character);
      }
   }

   private void changeControl(AI_Type ai) {
      Character character = getCharacter();
      if (character != null) {
         this._arena.setControl(character, true/*localControl*/, ai);
         this._arena.recomputeAllTargets(character);
      }
   }
   private void changeToRemoteConnection() {
      Character character = getCharacter();
      if (character != null) {
         this._arena.setControl(character, false/*localControl*/, null);
      }
   }

   private Character getCharacter() {
      ArenaLocation loc = (_moveFromLocation == null) ? _currentMouseLoc : _moveFromLocation;
      if (loc != null) {
         List<Character> chars = loc.getCharacters();
         if (!chars.isEmpty()) {
            return chars.get(0);
         }
      }
      return null;
   }

   static private void openDialog(Display display, String title, String prompt, String contents,
                                    boolean multiline, IActOnTextResults actOnResult) {
      final Shell shell = new Shell (display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
      shell.setText(title);
      FormLayout formLayout = new FormLayout ();
      formLayout.marginWidth = 10;
      formLayout.marginHeight = 10;
      formLayout.spacing = 10;
      shell.setLayout (formLayout);

      Label label = new Label (shell, SWT.NONE);
      {
         label.setText(prompt);
         FormData labelFormData = new FormData();
         labelFormData.left = new FormAttachment(0); // left side of parent window
         label.setLayoutData(labelFormData);
      }
      Button cancel = new Button(shell, SWT.PUSH);
      {
         cancel.setText("Cancel");
         FormData cancelFormData = new FormData();
         cancelFormData.width = 60;
         cancelFormData.right = new FormAttachment(100); // right side of button attaches at far right of window
         cancelFormData.bottom = new FormAttachment(100); // bottom side of button attaches at bottom of window
         cancel.setLayoutData(cancelFormData);
         cancel.addListener(SWT.Selection, e -> shell.close());
      }

      int style = SWT.BORDER | ((multiline) ? (SWT.MULTI | SWT.WRAP | SWT.V_SCROLL) : 0);
      Text text = new Text(shell, style);
      {
         FormData textFormData = new FormData();
         textFormData.width = ((contents != null) && contents.length() > 200) ? 500 : 200;
         textFormData.left = new FormAttachment(label, 0, SWT.RIGHT); // left side of text attached to right side of label
         textFormData.right = new FormAttachment(100); // right side of dialog
         //textFormData.top = new FormAttachment(label, 0, SWT.CENTER);
         textFormData.top = new FormAttachment(0); // top attaches to top of dialog
         textFormData.bottom = new FormAttachment(cancel, 0, SWT.TOP);
         text.setLayoutData(textFormData);
         text.setText(contents);
      }
      {
         Button ok = new Button (shell, SWT.PUSH);
         ok.setText ("OK");
         FormData okFormData = new FormData();
         okFormData.width = 60;
         okFormData.right = new FormAttachment(cancel, 0, SWT.DEFAULT);
         okFormData.bottom = new FormAttachment(100, 0);
         ok.setLayoutData(okFormData);
         ok.addListener(SWT.Selection, e -> {
            actOnResult.doAction(text.getText());
            shell.close();
         });
         shell.setDefaultButton (ok);
      }
      shell.pack ();
      shell.open ();
   }
}
