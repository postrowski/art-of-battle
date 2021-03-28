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
   private final CharacterWidget display;
   static final  int             LINES_OF_EQU = 5;
   public final  String          BELT         = "Belt";
   private final Button[]        buttons      = new Button[LINES_OF_EQU];
   private final Combo[]         equipment    = new Combo[LINES_OF_EQU];
   private final Text[]          equCost      = new Text[LINES_OF_EQU];
   private final Text[]          equLbs       = new Text[LINES_OF_EQU];
   private final Combo[]         location     = new Combo[LINES_OF_EQU];
   private       Text            armorCost    = null;
   private       Text            armorLbs     = null;
   private       Text            totalCost    = null;
   private       Text            totalLbs     = null;

   static final  int      MAX_ATTACK_STYLES_PER_WEAPON = 5;
   private final Text[][] styleName                    = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][] skillName                    = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][] styleRange                   = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][] styleTime                    = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];
   private final Text[][] styleDamage                  = new Text[3][MAX_ATTACK_STYLES_PER_WEAPON];

   private Text      rangePB     = null;
   private Text      rangeShort  = null;
   private Text      rangeMedium = null;
   private Text      rangeLong   = null;
   private Text      rangeDamage = null;
   private Text      rangeStyle  = null;
   private int       selectedEqu = 0;
   private TabFolder folder;

   public WeaponBlock(CharacterWidget display) {
      this.display = display;
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
      for (LimbType limbType : display.character.getRace().getLimbSet()) {
         if (limbType.isHand()) {
            if (display.character.getLimb(limbType) != null) {
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
      for (int i = 0; i < equipment.length ; i++) {
         buttons[i]   = new Button(leftBlock, SWT.LEFT | SWT.RADIO);
         equipment[i] = createCombo(leftBlock, SWT.READ_ONLY, 1/*hSpan*/, equipmentNames);
         equCost[i]   = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
         equLbs[i]    = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
         location[i]  = createCombo(leftBlock, SWT.READ_ONLY, 1 /*hSpan*/, locationNames);

         buttons[i].setEnabled(i == 0);
         equipment[i].setEnabled(i == 0);
         location[i].setEnabled(i == 0);

         buttons[i].addSelectionListener(this);
         equipment[i].addSelectionListener(this);
         equipment[i].addModifyListener(this);
         location[i].addModifyListener(this);
      }
      createLabel(leftBlock, "Armor: $", SWT.RIGHT, 2/*hSpan*/, null);
      armorCost = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      armorLbs = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      createLabel(leftBlock, "Lbs.", SWT.LEFT, 1/*hSpan*/, null);

      createLabel(leftBlock, "Totals: $", SWT.RIGHT, 2/*hSpan*/, null);
      totalCost = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      totalLbs = createText(leftBlock, "0", false/*editable*/, 1/*hSpan*/);
      createLabel(leftBlock, "Lbs.", SWT.LEFT, 1/*hSpan*/, null);

      folder = new TabFolder(rightBlock, SWT.NONE);
      folder.addSelectionListener(this);
      GridData data = new GridData(GridData.FILL_HORIZONTAL | SWT.BORDER);
      data.horizontalAlignment = SWT.CENTER;
      data.horizontalSpan = 5;
      folder.setLayoutData(data);
      Control[] itemList = new Control[equipment.length * 2];
      for (int i = 0; i < equipment.length ; i++) {
         itemList[i*2]   = equipment[i];
         itemList[(i*2)+1] = location[i];
      }
      leftBlock.setTabList(itemList);
      rightBlock.setTabList(new Control[] {folder});

      TabItem item = new TabItem(folder, SWT.NULL);
      item.setText( "Unarmed");
      Composite unarmedData = Helper.createComposite(folder, 1, GridData.FILL_BOTH);
      item.setControl( unarmedData );
      unarmedData.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(folder, SWT.NULL);
      item.setText( "Melee 1-handed");
      Composite meleeData1 = Helper.createComposite(folder, 1, GridData.FILL_BOTH);
      item.setControl( meleeData1 );
      meleeData1.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(folder, SWT.NULL);
      item.setText( "Melee 2-handed");
      Composite meleeData2 = Helper.createComposite(folder, 1, GridData.FILL_BOTH);
      item.setControl( meleeData2 );
      meleeData2.setLayout(new GridLayout(5 /*columns*/,false/*sameWidth*/));

      item = new TabItem(folder, SWT.NULL);
      item.setText( "Ranged");
      Composite rangedData = Helper.createComposite(folder, 1, GridData.FILL_BOTH);
      item.setControl( rangedData );
      rangedData.setLayout(new GridLayout(4 /*columns*/,false/*sameWidth*/));

      int s=0;
      for (Composite parentComp : new Composite[] {unarmedData, meleeData1, meleeData2}) {
         createLabel(parentComp, "style",       SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "skill",       SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "ranges",      SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "attack time", SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         createLabel(parentComp, "damage",      SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));
         for (int i = 0; i < styleName[s].length ; i++) {
            styleName[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
            skillName[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 50/*minWidth*/);
            styleRange[s][i]  = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
            styleTime[s][i]   = createText(parentComp, null, false, 1, null/*fontData*/, 30/*minWidth*/);
            styleDamage[s][i] = createText(parentComp, null, false, 1, null/*fontData*/, 40/*minWidth*/);
         }
         parentComp.setTabList(new Control[0]);
         s++;
      }

      createLabel(rangedData, "P-B range",    SWT.RIGHT, 1, null);
      rangePB = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      rangeStyle = createText(rangedData, "", false, 1);

      createLabel(rangedData, "Short range",  SWT.RIGHT, 1, null);
      rangeShort = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      createLabel(rangedData, "Damage",    SWT.CENTER, 1, new FontData("Arial", 8, SWT.BOLD));

      createLabel(rangedData, "Medium range", SWT.RIGHT, 1, null);
      rangeMedium = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 1, null);
      rangeDamage = createText(rangedData, "0", false, 1);

      createLabel(rangedData, "Long range",   SWT.RIGHT, 1, null);
      rangeLong = createText(rangedData, " ", false, 1);
      createLabel(rangedData, " ",    SWT.CENTER, 2, null);

      rangedData.setTabList(new Control[0]);
   }
   @Override
   public void modifyText(ModifyEvent e) {
      display.modifyText(e, this);
   }
   @Override
   public void updateDisplayFromCharacter(Character character) {
       // updateDisplayFromCharacter is used to update fields that have ModifyListeners:
      int i=0;
      if (character != null) {
         for (Hand hand : character.getArms()) {
            String heldThingName = hand.getHeldThingName();
            Thing heldThing = Thing.getThing(heldThingName, character.getRace());
            String[] items = location[i].getItems();
            boolean handFound = false;
            for (String element : items) {
               if (element.equals(hand.limbType.name)) {
                  handFound = true;
                  break;
               }
            }
            if (!handFound) {
               location[i].add(hand.limbType.name);
            }
            location[i].setText(hand.limbType.name);
            if (heldThing != null) {
               equipment[i].setText(heldThingName);
               equCost[i].setText(String.valueOf(heldThing.getCost()));
               equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
               equipment[i].setEnabled(true);
               buttons[i].setEnabled(true);
               location[i].setEnabled(true);
               i++;
            }
         }
         for (Thing equ : character.getEquipment()) {
            if (i >= equipment.length) {
               break;
            }
            equipment[i].setText(equ.getName());
            equCost[i].setText(String.valueOf(equ.getCost()));
            equLbs[i].setText(String.valueOf(equ.getAdjustedWeight()));
            equipment[i].setEnabled(true);
            buttons[i].setEnabled(true);
            location[i].setEnabled(true);
            location[i].setText(BELT);
            i++;
         }
      }

      boolean enabled = true;
      for ( ; i < equipment.length ; ) {
         equipment[i].setText("---");
         equCost[i].setText("0");
         equLbs[i].setText("0");
         location[i].setText(BELT);
         equipment[i].setEnabled(enabled);
         buttons[i].setEnabled(enabled);
         location[i].setEnabled(false);
         enabled = false;
         i++;
      }
      updateLocations();
   }
   @Override
   public void refreshDisplay(Character character) {
      // refreshDisplay is used to update fields that don't have ModifyListeners:
      boolean enabled = true;
      for (int i = 0; i < equipment.length ; i++) {
         String equName = equipment[i].getText();
         if (equName.equals("---")) {
            equCost[i].setText("0");
            equLbs[i].setText("0");
            equipment[i].setEnabled(enabled);
            buttons[i].setEnabled(enabled);
            location[i].setEnabled(false);
            enabled = false;
         }
         else {
            equipment[i].setEnabled(true);
            buttons[i].setEnabled(true);
            location[i].setEnabled(true);
            if (character != null) {
               Thing heldThing = Thing.getThing(equName, character.getRace());
               if (heldThing != null) {
                  if (heldThing.isReal()) {
                     equCost[i].setText(String.valueOf(heldThing.getCost()));
                     if ((heldThing.getRacialBase() == null) || (!character.getRace().getName().equals(heldThing.getRacialBase().getName()))) {
                        DebugBreak.debugBreak();
                     }
                     equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
                  } else {
                     equCost[i].setText("---");
                     equLbs[i].setText("0");
                     if (equName.equalsIgnoreCase(Weapon.NAME_HeadButt) || equName.equalsIgnoreCase(Weapon.NAME_KarateKick)) {
                        location[i].setText(BELT);
                        location[i].setEnabled(true);
                     }
                  }
                  equLbs[i].setText(String.valueOf(heldThing.getAdjustedWeight()));
               }
            }
         }
      }
      while ((selectedEqu > 0) && !(buttons[selectedEqu].getEnabled())) {
         buttons[selectedEqu].setSelection(false);
         buttons[--selectedEqu].setSelection(true);
      }

      Thing selectedEqu = Thing.getThing(equipment[this.selectedEqu].getText(), (character != null) ? character.getRace() : null);
      Weapon weap = null;
      if ((selectedEqu instanceof Weapon) && selectedEqu.isReal()){
         weap = (Weapon) selectedEqu;
      }

      rangePB.setText("");
      rangeShort.setText("");
      rangeMedium.setText("");
      rangeLong.setText("");
      rangeStyle.setText("");
      rangeDamage.setText("");

      boolean has1Handed = false;
      boolean has2Handed = false;
      boolean hasRanged = false;
      int[] itemsPerStyle = new int[] {0, 0, 0};
      if (weap != null) {
         byte charStr = (display.character == null) ? 0 : display.character.getAttributeLevel(Attribute.Strength);
         byte damBase = (display.character == null) ? 0 : display.character.getPhysicalDamageBase();
         for (WeaponStyleAttack element : weap.attackStyles) {
            if (element.isRanged()) {
               WeaponStyleAttackRanged rangeStyle = (WeaponStyleAttackRanged) element;
               if (character != null) {
                  byte adjustedStrength = character.getAdjustedStrength();
                  if (rangeStyle.isThrown()) {
                     rangePB.setText("-");
                  }
                  else {
                     rangePB.setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.POINT_BLANK, adjustedStrength)));
                  }

                  rangeShort.setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.SHORT, adjustedStrength)));
                  rangeMedium.setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.MEDIUM, adjustedStrength)));
                  rangeLong.setText(String.valueOf(rangeStyle.getDistanceForRange(RANGE.LONG, adjustedStrength)));
                  rangeDamage.setText(element.getDamageString(damBase));
                  if (rangeStyle.isMissile()) {
                     this.rangeStyle.setText("Missile");
                  }
                  else if (rangeStyle.isThrown()) {
                     this.rangeStyle.setText("Thrown");
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
               this.styleName[hands][itemsPerStyle[hands]].setText(styleName);
               skillName[hands][itemsPerStyle[hands]].setText(element.getSkillType().getName());
               String ranges = "" + element.getMinRange() + (element.getMinRange() == element.getMaxRange() ? "" : " - " + element.getMaxRange());
               styleRange[hands][itemsPerStyle[hands]].setText(ranges);
               styleTime[hands][itemsPerStyle[hands]].setText(String.valueOf(element.getSpeed(charStr)));
               styleDamage[hands][itemsPerStyle[hands]].setText(element.getDamageString(damBase));
               itemsPerStyle[hands]++;
            }
         }
      }
      for (int s = 0; s < styleName.length ; s++) {
         for (int i = itemsPerStyle[s]; i < styleName[s].length ; i++) {
            styleName[s][i].setText("");
            skillName[s][i].setText("");
            styleRange[s][i].setText("");
            styleTime[s][i].setText("");
            styleDamage[s][i].setText("");
         }
      }
      if ((folder.getSelectionIndex() == 1) && !has1Handed) {
         if (has2Handed) {
            folder.setSelection(2);
         }
         else if (hasRanged) {
            folder.setSelection(3);
         }
         else {
            folder.setSelection(0);
         }
      }
      else if ((folder.getSelectionIndex() == 2) && !has2Handed) {
         if (has1Handed) {
            folder.setSelection(1);
         }
         else if (hasRanged) {
            folder.setSelection(3);
         }
         else {
            folder.setSelection(0);
         }
      }
      else if ((folder.getSelectionIndex() == 3) && !hasRanged) {
         if (has1Handed) {
            folder.setSelection(1);
         }
         else if (has2Handed) {
            folder.setSelection(2);
         }
         else {
            folder.setSelection(0);
         }
      }
