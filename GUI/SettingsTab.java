import javax.swing.*;
import java.awt.*;

public class SettingsTab implements GUITab {
    private JPanel mainPanel;

    public SettingsTab() {
        mainPanel = buildMain();
    }

    private JPanel buildMain() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Settings Tab Placeholder"), BorderLayout.CENTER);
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
