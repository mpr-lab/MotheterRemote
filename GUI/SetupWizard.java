import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

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
        JPanel connectionPanel = buildConnectionPanel();
        JPanel tailscalePanel = buildTailscale();
        JPanel rpiConfigPanel = buildRpiConfigPanel();
        JPanel sshPanel = buildSSHPanel();
        JPanel finalPanel = buildFinalPanel();

        // TODO: if using radio then include sensor commands in GUI, if not, don't include

        cardPanel.add(disclaimerPanel, "0");
        cardPanel.add(connectionPanel, "1");
        cardPanel.add(tailscalePanel, "2");
        cardPanel.add(rpiConfigPanel, "3");
        cardPanel.add(sshPanel, "4");
        cardPanel.add(finalPanel, "5");

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

        addRpiProfile("", "");


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
    private JPanel buildCopyRow(String command){
        Utility util = new Utility();

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JTextField cmdField = new JTextField(command);
        cmdField.setEditable(false);
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(command));
        row.add(cmdField);
        row.add(Box.createRigidArea(new Dimension(10, 0)));
        row.add(copyBtn);
        row.setMaximumSize(new Dimension(500, 30));
        util.setFullWidth.accept(row);

        return row;
    }

    private JPanel buildSSHPanel() {
        Utility util = new Utility();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JTextArea instructions = util.buildTextArea(panel, 50);
        instructions.setText("The next step of setup involves setting up a secure shell access to the raspberry pis. For this step, you will need to open the terminal on your computer and run some commands. Follow each step carefully:");

        inner.add(instructions);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // STEP 1: CHECK IS SSH INSTALLED< IF IT's NOT, INSTALL IT
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.add(new JLabel("Step 1: Check is SSH is installed"));
        step1.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI1 = util.buildTextArea(step1, 30);
        copyI1.setText("Open up a new terminal. First, check whether or not you have ssh installed. To do this, run the following command:");

        String sshCmd = "ssh";
        JPanel checkSSHRow = buildCopyRow(sshCmd);

        step1.add(copyI1);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));
        step1.add(checkSSHRow);
        step1.add(Box.createRigidArea(new Dimension(0, 20)));

        // STEP 2 GENERATE SSH KEY
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.add(new JLabel("Step 2: Generate a SSH Key"));
        step2.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI2 = util.buildTextArea(step2, 75);
        copyI2.setText("Now that you have SSH, you must establish yourself as a known host for the RPi. This will allow you to connect remotely to the RPi without having to enter a password. We must generate a new SSH key which will be copied to the rpi. Copy the following command into your terminal:");

        String generateCmd = "ssh-keygen -t ed25519";
        JPanel genSSHRow = buildCopyRow(generateCmd);

        step2.add(copyI2);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(genSSHRow);
        step2.add(Box.createRigidArea(new Dimension(0, 20)));

        // STEP 3: COPY SSH KEY
        JPanel step3 = new JPanel();
        step3.setLayout(new BoxLayout(step3, BoxLayout.Y_AXIS));
        step3.add(new JLabel("Step 3: Copy SSH Key to Each Raspberry Pi"));
        step3.add(Box.createRigidArea(new Dimension(0, 10)));


        JTextArea copyI3 = util.buildTextArea(step3, 30);
        copyI3.setText("Next, we must copy the key that you just generated over to the RPi. In the terminal, run the following command:");

        String copyCmd = "ssh-copy-id <rpi_name>@<rpi_addr>";
        JPanel copySSHRow = buildCopyRow(copyCmd);

        JTextArea changeI3 = util.buildTextArea(step3, 30);
        changeI3.setText("Make sure to change <rpi_name> and <rpi_addr> with the correct information. You may need to input the RPi's password on this step.");

        step3.add(copyI3);
        step3.add(Box.createRigidArea(new Dimension(0, 10)));
        step3.add(copySSHRow);
        step3.add(Box.createRigidArea(new Dimension(0, 10)));
        step3.add(changeI3);
        step3.add(Box.createRigidArea(new Dimension(0, 20)));

        // STEP 4: VERIFY SSH CONNECTION
        JPanel step4 = new JPanel();
        step4.setLayout(new BoxLayout(step4, BoxLayout.Y_AXIS));
        step4.add(new JLabel("Step 3: Copy SSH Key to Each Raspberry Pi"));
        step4.add(Box.createRigidArea(new Dimension(0, 10)));


        JTextArea copyI4 = util.buildTextArea(step4, 30);
        copyI4.setText("Finally, check to make sure that the SSH connection is working properly. In the terminal, run the following command:");

        String verifyCmd = "ssh <rpi_name>@<rpi_addr>";
        JPanel verifySSHRow = buildCopyRow(verifyCmd);

        JTextArea changeI4 = util.buildTextArea(step4, 45);
        changeI4.setText("Make sure to change <rpi_name> and <rpi_addr> with the correct information. If you setup the ssh connection correctly, you should be able to access the RPi without having to input a password. you should now see");

        step4.add(copyI4);
        step4.add(Box.createRigidArea(new Dimension(0, 10)));
        step4.add(verifySSHRow);
        step4.add(Box.createRigidArea(new Dimension(0, 10)));
        step4.add(changeI4);
        step4.add(Box.createRigidArea(new Dimension(0, 20)));


        inner.add(step1);
        inner.add(step2);
        inner.add(step3);
        inner.add(step4);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    //    private JPanel buildSSHPanel() {
