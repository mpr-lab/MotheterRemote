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

public class BuildGUI extends JFrame{
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
    private static final int PORT = 12345;

    private final JTextArea  console  = new JTextArea();      // running log / output

    private BuildGUI(String hostAddr) {
        super("MotheterRemote");
        this.HOST = hostAddr;

        /* layout root */
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 560);
        setLayout(new BorderLayout());

        /* tabbed pane (center) */
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("RPi Command Center", new RPiCommandTab(console, HOST, PORT));
        tabs.addTab("Sensor Command Center", new SensorCommandTab(console, HOST, PORT));
        tabs.addTab("Data Sync", new DataTab(console, HOST, PORT));
        tabs.addTab("Settings", new SettingsTab(console, HOST, PORT));
        tabs.addTab("?", new HelpTab());
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        add(tabs, BorderLayout.CENTER);
        add(buildConsolePanel(), BorderLayout.SOUTH);

        startPythonBackend();
        SwingUtilities.invokeLater(() -> sendCommand("reload-config"));
    }
    private JPanel buildConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setPreferredSize(new Dimension(0, 150));

        JButton clear = new JButton("Clear log");
        clear.addActionListener(e -> console.setText(""));

        // Toggle button for minimizing/maximizing the console log
        JButton toggleButton = new JButton("Minimize");
        toggleButton.addActionListener(e -> toggleConsoleVisibility(scroll, toggleButton));

        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(clear, BorderLayout.EAST);
        p.add(toggleButton, BorderLayout.WEST); // Add toggle button to the panel

        return p;
    }

    private void toggleConsoleVisibility(JScrollPane scrollPane, JButton toggleButton) {
        if (scrollPane.isVisible()) {
            scrollPane.setVisible(false);
            toggleButton.setText("Restore");
        } else {
            scrollPane.setVisible(true);
            toggleButton.setText("Minimize");
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

    public static void main(String[] args) {
        String hostAddr = "buddy-surface";
//        /*  prompt BEFORE building the GUI */
//        String[] info = promptForHostInfo();
//        if(info==null){ System.exit(0); }
//
//        if(!initialWriteConfigs(info[0],info[1])) System.exit(0);

        SwingUtilities.invokeLater(() -> {
            BuildGUI gui = new BuildGUI(hostAddr);  // pass host_addr
            gui.setVisible(true);

        });
    }
}
