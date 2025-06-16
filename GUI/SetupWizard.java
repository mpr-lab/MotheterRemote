import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class SetupWizard extends JFrame {
    private Path profileSaveDir = Paths.get(System.getProperty("user.home"), "profiles");
    private Path SQMSaveDir = Paths.get(System.getProperty("user.home"), "SQMdata");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JButton nextButton = new JButton("Next");
    private final JButton backButton = new JButton("Back");
    private final JProgressBar progressBar = new JProgressBar(0, 10);
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

    private int numCards = 10;

    public SetupWizard() {
        super("Initial Setup Wizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        autoDetectSystem();
        System.out.println(detectedOS);
        detectedOS = util.getDetectedOSType();
        System.out.println("after update" + detectedOS);


        // Sections
        sectionStart.put("Host Setup", 0);
        sectionStart.put("RPi Setup", 2);
        sectionStart.put("SSH Setup", 4);

        wizardSteps.add(buildDisclaimerPanel());                // 0
        wizardSteps.add(buildTailscale());                      // 1
        wizardSteps.add(buildRadio());                          // 2
        wizardSteps.add(buildRpiConfigPanel());                 // 3
        wizardSteps.add(buildProfileDirectoryPanel());          // 4
        wizardSteps.add(buildSQMDirectoryPanel());          // 4
        wizardSteps.add(buildSSH());                            // 5
        wizardSteps.add(buildSSH_Step1());                      // 6
        wizardSteps.add(buildSSH_Step2());                      // 7
        wizardSteps.add(buildSSH_Step3());          // 8
        wizardSteps.add(buildSSH_Step4());          // 9
        wizardSteps.add(buildFinalPanel());         // 10


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

        // Always check and maintain order of optional panels before navigating
        manageOptionalPanels();

        if (currentCard < numCards - 1) {
            currentCard++;
            updateNav();
        }
    }

    private void manageOptionalPanels() {
        // Remove all optional panels first
        if (addedPanels.contains("tailscale")) {
            removePanel("tailscale");
        }
        if (addedPanels.contains("radio")) {
            removePanel("radio");
        }

        // Re-insert panels in correct order based on user selection
        int insertIndex = 2; // after disclaimer and connection

        if (yTailscale.isSelected()) {
            insertPanel(buildTailscaleSetup(), insertIndex++, "tailscale");
        }

        // The radio panel must come after the rpi config panel, which is always present at index 2 (or 3 if tailscale is inserted)
        int radioIndex = yTailscale.isSelected() ? insertIndex + 1 : insertIndex;
        if (yRadio.isSelected()) {
            insertPanel(buildRadioSetup(), radioIndex, "radio");
        }
    }

    private void insertPanel(JPanel panel, int index, String key) {
        panel.setName(key);
        wizardSteps.add(index, panel);
        addedPanels.add(key);
        rebuildCardPanel();
    }

    private void removePanel(String key) {
        for (int i = 0; i < wizardSteps.size(); i++) {
            JPanel p = wizardSteps.get(i);
            if (p.getName() != null && p.getName().equals(key)) {
                wizardSteps.remove(i);
                break;
            }
        }
        addedPanels.remove(key);
        rebuildCardPanel();
    }

    private void rebuildCardPanel() {
        cardPanel.removeAll();
        for (int i = 0; i < wizardSteps.size(); i++) {
            cardPanel.add(wizardSteps.get(i), String.valueOf(i));
        }
        numCards = wizardSteps.size();
        cardLayout.first(cardPanel);
        cardLayout.show(cardPanel, String.valueOf(currentCard));
        progressBar.setMaximum(numCards - 1);
        progressBar.setValue(currentCard);
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
        saveProgress();
    }
    private void autoDetectSystem() {
        try {
            File configOut = new File("host_config.properties");
            ProcessBuilder pb = new ProcessBuilder("python3", "../ssh/auto_setup.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(configOut));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[AutoDetect] " + line);
                writer.write(line);
                writer.newLine();
            }

            writer.close();
            process.waitFor();
            System.out.println("[AutoDetect] System info saved to " + configOut.getAbsolutePath());

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

            // OS detection
            props.setProperty("os", detectedOS);

            // Profile save directory
            props.setProperty("profileSaveDir", profileSaveDir.toString());

            // Write to file
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

            // Restore navigation state
            currentCard = Integer.parseInt(props.getProperty("currentCard", "0"));
            disclaimerAccepted = Boolean.parseBoolean(props.getProperty("disclaimerAccepted", "false"));
            yTailscale.setSelected(Boolean.parseBoolean(props.getProperty("tailscaleEnabled", "false")));
            yRadio.setSelected(Boolean.parseBoolean(props.getProperty("radioEnabled", "false")));

            // Restore OS selection
            String os = props.getProperty("os", "").toLowerCase();
            if (!os.isEmpty()) {
                detectedOS = os;
            } else {
                autoDetectSystem();
                detectedOS = util.getDetectedOSType();
            }

            // Restore user-chosen profile save directory
            String savedDir = props.getProperty("profileSaveDir");
            if (savedDir != null && !savedDir.isBlank()) {
                profileSaveDir = Paths.get(savedDir);
            }

            // Load profiles from chosen directory
            profilesPanel.removeAll();
            profiles.clear();
            if (Files.exists(profileSaveDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(profileSaveDir, "*_profile.properties")) {
                    for (Path file : stream) {
                        Properties p = new Properties();
                        try (FileReader reader = new FileReader(file.toFile())) {
                            p.load(reader);
                            String name = p.getProperty("rpi_name", "");
                            String addr = p.getProperty("rpi_addr", "");
                            if (!name.isEmpty() && !addr.isEmpty()) {
                                addRpiProfile(name, addr);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("[Setup] Failed to read profiles: " + e.getMessage());
                }
            }

            updateNav();

        } catch (IOException | NumberFormatException e) {
            System.err.println("[Setup] Failed to load progress: " + e.getMessage());
        }
    }


    private JPanel buildDisclaimerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea disclaimer = new JTextArea("IMPORTANT: Please read this disclaimer fully before continuing... NEED JAVA 24javac --release 21 ...\n");
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

        JLabel title = new JLabel("TAILSCALE");

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

        panel.add(new JLabel("TAILSCALE SETUP"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private final JCheckBox yRadio = new JCheckBox("yes");
    private JPanel buildRadio(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("RADIO");

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

        JLabel title = new JLabel("RADIO SETUP");

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

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(new JLabel("CONFIGURE RASPBERRY PI PROFILES"));
        north.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea rpiConfig = util.buildTextArea(north, 75);
        rpiConfig.setText("""
                Here is where you will set up new raspberry pi profiles. Input your rpi's name and ip address.
                
                If you are using tailscale, you can simply use the rpi's name as its address.
                """);
        north.add(rpiConfig);
        north.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton addProfile = new JButton("Add RPi Profile");
        addProfile.addActionListener(e -> addRpiProfile("", ""));

        panel.add(north, BorderLayout.NORTH);
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

    private JPanel buildProfileDirectoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Select a directory where your Raspberry Pi profiles will be saved:");
        JTextField pathField = new JTextField(profileSaveDir.toString(), 30);
        pathField.setEditable(false);

        JButton toggleChooserButton = new JButton("Show File Chooser");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(pathField);
        inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        inputPanel.add(toggleChooserButton);

        // Create the embedded file chooser
        JFileChooser embeddedChooser = new JFileChooser();
        embeddedChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        embeddedChooser.setPreferredSize(new Dimension(500, 275));  // smaller height
        embeddedChooser.setVisible(false);  // initially hidden

        // Toggle visibility of file chooser
        toggleChooserButton.addActionListener(e -> {
            boolean isVisible = embeddedChooser.isVisible();
            embeddedChooser.setVisible(!isVisible);
            toggleChooserButton.setText(isVisible ? "Show File Chooser" : "Hide File Chooser");
            panel.revalidate();
            panel.repaint();
        });

        // Handle selection or cancel
        embeddedChooser.addActionListener(e -> {
            if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                File selected = embeddedChooser.getSelectedFile();
                profileSaveDir = selected.toPath();
                pathField.setText(profileSaveDir.toString());
            }
            // Always hide after action
            embeddedChooser.setVisible(false);
            toggleChooserButton.setText("Show File Chooser");
            panel.revalidate();
            panel.repaint();
        });

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(inputPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(embeddedChooser);

        return panel;
    }

    private JPanel buildSQMDirectoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("Select a directory where data synced from the rpi will be saved:");
        JTextField pathField = new JTextField(SQMSaveDir.toString(), 30);
        pathField.setEditable(false);

        JButton toggleChooserButton = new JButton("Show File Chooser");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(pathField);
        inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        inputPanel.add(toggleChooserButton);

        // Create the embedded file chooser
        JFileChooser embeddedChooser = new JFileChooser();
        embeddedChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        embeddedChooser.setPreferredSize(new Dimension(500, 275));  // smaller height
        embeddedChooser.setVisible(false);  // initially hidden

        // Toggle visibility of file chooser
        toggleChooserButton.addActionListener(e -> {
            boolean isVisible = embeddedChooser.isVisible();
            embeddedChooser.setVisible(!isVisible);
            toggleChooserButton.setText(isVisible ? "Show File Chooser" : "Hide File Chooser");
            panel.revalidate();
            panel.repaint();
        });

        // Handle selection or cancel
        embeddedChooser.addActionListener(e -> {
            if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                File selected = embeddedChooser.getSelectedFile();
                SQMSaveDir = selected.toPath();
                pathField.setText(SQMSaveDir.toString());
            }
            // Always hide after action
            embeddedChooser.setVisible(false);
            toggleChooserButton.setText("Show File Chooser");
            panel.revalidate();
            panel.repaint();
        });

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(inputPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(embeddedChooser);

        return panel;
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

        panel.add(new JLabel("SSH SETUP"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
    private JPanel buildSSH_Step1(){
        JPanel panel = new JPanel();
        switch (detectedOS) {
            case "windows" -> {
                setup_windows windows = new setup_windows(util);
                panel = windows.buildSSH_Step1();
            }
//            case "mac"     -> {
//            }
            case "linux"   -> {
                setup_linux linux = new setup_linux(util);
                panel = linux.buildSSH_Step1();
            }
        }
        return panel;
    }

//    private JPanel buildSSH_Step1(){
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        JPanel inner = new JPanel();
//        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
//
//        // STEP 1: CHECK IS SSH INSTALLED< IF IT's NOT, INSTALL IT
//        JPanel step1 = new JPanel();
//        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
//        step1.add(new JLabel("Step 1: Check if SSH is installed"));
//        step1.add(Box.createRigidArea(new Dimension(0, 10)));
//
//        JTextArea copyI1 = util.buildTextArea(step1, 45);
//        copyI1.setText("Open up a new terminal. First, check whether or not you have ssh installed. This looks different on different operating systems. Your operating system is "+". If that does not seem right....");
//        String sshCmd = "";
//        JTextArea proceedI1 = util.buildTextArea(step1, 45);
//        switch (detectedOS) {
//            case "windows" -> {
//                sshCmd = "ssh";
//                proceedI1.setText("For windows users, there should be a line printed in the output of the terminal after running the first command that says 'Active: active (running)' if SSH is active. If this is the case, then move onto [STEP 2]. If not, move on to [STEP 2a].");
//            }
//            case "mac"     -> {
//                sshCmd = "sudo systemsetup -getremotelogin";
//                proceedI1.setText("For linux users, there should be a line printed in the output of the terminal after running the first command that says 'Active: active (running)' if SSH is active. If this is the case, then move onto [STEP 2]. If not, move on to [STEP 2a].");
//            }
//            case "linux"   -> {
//                sshCmd = "systemctl status sshd";
//                proceedI1.setText("For linux users, there should be a line printed in the output of the terminal after running the first command that says 'Active: active (running)' if SSH is active. If this is the case, then move onto [STEP 2]. If not, move on to [STEP 2a].");
//            }
//        }
//        JPanel checkSSHRow = buildCopyRow(sshCmd);
//
//        step1.add(copyI1);
//        step1.add(Box.createRigidArea(new Dimension(0, 10)));
//        step1.add(checkSSHRow);
//        step1.add(Box.createRigidArea(new Dimension(0, 10)));
//        step1.add(proceedI1);
//        step1.add(Box.createRigidArea(new Dimension(0, 30)));
//
//        // STEP 1a: DOWNLOAD SSH IF NOT DOWNLOADED
//        JPanel step1a = new JPanel();
//        step1a.setLayout(new BoxLayout(step1a, BoxLayout.Y_AXIS));
//        step1a.add(new JLabel("Step 1a: Download SSH"));
//        step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//
////        JTextArea copyI1a = util.buildTextArea(step1, 15);
////        copyI1.setText("If you do not already have ssh installed: ");
////        step1a.add(copyI1a);
////        step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//
//        switch (detectedOS) {
//            case "windows" -> {
//                JTextArea download = util.buildTextArea(step1, 200);
//                download.setText("""
//                        If SSH is installed, it will display help information about the command, including its usage and available options. If the terminal returns: "SSH not recognized", then this means SSH is not installed or enabled on your system. Follow these steps to download ssh:
//
//                        1) Go to Settings > Apps > Apps & Features > Optional Features
//
//                        2) Click on "Add a feature" and select OpenSSH Client
//
//                        3) Click "Install" to add the OpenSSH Client to your system
//
//                        4) You may need to restart your system for the changes to take effect
//
//                        5) After installing OpenSSH Client, open a new command prompt or PowerShell and type `ssh` again to confirm that it is now recognized.
//                        """);
//                step1a.add(download);
//                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//            }
//            case "mac"     -> {
//                JTextArea download = util.buildTextArea(step1, 200);
//                download.setText("""
//                Mac operating systems should come pre-installed with SSH. If SSH is enabled, the output will show "Remote Login: On". If it's off, it will show "Remote Login: Off". SSH may be disabled on your system. In that case, follow these steps to enable it:
//
//                1) Open System Preferences (Apple menu > System Preferences)
//
//                2) Go to Sharing
//
//                3) Look for "Remote Login" on the left-hand side
//
//                4) To enable SSH, activate the checkmark next to "Remote Login"
//                """);
//                step1a.add(download);
//                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//            }
//            case "linux"   -> {
//                JTextArea download = util.buildTextArea(step1, 45);
//                download.setText("""
//                If the terminal says anything along the lines of "Unit file sshd.service does not exist", then you do not have ssh installed. Run the following command in your terminal to install SSH:
//                """);
//                step1a.add(download);
//                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//
//                String downloadCmd = "sudo apt install openssh-server openssh-client";
//                JPanel downloadSSHRow = buildCopyRow(downloadCmd);
//                step1a.add(downloadSSHRow);
//                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//
//                JTextArea enable = util.buildTextArea(step1, 45);
//                enable.setText("""
//                It may be the case that SSH is installed but is not active. To enable SSH, run the following command in your terminal:
//                """);
//                step1a.add(download);
//                step1a.add(Box.createRigidArea(new Dimension(0, 10)));
//
//                String enableCmd = "sudo systemctl start ssh";
//                JPanel enableSSHRow = buildCopyRow(enableCmd);
//                step1a.add(enableSSHRow);
//                step1a.add(Box.createRigidArea(new Dimension(0, 20)));
//            }
//        }
//
//        inner.add(step1);
//        inner.add(step1a);
//
//        JScrollPane scroll = new JScrollPane(inner);
//        scroll.setBorder(null);
//
//        panel.add(scroll);
//        return panel;
//    }

    private JPanel buildSSH_Step2(){
        JPanel panel = new JPanel();
        switch (detectedOS) {
            case "windows" -> {
                setup_windows windows = new setup_windows(util);
                panel = windows.buildSSH_Step2();
            }
//            case "mac"     -> {
//            }
            case "linux"   -> {
                setup_linux linux = new setup_linux(util);
                panel = linux.buildSSH_Step2();
            }
        }
        return panel;
    }

    private JPanel buildSSH_Step3(){
        JPanel panel = new JPanel();
        switch (detectedOS) {
            case "windows" -> {
                setup_windows windows = new setup_windows(util);
                panel = windows.buildSSH_Step3();
            }
//            case "mac"     -> {
//            }
            case "linux"   -> {
                setup_linux linux = new setup_linux(util);
                panel = linux.buildSSH_Step3();
            }
        }
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
        step4.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 4: VERIFY SSH CONNECTION
        JPanel step5 = new JPanel();
        step5.setLayout(new BoxLayout(step5, BoxLayout.Y_AXIS));
        step5.add(new JLabel("Step 5: Verify SSH Connection"));
        step5.add(Box.createRigidArea(new Dimension(0, 10)));


        JTextArea I5 = util.buildTextArea(step5, 30);
        I5.setText("You have successfully set up the SSH connection for your RPi. Remember to repeat steps 1-4 of the SSH setup for each raspberry pi you have.");

        // Add to Panel
        inner.add(step4);
        inner.add(step5);

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
    private void saveHostPathsToConfig() {
        try {
            Path hostConfigPath = Paths.get("host_config.properties");

            // Load existing properties if the file already exists
            Properties props = new Properties();
            if (Files.exists(hostConfigPath)) {
                try (FileReader reader = new FileReader(hostConfigPath.toFile())) {
                    props.load(reader);
                }
            }

            // Update or add the new path entries
            props.setProperty("profile_save_path", profileSaveDir.toString());
            props.setProperty("sqm_data_path", SQMSaveDir.toString());

            // Write back to the file
            try (FileWriter writer = new FileWriter(hostConfigPath.toFile())) {
                props.store(writer, "Host Configuration Paths");
            }

            System.out.println("[Setup] Host paths saved to config: " + hostConfigPath);
            updatePathPy(SQMSaveDir.toString());

        } catch (IOException e) {
            System.err.println("[Setup] Failed to save host config paths: " + e.getMessage());
        }
    }
    public void updatePathPy(String newPath) {
        try {
            File file = new File("../ssh/configs_ssh.py");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("host_data_path ="))
                    line = "host_data_path = \"" + newPath + "\"";

                content.append(line).append("\n");
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

        } catch (IOException e) {
            util.append("[Error] configs.py update failed: " + e.getMessage());
        }
    }


    private void saveProfilesAndExit() {
        try {
            Files.createDirectories(profileSaveDir);
            for (RPiProfile p : profiles) {
                String name = p.nameField.getText().trim();
                String addr = p.addrField.getText().trim();
                if (!name.isEmpty() && !addr.isEmpty()) {
                    Properties props = new Properties();
                    props.setProperty("rpi_name", name);
                    props.setProperty("rpi_addr", addr);
                    props.store(new FileWriter(profileSaveDir.resolve(name + "_profile.properties").toFile()), "RPi Profile");
                }
            }
            saveHostPathsToConfig();
            Files.deleteIfExists(progressFile);
            JOptionPane.showMessageDialog(this, "Profiles saved to:\n" + profileSaveDir.toString() + "\nWizard complete.");
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