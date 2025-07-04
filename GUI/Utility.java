import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Utility {
    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    // Path to Python‑side configuration file (relative to project root)
    private static final String CONFIG_PATH  = "../ssh/configs_ssh.py";
    // Path to the Python backend we invoke with ProcessBuilder
    private static final File BACKEND_PATH = new File("../ssh/host_ssh.py");
    private static File PYTHON_FILE = new File("python/python.exe");

    // Folder on host where rsync‑ed data will be stored
    private static final Path   DATA_DIR     = null;
    private static Path PROFILE_DIR = null;

    private JTextArea  CONSOLE;      // running log / output

    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab
    public Utility(){}
    public Utility(JTextArea Console){
        this.CONSOLE = Console;
    }

    private void setConfigs(JTextArea console){
        CONSOLE = console;
    }

    /**
     * Detects and returns the appropriate Python executable path for the host OS.
     * @return Python interpreter path as String.
     */
    String getPythonPath(){
        String path = "";
        String os = getDetectedOSType();
//        if(os.equalsIgnoreCase("windows")){
//            PYTHON_FILE = new File("python/python.exe");
//            path = "python3";
//        }else{
//            path = "python3";
//        }
        return "python3";
    }

    /**
     * Updates the rpi_name and rpi_addr fields in the configs_ssh.py file.
     * @param newRpiName New Raspberry Pi name.
     * @param newRpiAddr New Raspberry Pi address.
     */
    public void updateConfigsPy(String newRpiName,  String newRpiAddr) {
        try {
            File file = new File(CONFIG_PATH);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("rpi_name ="))
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
     * Wraps a main panel with a right-side panel for layout.
     * @param main The main content panel.
     * @param side The side panel to attach to the right.
     * @return A new wrapper JPanel with both components.
     */
    public JPanel wrapWithRightPanel(JPanel main, JPanel side) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(main, BorderLayout.CENTER);
        side.setPreferredSize(new Dimension(300, 0));
        wrapper.add(side, BorderLayout.EAST);
        return wrapper;
    }

    /**
     * Runs the Python backend script with a command and streams its output to the console.
     * @param cmd The command string to send.
     */
//    public void sendCommand(String cmd) {
//        if (cmd == null || cmd.isBlank()) {
//            System.out.println("[DEBUG] Command is null or blank — skipping.");
//            return;
//        }
//
//        System.out.println("[DEBUG] Sending command: " + cmd);
//        append("\n> " + cmd);
//
//        try {
//            File backendFile = new File(BACKEND_PATH.getAbsolutePath());
//            System.out.println("[DEBUG] BACKEND_PATH = " + BACKEND_PATH);
//            System.out.println("[DEBUG] BACKEND_PATH absolute = " + backendFile.getAbsolutePath());
//            System.out.println("[DEBUG] File exists? " + backendFile.exists());
//
//            // Prepare command
//            ProcessBuilder pb = new ProcessBuilder(getPythonPath(), BACKEND_PATH.getAbsolutePath(), cmd);
//            pb.redirectErrorStream(true);
//            Process p = pb.start();
//
//            System.out.println("[DEBUG] Process started.");
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line;
//            int lineCount = 0;
//
//            while ((line = reader.readLine()) != null) {
//                System.out.println("[DEBUG] Python output: " + line);
//                append(line);
//                lineCount++;
//            }
//
//            int exitCode = p.waitFor();
//            System.out.println("[DEBUG] Process exited with code: " + exitCode);
//
//            if (lineCount == 0) {
//                System.out.println("[DEBUG] No output received from backend.");
//            }
//
//            if (exitCode == 0) {
//                showToast(CONSOLE, "Command sent: " + cmd, "success", 2000);
//            } else {
//                showToast(CONSOLE, "Command failed: " + cmd, "error", 2000);
//            }
//
//        } catch (IOException | InterruptedException ex) {
//            System.out.println("[DEBUG] Exception thrown: " + ex.getMessage());
//            ex.printStackTrace();
//            append("[ERR] " + ex.getMessage());
//            showToast(CONSOLE, "Error: " + ex.getMessage(), "error", 2000);
//        }
//    }
    public void sendCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;

        append("\n> " + cmd);

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", BACKEND_PATH.getAbsolutePath(), cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder outputBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                append(line);
                outputBuffer.append(line).append("\n");
            }

            int exitCode = p.waitFor();

            String output = outputBuffer.toString().toLowerCase(); // Normalize for easier checking
            if (exitCode == 0 && !output.contains("error")) {
                showToast(CONSOLE, "Command sent successfully: " + cmd, "success", 2000);
            } else if (output.contains("error") || output.contains("traceback") || exitCode != 0) {
                showToast(CONSOLE, "Command failed: " + cmd + "\n Check console for details.", "error", 3000);
            } else {
                showToast(CONSOLE, "Command completed with unknown status: " + cmd, "info", 3000);
            }

        } catch (IOException | InterruptedException ex) {
            append("[ERR] " + ex.getMessage());
            showToast(CONSOLE, "Error running command: " + ex.getMessage(), "error", 3000);
        }
    }


    /**
     * Shows a temporary toast-style popup message on the bottom-right of the parent window.
     * @param parentComponent A component inside the parent window.
     * @param message The message to display.
     * @param type Message type ("success", "error", etc.)
     * @param durationMillis How long to show the toast in milliseconds.
     */
    public void showToast(Component parentComponent, String message, String type, int durationMillis) {
        SwingUtilities.invokeLater(() -> {
            JWindow toast = new JWindow(SwingUtilities.getWindowAncestor(parentComponent));
            toast.setBackground(new Color(0, 0, 0, 0));

            Color bgColor;
            switch (type) {
                case "error":   bgColor = new Color(242, 178, 178, 220); break;
                case "success": bgColor = new Color(172, 188, 160, 220); break;
                case "info":    bgColor = new Color(155, 170, 194, 220); break;
                default:        bgColor = new Color(155, 170, 194, 220); break;
            }

            JPanel panel = new JPanel() {
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    super.paintComponent(g);
                }
            };
            panel.setOpaque(false);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JLabel label = new JLabel(message);
            label.setForeground(Color.WHITE);
            label.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 13));
            panel.add(label);

            toast.add(panel);
            toast.pack();

            // Position: bottom-right corner *within* the parent window
            if (parentComponent instanceof Component) {
                Component parent = SwingUtilities.getWindowAncestor(parentComponent);
                Point parentLoc = parent.getLocationOnScreen();
                Dimension parentSize = parent.getSize();
                int x = parentLoc.x + parentSize.width - toast.getWidth() - 30;
                int y = parentLoc.y + parentSize.height - toast.getHeight() - 50;
                toast.setLocation(x, y);
            }

            toast.setVisible(true);

            // Auto-dismiss after duration
            new Timer(durationMillis, e -> toast.setVisible(false)).start();
        });
    }

    /**
     * Starts the Python backend process in unbuffered mode and streams its output to the console.
     */
    public void startPythonBackend(){
        try{
            ProcessBuilder pb=new ProcessBuilder(getPythonPath(),"-u",BACKEND_PATH.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p=pb.start();
            new Thread(()->{
                try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()))){
                    String ln; while((ln=r.readLine())!=null) append("[PY] "+ln);
                }catch(IOException ex){ append("     [PY] "+ex.getMessage());}
            }).start();
        }catch(IOException ex){ append("\n     [GUI] Can't start backend: "+ex.getMessage()); }
    }

    /**
     * Safely appends a message to the console and scrolls to the bottom.
     * @param txt The text to append.
     */
    public void append(String txt){
        SwingUtilities.invokeLater(() -> {
            CONSOLE.append(txt+"\n");
            CONSOLE.setCaretPosition(CONSOLE.getDocument().getLength());
        });
    }

    /**
     * Copies the given text to the system clipboard.
     * @param text The text to copy.
     */
    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null
        );
    }

    /**
     * Builds a horizontal row with a read-only text field and a "Copy" button.
     * @param command The command string to show.
     * @param height The preferred height of the row.
     * @return The assembled JPanel.
     */
