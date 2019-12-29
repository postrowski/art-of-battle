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
   void buildBlock(Composite parent);
   // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
   void updateDisplayFromCharacter(Character character);
   // refreshDisplay is used to update fields that dont have ModifyListeners:
   void refreshDisplay(Character character);
   void updateCharacterFromDisplay(Character character);
   void enableControls(boolean enabledFlag);
}
