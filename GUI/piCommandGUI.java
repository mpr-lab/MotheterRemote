import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class piCommandGUI extends JFrame {
   private JTextField inputField;
   private JTextArea console;
   private JButton sendButton, rsyncButton, killButton, uiButton, helpButton, clearButton;

   private static final String HOST = "buddy"; // or your host IP
   private static final int PORT = 12345; // match Python's host_server

   public piCommandGUI() {
       setTitle("RPi Command Center");
       setSize(700, 450);
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       setLayout(new BorderLayout());

       inputField = new JTextField();
       console = new JTextArea();
       console.setEditable(false);
       console.setFont(new Font("Monospaced", Font.PLAIN, 12));

       sendButton = new JButton("Send");
       sendButton.addActionListener(e -> sendCommand(inputField.getText()));

       JPanel inputPanel = new JPanel(new BorderLayout());
       inputPanel.add(inputField, BorderLayout.CENTER);
       inputPanel.add(sendButton, BorderLayout.EAST);

       JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

       // Prebuilt command buttons
       rsyncButton = new JButton("rsync");
       killButton = new JButton("kill");
       uiButton = new JButton("ui");
       helpButton = new JButton("help");
       clearButton = new JButton("Clear");

       rsyncButton.addActionListener(e -> sendCommand("rsync"));
       killButton.addActionListener(e -> sendCommand("kill"));
       uiButton.addActionListener(e -> sendCommand("ui"));
       helpButton.addActionListener(e -> sendCommand("help"));
       clearButton.addActionListener(e -> console.setText(""));

       // Add buttons to panel
       commandPanel.add(rsyncButton);
       commandPanel.add(killButton);
       commandPanel.add(uiButton);
       commandPanel.add(helpButton);
       commandPanel.add(clearButton);

       add(commandPanel, BorderLayout.NORTH);
       add(new JScrollPane(console), BorderLayout.CENTER);
       add(inputPanel, BorderLayout.SOUTH);

       inputField.addActionListener(e -> sendCommand(inputField.getText()));
   }

   private void sendCommand(String command) {
       if (command == null || command.isEmpty()) return;

       try (Socket socket = new Socket(HOST, PORT);
            OutputStream output = socket.getOutputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

           output.write((command + "\n").getBytes("UTF-8"));
           output.flush();

           console.append("> " + command + "\n");

           // Read response
           String line;
           while ((line = input.readLine()) != null) {
               console.append(line + "\n");
           }

       } catch (IOException e) {
           console.append("Error: " + e.getMessage() + "\n");
       }

       inputField.setText("");
   }

   public static void main(String[] args) {
       SwingUtilities.invokeLater(() -> {
           piCommandGUI gui = new piCommandGUI();
           gui.setVisible(true);
       });
   }
}