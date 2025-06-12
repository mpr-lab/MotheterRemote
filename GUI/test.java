import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class test {
    private JPanel panel1;
    private JTree fileDirectory;
    private JButton openFolderButton;
    private JTextPane textPane1;
    private JButton refreshButton;
    private JLabel Data_Tab;

    public test() {
        openFolderButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
            }
        });
    }
}
