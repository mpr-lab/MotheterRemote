```java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Swing GUI that sends single-line commands to a Python host server
 * and prints any response in a console window.
 */
public class piCommandGUI extends JFrame {
    /* ---------- GUI components ---------- */
    private final JTextField inputField;
    private final JTextArea console;
    private final JButton statusButton, startButton, sendButton, rsyncButton, killButton,
            uiButton, helpButton, clearButton;

    /* ---------- Connection parameters (update to match configs.py) ---------- */
    private String HOST = "localhost"; // This will be overridden in showConfigDialog()
    private static final int PORT = 12345;    // must match configs.host_server

    /* ---------- Constructor sets up the GUI ---------- */
    public piCommandGUI() {
        setTitle("RPi Command Center");
        setSize(700, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ---- Console output area ----
        console = new JTextArea();
        console.setEditable(false);
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(console), BorderLayout.CENTER);

        // ---- Text field + “Send” button ----
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendCommand(inputField.getText()));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // ---- Pre-built command buttons ----
        statusButton = new JButton("status");
        startButton = new JButton("start");
        rsyncButton = new JButton("rsync");
        killButton = new JButton("kill");
        uiButton = new JButton("ui");
        helpButton = new JButton("help");
        clearButton = new JButton("Clear");

        statusButton.addActionListener(e -> sendCommand("status"));
        startButton.addActionListener(e -> sendCommand("start"));
        rsyncButton.addActionListener(e -> sendCommand("rsync"));
        killButton.addActionListener(e -> sendCommand("kill"));
        uiButton.addActionListener(e -> sendCommand("ui"));
        helpButton.addActionListener(e -> sendCommand("help"));
        clearButton.addActionListener(e -> console.setText(""));

        JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        commandPanel.add(statusButton);
        commandPanel.add(startButton);
        commandPanel.add(rsyncButton);
        commandPanel.add(killButton);
        commandPanel.add(uiButton);
        commandPanel.add(helpButton);
        commandPanel.add(clearButton);
        add(commandPanel, BorderLayout.NORTH);


        /* Allow Enter key in the text field to trigger sendCommand() */
        inputField.addActionListener(e -> sendCommand(inputField.getText()));
    }

    /* Java will overwrite these two lines in configs.py              */
    /*   host_name = "buddy"                                          */
    /*   host_addr = "buddy-surface"                                  */
    /* so keep the assignment format EXACTLY the same in that file.   */

    // -------- helper record to bring back two strings -------------
    private record HostInfo(String addr, String name) {
    }

    /* ---------- Start-up dialog that asks for BOTH fields ---- */
    private void showConfigDialog() {
        JTextField hostNameField = new JTextField(HOST); // use current HOST as default
        JTextField hostAddrField = new JTextField(HOST); // assuming HOST = host_addr

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Host Name (host_name):"));
        panel.add(hostNameField);
        panel.add(new JLabel("Host Address (host_addr):"));
        panel.add(hostAddrField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Enter Host Info",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newHostName = hostNameField.getText().trim();
            String newHostAddr = hostAddrField.getText().trim();

            if (!newHostAddr.isEmpty() && !newHostName.isEmpty()) {
                updateConfigsPy(newHostName, newHostAddr);  // update the Python configs.py
                HOST = newHostAddr;                         // update Java-side host IP
                console.append("Updated host_name and host_addr in configs.py\n");

                sendCommand("reload-config");
            } else {
                console.append("Error: Host name or address was empty.\n");
            }
        }
    }


    /* ---------- Write new values into configs.py -------------- */
    private void updateConfigsPy(String newHostName, String newHostAddr) {
        try {
            File file = new File("../comms/configs.py");  // adjust path if needed
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("host_name ="))
                    line = "host_name = \"" + newHostName + "\"";
                else if (line.trim().startsWith("host_addr ="))
                    line = "host_addr = \"" + newHostAddr + "\"";
                content.append(line).append("\n");
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content.toString());
            writer.close();

        } catch (IOException e) {
            console.append("Error updating configs.py: " + e.getMessage() + "\n");
        }
    }


    /**
     * Opens a socket, sends the command, then streams back any text from the host.
     * Diagnostic printouts go to stdout so you can watch the terminal too.
     */
    private void sendCommand(String command) {

        if (command == null || command.isEmpty()) return;

        System.out.println("DEBUG: Preparing to send -> " + command);
        console.append("\n> " + command + "\n");  // echo to GUI console

        /* Try-with-resources ensures socket closes automatically */
        try (Socket socket = new Socket(HOST, PORT);
             OutputStream out = socket.getOutputStream();
             BufferedReader inBuf = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("DEBUG: Connected to " + HOST + ":" + PORT);

            /* Send the command terminated by newline (matches Python side) */
            out.write((command + "\n").getBytes("UTF-8"));
            out.flush();
            System.out.println("DEBUG: Command bytes flushed to socket");

            /* Read any response line-by-line until the server closes the socket */
            String line;
            while ((line = inBuf.readLine()) != null) {
                console.append(line + "\n");
            }
            System.out.println("DEBUG: Server closed connection normally");

        } catch (IOException ex) {
            /* Most common failure points:
               - Unknown host / bad DNS
               - Connection refused (nothing listening on HOST:PORT)
               - Firewall blocking traffic
               - Server accepted connection but crashed mid-stream
             */
            console.append("Error: " + ex.getMessage() + "\n");
            System.err.println("ERROR: " + ex);
            ex.printStackTrace();  // full stack trace to terminal for deep debug
        }

        inputField.setText("");  // clear entry box for next command
    }

    private static Process pythonProcess;

    private static void startPythonBackend(piCommandGUI guiInstance) {
        try {
            String scriptPath = "../comms/host_to_client.py";  // Replace with actual path

            ProcessBuilder pb = new ProcessBuilder("python3", "-u", scriptPath);
            pb.redirectErrorStream(true); // combine stderr with stdout
            pythonProcess = pb.start();

            // Thread to read from the Python backend continuously
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        guiInstance.appendToConsole("[PY] " + line);
                    }
                } catch (IOException e) {
                    guiInstance.appendToConsole("[PY ERR] " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            guiInstance.appendToConsole("[Startup Error]: " + e.getMessage());
        }
    }


    public void appendToConsole(String text) {
        SwingUtilities.invokeLater(() -> {
            console.append(text + "\n");
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            piCommandGUI gui = new piCommandGUI();

            // Show config dialog BEFORE GUI becomes visible
            gui.showConfigDialog();  // sets HOST and updates configs.py

            // Start the Python backend process
            gui.setVisible(true);
            startPythonBackend(gui);  // already defined method

            // Now show the GUI window
            gui.setVisible(true);
        });
    }
}
```