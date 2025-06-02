/*
 * piCommandGUI.java
 * ------------------------------------------------------------
 * A Swing-based Java GUI for interacting with a RaspberryPi‑hosted
 * sky‑quality meter (SQM) system. The GUI communicates with a local
 * Python backend (host_to_client.py) over a TCP socket and exposes a
 * variety of commands for both the RaspberryPi and the SQM sensor
 * firmware. This heavily‑commented version is intended as a teaching
 * aid: each class member and logic block is annotated to explain its
 * purpose, assumptions, and side effects.
 *
 * Date: 2025‑06‑02
 * ------------------------------------------------------------------
 */

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

public class piCommandGUI extends JFrame {
/* ----  GLOBAL VARIABLES  ---------------------------------------------------------------------------------------------------------------------------------- */
    /* ---------- paths may need to tweak ---------- */
    private static final String CONFIG_PATH  = "../comms-GUI/configs.py";
    private static final String BACKEND_PATH = "../comms-GUI/host_to_client.py";
    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");

    /* ---------- network ---------- */
    private String HOST;                     // assigned by prompt
    private String NAME;
    private static final int PORT = 12345;   // match configs.host_server

    /* ---------- GUI widgets ---------- */
    private final JTextArea  console = new JTextArea();
    private final JTextField cmdField = new JTextField();
    private final DefaultListModel<String> fileModel = new DefaultListModel<>();

    private final JPanel sensorRightPanel = new JPanel(new BorderLayout());
    private final JPanel commandRightPanel = new JPanel(new BorderLayout());
    private final JTextArea commandOutputArea = new JTextArea();

    /* ----  CONSTRUCTOR  --------------------------------------------------------------------------------------------------------------------------------------- */
    private final JLabel statusLabel = new JLabel("Status: Initializing...");

    private piCommandGUI(String hostAddr) {
        super("MotheterRemote");
        this.HOST = hostAddr;

        /* layout root */
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 560);
        setLayout(new BorderLayout());

        /* tabbed pane (center) */
        JTabbedPane tabs = new JTabbedPane();
        sensorRightPanel.setBorder(BorderFactory.createTitledBorder("Command Input"));
        commandRightPanel.setBorder(BorderFactory.createTitledBorder("Backend Output"));
        commandOutputArea.setEditable(false);
        commandOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandRightPanel.add(new JScrollPane(commandOutputArea), BorderLayout.CENTER);

        tabs.addTab("RPi Command Center", wrapWithRightPanel(buildCommandTab(), commandRightPanel));
        tabs.addTab("Sensor Command Center", wrapWithRightPanel(buildSensorTab(), sensorRightPanel));
        tabs.addTab("Data Sync", buildDataTab());
        tabs.addTab("Settings", buildSettingsTab());

        add(tabs, BorderLayout.CENTER);


        // Create a wrapper panel for center + right side-by-side
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

// Set up both right panels
        sensorRightPanel.setPreferredSize(new Dimension(300, 0));
        sensorRightPanel.setBorder(BorderFactory.createTitledBorder("Command Input"));

        commandRightPanel.setPreferredSize(new Dimension(300, 0));
        commandRightPanel.setBorder(BorderFactory.createTitledBorder("Backend Output"));
        JTextArea commandOutputArea = new JTextArea();
        commandOutputArea.setEditable(false);
        commandOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandRightPanel.add(new JScrollPane(commandOutputArea), BorderLayout.CENTER);

//// Add listener to show appropriate right panel depending on tab
//        tabs.addChangeListener(e -> {
//            int selected = tabs.getSelectedIndex();
//            String title = tabs.getTitleAt(selected);
//            centerWrapper.remove(sensorRightPanel);
//            centerWrapper.remove(commandRightPanel);
//            if (title.equals("Sensor Command Center")) {
//                centerWrapper.add(sensorRightPanel, BorderLayout.EAST);
//            } else if (title.equals("RPi Command Center")) {
//                centerWrapper.add(commandRightPanel, BorderLayout.EAST);
//            }
//            centerWrapper.revalidate();
//            centerWrapper.repaint();
//        });

        /* console panel (south) */
        add(buildConsolePanel(), BorderLayout.SOUTH);

        /* launch backend & read its output */
        startPythonBackend();

        /* important: after backend is up, tell it to reload configs */
        SwingUtilities.invokeLater(() -> sendCommand("reload-config"));
    }

