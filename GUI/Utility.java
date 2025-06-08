import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        setConfigs(Console);
    }

    private void setConfigs(JTextArea console){
        CONSOLE = console;
    }

    public void loadFileList() {

        fileModel.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA_DIR)) {
            for (Path p : ds) fileModel.addElement(p.getFileName().toString());
            append("[GUI] File list loaded");
        } catch (IOException ex){ append("[GUI] Data dir error: "+ex.getMessage()); }
    }

    public void updateConfigsPy(
                                 String newRpiName,  String newRpiAddr) {
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

    public void sendCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        append("\n> " + cmd);

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", BACKEND_PATH, cmd);
            pb.redirectErrorStream(true);  // merge stderr with stdout
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                append(line);
            }

            p.waitFor();
        } catch (IOException | InterruptedException ex) {
            append("[ERR] " + ex.getMessage());
        }
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





}