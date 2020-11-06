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

   private Button       _newMapButton;
   private Button       _openMapButton;
   private Button       _saveMapButton;
   private Text         _arenaSizeXValue;
   private Text         _arenaSizeYValue;
   private Button       _setSizeButton;
   private Button       _hideViewFromLocalPlayersButton;
   private Button       _mapKnownToAllPlayersButton;
   private CombatServer _combatServer;
   public  Text         _bgImageFilePath;
   public  Button       _bgImageFileBtn;
   public  Slider       _bgImageAlphaSlider;
   private CombatMap    _originalMap;
   private String       _currentMapFileName;

   public MapInterface(CombatServer combatServer){
      _combatServer = combatServer;
   }

   public void openPort() {
      _arenaSizeXValue.setEnabled(false);
      _arenaSizeYValue.setEnabled(false);
   }
   public void closePort() {
      _arenaSizeXValue.setEnabled(true);
      _arenaSizeYValue.setEnabled(true);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if ((e.widget == _arenaSizeXValue) || (e.widget == _arenaSizeYValue)) {
         short x = Short.parseShort(_arenaSizeXValue.getText());
         short y = (short)(Short.parseShort(_arenaSizeYValue.getText()) * 2);
         CombatMap combatMap = _combatServer._map.getCombatMap();
         _setSizeButton.setEnabled((combatMap.getSizeX() != x) || (combatMap.getSizeY() != y));
      }
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _setSizeButton) {
         _combatServer.setMapSize(Short.parseShort(_arenaSizeXValue.getText()),
                                  (short)(Short.parseShort(_arenaSizeYValue.getText())*2));
         _setSizeButton.setEnabled(false);
      }
      else if (e.widget == _hideViewFromLocalPlayersButton) {
         _combatServer._map.setHideViewFromLocalPlayers(_hideViewFromLocalPlayersButton.getSelection());
      }
      else if (e.widget == _mapKnownToAllPlayersButton) {
         _combatServer._map.setKnownByAllPlayers(_mapKnownToAllPlayersButton.getSelection());
      }
      else if (e.widget == _bgImageAlphaSlider) {
         _combatServer._map.setBackgroundAlpha(_bgImageAlphaSlider.getSelection());
         _combatServer._map.getCombatMap().setBackgroundImageAlpha(_bgImageAlphaSlider.getSelection());
      }
      else if (e.widget == _bgImageFileBtn) {
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
            _bgImageFilePath.setText(selected);
            _combatServer._map.getCombatMap().setBackgroundImagePath(selected);
         }
      }
      else if (e.widget == _saveMapButton) {
         writeArenaMapToFile(_combatServer.getArena().getCombatMap(), true/*overwriteExistingFile*/);
         _originalMap = _combatServer.getArena().getCombatMap().clone();
      }
      else if (e.widget == _openMapButton) {
         FileDialog dialog = new FileDialog(_combatServer.getShell());
         dialog.setFilterExtensions(new String[] {"*.xml"});
         dialog.setFilterNames(new String[] {"Arena Files (*.xml)"});
         dialog.setFilterPath("Arenas");

         // Disable the current terrain and wall settings, or the mouse up
         // event will fire to the terrain interface after the load completes,
         // and the map will be edited immediately:
         _combatServer._terrainInterface.disableCurrentEdits();

         String fileName = dialog.open();
         if ((fileName != null) && (fileName.length() > 0)) {
            File sourceFile = new File("Arenas" + File.separator + fileName);
            if (!sourceFile.exists()) {
               sourceFile = new File(fileName);
            }
            if (sourceFile.exists()) {
               if (sourceFile.canRead()) {
                  _currentMapFileName = sourceFile.getAbsolutePath();
                  CombatMap map = new CombatMap();
                  map.serializeFromFile(sourceFile);
                  _combatServer.setMap(map, true/*clearCombatants*/);
                  _combatServer._map.setZoomToFit();
               }
            }
         }
      }
      else if (e.widget == _newMapButton) {
         NewMapDialog dialog = new NewMapDialog(_combatServer.getShell());
         dialog.open();
         if (!dialog.isCanceled()) {
            CombatMap map = new CombatMap(dialog.getSizeX(), (short) (dialog.getSizeY()*2), null/*diag*/);
            map.setName(dialog.getName());
            _originalMap = map.clone();
            _combatServer.setMap(map, true/*clearCombatants*/);
            _currentMapFileName = null; // make sure we don't overwrite the current file
         }
      }
      refreshSaveButton();
   }

   public void refreshSaveButton() {
      if (_saveMapButton != null) {
         boolean enabled = (_originalMap == null) || (!_originalMap.equals(_combatServer.getArena().getCombatMap()));
         _saveMapButton.setEnabled(enabled);
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
      if (_currentMapFileName == null) {
         sourceFile = new File("Arenas" + File.separator + map.getName().toLowerCase() + ".xml");
      }
      else {
         sourceFile = new File(_currentMapFileName);
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
         _bgImageFilePath = createText(bgImgBlock, "", false/*editable*/, 4/*hSpan*/);
         _bgImageFileBtn = createButton(bgImgBlock, "choose", 2, null, this);
         createLabel(bgImgBlock, "Alpha", SWT.LEFT, 1/*hSpan*/, null);
         _bgImageAlphaSlider = new Slider(bgImgBlock, SWT.HORIZONTAL);
         GridData sliderGridData = new GridData(GridData.FILL_HORIZONTAL);
         sliderGridData.horizontalSpan = 6;
         _bgImageAlphaSlider.setLayoutData(sliderGridData);
         _bgImageAlphaSlider.setBounds(0, 30, 200, 20);
         _bgImageAlphaSlider.setMaximum(255);
         _bgImageAlphaSlider.setMinimum(0);
         _bgImageAlphaSlider.setIncrement(16);
         _bgImageAlphaSlider.addSelectionListener(this);

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

      _newMapButton = new Button(block, SWT.PUSH);
      _newMapButton.setText("New Map");
      _newMapButton.addSelectionListener(this);

      _saveMapButton = new Button(block, SWT.PUSH);
      _saveMapButton.setText("Save Map");
      _saveMapButton.addSelectionListener(this);

      _openMapButton = new Button(block, SWT.PUSH);
      _openMapButton.setText("Load Map");
      _openMapButton.addSelectionListener(this);
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

         _arenaSizeXValue = new Text(sizeBlock, SWT.LEFT | SWT.BORDER);
         _arenaSizeXValue.setText("20");
         _arenaSizeXValue.setSize(30, 20);
         GridData data = new GridData();
         data.minimumWidth = 30;
         data.grabExcessHorizontalSpace = true;
         _arenaSizeXValue.setLayoutData(data);
         new Label(sizeBlock, SWT.CENTER).setText(" Height:");

         _arenaSizeYValue = new Text(sizeBlock, SWT.LEFT | SWT.BORDER);
         _arenaSizeYValue.setText("14");
         _arenaSizeYValue.setSize(30, 20);
         data = new GridData();
         data.minimumWidth = 30;
         data.grabExcessHorizontalSpace = true;
         _arenaSizeYValue.setLayoutData(data);

         _arenaSizeXValue.addModifyListener(this);
         _arenaSizeYValue.addModifyListener(this);

         _setSizeButton = new Button(sizeBlock, SWT.PUSH);
         _setSizeButton.setText("Set Size");
         _setSizeButton.addSelectionListener(this);
         _setSizeButton.setEnabled(false);
      }

      _hideViewFromLocalPlayersButton = new Button(outerBlock, SWT.CHECK);
      _hideViewFromLocalPlayersButton.setText("Hide map from local players.");
      _hideViewFromLocalPlayersButton.setSelection(false);
      _hideViewFromLocalPlayersButton.addSelectionListener(this);

      _mapKnownToAllPlayersButton = new Button(outerBlock, SWT.CHECK);
      _mapKnownToAllPlayersButton.setText("All players know the map.");
      _mapKnownToAllPlayersButton.setSelection(false);
      _mapKnownToAllPlayersButton.addSelectionListener(this);

   }

   public void setMap(CombatMap map) {
      _arenaSizeXValue.setText(String.valueOf(map.getSizeX()));
      _arenaSizeYValue.setText(String.valueOf(map.getSizeY()/2));
      _hideViewFromLocalPlayersButton.setSelection(map.isHideViewFromLocalPlayers());
      _mapKnownToAllPlayersButton.setSelection(map.isKnownByAllPlayers());
      _combatServer._map.setBackgroundAlpha(_bgImageAlphaSlider.getSelection());

      String backgroundImagePath = "";
      if (_combatServer._map.getCombatMap() != null) {
         backgroundImagePath = _combatServer._map.getCombatMap().getBackgroundImagePath();
         _bgImageAlphaSlider.setSelection(_combatServer._map.getCombatMap().getBackgroundImageAlpha());
         if (backgroundImagePath == null) {
            backgroundImagePath = "";
         }
      }
      _bgImageFilePath.setText(backgroundImagePath);
   }

   public short getMapSizeX() {
      return Short.parseShort(_arenaSizeXValue.getText().trim());
   }
   public short getMapSizeY() {
      return (short)(Short.parseShort(_arenaSizeYValue.getText().trim()) * 2);
   }
}