/* ----  TABS  ---------------------------------------------------------------------------------------------------------------------------------------------- */

    private JPanel wrapWithRightPanel(JPanel main, JPanel side) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(main, BorderLayout.CENTER);
        side.setPreferredSize(new Dimension(300, 0));
        wrapper.add(side, BorderLayout.EAST);
        return wrapper;
    }
    /**
     * buildCommandTab():
     *      constructs a new JPanel object that holds everything in the RPi command tab
     *       __ converts commands from host_to_client.py menu into distinct buttons
     *       __ sends commands back to host_to_client.py backend
     *
     * @return root --> a JPanel variable containing the contents of the tab
     */
    private JPanel buildCommandTab() {
        JPanel root = new JPanel(new BorderLayout(5,5));

        String[][] presets = {
                {"status", "Check the current process status"},
                {"start",  "Start the main data collection process"},
                {"rsync",  "Synchronize files from Pi to host"},
                {"kill",   "Kill the running process on the Pi"}
//                {"ui",     "Shows and allows user to run available commands built in to the sensor"},
//                {"help",   "List available commands"}
        };

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String[] p : presets) {
            JButton b = new JButton(p[0]);
            b.setToolTipText(p[1]);  // Set tooltip
            b.addActionListener(e -> sendCommand(p[0]));
            btnRow.add(b);
        }

        root.add(btnRow, BorderLayout.NORTH);

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendCommand(cmdField.getText()));
        cmdField.addActionListener(e -> sendBtn.doClick());

        JPanel south = new JPanel(new BorderLayout(3,3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn,  BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    /**
     *
     * @return
     */
    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField hostNameField = new JTextField(NAME);       // host_name
        JTextField hostAddrField = new JTextField(HOST);       // host_addr
        JTextField rpiNameField  = new JTextField("pi");       // rpi_name
        JTextField rpiAddrField  = new JTextField("pi");       // rpi_addr

        panel.add(new JLabel("Host Name (host_name):"));
        panel.add(hostNameField);
        panel.add(new JLabel("Host Address (host_addr):"));
        panel.add(hostAddrField);
        panel.add(new JLabel("RPi Name (rpi_name):"));
        panel.add(rpiNameField);
        panel.add(new JLabel("RPi Address (rpi_addr):"));
        panel.add(rpiAddrField);

        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> {
            String newHostName = hostNameField.getText().trim();
            String newHostAddr = hostAddrField.getText().trim();
            String newRpiName  = rpiNameField.getText().trim();
            String newRpiAddr  = rpiAddrField.getText().trim();

            if (!newHostName.isEmpty() && !newHostAddr.isEmpty() &&
                    !newRpiName.isEmpty()  && !newRpiAddr.isEmpty()) {

                updateConfigsPy(newHostName, newHostAddr, newRpiName, newRpiAddr);
                HOST = newHostAddr;
                NAME = newHostName;
                append("[Settings] configs.py updated.");
                sendCommand("reload-config");
            } else {
                append("[Error] One or more fields are empty.");
            }
        });

        panel.add(new JLabel()); // filler
        panel.add(saveButton);
        return panel;

    }

    /**
     *
     * @return root
     */
    private JPanel buildDataTab() {
        JList<String> list = new JList<>(fileModel);
        JScrollPane sp = new JScrollPane(list);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadFileList());

        JButton openDir = new JButton("Open Folder");
        openDir.addActionListener(e -> {
            try { Desktop.getDesktop().open(DATA_DIR.toFile()); }
            catch (IOException ex){ append("[GUI] "+ex.getMessage()); }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(refresh); btns.add(openDir);

        JPanel root = new JPanel(new BorderLayout(5,5));
        root.add(sp, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        loadFileList();
        return root;
    }


    private JPanel buildSensorTab() {
        // Updated Cmd class: uses Runnable instead of Supplier<String>
        class Cmd {
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
                new Cmd("Request Reading", "requests a reading", () -> sendCommand("rx")),
                new Cmd("Calibration Info", "requests calibration information", () -> sendCommand("cx")),
                new Cmd("Unit Info", "requests unit information", () -> sendCommand("ix"))
        ));

        /* 2) ARM / DISARM CAL */
        cat.put("Arm / Disarm Calibration", List.of(
                new Cmd("Arm Light", "zcalAx", () -> sendCommand("zcalAx")),
                new Cmd("Arm Dark", "zcalBx", () -> sendCommand("zcalBx")),
                new Cmd("Disarm", "zcalDx", () -> sendCommand("zcalDx"))
        ));

        /* 3) Interval / Threshold */
        cat.put("Interval / Threshold", List.of(
                new Cmd("Request Interval Settings", "Ix", () -> sendCommand("Ix")),
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
                new Cmd("Request Sim Values", "sx", () -> sendCommand("sx")),
                new Cmd("Run Simulation", "runs simulation", this::promptSimulation)
        ));

        /* 6) Data Logging Commands */
        cat.put("Data Logging Cmds", List.of(
                new Cmd("Request Pointer", "L1x", () -> sendCommand("L1x")),
                new Cmd("Log One Record", "L3x", () -> sendCommand("L3x")),
                new Cmd("Return One Record", "L4x", this::promptReturnOneRecord),
                new Cmd("Set Trigger Mode", "LMx", this::promptTriggerMode),
                new Cmd("Request Trigger Mode", "Lmx", () -> sendCommand("Lmx")),
                new Cmd("Request Interval Settings", "LIx", () -> sendCommand("LIx")),
                new Cmd("Set Interval Period", "LPx", this::promptLogIntervalPeriod),
                new Cmd("Set Threshold", "LPTx", this::promptLogThreshold)
        ));

        /* 7) Logging Utilities */
        cat.put("Logging Utilities", List.of(
                new Cmd("Request ID", "L0x", () -> sendCommand("L0x")),
                new Cmd("Erase Flash Chip", "L2x", this::confirmEraseFlash),  // update this if you also convert it to panel
                new Cmd("Battery Voltage", "L5x", () -> sendCommand("L5x")),
                new Cmd("Request Clock", "Lcx", () -> sendCommand("Lcx")),
                new Cmd("Set Clock", "Lcx", this::promptSetClock),
                new Cmd("Put Unit to Sleep", "Lsx", () -> sendCommand("Lsx")),
                new Cmd("Request Alarm Data", "Lax", () -> sendCommand("Lax"))
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

    private JPanel buildConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setPreferredSize(new Dimension(0,150));

        JButton clear = new JButton("Clear log");
        clear.addActionListener(e -> console.setText(""));

        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(clear,  BorderLayout.EAST);
        return p;
    }

/* ----  UTILITY  ------------------------------------------------------------------------------------------------------------------------------------------- */
    private void loadFileList() {
        fileModel.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA_DIR)) {
            for (Path p : ds) fileModel.addElement(p.getFileName().toString());
            append("[GUI] File list loaded");
        } catch (IOException ex){ append("[GUI] Data dir error: "+ex.getMessage()); }
    }

    private void updateConfigsPy(String newHostName, String newHostAddr,
                                 String newRpiName,  String newRpiAddr) {
        try {
            File file = new File("../comms-GUI/configs.py");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("host_name ="))
                    line = "host_name = \"" + newHostName + "\"";
                else if (line.trim().startsWith("host_addr ="))
                    line = "host_addr = \"" + newHostAddr + "\"";
                else if (line.trim().startsWith("rpi_name ="))
                    line = "rpi_name = \"" + newRpiName + "\"";
                else if (line.trim().startsWith("rpi_addr ="))
                    line = "rpi_addr = \"" + newRpiAddr + "\"";

                content.append(line).append("\n");
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

        } catch (IOException e) {
            append("[Error] configs.py update failed: " + e.getMessage());
        }
    }


    private void sendCommand(String cmd){
        if(cmd==null||cmd.isBlank()) return;
        append("\n> "+cmd);
        try(Socket s=new Socket(HOST,PORT);
            OutputStream o=s.getOutputStream();
            BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream()))){
            o.write((cmd+"\n").getBytes(StandardCharsets.UTF_8)); o.flush();
            String line; while((line=in.readLine())!=null) append(line);
        }catch(IOException ex){ append("[ERR] "+ex.getMessage()); }
    }

    private void startPythonBackend(){
        try{
            ProcessBuilder pb=new ProcessBuilder("python3","-u",BACKEND_PATH);
            pb.redirectErrorStream(true);
            Process p=pb.start();
            new Thread(()->{
                try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()))){
                    String ln; while((ln=r.readLine())!=null) append("[PY] "+ln);
                }catch(IOException ex){ append("     [PY] "+ex.getMessage());}
            }).start();
        }catch(IOException ex){ append("\n     [GUI] Can't start backend: "+ex.getMessage()); }
    }

    private void append(String txt){
        SwingUtilities.invokeLater(() -> {
            console.append(txt+"\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void sendStatusRequest() {
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 OutputStream out = socket.getOutputStream();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.write("status\n".getBytes(StandardCharsets.UTF_8));
                out.flush();

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line).append(" ");
                }

                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Status: " + response.toString().trim())
                );

            } catch (IOException e) {
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Status: ERROR - " + e.getMessage())
                );
            }
        }).start();
    }
