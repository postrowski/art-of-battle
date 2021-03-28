package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.CombatMap;
import ostrowski.combat.common.IMapWidget;
import ostrowski.combat.common.MapWidget2D;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.Collection;

public class TriggersInterface extends Helper implements SelectionListener, ModifyListener, PaintListener
{
   private       CombatMap                  map;
   private final java.util.List<IMapWidget> mapWidgets          = new ArrayList<>();
   private       List                       triggersList;
   private       Button                     addTrigger;
   private       Button                     setTriggerLocationsButton;
   private       Button                     deleteTrigger;
   private       boolean                    setTriggerLocations = false;
   private       Text                       triggerName;
   private       Label                      triggerNameLabel;
   private       Button                     triggerEnabled;
   private       Label                      triggerEnabledLabel;
   private       Button                     triggerRequiresEntireTeam;
   private       Label                      triggerRequiresEntireTeamLabel;
   private       Button                     triggerOnlyAffectsPlayers;
   private       Label                      triggerOnlyAffectsPlayersLabel;
   private       List                       eventsList;
   private       Button                     addEvent;
   private       Button                     setEventLocationsButton;
   private       Button                     deleteEvent;
   private       boolean                    setEventLocations   = false;
   private       Text                       eventName;
   private       Label                      eventNameLabel;
   private       Label                      eventTypeLabel;
   private       Combo                      eventTypeCombo;
   private       Label                      eventArgumentLabel;
   private       Text                       eventArgumentText;

