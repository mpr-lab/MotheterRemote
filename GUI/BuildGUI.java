import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class BuildGUI extends JFrame {
    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    private static final String CONFIG_PATH  = "../ssh/configs_ssh.py";
    private static final String BACKEND_PATH = "../ssh/host_ssh.py";
    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");

    /* ---------------- Network ---------------- */
    private String HOST;
    private String NAME;
    private static final int PORT = 12345;

    private final JTextArea console = new JTextArea();

    BuildGUI(String hostAddr, String hostName) {
        super("MotheterRemote");
        this.HOST = hostAddr;
        this.NAME = hostName;

        Utility util = new Utility(console);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 650);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("RPi Command Center", new RPiCommandTab(console));
        tabs.addTab("Sensor Command Center", new SensorCommandTab(console));
        tabs.addTab("Data Sync", new DataTab(console));
        tabs.addTab("Settings", new SettingsTab(console));
        tabs.addTab("?", new HelpTab());
        add(tabs, BorderLayout.CENTER);
        add(buildConsolePanel(), BorderLayout.SOUTH);

        util.startPythonBackend();
        SwingUtilities.invokeLater(() -> util.sendCommand("reload-config"));
    }

    private JPanel buildConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setPreferredSize(new Dimension(0, 150));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton clear = new JButton("Clear log");
        clear.addActionListener(e -> console.setText(""));
        JButton toggleButton = new JButton("Minimize");
        toggleButton.addActionListener(e -> toggleConsoleVisibility(scroll, toggleButton));

        btnRow.add(toggleButton);
        btnRow.add(clear);

        JPanel p = new JPanel(new BorderLayout());
        p.add(btnRow, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private void toggleConsoleVisibility(JScrollPane scrollPane, JButton toggleButton) {
        if (scrollPane.isVisible()) {
            scrollPane.setVisible(false);
            toggleButton.setText("Show Console");
        } else {
            scrollPane.setVisible(true);
            toggleButton.setText("Hide Console");
        }
    }

    private static String[] getRPIDetailsFromConfigs() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("../ssh/configs_ssh.py"));
            String name = null, addr = null;
            for (String line : lines) {
                if (line.startsWith("rpi_name")) {
                    name = line.split("=")[1].trim().replaceAll("['\"]", "");
                } else if (line.startsWith("rpi_addr")) {
                    addr = line.split("=")[1].trim().replaceAll("['\"]", "");
                }
            }
            return (name != null && addr != null) ? new String[]{addr, name} : null;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading configs.py: " + e.getMessage());
            return null;
        }
    }

    private static void promptAndRunGUI() {
        File profileDir = new File("profiles");
        String[] profileNames = profileDir.list((dir, name) -> name.endsWith("_profile.properties"));
        if (profileNames == null || profileNames.length == 0) {
            JOptionPane.showMessageDialog(null, "No RPi profiles found.");
            return;
        }

        for (int i = 0; i < profileNames.length; i++) {
            profileNames[i] = profileNames[i].replace("_profile.properties", "");
        }

        String selected = (String) JOptionPane.showInputDialog(null, "Select RPi Profile", "RPi Profile Selector",
                JOptionPane.PLAIN_MESSAGE, null, profileNames, profileNames[0]);

        if (selected != null) {
            File profileFile = new File("profiles", selected + "_profile.properties");
            Properties props = new Properties();
            try (FileReader reader = new FileReader(profileFile)) {
                props.load(reader);
                String rpiName = props.getProperty("rpi_name");
                String rpiAddr = props.getProperty("rpi_addr");

                // update configs_ssh.py
                Utility util = new Utility();
                util.updateConfigsPy("host", "host", rpiName, rpiAddr);

                SwingUtilities.invokeLater(() -> {
                    BuildGUI gui = new BuildGUI(rpiAddr, rpiName);
                    gui.setVisible(true);
                });
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Failed to read selected profile: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Path profilesDir = Paths.get("profiles");
        if (Files.notExists(profilesDir)) {
            SwingUtilities.invokeLater(SetupWizard::new);
        } else {
            promptAndRunGUI();
        }
    }
}
