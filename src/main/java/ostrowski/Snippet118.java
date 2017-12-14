package ostrowski;

/*
* Cursor example snippet: create a color cursor from an image file
*
* For a list of all SWT example snippets see
* http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/platform-swt-home/dev.html#snippets
*/
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class Snippet118 implements Listener {

       public static void main(String[] args) {
             Snippet118 snip = new Snippet118();
             snip.run();
       }

       Cursor        _currentCursor;
       Cursor        _paintCursor;
       Cursor        _arrowCursor;
       final Display _display;
       final Shell   _shell;

       private Snippet118() {
             this._display = new Display();
             this._shell = new Shell(_display);
             this._shell.setSize(150, 150);
             Button button = new Button(_shell, SWT.PUSH);
             button.setText("Change cursor");
             Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
             button.setSize(size);
             button.addListener(SWT.Selection, this);
             button.addListener(SWT.MouseEnter, this);
             button.addListener(SWT.MouseExit, this);
             _arrowCursor = new Cursor(_display, SWT.CURSOR_ARROW);
       }
       public void run() {
             _shell.open();
             while (!_shell.isDisposed()) {
               if (!_display.readAndDispatch()) {
                  _display.sleep();
               }
             }
             if (_paintCursor != null) {
               _paintCursor.dispose();
            }
             _display.dispose();
  }

  @Override
  public void handleEvent(Event e) {
         if (e.type == SWT.Selection) {
               FileDialog dialog = new FileDialog(_shell);
               dialog.setFilterExtensions(new String[] { "*.png", "*.*" });
               String name = dialog.open();
               if (name == null) {
                  return;
               }

               ImageData image = new ImageData(name);
               Cursor oldPaintCursor = _paintCursor;
               _paintCursor = new Cursor(_display, image, 0, 0);
               if (_currentCursor == oldPaintCursor) {
                      _shell.setCursor(_paintCursor);
                      _currentCursor = _paintCursor;
               }
               if (oldPaintCursor != null) {
                  oldPaintCursor.dispose();
               }
         }
         if (e.type == SWT.MouseExit) {
               if (_currentCursor != null) {
                      _currentCursor = _arrowCursor;
                      _shell.setCursor(_currentCursor);
               }
         }
         if (e.type == SWT.MouseEnter) {
               _currentCursor = _paintCursor;
               _shell.setCursor(_currentCursor);
         }
  }
}