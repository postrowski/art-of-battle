package ostrowski.combat.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import ostrowski.combat.common.CombatMap;
import ostrowski.ui.Helper;

import java.io.*;

public class MapInterface extends Helper implements SelectionListener, ModifyListener {

   private Button       newMapButton;
   private Button       openMapButton;
   private Button       saveMapButton;
   private Text         arenaSizeXValue;
   private Text         arenaSizeYValue;
   private Button       setSizeButton;
   private Button       hideViewFromLocalPlayersButton;
   private Button       mapKnownToAllPlayersButton;
   private CombatServer combatServer;
   public  Text         bgImageFilePath;
   public  Button       bgImageFileBtn;
   public  Slider       bgImageAlphaSlider;
   private CombatMap    originalMap;
   private String       currentMapFileName;

   public MapInterface(CombatServer combatServer){
      this.combatServer = combatServer;
   }

   public void openPort() {
      arenaSizeXValue.setEnabled(false);
      arenaSizeYValue.setEnabled(false);
   }
   public void closePort() {
      arenaSizeXValue.setEnabled(true);
      arenaSizeYValue.setEnabled(true);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if ((e.widget == arenaSizeXValue) || (e.widget == arenaSizeYValue)) {
         short x = Short.parseShort(arenaSizeXValue.getText());
         short y = (short)(Short.parseShort(arenaSizeYValue.getText()) * 2);
         CombatMap combatMap = combatServer.map.getCombatMap();
         setSizeButton.setEnabled((combatMap.getSizeX() != x) || (combatMap.getSizeY() != y));
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == setSizeButton) {
         combatServer.setMapSize(Short.parseShort(arenaSizeXValue.getText()),
                                 (short)(Short.parseShort(arenaSizeYValue.getText()) * 2));
         setSizeButton.setEnabled(false);
      }
      else if (e.widget == hideViewFromLocalPlayersButton) {
         combatServer.map.setHideViewFromLocalPlayers(hideViewFromLocalPlayersButton.getSelection());
      }
      else if (e.widget == mapKnownToAllPlayersButton) {
         combatServer.map.setKnownByAllPlayers(mapKnownToAllPlayersButton.getSelection());
      }
      else if (e.widget == bgImageAlphaSlider) {
         combatServer.map.setBackgroundAlpha(bgImageAlphaSlider.getSelection());
         combatServer.map.getCombatMap().setBackgroundImageAlpha(bgImageAlphaSlider.getSelection());
      }
      else if (e.widget == bgImageFileBtn) {
         Shell shell = e.display.getShells()[0];
         FileDialog fd = new FileDialog(shell, SWT.OPEN);
         fd.setText("Open");
         fd.setFilterPath("Arenas");
         fd.setFilterExtensions(new String[] {"*.png;*.gif;*.jpg", "*.*"});
         fd.setFilterNames(new String[] {"Image Files (*.png;*.gif;*.jpg)", "All files(*.*)"});
         String selected = fd.open();
         if (selected != null && !selected.isEmpty()) {
            String baseDir = System.getProperty("user.dir");
            String fileSeparator = System.getProperty("file.separator");
            baseDir = baseDir + fileSeparator + "arenas";
            if (selected.startsWith(baseDir)) {
               selected = selected.substring(baseDir.length());
            }
            else {
               // copy the file to the same directory as the arenas are stored
               File file = new File(selected);
               File newFile = new File(baseDir + fileSeparator + file.getName());
               String fullPath = newFile.getAbsolutePath();
               int i=0;
               while (newFile.exists()) {
                  newFile = new File(fullPath.split(".")[0] + i++ + fullPath.split(".")[1]);
               }
               try (InputStream in = new BufferedInputStream(new FileInputStream(file));
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {

                  byte[] buffer = new byte[1024];
                  int lengthRead;
                  while ((lengthRead = in.read(buffer)) > 0) {
                     out.write(buffer, 0, lengthRead);
                     out.flush();
                  }
               } catch (IOException ex) {
                  ex.printStackTrace();
               }
               selected = newFile.getName();
            }
            bgImageFilePath.setText(selected);
            combatServer.map.getCombatMap().setBackgroundImagePath(selected);
         }
      }
      else if (e.widget == saveMapButton) {
         writeArenaMapToFile(combatServer.getArena().getCombatMap(), true/*overwriteExistingFile*/);
         originalMap = combatServer.getArena().getCombatMap().clone();
      }
      else if (e.widget == openMapButton) {
         FileDialog dialog = new FileDialog(combatServer.getShell());
         dialog.setFilterExtensions(new String[] {"*.xml"});
         dialog.setFilterNames(new String[] {"Arena Files (*.xml)"});
         dialog.setFilterPath("Arenas");

         // Disable the current terrain and wall settings, or the mouse up
         // event will fire to the terrain interface after the load completes,
         // and the map will be edited immediately:
         combatServer.terrainInterface.disableCurrentEdits();

         String fileName = dialog.open();
         if ((fileName != null) && (fileName.length() > 0)) {
            File sourceFile = new File("Arenas" + File.separator + fileName);
            if (!sourceFile.exists()) {
               sourceFile = new File(fileName);
            }
            if (sourceFile.exists()) {
               if (sourceFile.canRead()) {
                  currentMapFileName = sourceFile.getAbsolutePath();
                  CombatMap map = new CombatMap();
                  map.serializeFromFile(sourceFile);
                  combatServer.setMap(map, true/*clearCombatants*/);
                  combatServer.map.setZoomToFit();
               }
            }
         }
      }
      else if (e.widget == newMapButton) {
         NewMapDialog dialog = new NewMapDialog(combatServer.getShell());
         dialog.open();
         if (!dialog.isCanceled()) {
            CombatMap map = new CombatMap(dialog.getSizeX(), (short) (dialog.getSizeY()*2), null/*diag*/);
            map.setName(dialog.getName());
            originalMap = map.clone();
            combatServer.setMap(map, true/*clearCombatants*/);
            currentMapFileName = null; // make sure we don't overwrite the current file
         }
      }
      refreshSaveButton();
   }

   public void refreshSaveButton() {
      if (saveMapButton != null) {
         boolean enabled = (originalMap == null) || (!originalMap.equals(combatServer.getArena().getCombatMap()));
         saveMapButton.setEnabled(enabled);
      }
   }

   public void writeArenaMapToFile(CombatMap map, boolean overwriteExistingFile) {
      File sourceDir = new File("Arenas");
      if (sourceDir.exists() && sourceDir.isFile()) {
         sourceDir.delete();
      }
      if (!sourceDir.exists()) {
         sourceDir.mkdirs();
      }
      File sourceFile;
      if (currentMapFileName == null) {
         sourceFile = new File("Arenas" + File.separator + map.getName().toLowerCase() + ".xml");
      }
      else {
         sourceFile = new File(currentMapFileName);
      }

      try {
         if (sourceFile.exists()) {
            if (!overwriteExistingFile) {
               return;
            }
            sourceFile.delete();
         }

         sourceFile.createNewFile();
         if (sourceFile.exists() && sourceFile.canWrite()) {
            map.serializeToFile(sourceFile);
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }


   @Override
   public void widgetDefaultSelected(SelectionEvent e) {

   }

   public void buildBlock(Composite parent) {
      Composite main = new Composite(parent, SWT.TRAIL);
      GridLayout mainGrid = new GridLayout(3, false);
      mainGrid.verticalSpacing = 1;
      mainGrid.horizontalSpacing = 1;
      main.setLayout(mainGrid);
      buildArenaNameBlock(main);
      buildArenaSizeBlock(main);
      {
         Composite bgImgBlock = createGroup(main, "Background Image", 4/*columns*/, false/*sameSize*/,
                                            1 /*hSpacing*/, 1 /*vSpacing*/);
         GridLayout grid = new GridLayout(7, false);
         grid.verticalSpacing = 1;
         grid.horizontalSpacing = 1;
         bgImgBlock.setLayout(grid);
         createLabel(bgImgBlock, "File:", SWT.LEFT, 1/*hSpan*/, null);
         bgImageFilePath = createText(bgImgBlock, "", false/*editable*/, 4/*hSpan*/);
         bgImageFileBtn = createButton(bgImgBlock, "choose", 2, null, this);
         createLabel(bgImgBlock, "Alpha", SWT.LEFT, 1/*hSpan*/, null);
         bgImageAlphaSlider = new Slider(bgImgBlock, SWT.HORIZONTAL);
         GridData sliderGridData = new GridData(GridData.FILL_HORIZONTAL);
         sliderGridData.horizontalSpan = 6;
         bgImageAlphaSlider.setLayoutData(sliderGridData);
         bgImageAlphaSlider.setBounds(0, 30, 200, 20);
         bgImageAlphaSlider.setMaximum(255);
         bgImageAlphaSlider.setMinimum(0);
         bgImageAlphaSlider.setIncrement(16);
         bgImageAlphaSlider.addSelectionListener(this);

         GridData data = new GridData(GridData.FILL_VERTICAL);
         data.grabExcessVerticalSpace = true;
         bgImgBlock.setLayoutData(data);
      }
   }

   /**
    * @param parent
    */
   private void buildArenaNameBlock(Composite parent)
   {
      Composite block = new Composite(parent, SWT.CENTER);
      GridLayout grid = new GridLayout(3, false);
      block.setLayout(grid);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalAlignment = SWT.CENTER;
      data.grabExcessHorizontalSpace = true;
      block.setLayoutData(data);

      newMapButton = new Button(block, SWT.PUSH);
      newMapButton.setText("New Map");
      newMapButton.addSelectionListener(this);

      saveMapButton = new Button(block, SWT.PUSH);
      saveMapButton.setText("Save Map");
      saveMapButton.addSelectionListener(this);

      openMapButton = new Button(block, SWT.PUSH);
      openMapButton.setText("Load Map");
      openMapButton.addSelectionListener(this);
   }

   /**
    * @param parent
    */
   private void buildArenaSizeBlock(Composite parent)
   {
      Composite outerBlock = createGroup(parent, "Arena size", 1, false,1 , 1);
      {
         Composite sizeBlock = new Composite(outerBlock, SWT.TRAIL);
         GridLayout grid = new GridLayout(5, false);
         sizeBlock.setLayout(grid);
         new Label(sizeBlock, SWT.LEFT).setText("Width:");

         arenaSizeXValue = new Text(sizeBlock, SWT.LEFT | SWT.BORDER);
         arenaSizeXValue.setText("20");
         arenaSizeXValue.setSize(30, 20);
         GridData data = new GridData();
         data.minimumWidth = 30;
         data.grabExcessHorizontalSpace = true;
         arenaSizeXValue.setLayoutData(data);
         new Label(sizeBlock, SWT.CENTER).setText(" Height:");

         arenaSizeYValue = new Text(sizeBlock, SWT.LEFT | SWT.BORDER);
         arenaSizeYValue.setText("14");
         arenaSizeYValue.setSize(30, 20);
         data = new GridData();
         data.minimumWidth = 30;
         data.grabExcessHorizontalSpace = true;
         arenaSizeYValue.setLayoutData(data);

         arenaSizeXValue.addModifyListener(this);
         arenaSizeYValue.addModifyListener(this);

         setSizeButton = new Button(sizeBlock, SWT.PUSH);
         setSizeButton.setText("Set Size");
         setSizeButton.addSelectionListener(this);
         setSizeButton.setEnabled(false);
      }

      hideViewFromLocalPlayersButton = new Button(outerBlock, SWT.CHECK);
      hideViewFromLocalPlayersButton.setText("Hide map from local players.");
      hideViewFromLocalPlayersButton.setSelection(false);
      hideViewFromLocalPlayersButton.addSelectionListener(this);

      mapKnownToAllPlayersButton = new Button(outerBlock, SWT.CHECK);
      mapKnownToAllPlayersButton.setText("All players know the map.");
      mapKnownToAllPlayersButton.setSelection(false);
      mapKnownToAllPlayersButton.addSelectionListener(this);

   }

   public void setMap(CombatMap map) {
      arenaSizeXValue.setText(String.valueOf(map.getSizeX()));
      arenaSizeYValue.setText(String.valueOf(map.getSizeY() / 2));
      hideViewFromLocalPlayersButton.setSelection(map.isHideViewFromLocalPlayers());
      mapKnownToAllPlayersButton.setSelection(map.isKnownByAllPlayers());
      combatServer.map.setBackgroundAlpha(bgImageAlphaSlider.getSelection());

      String backgroundImagePath = "";
      if (combatServer.map.getCombatMap() != null) {
         backgroundImagePath = combatServer.map.getCombatMap().getBackgroundImagePath();
         bgImageAlphaSlider.setSelection(combatServer.map.getCombatMap().getBackgroundImageAlpha());
         if (backgroundImagePath == null) {
            backgroundImagePath = "";
         }
      }
      bgImageFilePath.setText(backgroundImagePath);
   }

   public short getMapSizeX() {
      return Short.parseShort(arenaSizeXValue.getText().trim());
   }
   public short getMapSizeY() {
      return (short)(Short.parseShort(arenaSizeYValue.getText().trim()) * 2);
   }
}
