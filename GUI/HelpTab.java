import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class HelpTab extends JPanel{

    private JPanel helpPanel = new JPanel();
    public HelpTab(){
        setSize(800, 560);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> helpSelector = new JComboBox<>(new String[]{
                "General Help",
                "RPi Command Center Help",
                "Data Help",
                "Settings Help",
                "SSH Help"
        });

        JScrollPane scroll = new JScrollPane(helpPanel);

        helpSelector.addActionListener(e -> {
            String selection = (String) helpSelector.getSelectedItem();
            switch (selection) {
                case "General Help" -> setPanel(genHelp());
                case "RPi Command Center Help" -> setPanel(rpiHelp());
//                case "Sensor Command Center Help" -> setPanel(sensorHelp());
                case "Data Help" -> setPanel(dataHelp());
                case "Settings Help" -> setPanel(settingHelp());
                case "SSH Help" -> setPanel(settingHelp());
                case null, default -> setPanel(genHelp());

            }
        });

        panel.add(helpSelector, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }

    private JPanel buildTemplate(){
        JPanel template = new JPanel();
        template.setLayout(new BoxLayout(template, BoxLayout.Y_AXIS));
        template.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Helper to unify width and alignment
        return template;
    }
    int preferredWidth = 500;
    Consumer<JComponent> setFullWidth = comp -> {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d = comp.getPreferredSize();
        d.width = preferredWidth;
        comp.setMaximumSize(d);
    };

    private JTextArea buildTextArea(JPanel panel, int height){
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(panel.getBackground());
        textArea.setPreferredSize(new Dimension(preferredWidth, height));
        setFullWidth.accept(textArea);

        return textArea;
    }

    private JPanel genHelp() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setSize(800, 560);

        JPanel inner = buildTemplate();

        // Title
        JLabel title = new JLabel("GENERAL HELP");
        setFullWidth.accept(title);
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // General description
        JTextArea description = buildTextArea(inner, 375);
        description.setText("""
                This GUI allows interaction with a Raspberry Pi and sensor system.
                It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.


                LAYOUT:

                The overall GUI contains 4 main sections: The top dropdown menu, the tabs section, the main panel, and the console log.

                    * The top dropdown menu located at the very top of the GUI allows you to change which raspberry pi you want to connect to. This feature if useful if you setup multiple RPi profiles during setup.

                    * The tabs section located directly below the top dropdown menu allows you to switch what functions are shown on the main panel. Each of the 5 tabs have different features, more support for each tab can be found here in the help tab.

                    * The main panel which takes up the majority of the GUI displays the contents of the GUI which will change depending on what tab is selected.

                    * The console log can be minimized and restored. Located at the bottom of the GUI, the console log can be viewed from every tab and allows you to see...
                """);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // "How to Use" label
        JLabel howTO = new JLabel("How To Use:");
        setFullWidth.accept(howTO);
        inner.add(howTO);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // Instructional text
        JTextArea h2 = buildTextArea(inner, 60);
        h2.setText("The GUI uses tabbed panels to access different commands/functions. These tabs are as follows:");
        inner.add(h2);
        inner.add(Box.createRigidArea(new Dimension(0, 5)));

        // Optional: tab names as JList
        String[] tabs = { "RPi Command Center", "Sensor Command Center", "Data Sync", "Settings" };
        JList<String> listTabs = new JList<>(tabs);
        listTabs.setVisibleRowCount(4);
        listTabs.setFixedCellHeight(20);
        JScrollPane tabScroll = new JScrollPane(listTabs);
        tabScroll.setPreferredSize(new Dimension(preferredWidth, 80));
        tabScroll.setMaximumSize(new Dimension(preferredWidth, 80));
        tabScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(tabScroll);

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }


    public JPanel rpiHelp() {
        Utility util = new Utility();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setSize(800, 560);

        // Main vertical layout container
        JPanel inner = buildTemplate();

        // Title
        JLabel title = new JLabel("RPI COMMAND CENTER HELP");
        setFullWidth.accept(title);
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea description = buildTextArea(inner, 300);
        description.setText("""
                Use this tab to manage core RPi processes like starting/stopping the listener, checking status, and syncing files with the host.


                LAYOUT:
                
                * This tab of the GUI contains 3 main parts: the built in commands, the manual command field, and the output viewer.
                
                * There are 4 built in commands located at the top of the RPi Command Center Tab to communicate directly with the RPi and a dropdown menu for specific commands to control the sensor. For more help with these, see the commands section of this help page.
                
                * The manual command field located at the bottom of the RPi Command Tab allows you to type in a command.
                
                * The output viewer located on the right side of the RPi Command Tab allows you to view...
                """);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // Commands section label
        JLabel commands = new JLabel("RPi Commands");
        setFullWidth.accept(commands);
        inner.add(commands);
        inner.add(Box.createRigidArea(new Dimension(0, 5)));

        JTextArea cmdBio = buildTextArea(inner, 30);
        cmdBio.setText("Click on the commands below to see what they do and how to use them:");
        inner.add(cmdBio);

        // Command descriptions
        String[] cmds = {"status", "start", "rsync", "kill"};
        JList<String> list = new JList<>(cmds);
        list.setPreferredSize(new Dimension(150, 100));
        JPanel cmdList = new JPanel(new BorderLayout());
        cmdList.add(list, BorderLayout.CENTER);

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);

        JPanel cmdDesc = new JPanel(new BorderLayout());
        cmdDesc.add(desc, BorderLayout.CENTER);

        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("status", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        descriptions.put("start", "Starts the RPi listener process.\n\nUse this command to ");
        descriptions.put("rsync", "Syncs data from RPi to host via rsync.\n\nUse this command to ");
        descriptions.put("kill", "Terminates the RPi listener process.");

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = list.getSelectedValue();
                desc.setText(descriptions.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane cmdPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cmdList, cmdDesc);
        cmdPanel.setResizeWeight(0.3);
        cmdPanel.setPreferredSize(new Dimension(preferredWidth, 120));
        cmdPanel.setMaximumSize(new Dimension(preferredWidth, 120));
        inner.add(cmdPanel);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel senCommands = new JLabel("Sensor Commands");
        setFullWidth.accept(senCommands);
        inner.add(senCommands);
        inner.add(Box.createRigidArea(new Dimension(0, 5)));

        JTextArea senCmdBio = buildTextArea(inner, 30);
        senCmdBio.setText("Click on the commands below to see what they do and how to use them:");
        inner.add(senCmdBio);

        // Sensor command descriptions
        JComboBox<String> sensorSelector = new JComboBox<>(new String[]{
                "Readings & Info",
                "Arm / Disarm Calibration",
                "Interval / Threshold",
                "Manual Calibration",
                "Data Logging Commands",
                "Logging Utilities"
        });
        inner.add(sensorSelector);
        setFullWidth.accept(sensorSelector);
        JSplitPane senCmdPanel = new JSplitPane();
        senCmdPanel.setResizeWeight(0.3);
        senCmdPanel.setPreferredSize(new Dimension(preferredWidth, 200));
        senCmdPanel.setMaximumSize(new Dimension(preferredWidth, 200));

        inner.add(senCmdPanel);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        String[] senCmdsR = {"Request Reading", "Calibration Info", "Unit Info"};
        JList<String> senListR = new JList<>(senCmdsR);
        senListR.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListR = new JPanel(new BorderLayout());
        senCmdListR.add(senListR, BorderLayout.CENTER);

        JTextArea senDescR = new JTextArea();
        senDescR.setEditable(false);
        senDescR.setLineWrap(true);
        senDescR.setWrapStyleWord(true);

        JPanel senCmdDescR = new JPanel(new BorderLayout());
        senCmdDescR.add(senDescR, BorderLayout.CENTER);

        Map<String, String> senDescriptionsR = new LinkedHashMap<>();
        senDescriptionsR.put("Request Reading", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsR.put("Calibration Info", "Starts the RPi listener process.\n\nUse this command to ");
        senDescriptionsR.put("Unit Info", "Syncs data from RPi to host via rsync.\n\nUse this command to ");

        senListR.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListR.getSelectedValue();
                senDescR.setText(senDescriptionsR.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelR = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListR, senCmdDescR);
        senCmdPanelR.setResizeWeight(0.3);
        senCmdPanelR.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelR.setMaximumSize(new Dimension(preferredWidth, 120));

        // ARM / DISARM CAL
        String[] senCmdsA = {"Arm Light", "Arm Dark", "Disarm"};
        JList<String> senListA = new JList<>(senCmdsA);
        senListA.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListA = new JPanel(new BorderLayout());
        senCmdListA.add(senListA, BorderLayout.CENTER);

        JTextArea senDescA = new JTextArea();
        senDescA.setEditable(false);
        senDescA.setLineWrap(true);
        senDescA.setWrapStyleWord(true);

        JPanel senCmdDescA = new JPanel(new BorderLayout());
        senCmdDescA.add(senDescA, BorderLayout.CENTER);

        Map<String, String> senDescriptionsA = new LinkedHashMap<>();
        senDescriptionsA.put("Arm Light", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsA.put("Arm Dark", "Starts the RPi listener process.\n\nUse this command to ");
        senDescriptionsA.put("Disarm", "Syncs data from RPi to host via rsync.\n\nUse this command to ");

        senListA.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListA.getSelectedValue();
                senDescA.setText(senDescriptionsA.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListA, senCmdDescA);
        senCmdPanelA.setResizeWeight(0.3);
        senCmdPanelA.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelA.setMaximumSize(new Dimension(preferredWidth, 120));


        // Interval / Threshold
        String[] senCmdsI = {"Request Interval Settiings", "Set Interval Period", "Set Interval Threshold"};
        JList<String> senListI = new JList<>(senCmdsI);
        senListI.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListI = new JPanel(new BorderLayout());
        senCmdListI.add(senListI, BorderLayout.CENTER);

        JTextArea senDescI = new JTextArea();
        senDescI.setEditable(false);
        senDescI.setLineWrap(true);
        senDescI.setWrapStyleWord(true);

        JPanel senCmdDescI = new JPanel(new BorderLayout());
        senCmdDescI.add(senDescI, BorderLayout.CENTER);

        Map<String, String> senDescriptionsI = new LinkedHashMap<>();
        senDescriptionsI.put("Request Interval Settings", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsI.put("Set Interval Period", "Starts the RPi listener process.\n\nUse this command to ");
        senDescriptionsI.put("Set Interval Threshold", "Syncs data from RPi to host via rsync.\n\nUse this command to ");

        senListI.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListI.getSelectedValue();
                senDescI.setText(senDescriptionsI.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelI = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListI, senCmdDescI);
        senCmdPanelI.setResizeWeight(0.3);
        senCmdPanelI.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelI.setMaximumSize(new Dimension(preferredWidth, 120));

        // Manual Calibration
        String[] senCmdsM = {"Set Light Offset", "Set Light Temp", "Set Dark Period", "Set Dark Temp"};
        JList<String> senListM = new JList<>(senCmdsM);
        senListM.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListM = new JPanel(new BorderLayout());
        senCmdListM.add(senListM, BorderLayout.CENTER);

        JTextArea senDescM = new JTextArea();
        senDescM.setEditable(false);
        senDescM.setLineWrap(true);
        senDescM.setWrapStyleWord(true);

        JPanel senCmdDescM = new JPanel(new BorderLayout());
        senCmdDescM.add(senDescM, BorderLayout.CENTER);

        Map<String, String> senDescriptionsM = new LinkedHashMap<>();
        senDescriptionsM.put("Set Light Offset", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsM.put("Set Light Temp", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsM.put("Set Dark Period", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsM.put("Set Dark Temp", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");


        senListM.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListM.getSelectedValue();
                senDescM.setText(senDescriptionsM.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelM = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListM, senCmdDescM);
        senCmdPanelM.setResizeWeight(0.3);
        senCmdPanelM.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelM.setMaximumSize(new Dimension(preferredWidth, 120));

        // Simulation
        String[] senCmdsS = {"Request Simulation Values", "Run Simulation"};
        JList<String> senListS = new JList<>(senCmdsS);
        senListS.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListS = new JPanel(new BorderLayout());
        senCmdListS.add(senListS, BorderLayout.CENTER);

        JTextArea senDescS = new JTextArea();
        senDescS.setEditable(false);
        senDescS.setLineWrap(true);
        senDescS.setWrapStyleWord(true);

        JPanel senCmdDescS = new JPanel(new BorderLayout());
        senCmdDescS.add(senDescS, BorderLayout.CENTER);

        Map<String, String> senDescriptionsS = new LinkedHashMap<>();
        senDescriptionsS.put("Request Simulation Values", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsS.put("Run Simulation", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        

        senListS.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListS.getSelectedValue();
                senDescS.setText(senDescriptionsS.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelS = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListS, senCmdDescS);
        senCmdPanelS.setResizeWeight(0.3);
        senCmdPanelS.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelS.setMaximumSize(new Dimension(preferredWidth, 120));

        // Data Logging Commands
        String[] senCmdsD = {"Request Pointer", "Log One Record", "Return One Record", "Set Trigger Mode", "Request Trigger Mode", "Request Interval Settings", "Set Interval Period", "Set Threshold"};
        JList<String> senListD = new JList<>(senCmdsD);
        senListD.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListD = new JPanel(new BorderLayout());
        senCmdListD.add(senListD, BorderLayout.CENTER);

        JTextArea senDescD = new JTextArea();
        senDescD.setEditable(false);
        senDescD.setLineWrap(true);
        senDescD.setWrapStyleWord(true);

        JPanel senCmdDescD = new JPanel(new BorderLayout());
        senCmdDescD.add(senDescD, BorderLayout.CENTER);

        Map<String, String> senDescriptionsD = new LinkedHashMap<>();
        senDescriptionsD.put("Request Pointer", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Log One Record", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Return One Record", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Set Trigger Mode", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Request Trigger Mode", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Request Interval Settings", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Set Interval Period", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsD.put("Set Threshold", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");


        senListD.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListD.getSelectedValue();
                senDescD.setText(senDescriptionsD.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelD = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListD, senCmdDescD);
        senCmdPanelD.setResizeWeight(0.3);
        senCmdPanelD.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelD.setMaximumSize(new Dimension(preferredWidth, 120));

        // Manual Calibration
        String[] senCmdsL = {"Set Light Offset", "Set Light Temp", "Set Dark Period", "Set Dark Temp"};
        JList<String> senListL = new JList<>(senCmdsL);
        senListL.setPreferredSize(new Dimension(150, 100));
        JPanel senCmdListL = new JPanel(new BorderLayout());
        senCmdListL.add(senListL, BorderLayout.CENTER);

        JTextArea senDescL = new JTextArea();
        senDescL.setEditable(false);
        senDescL.setLineWrap(true);
        senDescL.setWrapStyleWord(true);

        JPanel senCmdDescL = new JPanel(new BorderLayout());
        senCmdDescL.add(senDescL, BorderLayout.CENTER);

        Map<String, String> senDescriptionsL = new LinkedHashMap<>();
        senDescriptionsL.put("Set Light Offset", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsL.put("Set Light Temp", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsL.put("Set Dark Period", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");
        senDescriptionsL.put("Set Dark Temp", "Checks if the RPi listener is active.\n\nUse this command to see if the thread is running. If it’s not, use the start command to activate the listener.");


        senListL.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = senListL.getSelectedValue();
                senDescL.setText(senDescriptionsL.getOrDefault(selected, "No description available."));
            }
        });

        JSplitPane senCmdPanelL = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, senCmdListL, senCmdDescL);
        senCmdPanelL.setResizeWeight(0.3);
        senCmdPanelL.setPreferredSize(new Dimension(preferredWidth, 120));
        senCmdPanelL.setMaximumSize(new Dimension(preferredWidth, 120));
        

        sensorSelector.addActionListener(e -> {
            String selection = (String) sensorSelector.getSelectedItem();
            switch (selection) {
                case "Readings & Info" -> setSplitPane(senCmdPanel, senCmdPanelR);
                case "Arm / Disarm Calibration" -> setSplitPane(senCmdPanel, senCmdPanelA);
                case "Interval / Threshold" -> setSplitPane(senCmdPanel, senCmdPanelI);
                case "Manual Calibration" -> setSplitPane(senCmdPanel, senCmdPanelM);
                case "Simulation" -> setSplitPane(senCmdPanel, senCmdPanelS);
                case "Data Logging Commands" -> setSplitPane(senCmdPanel, senCmdPanelD);
                case "Logging Utilities" -> setSplitPane(senCmdPanel, senCmdPanelL);
                case null, default -> clearSplitPane(senCmdPanel);

            }
        });
        
        // Troubleshooting label
        JLabel troubleshoot = new JLabel("Troubleshooting");
        setFullWidth.accept(troubleshoot);
        inner.add(troubleshoot);

        JTextArea tblsht = buildTextArea(inner, 150);
        inner.add(tblsht);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }


    private JPanel settingHelp() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setSize(800, 560);

        JPanel inner = buildTemplate();

        // Title
        JLabel title = new JLabel("SETTINGS HELP");
        setFullWidth.accept(title);
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // Settings description
        JTextArea description = new JTextArea("""
                This tab allows you to add, delete, and edit known RPi profiles.
                Edit RPi identifiers and IP addresses using the text fields. Click 'Save Settings' to update and reload the backend configuration, or add/delete profiles using the 'Add' and 'Delete' buttons.
                """);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBackground(panel.getBackground());
        description.setPreferredSize(new Dimension(preferredWidth, 375));
        setFullWidth.accept(description);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel dataHelp(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setSize(800, 560);

        JPanel inner = buildTemplate();
        // Title
        JLabel title = new JLabel("DATA HELP");
        setFullWidth.accept(title);
        inner.add(title);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // Settings description
        JTextArea description = new JTextArea("""
                This tab allows you to access data synced from the RPi on you computer.
                Navigate your file manager using the built in file chooser and use the button on the top right that says "open in file explorer" to open that file location.
                """);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBackground(panel.getBackground());
        description.setPreferredSize(new Dimension(preferredWidth, 375));
        setFullWidth.accept(description);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }



    private void setPanel(JPanel panel) {
        helpPanel.removeAll();
        helpPanel.add(panel, BorderLayout.CENTER);
        helpPanel.revalidate();
        helpPanel.repaint();
    }
    private void clearRightPanel() {
        setPanel(new JPanel());
    }

    private void setSplitPane(JSplitPane parent, JSplitPane panel) {
        parent.removeAll();
        parent.add(panel);
        parent.revalidate();
        parent.repaint();
    }
    private void clearSplitPane(JSplitPane parent) {
        setSplitPane(parent, new JSplitPane());
    }
}
