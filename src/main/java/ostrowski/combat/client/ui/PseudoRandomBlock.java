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
   public        Button       _usePseudoRandomNumbers;
   private       Text         _pseudoRandomNumberSeedText;
   private final CombatServer _combatServer;

   public PseudoRandomBlock(CombatServer combatServer) {
      _combatServer = combatServer;
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

      _usePseudoRandomNumbers = new Button(block, SWT.CHECK);
      _usePseudoRandomNumbers.setText("Use pseudo-random number seed:");
      _usePseudoRandomNumbers.setSelection(false);
      _usePseudoRandomNumbers.addSelectionListener(this);

      _pseudoRandomNumberSeedText = new Text(block, SWT.LEFT | SWT.BORDER);
      // over-write the Text field with a large empty string to keep it large for the initial pack.
      _pseudoRandomNumberSeedText.setText("       ");
      _pseudoRandomNumberSeedText.setEditable(false);
      _pseudoRandomNumberSeedText.setEnabled(false);
      _pseudoRandomNumberSeedText.addModifyListener(this);

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
      if (e.widget == _pseudoRandomNumberSeedText) {
         String value = _pseudoRandomNumberSeedText.getText();
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
         _combatServer.setPseudoRandomNumberSeed(val);
      }
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _usePseudoRandomNumbers) {
//         _pseudoRandomNumberSeedText.setEditable(_usePseudoRandomNumbers.getSelection());
         _pseudoRandomNumberSeedText.setEnabled(_usePseudoRandomNumbers.getSelection());
         _pseudoRandomNumberSeedText.setText(String.valueOf(_combatServer._pseudoRandomNumberSeed));
      }
   }


   public void setSeedText(int pseudoRandomNumberSeed) {
      _pseudoRandomNumberSeedText.setText(String.valueOf(pseudoRandomNumberSeed));
   }
   public String getSeedText() {
      return _pseudoRandomNumberSeedText.getText();
   }
   public boolean isUsingPseudoRandomNumber() {
      return _usePseudoRandomNumbers.getSelection();
   }
   public void setUsingPseudoRandomNumber(boolean usingPseudoRandomNumber) {
      _usePseudoRandomNumbers.setSelection(usingPseudoRandomNumber);
      if (usingPseudoRandomNumber) {
         _pseudoRandomNumberSeedText.setEnabled(true);
      }
   }
}
