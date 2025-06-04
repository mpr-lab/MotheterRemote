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

public class DataTab extends JPanel{
    /* ====  GLOBAL CONSTANTS & STATE  ==================================== */
    /* ---------------- File system paths ---------------- */
    // Path to Python‑side configuration file (relative to project root)
    private static final String CONFIG_PATH  = "../comms-GUI/configs.py";
    // Path to the Python backend we invoke with ProcessBuilder
    private static final String BACKEND_PATH = "../comms-GUI/host_to_client.py";
    // Folder on host where rsync‑ed data will be stored
    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");
    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab


    /* ---------------- Network ---------------- */
    private String HOST;   // server IP or hostname (user‑supplied)
    private String NAME;   // human‑friendly host name (user‑supplied)
    // Socket port must match configs.host_server in the Python backend
    private int PORT = 12345;
    private JTextArea CONSOLE;      // running log / output

    public DataTab(JTextArea Console, String Host, int Port){
        setSize(800, 560);
        setLayout(new BorderLayout());

        setConfigs(Console, Host, Port);

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

        add(root, BorderLayout.CENTER);
    }

    private void setConfigs(JTextArea console, String host, int port){
        CONSOLE = console;
        HOST = host;
        PORT = port;
    }

    private void loadFileList() {
        fileModel.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA_DIR)) {
            for (Path p : ds) fileModel.addElement(p.getFileName().toString());
            append("[GUI] File list loaded");
        } catch (IOException ex){ append("[GUI] Data dir error: "+ex.getMessage()); }
    }


    private void append(String txt){
        SwingUtilities.invokeLater(() -> {
            CONSOLE.append(txt+"\n");
            CONSOLE.setCaretPosition(CONSOLE.getDocument().getLength());
        });
    }

}