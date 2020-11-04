package ostrowski.combat.client.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.DebugBreak;
import ostrowski.combat.common.Character;
import ostrowski.combat.common.CharacterWidget;
import ostrowski.combat.common.enums.Attribute;
import ostrowski.combat.common.things.*;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttack;
import ostrowski.combat.common.weaponStyles.WeaponStyleAttackRanged;
import ostrowski.ui.Helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WeaponBlock extends Helper implements ModifyListener, IUIBlock, SelectionListener
{
   private final CharacterWidget _display;
   static final int LINES_OF_EQU = 6;
   public final String BELT = "Belt";
   private final Button[]         _buttons   = new Button[LINES_OF_EQU];
   private final Combo[]          _equipment = new Combo[LINES_OF_EQU];
   private final Text[]           _equCost   = new Text[LINES_OF_EQU];
   private final Text[]           _equLbs    = new Text[LINES_OF_EQU];
   private final Combo[]          _location  = new Combo[LINES_OF_EQU];
   private Text             _armorCost = null;
   private Text             _armorLbs  = null;
   private Text             _totalCost = null;
   private Text             _totalLbs  = null;

   static final int MAX_ATTACK_STYLES_PER_WEAPON = 5;
   private final Text[][]         _styleName   = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][]         _skillName   = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][]         _styleRange  = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][]         _styleTime   = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][]         _styleDamage = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];

   private Text             _rangePB     = null;
   private Text             _rangeShort  = null;
   private Text             _rangeMedium = null;
   private Text             _rangeLong   = null;
   private Text             _rangeDamage = null;
   private Text             _rangeStyle  = null;
   private int _selectedEqu = 0;
   private TabFolder _folder;

   public WeaponBlock(CharacterWidget display) {
      _display = display;
   }
   @Override
   public void buildBlock(Composite parent) {
      Group equipmentGroup = createGroup(parent, "Equipment", 2/*columns*/, false, 3/*hSpacing*/, 1/*vSpacing*/);

      Composite leftBlock = Helper.createComposite(equipmentGroup, 1, GridData.FILL_BOTH);
      Composite rightBlock = Helper.createComposite(equipmentGroup, 1, GridData.FILL_BOTH);
      leftBlock.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      List<String> equipmentNames = new ArrayList<>();
      List<String> locationNames  = new ArrayList<>();
      locationNames.add(BELT);
      for (LimbType limbType : _display._character.getRace().getLimbSet()) {
         if (limbType.isHand()) {
            if (_display._character.getLimb(limbType) != null) {
               locationNames.add(limbType.name);
            }
         }
      }
      equipmentNames.add("---");
      equipmentNames.addAll(Weapons.getWeaponNames(false/*includeNaturalWeapons*/));
      equipmentNames.add("---");
      equipmentNames.addAll(Shield.getShieldNames());
      equipmentNames.add("---");
      equipmentNames.addAll(Potion.getPotionNames());
      createLabel(leftBlock, "",      SWT.LEFT, 1, null);
      createLabel(leftBlock, "Name",  SWT.CENTER, 1, null);
      createLabel(leftBlock, "Cost",  SWT.CENTER, 1, null);
      createLabel(leftBlock, "Lbs.",  SWT.CENTER, 1, null);
      createLabel(leftBlock, "Place", SWT.CENTER, 1, null);
      for (int i=0 ; i<_equipment.length ; i++) {
         _buttons[i]   = new Button(leftBlock, SWT.LEFT | SWT.RADIO);
         _equipment[i] = createCombo(leftBlock, SWT.READ_ONLY, 1/*hSpan*/, equipmentNames);
         _equCost[i]   = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
         _equLbs[i]    = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
         _location[i]  = createCombo(leftBlock, SWT.READ_ONLY, 1 /*hSpan*/, locationNames);

         _buttons[i]  .setEnabled(i==0);
         _equipment[i].setEnabled(i==0);
         _location[i] .setEnabled(i==0);

         _buttons[i]  .addSelectionListener(this);
         _equipment[i].addSelectionListener(this);
         _equipment[i].addModifyListener(this);
         _location[i] .addModifyListener(this);
      }
      createLabel(leftBlock, "Armor: $", SWT.RIGHT, 2/*hSpan*/, null);
      _armorCost   = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      _armorLbs    = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      createLabel(leftBlock, "Lbs.", SWT.LEFT, 1/*hSpan*/, null);

      createLabel(leftBlock, "Totals: $", SWT.RIGHT, 2/*hSpan*/, null);
      _totalCost   = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      _totalLbs    = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      createLabel(leftBlock, "Lbs.", SWT.LEFT, 1/*hSpan*/, null);

      _folder = new TabFolder(rightBlock, SWT.NONE);
      _folder.addSelectionListener(this);
      GridData data = new GridData(GridData.FILL_HORIZONTAL | SWT.BORDER);
      data.horizontalAlignment = SWT.CENTER;
      data.horizontalSpan = 5;
      _folder.setLayoutData(data);
      Control[] itemList = new Control[_equipment.length*2];
      for (int i=0 ; i<_equipment.length ; i++) {
         itemList[i*2]   = _equipment[i];
         itemList[(i*2)+1] = _location[i];
      }
      leftBlock.setTabList(itemList);
      rightBlock.setTabList(new Control[] {_folder});

      TabItem item = new TabItem(_folder, SWT.NULL);
      item.setText( "Unarmed");
      Composite unarmedData = Helper.createComposite(_folder, 1, GridData.FILL_BOTH);
      item.setControl( unarmedData );
      unarmedData.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(_folder, SWT.NULL);
      item.setText( "Melee 1-handed");
      Composite meleeData1 = Helper.createComposite(_folder, 1, GridData.FILL_BOTH);
      item.setControl( meleeData1 );
      meleeData1.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(_folder, SWT.NULL);
      item.setText( "Melee 2-handed");
      Composite meleeData2 = Helper.createComposite(_folder, 1, GridData.FILL_BOTH);
      item.setControl( meleeData2 );
      meleeData2.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(_folder, SWT.NULL);
      item.setText( "Ranged");
      Composite rangedData = Helper.createComposite(_folder, 1, GridData.FILL_BOTH);
      item.setControl( rangedData );
      rangedData.setLayout(new GridLayout(4 /*columns*/,false/*sameWidth*/));

      int s=0;
      for (Composite parentComp : new Composite[] {unarmedData, meleeData1, meleeData2}) {
         createLabel(parentComp, "style",       SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "skill",       SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "ranges",      SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "attack time", SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "damage",      SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         for (int i=0 ; i<_styleName[s].length ; i++) {
            _styleName[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
            _skillName[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 50/*minWidth*/);
            _styleRange[s][i]  = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
            _styleTime[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 30/*minWidth*/);
            _styleDamage[s][i] = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
         }
         parentComp.setTabList(new Control[0]);
         s++;
      }

      createLabel(rangedData, "P-B range",    SWT.RIGHT, 1, null);
      _rangePB     = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      _rangeStyle = createText(rangedData, "", false, 1);

      createLabel(rangedData, "Short range",  SWT.RIGHT, 1, null);
      _rangeShort  = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      createLabel(rangedData, "Damage",    SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));

      createLabel(rangedData, "Medium range", SWT.RIGHT, 1, null);
      _rangeMedium = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      _rangeDamage = createText(rangedData, "0", false, 1);

      createLabel(rangedData, "Long range",   SWT.RIGHT, 1, null);
      _rangeLong   = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 2, null);

      rangedData.setTabList(new Control[0]);
   }
   @Override
   public void modifyText(ModifyEvent e) {
      _display.modifyText(e, this);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
       // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      int i=0;
      if (character != null) {
         for (Hand hand : character.getArms()) {
            String heldThingName = hand.getHeldThingName();
            Thing heldThing = Thing.getThing(heldThingName, character.getRace());
            String[] items = _location[i].getItems();
            boolean handFound = false;
            for (String element : items) {
               if (element.equals(hand._limbType.name)) {
                  handFound = true;
                  break;
               }
            }
            if (!handFound) {
               _location[i].add(hand._limbType.name);
            }
            _location[i].setText(hand._limbType.name);
            if (heldThing != null) {
               _equipment[i].setText(heldThingName);
               _equCost[i].setText(String.valueOf(heldThing.getCost()));
               _equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
               _equipment[i].setEnabled(true);
               _buttons[i].setEnabled(true);
               _location[i].setEnabled(true);
               i++;
            }
         }
         for (Thing equ : character.getEquipment()) {
            if (i >= _equipment.length) {
               break;
            }
            _equipment[i].setText(equ.getName());
            _equCost[i].setText(String.valueOf(equ.getCost()));
            _equLbs[i].setText(String.valueOf(equ.getAdjustedWeight()));
            _equipment[i].setEnabled(true);
            _buttons[i].setEnabled(true);
            _location[i].setEnabled(true);
            _location[i].setText(BELT);
            i++;
         }
      }

      boolean enabled = true;
      for ( ; i<_equipment.length ; ) {
         _equipment[i].setText("---");
         _equCost[i].setText("0");
         _equLbs[i].setText("0");
         _location[i].setText(BELT);
         _equipment[i].setEnabled(enabled);
         _buttons[i].setEnabled(enabled);
         _location[i].setEnabled(false);
         enabled = false;
         i++;
      }
      updateLocations();
   }
   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      boolean enabled = true;
      for (int i=0 ; i<_equipment.length ; i++) {
         String equName = _equipment[i].getText();
         if (equName.equals("---")) {
            _equCost[i].setText("0");
            _equLbs[i].setText("0");
            _equipment[i].setEnabled(enabled);
            _buttons[i].setEnabled(enabled);
            _location[i].setEnabled(false);
            enabled = false;
         }
         else {
            _equipment[i].setEnabled(true);
            _buttons[i].setEnabled(true);
            _location[i].setEnabled(true);
            if (character != null) {
               Thing heldThing = Thing.getThing(equName, character.getRace());
               if (heldThing != null) {
                  if (heldThing.isReal()) {
                     _equCost[i].setText(String.valueOf(heldThing.getCost()));
                     if ((heldThing.getRacialBase() == null) || (!character.getRace().getName().equals(heldThing.getRacialBase().getName()))) {
                        DebugBreak.debugBreak();
                     }
                     _equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
                  } else {
                     _equCost[i].setText("---");
                     _equLbs[i].setText("0");
                     if (equName.equalsIgnoreCase(Weapon.NAME_HeadButt) || equName.equalsIgnoreCase(Weapon.NAME_KarateKick)) {
                        _location[i].setText(BELT);
                        _location[i].setEnabled(true);
                     }
                  }
                  _equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
               }
            }
         }
      }
      while ((_selectedEqu >0) && !(_buttons[_selectedEqu].getEnabled())) {
         _buttons[_selectedEqu].setSelection(false);
         _buttons[--_selectedEqu].setSelection(true);
      }

      Thing selectedEqu = Thing.getThing(_equipment[_selectedEqu].getText(), (character != null) ? character.getRace() : null);
      Weapon weap = null;
      if ((selectedEqu instanceof Weapon) && selectedEqu.isReal()){
         weap = (Weapon) selectedEqu;
      }

      _rangePB    .setText("");
      _rangeShort .setText("");
      _rangeMedium.setText("");
      _rangeLong  .setText("");
      _rangeStyle .setText("");
      _rangeDamage.setText("");

      boolean has1Handed = false;
      boolean has2Handed = false;
      boolean hasRanged = false;
      int[] itemsPerStyle = new int[] {0, 0, 0};
      if (weap != null) {
         byte charStr = (_display._character == null) ? 0 : _display._character.getAttributeLevel(Attribute.Strength);
         byte damBase = (_display._character == null) ? 0 : _display._character.getPhysicalDamageBase();
         for (WeaponStyleAttack element : weap._attackStyles) {
            if (element.isRanged()) {
               WeaponStyleAttackRanged rangeStyle = (WeaponStyleAttackRanged) element;
               if (character != null) {
                  byte adjustedStrength = character.getAdjustedStrength();
                  if (rangeStyle.isThrown()) {
                     _rangePB .setText("-");
                  }
                  else {
                     _rangePB .setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.POINT_BLANK, adjustedStrength)));
                  }

                  _rangeShort .setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.SHORT,       adjustedStrength)));
                  _rangeMedium.setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.MEDIUM,      adjustedStrength)));
                  _rangeLong  .setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.LONG,        adjustedStrength)));
                  _rangeDamage.setText(element.getDamageString(damBase));
                  if (rangeStyle.isMissile()) {
                     _rangeStyle.setText("Missile");
                  }
                  else if (rangeStyle.isThrown()) {
                     _rangeStyle.setText("Thrown");
                  }
               }
               hasRanged = true;
            }
            else {
               int hands = (element.isTwoHanded()) ? 2 : 1;
               if (hands == 2) {
                  has2Handed = true;
               }
               else {
                  has1Handed = true;
               }
               String styleName = element.getName();
               int loc = styleName.indexOf(" ("+(hands)+"h)");
               if (loc > 0) {
                  styleName = styleName.substring(0, loc);
               }
               _styleName[hands][itemsPerStyle[hands]].setText(styleName);
               _skillName[hands][itemsPerStyle[hands]].setText(element.getSkillType().getName());
               String ranges = "" + element.getMinRange() + (element.getMinRange() == element.getMaxRange() ? "" : " - " + element.getMaxRange());
               _styleRange[hands][itemsPerStyle[hands]].setText(ranges);
               _styleTime[hands][itemsPerStyle[hands]].setText(String.valueOf(element.getSpeed(charStr)));
               _styleDamage[hands][itemsPerStyle[hands]].setText(element.getDamageString(damBase));
               itemsPerStyle[hands]++;
            }
         }
      }
      for (int s=0 ; s<_styleName.length ; s++) {
         for (int i=itemsPerStyle[s] ; i<_styleName[s].length ; i++) {
            _styleName[s][i].setText("");
            _skillName[s][i].setText("");
            _styleRange[s][i].setText("");
            _styleTime[s][i].setText("");
            _styleDamage[s][i].setText("");
         }
      }
      if ((_folder.getSelectionIndex() == 1) && !has1Handed) {
         if (has2Handed) {
            _folder.setSelection(2);
         }
         else if (hasRanged) {
            _folder.setSelection(3);
         }
         else {
            _folder.setSelection(0);
         }
      }
      else if ((_folder.getSelectionIndex() == 2) && !has2Handed) {
         if (has1Handed) {
            _folder.setSelection(1);
         }
         else if (hasRanged) {
            _folder.setSelection(3);
         }
         else {
            _folder.setSelection(0);
         }
      }
      else if ((_folder.getSelectionIndex() == 3) && !hasRanged) {
         if (has1Handed) {
            _folder.setSelection(1);
         }
         else if (has2Handed) {
            _folder.setSelection(2);
         }
         else {
            _folder.setSelection(0);
         }
      }
