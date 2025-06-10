import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.LinkedHashMap;

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
                "Sensor Command Center Help",
                "Data Help",
                "Settings Help"
        });

        JScrollPane scroll = new JScrollPane(helpPanel);

        helpSelector.addActionListener(e -> {
            String selection = (String) helpSelector.getSelectedItem();
            switch (selection) {
                case "General Help" -> setPanel(genHelp());
                case "RPi Command Center Help" -> setPanel(rpiHelp());
//                case "Sensor Command Center Help" -> helpText.setText("This tab provides an interface for issuing sensor-specific commands. Commands are grouped into categories, and some require additional user input shown in the right panel.");
//                case "Data Help" -> helpText.setText("Use this tab to view downloaded sensor data. You can refresh the file list and open the data directory directly.");
                case "Settings Help" -> setPanel(settingHelp());
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
    java.util.function.Consumer<JComponent> setFullWidth = comp -> {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d = comp.getPreferredSize();
        d.width = preferredWidth;
        comp.setMaximumSize(d);
    };

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
        JTextArea description = new JTextArea("""
                This GUI allows interaction with a Raspberry Pi and sensor system.
                It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.


                LAYOUT:
                
                * The overall GUI contains 4 main sections: The top dropdown menu, the tabs section, the main panel, and the console log.
                    1) The top dropdown menu located at the very top of the GUI allows you to change which raspberry pi you want to connect to. This feature if useful if you setup multiple RPi profiles during setup.
                    2) The tabs section located directly below the top dropdown menu allows you to switch what functions are shown on the main panel. Each of the 5 tabs have different features, more support for each tab can be found here in the help tab.
                    3) The main panel which takes up the majority of the GUI displays the contents of the GUI which will change depending on what tab is selected.
                    4) The console log can be minimized and restored. Located at the bottom of the GUI, the console log can be viewed from every tab and allows you to see...
                """);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBackground(panel.getBackground());
        description.setPreferredSize(new Dimension(preferredWidth, 300));
        setFullWidth.accept(description);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // "How to Use" label
        JLabel howTO = new JLabel("How To Use:");
        setFullWidth.accept(howTO);
        inner.add(howTO);
        inner.add(Box.createRigidArea(new Dimension(0, 5)));

        // Instructional text
        JTextArea h2 = new JTextArea("The GUI uses tabbed panels to access different commands/functions. These tabs are as follows:");
        h2.setEditable(false);
        h2.setLineWrap(true);
        h2.setWrapStyleWord(true);
        h2.setBackground(panel.getBackground());
        h2.setPreferredSize(new Dimension(preferredWidth, 60));
        setFullWidth.accept(h2);
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

        // General Description
        JTextArea description = new JTextArea("""
                Use this tab to manage core RPi processes like starting/stopping the listener, checking status, and syncing files with the host.


                LAYOUT:
                
                * This tab of the GUI contains 3 main parts: the built in commands, the manual command field, and the output viewer.
                * There are 4 built in commands located at the top of the RPi Command Center Tab, for more help with these, see the commands section of this help page.
                * The manual command field located at the bottom of the RPi Command Tab allows you to type in a command.
                * The output viewer located on the right side of the RPi Command Tab allows you to view...""");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBackground(panel.getBackground()); // blend with background
        description.setPreferredSize(new Dimension(500, 250));
        setFullWidth.accept(description);
        inner.add(description);
        inner.add(Box.createRigidArea(new Dimension(0, 10)));

        // Commands section label
        JLabel commands = new JLabel("Commands");
        setFullWidth.accept(commands);
        inner.add(commands);
        inner.add(Box.createRigidArea(new Dimension(0, 5)));
        JTextArea cmdBio = new JTextArea("Click on the commands below to see what they do and how to use them:");
        cmdBio.setEditable(false);
        cmdBio.setLineWrap(true);
        cmdBio.setWrapStyleWord(true);
        cmdBio.setBackground(panel.getBackground()); // blend with background
        cmdBio.setPreferredSize(new Dimension(500, 30));
        setFullWidth.accept(cmdBio);
        inner.add(cmdBio);

        // Command descriptions
        String[] cmds = {"status", "start", "rsync", "kill"};
        JList<String> list = new JList<>(cmds);
        list.setPreferredSize(new Dimension(150, 80));
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
        descriptions.put("start", "Starts the RPi listener process.");
        descriptions.put("rsync", "Syncs data from RPi to host via rsync.");
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

        // Troubleshooting label
        JLabel troubleshoot = new JLabel("Troubleshooting");
        setFullWidth.accept(troubleshoot);
        inner.add(troubleshoot);

        panel.add(inner, BorderLayout.CENTER);
        return panel;
    }

    public JPanel settingHelp(){
        JPanel panel = new JPanel();
        panel.setSize(800, 560);
        panel.setLayout(new BorderLayout());

        JPanel inner = new JPanel(new GridLayout(4,1));

        JLabel title = new JLabel("SETTINGS HELP");
        title.setPreferredSize(new Dimension(500, 20));  // optional: to give the title a fixed size

        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea();
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        description.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");

        JLabel howTO = new JLabel("How To Use:");
        JTextArea h2 = new JTextArea();
        h2.setEditable(false);
        h2.setLineWrap(true);
        h2.setWrapStyleWord(true);

        h2.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");


        inner.add(title);
        inner.add(description);
        inner.add(howTO);
        inner.add(h2);

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
}
