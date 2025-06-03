import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class RPiCommandTab implements GUITab {
    private final JPanel mainPanel;
    private final JPanel sidePanel = new JPanel(); // No specific side panel needed
    private final JTextField cmdField = new JTextField();
    private final Consumer<String> sendCommand;

    public RPiCommandTab(Consumer<String> sendCommand) {
        this.sendCommand = sendCommand;
        this.mainPanel = buildMain();
    }

    private JPanel buildMain() {
        JPanel root = new JPanel(new BorderLayout(5,5));

        String[][] presets = {
                {"status", "Check the current process status"},
                {"start",  "Start the main data collection process"},
                {"rsync",  "Synchronize files from Pi to host"},
                {"kill",   "Kill the running process on the Pi"}
        };

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String[] p : presets) {
            JButton b = new JButton(p[0]);
            b.setToolTipText(p[1]);
            b.addActionListener(e -> sendCommand.accept(p[0]));
            btnRow.add(b);
        }

        root.add(btnRow, BorderLayout.NORTH);

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendCommand.accept(cmdField.getText()));
        cmdField.addActionListener(e -> sendBtn.doClick());

        JPanel south = new JPanel(new BorderLayout(3,3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn,  BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public JPanel getSidePanel() {
        return null; // No side panel needed for this tab
    }
}
