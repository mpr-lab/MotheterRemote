import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        JPanel sshPanel = buildSSHPanel();

        cardPanel.add(disclaimerPanel, "0");
        cardPanel.add(rpiConfigPanel, "1");
        cardPanel.add(connectionPanel, "2");
        cardPanel.add(sshPanel, "3");

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

        JCheckBox acceptBox = new JCheckBox("I have read and accept the above.");
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
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        JTextField nameField = new JTextField(defaultName != null ? defaultName : "");
        JTextField addrField = new JTextField(defaultAddr != null ? defaultAddr : "");
        row.add(new JLabel("Name:"));
        row.add(nameField);
        row.add(new JLabel("Address:"));
        row.add(addrField);

        profilesPanel.add(row);
        profiles.add(new RPiProfile(nameField, addrField));

        profilesPanel.revalidate();
        profilesPanel.repaint();
    }

    private JCheckBox radioBox = new JCheckBox("Using Radios");
    private JCheckBox tailscaleBox = new JCheckBox("Using Tailscale");
    private JRadioButton sshBtn = new JRadioButton("SSH", true);
    private JRadioButton socketBtn = new JRadioButton("Socket");

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Choose Connection Type:"));
        ButtonGroup group = new ButtonGroup();
        group.add(sshBtn);
        group.add(socketBtn);
        panel.add(sshBtn);
        panel.add(socketBtn);

        panel.add(radioBox);
        panel.add(tailscaleBox);

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

    private void updateNav() {
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        backButton.setEnabled(currentCard > 0);
        nextButton.setEnabled(currentCard < 3);
    }

    private void setupSSHKeysForAll() {
        String home = System.getProperty("user.home");
        Path privateKey = Paths.get(home, ".ssh", "id_ed25519");
        try {
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

                    JSONObject json = new JSONObject();
                    json.put("rpi_name", rpiName);
                    json.put("rpi_addr", rpiAddr);
                    Files.writeString(Paths.get(rpiName + "_profile.json"), json.toString(4));
                }
            }

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
        SwingUtilities.invokeLater(SetupWizard::new);
    }
}