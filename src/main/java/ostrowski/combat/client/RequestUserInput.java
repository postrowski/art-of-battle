package ostrowski.combat.client;

/*
 * Created on Dec 2, 2005
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.IMapWidget;
import ostrowski.combat.common.TargetPrioritiesWidget;
import ostrowski.combat.protocol.request.RequestTarget;
import ostrowski.combat.server.Arena;
import ostrowski.combat.server.CombatServer;
import ostrowski.combat.server.Configuration;
import ostrowski.protocol.SyncRequest;

import java.util.HashMap;
import java.util.List;

public class RequestUserInput extends Dialog implements KeyListener, FocusListener, ControlListener {
   public final         Shell                   shell;
   private              Button[]                buttons;
   private              boolean                 backupSelected;
   private              boolean                 executed                 = false;
   private              boolean                 twoStepExecute           = false;
   private final        String                  message;
   private              Object                  result;
   private final        SyncRequest             req;
   private              TargetPrioritiesWidget  targetPrioritiesWidget   = null;
   private final        IMapWidget              reportSelectMapWidget;
   private final        Character               actingCharacter;
   private              StatusChit              statusChit               = null;
   private static final HashMap<Integer, Point> LOCATION_BY_CHARACTER_ID = new HashMap<>();

   public RequestUserInput(Shell parent, int style, SyncRequest req, boolean showChit) {
      this(parent, style, req, false, null, null, showChit);
   }

   public RequestUserInput(Shell parent, int style, SyncRequest req, boolean allowBackup,
                           IMapWidget reportSelectMapWidget, Character actingCharacter, boolean showChit) {
      super(parent, checkStyle(style));

      this.reportSelectMapWidget = reportSelectMapWidget;
      this.actingCharacter = actingCharacter;
      this.req = req;
      if (req instanceof RequestTarget) {
         twoStepExecute = true;
      }
      String    message  = req.getMessage();
      String[]  options  = req.getOptions();
      boolean[] enableds = req.getEnableds();

      if (message == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      this.message = message;

      shell = new Shell(parent, SWT.DIALOG_TRIM | checkStyle(style));
      shell.setText(getText());
      shell.setLayout(new GridLayout(2/*numColumns*/, false/*makeColumnsEqualWidth*/));
      shell.addFocusListener(this);
      if (showChit)
      {
         if (statusChit == null) {
            statusChit = new StatusChit(shell, SWT.MODELESS | SWT.NO_TRIM | SWT.NO_FOCUS);
         }
         if (actingCharacter != null) {
            statusChit.updateFromCharacter(actingCharacter);
         }
         // TODO: figure how to regain focus:
         //       these 4 attempts don't work:
         shell.setFocus();
         shell.forceActive();
         shell.setActive();
         shell.forceFocus();
      }

      new Label(shell, SWT.CENTER).setImage(shell.getDisplay().getSystemImage(checkImageStyle(style)));

      Composite body = new Composite(shell, SWT.NONE);
      body.addFocusListener(this);


      GridData data0 = new GridData();
      data0.grabExcessHorizontalSpace = true;
      data0.grabExcessVerticalSpace = true;
      data0.horizontalAlignment = GridData.FILL;
      data0.verticalAlignment = SWT.FILL;
      body.setLayoutData(data0);
      GridLayout layout = new GridLayout();
      layout.verticalSpacing = 3;
      body.setLayout(layout);

      message = Arena.convertFromHtmlToText(message);
      int loc = message.indexOf("\n");
      while (loc != -1) {
         addLabel(message.substring(0, loc), body);
         message = message.substring(loc+1);
         loc = message.indexOf("\n");
      }
      addLabel(message, body);
      addLabel("", body);

      Group optionsGroup = new Group(body, SWT.LEFT);
      optionsGroup.setText("options available");

      GridData data2 = new GridData();
      data2.grabExcessHorizontalSpace = false;
      data2.horizontalAlignment = SWT.CENTER;
      optionsGroup.setLayoutData(data2);

      if (req instanceof RequestTarget) {
         layout = new GridLayout(1, false/*sameWidth*/);
         // put some space between each action option.
         layout.verticalSpacing = 5;
         optionsGroup.setLayout(layout);

         targetPrioritiesWidget = new TargetPrioritiesWidget(null, null);
         List<Character> combatants = ((RequestTarget)req).getTargetCharacters();
         targetPrioritiesWidget.buildBlock(optionsGroup);
         targetPrioritiesWidget.updateCombatants(combatants);
      }
      else if (options != null) {
         int columns = 1;
         for (String option : options) {
            if (option.equals("\n")) {
               columns++;
            }
         }
         // multiply the number of columns by two, because we add an extra column for the shortcut key to every existing column
         columns *=2;

         layout = new GridLayout(columns, false/*makeColumnsEqualWidth*/);
         // put some space between each action option.
         layout.verticalSpacing = 5;
         optionsGroup.setLayout(layout);

         Color shortcutColor = new Color(body.getDisplay(), 128, 128, 128);

         buttons = new Button[options.length];
         Composite column = new Composite(optionsGroup, SWT.NONE);
         GridLayout grid = new GridLayout();
         boolean showShortCutInItsOwnColumn = true;
         grid.numColumns = showShortCutInItsOwnColumn ? 2 : 1;
         column.setLayout(grid);
         for (int i=0 ; i<options.length ; i++) {
            if (options[i].equals("\n")) {
               column = new Composite(optionsGroup, SWT.NONE);
               grid = new GridLayout();
               grid.numColumns = showShortCutInItsOwnColumn ? 2 : 1;
               column.setLayout(grid);
            }
            else {
               String shortcut = this.req.getStringOfKeyStrokeAssignedToOption(i);
               String buttonText = options[i];
               if (showShortCutInItsOwnColumn) {
                  Label shortcutLabel = new Label(column, SWT.RIGHT);
                  shortcutLabel.setForeground(shortcutColor);
                  shortcutLabel.setText(shortcut );
               }
               else {
                  if ((shortcut != null) && (shortcut.length() > 0)) {
                     buttonText += "    [" + shortcut + "]";
                  }
               }

               // Two-step operations use radio buttons. one-step operations use push buttons
               buttons[i] = new Button(column, (twoStepExecute ? (SWT.LEFT | SWT.RADIO) : (SWT.PUSH)));
               buttons[i].setText(buttonText);
               buttons[i].setEnabled(enableds[i]);
               buttons[i].addKeyListener(this);
               buttons[i].addFocusListener(this);
               buttons[i].addSelectionListener(new SelectionAdapter() {

                  @Override
                  public void widgetSelected(SelectionEvent e) {
                     int index = -1;
                     for (int j = 0; j < buttons.length ; j++) {
                        if (buttons[j] == e.widget) {
                           index = j;
                        }
                     }
                     if (index != -1) {
                        for (int k = 0; k < buttons.length ; k++) {
                           if (buttons[k] != null) {
                              buttons[k].setSelection(k == index);
                           }
                        }
                        result = index;
                     }
                     focusGained();

                     if (!twoStepExecute) {
                        shell.dispose();
                        executed = true;
                     }

                  }
               });
               if (!twoStepExecute) {
                  // Make the button fill the width of the current column,
                  // so they all have the same width and look nicer.
                  GridData gridData = new GridData();
                  gridData.horizontalAlignment = GridData.FILL;
                  gridData.grabExcessHorizontalSpace = true;
                  buttons[i].setLayoutData(gridData);
               }
            }
         }
         shortcutColor.dispose();
         //            int maxWidth = 0;
         //            for (Button button : buttons) {
         //               if (button == null)
         //                  continue;
         //               String text = button.getText();
         //               Point extent = shell.getDisplay().get.stringExtent(text);
         //            }
         //            for (Button button : buttons) {
         //               if (button == null)
         //                  continue;
         //               button.setSize(maxSize);
         //            }
      }


      Composite footer = new Composite(shell, SWT.NONE);

      GridData data3 = new GridData();
      data3.grabExcessHorizontalSpace = true;
      data3.horizontalAlignment = SWT.FILL;
      data3.horizontalSpan = 2;
      footer.setLayoutData(data3);

      RowLayout rowlayout = new RowLayout();
      rowlayout.justify = true;
      rowlayout.fill = true;
      footer.setLayout(rowlayout);

      if (twoStepExecute) {
         Button ok = new Button(footer, SWT.PUSH);
         ok.setText("  Execute ");
         ok.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               shell.dispose();
               executed = true;
            }
         });
         ok.addKeyListener(this);
         shell.setDefaultButton(ok);
      }

      if (allowBackup) {
         Button backupButton = new Button(footer, SWT.PUSH);
         backupButton.setText("  Backup  ");
         backupButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               backupSelected = true;
               shell.dispose();
            }
         });
      }
   }

   private void addLabel(String message, Composite body)
   {
      Label label = new Label(body, SWT.LEFT);
      GridData data1 = new GridData();
      data1.grabExcessHorizontalSpace = true;
      data1.grabExcessVerticalSpace = true;
      data1.minimumWidth = 400;
      data1.horizontalAlignment = SWT.FILL;
      data1.verticalAlignment = SWT.FILL;
      label.setLayoutData(data1);
      label.setText(message);
      label.addKeyListener(this);
   }

   protected static int checkStyle(int style) {
      if ((style & SWT.SYSTEM_MODAL) == SWT.SYSTEM_MODAL) {
         return SWT.SYSTEM_MODAL;
      } else if ((style & SWT.PRIMARY_MODAL) == SWT.PRIMARY_MODAL) {
         return SWT.PRIMARY_MODAL;
      } else if ((style & SWT.APPLICATION_MODAL) == SWT.APPLICATION_MODAL) {
         return SWT.APPLICATION_MODAL;
      }

      return style;
   }

   protected static int checkImageStyle(int style) {
      if ((style & SWT.ICON_ERROR) == SWT.ICON_ERROR) {
         return SWT.ICON_ERROR;
      } else if ((style & SWT.ICON_INFORMATION) == SWT.ICON_INFORMATION) {
         return SWT.ICON_INFORMATION;
      } else if ((style & SWT.ICON_QUESTION) == SWT.ICON_QUESTION) {
         return SWT.ICON_QUESTION;
      } else if ((style & SWT.ICON_WARNING) == SWT.ICON_WARNING) {
         return SWT.ICON_WARNING;
      } else if ((style & SWT.ICON_WORKING) == SWT.ICON_WORKING) {
         return SWT.ICON_WORKING;
      }

      return SWT.NONE;
   }

   public void setDefault(int defaultIndex)
   {
      if (defaultIndex != -1) {
         if (twoStepExecute) {
            if (buttons[defaultIndex] != null) {
               buttons[defaultIndex].setSelection(true);
            }
            if (result == null) {
               result = defaultIndex;
            }
         }
         else {
            shell.setDefaultButton(buttons[defaultIndex]);
         }
      }
   }

   public void setTitle(String string) {
      super.setText(string);
      shell.setText(string);
   }

   /*
    * This method returns the numeric index of the button,
    * NOT THE MESSAGE INDEX!
    */
   public Object open() {
      focusGained();
      shell.pack();
      shell.open();
      shell.layout();

      if (MessageDialog.topMessage != null) {
         if (MessageDialog.topMessage.shell.isDisposed()) {
            MessageDialog.topMessage = null;
         }
         else {
            MessageDialog.topMessage.shell.moveAbove(shell);
         }
      }

      Point statusChitLoc = null;
      if (actingCharacter != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_ID.get(actingCharacter.uniqueID);
         if (currentLoc == null) {
            currentLoc = shell.getLocation();
            LOCATION_BY_CHARACTER_ID.put(actingCharacter.uniqueID, currentLoc);
            Point size = shell.getSize();
            statusChitLoc = new Point((currentLoc.x + size.x) - 5, currentLoc.y);
         }
         else {
            shell.setLocation(currentLoc);
         }
      }
      if (statusChit != null)
      {
         statusChit.open();
         if (statusChitLoc != null) {
            statusChit.setLocation(statusChitLoc);
         }
      }

      shell.addKeyListener(this);
      shell.addControlListener(this);


      while (!shell.isDisposed()) {
         if (!shell.getDisplay().readAndDispatch()) {
            shell.getDisplay().sleep();
         }
      }
      if (statusChit != null) {
         if (CombatServer.isServer || !Configuration.showChit()) {
            statusChit.close();
            statusChit = null;
         }
      }

      if (!executed) {
         int[] optionIds = req.getOptionIDs();
         for (int i=0 ; i<optionIds.length ; i++) {
            if (buttons[i] == null) {
               continue;
            }
            if (optionIds[i] == SyncRequest.OPT_CANCEL_ACTION) {
               req.setAnswerByOptionIndex(i);
               result = i;
            }
         }
      }
      if ((req instanceof RequestTarget) && (targetPrioritiesWidget != null)) {
         RequestTarget reqTarget = (RequestTarget) req;
         reqTarget.setOrderedTargetIds(targetPrioritiesWidget.getOrderedEnemies());
         return null;
      }
      return result;
   }
   public boolean isBackupSelected() {
      return backupSelected;
   }
   public void focusGained() {
      if (reportSelectMapWidget != null) {
         reportSelectMapWidget.setFocusForCharacter(actingCharacter, req);
      }
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
      if (arg0.keyCode == SWT.ESC) {
         req.setAnswerID(SyncRequest.OPT_CANCEL_ACTION);
      }
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      if (req.keyPressed(arg0)) {
         int answerId = req.getFullAnswerID();
         int[] optionIds = req.getOptionIDs();
         for (int i=0 ; i<optionIds.length ; i++) {
            if (buttons[i] == null) {
               continue;
            }
            if (optionIds[i] == answerId) {
               buttons[i].setSelection(true);
               req.setAnswerByOptionIndex(i);
               result = i;
            }
            else {
               buttons[i].setSelection(false);
            }
         }
         executed = true;
         shell.close();
      }
   }
   @Override
   public void focusGained(FocusEvent arg0) {
      focusGained();
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
   @Override
   public void focusLost(FocusEvent arg0) {
   }
   @Override
   public void controlMoved(ControlEvent arg0) {
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
      Point currentLoc = shell.getLocation();
      if (actingCharacter != null) {
         Point oldLoc = LOCATION_BY_CHARACTER_ID.get(actingCharacter.uniqueID);
         if ((oldLoc == null) || !(oldLoc.equals(currentLoc))) {
            LOCATION_BY_CHARACTER_ID.put(actingCharacter.uniqueID, currentLoc);
         }
      }
   }
   @Override
   public void controlResized(ControlEvent arg0) {
      if (CombatServer.uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
}
