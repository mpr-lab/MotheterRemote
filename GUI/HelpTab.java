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
    private JLabel helpTitle = new JLabel();
    private JTextArea helpText = new JTextArea();


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

        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane();

//        scroll.add(helpTitle);
        scroll.add(helpText);

        helpSelector.addActionListener(e -> {
            String selection = (String) helpSelector.getSelectedItem();
            helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");

            switch (selection) {
                case "General Help" -> genHelp();
                case "RPi Command Center Help" -> rpiHelp();
                case "Sensor Command Center Help" -> sensorHelp();
                case "Data Help" -> dataHelp();
                case "Settings Help" -> settingsHelp();
            }
        });

        add(helpSelector, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
//        add(helpPanel, BorderLayout.CENTER);
    }
//    private JPanel buildMain() {
//        JPanel root = new JPanel(new BorderLayout(5, 5));
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

    private JPanel template(String title, String dStr){
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel(new GridLayout(3, 1, 1, 1));
        JScrollPane scroll = new JScrollPane(inner);

        JTextArea description = new JTextArea(dStr);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        inner.add(description);
        inner.add(new JLabel("Command Help"));
        inner.add(new JLabel("Troubleshooting:"));

        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }
    private void genHelp(){
        helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        helpTitle.setText("General Help");
//        return template("General Help", "This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
    }

    private void rpiHelp(){
        helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        helpTitle.setText("RPi Help");
//        return template("RPi Command Center Help", "Use this tab to manage core RPi processes like starting/stopping the listener, checking status, and syncing files with the host.");
    }

    private void sensorHelp(){
        helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        helpTitle.setText("Sensor Help");

    }

    private void dataHelp(){
        helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        helpTitle.setText("Data Help");

    }

    private void settingsHelp(){
        helpText.setText("This GUI allows interaction with a Raspberry Pi and sensor system. It includes tabs for sending commands, syncing data, configuring settings, and monitoring backend responses.");
        helpTitle.setText("Settings Help");

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