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

public class RPiCommandTab extends JPanel {
    /* ---------------- Network ---------------- */
    private String HOST;   // server IP or hostname (user‑supplied)
    private String NAME;   // human‑friendly host name (user‑supplied)
    private int PORT;
    private JTextArea  CONSOLE;      // running log / output
    public RPiCommandTab(JTextArea Console, String Host, String Name, int Port) {
        setConfigs(Console, Host, Name, Port);
        Utility util = new Utility(Console, Host, Name, Port);

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

        JButton sendBtn = new JButton("Send");
        JTextField cmdField = new JTextField();     // raw command entry

        sendBtn.addActionListener(e -> util.sendCommand(cmdField.getText()));
        cmdField.addActionListener(e -> sendBtn.doClick());

        JPanel commandRightPanel = new JPanel(new BorderLayout());
        JTextArea commandOutputArea = new JTextArea();

        commandRightPanel.setPreferredSize(new Dimension(300, 0));
        commandRightPanel.setBorder(BorderFactory.createTitledBorder("Backend Output"));
        commandOutputArea.setEditable(false);
        commandOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandRightPanel.add(new JScrollPane(commandOutputArea), BorderLayout.CENTER);

        // Add elements to GUI Panel
        JPanel south = new JPanel(new BorderLayout(3,3));
        south.add(cmdField, BorderLayout.CENTER);
        south.add(sendBtn,  BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);
        add(util.wrapWithRightPanel(panel, commandRightPanel));
    }
    private void setConfigs(JTextArea console, String host, String name, int port){
        CONSOLE = console;
        HOST = host;
        NAME = name;
        PORT = port;
    }

//    private void sendCommand(JTextArea console, String HOST, int PORT, String cmd){
//        if(cmd==null||cmd.isBlank()) return;
//        append(console, "\n> "+cmd);
//        try(Socket s=new Socket(HOST,PORT);
//            OutputStream o=s.getOutputStream();
//            BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream()))){
//            o.write((cmd+"\n").getBytes(StandardCharsets.UTF_8)); o.flush();
//            String line; while((line=in.readLine())!=null) append(console, line);
//        }catch(IOException ex){ append(console, "[ERR] "+ex.getMessage()); }
//    }
//    private JPanel wrapWithRightPanel(JPanel main, JPanel side) {
//        JPanel wrapper = new JPanel(new BorderLayout());
//        wrapper.add(main, BorderLayout.CENTER);
//        side.setPreferredSize(new Dimension(300, 0));
//        wrapper.add(side, BorderLayout.EAST);
//        return wrapper;
//    }
//    private void append(JTextArea console, String txt){
//        SwingUtilities.invokeLater(() -> {
//            console.append(txt+"\n");
//            console.setCaretPosition(console.getDocument().getLength());
//        });
//    }
}
