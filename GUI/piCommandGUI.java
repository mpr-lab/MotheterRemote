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
        tabs.addTab("RPi Command Center", buildCommandTab());
        tabs.addTab("Sensor Command Center", buildSensorTab());
        tabs.addTab("Data Sync",      buildDataTab());
        tabs.addTab("Settings",       buildSettingsTab());

        add(tabs, BorderLayout.CENTER);

        /* console panel (south) */
        add(buildConsolePanel(), BorderLayout.SOUTH);

        /* launch backend & read its output */
        startPythonBackend();

        /* important: after backend is up, tell it to reload configs */
        SwingUtilities.invokeLater(() -> sendCommand("reload-config"));
    }

/* ----  TABS  ---------------------------------------------------------------------------------------------------------------------------------------------- */

    /**
     * buildCommandTab():
     *      constructs a new JPanel object that holds everything in the RPi command tab
     *       > converts commands from host_to_client.py menu into distinct buttons
     *       > sends commands back to host_to_client.py backend
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


    /**
     * buildSensorTab():
     *      constructs a new JPanel object that holds everything in the sensor command tab
     *       > converts ui_commands.py menu into GUI
     *       > sends commands based on built-in sensor functions to RPi
     *         through host_to_client.py backend
     *
     * @return root --> a JPanel variable containing the contents of the tab
     */
    private JPanel buildSensorTab() {

        /* ===  MAP OF CATEGORIES → (Button Label, Tooltip, Command) === */
        class Cmd { String label, tip; Supplier<String> cmd; Cmd(String l,String t,Supplier<String> c){label=l;tip=t;cmd=c;}}

        Map<String,List<Cmd>> cat = new LinkedHashMap<>();

        /* 1) READINGS & INFO */
        cat.put("Readings & Info", List.of(
                new Cmd("Request Reading","requests a reading", ()->"rx"),
                new Cmd("Calibration Info","requests calibration information", ()->"cx"),
                new Cmd("Unit Info","requests unit information", ()->"ix")
        ));

        /* 2) ARM / DISARM CAL */
        cat.put("Arm / Disarm Calibration", List.of(
                new Cmd("Arm Light","zcalAx", ()->"zcalAx"),
                new Cmd("Arm Dark","zcalBx", ()->"zcalBx"),
                new Cmd("Disarm","zcalDx", ()->"zcalDx")
        ));

        /* 3) Interval / Threshold */
        cat.put("Interval / Threshold", List.of(
                new Cmd("Request Interval Settings","Sends interval setting request. Prompts two responses: reading w/ serial, and interval setting respons", ()->"Ix"),
                new Cmd("Set Interval Period","", this::promptIntervalPeriod),
                new Cmd("Set Interval Threshold","", this::promptIntervalThreshold)
        ));

        /* 4) Manual Cal */
        cat.put("Manual Calibration", List.of(
                new Cmd("Set Light Offset","manually set calibration: light offset", this::promptLightOffset),
                new Cmd("Set Light Temp","manually set calibration: light temperature", this::promptLightTemp),
                new Cmd("Set Dark Period","manually set calibration: dark period", this::promptDarkPeriod),
                new Cmd("Set Dark Temp","manually set calibration: dark temperature", this::promptDarkTemp)
        ));

        /* 5) Simulation */
        cat.put("Simulation", List.of(
                new Cmd("Request Sim Values","get simulation values", ()->"sx"),
                new Cmd("Run Simulation","runs a simulation", this::promptSimulation)
        ));

        /* 6) Data Logging Commands */
        cat.put("Data Logging Cmds", List.of(
                new Cmd("Request Pointer","L1x", ()->"L1x"),
                new Cmd("Log One Record","L3x", ()->"L3x"),
                new Cmd("Return One Record","L4…", this::promptReturnOneRecord),
                new Cmd("Set Trigger Mode","LMx", this::promptTriggerMode),
                new Cmd("Request Trigger Mode","Lmx", ()->"Lmx"),
                new Cmd("Request Interval Settings","LIx", ()->"LIx"),
                new Cmd("Set Interval Period","LPx", this::promptLogIntervalPeriod),
                new Cmd("Set Threshold","LPTx", this::promptLogThreshold)
        ));

        /* 7) Logging Utilities */
        cat.put("Logging Utilities", List.of(
                new Cmd("Request ID","L0x", ()->"L0x"),
                new Cmd("Erase Flash Chip","L2x", this::confirmEraseFlash),
                new Cmd("Battery Voltage","L5x", ()->"L5x"),
                new Cmd("Request Clock","Lcx", ()->"Lcx"),
                new Cmd("Set Clock","Lcx", this::promptSetClock),
                new Cmd("Put Unit to Sleep","Lsx", ()->"Lsx"),
                new Cmd("Request Alarm Data","Lax", ()->"Lax")
        ));

        /* ===  GUI BUILD === */
        JComboBox<String> combo = new JComboBox<>(cat.keySet().toArray(String[]::new));
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        /* Populate buttons for selected category */
        Runnable refresh = () -> {
            listPanel.removeAll();
            String key = (String) combo.getSelectedItem();
            for (Cmd c : cat.get(key)) {
                JButton b = new JButton(c.label);
                b.setToolTipText(c.tip);
                b.addActionListener(e -> {
                    String real = c.cmd.get();          // get command string (may prompt)
                    if(real!=null && !real.isBlank()){
                        sendCommand(real);
                    }
                });
                listPanel.add(b);
            }
            listPanel.revalidate();
            listPanel.repaint();
        };
        combo.addActionListener(e -> refresh.run());
        refresh.run(); // initial fill

        JPanel root = new JPanel(new BorderLayout(5,5));
        root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
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
    /* ------------------------------------------------------------------
   Below are helper pop-up methods for the few commands that need
   extra user input for the sensor UI tab.  Each returns the
   fully-formed command string (or null if the user cancels).
   ------------------------------------------------------------------ */
    private String promptIntervalPeriod(){
        JTextField val = new JTextField();
        String[] opts = {"Seconds","Minutes","Hours"};
        int unit = JOptionPane.showOptionDialog(this,val,"Interval Period – pick unit, then enter value",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,opts,opts[0]);
        if(unit<0) return null;
        if(JOptionPane.showConfirmDialog(this,val,"Enter integer value:",JOptionPane.OK_CANCEL_OPTION)!=0) return null;
        String txt = val.getText().trim();
        int t;
        try{
            t=Integer.parseInt(txt);
        }catch(Exception e){
            return null;
        }

        long seconds = switch(unit){
            case 1->t*60;
            case 2->t*3600;
            default->t;
        };

        String withZeros = String.format("%010d", seconds);
        return "p"+withZeros+"x";
    }

    private String promptIntervalThreshold(){
        String thr = JOptionPane.showInputDialog(this,"Threshold (mag/arcsec²):");
        if(thr==null||thr.isBlank()) return null;
        double d; try{ d=Double.parseDouble(thr);}catch(Exception e){return null;}
        String cmdPart = String.format("%08.2f", d).replace(' ','0');
        return "p"+cmdPart+"x";
    }

    private String confirmEraseFlash(){
        int res = JOptionPane.showConfirmDialog(this,
                "ERASE FLASH CHIP?\nThis cannot be undone.","Confirm",JOptionPane.OK_CANCEL_OPTION);
        return res==JOptionPane.OK_OPTION ? "L2x" : null;
    }

    private String promptReturnOneRecord(){
        String ptr = JOptionPane.showInputDialog(this,"Record pointer (0-9999999999):");
        if(ptr==null||!ptr.matches("\\d{1,10}")) return null;
        ptr = String.format("%010d", Long.parseLong(ptr));
        return "L4"+ptr+"x";
    }

/* ------------------------------------------------------------------
   STUB HELPERS for Manual-Cal, Simulation, Logging, etc.
   Each shows a simple dialog, validates basic input,
   and returns the full command string (or null to abort).
   ------------------------------------------------------------------ */

    /* -------- Manual Calibration -------- */
    private String promptLightOffset() {          // zcal5<value>x
        String v = JOptionPane.showInputDialog(this, "Light offset (mag/arcsec²):");
        if(v==null) return null;
        try{
            Double.parseDouble(v);
        }catch(Exception e){
            return null;
        }

        return "zcal5"+String.format("%08.2f", Double.parseDouble(v)).replace(' ','0')+"x";
    }
    private String promptLightTemp() {            // zcal6<value>x
        String v = JOptionPane.showInputDialog(this, "Light temperature (°C):");
        if(v==null) return null;
        try{
            Double.parseDouble(v);
        }catch(Exception e){
            return null;
        }

        return "zcal6"+String.format("%03.1f", Double.parseDouble(v)).replace(' ','0')+"x";
    }
    private String promptDarkPeriod(){            // zcal7<value>x
        String v = JOptionPane.showInputDialog(this, "Dark-period (s):");
        if(v==null) return null;
        try{
            Double.parseDouble(v);
        }catch(Exception e){
            return null;
        }

        return "zcal7"+String.format("%07.3f", Double.parseDouble(v)).replace(' ','0')+"x";
    }
    private String promptDarkTemp(){              // zcal8<value>x
        String v = JOptionPane.showInputDialog(this, "Dark temperature (°C):");
        if(v==null) return null;
        try{
            Double.parseDouble(v);
        }catch(Exception e){
            return null;
        }

        return "zcal8"+String.format("%03.1f", Double.parseDouble(v)).replace(' ','0')+"x";
    }

    /* -------- Simulation -------- */
    private String promptSimulation(){            // S,count,freq,temp x
        JTextField counts     = new JTextField();
        JTextField frequency  = new JTextField();
        JTextField temp       = new JTextField();
        JPanel p = new JPanel(new GridLayout(0,2,5,5));
        p.add(new JLabel("Counts:"));     p.add(counts);
        p.add(new JLabel("Frequency (Hz):")); p.add(frequency);
        p.add(new JLabel("Temperature (°C):")); p.add(temp);
        int res = JOptionPane.showConfirmDialog(this,p,"Simulation params",
                JOptionPane.OK_CANCEL_OPTION);
        if(res!=JOptionPane.OK_OPTION) return null;
        try{
            long  c = Long.parseLong(counts.getText().trim());
            long  f = Long.parseLong(frequency.getText().trim());
            int   t = (int)Double.parseDouble(temp.getText().trim());
            String sc = String.format("%010d",c);
            String sf = String.format("%010d",f);
            String st = String.format("%010d",t);
            return "S,"+sc+","+sf+","+st+"x";
        }catch(Exception e){ return null; }
    }

    /* -------- Trigger-mode (0-7) -------- */
    private String promptTriggerMode(){           // LM<mode>x
        String[] opts = {"0","1","2","3","4","5","6","7"};
        String m = (String)JOptionPane.showInputDialog(this,
                "Select trigger-mode:", "Trigger-Mode",
                JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        return (m==null)?null : "LM"+m+"x";
    }

    /* -------- Logging interval period -------- */
    private String promptLogIntervalPeriod(){     // LP[S|M]<value>x
        JTextField val = new JTextField();
        String[] units = {"Seconds","Minutes"};
        int u = JOptionPane.showOptionDialog(this,val,
                "Interval Period – choose unit, then enter value",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,
                null,units,units[0]);
        if(u<0) return null;
        if(JOptionPane.showConfirmDialog(this,val,
                "Enter integer value:",JOptionPane.OK_CANCEL_OPTION)!=0) return null;
        try{
            int v=Integer.parseInt(val.getText().trim());
            String zeros = String.format("%05d",v);
            return (u==0? "LPS": "LPM")+zeros+"x";
        }catch(Exception e){ return null; }
    }

    /* -------- Logging threshold -------- */
    private String promptLogThreshold(){          // LPT<threshold>x
        String v = JOptionPane.showInputDialog(this,"Threshold (mag/arcsec²):");
        if(v==null) return null;
        try{ Double.parseDouble(v);}catch(Exception e){ return null; }
        return "LPT"+String.format("%08.2f",Double.parseDouble(v)).replace(' ','0')+"x";
    }

    /* -------- Set Clock -------- */
    private String promptSetClock(){                // LcYYYY-MM-DD w HH:MM:SSx
        JTextField date = new JTextField();         // yyyyMMdd
        JTextField time = new JTextField();         // HHmmss
        JPanel p=new JPanel(new GridLayout(0,2,5,5));
        p.add(new JLabel("Date (YYYYMMDD):")); p.add(date);
        p.add(new JLabel("Time (HHMMSS):"));   p.add(time);
        if(JOptionPane.showConfirmDialog(this,p,"Set Clock",
                JOptionPane.OK_CANCEL_OPTION)!=0) return null;
        String d=date.getText().trim();
        String t=time.getText().trim();
        if(!d.matches("\\d{8}")||!t.matches("\\d{6}")) return null;
        /* weekday – 0 placeholder (backend may ignore) */
        String formatted = d.substring(0,4)+"-"+d.substring(4,6)+"-"+d.substring(6)
                +" 0 "+t.substring(0,2)+":"+t.substring(2,4)+":"+t.substring(4);
        return "Lc"+formatted+"x";
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

