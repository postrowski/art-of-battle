/*
 * Created on Jun 1, 2007
 */
package ostrowski.combat.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ostrowski.combat.common.RuleComposite;
import ostrowski.ui.Helper;

public class Configuration implements SelectionListener
{
   public RuleComposite  _ruleComposite             = null;

   static public boolean _spellTnAffectedByMa       = true;
   static public boolean _useSimpleDamage           = true;
   static public boolean _useSimpleDice             = true;
   static public boolean _useExtendedDice           = false;
   static public boolean _useBellCurveDice          = false;
   static public boolean _useComplexTOUDice         = true;
   static public boolean _use3DMap                  = true;
   static public boolean _showChit                  = true;
   static public int     _serverPort                = 1777;
   private Button        _spellTnAffectedByMaButton = null;
   private Button        _useSimpleDamageButton     = null;
   //private Button        _useSimpleDiceButton       = null;
   private Button        _useComplexTOUDiceButton   = null;
   private Button        _diceExtendedButton        = null;
   private Button        _diceSimpleButton          = null;
   private Button        _diceBellCurveButton       = null;
   private Button        _use3DMapButton            = null;
   private Button        _showChitButton            = null;
   private Text          _serverPortText            = null;

   public static boolean isSpellTnAffectedByMa() {
      return _spellTnAffectedByMa;
   }

   public static boolean useSimpleDamage() {
      return _useSimpleDamage;
   }

   public static boolean useExtendedDice() {
      return _useExtendedDice;
   }
   public static boolean useSimpleDice() {
      return _useSimpleDice;
   }
   public static boolean useBellCurveDice() {
      return _useBellCurveDice;
   }

   public static boolean useComplexTOUDice() {
      return _useComplexTOUDice;
   }
   public static boolean use3DMap() {
      return _use3DMap;
   }
   public static boolean showChit() {
      return _showChit;
   }
   public static int serverPort() {
      return _serverPort;
   }

   public Configuration() {
   }