// Utility to set sensor input panel content
        private void setRightPanel(JPanel panel) {
            sensorRightPanel.removeAll();
            sensorRightPanel.add(panel, BorderLayout.CENTER);
            sensorRightPanel.revalidate();
            sensorRightPanel.repaint();
        }

// Utility to clear sensor input panel
        private void clearRightPanel() {
            setRightPanel(new JPanel());
        }

// Utility to append backend output to command right panel
        private void appendToCommandRightPanel(String txt) {
            SwingUtilities.invokeLater(() -> {
                commandOutputArea.append(txt + "\n");
                commandOutputArea.setCaretPosition(commandOutputArea.getDocument().getLength());
            });
        }

    // Inline panel versions with original method names
    private void promptIntervalPeriod() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        JComboBox<String> unitBox = new JComboBox<>(new String[]{"Seconds", "Minutes", "Hours"});
        JTextField valueField = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            try {
                int t = Integer.parseInt(valueField.getText().trim());
                long seconds = switch(unitBox.getSelectedIndex()) {
                    case 1 -> t * 60;
                    case 2 -> t * 3600;
                    default -> t;
                };
                String withZeros = String.format("%010d", seconds);
                sendCommand("p" + withZeros + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid number");
            }
        });

        panel.add(unitBox);
        panel.add(valueField);
        panel.add(submit);
        setRightPanel(panel);
    }

    private void promptIntervalThreshold() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextField field = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            try {
                double d = Double.parseDouble(field.getText().trim());
                String cmdPart = String.format("%08.2f", d).replace(' ', '0');
                sendCommand("p" + cmdPart + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        panel.add(new JLabel("Threshold (mag/arcsec²):"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptReturnOneRecord() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextField ptrField = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            String ptr = ptrField.getText().trim();
            if (ptr.matches("\\d{1,10}")) {
                ptr = String.format("%010d", Long.parseLong(ptr));
                sendCommand("L4" + ptr + "x");
                clearRightPanel();
            } else {
                append("[Error] Invalid pointer");
            }
        });

        panel.add(new JLabel("Record pointer (0-9999999999):"), BorderLayout.NORTH);
        panel.add(ptrField, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLightOffset() {
        promptManualCalibration("Light offset (mag/arcsec²):", "%08.2f", "zcal5");
    }

    private void promptLightTemp() {
        promptManualCalibration("Light temperature (°C):", "%03.1f", "zcal6");
    }

    private void promptDarkPeriod() {
        promptManualCalibration("Dark-period (s):", "%07.3f", "zcal7");
    }

    private void promptDarkTemp() {
        promptManualCalibration("Dark temperature (°C):", "%03.1f", "zcal8");
    }

    private void promptManualCalibration(String labelText, String format, String prefix) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextField field = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            try {
                double v = Double.parseDouble(field.getText().trim());
                sendCommand(prefix + String.format(format, v).replace(' ', '0') + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        panel.add(new JLabel(labelText), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSimulation() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField countField = new JTextField();
        JTextField freqField = new JTextField();
        JTextField tempField = new JTextField();
        JButton submit = new JButton("Submit");

        panel.add(new JLabel("Counts:")); panel.add(countField);
        panel.add(new JLabel("Frequency (Hz):")); panel.add(freqField);
        panel.add(new JLabel("Temperature (°C):")); panel.add(tempField);
        panel.add(new JLabel()); panel.add(submit);

        submit.addActionListener(e -> {
            try {
                long c = Long.parseLong(countField.getText().trim());
                long f = Long.parseLong(freqField.getText().trim());
                int t = (int) Double.parseDouble(tempField.getText().trim());
                sendCommand("S," + String.format("%010d", c) + "," + String.format("%010d", f) + "," + String.format("%010d", t) + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        setRightPanel(panel);
    }

    private void promptTriggerMode() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JComboBox<String> modes = new JComboBox<>(new String[]{"0", "1", "2", "3", "4", "5", "6", "7"});
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            sendCommand("LM" + modes.getSelectedItem() + "x");
            clearRightPanel();
        });

        panel.add(new JLabel("Select Trigger Mode:"), BorderLayout.NORTH);
        panel.add(modes, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptLogIntervalPeriod() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        JComboBox<String> unitBox = new JComboBox<>(new String[]{"Seconds", "Minutes"});
        JTextField valueField = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            try {
                int v = Integer.parseInt(valueField.getText().trim());
                String zeros = String.format("%05d", v);
                String prefix = unitBox.getSelectedIndex() == 0 ? "LPS" : "LPM";
                sendCommand(prefix + zeros + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid number");
            }
        });

        panel.add(unitBox);
        panel.add(valueField);
        panel.add(submit);
        setRightPanel(panel);
    }

    private void promptLogThreshold() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextField field = new JTextField();
        JButton submit = new JButton("Submit");

        submit.addActionListener(e -> {
            try {
                double d = Double.parseDouble(field.getText().trim());
                sendCommand("LPT" + String.format("%08.2f", d).replace(' ', '0') + "x");
                clearRightPanel();
            } catch (Exception ex) {
                append("[Error] Invalid input");
            }
        });

        panel.add(new JLabel("Threshold (mag/arcsec²):"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.add(submit, BorderLayout.SOUTH);
        setRightPanel(panel);
    }

    private void promptSetClock() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField dateField = new JTextField();
        JTextField timeField = new JTextField();
        JButton submit = new JButton("Submit");

        panel.add(new JLabel("Date (YYYYMMDD):")); panel.add(dateField);
        panel.add(new JLabel("Time (HHMMSS):")); panel.add(timeField);
        panel.add(new JLabel()); panel.add(submit);

        submit.addActionListener(e -> {
            String d = dateField.getText().trim();
            String t = timeField.getText().trim();
            if (!d.matches("\\d{8}") || !t.matches("\\d{6}")) {
                append("[Error] Invalid date/time format");
                return;
            }
            String formatted = d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6) +
                    " 0 " + t.substring(0, 2) + ":" + t.substring(2, 4) + ":" + t.substring(4);
            sendCommand("Lc" + formatted + "x");
            clearRightPanel();
        });

        setRightPanel(panel);
    }

    private String confirmEraseFlash(){
        int res = JOptionPane.showConfirmDialog(this,
                "ERASE FLASH CHIP?\nThis cannot be undone.","Confirm",JOptionPane.OK_CANCEL_OPTION);
        return res==JOptionPane.OK_OPTION ? "L2x" : null;
    }

/* ----  INITIAL PROMPT  ------------------------------------------------------------------------------------------------------------------------------------ */
    private static String[] promptForHostInfo(){
        JTextField nameField = new JTextField();
        JTextField addrField = new JTextField();

        JPanel p=new JPanel(new GridLayout(0,1,5,5));
        p.add(new JLabel("host_name:")); p.add(nameField);
        p.add(new JLabel("host_addr:")); p.add(addrField);

        int res=JOptionPane.showConfirmDialog(null,p,"Configure Host",JOptionPane.OK_CANCEL_OPTION);
        if(res!=JOptionPane.OK_OPTION) return null;

        String n=nameField.getText().trim();
        String a=addrField.getText().trim();
        if(n.isEmpty()||a.isEmpty()) return null;
        return new String[]{n,a};
    }

    private static boolean initialWriteConfigs(String n,String a){
        try{
            List<String> lines = Files.readAllLines(Paths.get(CONFIG_PATH), StandardCharsets.UTF_8);
            for(int i=0;i<lines.size();i++){
                String t=lines.get(i).trim();
                if(t.startsWith("host_name")) lines.set(i,"host_name = \""+n+"\"");
                else if(t.startsWith("host_addr")) lines.set(i,"host_addr = \""+a+"\"");
            }
            Files.write(Paths.get(CONFIG_PATH), lines, StandardCharsets.UTF_8);
            return true;
        }catch(IOException ex){
            JOptionPane.showMessageDialog(null,"Failed to update configs.py:\n"+ex.getMessage());
            return false;
        }
    }


 /* ----  MAIN  --------------------------------------------------------------------------------------------------------------------------------------------- */
    public static void main(String[] args) {
        /*  prompt BEFORE building the GUI */
        String[] info = promptForHostInfo();
        if(info==null){ System.exit(0); }

        if(!initialWriteConfigs(info[0],info[1])) System.exit(0);

        SwingUtilities.invokeLater(() -> {
            piCommandGUI gui = new piCommandGUI(info[1]);  // pass host_addr
            gui.setVisible(true);

            //uncomment when status bar works
//            Timer statusTimer = new Timer(5000, e -> gui.sendStatusRequest());
//            statusTimer.start();
        });
    }
}

