import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
public class Utility {
    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    // Path to Python‑side configuration file (relative to project root)
    private static final String CONFIG_PATH  = "../ssh/configs_ssh.py";
    // Path to the Python backend we invoke with ProcessBuilder
    private static final String BACKEND_PATH = "../ssh/host_ssh.py";
    // Folder on host where rsync‑ed data will be stored
    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");

    private JTextArea  CONSOLE;      // running log / output

    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab
    public Utility(){}
    public Utility(JTextArea Console){
        this.CONSOLE = Console;
    }

    private void setConfigs(JTextArea console){
        CONSOLE = console;
    }


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

    public JPanel wrapWithRightPanel(JPanel main, JPanel side) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(main, BorderLayout.CENTER);
        side.setPreferredSize(new Dimension(300, 0));
        wrapper.add(side, BorderLayout.EAST);
        return wrapper;
    }
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
//            File backendFile = new File(BACKEND_PATH);
//            System.out.println("[DEBUG] BACKEND_PATH = " + BACKEND_PATH);
//            System.out.println("[DEBUG] BACKEND_PATH absolute = " + backendFile.getAbsolutePath());
//            System.out.println("[DEBUG] File exists? " + backendFile.exists());
//
//            String detectedOS = getDetectedOSType();
//
//            // Use detectedOS to choose Python command
//            String pythonCmd;
//            switch (detectedOS) {
//                case "windows":
//                    pythonCmd = "python";
//                    break;
//                case "linux", "mac":
//                    pythonCmd = "python3";
//                    break;
//                default:
//                    pythonCmd = "python3"; // Fallback
//                    System.err.println("[WARN] Unknown OS, defaulting to 'python3'");
//            }
//
//            // Prepare command
//            ProcessBuilder pb = new ProcessBuilder(pythonCmd, BACKEND_PATH, cmd);
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


//    public void sendCommand(String cmd) {
//        if (cmd == null || cmd.isBlank()) return;
//        append("\n> " + cmd);
//
//        try {
//            ProcessBuilder pb = new ProcessBuilder("python3", BACKEND_PATH, cmd);
//            pb.redirectErrorStream(true);  // merge stderr with stdout
//            Process p = pb.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                append(line);
//            }
//
//            p.waitFor();
//        } catch (IOException | InterruptedException ex) {
//            append("[ERR] " + ex.getMessage());
//        }
//    }
public void sendCommand(String cmd) {
    if (cmd == null || cmd.isBlank()) {
        System.out.println("[DEBUG] Command is null or blank — skipping.");
        return;
    }

    System.out.println("[DEBUG] Sending command: " + cmd);
    append("\n> " + cmd);

    try {
        File backendFile = new File(BACKEND_PATH);
        System.out.println("[DEBUG] BACKEND_PATH = " + BACKEND_PATH);
        System.out.println("[DEBUG] BACKEND_PATH absolute = " + backendFile.getAbsolutePath());
        System.out.println("[DEBUG] File exists? " + backendFile.exists());

        // Prepare command
        ProcessBuilder pb = new ProcessBuilder("python3", BACKEND_PATH, cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        System.out.println("[DEBUG] Process started.");

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        int lineCount = 0;

        while ((line = reader.readLine()) != null) {
            System.out.println("[DEBUG] Python output: " + line);
            append(line);
            lineCount++;
        }

        int exitCode = p.waitFor();
        System.out.println("[DEBUG] Process exited with code: " + exitCode);

        if (lineCount == 0) {
            System.out.println("[DEBUG] No output received from backend.");
        }

        if (exitCode == 0) {
            showToast(CONSOLE, "Command sent: " + cmd, "success", 2000);
        } else {
            showToast(CONSOLE, "Command failed: " + cmd, "error", 2000);
        }

    } catch (IOException | InterruptedException ex) {
        System.out.println("[DEBUG] Exception thrown: " + ex.getMessage());
        ex.printStackTrace();
        append("[ERR] " + ex.getMessage());
        showToast(CONSOLE, "Error: " + ex.getMessage(), "error", 2000);
    }
}


    public void showToast(Component parentComponent, String message, String type, int durationMillis) {
        SwingUtilities.invokeLater(() -> {
            JWindow toast = new JWindow(SwingUtilities.getWindowAncestor(parentComponent));
            toast.setBackground(new Color(0, 0, 0, 0));

            Color bgColor;
//            switch (type.toLowerCase()) {
//                case "success": bgColor = new Color(155, 170, 194, 220); break;     // Green
//                case "error":   bgColor = new Color(155, 170, 194, 220); break;     // Red
//                default:        bgColor = new Color(0, 0, 0, 200); break;       // Black
//            }
            bgColor = new Color(155, 170, 194, 220);

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



    public void startPythonBackend(){
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

    public void append(String txt){
        SwingUtilities.invokeLater(() -> {
            CONSOLE.append(txt+"\n");
            CONSOLE.setCaretPosition(CONSOLE.getDocument().getLength());
        });
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(text), null
        );
    }

    JPanel buildCopyRow(String command, int height){
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        JTextField cmdField = new JTextField(command);
        cmdField.setEditable(false);
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> copyToClipboard(command));
        row.add(cmdField);
        row.add(Box.createRigidArea(new Dimension(10, 0)));
        row.add(copyBtn);
        row.setMaximumSize(new Dimension(500, height));
        setFullWidth.accept(row);

        return row;
    }

    private JPanel buildTemplate(){
        JPanel template = new JPanel();
        template.setLayout(new BoxLayout(template, BoxLayout.Y_AXIS));
        template.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Helper to unify width and alignment
        return template;
    }
    int preferredWidth = 475;
    java.util.function.Consumer<JComponent> setFullWidth = comp -> {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension d = comp.getPreferredSize();
        d.width = preferredWidth;
        comp.setMaximumSize(d);
    };

    JTextArea buildTextArea(JPanel panel, int height){
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(panel.getBackground());
        textArea.setPreferredSize(new Dimension(preferredWidth, height));
        setFullWidth.accept(textArea);

        return textArea;
    }

    String getDetectedOSType() {
        File configFile = new File("profiles/host_config.properties");

        if (!configFile.exists()) {
            System.err.println("[OS Detect] Config file does not exist.");
            return "unknown";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
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