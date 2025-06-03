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

public class SensorCommandTab implements GUITab {
    private JPanel mainPanel;
    private JPanel sidePanel;

    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    // Path to Python‑side configuration file (relative to project root)
    private static final String CONFIG_PATH  = "../comms-GUI/configs.py";
    // Path to the Python backend we invoke with ProcessBuilder
    private static final String BACKEND_PATH = "../comms-GUI/host_to_client.py";
    // Folder on host where rsync‑ed data will be stored
    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");

    /* ---------------- Network ---------------- */
    private String HOST;   // server IP or hostname (user‑supplied)
    private String NAME;   // human‑friendly host name (user‑supplied)
    // Socket port must match configs.host_server in the Python backend
    private static final int PORT = 12345;

    /* ---------------- Swing widgets ---------------- */
    private final JTextArea  console  = new JTextArea();      // running log / output
    private final JTextField cmdField = new JTextField();     // raw command entry
    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab

    private final JPanel sensorRightPanel = new JPanel(new BorderLayout());
    private final JPanel commandRightPanel = new JPanel(new BorderLayout());
    private final JTextArea commandOutputArea = new JTextArea();

    /* ----  CONSTRUCTOR  --------------------------------------------------------------------------------------------------------------------------------------- */
    private final JLabel statusLabel = new JLabel("Status: Initializing...");


    public SensorCommandTab() {
        mainPanel = buildMain();
        sidePanel = buildSide();
    }

