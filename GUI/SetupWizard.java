import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.awt.datatransfer.StringSelection;

public class SetupWizard extends JFrame {
    private Path profileSaveDir = Paths.get("profiles");

    private Path SQMSaveDir = Paths.get("SQMData");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JButton nextButton = new JButton("Next");
    private final JButton backButton = new JButton("Back");
    private final JProgressBar progressBar = new JProgressBar(0, 10);
    private int currentCard = 0;

    private final java.util.List<RPiProfile> profiles = new ArrayList<>();
    private JPanel profilesPanel;

    private boolean disclaimerAccepted = false;
    private final Path progressFile = Paths.get(".setup_progress.properties");
    private String detectedOS = "";
    Utility util = new Utility();
    int WIDTH = 500;
    int HEIGHT = 650;

    private final java.util.List<JPanel> wizardSteps = new ArrayList<>();
    private final Set<String> addedPanels = new HashSet<>(); // prevent duplicate inserts

    private int numCards = 12;
    private final File SETUP_PATH = new File("python-scripts/ssh/auto_setup.py");

    public SetupWizard() {
        super("Initial Setup Wizard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        autoDetectSystem();
        detectedOS = util.getDetectedOSType();

        wizardSteps.add(buildDisclaimerPanel());                // 0
        wizardSteps.add(buildTailscale());                      // 1
        wizardSteps.add(buildRadio());                          // 2
        wizardSteps.add(buildRPiSetupPanel());                  // 3
        wizardSteps.add(buildRpiConfigPanel());                 // 4
        wizardSteps.add(buildProfileDirectoryPanel());          // 5
        wizardSteps.add(buildSQMDirectoryPanel());              // 6
        wizardSteps.add(buildSSH());                            // 7
        wizardSteps.add(buildSSH_Step1());                      // 8
        wizardSteps.add(buildSSH_Step2());                      // 9
        wizardSteps.add(buildSSH_Step3());                      // 10
        wizardSteps.add(buildSSH_Step4());                      // 11
        wizardSteps.add(buildFinalPanel());                     // 12


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

        int radioIndex = yTailscale.isSelected() ? insertIndex + 1 : insertIndex;
        if (yRadio.isSelected()) {
            insertPanel(buildRadioSetup(), insertIndex + 1, "radio");
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
            ProcessBuilder pb = new ProcessBuilder(util.getPythonPath(), SETUP_PATH.getAbsolutePath());
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
    Border spaceBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1);

    private Border createTitle(String title){
        TitledBorder titleBorder = BorderFactory.createTitledBorder(title);
        titleBorder.setTitleJustification(TitledBorder.CENTER);
        Border compoundBorder = BorderFactory.createCompoundBorder(titleBorder, spaceBorder);
        return compoundBorder;
    }

    private void unbold(JLabel text){
        Font currentFont = text.getFont();
        Font plainFont = currentFont.deriveFont(Font.PLAIN);
        text.setFont(plainFont);
    }

    private JPanel buildDisclaimerPanel() {
        // JPanel panel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inner.setMaximumSize(new Dimension(WIDTH - 20, HEIGHT - 20));
        JLabel disclaimer = new JLabel("<html>IMPORTANT: Please read this disclaimer fully before continuing... NEED JAVA 24javac --release 21 ...\n<html>");
        // JTextArea disclaimer = new JTextArea("IMPORTANT: Please read this disclaimer fully before continuing... NEED JAVA 24javac --release 21 ...\n");
        // disclaimer.setWrapStyleWord(true);
        // disclaimer.setLineWrap(true);
        // disclaimer.setEditable(false);
        // panel.add(new JScrollPane(disclaimer), BorderLayout.CENTER);
        inner.add(disclaimer);

        JCheckBox acceptBox = new JCheckBox("I have read the above.");
        acceptBox.addItemListener(e -> disclaimerAccepted = acceptBox.isSelected());
        panel.add(inner, BorderLayout.CENTER);
        panel.add(acceptBox, BorderLayout.SOUTH);
        return panel;
    }

    private final JCheckBox yTailscale = new JCheckBox("yes");

    private JPanel buildTailscale() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(createTitle("TAILSCALE"));

        // INNER
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel info = new JLabel("<html>Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device. Tailscale is only required for cellular connections but may be useful in WiFi setups as well, because it lets you maintain a static IP address.<html>");
        unbold(info);
        inner.add(info);

        // SOUTH
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        JLabel question = new JLabel("Will you be using Tailscale?");

        south.add(question);
        south.add(Box.createVerticalStrut(10));
        south.add(yTailscale);

        // ADDING ELEMENTS TO FULL PANEL
        panel.add(inner, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }


    String htmlStart = "<html>";
    String htmlEnd = "<html>";
    private JPanel buildTailscaleSetup(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitle("TAILSCALE SETUP"));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createCompoundBorder(lineBorder, spaceBorder));

        // STEP 1:
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.setBorder(createTitle("Step 1: Create a Tailscale Account"));
        JLabel I1 = new JLabel(htmlStart + "Log in to Tailscale with a GitHub account; this can be a personal or organization account. Other users can be added later via email or an invite link, but only three users are allowed on a free plan." + htmlEnd);
        unbold(I1);
        step1.add(I1);

        // STEP 1:
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.setBorder(createTitle("Step 2: Download Tailscale on your computer"));

        JLabel I2a = new JLabel(htmlStart + "On your computer, open up a browser, go to the Tailscale download page and get the app. The link can be found below:" + htmlEnd);
        unbold(I2a);
        String link = "https://tailscale.com/download";
        JPanel tailscaleDwnld = buildCopyRow(link);
        JLabel I2b = new JLabel(htmlStart + "Up to one hundred devices can be added for free, so don't worry about having too many devices online." + htmlEnd);
        unbold(I2b);

        step2.add(I2a);
        step2.add(Box.createVerticalStrut(10));
        step2.add(tailscaleDwnld);
        step2.add(Box.createVerticalStrut(10));
        step2.add(I2b);
        step2.add(Box.createVerticalStrut(20));

        // add to panel
        inner.add(step1);
        inner.add(step2);
        
        panel.add(inner, BorderLayout.CENTER);

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

        JTextArea info = util.buildTextArea(panel);
        info.setText("""
                Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device
                """);

        JLabel question = new JLabel("Will you be using Radios?");

        inner.add(info);
        inner.add(Box.createVerticalStrut(10));
        bottom.add(question);
        bottom.add(Box.createVerticalStrut(10));
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

        // STEP 1:

        panel.add(title, BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    private JPanel  buildRPiSetupPanel(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(createTitle("RASPBERRY PI SETUP"));     
        JLabel rpiIntro = new JLabel(htmlStart + "In order to get this system working, you must now set up your raspberry Pi devices. Follow the given instructions below:" + htmlEnd);
        unbold(rpiIntro);
        panel.add(rpiIntro);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(BorderFactory.createCompoundBorder(lineBorder, spaceBorder));

        // STEP 1: INSTALL CUSTOM IMAGE
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.setBorder(createTitle("Step 1: Download the Custom MPR-Remote RPi image"));

        JLabel copyI1 = new JLabel(htmlStart + "To setup your raspberry pi, you will need to download the MPR-Remote custom RPi image to an SD card. Navigate to the MPR bio-gui repository on Github, located at this link:" + htmlEnd);
        unbold(copyI1);
        String gitLink = "https://github.com/mpr-lab/bio-gui";
        JPanel githubRow = util.buildCopyRow(gitLink);

        JLabel proceedI1 = new JLabel(htmlStart + "Here, you will need to find the releases page, which is often located on the right. Go to the latest release and download the file called 'MPR-Remote.img'. This file contains a new Raspberry Pi Operating system, and all the necessary packages and project files to make your RPi run the MPR-Remote" + htmlEnd);
        unbold(proceedI1);

        step1.add(copyI1);
        step1.add(Box.createVerticalStrut(10));
        step1.add(githubRow);
        step1.add(Box.createVerticalStrut(10));
        step1.add(proceedI1);

        // STEP 2: INSTALL CUSTOM IMAGE
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.setBorder(createTitle("Step 2: Install Custom Image to RPi"));

        JLabel copyI2 = new JLabel(htmlStart + "Now, you must burn the custom Image onto a new microSD card. We recommend using the official Raspberry Pi Imager app which can be downloaded at this link:" + htmlEnd);
        unbold(copyI2);
        String imagerLink = "https://www.raspberrypi.com/software/";
        JPanel imagerRow = util.buildCopyRow(imagerLink);

        JLabel proceedI2 = new JLabel(htmlStart + """
                Once you have downloaded the Raspberry Pi Imager, boot it up. Insert a blank microSD card into your laptop (it doesn't necessarily need to be blank, but if it is not then all of it's contents will we wiped from it).<br>
                
                Under 'Raspberry Pi Device', select 'Raspberry Pi 4'.<br>
                
                Next, under 'Operating System' select 'Use Custom', which should be the last option. This will prompt you to provide a '.img' file. Navigate to the location where you installed the MPR-Remote custom image ('MPR-Remote.img') and select it.<br>
                
                Finally, choose the storage device, it should match the microSD card you inserted into your computer. Click yes and allow the RPi Imager to install all the files onto your microSD card.<br>
                """ + htmlEnd);
        unbold(proceedI2);

        step2.add(copyI2);
        step2.add(Box.createVerticalStrut(10));
        step2.add(imagerRow);
        step2.add(Box.createVerticalStrut(10));
        step2.add(proceedI2);

        // STEP 3: RASPBERRY PI
        JPanel step3 = new JPanel();
        step3.setLayout(new BoxLayout(step3, BoxLayout.Y_AXIS));
        step3.add(new JLabel("Step 3: Setting up the RPi"));
        step3.add(Box.createVerticalStrut(10));

        JTextArea copyI3 = util.buildTextArea(step3);
        copyI3.setText("The Raspberry Pi should already have everything it needs to run, but in order to connect your computer to it, you need some of it's information. First, you need the RPi's IP address. FILL THIS IN");
        String IPcmd = "Hostname -I";
        JPanel IPRow = util.buildCopyRow(IPcmd);

        JTextArea proceedI3 = util.buildTextArea(step3);
        proceedI3.setText(
                """
                FILL IN
                """);

        JTextArea copyI4 = util.buildTextArea(step3);
        copyI4.setText("If you are using Tailscale, you must run a few commands to get the RPi device to be a recognized device on your computer. Tailscale comes preinstalled on your Raspberry Pi with the custom image, but you still need to connect your machine to your Tailscale network and authenticate in your browser. Running the following command:");

        String tailscaleCmd = "sudo tailscale up";
        JPanel tailscaleRow = util.buildCopyRow(tailscaleCmd);

        JTextArea proceedI4 = util.buildTextArea(step3);
        proceedI4.setText("will generate a link which will allow you to log in in your browser. You can go to this link from another device, if you don't want to deal with using a web browser on a headless Pi. ");

        step3.add(copyI3);
        step3.add(Box.createVerticalStrut(10));
        step3.add(IPRow);
        step3.add(Box.createVerticalStrut(10));
        step3.add(proceedI3);
        step3.add(Box.createVerticalStrut(10));
        step3.add(copyI4);
        step3.add(Box.createVerticalStrut(10));
        step3.add(tailscaleRow);
        step3.add(Box.createVerticalStrut(10));
        step3.add(proceedI4);
        step3.add(Box.createVerticalStrut(20));

        inner.add(step1);
        inner.add(step2);
        inner.add(step3);

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
        north.add(Box.createVerticalStrut(10));

        JTextArea rpiConfig = util.buildTextArea(north);
        rpiConfig.setText("""
                Here is where you will set up new raspberry pi profiles. Input your rpi's name and ip address.
                
                If you are using tailscale, you can simply use the rpi's name as its address.
                """);
        north.add(rpiConfig);
        north.add(Box.createVerticalStrut(10));

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

        // Create it if it doesn't exist
        if (!Files.exists(profileSaveDir)) {
            try {
                Files.createDirectories(profileSaveDir);
            } catch (IOException e) {
                System.err.println("Failed to create profiles directory: " + e.getMessage());
            }
        }

        JLabel label = new JLabel("Select a directory where your Raspberry Pi profiles will be saved:");
        JTextField pathField = new JTextField(profileSaveDir.toFile().getAbsolutePath(), 30);
        pathField.setEditable(false);

        JButton toggleChooserButton = new JButton("Show File Chooser");

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(pathField);
        inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        inputPanel.add(toggleChooserButton);

        // Create the embedded file chooser
        JFileChooser embeddedChooser = new JFileChooser();
        embeddedChooser.setCurrentDirectory(profileSaveDir.toFile());
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
        panel.add(Box.createVerticalStrut(10));
        panel.add(inputPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(embeddedChooser);

        return panel;
    }

    private JPanel buildSQMDirectoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create it if it doesn't exist
        if (!Files.exists(SQMSaveDir)) {
            try {
                Files.createDirectories(SQMSaveDir);
            } catch (IOException e) {
                System.err.println("Failed to create profiles directory: " + e.getMessage());
            }
        }

        JLabel label = new JLabel("Select a directory where data synced from the rpi will be saved:");
        JTextField pathField = new JTextField(SQMSaveDir.toFile().getAbsolutePath(), 30);
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
        embeddedChooser.setCurrentDirectory(SQMSaveDir.toFile());
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
        panel.add(Box.createVerticalStrut(10));
        panel.add(inputPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(embeddedChooser);

        return panel;
    }


    private JPanel buildSSH(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(Box.createVerticalStrut(10));
        inner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = util.buildTextArea(inner);
        description.setText("This GUI uses Secure Shell to remotely access and run commands on the raspberry pi. This section of the setup will walk you through setting up Secure Shell (SSH), and using it to connect to a raspberry pi.");
        inner.add(description);
        inner.add(Box.createVerticalStrut(10));

        JTextArea terminal = util.buildTextArea(inner);
        terminal.setText("In order to set up SSH, you will need to use the terminal on your computer. In order to open the terminal");
        inner.add(terminal);
        inner.add(Box.createVerticalStrut(10));
        JScrollPane scroll = new JScrollPane(inner);
        scroll.getViewport().setViewPosition(new Point(0, 0));
        scroll.repaint();

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
            case "linux", "mac"   -> {
                setup_linux linux = new setup_linux(util);
                panel = linux.buildSSH_Step1();
            }
        }
        return panel;
    }

    private JPanel buildSSH_Step2(){
        JPanel panel = new JPanel();
        switch (detectedOS) {
            case "windows" -> {
                setup_windows windows = new setup_windows(util);
                panel = windows.buildSSH_Step2();
            }
//            case "mac"     -> {
//            }
            case "linux", "mac"   -> {
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
            case "linux", "mac"   -> {
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
        inner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inner.setMaximumSize(new Dimension(WIDTH - 20, HEIGHT - 20));

        // STEP 4: VERIFY SSH CONNECTION
        JPanel step4 = new JPanel();
        step4.setLayout(new BoxLayout(step4, BoxLayout.Y_AXIS));
        step4.add(new JLabel("Step 4: Verify SSH Connection"));
        step4.add(Box.createVerticalStrut(10));

        JLabel copyI4 = new JLabel("<html><p>Finally, check to make sure that the SSH connection is working properly. In the terminal, run the following command:<p><html>");
        // copyI4.setFont(new Font("Arial", Font.PLAIN, 12));

        String verifyCmd = "ssh <rpi_name>@<rpi_addr>";
        JPanel verifySSHRow = buildCopyRow(verifyCmd);

        JTextArea changeI4 = util.buildTextArea(step4);
        changeI4.setText("Make sure to change <rpi_name> and <rpi_addr> with the correct information. If you setup the ssh connection correctly, you should be able to access the RPi without having to input a password.");

        step4.add(copyI4);
        step4.add(Box.createVerticalStrut(10));
        step4.add(verifySSHRow);
        step4.add(Box.createVerticalStrut(10));
        step4.add(changeI4);
        step4.add(Box.createVerticalStrut(20));

        // STEP 4: VERIFY SSH CONNECTION
        JPanel step5 = new JPanel();
        step5.setLayout(new BoxLayout(step5, BoxLayout.Y_AXIS));
        step5.add(new JLabel("Step 5: Verify SSH Connection"));
        step5.add(Box.createVerticalStrut(10));


        JTextArea I5 = util.buildTextArea(step5);
        I5.setText("You have successfully set up the SSH connection for your RPi. Remember to repeat steps 1-4 of the SSH setup for each raspberry pi you have.");

        // Add to Panel
        inner.add(step4);
        inner.add(step5);

        // JScrollPane scroll = new JScrollPane(inner);
        // scroll.getViewport().setViewPosition(new Point(0, 0));
        // scroll.repaint();

        // panel.add(scroll);
        panel.add(inner);
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
    private void updatePathPy(String newPath) {
        try {
            String pythonPath;
            if (detectedOS.toLowerCase().contains("win")) {
                // Escape backslashes for Windows Python
                pythonPath = newPath.replace("\\", "\\\\");
            } else {
                // Keep forward slashes for Linux/Mac
                pythonPath = newPath;
            }

            File file = new File("python-scripts/ssh/configs_ssh.py");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("host_data_path ="))
                    line = "host_data_path =\"" + pythonPath + "\"";
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
                util.updateConfigsPy(name, addr);
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
        UIManager.put("defaultFont", new Font("Arial", Font.PLAIN, 12));
        SwingUtilities.invokeLater(SetupWizard::new);
    }
}