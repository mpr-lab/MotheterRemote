import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Simple Swing GUI that sends single-line commands to a Python host server
 * and prints any response in a console window.
 */
public class piCommandGUI extends JFrame {
    /* ---------- GUI components ---------- */
    private final JTextField inputField;
    private final JTextArea  console;
    private final JButton    statusButton, sendButton, rsyncButton, killButton,
            uiButton, helpButton, clearButton;

    /* ---------- Connection parameters (update to match configs.py) ---------- */
    private static final String HOST = "buddy-surface";  // hostname or IP of the *host* computer
    private static final int    PORT = 12345;    // must match configs.host_server

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
        inputPanel.add(sendButton,  BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // ---- Pre-built command buttons ----
        statusButton = new JButton("status");
        rsyncButton = new JButton("rsync");
        killButton  = new JButton("kill");
        uiButton    = new JButton("ui");
        helpButton  = new JButton("help");
        clearButton = new JButton("Clear");

        statusButton.addActionListener(e -> sendCommand("status"));
        rsyncButton.addActionListener(e -> sendCommand("rsync"));
        killButton .addActionListener(e -> sendCommand("kill"));
        uiButton   .addActionListener(e -> sendCommand("ui"));
        helpButton .addActionListener(e -> sendCommand("help"));
        clearButton.addActionListener(e -> console.setText(""));

        JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        commandPanel.add(statusButton);
        commandPanel.add(rsyncButton);
        commandPanel.add(killButton);
        commandPanel.add(uiButton);
        commandPanel.add(helpButton);
        commandPanel.add(clearButton);
        add(commandPanel, BorderLayout.NORTH);

//        //kill python process when window closes
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                if (pythonProcess != null) {
//                    pythonProcess.destroy();
//                }
//            }
//        });


        /* Allow Enter key in the text field to trigger sendCommand() */
        inputField.addActionListener(e -> sendCommand(inputField.getText()));
    }

    /**
     * Opens a socket, sends the command, then streams back any text from the host.
     * Diagnostic printouts go to stdout so you can watch the terminal too.
     */
    private void sendCommand(String command) {

        if (command == null || command.isEmpty()) return;

//        System.out.println("DEBUG: Preparing to send -> " + command);
        console.append("\n> " + command + "\n");  // echo to GUI console

        /* Try-with-resources ensures socket closes automatically */
        try (Socket socket = new Socket(HOST, PORT);
             OutputStream   out   = socket.getOutputStream();
             BufferedReader inBuf = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

//            System.out.println("DEBUG: Connected to " + HOST + ":" + PORT);

            /* Send the command terminated by newline (matches Python side) */
            out.write((command + "\n").getBytes("UTF-8"));
            out.flush();
//            System.out.println("DEBUG: Command bytes flushed to socket");

            /* Read any response line-by-line until the server closes the socket */
            String line;
            while ((line = inBuf.readLine()) != null) {
                console.append(line + "\n");
            }

//            System.out.println("DEBUG: Server closed connection normally");

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




    /* ---------- Main entry point ---------- */
//    public static void main(String[] args) {
//        /* Launch on Swing event thread */
//        SwingUtilities.invokeLater(() -> {
//            System.out.println("DEBUG: Launching GUI …");
//            piCommandGUI gui = new piCommandGUI();
//            gui.setVisible(true);
//        });
//    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            piCommandGUI gui = new piCommandGUI();
            gui.setVisible(true);

            startPythonBackend(gui);  // Start Python backend after GUI is up
        });
    }



}
