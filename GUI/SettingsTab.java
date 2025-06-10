import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class SettingsTab extends JPanel {
    private JTextArea CONSOLE;
    private final Utility util;
    private final JComboBox<String> profileSelector;
    private final JTextField rpiNameField;
    private final JTextField rpiAddrField;
    private final Properties currentProps = new Properties();

    public SettingsTab(Utility util) {
//        setConfigs(Console);
//        util = new Utility(Console);
        this.util = util;
        setSize(800, 560);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Load profile names
        File profileDir = new File("profiles");
        String[] profileNames = profileDir.list((dir, name) -> name.endsWith("_profile.properties"));
        if (profileNames == null || profileNames.length == 0) {
            profileNames = new String[] { "No profiles found" };
        } else {
            for (int i = 0; i < profileNames.length; i++) {
                profileNames[i] = profileNames[i].replace("_profile.properties", "");
            }
        }

        profileSelector = new JComboBox<>(profileNames);
        profileSelector.addActionListener(e -> loadSelectedProfile());

        rpiNameField = new JTextField();
        rpiAddrField = new JTextField();

        panel.add(new JLabel("Select RPi Profile:"));
        panel.add(profileSelector);
        panel.add(new JLabel("RPi Name (rpi_name):"));
        panel.add(rpiNameField);
        panel.add(new JLabel("RPi Address (rpi_addr):"));
        panel.add(rpiAddrField);

        JButton saveButton = new JButton("Save Profile");
        saveButton.addActionListener(e -> saveProfile());

        JButton addButton = new JButton("Add Profile");
        addButton.addActionListener(e -> addProfile());

        JButton deleteButton = new JButton("Delete Profile");
        deleteButton.addActionListener(e -> deleteProfile());

        panel.add(saveButton);
        panel.add(deleteButton);
        panel.add(new JLabel());
        panel.add(addButton);

        add(panel, BorderLayout.CENTER);

        if (profileSelector.getItemCount() > 0) {
            loadSelectedProfile();
        }
    }

    private void setConfigs(JTextArea console) {
        CONSOLE = console;
    }

    private void loadSelectedProfile() {
        String selected = (String) profileSelector.getSelectedItem();
        if (selected == null || selected.equals("No profiles found")) return;

        File profileFile = new File("profiles", selected + "_profile.properties");
        try (FileReader reader = new FileReader(profileFile)) {
            currentProps.clear();
            currentProps.load(reader);
            rpiNameField.setText(currentProps.getProperty("rpi_name", ""));
            rpiAddrField.setText(currentProps.getProperty("rpi_addr", ""));
        } catch (IOException ex) {
            util.append("[Error] Failed to load profile: " + ex.getMessage());
        }
    }

    private void saveProfile() {
        String selected = (String) profileSelector.getSelectedItem();
        if (selected == null || selected.equals("No profiles found")) return;

        String rpiName = rpiNameField.getText().trim();
        String rpiAddr = rpiAddrField.getText().trim();

        if (!rpiName.isEmpty() && !rpiAddr.isEmpty()) {
            currentProps.setProperty("rpi_name", rpiName);
            currentProps.setProperty("rpi_addr", rpiAddr);
            File profileFile = new File("profiles", selected + "_profile.properties");
            try (FileWriter writer = new FileWriter(profileFile)) {
                currentProps.store(writer, null);
                util.append("[Settings] Profile updated: " + selected);
                util.updateConfigsPy(rpiName, rpiAddr);
            } catch (IOException ex) {
                util.append("[Error] Failed to save profile: " + ex.getMessage());
            }
        } else {
            util.append("[Error] One or more fields are empty.");
        }
    }

    private void addProfile() {
        String newProfileName = JOptionPane.showInputDialog(this, "Enter new profile name:");
        if (newProfileName != null && !newProfileName.trim().isEmpty()) {
            File newProfileFile = new File("profiles", newProfileName + "_profile.properties");
            if (newProfileFile.exists()) {
                util.append("[Error] Profile already exists.");
                return;
            }
            try (FileWriter writer = new FileWriter(newProfileFile)) {
                Properties props = new Properties();
                props.setProperty("rpi_name", "");
                props.setProperty("rpi_addr", "");
                props.store(writer, null);
                profileSelector.addItem(newProfileName);
                profileSelector.setSelectedItem(newProfileName);
                util.append("[Settings] New profile added: " + newProfileName);
            } catch (IOException ex) {
                util.append("[Error] Failed to add profile: " + ex.getMessage());
            }
        }
    }

    private void deleteProfile() {
        String selected = (String) profileSelector.getSelectedItem();
        if (selected == null || selected.equals("No profiles found")) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this profile?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            File profileFile = new File("profiles", selected + "_profile.properties");
            if (profileFile.exists() && profileFile.delete()) {
                profileSelector.removeItem(selected);
                util.append("[Settings] Profile deleted: " + selected);
                if (profileSelector.getItemCount() > 0) {
                    profileSelector.setSelectedIndex(0);
                    loadSelectedProfile();
                } else {
                    rpiNameField.setText("");
                    rpiAddrField.setText("");
                }
            } else {
                util.append("[Error] Failed to delete profile file.");
            }
        }
    }
}
