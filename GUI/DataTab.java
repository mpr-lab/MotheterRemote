import javax.swing.*;
import java.awt.*;

public class DataTab implements GUITab {
    private JPanel mainPanel;

    public DataTab() {
        mainPanel = buildMain();
    }

    private JPanel buildMain() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Data Sync Tab Placeholder"), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public JPanel getSidePanel() {
        return null;
    }
}
