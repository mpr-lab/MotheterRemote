import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class SettingsTab extends JPanel {
    private final Utility util;
    private final DefaultListModel<String> profileListModel = new DefaultListModel<>();
    private final JList<String> profileList = new JList<>(profileListModel);
    private final JTextField rpiNameField = new JTextField();
    private final JTextField rpiAddrField = new JTextField();
    private final Properties currentProps = new Properties();
    private String currentProfile;

    public SettingsTab(Utility util) {
        this.util = util;
        setLayout(new BorderLayout());

        // Left panel: profile list and add button
        JPanel leftPanel = new JPanel(new BorderLayout());
        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadProfile(profileList.getSelectedValue());
            }
        });
        JScrollPane listScroll = new JScrollPane(profileList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel rpis = new JLabel("Raspberry Pi Profiles:");
        topLeft.add(rpis);

        JButton addButton = new JButton("+");
        addButton.setToolTipText("Add new profile");
        addButton.addActionListener(e -> addProfile());
        JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomLeft.add(addButton);
        leftPanel.add(topLeft, BorderLayout.NORTH);
        leftPanel.add(bottomLeft, BorderLayout.SOUTH);

        // Right panel: profile details and action buttons
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rightPanel.add(new JLabel("RPi Name (rpi_name):"));
        rightPanel.add(rpiNameField);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        rightPanel.add(new JLabel("RPi Address (rpi_addr):"));
        rightPanel.add(rpiAddrField);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveProfile());
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteProfile());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(buttonPanel);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);

        loadAllProfiles();
    }

    private void loadAllProfiles() {
        profileListModel.clear();
        File profileDir = new File(util.getProfileSaveDirFromConfig().toString());
        if (!profileDir.exists()) return;

        String[] profileNames = profileDir.list((dir, name) -> name.endsWith("_profile.properties"));
        if (profileNames != null) {
            Arrays.stream(profileNames)
                    .map(name -> name.replace("_profile.properties", ""))
                    .sorted()
                    .forEach(profileListModel::addElement);
        }
    }

    private void loadProfile(String profileName) {
        if (profileName == null || profileName.isEmpty()) return;

        currentProfile = profileName;
        File profileFile = new File("profiles", profileName + "_profile.properties");
        try (FileReader reader = new FileReader(profileFile)) {
            currentProps.clear();
            currentProps.load(reader);
            rpiNameField.setText(currentProps.getProperty("rpi_name", ""));
            rpiAddrField.setText(currentProps.getProperty("rpi_addr", ""));
            util.append("[Settings] Loaded profile: " + profileName);
        } catch (IOException ex) {
            util.append("[Error] Failed to load profile: " + ex.getMessage());
        }
    }

    private void saveProfile() {
        if (currentProfile == null) {
            util.append("[Error] No profile selected.");
            return;
        }

        String rpiName = rpiNameField.getText().trim();
        String rpiAddr = rpiAddrField.getText().trim();

        if (!rpiName.isEmpty() && !rpiAddr.isEmpty()) {
            currentProps.setProperty("rpi_name", rpiName);
            currentProps.setProperty("rpi_addr", rpiAddr);
            File profileFile = new File("profiles", currentProfile + "_profile.properties");
            try (FileWriter writer = new FileWriter(profileFile)) {
                currentProps.store(writer, null);
                util.append("[Settings] Profile saved: " + currentProfile);
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
                profileListModel.addElement(newProfileName);
                profileList.setSelectedValue(newProfileName, true);
                util.append("[Settings] New profile added: " + newProfileName);
                BuildGUI.refreshProfileList();

            } catch (IOException ex) {
                util.append("[Error] Failed to add profile: " + ex.getMessage());
            }
        }
    }

    private void deleteProfile() {
        if (currentProfile == null) {
            util.append("[Error] No profile selected to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure? This cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            File profileFile = new File("profiles", currentProfile + "_profile.properties");
            if (profileFile.exists() && profileFile.delete()) {
                util.append("[Settings] Profile deleted: " + currentProfile);
                profileListModel.removeElement(currentProfile);
                rpiNameField.setText("");
                rpiAddrField.setText("");
                currentProfile = null;
                BuildGUI.refreshProfileList();
            } else {
                util.append("[Error] Failed to delete profile file.");
            }
        }
    }
}