    private JPanel buildMain() {
        /*
         * We build a Map<Category name, List<Cmd>> where each Cmd bundles
         * a button label, tooltip, and a Supplier<String> that returns the
         * actual command to send.  Most commands are fixed strings, but
         * those requiring user input pop up a dialog and may return null
         * (indicating the dialog was cancelled or the input invalid).
         */        class Cmd {
            String label, tip;
            Runnable cmd;
            Cmd(String l, String t, Runnable c) {
                label = l;
                tip = t;
                cmd = c;
            }
        }

        Map<String, List<Cmd>> cat = new LinkedHashMap<>();

        /* 1) READINGS & INFO */
        cat.put("Readings & Info", List.of(
                new Cmd("Request Reading", "requests a reading", () -> Utility.sendCommand("rx")),
                new Cmd("Calibration Info", "requests calibration information", () -> Utility.sendCommand("cx")),
                new Cmd("Unit Info", "requests unit information", () -> Utility.sendCommand("ix"))
        ));

        /* 2) ARM / DISARM CAL */
        cat.put("Arm / Disarm Calibration", List.of(
                new Cmd("Arm Light", "zcalAx", () -> Utility.sendCommand("zcalAx")),
                new Cmd("Arm Dark", "zcalBx", () -> Utility.sendCommand("zcalBx")),
                new Cmd("Disarm", "zcalDx", () -> Utility.sendCommand("zcalDx"))
        ));

        /* 3) Interval / Threshold */
        cat.put("Interval / Threshold", List.of(
                new Cmd("Request Interval Settings", "Ix", () -> Utility.sendCommand("Ix")),
                new Cmd("Set Interval Period", "", this::promptIntervalPeriod),
                new Cmd("Set Interval Threshold", "", this::promptIntervalThreshold)
        ));

        /* 4) Manual Calibration */
        cat.put("Manual Calibration", List.of(
                new Cmd("Set Light Offset", "manual light offset", this::promptLightOffset),
                new Cmd("Set Light Temp", "manual light temperature", this::promptLightTemp),
                new Cmd("Set Dark Period", "manual dark period", this::promptDarkPeriod),
                new Cmd("Set Dark Temp", "manual dark temperature", this::promptDarkTemp)
        ));

        /* 5) Simulation */
        cat.put("Simulation", List.of(
                new Cmd("Request Sim Values", "sx", () -> Utility.sendCommand("sx")),
                new Cmd("Run Simulation", "runs simulation", this::promptSimulation)
        ));

        /* 6) Data Logging Commands */
        cat.put("Data Logging Cmds", List.of(
                new Cmd("Request Pointer", "L1x", () -> Utility.sendCommand("L1x")),
                new Cmd("Log One Record", "L3x", () -> Utility.sendCommand("L3x")),
                new Cmd("Return One Record", "L4x", this::promptReturnOneRecord),
                new Cmd("Set Trigger Mode", "LMx", this::promptTriggerMode),
                new Cmd("Request Trigger Mode", "Lmx", () -> Utility.sendCommand("Lmx")),
                new Cmd("Request Interval Settings", "LIx", () -> Utility.sendCommand("LIx")),
                new Cmd("Set Interval Period", "LPx", this::promptLogIntervalPeriod),
                new Cmd("Set Threshold", "LPTx", this::promptLogThreshold)
        ));

        /* 7) Logging Utilities */
        cat.put("Logging Utilities", List.of(
                new Cmd("Request ID", "L0x", () -> Utility.sendCommand("L0x")),
//                new Cmd("Erase Flash Chip", "L2x", this::confirmEraseFlash),  // update this if you also convert it to panel
                new Cmd("Battery Voltage", "L5x", () -> Utility.sendCommand("L5x")),
                new Cmd("Request Clock", "Lcx", () -> Utility.sendCommand("Lcx")),
                new Cmd("Set Clock", "Lcx", this::promptSetClock),
                new Cmd("Put Unit to Sleep", "Lsx", () -> Utility.sendCommand("Lsx")),
                new Cmd("Request Alarm Data", "Lax", () -> Utility.sendCommand("Lax"))
        ));

        /* === GUI BUILD === */
        JComboBox<String> combo = new JComboBox<>(cat.keySet().toArray(new String[0]));
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Runnable refresh = () -> {
            listPanel.removeAll();
            String key = (String) combo.getSelectedItem();
            for (Cmd c : cat.get(key)) {
                JButton b = new JButton(c.label);
                b.setToolTipText(c.tip);
                b.addActionListener(e -> c.cmd.run());
                listPanel.add(b);
            }
            listPanel.revalidate();
            listPanel.repaint();
        };

        combo.addActionListener(e -> refresh.run());
        refresh.run(); // initial population

        JPanel root = new JPanel(new BorderLayout(5, 5));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(combo, BorderLayout.NORTH);
        root.add(new JScrollPane(listPanel), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSide() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Command Input"));
        return panel;
    }

    


    private void setRightPanel(JPanel panel) {
        sensorRightPanel.removeAll();
        sensorRightPanel.add(panel, BorderLayout.CENTER);
        sensorRightPanel.revalidate();
        sensorRightPanel.repaint();
    }

    private void clearRightPanel() {
        setRightPanel(new JPanel());
    }

    private void appendToCommandRightPanel(String txt) {
        SwingUtilities.invokeLater(() -> {
            commandOutputArea.append(txt + "\n");
            commandOutputArea.setCaretPosition(commandOutputArea.getDocument().getLength());
        });
    }
    /* -------------------------------------------------------------------
     * Helper dialogs for commands that need extra user input.
     *
     * Each method sneds the fully‑formatted command string, or clears the
     * input panel if the operation was cancelled / invalid.
     * ---------------------------------------------------------------- */

    private void promptIntervalPeriod() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(4, 1, 5, 5));
        JLabel unitLabel = new JLabel("Select unit:");
        JComboBox<String> unitBox = new JComboBox<>(new String[]{"Seconds", "Minutes", "Hours"});
        JLabel valueLabel = new JLabel("Enter value:");
        JTextField valueField = new JTextField();

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                int t = Integer.parseInt(valueField.getText().trim());
                long seconds = switch(unitBox.getSelectedIndex()) {
                    case 1 -> t * 60;               // minutes → seconds
                    case 2 -> t * 3600;             // hours   → seconds
                    default -> t;
                };
                String withZeros = String.format("%010d", seconds);
                Utility.sendCommand("p" + withZeros + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid number");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        inner.add(unitLabel);
        inner.add(unitBox);
        inner.add(valueLabel);
        inner.add(valueField);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }


