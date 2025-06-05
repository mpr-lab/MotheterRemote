import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Supplier;   // the lambda-returning-string type
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

//        JTextArea helpText = new JTextArea();
//        helpText.setEditable(false);
//        helpText.setLineWrap(true);
//        helpText.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(helpPanel);

        helpSelector.addActionListener(e -> {
            String selection = (String) helpSelector.getSelectedItem();

//            helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");

//            switch (selection) {
//                case "General Help" -> helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
//                case "RPi Command Center Help" -> helpText.setText("Use this tab to manage core RPi processes like starting/stopping the listener, checking status, and syncing files with the host.");
//                case "Sensor Command Center Help" -> helpText.setText("This tab provides an interface for issuing sensor-specific commands. Commands are grouped into categories, and some require additional user input shown in the right panel.");
//                case "Data Help" -> helpText.setText("Use this tab to view downloaded sensor data. You can refresh the file list and open the data directory directly.");
//                case "Settings Help" -> helpText.setText("Edit host and RPi identifiers and IP addresses here. Click 'Save Settings' to update and reload the backend configuration.");
//            }
            switch (selection) {
                case "General Help" -> setPanel(genHelp());
                case "RPi Command Center Help" -> setPanel(rpiHelp());
//                case "Sensor Command Center Help" -> helpText.setText("This tab provides an interface for issuing sensor-specific commands. Commands are grouped into categories, and some require additional user input shown in the right panel.");
//                case "Data Help" -> helpText.setText("Use this tab to view downloaded sensor data. You can refresh the file list and open the data directory directly.");
                case "Settings Help" -> setPanel(settingHelp());
            }
        });

        panel.add(helpSelector, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);
    }

    private JPanel genHelp(){
        JPanel panel = new JPanel();
        panel.setSize(800, 560);
        panel.setLayout(new BorderLayout());

        JPanel inner = new JPanel(new GridLayout(4,1));

        JLabel title = new JLabel("GENERAL HELP");
        title.setPreferredSize(new Dimension(650, 30));  // optional: to give the title a fixed size

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

    public JPanel rpiHelp(){
        JPanel panel = new JPanel();
        panel.setSize(800, 560);
        panel.setLayout(new BorderLayout());

        JPanel inner = new JPanel(new GridLayout(5,1));

        JLabel title = new JLabel("RPI COMMAND CENTER HELP");
        title.setPreferredSize(new Dimension(650, 30));  // optional: to give the title a fixed size

        title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea();
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        description.setText("Use this tab to manage core RPi processes like starting/stopping the listener, checking status, and syncing files with the host.");

        JLabel commands = new JLabel("Commands");
//        JPanel cmdList = new JPanel(new GridLayout(4,1));
//        JPanel cmdDesc = new JPanel();
//
        JTextArea cmdHelp = new JTextArea();
        cmdHelp.setEditable(false);
        cmdHelp.setLineWrap(true);
        cmdHelp.setWrapStyleWord(true);
        cmdHelp.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");

        JLabel troubleshoot = new JLabel("Troubleshooting");

        inner.add(title);
        inner.add(description);
        inner.add(commands);
        inner.add(cmdHelp);
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
        title.setPreferredSize(new Dimension(650, 30));  // optional: to give the title a fixed size

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
