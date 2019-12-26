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
import ostrowski.combat.common.enums.TerrainType;
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
   private Menu _characterPopupMenu;

   private MenuItem _changeControlMenuItem;
   private final Map<AI_Type, MenuItem> _controlMenuSubItem = new HashMap<>();
   private MenuItem _aiLocal;
   private MenuItem _aiRemote;
   private MenuItem _changeTeamMenuItem;
   private final Map<String, MenuItem> _teamMenuSubItem = new HashMap<>();
   private MenuItem _relocateCharacter;
   private MenuItem _removeCharacter;
   private MenuItem _loadCharacter;
   private MenuItem _saveCharacter;
   private MenuItem _editCharacterCondition;
   private MenuItem _addCharacter;
   private MenuItem _addItem;
   private MenuItem _removeItems;
   private MenuItem _terrainMenuItem;
   private final Map<TerrainType, MenuItem> _setTerrain = new HashMap<>();

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
         if ((_characterPopupMenu == null) || _characterPopupMenu.isDisposed()) {
            _characterPopupMenu = new Menu(shell);
            {
               _changeControlMenuItem = createMenu(_characterPopupMenu, "Change Control", null, SWT.CASCADE);
               Menu subMenuChangeControl = new Menu(_changeControlMenuItem);
               _changeControlMenuItem.setMenu(subMenuChangeControl);
               _aiRemote = createMenu(subMenuChangeControl, "Remote Connection", e -> changeToRemoteConnection(), SWT.CHECK);
               _aiLocal = createMenu(subMenuChangeControl, "Local Control", e -> changeControl(null), SWT.CHECK);
               for (AI_Type ai : AI_Type.values()) {
                  _controlMenuSubItem.put(ai, createMenu(subMenuChangeControl, ai.name(),
                                                         e -> changeControl(ai), SWT.CHECK));
               }
            }
            {
               _changeTeamMenuItem = createMenu(_characterPopupMenu, "Change Team", null, SWT.CASCADE);
               Menu subMenuChangeTeam = new Menu(_changeTeamMenuItem);
               _changeTeamMenuItem.setMenu(subMenuChangeTeam);
               for (String teamName : Enums.TEAM_NAMES) {
                  _teamMenuSubItem.put(teamName, createMenu(subMenuChangeTeam, teamName,
                                                            e -> changeTeam(teamName), SWT.CHECK));
               }
            }

            _relocateCharacter = createMenu(_characterPopupMenu, "Relocate Character", e -> relocateCharacter(), SWT.NONE);
            _removeCharacter = createMenu(_characterPopupMenu, "Remove Character", e -> removeCharacter(), SWT.NONE);
            _loadCharacter = createMenu(_characterPopupMenu, "Load Character in editor",e -> editCharacter(true), SWT.NONE);
            _saveCharacter = createMenu(_characterPopupMenu, "Save Character from editor", e -> editCharacter(false), SWT.NONE);
            _editCharacterCondition = createMenu(_characterPopupMenu, "Edit Character Condition", e -> editCharacterCondition(), SWT.NONE);

            _addCharacter = createMenu(_characterPopupMenu, "Add Character", e -> addCharacter(), SWT.NONE);
            _addItem = createMenu(_characterPopupMenu, "Add Item", e -> addItem(), SWT.NONE);
            {
               _terrainMenuItem = createMenu(_characterPopupMenu, "Terrain", null, SWT.CASCADE);
               Menu subMenuEditTerrain = new Menu(_terrainMenuItem);
               _terrainMenuItem.setMenu(subMenuEditTerrain);
               for (TerrainType terrainType : TerrainType.values()) {
                  _setTerrain.put(terrainType, createMenu(subMenuEditTerrain, terrainType.name(),
                                                          e -> setTerrain(terrainType), SWT.CHECK));
               }
            }
            _removeItems = createMenu(_characterPopupMenu, "Remove all items", e -> addRemoveItems(), SWT.NONE);
         }
         _characterPopupMenu.setVisible(false);
      }
      if (loc != null) {
         List<Character> characters = loc.getCharacters();
         List<Object> things = loc.getThings();
         boolean characterPresent = !characters.isEmpty();
         _changeControlMenuItem.setEnabled(characterPresent);
         _changeTeamMenuItem.setEnabled(characterPresent);
         _addCharacter.setEnabled(!characterPresent);
         _relocateCharacter.setEnabled(characterPresent);
         _loadCharacter.setEnabled(characterPresent);
         _saveCharacter.setEnabled(characterPresent);
         _removeCharacter.setEnabled(characterPresent);
         _editCharacterCondition.setEnabled(characterPresent);
         if (characterPresent) {
            Character character = loc.getCharacters().get(0);
            for (String teamName : Enums.TEAM_NAMES) {
               boolean selected = Enums.TEAM_NAMES[character._teamID].equals(teamName);
               _teamMenuSubItem.get(teamName).setSelection(selected);
            }

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
         _characterPopupMenu.setLocation(Display.getCurrent().getCursorLocation().x,
                                         Display.getCurrent().getCursorLocation().y);
         _characterPopupMenu.setVisible(true);
         _currentMouseLoc = loc;

         _removeItems.setEnabled(things.size() > characters.size());

         for (TerrainType terrainType : TerrainType.values()) {
            boolean selected = _currentMouseLoc.getTerrain() == terrainType;
            _setTerrain.get(terrainType).setSelection(selected);
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
      ArrayList<Object> things = new ArrayList<>();
      things.addAll(_currentMouseLoc.getThings());
      for (Object thing : things) {
         if (!(thing instanceof Character)) {
            _currentMouseLoc.remove(thing);
         }
      }
   }

   private void addItem() {
      openDialog(Display.getCurrent(), "Add Item", "Item to add:", "", false /*multiLine*/,
                 new IActOnTextResults() {
         @Override
         public void doAction(String results) {
            Thing thing = Thing.getThing(results, Race.getRace(Race.NAME_Human, Race.Gender.MALE));
            if (thing != null) {
               _currentMouseLoc.addThing(thing);
            }
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
         Document charConditionDoc = null;
         try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            charConditionDoc = builder.newDocument();
            Element mainElement = originalCharacter.getCondition().getXMLObject(charConditionDoc, "\n");

            DOMImplementationLS domImplLS = (DOMImplementationLS) charConditionDoc.getImplementation();
            LSSerializer serializer = domImplLS.createLSSerializer();
            String xmlStr = serializer.writeToString(mainElement);

            // Open a dialog to edit the condition
            openDialog(Display.getCurrent(), "Edit Condition", "Player condition:", xmlStr, true /*multiLine*/,
                       new IActOnTextResults() {
               @Override
               public void doAction(String results) {
                  InputSource src = new InputSource(new StringReader(results));
                  try {
                     Document doc = factory.newDocumentBuilder().parse(src);
                     character.getCondition().serializeFromXmlObject(doc.getFirstChild());

                     ObjectChanged changeNotif = new ObjectChanged(originalCharacter, character);
                     character.notifyWatchers(originalCharacter, character, changeNotif, null/*skipList*/, null/*diag*/);
                  } catch (SAXException | IOException | ParserConfigurationException e) {
                     e.printStackTrace();
                  }
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

   private void relocateCharacter() {
      Character character = getCharacter();
      if (character != null) {
      }
   }

   private void changeControl(AI_Type ai) {
      Character character = getCharacter();
      if (character != null) {
         this._arena.setControl(character, true/*localControl*/, ai);
      }
   }
   private void changeToRemoteConnection() {
      Character character = getCharacter();
      if (character != null) {
         this._arena.setControl(character, false/*localControl*/, null);
      }
   }

   private Character getCharacter() {
      if (_currentMouseLoc != null) {
         List<Character> chars = _currentMouseLoc.getCharacters();
         if (!chars.isEmpty()) {
            return chars.get(0);
         }
      }
      return null;
   }

   static private String openDialog(Display shell, String title, String prompt, String contents,
                                    boolean multiline, IActOnTextResults actOnResult) {
      final Shell dialog = new Shell (shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      dialog.setText(title);
      FormLayout formLayout = new FormLayout ();
      formLayout.marginWidth = 10;
      formLayout.marginHeight = 10;
      formLayout.spacing = 10;
      dialog.setLayout (formLayout);

      Label label = new Label (dialog, SWT.NONE);
      label.setText (prompt);
      FormData data = new FormData ();
      label.setLayoutData (data);

      Button cancel = new Button (dialog, SWT.PUSH);
      cancel.setText ("Cancel");
      data = new FormData ();
      data.width = 60;
      data.right = new FormAttachment (100, 0);
      data.bottom = new FormAttachment (100, 0);
      cancel.setLayoutData (data);
      cancel.addListener(SWT.Selection, new Listener() {
         @Override
         public void handleEvent(Event e) {
            dialog.close();
         }
      });

      int style = SWT.BORDER;
      if (multiline) {
         style = SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL;
      }
      final Text text = new Text(dialog, style);
      data = new FormData ();
      data.width = 200;
      data.left = new FormAttachment (label, 0, SWT.DEFAULT);
      data.right = new FormAttachment (100, 0);
      data.top = new FormAttachment (label, 0, SWT.CENTER);
      data.bottom = new FormAttachment (cancel, 0, SWT.DEFAULT);
      text.setLayoutData (data);
      text.setText(contents);

      Button ok = new Button (dialog, SWT.PUSH);
      ok.setText ("OK");
      data = new FormData ();
      data.width = 60;
      data.right = new FormAttachment (cancel, 0, SWT.DEFAULT);
      data.bottom = new FormAttachment (100, 0);
      ok.setLayoutData (data);
      ok.addListener(SWT.Selection, new Listener() {
         @Override
         public void handleEvent(Event e) {
            actOnResult.doAction(text.getText());
            dialog.close();
         }
      });

      dialog.setDefaultButton (ok);
      dialog.pack ();
      dialog.open ();
      return text.getText();
   }
}
