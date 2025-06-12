import javax.swing.*;
import java.awt.*;
public class setup_linux {
    private final Utility util;
    String detectedOS = "";

    public setup_linux(Utility util){
        this.util = util;
    }
        JPanel buildSSH_Step1(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 1: CHECK IS SSH INSTALLED< IF IT's NOT, INSTALL IT
        JPanel step1 = new JPanel();
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));
        step1.add(new JLabel("Step 1: Check if SSH is installed"));
        step1.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI1 = util.buildTextArea(step1, 45);
        copyI1.setText("Open up a new terminal. First, check whether or not you have ssh installed. This looks different on different operating systems. Your operating system is "+ detectedOS +". If that does not seem right....");
        String sshCmd = "systemctl status sshd";
        JPanel checkSSHRow = util.buildCopyRow(sshCmd, 30);

        JTextArea proceedI1 = util.buildTextArea(step1, 45);
        proceedI1.setText("For linux users, there should be a line printed in the output of the terminal after running the first command that says 'Active: active (running)' if SSH is active. If this is the case, then move onto [STEP 2]. If not, move on to [STEP 2a].");


        step1.add(copyI1);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));
        step1.add(checkSSHRow);
        step1.add(Box.createRigidArea(new Dimension(0, 10)));
        step1.add(proceedI1);
        step1.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 1a: DOWNLOAD SSH IF NOT DOWNLOADED
        JPanel step1a = new JPanel();
        step1a.setLayout(new BoxLayout(step1a, BoxLayout.Y_AXIS));
        step1a.add(new JLabel("Step 1a: Download SSH"));
        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea download = util.buildTextArea(step1, 45);
        download.setText("""
            If the terminal says anything along the lines of "Unit file sshd.service does not exist", then you do not have ssh installed. Run the following command in your terminal to install SSH:
            """);
        step1a.add(download);
        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

        String downloadCmd = "sudo apt install openssh-server openssh-client";
        JPanel downloadSSHRow = util.buildCopyRow(downloadCmd, 30);
        step1a.add(downloadSSHRow);
        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea enable = util.buildTextArea(step1, 45);
        enable.setText("""
            It may be the case that SSH is installed but is not active. To enable SSH, run the following command in your terminal:
            """);
        step1a.add(download);
        step1a.add(Box.createRigidArea(new Dimension(0, 10)));

        String enableCmd = "sudo systemctl start ssh";
        JPanel enableSSHRow = util.buildCopyRow(enableCmd, 30);
        step1a.add(enableSSHRow);
        step1a.add(Box.createRigidArea(new Dimension(0, 20)));

        // STEP 1b: VERIFY SSH DOWNLOAD
        JPanel step1b = new JPanel();
        step1b.setLayout(new BoxLayout(step1b, BoxLayout.Y_AXIS));
        step1b.add(new JLabel("Step 1b: Verify that SSH was installed"));
        step1b.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI1b = util.buildTextArea(step1b, 45);
        copyI1b.setText("Now that you have downloaded/enabled OpenSSH, verify whether or not you have ssh installed. run the following command again:");
        JPanel checkbSSHRow = util.buildCopyRow(sshCmd, 30);

        JTextArea proceedI1b = util.buildTextArea(step1b, 45);
        proceedI1b.setText("Now, there should be a line printed in the output of the terminal that says 'Active: active (running)'. proceed to [STEP 2].");

        step1b.add(copyI1b);
        step1b.add(Box.createRigidArea(new Dimension(0, 10)));
        step1b.add(checkbSSHRow);
        step1b.add(Box.createRigidArea(new Dimension(0, 10)));
        step1b.add(proceedI1b);
        step1b.add(Box.createRigidArea(new Dimension(0, 30)));

        inner.add(step1);
        inner.add(step1a);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    JPanel buildSSH_Step2(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 2 GENERATE SSH KEY
        JPanel step2 = new JPanel();
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));
        step2.add(new JLabel("Step 2: SSH Key"));
        step2.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI2 = util.buildTextArea(step2, 60);
        copyI2.setText("Now that SSH is downloaded, you must establish yourself as a known host for the RPi. This will allow you to connect remotely to the RPi without having to enter a password. First, let's check to see if you already have an SSH key. Run the following command in your terminal:");

        String checkCmd = "ls -al ~/.ssh";
        JPanel checkKeySSHRow = util.buildCopyRow(checkCmd,30);

        JTextArea proceedI2 = util.buildTextArea(step2, 30);
        proceedI2.setText("If there is a file called id_ed_25519, then you already have an SSH key. Proceed to [STEP 3]. If you do not have a file called id_ed_25519, proceed to [STEP 2a].");

        step2.add(copyI2);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(checkKeySSHRow);
        step2.add(Box.createRigidArea(new Dimension(0, 10)));
        step2.add(proceedI2);
        step2.add(Box.createRigidArea(new Dimension(0, 30)));

        // STEP 2 GENERATE SSH KEY
        JPanel step2a = new JPanel();
        step2a.setLayout(new BoxLayout(step2a, BoxLayout.Y_AXIS));
        step2a.add(new JLabel("Step 2a: Generate a SSH Key"));
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));

        JTextArea copyI2a = util.buildTextArea(step2, 30);
        copyI2a.setText("If the SSH key does not exist yet, we must generate one to be copied to the RPi. Run the following command into your terminal:");

        String generateCmd = "ssh-keygen -t ed25519";
        JPanel genSSHRow = util.buildCopyRow(generateCmd, 30);

        step2a.add(copyI2a);
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));
        step2a.add(genSSHRow);
        step2a.add(Box.createRigidArea(new Dimension(0, 10)));

        // Add to Panel
        inner.add(step2);
        inner.add(step2a);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }

    JPanel buildSSH_Step3(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // STEP 3: COPY SSH KEY
        JPanel step3 = new JPanel();
        step3.setLayout(new BoxLayout(step3, BoxLayout.Y_AXIS));
        step3.add(new JLabel("Step 3: Copy SSH Key to Each Raspberry Pi"));
        step3.add(Box.createRigidArea(new Dimension(0, 10)));


        JTextArea copyI3 = util.buildTextArea(step3, 30);
        copyI3.setText("Next, we must copy the key that you just generated over to the RPi. In the terminal, run the following command:");

        String copyCmd = "ssh-copy-id <rpi_name>@<rpi_addr>";
        JPanel copySSHRow = util.buildCopyRow(copyCmd, 30);

        JTextArea changeI3 = util.buildTextArea(step3, 30);
        changeI3.setText("Make sure to change <rpi_name> and <rpi_addr> with the correct information. You may need to input the RPi's password on this step.");

        step3.add(copyI3);
        step3.add(Box.createRigidArea(new Dimension(0, 10)));
        step3.add(copySSHRow);
        step3.add(Box.createRigidArea(new Dimension(0, 10)));
        step3.add(changeI3);
        step3.add(Box.createRigidArea(new Dimension(0, 20)));

        // Add to Panel
        inner.add(step3);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);

        panel.add(scroll);
        return panel;
    }
}