   public void buildDisplay(Composite parent, boolean isServer) {
      if (parent instanceof RuleComposite) {
         _ruleComposite = (RuleComposite) parent;
      }
      readFromFile();
      Composite topGridBlock = new Composite(parent, SWT.NONE);
      topGridBlock.setLayout(new GridLayout((isServer ? 3 : 1), true));
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.grabExcessVerticalSpace = false;
      data.grabExcessHorizontalSpace = true;
      topGridBlock.setLayoutData(data);

      Helper helper = new Helper();
      Composite leftGroup  = null;
      Group middleGroup    = null;
      Composite rightGroup = null;
      if (isServer) {
         leftGroup   = helper.createGroup(topGridBlock, "Rule Tweaks", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
         middleGroup = helper.createGroup(topGridBlock, "Attack dice", 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
         rightGroup  = helper.createGroup(topGridBlock, "Game UI",     3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      }
      else {
         rightGroup  = helper.createGroup(topGridBlock, "Game UI",     1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      }

      if (leftGroup != null) {
         _spellTnAffectedByMaButton = new Button(leftGroup, SWT.CHECK);
         _spellTnAffectedByMaButton.setSelection(_spellTnAffectedByMa);
         _spellTnAffectedByMaButton.addSelectionListener(this);
         _spellTnAffectedByMaButton.setText("Spell TN affected by MA");

         _useSimpleDamageButton = new Button(leftGroup, SWT.CHECK);
         _useSimpleDamageButton.setSelection(_useSimpleDamage);
         _useSimpleDamageButton.addSelectionListener(this);
         _useSimpleDamageButton.setText("Use simple damage");
      }


      _showChitButton = new Button(rightGroup, SWT.CHECK);
      _showChitButton.setSelection(_showChit);
      _showChitButton.addSelectionListener(this);
      _showChitButton.setText("Show Game Chit");

      if (isServer) {
         Label serverPortLabel = new Label(rightGroup, SWT.NONE);
         serverPortLabel.setText("Server Port:");
         _serverPortText = new Text(rightGroup, (SWT.LEFT | SWT.BORDER));
         _serverPortText.setText(String.valueOf(_serverPort));
         _serverPortText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
               _serverPort = Integer.valueOf(_serverPortText.getText());
            }
         });
      }

      _use3DMapButton = new Button(rightGroup, SWT.CHECK);
      _use3DMapButton.setSelection(_use3DMap);
      _use3DMapButton.addSelectionListener(this);
      _use3DMapButton.setText("Use 3D Map (requires restart)");

      if (middleGroup != null) {
         _diceExtendedButton  = Helper.createRadioButton(middleGroup, "Complex set", null, this);
         _diceSimpleButton    = Helper.createRadioButton(middleGroup, "Simple set (d10)", null, this);
         _diceBellCurveButton = Helper.createRadioButton(middleGroup, "Bell curve (d10±)", null, this);
         _diceExtendedButton.setSelection(_useExtendedDice);
         _diceSimpleButton.setSelection(_useSimpleDice);
         _diceBellCurveButton.setSelection(_useBellCurveDice);
      }

//      _useSimpleDiceButton = new Button(parent, SWT.CHECK);
//      _useSimpleDiceButton.setSelection(_useSimpleDice);
//      _useSimpleDiceButton.addSelectionListener(this);
//      _useSimpleDiceButton.setText("Use simple dice (d10s)");

      if (middleGroup != null) {
         _useComplexTOUDiceButton = new Button(middleGroup, SWT.CHECK);
         _useComplexTOUDiceButton.setSelection(_useComplexTOUDice);
         _useComplexTOUDiceButton.addSelectionListener(this);
         _useComplexTOUDiceButton.setText("Use complex dice for TOU rolls");

         // make _useComplexTOUDiceButton span all three columns:
         GridData layoutData = new GridData();
         layoutData.horizontalSpan = 3;
         layoutData.grabExcessHorizontalSpace = true;
         layoutData.grabExcessVerticalSpace = true;
         _useComplexTOUDiceButton.setLayoutData(layoutData);
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if ((e.widget == _spellTnAffectedByMaButton) && (_spellTnAffectedByMaButton != null)) {
         _spellTnAffectedByMa = _spellTnAffectedByMaButton.getSelection();
      }
      else if ((e.widget == _useSimpleDamageButton) && (_useSimpleDamageButton != null)) {
         _useSimpleDamage = _useSimpleDamageButton.getSelection();
      }
//      else if (e.widget == _useSimpleDiceButton) {
//         _useSimpleDice = _useSimpleDiceButton.getSelection();
//         _useComplexTOUDiceButton.setEnabled(_useSimpleDice);
//         if (!_useSimpleDice) {
//            _useComplexTOUDice = true;
//         }
//      }
      else if ((e.widget == _diceExtendedButton) && (_diceExtendedButton != null)) {
         _useExtendedDice = _diceExtendedButton.getSelection();
      }
      else if ((e.widget == _diceSimpleButton) && ( _diceSimpleButton != null)) {
         _useSimpleDice = _diceSimpleButton.getSelection();
      }
      else if ((e.widget == _diceBellCurveButton ) && (_diceBellCurveButton != null)) {
         _useBellCurveDice = _diceBellCurveButton.getSelection();
      }
      else if ((e.widget == _useComplexTOUDiceButton) && (_useComplexTOUDiceButton != null)) {
         _useComplexTOUDice = _useComplexTOUDiceButton.getSelection();
      }
      else if (e.widget == _use3DMapButton) {
         _use3DMap = _use3DMapButton.getSelection();
      }
      else if (e.widget == _showChitButton) {
         _showChit = _showChitButton.getSelection();
      }
      writeToFile();
      if (_ruleComposite != null) {
         _ruleComposite.updateRulesSection();
      }
   }

   public void writeToFile() {
      File configFile = new File("Config.cnf");
      try (FileWriter out = new FileWriter(configFile))
      {
         writeBoolean(out, "spellTnAffectedByMa", _spellTnAffectedByMa);
         out.write("\n");
         writeBoolean(out, "useSimpleDamage", _useSimpleDamage);
         out.write("\n");
         writeBoolean(out, "useSimpleDice", _useSimpleDice);
         out.write("\n");
         writeBoolean(out, "useExtendedDice", _useExtendedDice);
         out.write("\n");
         writeBoolean(out, "useBellCurveDice", _useBellCurveDice);
         out.write("\n");
         writeBoolean(out, "useComplexTOUDice", _useComplexTOUDice);
         out.write("\n");
         writeBoolean(out, "use3DMap", _use3DMap);
         out.write("\n");
         writeBoolean(out, "showChit", _showChit);
         out.write("\n");
         writeString(out, "serverPort", String.valueOf(_serverPort));
         out.write("\n");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void readFromFile() {
      File configFile = new File("Config.cnf");
      try (FileReader fileReader = new FileReader(configFile);
           BufferedReader input = new BufferedReader(fileReader)) {
         String inputLine = null;
         ArrayList<String> fileLines = new ArrayList<>();
         try {
            while ((inputLine = input.readLine()) != null) {
               String line = inputLine.trim();
               if ((line != null) && (line.length() > 0)) {
                  fileLines.add(line);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }

         _spellTnAffectedByMa = readBoolean(fileLines, "spellTnAffectedByMa", _spellTnAffectedByMa);
         _useSimpleDamage     = readBoolean(fileLines, "useSimpleDamage",     _useSimpleDamage);
         _useSimpleDice       = readBoolean(fileLines, "useSimpleDice",       _useSimpleDice);
         _useExtendedDice     = readBoolean(fileLines, "useExtendedDice",     _useExtendedDice);
         _useBellCurveDice    = readBoolean(fileLines, "useBellCurveDice",    _useBellCurveDice);
         _useComplexTOUDice   = readBoolean(fileLines, "useComplexTOUDice",   _useComplexTOUDice);
         _use3DMap            = readBoolean(fileLines, "use3DMap",            _use3DMap);
         _showChit            = readBoolean(fileLines, "showChit",            _showChit);
         String serverPort    = readString(fileLines,  "serverPort");
         if (serverPort != null) {
            _serverPort = Integer.valueOf(serverPort);
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }

   private static boolean readBoolean(ArrayList<String> fileLines, String name, boolean defaultValue) {
      String value = readString(fileLines, name);
      if (value == null) {
         return defaultValue;
      }
      return (value.equalsIgnoreCase("true"));
   }

   private static String readString(ArrayList<String> fileLines, String name) {
      for (String line : fileLines) {
         if (line.startsWith(name)) {
            String remainder = line.substring(name.length()).trim();
            if (remainder.startsWith("=")) {
               return remainder.substring(1).trim();
            }
         }
      }
      return null;
   }

   private static void writeBoolean(FileWriter out, String name, boolean value) throws IOException {
      out.write(name);
      if (value) {
         out.write(" = true");
      }
      else {
         out.write(" = false");
      }
   }

   private static void writeString(FileWriter out, String name, String value) throws IOException {
      out.write(name);
      out.write(" = ");
      out.write(value);
   }
}
