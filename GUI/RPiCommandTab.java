import javax.swing.*;
import java.awt.*;


public class RPiCommandTab extends JPanel {
    private final Utility util;
    private JTextArea  CONSOLE;      // running log / output
    public RPiCommandTab(Utility util) {
        this.util = util;
//        setConfigs(Console);
//        Utility util = new Utility(Console);

        setSize(800, 560);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[][] presets = {
                {"status", "Check the current process status"},
                {"start",  "Start the main data collection process"},
                {"rsync",  "Synchronize files from Pi to host"},
                {"kill",   "Kill the running process on the Pi"}
//                {"ui",     "Shows and allows user to run available commands built in to the sensor"},
//                {"help",   "List available commands"}
        };

        // Setup for row of common commands
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String[] p : presets) {
            JButton b = new JButton(p[0]);
            b.setToolTipText(p[1]);  // Set tooltip
            b.addActionListener(e -> util.sendCommand(p[0]));
            btnRow.add(b);
        }

        panel.add(btnRow, BorderLayout.NORTH);

        JButton sendBtn = new JButton("Send");
        JTextField cmdField = new JTextField();     // raw command entry

        sendBtn.addActionListener(e -> util.sendCommand(cmdField.getText()));
        cmdField.addActionListener(e -> sendBtn.doClick());

        JPanel commandRightPanel = new JPanel(new BorderLayout());
        JTextArea commandOutputArea = new JTextArea();

        commandRightPanel.setPreferredSize(new Dimension(300, 0));
        commandRightPanel.setBorder(BorderFactory.createTitledBorder("Backend Output"));
        commandOutputArea.setEditable(false);
        commandOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandRightPanel.add(new JScrollPane(commandOutputArea), BorderLayout.CENTER);

        // Add elements to GUI Panel
        JPanel south = new JPanel(new BorderLayout(3,3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn,  BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        add(util.wrapWithRightPanel(panel, commandRightPanel));
    }
    private void setConfigs(JTextArea console){
        CONSOLE = console;

    }

}
