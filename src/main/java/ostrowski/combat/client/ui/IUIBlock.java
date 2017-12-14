/*
 * Created on May 22, 2006
 *
 */
package ostrowski.combat.client.ui;

import org.eclipse.swt.widgets.Composite;

import ostrowski.combat.common.Character;
import ostrowski.combat.common.enums.Enums;

public interface IUIBlock extends Enums
{
   public void buildBlock(Composite parent);
   // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   public void updateDisplayFromCharacter(Character character);
   // refreshDisplay is used to update fields that dont have ModifyListeners:
   public void refreshDisplay(Character character);
   public void updateCharacterFromDisplay(Character character);
   public void enableControls(boolean enabledFlag);
}
