import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class BuildGUI extends JFrame {
    private final JTextArea console = new JTextArea();
    private final Map<String, GUITab> tabMap = new LinkedHashMap<>();
    private final static String HOST = "127.0.0.1";
    private final static int PORT = 12345;

    public BuildGUI() {
        super("MotheterRemote");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        // Initialize tabs with shared backend functions
        tabMap.put("RPi Command Center", new RPiCommandTab());
        tabMap.put("Sensor Command Center", new SensorCommandTab());
        tabMap.put("Data Sync", new DataTab());
        tabMap.put("Settings", new SettingsTab());
        tabMap.put("Help", new HelpTab());

        // Create tabbed pane and wrapper
        JTabbedPane tabs = new JTabbedPane();
        JPanel centerWrapper = new JPanel(new BorderLayout());

        for (Map.Entry<String, GUITab> entry : tabMap.entrySet()) {
            tabs.addTab(entry.getKey(), entry.getValue().getMainPanel());
        }

        JPanel dynamicRightPanel = new JPanel(new BorderLayout());
        dynamicRightPanel.setPreferredSize(new Dimension(300, 0));

        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            for (Map.Entry<String, GUITab> entry : tabMap.entrySet()) {
                if (entry.getValue().getMainPanel() == selected) {
                    JPanel right = entry.getValue().getSidePanel();
                    centerWrapper.remove(dynamicRightPanel);
                    if (right != null) {
                        dynamicRightPanel.removeAll();
                        dynamicRightPanel.add(right, BorderLayout.CENTER);
                        centerWrapper.add(dynamicRightPanel, BorderLayout.EAST);
                    }
                    centerWrapper.revalidate();
                    centerWrapper.repaint();
                    break;
                }
            }
        });

        centerWrapper.add(tabs, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);
        add(buildConsolePanel(), BorderLayout.SOUTH);
    }

    private JPanel buildConsolePanel() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setPreferredSize(new Dimension(0, 150));

        JButton clear = new JButton("Clear log");
        clear.addActionListener(e -> console.setText(""));

        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(clear, BorderLayout.EAST);
        return p;
    }

    public void append(String txt) {
        SwingUtilities.invokeLater(() -> {
            console.append(txt + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void sendCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) return;
        append("> " + cmd);
        try (Socket s = new Socket(HOST, PORT);
             OutputStream o = s.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            o.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
            o.flush();
            String line;
            while ((line = in.readLine()) != null) append(line);
        } catch (IOException ex) {
            append("[ERR] " + ex.getMessage());
        }
    }

    private void setRightPanel(JPanel panel) {
        for (GUITab tab : tabMap.values()) {
            if (tab instanceof SensorCommandTab) {
                tab.getSidePanel().removeAll();
                tab.getSidePanel().add(panel, BorderLayout.CENTER);
                tab.getSidePanel().revalidate();
                tab.getSidePanel().repaint();
                break;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BuildGUI gui = new BuildGUI();
            gui.setVisible(true);
        });
    }
}