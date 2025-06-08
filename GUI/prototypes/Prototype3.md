```java

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Supplier;   // functional interface for lamdas returning String


/**
 * The main GUI class. On launch, it asks the user for <code>host_name</code>
 * and <code>host_addr</code>, updates <code>configs.py</code> with those
 * values, then spins up the Python backend (which itself listens on
 * {@value #PORT}).  Tabs are created for Pi commands, sensor commands,
 * data synchronisation, and settings.
 */
public class prototypes.piCommandGUI extends JFrame {

    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    // Path to Python‑side configuration file (relative to project root)
    private static final String CONFIG_PATH = "../comms-GUI/configs.py";
    // Path to the Python backend we invoke with ProcessBuilder
    private static final String BACKEND_PATH = "../comms-GUI/host_to_client.py";
    // Folder on host where rsync‑ed data will be stored
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), "SQMdata");

    /* ---------------- Network ---------------- */
    private String HOST;   // server IP or hostname (user‑supplied)
    private String NAME;   // human‑friendly host name (user‑supplied)
    // Socket port must match configs.host_server in the Python backend
    private static final int PORT = 12345;

    /* ---------------- Swing widgets ---------------- */
    private final JTextArea console = new JTextArea();      // running log / output
    private final JTextField cmdField = new JTextField();     // raw command entry
    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab

    // A small status label (currently updated only by sendStatusRequest())
    private final JLabel statusLabel = new JLabel("Status: Initializing...");

    /* --------------------------------------------------------------------
     * Constructor
     * ------------------------------------------------------------------ */
    private prototypes.piCommandGUI(String hostAddr) {
        super("MotheterRemote");
        this.HOST = hostAddr;

        /* ---------------- Frame setup ---------------- */
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 560);
        setLayout(new BorderLayout());

        /* ---------------- Tabbed pane (CENTER) ---------------- */
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("RPi Command Center", buildCommandTab());
        tabs.addTab("Sensor Command Center", buildSensorTab());
        tabs.addTab("Data Sync", buildDataTab());
        tabs.addTab("Settings", buildSettingsTab());
        add(tabs, BorderLayout.CENTER);

        /* ---------------- Console panel (SOUTH) ---------------- */
        add(buildConsolePanel(), BorderLayout.SOUTH);

        /* ---------------- Launch Python backend ---------------- */
        startPythonBackend();

        /* Make sure backend picks up any config changes we just wrote */
        SwingUtilities.invokeLater(() -> sendCommand("reload-config"));
    }

    /* ====================================================================
     * TAB BUILDERS
     * ================================================================= */

    /**
     * Build the RaspberryPi command tab.  Preset commands appear as
     * individual buttons, plus a free‑text field for arbitrary commands.
     */
    private JPanel buildCommandTab() {
        JPanel root = new JPanel(new BorderLayout(5, 5));

        // Preset commands and their tooltips
        String[][] presets = {
                {"status", "Check the current process status"},
                {"start", "Start the main data collection process"},
                {"rsync", "Synchronize files from Pi to host"},
                {"kill", "Kill the running process on the Pi"}
                // {"ui", "Shows available commands built in to the sensor"},
                // {"help", "List available commands"}
        };

        /* --- Top row of buttons --- */
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (String[] p : presets) {
            JButton b = new JButton(p[0]);
            b.setToolTipText(p[1]);
            b.addActionListener(e -> sendCommand(p[0]));
            btnRow.add(b);
        }
        root.add(btnRow, BorderLayout.NORTH);

        /* --- Free‑text command entry at bottom of tab --- */
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendCommand(cmdField.getText()));
        // Fire button when user hits ↵ in the text field
        cmdField.addActionListener(e -> sendBtn.doClick());

        JPanel south = new JPanel(new BorderLayout(3, 3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        return root;
    }

    /**
     * Build the Settings tab. Lets the user change host / RPi names and
     * addresses, writes them back to <code>configs.py</code>, and tells the
     * backend to reload.
     */
    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Editable text fields pre‑filled with current values
        JTextField hostNameField = new JTextField(NAME);
        JTextField hostAddrField = new JTextField(HOST);
        JTextField rpiNameField = new JTextField("pi");
        JTextField rpiAddrField = new JTextField("pi");

        panel.add(new JLabel("Host Name (host_name):"));
        panel.add(hostNameField);
        panel.add(new JLabel("Host Address (host_addr):"));
        panel.add(hostAddrField);
        panel.add(new JLabel("RPi Name (rpi_name):"));
        panel.add(rpiNameField);
        panel.add(new JLabel("RPi Address (rpi_addr):"));
        panel.add(rpiAddrField);

        /* --- Save button commits changes to disk and reloads backend --- */
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> {
            String newHostName = hostNameField.getText().trim();
            String newHostAddr = hostAddrField.getText().trim();
            String newRpiName = rpiNameField.getText().trim();
            String newRpiAddr = rpiAddrField.getText().trim();

            if (!newHostName.isEmpty() && !newHostAddr.isEmpty() &&
                    !newRpiName.isEmpty() && !newRpiAddr.isEmpty()) {

                updateConfigsPy(newHostName, newHostAddr, newRpiName, newRpiAddr);
                HOST = newHostAddr;   // update in‑memory copies
                NAME = newHostName;
                append("[Settings] configs.py updated.");
                sendCommand("reload-config");
            } else {
                append("[Error] One or more fields are empty.");
            }
        });

        panel.add(new JLabel()); // filler for alignment
        panel.add(saveButton);
        return panel;
    }

    /**
     * Build the Data tab. Shows files in <code>DATA_DIR</code>, lets the
     * user refresh the list or open the folder in the OS file explorer.
     */
    private JPanel buildDataTab() {
        JList<String> list = new JList<>(fileModel);
        JScrollPane sp = new JScrollPane(list);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadFileList());

        JButton openDir = new JButton("Open Folder");
        openDir.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DATA_DIR.toFile());
            } catch (IOException ex) {
                append("[GUI] " + ex.getMessage());
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.add(refresh);
        btns.add(openDir);

        JPanel root = new JPanel(new BorderLayout(5, 5));
        root.add(sp, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        loadFileList();           // initial population
        return root;
    }

    /**
     * Build the Sensor Command tab. This tab is more complex because
     * commands are grouped into categories and some require additional
     * user input (handled by helper <code>promptXxx()</code> methods).
     */
    private JPanel buildSensorTab() {

        /*
         * We build a Map<Category name, List<Cmd>> where each Cmd bundles
         * a button label, tooltip, and a Supplier<String> that returns the
         * actual command to send.  Most commands are fixed strings, but
         * those requiring user input pop up a dialog and may return null
         * (indicating the dialog was cancelled or the input invalid).
         */
        class Cmd {
            String label, tip;
            Supplier<String> cmd;

            Cmd(String l, String t, Supplier<String> c) {
                label = l;
                tip = t;
                cmd = c;
            }
        }
        Map<String, List<Cmd>> cat = new LinkedHashMap<>();

        /* 1) READINGS & INFO */
        cat.put("Readings & Info", List.of(
                new Cmd("Request Reading", "requests a reading", () -> "rx"),
                new Cmd("Calibration Info", "requests calibration information", () -> "cx"),
                new Cmd("Unit Info", "requests unit information", () -> "ix")
        ));

        /* 2) ARM / DISARM CAL */
        cat.put("Arm / Disarm Calibration", List.of(
                new Cmd("Arm Light", "zcalAx", () -> "zcalAx"),
                new Cmd("Arm Dark", "zcalBx", () -> "zcalBx"),
                new Cmd("Disarm", "zcalDx", () -> "zcalDx")
        ));

        /* 3) Interval / Threshold */
        cat.put("Interval / Threshold", List.of(
                new Cmd("Request Interval Settings", "Sends interval setting request", () -> "Ix"),
                new Cmd("Set Interval Period", "", this::promptIntervalPeriod),
                new Cmd("Set Interval Threshold", "", this::promptIntervalThreshold)
        ));

        /* 4) Manual Cal */
        cat.put("Manual Calibration", List.of(
                new Cmd("Set Light Offset", "manually set calibration: light offset", this::promptLightOffset),
                new Cmd("Set Light Temp", "manually set calibration: light temperature", this::promptLightTemp),
                new Cmd("Set Dark Period", "manually set calibration: dark period", this::promptDarkPeriod),
                new Cmd("Set Dark Temp", "manually set calibration: dark temperature", this::promptDarkTemp)
        ));

        /* 5) Simulation */
        cat.put("Simulation", List.of(
                new Cmd("Request Sim Values", "get simulation values", () -> "sx"),
                new Cmd("Run Simulation", "runs a simulation", this::promptSimulation)
        ));

        /* 6) Data Logging Commands */
        cat.put("Data Logging Cmds", List.of(
                new Cmd("Request Pointer", "L1x", () -> "L1x"),
                new Cmd("Log One Record", "L3x", () -> "L3x"),
                new Cmd("Return One Record", "L4…", this::promptReturnOneRecord),
                new Cmd("Set Trigger Mode", "LMx", this::promptTriggerMode),
                new Cmd("Request Trigger Mode", "Lmx", () -> "Lmx"),
                new Cmd("Request Interval Settings", "LIx", () -> "LIx"),
                new Cmd("Set Interval Period", "LPx", this::promptLogIntervalPeriod),
                new Cmd("Set Threshold", "LPTx", this::promptLogThreshold)
        ));

        /* 7) Logging Utilities */
        cat.put("Logging Utilities", List.of(
                new Cmd("Request ID", "L0x", () -> "L0x"),
                new Cmd("Erase Flash Chip", "L2x", this::confirmEraseFlash),
                new Cmd("Battery Voltage", "L5x", () -> "L5x"),
                new Cmd("Request Clock", "Lcx", () -> "Lcx"),
                new Cmd("Set Clock", "Lcx", this::promptSetClock),
                new Cmd("Put Unit to Sleep", "Lsx", () -> "Lsx"),
                new Cmd("Request Alarm Data", "Lax", () -> "Lax")
        ));

        /* ===  GUI BUILD  === */
        JComboBox<String> combo = new JComboBox<>(cat.keySet().toArray(String[]::new));
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        // Populate buttons for the currently selected category
        Runnable refresh = () -> {
            listPanel.removeAll();
            String key = (String) combo.getSelectedItem();
            for (Cmd c : cat.get(key)) {
                JButton b = new JButton(c.label);
                b.setToolTipText(c.tip);
                b.addActionListener(e -> {
                    String real = c.cmd.get();          // may prompt user
                    if (real != null && !real.isBlank()) {
                        sendCommand(real);
                    }
                });
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

    /**
     * Build the scrolling console log at the bottom of the window. Info is
     * appended via {@link #append(String)} whenever something noteworthy
     * happens (command sent/received, backend output, etc.).
     */
    private JPanel buildConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setPreferredSize(new Dimension(0, 150));

        JButton clear = new JButton("Clear log");
        clear.addActionListener(e -> console.setText(""));

        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(clear, BorderLayout.EAST);
        return p;
    }

    /* ====================================================================
     * UTILITY METHODS
     * ================================================================= */

    /** Refresh {@link #fileModel} with the contents of {@link #DATA_DIR}. */
    private void loadFileList() {
        fileModel.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA_DIR)) {
            for (Path p : ds) fileModel.addElement(p.getFileName().toString());
            append("[GUI] File list loaded");
        } catch (IOException ex) {
            append("[GUI] Data dir error: " + ex.getMessage());
        }
    }

    /**
     * In‑place update of <code>configs.py</code>. Only the four lines that
     * start with <code>host_name</code>, <code>host_addr</code>,
     * <code>rpi_name</code>, or <code>rpi_addr</code> are modified.
     */
    private void updateConfigsPy(String newHostName, String newHostAddr,
                                 String newRpiName, String newRpiAddr) {
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

    /**
     * Send <var>cmd</var> to the backend over a fresh TCP connection.
     * The backend echoes its output until the socket closes; we relay each
     * line to the console.
     */
    private void sendCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        append("\n> " + cmd);
        try (Socket s = new Socket(HOST, PORT);
             OutputStream o = s.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            o.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
            o.flush();
            String line;
            while ((line = in.readLine()) != null) append(line);
        } catch (IOException ex) {
            append("[ERR] " + ex.getMessage());
        }
    }

    /** Launch the Python backend as <code>python3 -u host_to_client.py</code>. */
    private void startPythonBackend() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-u", BACKEND_PATH);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            // Pump backend stdout into the GUI console on a background thread
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String ln;
                    while ((ln = r.readLine()) != null) append("[PY] " + ln);
                } catch (IOException ex) {
                    append("     [PY] " + ex.getMessage());
                }
            }).start();
        } catch (IOException ex) {
            append("\n     [GUI] Can't start backend: " + ex.getMessage());
        }
    }

    /** Convenience wrapper around appending to the console on the EDT. */
    private void append(String txt) {
        SwingUtilities.invokeLater(() -> {
            console.append(txt + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    /**
     * Example of polling the backend for status. Currently unused (commented
     * out in <code>main</code>), but provides a template for periodic tasks.
     */
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

    /* -------------------------------------------------------------------
     * Helper dialogs for commands that need extra user input.
     * Each method returns the fully‑formatted command string, or null if
     * the operation was cancelled / invalid.
     * ---------------------------------------------------------------- */

    // ---------- Interval / Threshold ----------
    private String promptIntervalPeriod() {
        JTextField val = new JTextField();
        String[] opts = {"Seconds", "Minutes", "Hours"};
        int unit = JOptionPane.showOptionDialog(this, val, "Interval Period – pick unit, then enter value",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        if (unit < 0) return null;
        if (JOptionPane.showConfirmDialog(this, val, "Enter integer value:", JOptionPane.OK_CANCEL_OPTION) != 0)
            return null;
        String txt = val.getText().trim();
        int t;
        try {
            t = Integer.parseInt(txt);
        } catch (Exception e) {
            return null;
        }

        long seconds = switch (unit) {
            case 1 -> t * 60;         // minutes → seconds
            case 2 -> t * 3600;       // hours   → seconds
            default -> t;
        };

        String withZeros = String.format("%010d", seconds);
        return "p" + withZeros + "x";
    }

    private String promptIntervalThreshold() {
        String thr = JOptionPane.showInputDialog(this, "Threshold (mag/arcsec²):");
        if (thr == null || thr.isBlank()) return null;
        double d;
        try {
            d = Double.parseDouble(thr);
        } catch (Exception e) {
            return null;
        }
        String cmdPart = String.format("%08.2f", d).replace(' ', '0');
        return "p" + cmdPart + "x";
    }

    // ---------- Flash erase confirmation ----------
    private String confirmEraseFlash() {
        int res = JOptionPane.showConfirmDialog(this,
                "ERASE FLASH CHIP?\nThis cannot be undone.", "Confirm", JOptionPane.OK_CANCEL_OPTION);
        return res == JOptionPane.OK_OPTION ? "L2x" : null;
    }

    // ---------- Return one record ----------
    private String promptReturnOneRecord() {
        String ptr = JOptionPane.showInputDialog(this, "Record pointer (0-9999999999):");
        if (ptr == null || !ptr.matches("\\d{1,10}")) return null;
        ptr = String.format("%010d", Long.parseLong(ptr));
        return "L4" + ptr + "x";
    }

    /* -----  Manual Calibration helpers  ----- */
    private String promptLightOffset() {          // zcal5<value>x
        String v = JOptionPane.showInputDialog(this, "Light offset (mag/arcsec²):");
        if (v == null) return null;
        try {
            Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
        return "zcal5" + String.format("%08.2f", Double.parseDouble(v)).replace(' ', '0') + "x";
    }

    private String promptLightTemp() {            // zcal6<value>x
        String v = JOptionPane.showInputDialog(this, "Light temperature (°C):");
        if (v == null) return null;
        try {
            Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
        return "zcal6" + String.format("%03.1f", Double.parseDouble(v)).replace(' ', '0') + "x";
    }

    private String promptDarkPeriod() {            // zcal7<value>x
        String v = JOptionPane.showInputDialog(this, "Dark-period (s):");
        if (v == null) return null;
        try {
            Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
        return "zcal7" + String.format("%07.3f", Double.parseDouble(v)).replace(' ', '0') + "x";
    }

    private String promptDarkTemp() {              // zcal8<value>x
        String v = JOptionPane.showInputDialog(this, "Dark temperature (°C):");
        if (v == null) return null;
        try {
            Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
        return "zcal8" + String.format("%03.1f", Double.parseDouble(v)).replace(' ', '0') + "x";
    }

    /* -----  Simulation helper  ----- */
    private String promptSimulation() {            // S,count,freq,temp x
        JTextField counts = new JTextField();
        JTextField frequency = new JTextField();
        JTextField temp = new JTextField();
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        p.add(new JLabel("Counts:"));
        p.add(counts);
        p.add(new JLabel("Frequency (Hz):"));
        p.add(frequency);
        p.add(new JLabel("Temperature (°C):"));
        p.add(temp);
        int res = JOptionPane.showConfirmDialog(this, p, "Simulation params",
                JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;
        try {
            long c = Long.parseLong(counts.getText().trim());
            long f = Long.parseLong(frequency.getText().trim());
            int t = (int) Double.parseDouble(temp.getText().trim());
            String sc = String.format("%010d", c);
            String sf = String.format("%010d", f);
            String st = String.format("%010d", t);
            return "S," + sc + "," + sf + "," + st + "x";
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Trigger mode ----------
    private String promptTriggerMode() {           // LM<mode>x
        String[] opts = {"0", "1", "2", "3", "4", "5", "6", "7"};
        String m = (String) JOptionPane.showInputDialog(this,
                "Select trigger-mode:", "Trigger-Mode",
                JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        return (m == null) ? null : "LM" + m + "x";
    }

    // ---------- Logging interval period ----------
    private String promptLogIntervalPeriod() {     // LP[S|M]<value>x
        JTextField val = new JTextField();
        String[] units = {"Seconds", "Minutes"};
        int u = JOptionPane.showOptionDialog(this, val,
                "Interval Period – choose unit, then enter value",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, units, units[0]);
        if (u < 0) return null;
        if (JOptionPane.showConfirmDialog(this, val,
                "Enter integer value:", JOptionPane.OK_CANCEL_OPTION) != 0) return null;
        try {
            int v = Integer.parseInt(val.getText().trim());
            String zeros = String.format("%05d", v);
            return (u == 0 ? "LPS" : "LPM") + zeros + "x";
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Logging threshold ----------
    private String promptLogThreshold() {          // LPT<threshold>x
        String v = JOptionPane.showInputDialog(this, "Threshold (mag/arcsec²):");
        if (v == null) return null;
        try {
            Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
        return "LPT" + String.format("%08.2f", Double.parseDouble(v)).replace(' ', '0') + "x";
    }

    // ---------- Set clock ----------
    private String promptSetClock() {              // LcYYYY-MM-DD w HH:MM:SSx
        JTextField date = new JTextField();      // yyyyMMdd
        JTextField time = new JTextField();      // HHmmss
        JPanel p = new JPanel(new GridLayout(0, 2, 5, 5));
        p.add(new JLabel("Date (YYYYMMDD):"));
        p.add(date);
        p.add(new JLabel("Time (HHMMSS):"));
        p.add(time);
        if (JOptionPane.showConfirmDialog(this, p, "Set Clock",
                JOptionPane.OK_CANCEL_OPTION) != 0) return null;
        String d = date.getText().trim();
        String t = time.getText().trim();
        if (!d.matches("\\d{8}") || !t.matches("\\d{6}")) return null;
        /* weekday – 0 placeholder (backend may ignore) */
        String formatted = d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6)
                + " 0 " + t.substring(0, 2) + ":" + t.substring(2, 4) + ":" + t.substring(4);
        return "Lc" + formatted + "x";
    }

    /* ====================================================================
     * INITIAL PROMPT & MAIN ENTRY POINT
     * ================================================================= */

    /** Ask user for <code>host_name</code> and <code>host_addr</code>. */
    private static String[] promptForHostInfo() {
        JTextField nameField = new JTextField();
        JTextField addrField = new JTextField();

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("host_name:"));
        p.add(nameField);
        p.add(new JLabel("host_addr:"));
        p.add(addrField);

        int res = JOptionPane.showConfirmDialog(null, p, "Configure Host", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;

        String n = nameField.getText().trim();
        String a = addrField.getText().trim();
        if (n.isEmpty() || a.isEmpty()) return null;
        return new String[]{n, a};
    }

    /** Write initial host_name / host_addr values into configs.py. */
    private static boolean initialWriteConfigs(String n, String a) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(CONFIG_PATH), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String t = lines.get(i).trim();
                if (t.startsWith("host_name")) lines.set(i, "host_name = \"" + n + "\"");
                else if (t.startsWith("host_addr")) lines.set(i, "host_addr = \"" + a + "\"");
            }
            Files.write(Paths.get(CONFIG_PATH), lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Failed to update configs.py:\n" + ex.getMessage());
            return false;
        }
    }

    /* ---------------- MAIN ---------------- */
    public static void main(String[] args) {
        /* Ask user for host info BEFORE constructing the GUI. */
        String[] info = promptForHostInfo();
        if (info == null) {
            System.exit(0);
        }

        if (!initialWriteConfigs(info[0], info[1])) System.exit(0);

        SwingUtilities.invokeLater(() -> {
            prototypes.piCommandGUI gui = new prototypes.piCommandGUI(info[1]);  // pass host_addr
            gui.setVisible(true);

            // Uncomment to start periodic status polling (needs backend support)
            // Timer statusTimer = new Timer(5000, e -> gui.sendStatusRequest());
            // statusTimer.start();
        });
    }
}

```