   public void setMap(IMapWidget mapWidget) {
      if (mapWidget != null) {
         mapWidgets.add(mapWidget);
         setMap(mapWidget.getCombatMap());
      }
   }
   public void setMap(CombatMap map) {
      this.map = map;
      triggersList.removeAll();
      if (map != null) {
         java.util.List<ArenaTrigger> triggers = map.getTriggers();
         if ((triggers != null) && (triggers.size() > 0)) {
            for (ArenaTrigger trigger : triggers) {
               triggersList.add(trigger.getName());
            }
            triggersList.setSelection(0);
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
         triggersList = createList(triggerButtonsLeftBlock);
         addTrigger = createButton(triggerButtonsLeftBlock, "Add Trigger", 1/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
      }
      {
         Composite triggerButtonsMiddleLeftBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(2, false);
         triggerButtonsMiddleLeftBlock.setLayout(grid);
         triggerNameLabel = createLabel(triggerButtonsMiddleLeftBlock, "Name:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         triggerName = createText(triggerButtonsMiddleLeftBlock, "", true/*enabled*/, 1/*hSpen*/);

         triggerEnabled = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         triggerEnabled.setSelection(true);
         triggerEnabledLabel = createLabel(triggerButtonsMiddleLeftBlock, "Enabled", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         triggerRequiresEntireTeam = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         triggerRequiresEntireTeamLabel = createLabel(triggerButtonsMiddleLeftBlock, "Requires Entire Team", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         triggerOnlyAffectsPlayers = new Button(triggerButtonsMiddleLeftBlock, SWT.CHECK);
         triggerOnlyAffectsPlayersLabel = createLabel(triggerButtonsMiddleLeftBlock, "Doesn't affect AI", SWT.LEFT, 1/*hSpan*/, null/*fontData*/);

         setTriggerLocationsButton = createButton(triggerButtonsMiddleLeftBlock, "Set Location(s)", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         deleteTrigger = createButton(triggerButtonsMiddleLeftBlock, "Delete Trigger", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         setTriggerLocationsButton.addPaintListener(this);

         triggerName.addModifyListener(this);
         triggerName.addSelectionListener(this);
         triggerEnabled.addSelectionListener(this);
         triggerRequiresEntireTeam.addSelectionListener(this);
         triggerOnlyAffectsPlayers.addSelectionListener(this);
      }
      {
         Composite triggerButtonsMiddleRightBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(1, false);
         triggerButtonsMiddleRightBlock.setLayout(grid);
         eventsList = createList(triggerButtonsMiddleRightBlock);
         addEvent = createButton(triggerButtonsMiddleRightBlock, "Add Event", 1/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
      }
      {
         Composite triggerButtonsRightBlock = new Composite(triggerButtonsBlock, SWT.TRAIL);
         grid = new GridLayout(2, false);
         triggerButtonsRightBlock.setLayout(grid);

         eventNameLabel = createLabel(triggerButtonsRightBlock, "Name:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         eventName = createText(triggerButtonsRightBlock, "", true/*enabled*/, 1/*hSpen*/);

         eventTypeLabel = createLabel(triggerButtonsRightBlock, "Type:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         eventTypeCombo = createCombo(triggerButtonsRightBlock, SWT.READ_ONLY | SWT.DROP_DOWN, 1/*hSpen*/, ArenaEvent.EVENT_TYPES);

         eventArgumentLabel = createLabel(triggerButtonsRightBlock, "Data:", SWT.RIGHT, 1/*hSpan*/, null/*fontData*/);
         eventArgumentText = createText(triggerButtonsRightBlock, "", true /*enabled*/, 1/*hSpen*/);

         setEventLocationsButton = createButton(triggerButtonsRightBlock, "Set Location(s)", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         deleteEvent = createButton(triggerButtonsRightBlock, "Delete Event", 2/*hSpan*/, null/*fontData*/, this/*SelectionListener*/);
         setEventLocationsButton.addPaintListener(this);

         setEventEnabled(false);
         eventName.addModifyListener(this);
         eventName.addSelectionListener(this);

         eventTypeCombo.addModifyListener(this);
         eventArgumentText.addModifyListener(this);
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
      triggerName.setEnabled(enabled);
      triggerNameLabel.setEnabled(enabled);
      triggerEnabled.setEnabled(enabled);
      triggerEnabledLabel.setEnabled(enabled);
      triggerRequiresEntireTeam.setEnabled(enabled);
      triggerRequiresEntireTeamLabel.setEnabled(enabled);
      triggerOnlyAffectsPlayers.setEnabled(enabled);
      triggerOnlyAffectsPlayersLabel.setEnabled(enabled);
      deleteTrigger.setEnabled(enabled);
      addEvent.setEnabled(enabled);
      if (!enabled) {
         setEventEnabled(false);
      }
      setTriggerLocationsButton.setEnabled(enabled);
      if (!enabled) {
         setTriggerLocations = false;
      }
      setTriggerLocationsButton.redraw();
   }
   private void setEventEnabled(boolean enabled) {
      eventsList.setEnabled(enabled);
      eventNameLabel.setEnabled(enabled);
      eventName.setEnabled(enabled);
      eventTypeLabel.setEnabled(enabled);
      eventTypeCombo.setEnabled(enabled);
      eventArgumentLabel.setEnabled(enabled);
      eventArgumentText.setEnabled(enabled);
      deleteEvent.setEnabled(enabled);
      setEventLocationsButton.setEnabled(enabled);
      if (!enabled) {
         setEventLocations = false;
      }
      setEventLocationsButton.redraw();
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == addTrigger) {
         ArenaTrigger newTrigger = new ArenaTrigger("New Trigger");
         // assume this is a single-shot trigger
         ArenaEvent singleShot = new ArenaEvent("single shot");
         singleShot.setType(ArenaEvent.EVENT_TYPE_DISABLE_THIS_TRIGGER);
         newTrigger.getEvents().add(singleShot);
         map.getTriggers().add(newTrigger);
         triggersList.add(newTrigger.getName());
         triggersList.select(triggersList.getItemCount() - 1);
         if (map != null) {
            map.setSelectedTrigger(newTrigger);
         }
         for (IMapWidget mapWidget : mapWidgets) {
            mapWidget.redraw();
         }
         triggerName.setText(newTrigger.getName());
         triggerEnabled.setSelection(newTrigger.getEnabled());
         setCurrentTrigger(newTrigger);
         triggerRequiresEntireTeam.setSelection(newTrigger.getRequiresEntireTeam());
         triggerOnlyAffectsPlayers.setSelection(newTrigger.getOnlyAffectsPlayers());
      }
      else if (e.widget == setTriggerLocationsButton) {
         setTriggerLocations = !setTriggerLocations;
         setEventLocations = false;
         setEventLocationsButton.redraw();
         setTriggerLocationsButton.redraw();
      }
      else if (e.widget == setEventLocationsButton) {
         setEventLocations = !setEventLocations;
         setTriggerLocations = false;
         setTriggerLocationsButton.redraw();
         setEventLocationsButton.redraw();
      }
      else if (e.widget == deleteTrigger) {
         ArenaTrigger selectedTrigger = getCurrentlySelectedTrigger();
         for (ArenaTrigger trigger : map.getTriggers()) {
            if (selectedTrigger.getName().equals(trigger.getName())) {
               map.getTriggers().remove(trigger);
               break;
            }
         }
         triggersList.remove(selectedTrigger.getName());
         // select the last item in the list
         int index = triggersList.getItemCount() - 1;
         triggersList.select(index);
         ArenaTrigger trigger = null;
         if (index >= 0) {
            trigger = map.getTriggers().get(index);
         }
         if (map != null) {
            map.setSelectedTrigger(trigger);
         }
         for (IMapWidget mapWidget : mapWidgets) {
            mapWidget.redraw();
         }
         triggerName.setText(trigger == null ? "" : trigger.getName());
         triggerEnabled.setSelection(trigger != null && trigger.getEnabled());
         triggerRequiresEntireTeam.setSelection(trigger != null && trigger.getRequiresEntireTeam());
         triggerOnlyAffectsPlayers.setSelection(trigger != null && trigger.getOnlyAffectsPlayers());
         setCurrentTrigger(trigger);
      }
      else if (e.widget == deleteEvent) {
         ArenaTrigger selectedTrigger = getCurrentlySelectedTrigger();
         ArenaEvent selectedEvent = getCurrentlySelectedEvent();
         if ((selectedTrigger != null) && (selectedEvent != null)) {
            selectedTrigger.getEvents().remove(selectedEvent);
            eventsList.remove(selectedEvent.getName());
            // select the last item in the list
            int index = eventsList.getItemCount() - 1;
            eventsList.select(index);
            if (map != null) {
               map.setSelectedTrigger(null);
            }
            for (IMapWidget mapWidget : mapWidgets) {
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
      else if (e.widget == addEvent) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         if (trigger != null) {
            ArenaEvent newEvent = new ArenaEvent("New Event");
            trigger.getEvents().add(newEvent);
            eventsList.add(newEvent.getName());
            eventsList.select(eventsList.getItemCount() - 1);
            setEvent(newEvent);
            setEventEnabled(true);
         }
      }
      else if (e.widget == triggersList) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         setCurrentTrigger(trigger);
      }
      else if (e.widget == eventsList) {
         ArenaEvent event = getCurrentlySelectedEvent();
         if (event != null) {
            setEvent(event);
         }
      }
      else if ((e.widget == triggerEnabled) ||
               (e.widget == triggerRequiresEntireTeam) ||
               (e.widget == triggerOnlyAffectsPlayers) ) {
         ArenaTrigger trigger = getCurrentlySelectedTrigger();
         if (trigger != null) {
            trigger.setEnabled(triggerEnabled.getSelection());
            trigger.setRequiresEntireTeam(triggerRequiresEntireTeam.getSelection());
            trigger.setOnlyAffectsPlayers(triggerOnlyAffectsPlayers.getSelection());
         }
      }
      CombatServer._this.mapInterface.refreshSaveButton();
   }

   private void setEvent(ArenaEvent event) {
      eventName.setText((event == null) ? "" : event.getName());
      eventTypeCombo.setText((event == null) ? "" : event.getType());
      eventArgumentText.setText((event == null) ? "" : event.getData());
   }

   private void setCurrentTrigger(ArenaTrigger trigger) {
      ArenaTrigger oldTrigger = null;
      if (map != null) {
         oldTrigger = map.getSelectedTrigger();
         map.setSelectedTrigger(trigger);
      }
      boolean contains2Dmap = false;
      for (IMapWidget mapWidget : mapWidgets) {
         if (mapWidget instanceof MapWidget2D) {
            contains2Dmap = true;
            break;
         }
      }

      if (contains2Dmap){
         java.util.List<ArenaCoordinates> locs = new ArrayList<>();
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
         for (IMapWidget mapWidget : mapWidgets) {
            if (mapWidget instanceof MapWidget2D) {
               ((MapWidget2D)mapWidget).redraw(locs);
            }
         }
      }

      setEventEnabled(trigger != null);
      setTriggerEnabled(trigger != null);
      triggerName.setText((trigger != null) ? trigger.getName() : "");
      triggerEnabled.setSelection((trigger != null) && trigger.getEnabled());
      triggerRequiresEntireTeam.setSelection((trigger != null) && trigger.getRequiresEntireTeam());
      triggerOnlyAffectsPlayers.setSelection((trigger != null) && trigger.getOnlyAffectsPlayers());
      eventsList.removeAll();
      setEvent(null);
      setEventEnabled(false);

      if (trigger != null) {
         java.util.List<ArenaEvent> events = trigger.getEvents();
         boolean eventsExist = ((events != null) && (events.size() > 0));
         if (eventsExist) {
            ArenaEvent firstEvent = null;
            for (ArenaEvent event : events) {
               if (firstEvent == null) {
                  firstEvent = event;
               }
               eventsList.add(event.getName());
            }
            eventsList.setSelection(0);
            setEvent(firstEvent);
            setEventEnabled(true);
         }
      }
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (!CombatServer.inModify) {
         CombatServer.inModify = true;
         try {
            if (e.widget == triggerName) {
               ArenaTrigger trigger = getCurrentlySelectedTrigger();
               if (trigger != null) {
                  int index = triggersList.getSelectionIndex();
                  triggersList.setItem(index, triggerName.getText());
                  trigger.setName(triggerName.getText());
               }
            }
            else if ((e.widget == eventName) ||
                     (e.widget == eventTypeCombo) ||
                     (e.widget == eventArgumentText)) {
               ArenaEvent event = getCurrentlySelectedEvent();
               if (event != null) {
                  int index = eventsList.getSelectionIndex();
                  eventsList.setItem(index, eventName.getText());
                  if (e.widget == eventName) {
                     event.setName(eventName.getText());
                  }
                  if (e.widget == eventTypeCombo) {
                     event.setType(eventTypeCombo.getText());
                  }
                  if (e.widget == eventArgumentText) {
                     event.setData(eventArgumentText.getText());
                  }
               }
            }
            CombatServer._this.mapInterface.refreshSaveButton();
         }
         finally {
            CombatServer.inModify = false;
         }
      }
   }

   private ArenaEvent getCurrentlySelectedEvent() {
      ArenaTrigger trigger = getCurrentlySelectedTrigger();
      if (trigger != null) {
         String[] selections = eventsList.getSelection();
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
      String[] selections = triggersList.getSelection();
      if (selections.length == 1) {
         String selection = selections[0];
         for (ArenaTrigger trigger : map.getTriggers()) {
            if (trigger.getName().equals(selection)) {
               return trigger;
            }
         }
      }
      return null;
   }
   @Override
   public void paintControl(PaintEvent event) {
      if (((event.widget == setEventLocationsButton) && setEventLocations)
          || ((event.widget == setTriggerLocationsButton) && setTriggerLocations)) {
         RGB fillColor = new RGB(255, 192, 192);
         if (setEventLocations) {
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
         bgColor.dispose();
         fgColor.dispose();
      }
   }

   public void onMouseDown(ArenaLocation loc, Event event, double angleFromCenter, double normalizedDistFromCenter,
                           IMapWidget mapWidget, java.util.List<ArenaCoordinates> locationsToRedraw) {
   }
   public void onMouseUp(ArenaLocation loc, Event mouseUpEvent, double angleFromCenter, double normalizedDistFromCenter,
                         CombatMap combatMap, java.util.List<ArenaCoordinates> locationsToRedraw) {
      if (loc == null) {
         return;
      }
      boolean redrawLoc = false;
      if (setEventLocations) {
         ArenaEvent event = getCurrentlySelectedEvent();
         if (event != null) {
            if (event.eventLocations == null) {
               event.eventLocations = new ArrayList<>();
            }
            if (event.eventLocations.contains(loc)) {
               event.eventLocations.remove(loc);
            }
            else {
               event.eventLocations.add(loc);
            }
            redrawLoc = true;
         }
      }
      if (setTriggerLocations) {
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
