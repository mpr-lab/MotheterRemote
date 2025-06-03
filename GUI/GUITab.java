import javax.swing.*;
import java.awt.*;
import java.io.*;

public interface GUITab {
    JPanel getMainPanel(); // the main content of the tab
    JPanel getSidePanel(); // optional side panel (null if none)
}