//        JPanel panel = new JPanel(new BorderLayout(10, 10));
//        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        JTextArea info = new JTextArea("This step will configure SSH key-based access to all Raspberry Pi profiles using ED25519.");
//        info.setLineWrap(true);
//        info.setWrapStyleWord(true);
//        info.setEditable(false);
//
//        JButton setupBtn = new JButton("Setup SSH for All Profiles");
//        setupBtn.addActionListener(e -> setupSSHKeysForAll());
//
//        panel.add(new JScrollPane(info), BorderLayout.CENTER);
//        panel.add(setupBtn, BorderLayout.SOUTH);
//        return panel;
//    }
    private JPanel buildFinalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Setup Complete"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(new JLabel("Click Finish to save your Raspberry Pi profiles."));

        JButton finishBtn = new JButton("Finish Setup");
        finishBtn.addActionListener(e -> {
            Path profilesDir = Paths.get("profiles");
            try {
                Files.createDirectories(profilesDir);

                int saved = 0;
                for (RPiProfile profile : profiles) {
                    String name = profile.nameField.getText().trim();
                    String addr = profile.addrField.getText().trim();

                    if (!name.isEmpty() && !addr.isEmpty()) {
                        Properties props = new Properties();
                        props.setProperty("rpi_name", name);
                        props.setProperty("rpi_addr", addr);

                        File file = profilesDir.resolve(name + "_profile.properties").toFile();
                        try (FileWriter writer = new FileWriter(file)) {
                            props.store(writer, "RPi Profile");
                            saved++;
                        }
                    }
                }

                JOptionPane.showMessageDialog(this,
                        "Saved " + saved + " profile(s) to 'profiles/' folder.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);

                dispose();  // Close the wizard

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save profiles: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        finishBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(finishBtn);

        return panel;
    }


    private void updateNav() {
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        backButton.setEnabled(currentCard > 0);
        nextButton.setEnabled(currentCard < 5);
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null
        );
    }



    // AUTO SSH SETUP --> SECURITY ISSUE?
//    private void setupSSHKeysForAll() {
//        String home = System.getProperty("user.home");
//        Path privateKey = Paths.get(home, ".ssh", "id_ed25519");
//        Path publicKey = privateKey.resolveSibling("id_ed25519.pub");
//        Path profilesDir = Paths.get("profiles");
//
//        try {
//            // Check for sshpass
//            ProcessBuilder checkSshpass = new ProcessBuilder("which", "sshpass");
//            if (checkSshpass.start().waitFor() != 0) {
//                JOptionPane.showMessageDialog(this, "'sshpass' is not installed. Please install it before continuing.");
//                return;
//            }
//
//            // Ensure SSH key exists
//            if (!Files.exists(privateKey)) {
//                ProcessBuilder genKey = new ProcessBuilder(
//                        "ssh-keygen", "-t", "ed25519", "-f", privateKey.toString(), "-N", ""
//                );
//                genKey.inheritIO().start().waitFor();
//            }
//
//            // Load public key
//            String pubKey = Files.readString(publicKey);
//            Files.createDirectories(profilesDir);
//
//            StringBuilder resultSummary = new StringBuilder();
//
//            for (RPiProfile profile : profiles) {
//                String rpiName = profile.nameField.getText().trim();
//                String rpiAddr = profile.addrField.getText().trim();
//
//                if (rpiName.isEmpty() || rpiAddr.isEmpty()) {
//                    resultSummary.append("Skipped profile with empty name or address.\n");
//                    continue;
//                }
//
//                String password = JOptionPane.showInputDialog(this, "Enter password for '" + rpiName + "' at " + rpiAddr);
//                if (password == null || password.isEmpty()) {
//                    resultSummary.append("Skipped ").append(rpiName).append(" — no password provided.\n");
//                    continue;
//                }
//
//                // Send public key to Pi
//                String sendKeyCmd = "echo '" + pubKey + "' | sshpass -p '" + password + "' ssh -o StrictHostKeyChecking=no " +
//                        rpiName + "@" + rpiAddr +
//                        " 'mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys'";
//
//                ProcessBuilder sendKeyPB = new ProcessBuilder("bash", "-c", sendKeyCmd);
//                int sendExit = sendKeyPB.inheritIO().start().waitFor();
//                if (sendExit != 0) {
//                    resultSummary.append("Failed to push key to ").append(rpiName).append(" at ").append(rpiAddr).append("\n");
//                    continue;
//                }
//
//                // Verify key works
//                String verifyCmd = "ssh -o BatchMode=yes -o ConnectTimeout=5 " + rpiName + "@" + rpiAddr + " 'echo success'";
//                ProcessBuilder verifyPB = new ProcessBuilder("bash", "-c", verifyCmd);
//                int verifyExit = verifyPB.start().waitFor();
//
//                if (verifyExit == 0) {
//                    // Save profile
//                    Properties props = new Properties();
//                    props.setProperty("rpi_name", rpiName);
//                    props.setProperty("rpi_addr", rpiAddr);
//                    try (FileWriter writer = new FileWriter(profilesDir.resolve(rpiName + "_profile.properties").toFile())) {
//                        props.store(writer, "RPi Profile");
//                    }
//                    resultSummary.append("SSH setup verified for ").append(rpiName).append(" at ").append(rpiAddr).append("\n");
//                } else {
//                    resultSummary.append("SSH verification failed for ").append(rpiName).append(" at ").append(rpiAddr).append("\n");
//                }
//            }
//
//            JOptionPane.showMessageDialog(this, resultSummary.toString(), "SSH Setup Results", JOptionPane.INFORMATION_MESSAGE);
//
//        } catch (IOException | InterruptedException ex) {
//            JOptionPane.showMessageDialog(this, "SSH setup failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//            ex.printStackTrace();
//        }
//    }


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