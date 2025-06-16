// Refactored BuildGUI.java with Auto-Refreshing Profile List via Static Reference

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BuildGUI extends JFrame {
    private static final JTextArea console = new JTextArea();
    private static JComboBox<String> profileDropdown = new JComboBox<>();
    private final JButton confirmProfileButton = new JButton("Confirm");

    private static boolean setup = false;


//    private static Path PROFILE_DIR = util.getProfileSaveDirFromConfig();

    public BuildGUI() {
        super("MotheterRemote");
        Utility util = new Utility(console);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 650);
        setLayout(new BorderLayout());

        JPanel topPanel = buildProfileSelector(util);
        add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Command Center", new RPiCommandTab(util));
//        tabs.addTab("Sensor Command Center", new SensorCommandTab(util));
        tabs.addTab("Data Sync", new DataTab(util));
        tabs.addTab("Settings", new SettingsTab(util));
        tabs.addTab("?", new HelpTab());
        add(tabs, BorderLayout.CENTER);

        add(buildConsolePanel(), BorderLayout.SOUTH);

        util.startPythonBackend();
        SwingUtilities.invokeLater(() -> util.sendCommand("reload-config"));
    }

    private JPanel buildProfileSelector(Utility util) {
        Path PROFILE_DIR = util.getProfileSaveDirFromConfig();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("Select RPi Profile:");
        JButton refreshButton = new JButton("⟳");
        refreshButton.setToolTipText("Refresh Profile List");
        refreshButton.addActionListener(e -> refreshProfileList());

        loadProfileList();

        confirmProfileButton.addActionListener(e -> {
            String selected = (String) profileDropdown.getSelectedItem();
            if (selected != null) {
                File profileFile = new File(PROFILE_DIR.toString(), selected + "_profile.properties");
                Properties props = new Properties();
                try (FileReader reader = new FileReader(profileFile)) {
                    props.load(reader);
                    String rpiName = props.getProperty("rpi_name");
                    String rpiAddr = props.getProperty("rpi_addr");
                    util.updateConfigsPy(rpiName, rpiAddr);
                    JOptionPane.showMessageDialog(this, "Updated configs_ssh.py for profile: " + selected);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to read selected profile: " + ex.getMessage());
                }
            }
        });

        panel.add(label);
        panel.add(profileDropdown);
        panel.add(confirmProfileButton);
        panel.add(refreshButton);

        return panel;
    }

    public static void refreshProfileList() {
        Utility util = new Utility();
        profileDropdown.removeAllItems();
        File profileDir = new File(util.getProfileSaveDirFromConfig().toString());
        String[] profileNames = profileDir.list((dir, name) -> name.endsWith("_profile.properties"));
        if (profileNames != null) {
            for (String name : profileNames) {
                profileDropdown.addItem(name.replace("_profile.properties", ""));
            }
        }
    }

    private void loadProfileList() {
        refreshProfileList();
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

        scroll.setVisible(false);

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

    public static void main(String[] args) {
        Path setupDir = Paths.get("host_config.properties");
        if (Files.notExists(setupDir)) {
            SwingUtilities.invokeLater(SetupWizard::new);
        } else {
            SwingUtilities.invokeLater(() -> {
                BuildGUI gui = new BuildGUI();
                gui.setVisible(true);
            });
        }
    }
//    public static void main(String[] args) {
//        if (!setup) {
//            SwingUtilities.invokeLater(SetupWizard::new);
//            setup = true;
//        } else {
//            SwingUtilities.invokeLater(() -> {
//                BuildGUI gui = new BuildGUI();
//                gui.setVisible(true);
//            });
//        }
//    }
}
