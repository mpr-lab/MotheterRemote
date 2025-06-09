import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SetupWizard extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);
    private JButton nextButton = new JButton("Next");
    private JButton backButton = new JButton("Back");
    private int currentCard = 0;

    private java.util.List<RPiProfile> profiles = new ArrayList<>();
    private JPanel profilesPanel;

    public SetupWizard() {
        super("Initial Setup Wizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        JPanel disclaimerPanel = buildDisclaimerPanel();
        JPanel rpiConfigPanel = buildRpiConfigPanel();
        JPanel connectionPanel = buildConnectionPanel();
        JPanel tailscalePanel = buildTailscale();
        JPanel sshPanel = buildSSHPanel();

        // TODO: if using radio then include sensor commands in GUI, if not, don't include

        cardPanel.add(disclaimerPanel, "0");
        cardPanel.add(rpiConfigPanel, "1");
        cardPanel.add(connectionPanel, "2");
        cardPanel.add(tailscalePanel, "3");
        cardPanel.add(sshPanel, "4");

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        navPanel.add(backButton);
        navPanel.add(nextButton);

        add(cardPanel, BorderLayout.CENTER);
        add(navPanel, BorderLayout.SOUTH);

        backButton.setEnabled(false);

        nextButton.addActionListener(e -> {
            if (currentCard == 0 && !disclaimerAccepted) {
                JOptionPane.showMessageDialog(this, "Please read and accept the disclaimer to continue.");
                return;
            }
            currentCard++;
            updateNav();
        });

        backButton.addActionListener(e -> {
            currentCard--;
            updateNav();
        });

        setVisible(true);
    }

    private boolean disclaimerAccepted = false;

    private JPanel buildDisclaimerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea disclaimer = new JTextArea();
        disclaimer.setText("IMPORTANT: Please read this disclaimer fully before continuing...\n(Insert content here)");
        disclaimer.setWrapStyleWord(true);
        disclaimer.setLineWrap(true);
        disclaimer.setEditable(false);
        JScrollPane scroll = new JScrollPane(disclaimer);

        JCheckBox acceptBox = new JCheckBox("I have read the above.");
        acceptBox.addItemListener(e -> disclaimerAccepted = acceptBox.isSelected());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(acceptBox, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRpiConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        profilesPanel = new JPanel();
        profilesPanel.setLayout(new BoxLayout(profilesPanel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton addProfile = new JButton("Add RPi Profile");
        addProfile.addActionListener(e -> addRpiProfile(null, null));

        panel.add(new JScrollPane(profilesPanel), BorderLayout.CENTER);
        panel.add(addProfile, BorderLayout.SOUTH);

        addRpiProfile("pi", "192.168.1.10");
        return panel;
    }

    private void addRpiProfile(String defaultName, String defaultAddr) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.setMaximumSize(new Dimension(500, 40));

        JTextField nameField = new JTextField(defaultName != null ? defaultName : "");
        JTextField addrField = new JTextField(defaultAddr != null ? defaultAddr : "");

        container.add(new JLabel("Name:"));
        container.add(Box.createRigidArea(new Dimension(5, 0)));
        container.add(nameField);
        container.add(Box.createRigidArea(new Dimension(10, 0)));
        container.add(new JLabel("Address:"));
        container.add(Box.createRigidArea(new Dimension(5, 0)));
        container.add(addrField);

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> {
            profilesPanel.remove(container);
            profiles.removeIf(p -> p.nameField == nameField && p.addrField == addrField);
            profilesPanel.revalidate();
            profilesPanel.repaint();
        });
        container.add(Box.createRigidArea(new Dimension(10, 0)));
        container.add(deleteBtn);

        profilesPanel.add(container);
        profiles.add(new RPiProfile(nameField, addrField));

        profilesPanel.revalidate();
        profilesPanel.repaint();
    }

    private JCheckBox radioBox = new JCheckBox("Using Radios");
    private JCheckBox tailscaleBox = new JCheckBox("Using Tailscale");
    private JRadioButton wifiBtn = new JRadioButton("Wifi", true);
    private JRadioButton ethernetBtn = new JRadioButton("Ethernet");
    private JRadioButton cellularBtn = new JRadioButton("Cellular");

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Choose Connection Type:"));
        ButtonGroup group = new ButtonGroup();
        group.add(wifiBtn);
        group.add(ethernetBtn);
        group.add(cellularBtn);
        panel.add(wifiBtn);
        panel.add(ethernetBtn);
        panel.add(cellularBtn);

        panel.add(radioBox);
        panel.add(tailscaleBox);

        return panel;
    }

    private JPanel buildTailscale(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel instructions = new JLabel("Instructions on Using Tailscale:");

        JTextArea info = new JTextArea("This step will configure SSH key-based access to all Raspberry Pi profiles using ED25519.");
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setEditable(false);

        panel.add(instructions, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSSHPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea info = new JTextArea("This step will configure SSH key-based access to all Raspberry Pi profiles using ED25519.");
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setEditable(false);

        JButton setupBtn = new JButton("Setup SSH for All Profiles");
        setupBtn.addActionListener(e -> setupSSHKeysForAll());

        panel.add(new JScrollPane(info), BorderLayout.CENTER);
        panel.add(setupBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFinalPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton finishBtn = new JButton("Finish Setup");

        panel.add(finishBtn);
        return panel;
    }

    private void updateNav() {
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        backButton.setEnabled(currentCard > 0);
        nextButton.setEnabled(currentCard < 4);
    }

    private void setupSSHKeysForAll() {
        String home = System.getProperty("user.home");
        Path privateKey = Paths.get(home, ".ssh", "id_ed25519");
        Path profilesDir = Paths.get("profiles");
        try {
            Files.createDirectories(profilesDir);

            if (!Files.exists(privateKey)) {
                ProcessBuilder genKey = new ProcessBuilder(
                        "ssh-keygen", "-t", "ed25519", "-f", privateKey.toString(), "-N", ""
                );
                genKey.inheritIO().start().waitFor();
            }
            String pubKey = Files.readString(Paths.get(privateKey + ".pub"));

            for (RPiProfile profile : profiles) {
                String rpiName = profile.nameField.getText().trim();
                String rpiAddr = profile.addrField.getText().trim();
                String password = JOptionPane.showInputDialog(this, "Enter password for '" + rpiName + "' at " + rpiAddr);

                if (password != null && !password.isEmpty()) {
                    String command = "echo '" + pubKey + "' | sshpass -p '" + password + "' ssh -o StrictHostKeyChecking=no " + rpiName + "@" + rpiAddr + " 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'";
                    ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                    pb.inheritIO();
                    pb.start().waitFor();

                    Properties props = new Properties();
                    props.setProperty("rpi_name", rpiName);
                    props.setProperty("rpi_addr", rpiAddr);
                    try (FileWriter writer = new FileWriter(profilesDir.resolve(rpiName + "_profile.properties").toFile())) {
                        props.store(writer, "RPi Profile");
                    }
                }
            }

            // NEED A NEW MESSAGE THAT SHOWS WHETHER OR NOT SSH SETUP ACTUALLY WORKED
            JOptionPane.showMessageDialog(this, "SSH setup complete for all profiles.");

        } catch (IOException | InterruptedException ex) {
            JOptionPane.showMessageDialog(this, "SSH setup failed: " + ex.getMessage());
        }
    }

    static class RPiProfile {
        JTextField nameField;
        JTextField addrField;

        RPiProfile(JTextField nameField, JTextField addrField) {
            this.nameField = nameField;
            this.addrField = addrField;
        }
    }

    public static void main(String[] args) {
//        Path profilesDir = Paths.get("profiles");
//        if (Files.exists(profilesDir)) {
//            System.out.println("Profiles directory already exists. Skipping setup wizard.");
//            return;
//        }
        SwingUtilities.invokeLater(SetupWizard::new);
    }
}