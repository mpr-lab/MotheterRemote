// Refactored SetupWizard.java with Sectional Flow, Progress Saving, and Progress Bar

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
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JButton nextButton = new JButton("Next");
    private final JButton backButton = new JButton("Back");
    private final JProgressBar progressBar = new JProgressBar(0, 5);
    private final Map<String, Integer> sectionStart = new LinkedHashMap<>();
    private int currentCard = 0;

    private final java.util.List<RPiProfile> profiles = new ArrayList<>();
    private JPanel profilesPanel;

    private boolean disclaimerAccepted = false;
    private final Path progressFile = Paths.get(".setup_progress");

    public SetupWizard() {
        super("Initial Setup Wizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // Sections
        sectionStart.put("Host Setup", 0);
        sectionStart.put("RPi Setup", 2);
        sectionStart.put("SSH Setup", 4);

        // Panels per step
        cardPanel.add(buildDisclaimerPanel(), "0");       // Host Setup
        cardPanel.add(buildConnectionPanel(), "1");
        cardPanel.add(buildTailscale(), "2");             // RPi Setup
        cardPanel.add(buildRpiConfigPanel(), "3");
        cardPanel.add(buildSSHPanel(), "4");              // SSH Setup
        cardPanel.add(buildFinalPanel(), "5");

        JPanel navPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(backButton);
        buttonPanel.add(nextButton);

        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        navPanel.add(progressBar, BorderLayout.CENTER);
        navPanel.add(buttonPanel, BorderLayout.EAST);

        add(cardPanel, BorderLayout.CENTER);
        add(navPanel, BorderLayout.SOUTH);

        backButton.setEnabled(false);
        nextButton.addActionListener(e -> nextCard());
        backButton.addActionListener(e -> prevCard());

        loadProgress();
        setVisible(true);
    }

    private void nextCard() {
        if (currentCard == 0 && !disclaimerAccepted) {
            JOptionPane.showMessageDialog(this, "Please accept the disclaimer to continue.");
            return;
        }
        if (currentCard < 5) currentCard++;
        updateNav();
    }

    private void prevCard() {
        if (currentCard > 0) currentCard--;
        updateNav();
    }

    private void updateNav() {
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        backButton.setEnabled(currentCard > 0);
        nextButton.setEnabled(currentCard < 5);
        progressBar.setValue(currentCard);
        progressBar.setString("Step " + (currentCard + 1) + " of 6");
        saveProgress();
    }

    private void saveProgress() {
        try {
            Files.writeString(progressFile, String.valueOf(currentCard));
        } catch (IOException e) {
            System.err.println("[Setup] Failed to save progress: " + e.getMessage());
        }
    }

    private void loadProgress() {
        if (Files.exists(progressFile)) {
            try {
                currentCard = Integer.parseInt(Files.readString(progressFile).trim());
                updateNav();
            } catch (IOException | NumberFormatException ignored) {}
        }
    }

    private JPanel buildDisclaimerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea disclaimer = new JTextArea("IMPORTANT: Please read this disclaimer fully before continuing...");
        disclaimer.setWrapStyleWord(true);
        disclaimer.setLineWrap(true);
        disclaimer.setEditable(false);
        panel.add(new JScrollPane(disclaimer), BorderLayout.CENTER);

        JCheckBox acceptBox = new JCheckBox("I have read the above.");
        acceptBox.addItemListener(e -> disclaimerAccepted = acceptBox.isSelected());
        panel.add(acceptBox, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JRadioButton wifi = new JRadioButton("WiFi", true);
        JRadioButton ethernet = new JRadioButton("Ethernet");
        JRadioButton cellular = new JRadioButton("Cellular");
        ButtonGroup group = new ButtonGroup();
        group.add(wifi); group.add(ethernet); group.add(cellular);
        panel.add(new JLabel("Select connection method:"));
        panel.add(wifi); panel.add(ethernet); panel.add(cellular);
        panel.add(new JCheckBox("Using Tailscale"));
        panel.add(new JCheckBox("Using Radios"));
        return panel;
    }

    private JPanel buildTailscale() {
        Utility util = new Utility();
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel instructions = new JLabel("Tailscale Setup:");

        JTextArea info = util.buildTextArea(panel, 300);
        info.setText("""
                Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device
                
                Log in to Tailscale with a GitHub account; this can be a personal or organization account. Other users can be added later via email or an invite link, but only three users are allowed on a free plan
                
                On your computer, go to the Tailscale download page (https://tailscale.com/download) and get the app. Up to one hundred devices can be added for free, so don't worry about having too many devices online.
                """);

        panel.add(instructions, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRpiConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        profilesPanel = new JPanel();
        profilesPanel.setLayout(new BoxLayout(profilesPanel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        JButton addProfile = new JButton("Add RPi Profile");
        addProfile.addActionListener(e -> addRpiProfile("", ""));

        panel.add(new JScrollPane(profilesPanel), BorderLayout.CENTER);
        panel.add(addProfile, BorderLayout.SOUTH);

        addRpiProfile("", "");
        return panel;

    }

    private void addRpiProfile(String name, String addr) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        container.setMaximumSize(new Dimension(500, 40));

        JTextField nameField = new JTextField(name != null ? name : "");
        JTextField addrField = new JTextField(addr != null ? addr : "");

        container.add(new JLabel("Name:"));
        container.add(Box.createRigidArea(new Dimension(5, 0)));
        container.add(nameField);
        container.add(Box.createRigidArea(new Dimension(10, 0)));
        container.add(new JLabel("Address:"));
        container.add(Box.createRigidArea(new Dimension(5, 0)));
        container.add(addrField);

        JButton deleteBtn = new JButton(" X ");
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

    private JPanel buildFinalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Setup complete.");
        JButton finish = new JButton("Finish");
        finish.addActionListener(e -> saveProfilesAndExit());
        panel.add(label);
        panel.add(finish);
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
    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null
        );
    }

    private void saveProfilesAndExit() {
        try {
            Files.createDirectories(Paths.get("profiles"));
            for (RPiProfile p : profiles) {
                String name = p.nameField.getText().trim();
                String addr = p.addrField.getText().trim();
                if (!name.isEmpty() && !addr.isEmpty()) {
                    Properties props = new Properties();
                    props.setProperty("rpi_name", name);
                    props.setProperty("rpi_addr", addr);
                    props.store(new FileWriter("profiles/" + name + "_profile.properties"), "RPi Profile");
                }
            }
            Files.deleteIfExists(progressFile);
            JOptionPane.showMessageDialog(this, "Profiles saved. Wizard complete.");
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving profiles: " + e.getMessage());
        }
    }

    static class RPiProfile {
        JTextField nameField, addrField;
        RPiProfile(JTextField name, JTextField addr) {
            this.nameField = name;
            this.addrField = addr;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SetupWizard::new);
    }
}