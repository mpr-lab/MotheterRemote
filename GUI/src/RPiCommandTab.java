import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class RPiCommandTab extends JPanel {
    private final Utility util;
    private JTextArea  CONSOLE;      // running log / output
    public JPanel rightPanel = new JPanel(new BorderLayout());

    public RPiCommandTab(Utility util) {
        this.util = util;
//        setConfigs(Console);
//        Utility util = new Utility(Console);

        setSize(800, 560);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[][] presets = {
                {"status", "Check the current process status"},
                {"start",  "Start the main data collection process"},
                {"rsync",  "Synchronize files from Pi to host"},
                {"kill",   "Kill the running process on the Pi"}
//                {"ui",     "Shows and allows user to run available commands built in to the sensor"},
//                {"help",   "List available commands"}
        };

        // Setup for row of common commands
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String[] p : presets) {
            JButton b = new JButton(p[0]);
            b.setToolTipText(p[1]);  // Set tooltip
            b.addActionListener(e -> util.sendCommand(p[0]));
            btnRow.add(b);
        }

        panel.add(btnRow, BorderLayout.NORTH);

        class Cmd {
            String label, tip;
            Runnable cmd;
            Cmd(String l, String t, Runnable c) {
                label = l;
                tip = t;
                cmd = c;
            }
        }

        Map<String, java.util.List<Cmd>> cat = new LinkedHashMap<>();

        /* 1) READINGS & INFO */
        cat.put("Readings & Info", java.util.List.of(
                new Cmd("Request Reading", "requests a reading", () -> util.sendCommand("rx")),
                new Cmd("Calibration Info", "requests calibration information", () -> util.sendCommand("cx")),
                new Cmd("Unit Info", "requests unit information", () -> util.sendCommand("ix"))
        ));

        /* 2) ARM / DISARM CAL */
        cat.put("Arm / Disarm Calibration", java.util.List.of(
                new Cmd("Arm Light", "zcalAx", () -> util.sendCommand("zcalAx")),
                new Cmd("Arm Dark", "zcalBx", () -> util.sendCommand("zcalBx")),
                new Cmd("Disarm", "zcalDx", () -> util.sendCommand("zcalDx"))
        ));

        /* 3) Interval / Threshold */
        cat.put("Interval / Threshold", java.util.List.of(
                new Cmd("Request Interval Settings", "Ix", () -> util.sendCommand("Ix")),
                new Cmd("Set Interval Period", "", this::promptIntervalPeriod),
                new Cmd("Set Interval Threshold", "", this::promptIntervalThreshold)
        ));

        /* 4) Manual Calibration */
        cat.put("Manual Calibration", java.util.List.of(
                new Cmd("Set Light Offset", "manual light offset", this::promptLightOffset),
                new Cmd("Set Light Temp", "manual light temperature", this::promptLightTemp),
                new Cmd("Set Dark Period", "manual dark period", this::promptDarkPeriod),
                new Cmd("Set Dark Temp", "manual dark temperature", this::promptDarkTemp)
        ));

        /* 5) Simulation */
        cat.put("Simulation", java.util.List.of(
                new Cmd("Request Sim Values", "sx", () -> util.sendCommand("sx")),
                new Cmd("Run Simulation", "runs simulation", this::promptSimulation)
        ));

        /* 6) Data Logging Commands */
        cat.put("Data Logging Cmds", java.util.List.of(
                new Cmd("Request Pointer", "L1x", () -> util.sendCommand("L1x")),
                new Cmd("Log One Record", "L3x", () -> util.sendCommand("L3x")),
                new Cmd("Return One Record", "L4x", this::promptReturnOneRecord),
                new Cmd("Set Trigger Mode", "LMx", this::promptTriggerMode),
                new Cmd("Request Trigger Mode", "Lmx", () -> util.sendCommand("Lmx")),
                new Cmd("Request Interval Settings", "LIx", () -> util.sendCommand("LIx")),
                new Cmd("Set Interval Period", "LPx", this::promptLogIntervalPeriod),
                new Cmd("Set Threshold", "LPTx", this::promptLogThreshold)
        ));

        /* 7) Logging Utilities */
        cat.put("Logging Utilities", List.of(
                new Cmd("Request ID", "L0x", () -> util.sendCommand("L0x")),
                new Cmd("Erase Flash Chip", "L2x", this::confirmEraseFlash),  // update this if you also convert it to panel
                new Cmd("Battery Voltage", "L5x", () -> util.sendCommand("L5x")),
                new Cmd("Request Clock", "Lcx", () -> util.sendCommand("Lcx")),
                new Cmd("Set Clock", "Lcx", this::promptSetClock),
                new Cmd("Put Unit to Sleep", "Lsx", () -> util.sendCommand("Lsx")),
                new Cmd("Request Alarm Data", "Lax", () -> util.sendCommand("Lax"))
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

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(combo, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(listPanel), BorderLayout.CENTER);


        JButton sendBtn = new JButton("Send");
        JTextField cmdField = new JTextField();     // raw command entry

        sendBtn.addActionListener(e -> util.sendCommand(cmdField.getText()));
        cmdField.addActionListener(e -> sendBtn.doClick());

//        JPanel commandRightPanel = new JPanel(new BorderLayout());
//        JTextArea commandOutputArea = new JTextArea();

//        commandRightPanel.setPreferredSize(new Dimension(300, 0));
//        commandRightPanel.setBorder(BorderFactory.createTitledBorder("Backend Output"));
//        commandOutputArea.setEditable(false);
//        commandOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
//        commandRightPanel.add(new JScrollPane(commandOutputArea), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(300, 0));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Command Input"));

        add(util.wrapWithRightPanel(mainPanel, rightPanel));

        // Add elements to GUI Panel
        JPanel south = new JPanel(new BorderLayout(3,3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn,  BorderLayout.EAST);

        panel.add(mainPanel, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        add(util.wrapWithRightPanel(panel, rightPanel));
    }
    private void setRightPanel(JPanel panel) {
        rightPanel.removeAll();
        rightPanel.add(panel, BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void clearRightPanel() {
        setRightPanel(new JPanel());
    }













    /* -------------------------------------------------------------------
     * Helper dialogs for commands that need extra user input.
     *
     * Each method sneds the fully‑formatted command string, or clears the
     * input panel if the operation was cancelled / invalid.
     * ---------------------------------------------------------------- */
    private void promptIntervalPeriod() {
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("p" + withZeros + "x");
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid number");
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
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("p" + cmdPart + "x");
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptReturnOneRecord() {
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("L4" + ptr + "x");
                clearRightPanel();
            } else {
                util.append("[Error] Invalid pointer");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLightOffset() {                              // zcal5<value>x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLightTemp() {                                //zcal6<value>x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptDarkPeriod() {                               //zcal7<value>x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptDarkTemp() {                                 //zcal8<value>x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSimulation() {                               // S,count.freq,temp x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("S," + sc + "," + sf + "," + st + "x");
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptTriggerMode() {                              // LM<mode>x
        Utility util = new Utility(CONSOLE);

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
            util.sendCommand("LM" + m + "x");
            clearRightPanel();
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLogIntervalPeriod() {                        // LP[S|M]<value>x
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand(cmd);
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
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
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("LPT" + String.format("%08.2f", value).replace(' ', '0') + "x");
                clearRightPanel();
            } catch (Exception ex) {
                util.append("[Error] Invalid input");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSetClock() {                                 // LcYYYY-MM-DD w HH:MM:SSx
        Utility util = new Utility(CONSOLE);

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
                util.sendCommand("Lc" + formatted + "x");
                clearRightPanel();
            } else {
                util.append("[Error] Invalid date/time");
            }
        });

        cancel.addActionListener(e -> clearRightPanel());

        panel.add(inner, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private String confirmEraseFlash(){
        int res = JOptionPane.showConfirmDialog(this,
                "ERASE FLASH CHIP?\nThis cannot be undone.","Confirm",JOptionPane.OK_CANCEL_OPTION);
        return res==JOptionPane.OK_OPTION ? "L2x" : null;
    }



}