//    JPanel buildCopyRow(String command, int height){
//        JPanel row = new JPanel();
//        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
//        JTextField cmdField = new JTextField(command);
//        cmdField.setEditable(false);
//        JButton copyBtn = new JButton("Copy");
//        copyBtn.addActionListener(e -> copyToClipboard(command));
//        row.add(cmdField);
//        row.add(Box.createRigidArea(new Dimension(10, 0)));
//        row.add(copyBtn);
//        row.setMaximumSize(new Dimension(500, height));
//        setFullWidthCopy.accept(row);
//
//        return row;
//    }
    JPanel buildCopyRow(String command) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

        JTextField cmdField = new JTextField(command);
        cmdField.setEditable(false);

        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(command));

        row.add(cmdField);
        row.add(Box.createRigidArea(new Dimension(10, 0)));
        row.add(copyBtn);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, cmdField.getPreferredSize().height));
        return row;
    }


    int preferredWidth = 465;
    java.util.function.Consumer<JComponent> setFullWidth = comp -> {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d = comp.getPreferredSize();
        d.width = preferredWidth;
        comp.setMaximumSize(d);
    };
//    java.util.function.Consumer<JComponent> setFullWidthCopy = comp -> {
//        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
//        Dimension d = comp.getPreferredSize();
//        d.width = 450;
//        comp.setMaximumSize(d);
//    };

    /**
     * Builds a read-only, word-wrapped JTextArea with consistent styling.
     * @param panel The parent panel to inherit background color.
     * @param height The preferred height of the text area.
     * @return The configured JTextArea.
     */
