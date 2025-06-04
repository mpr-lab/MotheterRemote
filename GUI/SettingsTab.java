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

public class SettingsTab extends JPanel{
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
    private int PORT = 12345;
    private JTextArea  CONSOLE;      // running log / output

    public SettingsTab(JTextArea Console, String Host, int Port){
        setSize(800, 560);
        setLayout(new BorderLayout());

        setConfigs(Console, Host, Port);

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

        add(panel, BorderLayout.CENTER);
    }
    private void setConfigs(JTextArea console, String host, int port){
        CONSOLE = console;
        HOST = host;
        PORT = port;
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
    private void append(String txt){
        SwingUtilities.invokeLater(() -> {
            CONSOLE.append(txt+"\n");
            CONSOLE.setCaretPosition(CONSOLE.getDocument().getLength());
        });
    }

}