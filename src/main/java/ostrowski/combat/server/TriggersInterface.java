package ostrowski.combat.server;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.IMapWidget;
import ostrowski.combat.common.MapWidget2D;
import ostrowski.ui.Helper;

public class TriggersInterface extends Helper implements SelectionListener, ModifyListener, PaintListener
{
   private CombatMap  _map;
   private final ArrayList<IMapWidget> _mapWidgets = new ArrayList<>();
   private List       _triggersList;
   private Button     _addTrigger;
   private Button     _setTriggerLocationsButton;
   private Button     _deleteTrigger;
   private boolean    _setTriggerLocations = false;

   private Text       _triggerName;
   private Label      _triggerNameLabel;
   private Button     _triggerEnabled;
   private Label      _triggerEnabledLabel;
   private Button     _triggerRequiresEntireTeam;
   private Label      _triggerRequiresEntireTeamLabel;
   private Button     _triggerOnlyAffectsPlayers;
   private Label      _triggerOnlyAffectsPlayersLabel;

   private List       _eventsList;
   private Button     _addEvent;
   private Button     _setEventLocationsButton;
   private Button     _deleteEvent;
   private boolean    _setEventLocations   = false;

   private Text       _eventName;
   private Label      _eventNameLabel;
   private Label      _eventTypeLabel;
   private Combo      _eventTypeCombo;
   private Label      _eventArgumentLabel;
   private Text       _eventArgumentText;

   public void setMap(IMapWidget mapWidget) {
      if (mapWidget != null) {
         _mapWidgets.add(mapWidget);
      }
      setMap(mapWidget.getCombatMap());
   }
   public void setMap(CombatMap map) {
      _map = map;
      _triggersList.removeAll();
      if (map != null) {
         ArrayList<ArenaTrigger> triggers = map.getTriggers();
         if ((triggers != null) && (triggers.size() > 0)) {
            for (ArenaTrigger trigger : triggers) {
               _triggersList.add(trigger.getName());
            }
            _triggersList.setSelection(0);
            setCurrentTrigger(map.getTriggers().get(0));
            return;
         }
      }
      setCurrentTrigger(null);
   }