// TODO: can we disable the TabItem itself?
//      _folder.getItems()[1].getControl().setEnabled(has1Handed);
//      _folder.getItems()[2].getControl().setEnabled(has2Handed);
//      _folder.getItems()[3].getControl().setEnabled(hasRanged);
      _armorLbs.setText((character == null) ? "" : String.valueOf(character.getWeightArmor()));
      _armorCost.setText(((character == null) ? "" : String.valueOf(character.getArmor().getCost())));

      _totalLbs.setText((character == null) ? "" : String.valueOf(character.getWeightCarried()));
      _totalCost.setText(((character == null) ? "" : String.valueOf(character.getTotalCost())));
      updateLocations();
      updateUnarmedWeapons(character);
   }

   private void updateUnarmedWeapons(Character character) {
      List<Weapon> weapons = new ArrayList<>();
      List<WeaponStyleAttack> attackStyles = new ArrayList<>();
      int itemsPerStyle = 0;
      byte charStr = (_display._character == null) ? 0 : _display._character.getAttributeLevel(Attribute.Strength);
      byte damBase = (_display._character == null) ? 0 : _display._character.getPhysicalDamageBase();
      if (character != null) {
         List<Limb> limbs = character.getLimbs();
         for (Limb limb : limbs) {
            Weapon weapon = limb.getWeapon(character);
            if (weapon != null) {
               if (weapon.isReal()) {
                  // don't list weapons that aren't unarmed combat in this list.
                  continue;
               }
               if (!weapons.contains(weapon)) {
                  weapons.add(weapon);
                  for (WeaponStyleAttack element : weapon.getAttackStyles()) {
                     if (attackStyles.contains(element)) {
                        continue;
                     }
                     attackStyles.add(element);
                     int minSkill = element.getMinSkill();
                     byte charSkill = character.getSkillLevel(element, false/*adjustForPain*/,
                                                              limb._limbType/*useHand*/, false/*sizeAdjust*/,
                                                              false/*AdjustForEnumbrance*/, false/*adjustForHolds*/);
                     if ((minSkill == 0) || (minSkill <= charSkill)) {
                        String styleName = element.getName();
                        String skillName = element.getSkillType().getName();
                        _styleName[0][itemsPerStyle].setText(styleName);
                        if (element.getSkillPenalty() > 0) {
                           skillName += " - " + element.getSkillPenalty();
                        }
                        _skillName[0][itemsPerStyle].setText(skillName);
                        String ranges = "" + element.getMinRange() + (element.getMinRange() == element.getMaxRange() ? "" : " - " + element.getMaxRange());
                        _styleRange[0][itemsPerStyle].setText(ranges);
                        _styleTime[0][itemsPerStyle].setText(String.valueOf(element.getSpeed(charStr)));
                        _styleDamage[0][itemsPerStyle].setText(element.getDamageString(damBase));
                        itemsPerStyle++;
                        if (itemsPerStyle >= _styleName[0].length) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
      for (int i=itemsPerStyle ; i<_styleName[0].length ; i++) {
         _styleName[0][i].setText("");
         _skillName[0][i].setText("");
         _styleRange[0][i].setText("");
         _styleTime[0][i].setText("");
         _styleDamage[0][i].setText("");
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character == null) {
         return;
      }
      character.clearEquipmentList();
      HashSet<LimbType> handHoldsAnItem = new HashSet<>();
      for (int i=0 ; i<_equipment.length ; i++) {
         String equName = _equipment[i].getText();
         if (!equName.equals("---")) {
            String location = _location[i].getText();
            LimbType hand = LimbType.getByName(location);
            if (hand != null) {
               Limb limb = character.getLimb(hand);
               if (limb != null) {
                  limb.setHeldThing(null, character);
                  limb.setHeldThing(Thing.getThing(equName, character.getRace()), character);
                  handHoldsAnItem.add(hand);
               }
            }
            else {
               character.addEquipment(Thing.getThing(equName, true/*allowTool*/, character.getRace()));
            }
         }
      }
      // Clear the held item on each hand that isn't holding something from the above loop
      for (LimbType limbType : character.getRace().getLimbSet()) {
         if (!handHoldsAnItem.contains(limbType)) {
            Limb limb = character.getLimb(limbType);
            if (limb != null) {
               limb.setHeldThing(null, character);
            }
         }
      }
   }
   @Override
   public void widgetDefaultSelected(SelectionEvent e)
   {
   }
   @Override
   public void widgetSelected(SelectionEvent e)
   {
      for (int i=0 ; i<_buttons.length ; i++) {
         if ((e.widget == _buttons[i]) ||
             (e.widget == _equipment[i])) {
            if (e.widget == _equipment[i]) {
               _buttons[_selectedEqu].setSelection(false);
               _buttons[i].setSelection(true);
            }
            _selectedEqu  = i;
            refreshDisplay(_display._character);
            return;
         }
      }
   }
   public void updateLocations() {
      // Build the list of valid locations
      List<String> locationNames  = new ArrayList<>();
      locationNames.add(BELT);
      Character self = _display._character;
      for (Hand hand : self.getArms()) {
         locationNames.add(hand._limbType.name);
      }
      // remove the occupied locations, and check for 2-handed weapons
      for (int i=0 ; i<_location.length ; i++) {
         String selectedHand = _location[i].getText();
         if (locationNames.contains(selectedHand)) {
            if (!selectedHand.equals(BELT)) {
               locationNames.remove(selectedHand);
               Thing thing = Thing.getThing(_equipment[i].getText(), self.getRace());
               if (thing instanceof Weapon) {
                  Weapon weap = (Weapon) thing;
                  if (weap.isOnlyTwoHanded()) {
                     for (LimbType limbType : self.getRace().getLimbSet()) {
                        if (selectedHand.equals(limbType.name)) {
                           LimbType pairedType = limbType.getPairedType();
                           String pairHand = null;
                           if (pairedType != null) {
                              pairHand = pairedType.name;
                           }
                           if (locationNames.contains(pairHand)) {
                              locationNames.remove(pairHand);
                           }
                           else {
                              _location[i].setText("---");
                              _location[i].setEnabled(false);
                           }
                        }
                     }
                  }
                  else if (!weap.isReal()) {
                     _location[i].setText(BELT);
                  }
               }
            }
         }
         else {
            _location[i].setText(BELT);
         }
      }
      // remove the occupied locations, and check for 2-handed weapons
      for (int i=0 ; i<_location.length ; i++) {
         String selectedHand = _location[i].getText();
         for (int j=0 ; j<_location[i].getItemCount() ; j++) {
            String item = _location[i].getItem(j);
            if (!item.equals(selectedHand) && !(locationNames.contains(item))) {
               _location[i--].remove(j);
               break;
            }
         }
      }
      // Add the unused positions:
      for (String locName : locationNames) {
         for (Combo element : _location) {
            boolean itemFound = false;
            String[] items = element.getItems();
            for (String element2 : items) {
               if (element2.equals(locName)) {
                  itemFound = true;
                  break;
               }
            }
            if(!itemFound) {
               element.add(locName);
            }
         }
      }
   }
}
