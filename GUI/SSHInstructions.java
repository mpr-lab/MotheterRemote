import javax.swing.*;
import java.awt.*;

public class SSHInstructions extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private Utility util = new Utility();
    public SSHInstructions(Utility util) {
        super("SSH Setup Instructions:");
        util = this.util;
        setSize(600, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
//        String os = util.getDetectedOSType();

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

//        switch(os){
//            case "windows" -> {
                setup_windows windows = new setup_windows(util);
                cardPanel.add(windows.buildSSH_Step1());
                cardPanel.add(windows.buildSSH_Step2());
                cardPanel.add(windows.buildSSH_Step3());
                cardPanel.add(windows.buildSSH_Step4());
//            }
//            case "linux", "mac"   -> {
//                setup_linux linux = new setup_linux(util);
//                cardPanel.add(linux.buildSSH_Step1());
//                cardPanel.add(linux.buildSSH_Step2());
//                cardPanel.add(linux.buildSSH_Step3());
//                cardPanel.add(linux.buildSSH_Step4());
//            }
//        }

        add(cardPanel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel();
        JButton back = new JButton("Back");
        JButton next = new JButton("Next");

        back.addActionListener(e -> cardLayout.previous(cardPanel));
        next.addActionListener(e -> cardLayout.next(cardPanel));

        navPanel.add(back);
        navPanel.add(next);
        add(navPanel, BorderLayout.SOUTH);
    }
}
