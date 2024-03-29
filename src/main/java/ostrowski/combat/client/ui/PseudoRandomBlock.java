package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.Character;
import ostrowski.combat.server.CombatServer;
import ostrowski.ui.Helper;

public class PseudoRandomBlock extends Helper implements IUIBlock, ModifyListener, SelectionListener
{
   public        Button       usePseudoRandomNumbers;
   private       Text         pseudoRandomNumberSeedText;
   private final CombatServer combatServer;

   public PseudoRandomBlock(CombatServer combatServer) {
      this.combatServer = combatServer;
   }

   @Override
   public void buildBlock(Composite parent) {
      Group playGroup = createGroup(parent, "Randomness", 3/*columns*/, false/*sameSize*/, 3/*hSpacing*/, 1/*vSpacing*/);
      Composite block   = new Composite(playGroup, SWT.LEFT);

      GridLayout grid = new GridLayout(2, false);
      block.setLayout(grid);
      GridData data = new GridData();
      data.horizontalAlignment = SWT.BEGINNING;
      block.setLayoutData(data);

      usePseudoRandomNumbers = new Button(block, SWT.CHECK);
      usePseudoRandomNumbers.setText("Use pseudo-random number seed:");
      usePseudoRandomNumbers.setSelection(false);
      usePseudoRandomNumbers.addSelectionListener(this);

      pseudoRandomNumberSeedText = new Text(block, SWT.LEFT | SWT.BORDER);
      // over-write the Text field with a large empty string to keep it large for the initial pack.
      pseudoRandomNumberSeedText.setText("       ");
      pseudoRandomNumberSeedText.setEditable(false);
      pseudoRandomNumberSeedText.setEnabled(false);
      pseudoRandomNumberSeedText.addModifyListener(this);

   }

   @Override
   public void refreshDisplay(Character character) {
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
   }

   @Override
   public void updateDisplayFromCharacter(Character character) {
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (e.widget == pseudoRandomNumberSeedText) {
         String value = pseudoRandomNumberSeedText.getText();
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
         combatServer.setPseudoRandomNumberSeed(val);
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == usePseudoRandomNumbers) {
//        pseudoRandomNumberSeedText.setEditable(usePseudoRandomNumbers.getSelection());
         pseudoRandomNumberSeedText.setEnabled(usePseudoRandomNumbers.getSelection());
         pseudoRandomNumberSeedText.setText(String.valueOf(combatServer.pseudoRandomNumberSeed));
      }
   }


   public void setSeedText(int pseudoRandomNumberSeed) {
      pseudoRandomNumberSeedText.setText(String.valueOf(pseudoRandomNumberSeed));
   }
   public String getSeedText() {
      return pseudoRandomNumberSeedText.getText();
   }
   public boolean isUsingPseudoRandomNumber() {
      return usePseudoRandomNumbers.getSelection();
   }
   public void setUsingPseudoRandomNumber(boolean usingPseudoRandomNumber) {
      usePseudoRandomNumbers.setSelection(usingPseudoRandomNumber);
      if (usingPseudoRandomNumber) {
         pseudoRandomNumberSeedText.setEnabled(true);
      }
   }
}
