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
    private final JProgressBar progressBar = new JProgressBar(0, 7);
    private final Map<String, Integer> sectionStart = new LinkedHashMap<>();
    private int currentCard = 0;

    private final java.util.List<RPiProfile> profiles = new ArrayList<>();
    private JPanel profilesPanel;

    private boolean disclaimerAccepted = false;
    private final Path progressFile = Paths.get(".setup_progress.properties");
    private String detectedOS = "";
    Utility util = new Utility();

    private final java.util.List<JPanel> wizardSteps = new ArrayList<>();
    private final Set<String> addedPanels = new HashSet<>(); // prevent duplicate inserts

    private int numCards = 9;

    public SetupWizard() {
        super("Initial Setup Wizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        autoDetectSystem();

        // Sections
        sectionStart.put("Host Setup", 0);
        sectionStart.put("RPi Setup", 2);
        sectionStart.put("SSH Setup", 4);

        wizardSteps.add(buildDisclaimerPanel());    // 0
        wizardSteps.add(buildTailscale());          // 1
        wizardSteps.add(buildRadio());              // 2
        wizardSteps.add(buildRpiConfigPanel());     // 3
        wizardSteps.add(buildSSH());                // 4
        wizardSteps.add(buildSSH_Step1());          // 5
        wizardSteps.add(buildSSH_Step2());          // 6
        wizardSteps.add(buildSSH_Step3());          // 7
        wizardSteps.add(buildSSH_Step4());          // 8
        wizardSteps.add(buildFinalPanel());         // 9


        // Add all to cardPanel
        for (int i = 0; i < wizardSteps.size(); i++) {
            cardPanel.add(wizardSteps.get(i), String.valueOf(i));
        }
        numCards = wizardSteps.size();

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

        // Inject tailscale panel after user selection
        if (currentCard == 1 && yTailscale.isSelected() && !addedPanels.contains("tailscale")) {
            insertPanel(buildTailscaleSetup(), currentCard + 1, "tailscale");
        }

        // Inject radio panel after user selection
        if (currentCard == 2 && yRadio.isSelected() && !addedPanels.contains("radio")) {
            insertPanel(buildRadioSetup(), currentCard + 1, "radio");
        }

        if (currentCard < numCards - 1) {
            currentCard++;
            updateNav();
        }
    }

    private void insertPanel(JPanel panel, int index, String key) {
        wizardSteps.add(index, panel);
        // Rebuild cardPanel
        cardPanel.removeAll();
        for (int i = 0; i < wizardSteps.size(); i++) {
            cardPanel.add(wizardSteps.get(i), String.valueOf(i));
        }
        addedPanels.add(key);
        numCards = wizardSteps.size();
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private void prevCard() {
        if (currentCard > 0) currentCard--;
        updateNav();
    }

    private void updateNav() {
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        backButton.setEnabled(currentCard > 0);
        nextButton.setEnabled(currentCard < numCards);
        progressBar.setValue(currentCard);
        progressBar.setString("Step " + (currentCard + 1) + " of " + numCards);
        saveProgress();
    }
    private void autoDetectSystem() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "../comms-GUI/auto_setup.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("OS=")) {
                    detectedOS = line.substring(3).trim().toLowerCase();
                }
            }

            process.waitFor();

            System.out.println("[AutoDetect] OS = " + detectedOS);

        } catch (IOException | InterruptedException e) {
            System.err.println("[Auto-Detect] Failed to run auto_setup.py: " + e.getMessage());
        }
    }



    private void saveProgress() {
        try {
            Properties props = new Properties();

            // Navigation state
            props.setProperty("currentCard", String.valueOf(currentCard));
            props.setProperty("disclaimerAccepted", String.valueOf(disclaimerAccepted));
            props.setProperty("tailscaleEnabled", String.valueOf(yTailscale.isSelected()));
            props.setProperty("radioEnabled", String.valueOf(yRadio.isSelected()));

            // OS selection
            props.setProperty("os", detectedOS);

            // RPi profiles
            for (int i = 0; i < profiles.size(); i++) {
                RPiProfile p = profiles.get(i);
                props.setProperty("profile." + i + ".name", p.nameField.getText().trim());
                props.setProperty("profile." + i + ".addr", p.addrField.getText().trim());
            }

            try (FileWriter fw = new FileWriter(progressFile.toFile())) {
                props.store(fw, "Wizard Progress");
            }

        } catch (IOException e) {
            System.err.println("[Setup] Failed to save progress: " + e.getMessage());
        }
    }


    private void loadProgress() {
        if (!Files.exists(progressFile)) return;

        Properties props = new Properties();
        try (FileReader fr = new FileReader(progressFile.toFile())) {
            props.load(fr);

            // Current position
            currentCard = Integer.parseInt(props.getProperty("currentCard", "0"));
            disclaimerAccepted = Boolean.parseBoolean(props.getProperty("disclaimerAccepted", "false"));
            yTailscale.setSelected(Boolean.parseBoolean(props.getProperty("tailscaleEnabled", "false")));
            yRadio.setSelected(Boolean.parseBoolean(props.getProperty("radioEnabled", "false")));

            // OS selection
            String os = props.getProperty("os", "").toLowerCase();
            if (!os.isEmpty()) {
                detectedOS = os;
            } else {
                autoDetectSystem(); // fallback if not present
            }
            System.out.println("[LoadProgress] Loaded OS = " + detectedOS);

            // Restore RPi profiles
            profilesPanel.removeAll();
            profiles.clear();
            int i = 0;
            while (props.containsKey("profile." + i + ".name")) {
                String name = props.getProperty("profile." + i + ".name", "");
                String addr = props.getProperty("profile." + i + ".addr", "");
                addRpiProfile(name, addr);
                i++;
            }

            updateNav();

        } catch (IOException | NumberFormatException e) {
            System.err.println("[Setup] Failed to load progress: " + e.getMessage());
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

    private final JCheckBox yTailscale = new JCheckBox("yes");
    private JPanel buildTailscale() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Tailscale");

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JTextArea info = util.buildTextArea(panel, 200);
        info.setText("""
            Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device. Tailscale is only required for cellular connections but may be useful in WiFi setups as well, because it lets you maintain a static IP address.
            """);

        JLabel question = new JLabel("Will you be using Tailscale?");

        inner.add(info);
        bottom.add(question);
        bottom.add(Box.createRigidArea(new Dimension(0, 10)));
        bottom.add(yTailscale);

        panel.add(title, BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTailscaleSetup(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        // STEP 1:
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.add(new JLabel("Step 1: Create a Tailscale Account"));
        step1.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea I1 = util.buildTextArea(step1, 75);
        I1.setText("Log in to Tailscale with a GitHub account; this can be a personal or organization account. Other users can be added later via email or an invite link, but only three users are allowed on a free plan.");
        step1.add(I1);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));

        // STEP 1:
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.add(new JLabel("Step 2: Download Tailscale on your computer"));
        step2.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea I2a = util.buildTextArea(step1, 30);
        I2a.setText("On your computer, open up a browser, go to the Tailscale download page and get the app. The link can be found below:");
        String link = "https://tailscale.com/download";
        JPanel tailscaleDwnld = buildCopyRow(link);
        JTextArea I2b = util.buildTextArea(step1, 30);
        I2b.setText("Up to a hundred devices can be added for free, so don't worry about having too many devices online.");

        step2.add(I2a);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(tailscaleDwnld);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(I2b);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));

        // add to panel
        inner.add(step1);
        inner.add(step2);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(new JLabel("Tailscale Setup"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private final JCheckBox yRadio = new JCheckBox("yes");
    private JPanel buildRadio(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Radio");

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JTextArea info = util.buildTextArea(panel, 300);
        info.setText("""
                Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device
                """);

        JLabel question = new JLabel("Will you be using Radios?");

        inner.add(info);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));
        bottom.add(question);
        bottom.add(Box.createRigidArea(new Dimension(0, 10)));
        bottom.add(yRadio);

        panel.add(title, BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRadioSetup(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Radio Setup");

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        panel.add(title, BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRpiConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        profilesPanel = new JPanel();
        profilesPanel.setLayout(new BoxLayout(profilesPanel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Configure Raspberry Pi Profiles"), BorderLayout.NORTH);


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

    private JPanel buildSSH(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea description = util.buildTextArea(inner, 30);
        description.setText("This GUI uses Secure Shell to remotely access and run commands on the raspberry pi. This section of the setup will walk you through setting up Secure Shell (SSH), and using it to connect to a raspberry pi.");
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea terminal = util.buildTextArea(inner, 30);
        terminal.setText("In order to set up SSH, you will need to use the terminal on your computer. In order to open the terminal");
        inner.add(terminal);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(new JLabel("SSH Setup"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSSH_Step1(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 1: CHECK IS SSH INSTALLED< IF IT's NOT, INSTALL IT
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.add(new JLabel("Step 1: Check if SSH is installed"));
        step1.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI1 = util.buildTextArea(step1, 30);
        copyI1.setText("Open up a new terminal. First, check whether or not you have ssh installed. This looks different on different operating systems. Your operating system is "+". If that does not seem right....");
        String sshCmd = "";
        JTextArea proceedI1 = util.buildTextArea(step1, 45);
        switch (detectedOS) {
            case "windows" -> {
                sshCmd = "ssh";
            }
            case "mac"     -> {
                sshCmd = "sudo systemsetup -getremotelogin";
            }
            case "linux"   -> {
                sshCmd = "systemctl status sshd";
                proceedI1.setText("For linux users, there should be a line printed in the output of the terminal after running the first command that says 'Active: active (running)' if SSH is active. If this is the case, then move onto [STEP 2]. If not, move on to [STEP 2a].");
            }
        }
        JPanel checkSSHRow = buildCopyRow(sshCmd);

        step1.add(copyI1);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));
        step1.add(checkSSHRow);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));
        step1.add(proceedI1);
        step1.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 1a: DOWNLOAD SSH IF NOT DOWNLOADED
        JPanel step1a = new JPanel();
        step1a.setLayout(new BoxLayout(step1a, BoxLayout.Y_AXIS));
        step1a.add(new JLabel("Step 1a: Download SSH"));
        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

//        JTextArea copyI1a = util.buildTextArea(step1, 15);
//        copyI1.setText("If you do not already have ssh installed: ");
//        step1a.add(copyI1a);
//        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

        switch (detectedOS) {
            case "windows" -> {
                JTextArea download = util.buildTextArea(step1, 200);
                download.setText("""
                        If SSH is installed, it will display help information about the command, including its usage and available options. If the terminal returns: "SSH not recognized", then this means SSH is not installed or enabled on your system. Follow these steps to download ssh:
                                  
                        1) Go to Settings > Apps > Apps & Features > Optional Features
                                            
                        2) Click on "Add a feature" and select OpenSSH Client
                                            
                        3) Click "Install" to add the OpenSSH Client to your system
                                            
                        4) You may need to restart your system for the changes to take effect
                                            
                        5) After installing OpenSSH Client, open a new command prompt or PowerShell and type `ssh` again to confirm that it is now recognized.                
                        """);
                step1a.add(download);
                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            case "mac"     -> {
                JTextArea download = util.buildTextArea(step1, 200);
                download.setText("""
                Mac operating systems should come pre-installed with SSH. If SSH is enabled, the output will show "Remote Login: On". If it's off, it will show "Remote Login: Off". SSH may be disabled on your system. In that case, follow these steps to enable it:
                
                1) Open System Preferences (Apple menu > System Preferences)
                
                2) Go to Sharing
                
                3) Look for "Remote Login" on the left-hand side
                
                4) To enable SSH, activate the checkmark next to "Remote Login"
                """);
                step1a.add(download);
                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            case "linux"   -> {
                JTextArea download = util.buildTextArea(step1, 60);
                download.setText("""
                If the terminal says anything along the lines of "Unit file sshd.service does not exist", then you do not have ssh installed. Run the following command in your terminal to install SSH:
                """);
                step1a.add(download);
                step1a.add(Box.createRigidArea(new Dimension(0, 10)));

                String downloadCmd = "sudo apt install openssh-server openssh-client";
                JPanel downloadSSHRow = buildCopyRow(downloadCmd);
                step1a.add(downloadSSHRow);
                step1a.add(Box.createRigidArea(new Dimension(0, 10)));

                JTextArea enable = util.buildTextArea(step1, 45);
                enable.setText("""
                It may be the case that SSH is installed but is not active. To enable SSH, run the following command in your terminal:
                """);
                step1a.add(download);
                step1a.add(Box.createRigidArea(new Dimension(0, 10)));

                String enableCmd = "sudo systemctl start ssh";
                JPanel enableSSHRow = buildCopyRow(enableCmd);
                step1a.add(enableSSHRow);
                step1a.add(Box.createRigidArea(new Dimension(0, 20)));
            }
        }

        inner.add(step1);
        inner.add(step1a);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    private JPanel buildSSH_Step2(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 2 GENERATE SSH KEY
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.add(new JLabel("Step 2: SSH Key"));
        step2.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI2 = util.buildTextArea(step2, 75);
        copyI2.setText("Now that SSH is downloaded, you must establish yourself as a known host for the RPi. This will allow you to connect remotely to the RPi without having to enter a password. First, let's check to see if you already have an SSH key. Run the following command in your terminal:");

        String checkCmd = "ls -al ~/.ssh";
        JPanel checkKeySSHRow = buildCopyRow(checkCmd);

        JTextArea proceedI2 = util.buildTextArea(step2, 30);
        proceedI2.setText("If there is a file called id_ed_25519, then you already have an SSH key. Proceed to [STEP 3]. If you do not have a file called id_ed_25519, proceed to [STEP 2a].");

        step2.add(copyI2);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(checkKeySSHRow);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(proceedI2);
        step2.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 2 GENERATE SSH KEY
        JPanel step2a = new JPanel();
        step2a.setLayout(new BoxLayout(step2a, BoxLayout.Y_AXIS));
        step2a.add(new JLabel("Step 2a: Generate a SSH Key"));
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI2a = util.buildTextArea(step2, 30);
        copyI2a.setText("If the SSH key does not exist yet, we must generate one to be copied to the RPi. Run the following command into your terminal:");

        String generateCmd = "ssh-keygen -t ed25519";
        JPanel genSSHRow = buildCopyRow(generateCmd);

        step2a.add(copyI2a);
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));
        step2a.add(genSSHRow);
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add to Panel
        inner.add(step2);
        inner.add(step2a);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    private JPanel buildSSH_Step3(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

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

        // Add to Panel
        inner.add(step3);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    private JPanel buildSSH_Step4(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 4: VERIFY SSH CONNECTION
        JPanel step4 = new JPanel();
        step4.setLayout(new BoxLayout(step4, BoxLayout.Y_AXIS));
        step4.add(new JLabel("Step 4: Verify SSH Connection"));
        step4.add(Box.createRigidArea(new Dimension(0, 10)));


        JTextArea copyI4 = util.buildTextArea(step4, 30);
        copyI4.setText("Finally, check to make sure that the SSH connection is working properly. In the terminal, run the following command:");

        String verifyCmd = "ssh <rpi_name>@<rpi_addr>";
        JPanel verifySSHRow = buildCopyRow(verifyCmd);

        JTextArea changeI4 = util.buildTextArea(step4, 45);
        changeI4.setText("Make sure to change <rpi_name> and <rpi_addr> with the correct information. If you setup the ssh connection correctly, you should be able to access the RPi without having to input a password.");

        step4.add(copyI4);
        step4.add(Box.createRigidArea(new Dimension(0, 10)));
        step4.add(verifySSHRow);
        step4.add(Box.createRigidArea(new Dimension(0, 10)));
        step4.add(changeI4);
        step4.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add to Panel
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
        util.setFullWidth.accept(label);
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