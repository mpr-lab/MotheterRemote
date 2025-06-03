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
public class HelpTab extends JFrame {
    private JPanel mainPanel;
    private JPanel helpPanel = new JPanel();


    //    public HelpTab() {
//        mainPanel = buildMain();
//    }
    public HelpTab(){
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 560);
        setLayout(new BorderLayout());

//        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> helpSelector = new JComboBox<>(new String[]{
                "General Help",
                "RPi Command Center Help",
                "Sensor Command Center Help",
                "Data Help",
                "Settings Help"
        });
        //        JTextArea helpText = new JTextArea();
//        inner.setEditable(false);
//        inner.setLineWrap(true);
//        helpText.setWrapStyleWord(true);
//        JScrollPane scroll = new JScrollPane(helpText);

        helpSelector.addActionListener(e -> {
            String selection = (String) helpSelector.getSelectedItem();
//            helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");

            switch (selection) {
                case "General Help" -> setPanel(genHelp());
                case "RPi Command Center Help" -> setPanel(rpiHelp());
                case "Sensor Command Center Help" -> setPanel(sensorHelp());
                case "Data Help" -> setPanel(dataHelp());
                case "Settings Help" -> setPanel(settingsHelp());
            }
        });

        add(helpSelector, BorderLayout.NORTH);
        add(helpPanel, BorderLayout.CENTER);
    }
//    private JPanel buildMain() {
//        JPanel root = new JPanel(new BorderLayout(10, 10));
//        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        JComboBox<String> helpSelector = new JComboBox<>(new String[]{
//                "General Help",
//                "RPi Command Center Help",
//                "Sensor Command Center Help",
//                "Data Help",
//                "Settings Help"
//        });
//
//        JPanel inner = new JPanel();
////        JTextArea helpText = new JTextArea();
////        helpText.setEditable(false);
////        helpText.setLineWrap(true);
////        helpText.setWrapStyleWord(true);
////        JScrollPane scroll = new JScrollPane(helpText);
//
//        helpSelector.addActionListener(e -> {
//            String selection = (String) helpSelector.getSelectedItem();
////            helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
//
//            switch (selection) {
//                case "General Help" -> inner.add(genHelp());
//                case "RPi Command Center Help" -> inner.add(rpiHelp());
//                case "Sensor Command Center Help" -> inner.add(sensorHelp());
//                case "Data Help" -> inner.add(dataHelp());
//                case "Settings Help" -> inner.add(settingsHelp());
//            }
//        });
//
//        root.add(helpSelector, BorderLayout.NORTH);
//        root.add(inner, BorderLayout.CENTER);
//        return root;
//    }

    private JPanel genHelp(){
        setSize(800, 560);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        panel.add(description, BorderLayout.NORTH);
        panel.add(new JLabel("General Help:"));

        return panel;
    }

    private JPanel rpiHelp(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setSize(800, 560);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        panel.add(description, BorderLayout.NORTH);
        panel.add(new JLabel("RPi Help:"));

        return panel;
    }

    private JPanel sensorHelp(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setSize(800, 560);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        panel.add(description, BorderLayout.NORTH);
        panel.add(new JLabel("Sensor Help:"));

        return panel;
    }

    private JPanel dataHelp(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setSize(800, 560);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        panel.add(description, BorderLayout.NORTH);
        panel.add(new JLabel("Data Help:"));

        return panel;
    }

    private JPanel settingsHelp(){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setSize(800, 560);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea description = new JTextArea("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        panel.add(description, BorderLayout.NORTH);
        panel.add(new JLabel("Settings Help:"));

        return panel;
    }

    private void setPanel(JPanel panel) {
        helpPanel.removeAll();
        helpPanel.add(panel, BorderLayout.CENTER);
        helpPanel.revalidate();
        helpPanel.repaint();
    }

    private void clearPanel() {
        setPanel(new JPanel());
    }


    public static void main(String[] args){
        HelpTab tab = new HelpTab();
        tab.setVisible(true);

//        tab.setVisible(true);
    }

//
//    @Override
//    public JPanel getMainPanel() {
//        return mainPanel;
//    }
//
//    @Override
//    public JPanel getSidePanel() {
//        return null;
//    }

}