   public void buildBlock(Composite parentComposite)
   {
      Composite triggerButtonsBlock = new Composite(parentComposite, SWT.TRAIL);
      GridLayout grid = new GridLayout(4, false);
      triggerButtonsBlock.setLayout(grid);
      {
         Composite triggerButtonsLeftBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(1, false);
         triggerButtonsLeftBlock.setLayout(grid);
         _triggersList = createList(triggerButtonsLeftBlock);
         _addTrigger = createButton(triggerButtonsLeftBlock, "Add Trigger", 1/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
      }
      {
         Composite triggerButtonsMiddleLeftBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(2, false);
         triggerButtonsMiddleLeftBlock.setLayout(grid);
         _triggerNameLabel = createLabel(triggerButtonsMiddleLeftBlock, "Name:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         _triggerName = createText(triggerButtonsMiddleLeftBlock, "", true/*enabled*/, 1/*hSpen*/);

         _triggerEnabled = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         _triggerEnabled.setSelection(true);
         _triggerEnabledLabel = createLabel(triggerButtonsMiddleLeftBlock, "Enabled", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         _triggerRequiresEntireTeam = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         _triggerRequiresEntireTeamLabel = createLabel(triggerButtonsMiddleLeftBlock, "Requires Entire Team", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         _triggerOnlyAffectsPlayers = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         _triggerOnlyAffectsPlayersLabel = createLabel(triggerButtonsMiddleLeftBlock, "Doesn't affect AI", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         _setTriggerLocationsButton = createButton(triggerButtonsMiddleLeftBlock, "Set Location(s)", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         _deleteTrigger = createButton(triggerButtonsMiddleLeftBlock, "Delete Trigger", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         _setTriggerLocationsButton.addPaintListener(this);

         _triggerName.addModifyListener(this);
         _triggerName.addSelectionListener(this);
         _triggerEnabled.addSelectionListener(this);
         _triggerRequiresEntireTeam.addSelectionListener(this);
         _triggerOnlyAffectsPlayers.addSelectionListener(this);
      }
      {
         Composite triggerButtonsMiddleRightBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(1, false);
         triggerButtonsMiddleRightBlock.setLayout(grid);
         _eventsList = createList(triggerButtonsMiddleRightBlock);
         _addEvent = createButton(triggerButtonsMiddleRightBlock, "Add Event", 1/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
      }
      {
         Composite triggerButtonsRightBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(2, false);
         triggerButtonsRightBlock.setLayout(grid);

         _eventNameLabel = createLabel(triggerButtonsRightBlock, "Name:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         _eventName = createText(triggerButtonsRightBlock, "", true/*enabled*/, 1/*hSpen*/);

         _eventTypeLabel = createLabel(triggerButtonsRightBlock, "Type:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         _eventTypeCombo = createCombo(triggerButtonsRightBlock, SWT.READ_ONLY | SWT.DROP_DOWN, 1/*hSpen*/, ArenaEvent.EVENT_TYPES);

         _eventArgumentLabel = createLabel(triggerButtonsRightBlock, "Data:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         _eventArgumentText = createText(triggerButtonsRightBlock, "", true /*enabled*/, 1/*hSpen*/);

         _setEventLocationsButton = createButton(triggerButtonsRightBlock, "Set Location(s)", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         _deleteEvent = createButton(triggerButtonsRightBlock, "Delete Event", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         _setEventLocationsButton.addPaintListener(this);

         setEventEnabled(false);
         _eventName.addModifyListener(this);
         _eventName.addSelectionListener(this);

         _eventTypeCombo.addModifyListener(this);
         _eventArgumentText.addModifyListener(this);
      }
      setTriggerEnabled(false);
   }

   private List createList(Composite parent) {
      List list = new List(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
      GridData data = new GridData();
      data.verticalAlignment = GridData.FILL;
      data.horizontalAlignment = GridData.BEGINNING;
      data.grabExcessVerticalSpace = true;
      data.grabExcessHorizontalSpace = false;
      data.minimumHeight = 200;
      data.minimumWidth = 200;
      list.setLayoutData(data);
      list.addSelectionListener(this);
      return list;
   }
   private void setTriggerEnabled(boolean enabled) {
      _triggerName.setEnabled(enabled);
      _triggerNameLabel.setEnabled(enabled);
      _triggerEnabled.setEnabled(enabled);
      _triggerEnabledLabel.setEnabled(enabled);
      _triggerRequiresEntireTeam.setEnabled(enabled);
      _triggerRequiresEntireTeamLabel.setEnabled(enabled);
      _triggerOnlyAffectsPlayers.setEnabled(enabled);
      _triggerOnlyAffectsPlayersLabel.setEnabled(enabled);
      _deleteTrigger.setEnabled(enabled);
      _addEvent.setEnabled(enabled);
      if (!enabled) {
         setEventEnabled(false);
      }
      _setTriggerLocationsButton.setEnabled(enabled);
      if (!enabled) {
         _setTriggerLocations = false;
      }
      _setTriggerLocationsButton.redraw();
   }
   private void setEventEnabled(boolean enabled) {
      _eventsList.setEnabled(enabled);
      _eventNameLabel.setEnabled(enabled);
      _eventName.setEnabled(enabled);
      _eventTypeLabel.setEnabled(enabled);
      _eventTypeCombo.setEnabled(enabled);
      _eventArgumentLabel.setEnabled(enabled);
      _eventArgumentText.setEnabled(enabled);
      _deleteEvent.setEnabled(enabled);
      _setEventLocationsButton.setEnabled(enabled);
      if (!enabled) {
         _setEventLocations = false;
      }
      _setEventLocationsButton.redraw();
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _addTrigger) {
         ArenaTrigger newTrigger = new ArenaTrigger("New Trigger");
         // assume this is a single-shot trigger
         ArenaEvent singleShot = new ArenaEvent("single shot");
         singleShot.setType(ArenaEvent.EVENT_TYPE_DISABLE_THIS_TRIGGER);
         newTrigger.getEvents().add(singleShot);
         _map.getTriggers().add(newTrigger);
         _triggersList.add(newTrigger.getName());
         _triggersList.select(_triggersList.getItemCount()-1);
         if (_map != null) {
            _map.setSelectedTrigger(newTrigger);
         }
         for (IMapWidget mapWidget : _mapWidgets) {
            mapWidget.redraw();
         }
         _triggerName.setText(newTrigger.getName());
         _triggerEnabled.setSelection(newTrigger.getEnabled());
         setCurrentTrigger(newTrigger);
         _triggerRequiresEntireTeam.setSelection(newTrigger.getRequiresEntireTeam());
         _triggerOnlyAffectsPlayers.setSelection(newTrigger.getOnlyAffectsPlayers());
      }
      else if (e.widget == _setTriggerLocationsButton) {
         _setTriggerLocations = !_setTriggerLocations;
         _setEventLocations = false;
         _setEventLocationsButton.redraw();
         _setTriggerLocationsButton.redraw();
      }
      else if (e.widget == _setEventLocationsButton) {
         _setEventLocations = !_setEventLocations;
         _setTriggerLocations = false;
         _setTriggerLocationsButton.redraw();
         _setEventLocationsButton.redraw();
      }
      else if (e.widget == _deleteTrigger) {
         ArenaTrigger selectedTrigger = getCurrentlySelectedTrigger();
         for (ArenaTrigger trigger : _map.getTriggers()) {
            if (selectedTrigger.getName().equals(trigger.getName())) {
               _map.getTriggers().remove(trigger);
               break;
            }
         }
         _triggersList.remove(selectedTrigger.getName());
         // select the last item in the list
         int index = _triggersList.getItemCount()-1;
         _triggersList.select(index);
         ArenaTrigger trigger = null;
         if (index >= 0) {
            trigger = _map.getTriggers().get(index);
         }
         if (_map != null) {
            _map.setSelectedTrigger(trigger);
         }
         for (IMapWidget mapWidget : _mapWidgets) {
            mapWidget.redraw();
         }
         _triggerName.setText(trigger==null ? "" : trigger.getName());
         _triggerEnabled.setSelection(trigger != null && trigger.getEnabled());
         _triggerRequiresEntireTeam.setSelection(trigger != null && trigger.getRequiresEntireTeam());
         _triggerOnlyAffectsPlayers.setSelection(trigger != null && trigger.getOnlyAffectsPlayers());
         setCurrentTrigger(trigger);
      }
      else if (e.widget == _deleteEvent) {
         ArenaTrigger selectedTrigger = getCurrentlySelectedTrigger();
         ArenaEvent selectedEvent = getCurrentlySelectedEvent();
         if ((selectedTrigger != null) && (selectedEvent != null)) {
            selectedTrigger.getEvents().remove(selectedEvent);
            _eventsList.remove(selectedEvent.getName());
            // select the last item in the list
            int index = _eventsList.getItemCount()-1;
            _eventsList.select(index);
            ArenaTrigger trigger = null;
            if (_map != null) {
               _map.setSelectedTrigger(trigger);
            }
            for (IMapWidget mapWidget : _mapWidgets) {
               mapWidget.redraw();
            }
            if (index >= 0) {
               setEvent(selectedTrigger.getEvents().get(index));
            }
            else {
               setEvent(null);
               setEventEnabled(false);
            }
         }
      }
      else if (e.widget == _addEvent) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         if (trigger != null) {
            ArenaEvent newEvent = new ArenaEvent("New Event");
            trigger.getEvents().add(newEvent);
            _eventsList.add(newEvent.getName());
            _eventsList.select(_eventsList.getItemCount()-1);
            setEvent(newEvent);
            setEventEnabled(true);
         }
      }
      else if (e.widget == _triggersList) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         setCurrentTrigger(trigger);
      }
      else if (e.widget == _eventsList) {
         ArenaEvent event = getCurrentlySelectedEvent();
         if (event != null) {
            setEvent(event);
         }
      }
      else if ((e.widget == _triggerEnabled)       ||
               (e.widget == _triggerRequiresEntireTeam) ||
               (e.widget == _triggerOnlyAffectsPlayers) ) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         if (trigger != null) {
            trigger.setEnabled(_triggerEnabled.getSelection());
            trigger.setRequiresEntireTeam(_triggerRequiresEntireTeam.getSelection());
            trigger.setOnlyAffectsPlayers(_triggerOnlyAffectsPlayers.getSelection());
         }
      }
      CombatServer._this.refreshSaveButton();
   }

   private void setEvent(ArenaEvent event) {
      _eventName.setText((event==null) ? "" : event.getName());
      _eventTypeCombo.setText((event==null) ? "" : event.getType());
      _eventArgumentText.setText((event==null) ? "" : event.getData());
   }

   private void setCurrentTrigger(ArenaTrigger trigger) {
      ArenaTrigger oldTrigger = null;
      if (_map != null) {
         oldTrigger = _map.getSelectedTrigger();
         _map.setSelectedTrigger(trigger);
      }
      boolean contains2Dmap = false;
      for (IMapWidget mapWidget : _mapWidgets) {
         if (mapWidget instanceof MapWidget2D) {
            contains2Dmap = true;
            break;
         }
      }

      if (contains2Dmap){
         ArrayList<ArenaCoordinates> locs = new ArrayList<>();
         if (oldTrigger != null) {
            locs.addAll(oldTrigger.getTriggerCoordinates());
            for (ArenaEvent event : oldTrigger.getEvents()) {
               Collection<ArenaCoordinates> eventLocations = event.getLocations();
               if (eventLocations != null) {
                  locs.addAll(eventLocations);
               }
            }
         }
         if (trigger != null) {
            locs.addAll(trigger.getTriggerCoordinates());
            for (ArenaEvent event : trigger.getEvents()) {
               Collection<ArenaCoordinates> eventLocations = event.getLocations();
               if (eventLocations != null) {
                  locs.addAll(eventLocations);
               }
            }
         }
         for (IMapWidget mapWidget : _mapWidgets) {
            if (mapWidget instanceof MapWidget2D) {
               ((MapWidget2D)mapWidget).redraw(locs);
            }
         }
      }

      setEventEnabled(trigger != null);
      setTriggerEnabled(trigger != null);
      _triggerName.setText((trigger != null) ? trigger.getName() : "");
      _triggerEnabled.setSelection((trigger != null) && trigger.getEnabled());
      _triggerRequiresEntireTeam.setSelection((trigger != null) && trigger.getRequiresEntireTeam());
      _triggerOnlyAffectsPlayers.setSelection((trigger != null) && trigger.getOnlyAffectsPlayers());
      _eventsList.removeAll();
      setEvent(null);
      setEventEnabled(false);

      if (trigger != null) {
         ArrayList<ArenaEvent> events = trigger.getEvents();
         boolean eventsExist = ((events != null) && (events.size() > 0));
         if (eventsExist) {
            ArenaEvent firstEvent = null;
            for (ArenaEvent event : events) {
               if (firstEvent == null) {
                  firstEvent = event;
               }
               _eventsList.add(event.getName());
            }
            _eventsList.setSelection(0);
            if (firstEvent != null) {
               setEvent(firstEvent);
               setEventEnabled(true);
            }
         }
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (!CombatServer._inModify) {
         CombatServer._inModify = true;
         try {
            if (e.widget == _triggerName) {
               ArenaTrigger trigger = getCurrentlySelectedTrigger();
               if (trigger != null) {
                  int index = _triggersList.getSelectionIndex();
                  _triggersList.setItem(index, _triggerName.getText());
                  trigger.setName(_triggerName.getText());
               }
            }
            else if ((e.widget == _eventName)      ||
                     (e.widget == _eventTypeCombo) ||
                     (e.widget == _eventArgumentText)) {
               ArenaEvent event = getCurrentlySelectedEvent();
               if (event != null) {
                  int index = _eventsList.getSelectionIndex();
                  _eventsList.setItem(index, _eventName.getText());
                  if (e.widget == _eventName) {
                     event.setName(_eventName.getText());
                  }
                  if (e.widget == _eventTypeCombo) {
                     event.setType(_eventTypeCombo.getText());
                  }
                  if (e.widget == _eventArgumentText) {
                     event.setData(_eventArgumentText.getText());
                  }
               }
            }
            CombatServer._this.refreshSaveButton();
         }
         finally {
            CombatServer._inModify = false;
         }
      }
   }

   private ArenaEvent getCurrentlySelectedEvent() {
      ArenaTrigger trigger = getCurrentlySelectedTrigger();
      if (trigger != null) {
         String[] selections = _eventsList.getSelection();
         if (selections.length == 1) {
            String selection = selections[0];
            for (ArenaEvent event : trigger.getEvents()) {
               if (event.getName().equals(selection)) {
                  return event;
               }
            }
         }
      }
      return null;
   }
   public ArenaTrigger getCurrentlySelectedTrigger() {
      String[] selections = _triggersList.getSelection();
      if (selections.length == 1) {
         String selection = selections[0];
         for (ArenaTrigger trigger : _map.getTriggers()) {
            if (trigger.getName().equals(selection)) {
               return trigger;
            }
         }
      }
      return null;
   }
   @Override
   public void paintControl(PaintEvent event) {
      if (((event.widget == _setEventLocationsButton) && _setEventLocations)
          || ((event.widget == _setTriggerLocationsButton) && _setTriggerLocations)) {
         RGB fillColor = new RGB(255, 192, 192);
         if (_setEventLocations) {
            fillColor = new RGB(192, 192, 255);
         }
         RGB textColor = MapWidget2D.invertColor(fillColor);
         Color bgColor = new Color(event.display, fillColor);
         Color fgColor = new Color(event.display, textColor);
         event.gc.setBackground(bgColor);
         event.gc.setForeground(fgColor);
         event.gc.fillRectangle(2, 2, event.width-4, event.height-4);

         String text = ((Button)event.widget).getText();
         Point extent = event.gc.stringExtent(text);
         int xOffset = (event.width-extent.x)/2;
         int yOffset = (event.height-extent.y)/2;
         event.gc.drawText(text, xOffset, yOffset);
         if (bgColor != null) {
            bgColor.dispose();
         }
         if (fgColor != null) {
            fgColor.dispose();
         }
      }
   }

   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter,
                           IMapWidget mapWidget, ArrayList<ArenaCoordinates> locationsToRedraw) {
   }
   public void onMouseUp(ArenaLocation loc, Event mouseUpEvent, double angleFromCenter, double normalizedDistFromCenter, CombatMap combatMap, ArrayList<ArenaCoordinates> locationsToRedraw) {
      if (loc == null) {
         return;
      }
      boolean redrawLoc = false;
      if (_setEventLocations) {
         ArenaEvent event = getCurrentlySelectedEvent();
         if (event != null) {
            if (event._eventLocations == null) {
               event._eventLocations = new ArrayList<>();
            }
            if (event._eventLocations.contains(loc)) {
               event._eventLocations.remove(loc);
            }
            else if (event._eventLocations.contains(loc)) {
               event._eventLocations.remove(loc);
            }
            else {
               event._eventLocations.add(loc);
            }
            redrawLoc = true;
         }
      }
      if (_setTriggerLocations) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         if (trigger != null) {
            if (trigger.isTriggerAtLocation(loc, null/*mover*/)) {
               trigger.removeTriggerAtLocation(loc);
            }
            else {
               trigger.addTriggerAtLocation(loc);
            }
            redrawLoc = true;
         }
      }
      if (redrawLoc && (locationsToRedraw != null)) {
         locationsToRedraw.add(loc);
      }
   }
}