    private void promptIntervalThreshold() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Threshold (mag/arcsec²):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double d = Double.parseDouble(field.getText().trim());
                String cmdPart = String.format("%08.2f", d).replace(' ', '0');
                Utility.sendCommand("p" + cmdPart + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptReturnOneRecord() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Record pointer (0-9999999999):"));
        JTextField ptrField = new JTextField();
        inner.add(ptrField);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            String ptr = ptrField.getText().trim();
            if (ptr.matches("\\d{1,10}")) {
                ptr = String.format("%010d", Long.parseLong(ptr));
                Utility.sendCommand("L4" + ptr + "x");
                clearRightPanel();
            } else {
                append("[Error] Invalid pointer");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLightOffset() {                              // zcal5<value>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Light offset (mag/arcsec²):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double value = Double.parseDouble(field.getText().trim());
                String cmd = "zcal5" + String.format("%08.2f", value).replace(' ', '0') + "x";
                Utility.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLightTemp() {                                //zcal6<value>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Light temperature (°C):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double value = Double.parseDouble(field.getText().trim());
                String cmd = "zcal6" + String.format("%03.1f", value).replace(' ', '0') + "x";
                Utility.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptDarkPeriod() {                               //zcal7<value>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Dark period (s):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double value = Double.parseDouble(field.getText().trim());
                String cmd = "zcal7" + String.format("%07.3f", value).replace(' ', '0') + "x";
                Utility.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptDarkTemp() {                                 //zcal8<value>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Dark temperature (°C):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double value = Double.parseDouble(field.getText().trim());
                String cmd = "zcal8" + String.format("%03.1f", value).replace(' ', '0') + "x";
                Utility.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSimulation() {                               // S,count.freq,temp x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField countsField = new JTextField();
        JTextField freqField = new JTextField();
        JTextField tempField = new JTextField();

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        inner.add(new JLabel("Counts:"));
        inner.add(countsField);
        inner.add(new JLabel("Frequency (Hz):"));
        inner.add(freqField);
        inner.add(new JLabel("Temperature (°C):"));
        inner.add(tempField);

        submit.addActionListener(e -> {
            try {
                long c = Long.parseLong(countsField.getText().trim());
                long f = Long.parseLong(freqField.getText().trim());
                int t = (int) Double.parseDouble(tempField.getText().trim());
                String sc = String.format("%010d", c);
                String sf = String.format("%010d", f);
                String st = String.format("%010d", t);
                Utility.sendCommand("S," + sc + "," + sf + "," + st + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptTriggerMode() {                              // LM<mode>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Select trigger mode (0–7):"));
        JComboBox<String> modeBox = new JComboBox<>(new String[]{"0", "1", "2", "3", "4", "5", "6", "7"});
        inner.add(modeBox);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            String m = (String) modeBox.getSelectedItem();
            Utility.sendCommand("LM" + m + "x");
            clearRightPanel();
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLogIntervalPeriod() {                        // LP[S|M]<value>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(4, 1, 5, 5));
        JLabel unitLabel = new JLabel("Select unit:");
        JComboBox<String> unitBox = new JComboBox<>(new String[]{"Seconds", "Minutes"});
        JLabel valueLabel = new JLabel("Enter value:");
        JTextField valueField = new JTextField();

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                int v = Integer.parseInt(valueField.getText().trim());
                String zeros = String.format("%05d", v);
                String cmd = (unitBox.getSelectedIndex() == 0 ? "LPS" : "LPM") + zeros + "x";
                Utility.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        inner.add(unitLabel);
        inner.add(unitBox);
        inner.add(valueLabel);
        inner.add(valueField);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLogThreshold() {                             // LPT<threshold>x
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(2, 1, 5, 5));
        inner.add(new JLabel("Threshold (mag/arcsec²):"));
        JTextField field = new JTextField();
        inner.add(field);

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        submit.addActionListener(e -> {
            try {
                double value = Double.parseDouble(field.getText().trim());
                Utility.sendCommand("LPT" + String.format("%08.2f", value).replace(' ', '0') + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSetClock() {                                 // LcYYYY-MM-DD w HH:MM:SSx
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel inner = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField dateField = new JTextField();                    // yyyyMMdd
        JTextField timeField = new JTextField();                    // HHmmss

        FlowLayout layout = new FlowLayout();
        JPanel btnRow = new JPanel();
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");

        btnRow.setLayout(layout);
        btnRow.add(submit);
        btnRow.add(cancel);

        inner.add(new JLabel("Date (YYYYMMDD):")); inner.add(dateField);
        inner.add(new JLabel("Time (HHMMSS):")); inner.add(timeField);

        submit.addActionListener(e -> {
            String d = dateField.getText().trim();
            String t = timeField.getText().trim();
            if (d.matches("\\d{8}") && t.matches("\\d{6}")) {
                String formatted = d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6) +
                        " 0 " + t.substring(0, 2) + ":" + t.substring(2, 4) + ":" + t.substring(4);
                Utility.sendCommand("Lc" + formatted + "x");
                clearRightPanel();
            } else {
                append("[Error] Invalid date/time");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

//    private String confirmEraseFlash(){
//        int res = JOptionPane.showConfirmDialog(this,
//                "ERASE FLASH CHIP?\nThis cannot be undone.","Confirm",JOptionPane.OK_CANCEL_OPTION);
//        return res==JOptionPane.OK_OPTION ? "L2x" : null;
//    }


    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public JPanel getSidePanel() {
        return sidePanel;
    }
}
