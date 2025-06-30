//import javax.swing.*;
//import java.awt.*;
//import java.io.IOException;
//import java.nio.file.DirectoryStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//public class DataTab extends JPanel{
//    private static final Path   DATA_DIR     = Paths.get(System.getProperty("user.home"), "SQMdata");
//    private final DefaultListModel<String> fileModel = new DefaultListModel<>(); // for JList in Data tab
//
//    private JTextArea CONSOLE;      // running log / output
//    private final Utility util;
//
//
//    public DataTab(Utility util){
////        setConfigs(Console);
////        Utility util = new Utility(Console);
//        this.util = util;
//        setSize(800, 560);
//        setLayout(new BorderLayout());
//
//        JList<String> list = new JList<>(fileModel);
//        JScrollPane sp = new JScrollPane(list);
//
//        JButton refresh = new JButton("Refresh");
//        refresh.addActionListener(e -> loadFileList());
//
//        JButton openDir = new JButton("Open Folder");
//        openDir.addActionListener(e -> {
//            try { Desktop.getDesktop().open(DATA_DIR.toFile()); }
//            catch (IOException ex){ util.append("[GUI] "+ex.getMessage()); }
//        });
//
//        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        btns.add(refresh); btns.add(openDir);
//
//        JPanel root = new JPanel(new BorderLayout(5,5));
//        root.add(sp, BorderLayout.CENTER);
//        root.add(btns, BorderLayout.SOUTH);
//        loadFileList();
//
//        add(root, BorderLayout.CENTER);
//    }
//
//    private void setConfigs(JTextArea console){
//        CONSOLE = console;
//    }
//
//    private void loadFileList() {
//        fileModel.clear();
//        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DATA_DIR)) {
//            for (Path p : ds) fileModel.addElement(p.getFileName().toString());
//            util.append("[GUI] File list loaded");
//        } catch (IOException ex){ util.append("[GUI] Data dir error: "+ex.getMessage()); }
//    }
//
//
//}
//
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataTab extends JPanel {
    Utility util = new Utility();
    private static Path DATA_DIR = null;

    public DataTab(Utility util) {
        this.util = util;
        DATA_DIR = util.getSQMSaveDirFromConfig();
        setSize(800, 560);
        setLayout(new BorderLayout());

        JLabel label = new JLabel("Browse data files in: " + DATA_DIR.toString());
        JFileChooser fileChooser = new JFileChooser(DATA_DIR.toFile());
        fileChooser.setControlButtonsAreShown(false); // hides approve/cancel buttons
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        JButton openDir = new JButton("Open Folder in File Explorer");
        openDir.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DATA_DIR.toFile());
            } catch (IOException ex) {
                util.append("[GUI] " + ex.getMessage());
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(label, BorderLayout.CENTER);
        topPanel.add(openDir, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(topPanel, BorderLayout.NORTH);
        add(fileChooser, BorderLayout.CENTER);
    }
}
