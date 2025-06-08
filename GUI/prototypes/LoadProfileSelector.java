package prototypes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LoadProfileSelector {
    public static void showAndUpdateConfigs() {
        File profileDir = new File("profiles");
        if (!profileDir.exists() || !profileDir.isDirectory()) {
            JOptionPane.showMessageDialog(null, "No profiles found in 'profiles/' directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] files = profileDir.listFiles((dir, name) -> name.endsWith("_profile.properties"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(null, "No valid profile files found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] profileNames = Arrays.stream(files)
                .map(file -> file.getName().replace("_profile.properties", ""))
                .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                null,
                "Choose a Raspberry Pi profile:",
                "Select RPi Profile",
                JOptionPane.QUESTION_MESSAGE,
                null,
                profileNames,
                profileNames[0]);

        if (selected != null) {
            File profileFile = new File(profileDir, selected + "_profile.properties");
            Properties props = new Properties();
            try (FileReader reader = new FileReader(profileFile)) {
                props.load(reader);
                String rpiName = props.getProperty("rpi_name");
                String rpiAddr = props.getProperty("rpi_addr");

                Utility util = new Utility();
                util.updateConfigsPy("", "", rpiName, rpiAddr);

                JOptionPane.showMessageDialog(null, "Profile '" + selected + "' loaded successfully.");

                // Launch the main GUI
                SwingUtilities.invokeLater(() -> {
                    BuildGUI gui = new BuildGUI(rpiAddr, rpiName);
                    gui.setVisible(true);
                });

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Failed to load profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void launchWithProfile(String rpiName, String rpiAddr) {
        Utility util = new Utility();
        util.updateConfigsPy("", "", rpiName, rpiAddr);

    }
}
