# MotheterRemote GUI Documentation
A Swing-based desktop application that lets you control a Raspberry-Pi sensor system, monitor its status, adjust settings, and sync data without touching a terminal window.

---
## Features
| Area                      | What you can do                                                                                                                                          |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Start-up Prompt**       | Enter `host_name` and `host_addr`; values are written to `configs.py` before the GUI opens.                                                              |
| **RPi Command Center**    | One-click buttons for `status`, `start`, `rsync`, `kill`, plus a free-text field to send any manual command.                                             |
| **Sensor Command Center** | Graphical replacement for `ui_commands.py`. Commands are grouped into categories (combo-box) with hover tool-tips. Dialogs collect any extra parameters. |
| **Data Sync**             | Refresh or open the local data directory (`~/SQMdata`) and view its file list.                                                                           |
| **Settings**              | Edit and save `host_name`, `host_addr`, `rpi_name`, and `rpi_addr`. Updates `configs.py` and triggers a runtime config reload in the Python backend      |
| **Log Monitor**           | Scrollable console (visible in RPi Command Center) shows outgoing commands, backend replies, and Python process output.                                  |
| **Backend Autostart**     | `host_to_client.py` is launched in unbuffered mode (`python3 -u`). Its stdout/stderr stream into the GUI                                                 |
---
## Prerequisites
* Java 17+ 
* Python 3.x
* The Python backend scripts (host_to_client.py, configs.py, ui_commands.py) which should already be in the MotheterRemote repository

---
## GUI Walk-through:
### RPi Command Center
* Top row: common commands (`status`, `start`, `rsync`, `kill`) sent to backend via `sendCommand()`.
* Center: live log monitor (append-only).
* Bottom: free-text command + Send.

### Sensor Command Center
* Category combo-box filters the command list.
* Each button shows a tooltip and, when required, pops up a dialog for parameters.
* Built command strings are sent to the backend via `sendCommand()`.

### Data

### Help

### Settings
* Edit host & Pi identifiers.
* Save Settings writes four lines in `configs.py`, updates the GUI’s in-memory variables, then sends `reload-config` so `host_to_client.py` re-imports its config.

---

[//]: # (NEED TO EXPLAIN THESE BETTER AND ADD HELPER FUNCTIONS)
## Explanation of Methods
### `loadFileList()` 
refreshes file list from DATA_DIR

### `sendCommand(cmd)`
sends string over socket to backend

### `updateConfigsPy(...)`
safely edits configs.py

### `startPythonBackend()`
launches backend in a subprocess

[//]: # (### `sendStatusRequest&#40;&#41;`)
[//]: # (async status check, updates label)

