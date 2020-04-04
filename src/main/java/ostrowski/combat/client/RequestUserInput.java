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
   public final Shell                     _shell;
   private Button[]                       _buttons;
   private boolean                        _backupSelected;
   private boolean                        _executed                = false;
   private boolean                        _twoStepExecute          = false;
   private final String                   _message;
   private Object                         _result;
   private final SyncRequest              _req;
   private TargetPrioritiesWidget         _targetPrioritiesWidget  = null;
   private final IMapWidget               _reportSelectMapWidget;
   private final Character                _actingCharacter;
   private              StatusChit              _statusChit              = null;
   private static final HashMap<Integer, Point> LOCATION_BY_CHARACTER_ID = new HashMap<>();

   public RequestUserInput(Shell parent, int style, SyncRequest req, boolean showChit) {
      this(parent, style, req, false, null, null, showChit);
   }

   public RequestUserInput(Shell parent, int style, SyncRequest req, boolean allowBackup,
                           IMapWidget reportSelectMapWidget, Character actingCharacter, boolean showChit) {
      super(parent, checkStyle(style));

      _reportSelectMapWidget = reportSelectMapWidget;
      _actingCharacter = actingCharacter;
      _req = req;
      if (req instanceof RequestTarget) {
         _twoStepExecute = true;
      }
      String    message  = req.getMessage();
      String[]  options  = req.getOptions();
      boolean[] enableds = req.getEnableds();

      if (message == null) {
         SWT.error(SWT.ERROR_NULL_ARGUMENT);
      }
      _message = message;

      _shell = new Shell(parent, SWT.DIALOG_TRIM | checkStyle(style));
      _shell.setText(getText());
      _shell.setLayout(new GridLayout(2/*numColumns*/, false/*makeColumnsEqualWidth*/));
      _shell.addFocusListener(this);
      if (showChit)
      {
         if (_statusChit == null) {
            _statusChit = new StatusChit(_shell, SWT.MODELESS | SWT.NO_TRIM | SWT.NO_FOCUS);
         }
         if (actingCharacter != null) {
            _statusChit.updateFromCharacter(actingCharacter);
         }
         // TODO: figure how to regain focus:
         //       these 4 attempts don't work:
         _shell.setFocus();
         _shell.forceActive();
         _shell.setActive();
         _shell.forceFocus();
      }

      new Label(_shell, SWT.CENTER).setImage(_shell.getDisplay().getSystemImage(checkImageStyle(style)));

      Composite body = new Composite(_shell, SWT.NONE);
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

      if (message != null) {
         message = Arena.convertFromHtmlToText(message);
         int loc = message.indexOf("\n");
         while (loc != -1) {
            addLabel(message.substring(0, loc), body);
            message = message.substring(loc+1);
            loc = message.indexOf("\n");
         }
         addLabel(message, body);
      }
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

         _targetPrioritiesWidget  = new TargetPrioritiesWidget(null, null);
         List<Character> combatants = ((RequestTarget)req).getTargetCharacters();
         _targetPrioritiesWidget.buildBlock(optionsGroup);
         _targetPrioritiesWidget.updateCombatants(combatants );
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

         _buttons = new Button[options.length];
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
               String shortcut = _req.getStringOfKeyStrokeAssignedToOption(i);
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
               _buttons[i] = new Button(column, (_twoStepExecute ? (SWT.LEFT | SWT.RADIO) : (SWT.PUSH)));
               _buttons[i].setText(buttonText);
               _buttons[i].setEnabled(enableds[i]);
               _buttons[i].addKeyListener(this);
               _buttons[i].addFocusListener(this);
               _buttons[i].addSelectionListener(new SelectionAdapter() {

                  @Override
                  public void widgetSelected(SelectionEvent e) {
                     int index = -1;
                     for (int j=0 ; j<_buttons.length ; j++) {
                        if (_buttons[j] == e.widget) {
                           index = j;
                        }
                     }
                     if (index != -1) {
                        for (int k=0 ; k<_buttons.length ; k++) {
                           if (_buttons[k] != null) {
                              _buttons[k].setSelection(k == index);
                           }
                        }
                        _result = index;
                     }
                     focusGained();

                     if (!_twoStepExecute) {
                        _shell.dispose();
                        _executed = true;
                     }

                  }
               });
               if (!_twoStepExecute) {
                  // Make the button fill the width of the current column,
                  // so they all have the same width and look nicer.
                  GridData gridData = new GridData();
                  gridData.horizontalAlignment = GridData.FILL;
                  gridData.grabExcessHorizontalSpace = true;
                  _buttons[i].setLayoutData(gridData);
               }
            }
         }
         shortcutColor.dispose();
         //            int maxWidth = 0;
         //            for (Button button : _buttons) {
         //               if (button == null)
         //                  continue;
         //               String text = button.getText();
         //               Point extent = _shell.getDisplay().get.stringExtent(text);
         //            }
         //            for (Button button : _buttons) {
         //               if (button == null)
         //                  continue;
         //               button.setSize(maxSize);
         //            }
      }


      Composite footer = new Composite(_shell, SWT.NONE);

      GridData data3 = new GridData();
      data3.grabExcessHorizontalSpace = true;
      data3.horizontalAlignment = SWT.FILL;
      data3.horizontalSpan = 2;
      footer.setLayoutData(data3);

      RowLayout rowlayout = new RowLayout();
      rowlayout.justify = true;
      rowlayout.fill = true;
      footer.setLayout(rowlayout);

      if (_twoStepExecute) {
         Button ok = new Button(footer, SWT.PUSH);
         ok.setText("  Execute ");
         ok.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               _shell.dispose();
               _executed = true;
            }
         });
         ok.addKeyListener(this);
         _shell.setDefaultButton(ok);
      }

      if (allowBackup) {
         Button backupButton = new Button(footer, SWT.PUSH);
         backupButton.setText("  Backup  ");
         backupButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               _backupSelected = true;
               _shell.dispose();
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
         if (_twoStepExecute) {
            if (_buttons[defaultIndex] != null) {
               _buttons[defaultIndex].setSelection(true);
            }
            if (_result == null) {
               _result = defaultIndex;
            }
         }
         else {
            _shell.setDefaultButton(_buttons[defaultIndex]);
         }
      }
   }

   public void setTitle(String string) {
      super.setText(string);
      _shell.setText(string);
   }

   public String getMessage() {
      return _message;
   }

   /*
    * This method returns the numeric index of the button,
    * NOT THE MESSAGE INDEX!
    */
   public Object open() {
      focusGained();
      _shell.pack();
      _shell.open();
      _shell.layout();

      if (MessageDialog._topMessage != null) {
         if (MessageDialog._topMessage._shell.isDisposed()) {
            MessageDialog._topMessage = null;
         }
         else {
            MessageDialog._topMessage._shell.moveAbove(_shell);
         }
      }

      Point statusChitLoc = null;
      if (_actingCharacter != null) {
         Point currentLoc = LOCATION_BY_CHARACTER_ID.get(_actingCharacter._uniqueID);
         if (currentLoc == null) {
            currentLoc = _shell.getLocation();
            LOCATION_BY_CHARACTER_ID.put(_actingCharacter._uniqueID, currentLoc);
            Point size = _shell.getSize();
            statusChitLoc = new Point((currentLoc.x + size.x) - 5, currentLoc.y);
         }
         else {
            _shell.setLocation(currentLoc);
         }
      }
      if (_statusChit != null)
      {
         _statusChit.open();
         if (statusChitLoc != null) {
            _statusChit.setLocation(statusChitLoc);
         }
      }

      _shell.addKeyListener(this);
      _shell.addControlListener(this);


      while (!_shell.isDisposed()) {
         if (!_shell.getDisplay().readAndDispatch()) {
            _shell.getDisplay().sleep();
         }
      }
      if (_statusChit != null) {
         if (CombatServer._isServer || !Configuration.showChit()) {
            _statusChit.close();
            _statusChit = null;
         }
      }

      if (!_executed) {
         int[] optionIds = _req.getOptionIDs();
         for (int i=0 ; i<optionIds.length ; i++) {
            if (_buttons[i] == null) {
               continue;
            }
            if (optionIds[i] == SyncRequest.OPT_CANCEL_ACTION) {
               _req.setAnswerByOptionIndex(i);
               _result = i;
            }
         }
      }
      if ((_req instanceof RequestTarget) && (_targetPrioritiesWidget != null)) {
         RequestTarget reqTarget = (RequestTarget) _req;
         reqTarget.setOrderedTargetIds(_targetPrioritiesWidget.getOrderedEnemies());
         return null;
      }
      return _result;
   }
   public boolean isBackupSelected() {
      return _backupSelected;
   }
   public void focusGained() {
      if (_reportSelectMapWidget != null) {
         _reportSelectMapWidget.setFocusForCharacter(_actingCharacter, _req);
      }
   }
   @Override
   public void keyPressed(KeyEvent arg0) {
      if (arg0.keyCode == SWT.ESC) {
         _req.setAnswerID(SyncRequest.OPT_CANCEL_ACTION);
      }
   }

   @Override
   public void keyReleased(KeyEvent arg0) {
      if (_req.keyPressed(arg0)) {
         int answerId = _req.getFullAnswerID();
         int[] optionIds = _req.getOptionIDs();
         for (int i=0 ; i<optionIds.length ; i++) {
            if (_buttons[i] == null) {
               continue;
            }
            if (optionIds[i] == answerId) {
               _buttons[i].setSelection(true);
               _req.setAnswerByOptionIndex(i);
               _result = i;
            }
            else {
               _buttons[i].setSelection(false);
            }
         }
         _executed = true;
         _shell.close();
      }
   }
   @Override
   public void focusGained(FocusEvent arg0) {
      focusGained();
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
   @Override
   public void focusLost(FocusEvent arg0) {
   }
   @Override
   public void controlMoved(ControlEvent arg0) {
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
      Point currentLoc = _shell.getLocation();
      if (_actingCharacter != null) {
         Point oldLoc = LOCATION_BY_CHARACTER_ID.get(_actingCharacter._uniqueID);
         if ((oldLoc == null) || !(oldLoc.equals(currentLoc))) {
            LOCATION_BY_CHARACTER_ID.put(_actingCharacter._uniqueID, currentLoc);
         }
      }
   }
   @Override
   public void controlResized(ControlEvent arg0) {
      if (CombatServer._uses3dMap) {
         CombatServer._this.redrawMap();
      }
   }
}
