/*
 * Created on Jun 1, 2007
 */
package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.RuleComposite;
import ostrowski.ui.Helper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Configuration implements SelectionListener
{
   public RuleComposite ruleComposite = null;

   static public boolean spellTnAffectedByMa          = true;
   static public boolean useSimpleDamage              = false;
   static public boolean useSimpleDice                = false;
   static public boolean useExtendedDice              = false;
   static public boolean useBellCurveDice             = true;
   static public boolean useComplexTOUDice            = true;
   static public boolean useD6ForAttributeRolls       = true;
   static public boolean use3DMap                     = false;
   static public boolean showChit                     = false;
   static public boolean rollDice                     = false;
   static public int     serverPort                   = 1777;
   private       Button  spellTnAffectedByMaButton    = null;
   private       Button  useSimpleDamageButton        = null;
   //private Button        useSimpleDiceButton        = null;
   private       Button  useComplexTOUDiceButton      = null;
   private       Button  useD6ForAttributeRollsButton = null;
   private       Button  diceExtendedButton           = null;
   private       Button  diceSimpleButton             = null;
   private       Button  diceBellCurveButton          = null;
   private       Button  use3DMapButton               = null;
   private       Button  showChitButton               = null;
   private       Button  rollDiceButton               = null;
   private       Text    serverPortText               = null;

   public static boolean isSpellTnAffectedByMa() {
      return spellTnAffectedByMa;
   }

   public static boolean useSimpleDamage() {
      return useSimpleDamage;
   }

   public static boolean useExtendedDice() {
      return useExtendedDice;
   }
   public static boolean useSimpleDice() {
      return useSimpleDice;
   }
   public static boolean useBellCurveDice() {
      return useBellCurveDice;
   }

   public static boolean useComplexTOUDice() {
      return useComplexTOUDice;
   }
   public static boolean useD6ForAttributeRolls() {
      return useD6ForAttributeRolls;
   }
   public static boolean use3DMap() {
      return use3DMap;
   }
   public static boolean showChit() {
      return showChit;
   }
   public static boolean rollDice() {
      return rollDice;
   }
   public static int serverPort() {
      return serverPort;
   }

   public Configuration() {
   }

   public void buildDisplay(Composite parent, boolean isServer) {
      if (parent instanceof RuleComposite) {
         ruleComposite = (RuleComposite) parent;
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
      Composite rightGroup;
      if (isServer) {
         leftGroup   = helper.createGroup(topGridBlock, "Rule Tweaks", 1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
         middleGroup = helper.createGroup(topGridBlock, "Attack dice", 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
         rightGroup  = helper.createGroup(topGridBlock, "Game UI",     3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      }
      else {
         rightGroup  = helper.createGroup(topGridBlock, "Game UI",     1/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 3/*vSpacing*/);
      }

      if (leftGroup != null) {
         spellTnAffectedByMaButton = new Button(leftGroup, SWT.CHECK);
         spellTnAffectedByMaButton.setSelection(spellTnAffectedByMa);
         spellTnAffectedByMaButton.addSelectionListener(this);
         spellTnAffectedByMaButton.setText("Spell TN affected by MA");

         useSimpleDamageButton = new Button(leftGroup, SWT.CHECK);
         useSimpleDamageButton.setSelection(useSimpleDamage);
         useSimpleDamageButton.addSelectionListener(this);
         useSimpleDamageButton.setText("Use simple damage");
      }


      showChitButton = new Button(rightGroup, SWT.CHECK);
      showChitButton.setSelection(showChit);
      showChitButton.addSelectionListener(this);
      showChitButton.setText("Show Game Chit");

      rollDiceButton = new Button(rightGroup, SWT.CHECK);
      rollDiceButton.setSelection(rollDice);
      rollDiceButton.addSelectionListener(this);
      rollDiceButton.setText("Roll Dice");

      use3DMapButton = new Button(rightGroup, SWT.CHECK);
      use3DMapButton.setSelection(use3DMap);
      use3DMapButton.addSelectionListener(this);
      use3DMapButton.setText("Use 3D Map (requires restart)");

      if (isServer) {
         Label serverPortLabel = new Label(rightGroup, SWT.NONE);
         serverPortLabel.setText("Server Port:");
         serverPortText = new Text(rightGroup, (SWT.LEFT | SWT.BORDER));
         serverPortText.setText(String.valueOf(serverPort));
         serverPortText.addModifyListener(e -> serverPort = Integer.parseInt(serverPortText.getText()));
      }

      if (middleGroup != null) {
         diceExtendedButton = Helper.createRadioButton(middleGroup, "Complex set", null, this);
         diceSimpleButton = Helper.createRadioButton(middleGroup, "Simple set (d10)", null, this);
         diceBellCurveButton = Helper.createRadioButton(middleGroup, "Bell curve (d10±)", null, this);
         diceExtendedButton.setSelection(useExtendedDice);
         diceSimpleButton.setSelection(useSimpleDice);
         diceBellCurveButton.setSelection(useBellCurveDice);
      }

//      useSimpleDiceButton = new Button(parent, SWT.CHECK);
//      useSimpleDiceButton.setSelection(useSimpleDice);
//      useSimpleDiceButton.addSelectionListener(this);
//      useSimpleDiceButton.setText("Use simple dice (d10s)");

      if (middleGroup != null) {
         useComplexTOUDiceButton = new Button(middleGroup, SWT.CHECK);
         useComplexTOUDiceButton.setSelection(useComplexTOUDice);
         useComplexTOUDiceButton.addSelectionListener(this);
         useComplexTOUDiceButton.setText("Use complex dice for TOU rolls");

         // make useComplexTOUDiceButton span all three columns:
         GridData layoutData = new GridData();
         layoutData.horizontalSpan = 3;
         layoutData.grabExcessHorizontalSpace = true;
         layoutData.grabExcessVerticalSpace = true;
         useComplexTOUDiceButton.setLayoutData(layoutData);
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      boolean updateRules = true;
      if ((e.widget == spellTnAffectedByMaButton) && (spellTnAffectedByMaButton != null)) {
         spellTnAffectedByMa = spellTnAffectedByMaButton.getSelection();
      }
      else if ((e.widget == useSimpleDamageButton) && (useSimpleDamageButton != null)) {
         useSimpleDamage = useSimpleDamageButton.getSelection();
      }
//      else if (e.widget == useSimpleDiceButton) {
//         useSimpleDice = useSimpleDiceButton.getSelection();
//         useComplexTOUDiceButton.setEnabled(useSimpleDice);
//         if (!useSimpleDice) {
//            useComplexTOUDice = true;
//         }
//      }
      else if ((e.widget == diceExtendedButton) && (diceExtendedButton != null)) {
         useExtendedDice = diceExtendedButton.getSelection();
      }
      else if ((e.widget == diceSimpleButton) && (diceSimpleButton != null)) {
         useSimpleDice = diceSimpleButton.getSelection();
      }
      else if ((e.widget == diceBellCurveButton) && (diceBellCurveButton != null)) {
         useBellCurveDice = diceBellCurveButton.getSelection();
      }
      else if ((e.widget == useComplexTOUDiceButton) && (useComplexTOUDiceButton != null)) {
         useComplexTOUDice = useComplexTOUDiceButton.getSelection();
      }
      else if (e.widget == use3DMapButton) {
         use3DMap = use3DMapButton.getSelection();
         updateRules = false;
      }
      else if (e.widget == showChitButton) {
         showChit = showChitButton.getSelection();
         updateRules = false;
      }
      else if (e.widget == rollDiceButton) {
         rollDice = rollDiceButton.getSelection();
         updateRules = false;
      }
      writeToFile();
      if ((ruleComposite != null) && updateRules) {
         ruleComposite.updateRulesSection();
      }
   }

   public void writeToFile() {
      File configFile = new File("Config.cnf");
      try (FileWriter out = new FileWriter(configFile))
      {
         writeBoolean(out, "spellTnAffectedByMa", spellTnAffectedByMa);
         out.write("\n");
         writeBoolean(out, "useSimpleDamage", useSimpleDamage);
         out.write("\n");
         writeBoolean(out, "useSimpleDice", useSimpleDice);
         out.write("\n");
         writeBoolean(out, "useExtendedDice", useExtendedDice);
         out.write("\n");
         writeBoolean(out, "useBellCurveDice", useBellCurveDice);
         out.write("\n");
         writeBoolean(out, "useComplexTOUDice", useComplexTOUDice);
         out.write("\n");
         writeBoolean(out, "useD6ForAttributeRolls", useD6ForAttributeRolls);
         out.write("\n");
         writeBoolean(out, "use3DMap", use3DMap);
         out.write("\n");
         writeBoolean(out, "showChit", showChit);
         out.write("\n");
         writeBoolean(out, "rollDice", rollDice);
         out.write("\n");
         writeString(out, "serverPort", String.valueOf(serverPort));
         out.write("\n");
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void readFromFile() {
      File configFile = new File("Config.cnf");
      try (FileReader fileReader = new FileReader(configFile);
           BufferedReader input = new BufferedReader(fileReader)) {
         List<String> fileLines = new ArrayList<>();
         try {
            String inputLine;
            while ((inputLine = input.readLine()) != null) {
               String line = inputLine.trim();
               if (line.length() > 0) {
                  fileLines.add(line);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }

         spellTnAffectedByMa = readBoolean(fileLines, "spellTnAffectedByMa", spellTnAffectedByMa);
         useSimpleDamage = readBoolean(fileLines, "useSimpleDamage", useSimpleDamage);
         useSimpleDice = readBoolean(fileLines, "useSimpleDice", useSimpleDice);
         useExtendedDice = readBoolean(fileLines, "useExtendedDice", useExtendedDice);
         useBellCurveDice = readBoolean(fileLines, "useBellCurveDice", useBellCurveDice);
         useComplexTOUDice      = readBoolean(fileLines, "useComplexTOUDice",      useComplexTOUDice);
         useD6ForAttributeRolls = readBoolean(fileLines, "useD6ForAttributeRolls", useD6ForAttributeRolls);
         use3DMap = readBoolean(fileLines, "use3DMap", use3DMap);
         showChit = readBoolean(fileLines, "showChit", showChit);
         rollDice = readBoolean(fileLines, "rollDice", rollDice);
         String serverPort       = readString(fileLines,  "serverPort");
         if (serverPort != null) {
            Configuration.serverPort = Integer.parseInt(serverPort);
         }
      } catch (FileNotFoundException e1) {
         writeToFile();
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }

   private static boolean readBoolean(List<String> fileLines, String name, boolean defaultValue) {
      String value = readString(fileLines, name);
      if (value == null) {
         return defaultValue;
      }
      return (value.equalsIgnoreCase("true"));
   }

   private static String readString(List<String> fileLines, String name) {
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