//    JTextArea buildTextArea(JPanel panel, int height){
//        JTextArea textArea = new JTextArea();
//        textArea.setEditable(false);
//        textArea.setLineWrap(true);
//        textArea.setWrapStyleWord(true);
//        textArea.setBackground(panel.getBackground());
//        textArea.setPreferredSize(new Dimension(preferredWidth, height));
//        setFullWidth.accept(textArea);
//
//        return textArea;
//    }
    JTextArea buildTextArea(JPanel panel) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(panel.getBackground());
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Let it expand naturally in a scrollable layout
        textArea.setMaximumSize(new Dimension(preferredWidth, Integer.MAX_VALUE));
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        return textArea;
    }


    /**
     * Reads the profile save directory from host_config.properties.
     * @return The Path to the profile save directory, or null if not set.
     */
    public Path getProfileSaveDirFromConfig() {
        Path configPath = Paths.get("host_config.properties");

        if (!Files.exists(configPath)) return null;

        Properties props = new Properties();
        try (FileReader reader = new FileReader(configPath.toFile())) {
            props.load(reader);
            String path = props.getProperty("profile_save_path");
            return (path != null && !path.isBlank()) ? Paths.get(path) : null;
        } catch (IOException e) {
            System.err.println("[Utility] Failed to load profile_save_path: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads the SQM data save directory from host_config.properties.
     * @return The Path to the SQM data save directory, or null if not set.
     */
    public Path getSQMSaveDirFromConfig() {
        Path configPath = Paths.get("host_config.properties");

        if (!Files.exists(configPath)) return null;

        Properties props = new Properties();
        try (FileReader reader = new FileReader(configPath.toFile())) {
            props.load(reader);
            String path = props.getProperty("sqm_data_path");
            return (path != null && !path.isBlank()) ? Paths.get(path) : null;
        } catch (IOException e) {
            System.err.println("[Utility] Failed to load sqm_data_path: " + e.getMessage());
            return null;
        }
    }

    /**
     * Detects the host OS type by reading host_config.properties.
     * @return The detected OS name in lowercase, or "unknown" if not found.
     */
    String getDetectedOSType() {
        File configFile = new File("host_config.properties");

        if (!configFile.exists()) {
            System.err.println("[OS Detect] Config file does not exist.");
            return "unknown";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader("host_config.properties"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("OS=")) {
                    String os = line.substring(3).trim().toLowerCase();
                    System.out.println("[OS Detect] Detected OS: " + os);
                    return os;
                }
            }
        } catch (IOException e) {
            System.err.println("[OS Detect] Failed to read config: " + e.getMessage());
        }

        return "unknown";
    }

}