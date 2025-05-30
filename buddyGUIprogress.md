# buddy's GUI development notes

## why java?
### Cross-Platform Compatibility
Runs on Windows, macOS, and Linux without needing to rewrite the UI code.

Ideal when the GUI needs to be used from different machines or OSes to control the Raspberry Pi.

### strong UI toolkit with swing
Swing provides a rich set of widgets (buttons, tabs, text areas, dialogs, etc.) that are sufficient for the needs of:
* Command buttons 
* Real-time logs 
* File listing and settings forms

### robust multithreading
built-in concurrency makes it easier to handle:
* Socket communication in the background
* Live logging and GUI updates
* Async command responses (e.g., status updates)

### maintainable and Modular
GUI logic is separated from backend logic (handled in Python), allowing easier development and debugging on both ends.

### widely known/easy accessible
java is commonly taught and widely used, so it's easier for other developers to maintain or extend.
It avoids forcing the user to install and configure a web server, browser-based interface, and is taught in Smith's CS curriculum so avoids having to learn a new language.

---
## rough prototype
implemented a GUI that runs the python backend silently --> had buttons for each command and printed text from the terminal into a text are

Need to make this better because it is just a glorified terminal

Just to keep a copy of the original GUI prototype:
```java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.net.Socket;

/**
 * Swing GUI that sends single-line commands to a Python host server
 * and prints any response in a console window.
 */
public class piCommandGUI extends JFrame {
    /* ---------- GUI components ---------- */
    private final JTextField inputField;
    private final JTextArea  console;
    private final JButton    statusButton, startButton, sendButton, rsyncButton, killButton,
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
        inputPanel.add(sendButton,  BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // ---- Pre-built command buttons ----
        statusButton = new JButton("status");
        startButton = new JButton("start");
        rsyncButton = new JButton("rsync");
        killButton  = new JButton("kill");
        uiButton    = new JButton("ui");
        helpButton  = new JButton("help");
        clearButton = new JButton("Clear");

        statusButton.addActionListener(e -> sendCommand("status"));
        startButton.addActionListener(e -> sendCommand("start"));
        rsyncButton.addActionListener(e -> sendCommand("rsync"));
        killButton .addActionListener(e -> sendCommand("kill"));
        uiButton   .addActionListener(e -> sendCommand("ui"));
        helpButton .addActionListener(e -> sendCommand("help"));
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
    private record HostInfo(String addr, String name) {}

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
             OutputStream   out   = socket.getOutputStream();
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

### how to improve?
- **tabs for each function**
  - _**Command Center**_
  - _**Settings:** update host settings or different RPi's maybe?_
  - _**Viewing data:** view data from rsync?_
- **real-time status updates?**
- **filter log by type**
    - _errors_
    - _info_
    - _etc?_
- **progress bar for rsync**

---
## prototype 2
### Implemented Tabs:
##### Command Center
* you can use the buttons to send commands (hover to see what buttons do)
* serial monitor located at the bottom of the GUI panel
  * need a better way to display data...

##### Settings
* update host and rpi name + address

##### Data Sync
* not sure if files actually can be viewed from it....


---
## prototype 3 (with sensor command center)
##### Command Center &rarr; RPi Command Center
* now only has 4 commands (`start`, `status`, `kill`, `rsync`)
* the `help` command was replaced with tooltips, going to change help button to be its own tab that explains how to use the GUI rather than listing all the commands

##### Sensor Command Center
* handles everything that the `ui` command did now with an graphical interface rather than in the terminal
* drop down menu to change the category of which command you want to run (based on the structure in `ui_commands.py`)
* a separate "screen" for each option in the dropdown with buttons to run commands in their respective categories
* uses helper functions to actually run the commands
* If a command requires user input, a popup appears to enter information
  * maybe adding a popup/second popup for every command to confirm that that is the command you want to send? and something other than the log that shows that the command was either sent/not sent

##### Data Sync
* should just open the file location in file explorer/finder or whatever (don't make my own file explorer interface)

##### Settings
* everything is the same,, a way to update your settings


### how to improve?
- **real-time status updates?** !!!
- **filter log by type** (or color coding?)
    - _errors_
    - _info_
    - _etc?_
- **progress bar for rsync**
- **Revamping the load in screen**
    - _adding auto get ip info and stuff_
    - _dropdown to switch between connection type_
    - _known pis? if multiple pis, a dropdown to switch between them_
    - also updating settings page to account for all that
- **Better way to display feedback from terminal**
---
## ?
how to make status update regularly

how to package the gui into an executable





---
## Packaging
* [JSmooth](https://jsmooth.sourceforge.net/)
* [Jar2exe](https://www.jar2exe.com/)





---
## logs

### 5/27/2025
Looked through all the code from spring semester, refamiliarized with what I was doing and how motheter remote works

started trying to fix GUI connection issue

---
### 5/28/2025
Still trying to fix connection issue

GUI only sends commands and shows what commands were sent, does not print was is shown when running `host_to_client.py` in the terminal... need to fix that

tried implementing a way to automatically read in configs properties from `configs.py`
* made `readConfigs.py` file to dump all the info from `configs.py` into a `config.properties`
* GUI should be able to yoink all the info from `config.properties` so user doesn't have to manually input their _addr_ and _name_ into `configs.py`
* for some reason the location of `config.properties` matters and idk where it should be placed?

---
### 5/29/2025
Fixed host name problem, alot of errors coming from `host_to_client.py` not supporting the GUI so I had to change some stuff so that it would

Got GUI to print what comes through in the terminal

Finished debugging connection issues --> fixed the big ssh key and ssh missing but not (??) on my laptop

decided to scrap whole `config.properties` debacle &rarr; have a designated place in the GUI for user to update their stuff, no need to access code.
Added inputting host name and ip screen which also updates `configs.py` file when starting up GUI
* need to figure out how to make the `host_to_client.py`

Figured out how to make it update using:
```python
import importlib
importlib.reload()
```
which when called with `configs` basically reloads the whole library effectively grabbing the new values that were changed in `configs`

#### started revamping GUI
added tabs for specific functions in the GUI

\
\
\
_**TODO**_ - - - - - - - -
- fix UI Button... why does it break everything
- figure out if rsync works

---
### 5/30/2025
Started to tackle the UI errors, &rarr; decided to turn the whole `ui_command.py` file into its own GUI of sorts and bonk that into its own tab:
* proposed structure of sensor UI tab:
  * dropdown menu for each category
  * buttons within each dropdown
  * when user input needed, a popup appears
  * tooltips

For the UI tab I had to make separate helper functions for every command... is there a better way to tackle this? (prop ya lol)

Started making documentation for the GUI,, still need to in depth explanation how all the methods work.
Also started adding javadoc comments to the GUI and overall polishing/making code easier to read &rarr; probably would be good to comment a little more...

I want to add a help/about button that has some of the documentation/how to use the GUI just in case the user needs help?? also there must be a better way to display the status and whatever whatever that comes out of the terminal.

\
\
\
_**TODO**_ - - - - - - - - 
* Next work day: work on adding the help button,, use the weekend to brainstorm what would be a good way to display status data
* Start thinking about how to implement Skye's auto ip yoink script so that we can bypass the whole input host_addr and host_name thing and think about wifi/ethernet/cellular dropdown thingy.