// TODO: can we disable the TabItem itself?
//      folder.getItems()[1].getControl().setEnabled(has1Handed);
//      folder.getItems()[2].getControl().setEnabled(has2Handed);
//      folder.getItems()[3].getControl().setEnabled(hasRanged);
      armorLbs.setText((character == null) ? "" : String.valueOf(character.getWeightArmor()));
      armorCost.setText(((character == null) ? "" : String.valueOf(character.getArmor().getCost())));

      totalLbs.setText((character == null) ? "" : String.valueOf(character.getWeightCarried()));
      totalCost.setText(((character == null) ? "" : String.valueOf(character.getTotalCost())));
      updateLocations();
      updateUnarmedWeapons(character);
   }

   private void updateUnarmedWeapons(Character character) {
      List<Weapon> weapons = new ArrayList<>();
      List<WeaponStyleAttack> attackStyles = new ArrayList<>();
      int itemsPerStyle = 0;
      byte charStr = (display.character == null) ? 0 : display.character.getAttributeLevel(Attribute.Strength);
      byte damBase = (display.character == null) ? 0 : display.character.getPhysicalDamageBase();
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
                                                              limb.limbType/*useHand*/, false/*sizeAdjust*/,
                                                              false/*AdjustForEnumbrance*/, false/*adjustForHolds*/);
                     if ((minSkill == 0) || (minSkill <= charSkill)) {
                        String styleName = element.getName();
                        String skillName = element.getSkillType().getName();
                        this.styleName[0][itemsPerStyle].setText(styleName);
                        if (element.getSkillPenalty() > 0) {
                           skillName += " - " + element.getSkillPenalty();
                        }
                        this.skillName[0][itemsPerStyle].setText(skillName);
                        String ranges = "" + element.getMinRange() + (element.getMinRange() == element.getMaxRange() ? "" : " - " + element.getMaxRange());
                        styleRange[0][itemsPerStyle].setText(ranges);
                        styleTime[0][itemsPerStyle].setText(String.valueOf(element.getSpeed(charStr)));
                        styleDamage[0][itemsPerStyle].setText(element.getDamageString(damBase));
                        itemsPerStyle++;
                        if (itemsPerStyle >= this.styleName[0].length) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
      for (int i = itemsPerStyle; i < styleName[0].length ; i++) {
         styleName[0][i].setText("");
         skillName[0][i].setText("");
         styleRange[0][i].setText("");
         styleTime[0][i].setText("");
         styleDamage[0][i].setText("");
      }
   }

   @Override
   public void updateCharacterFromDisplay(Character character) {
      if (character == null) {
         return;
      }
      character.clearEquipmentList();
      HashSet<LimbType> handHoldsAnItem = new HashSet<>();
      for (int i = 0; i < equipment.length ; i++) {
         String equName = equipment[i].getText();
         if (!equName.equals("---")) {
            String location = this.location[i].getText();
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
      for (int i = 0; i < buttons.length ; i++) {
         if ((e.widget == buttons[i]) ||
             (e.widget == equipment[i])) {
            if (e.widget == equipment[i]) {
               buttons[selectedEqu].setSelection(false);
               buttons[i].setSelection(true);
            }
            selectedEqu = i;
            refreshDisplay(display.character);
            return;
         }
      }
   }
   public void updateLocations() {
      // Build the list of valid locations
      List<String> locationNames  = new ArrayList<>();
      locationNames.add(BELT);
      Character self = display.character;
      for (Hand hand : self.getArms()) {
         locationNames.add(hand.limbType.name);
      }
      // remove the occupied locations, and check for 2-handed weapons
      for (int i = 0; i < location.length ; i++) {
         String selectedHand = location[i].getText();
         if (locationNames.contains(selectedHand)) {
            if (!selectedHand.equals(BELT)) {
               locationNames.remove(selectedHand);
               Thing thing = Thing.getThing(equipment[i].getText(), self.getRace());
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
                              location[i].setText("---");
                              location[i].setEnabled(false);
                           }
                        }
                     }
                  }
                  else if (!weap.isReal()) {
                     location[i].setText(BELT);
                  }
               }
            }
         }
         else {
            location[i].setText(BELT);
         }
      }
      // remove the occupied locations, and check for 2-handed weapons
      for (int i = 0; i < location.length ; i++) {
         String selectedHand = location[i].getText();
         for (int j = 0; j < location[i].getItemCount() ; j++) {
            String item = location[i].getItem(j);
            if (!item.equals(selectedHand) && !(locationNames.contains(item))) {
               location[i--].remove(j);
               break;
            }
         }
      }
      // Add the unused positions:
      for (String locName : locationNames) {
         for (Combo element : location) {
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
