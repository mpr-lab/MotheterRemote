# buddy's GUI development notes
//    ps -ef | grep python



## why java?
### Cross-Platform Compatibility
Runs on Windows, macOS, and Linux without needing to rewrite the UI code.

Ideal when the GUI needs to be used from different machines or OSes to control the Raspberry Pi.

### strong UI toolkit with swing
Swing provides a rich set of widgets (buttons, tabs, text areas, dialogs, etc.) that are sufficient for the needs of:
* Command buttons 
* Real-time logs 
* File listing and settings forms


### maintainable and modular
GUI logic is separated from backend logic (handled in Python), allowing easier development and debugging on both ends.

### widely known/easy accessible
java is commonly taught and widely used, so it's easier for other developers to maintain or extend.
It avoids forcing the user to install and configure a web server, browser-based interface, and is taught in Smith's CS curriculum so avoids having to learn a new language.

---
## rough prototype
implemented a GUI that runs the python backend silently --> had buttons for each command and printed text from the terminal into a text are

Need to make this better because it is just a glorified terminal

### how to improve?
- **tabs for each function**
  - _**Command Center**_
  - _**Settings:** update host settings or different RPi's maybe?_
  - _**Viewing data:** view data from rsync?_
- **real-time status updates?**
- **filter log by type**
    - _errors_
    - _info_
    - _etc?_
- **progress bar for rsync**

---
## prototype 2
### Implemented Tabs:
##### Command Center
* you can use the buttons to send commands (hover to see what buttons do)
* serial monitor located at the bottom of the GUI panel
  * need a better way to display data...

##### Settings
* update host and rpi name + address

##### Data Sync
* not sure if files actually can be viewed from it....


---
## prototype 3 (with sensor command center)
### Updating Tabs and fixing buttons
I figured that having a UI button that 1 doesn't work, and 2 just runs in the terminal kind of defeated the purpose of having a GUI, so I implemented a new tab to control all the commands from the original `ui_commands.py` file. After renaming the tabs respectively to give some distinction between what tab does what we have the following changes:

##### Command Center &rarr; RPi Command Center
* now only has 4 commands (`start`, `status`, `kill`, `rsync`)
* the `help` command was replaced with tooltips, going to change help button to be its own tab that explains how to use the GUI rather than listing all the commands

##### Sensor Command Center
* handles everything that the `ui` command did now with an graphical interface rather than in the terminal
* drop down menu to change the category of which command you want to run (based on the structure in `ui_commands.py`)
* a separate "screen" for each option in the dropdown with buttons to run commands in their respective categories
* uses helper functions to actually run the commands
* If a command requires user input, a popup appears to enter information
    * maybe adding a popup/second popup for every command to confirm that that is the command you want to send? and something other than the log that shows that the command was either sent/not sent

##### Data Sync
* should just open the file location in file explorer/finder or whatever (don't make my own file explorer interface)

##### Settings
* everything is the same,, a way to update your settings

just so this exists somewhere:

### how to improve?
- **real-time status updates?** !!!
- **filter log by type** (or color coding?)
    - _errors_
    - _info_
    - _etc?_
- **progress bar for rsync**
- **Revamping the load in screen**
    - _adding auto get ip info and stuff_
    - _dropdown to switch between connection type_
    - _known pis? if multiple pis, a dropdown to switch between them_
    - also updating settings page to account for all that
- **Better way to display feedback from terminal**
---
## prototype 4
### still making small adjustments to tabs
##### RPi Command Center
* added a pane on the right side of the GUI to better display information, still kinda confused on what direction to go, but going to work with Skye hopefully to make this work

##### Sensor Command Center
* Implemented right side panel instead of popup windows for easier flow
* added a popup window with 2 second time that tells user that the command was sent &rarr; should solve the problem of user sending too many commands too quickly
* fixed some of the user inputs that had text areas where a dropdown would make more sense
* confirmed that commands run properly

##### Data Sync
* everything is the same

##### Settings
* everything is the same

##### Help
* added a new help tab which will go more in depth on each of the command center tabs and how to use the GUI as a whole
* dropdown menu to access pages on more specific parts of the help center:
  * General
  * Rpi Command
  * Sensor Command
  * Data Sync
  * Settings

### how to improve?
* **restructuring project files to make them more manageable**

---
## prototype 5
Restructured the GUI: each tab is its own java file:
### `GUI.java`
Contains the constructor for the full GUI

### `RPiCommandTab.java`
Contains the constructor for the RPi Command Center Tab

### `prototypes.SensorCommandTab.java`
Contains the constructor for the Sensor Command Center Tab

### `DataTab.java`
Contains the constructor for the Data Tab

### `SettingsTab.java`
Contains the constructor for the Settings Tab

### `HelpTab.java`
Contains the constructor for the Help Tab

### `Utility.java`
Contains the utility methods used across all GUI tabs

---
## prototype 6
### Setup Wizard

### RPi Profiles

---
## ?
how to make status update regularly

how to package the gui into an executable





---
## Packaging
* [JSmooth](https://jsmooth.sourceforge.net/)
* [Jar2exe](https://www.jar2exe.com/)





---
## logs

### 5/27/2025
Looked through all the code from spring semester, refamiliarized with what I was doing and how motheter remote works

started trying to fix GUI connection issue

---
### 5/28/2025
Still trying to fix connection issue

GUI only sends commands and shows what commands were sent, does not print was is shown when running `host_to_client.py` in the terminal... need to fix that

tried implementing a way to automatically read in configs properties from `configs.py`
* made `readConfigs.py` file to dump all the info from `configs.py` into a `config.properties`
* GUI should be able to yoink all the info from `config.properties` so user doesn't have to manually input their _addr_ and _name_ into `configs.py`
* for some reason the location of `config.properties` matters and idk where it should be placed?

---
### 5/29/2025
Fixed host name problem, alot of errors coming from `host_to_client.py` not supporting the GUI so I had to change some stuff so that it would

Got GUI to print what comes through in the terminal

Finished debugging connection issues --> fixed the big ssh key and ssh missing but not (??) on my laptop

decided to scrap whole `config.properties` debacle &rarr; have a designated place in the GUI for user to update their stuff, no need to access code.
Added inputting host name and ip screen which also updates `configs.py` file when starting up GUI
* need to figure out how to make the `host_to_client.py`

Figured out how to make it update using:
```python
import importlib
importlib.reload()
```
which when called with `configs` basically reloads the whole library effectively grabbing the new values that were changed in `configs`

#### started revamping GUI
added tabs for specific functions in the GUI

\
\
\
_**TODO**_ - - - - - - - -
- fix UI Button... why does it break everything
- figure out if rsync works

---
### 5/30/2025
Started to tackle the UI errors, &rarr; decided to turn the whole `ui_command.py` file into its own GUI of sorts and bonk that into its own tab:
* proposed structure of sensor UI tab:
  * dropdown menu for each category
  * buttons within each dropdown
  * when user input needed, a popup appears
  * tooltips

For the UI tab I had to make separate helper functions for every command... is there a better way to tackle this? (prop ya lol)

Started making documentation for the GUI,, still need to in depth explanation how all the methods work.
Also started adding javadoc comments to the GUI and overall polishing/making code easier to read &rarr; probably would be good to comment a little more...

I want to add a help/about button that has some of the documentation/how to use the GUI just in case the user needs help?? also there must be a better way to display the status and whatever whatever that comes out of the terminal.

\
\
\
_**TODO**_ - - - - - - - - 
* Next work day: work on adding the help button,, use the weekend to brainstorm what would be a good way to display status data
* Start thinking about how to implement Skye's auto ip yoink script so that we can bypass the whole input host_addr and host_name thing and think about wifi/ethernet/cellular dropdown thingy.

---
### 6/2/2025
Changed the popup windows for Sensor Commands to a panel on the right side of the GUI. Updates some of the helper function windows to flow a little better:
* interval/threshold &rarr; set interval period
  * unit as dropdown, value all in one step
* data logging cmds &rarr; set interval period
  * unit as dropdown, value all in one step

Implemented those changes as follows:

**Right Pane: Dynamic Command Panel**
- Appears when a command is selected from the left 
- Contents change depending on the command type

**Updated interface for user input:**
- ##### 1. Set Interval Period / Set Logging Interval
  - **Fields:**
    - `Interval Value:` `[_________]`
    - `Units:` `[ms ▼]`
  - **Send Button:**
    - `[ Send Command ]`
- ##### 2. Set Threshold (Temperature / Humidity)
  - **Fields:**
    - `Threshold Value:` `[_________]`
    - `Units:` (optional; °C or % if needed)
  - **Send Button:**
    - `[ Send Command ]`
- ##### 3. Start/Stop Monitoring, Enable/Disable Logging
  - **Display:**
    - Short description
    - Confirm button

  - **Example:**
    > Press "Send Command" to start sensor monitoring.

  - **Send Button:**
    - `[ Send Command ]`

- ##### 4. Query Sensor Data
  - **Display:**
    - Description: "Fetch current temperature and humidity from the Raspberry Pi."
  - **Send Button:**
    - `[ Send Command ]`


\
\
\
_**TODO**_ - - - - - - - -
- Need to add a popup alert to notify user if command was sent or not sent with 2 second timer to prevent user from sending too many commands too fast.
- code initial setup wizard that saves users addr and name as well as any raspberry pis that are set up as well, update settings tab to accomodate
- code help tab that explains how to use the GUI
- still fix the RPi command center tab to display data better... not sure how to do that still

---
### 6/3/2025
Talked to Skye about the setup wizard... have big plans for this but not sure how to implement

Trying to move away from using socket in the backend to using SSH, need to update `sendCommand()` to accomodate the changes

Added a Help tab:
* dropdown menu to choose what to help with
  * general help: how GUI works as a whole
  * RPi command help: how to use the RPi Command Center Tab
    * add an in depth section on how each button works 
    * hopefully add a troubleshooting section
  * Sensor command help: how to use
    * documentation on what each button does in the sensor UI and how to use them/ when to use them
    * add a troubleshooting section
  * Data Sync Help:
  * Settings Help:
    * add a troubleshooting section

Trying and failing to restructure GUI so that each tab is its own java file (right now `prototypes.piCommandGUI.java` is over 1000 lines of code) &rarr; this would make maintenance a lot easier and make everything more modular.

**Problems right now:**
* moving everything to separate java files kinda fucks with the console connection
* trying to find a better way to consolidate code and not have a bunch of duplicate lines (maybe using a utility file?)
* mostly the connection issue, and everything is currently going wrong with the sensor page
* other pages mostly work? need to rework the right side pane thing


\
\
\
_**TODO**_ - - - - - - - -
* continue work shopping help center
* **restructuring code**

---
### 6/4/2025
Restructured everything (this took a while)

---
### 6/5/2025
common methods now contained in `Utility.java`
worked on help tab
rewrote `sendCommand()` to now work with the ssh-based backend
---
### 6/6/2025
I was sick... started trying to build out setup wizard

---
### 6/8/2025
Setup wizard main framework
packaging

---
### 6/9/2025
more to setup wizard, added save and load progress, making new changes compatible with GUI:
* multiple rpi profiles, can switch between them with a dropdown menu
* can add/delete profiles from settings tab

Did a lot of work the help tab:
* general instructions pretty much done
* rpi instructions pretty much done
* sensor in progress
* data in progress
* settings instructions done

---
### 6/10/2025
added hide/show console button and made it so that the console starts minimized

added a toast popup at the bottom of the GUI that confirms to the user if a command was sent
* separate colors for state of command? (red &rarr; error, green &rarr; success)
 
Coded out auto ssh setup that automatically generate a new ssh key if the user doesn't have it already then copies it to the raspberry pi using `sshpass`. If prompted for the password, a popup window will appear where user can type in their password

\
\
\
_**TODO**_ - - - - - - - -
* continue workshopping help center
* finish tailscale and radio instructions
* display information about status and about rsync

---
### 6/11/2025
got rid of connection page: that can all be configured automatically with `auto_setup.py`
* added functionaliy to autosetup, the setup wizrd gives different instructions based on the operating system that the host computer is using
* need to have a fallback just incase auto setup is incorrect: maybe a separate button that shows all instructions?

auto ssh function poses secuirity threat, switched to step-by-step instructions for ssh. updated ssh instructions to have each step be on a page. Wrote tailscale instructions and figured out logic for inserting cardpanels only when button is selected

combined RPiCommandCenter and SensorCommandCenter to be on one single tab, did not make sense for them to be separated because you need would need to switch screens to access rsync and such buttons which would become annoying when trying fetch data.
* thinking about maybe adding a function that automatically sends rsync after like 10 seconds when user runs any sensor command 
* need to update the help tab to accommodate this change

\
\
\
  _**TODO**_ - - - - - - - -
* continue work help center
* radio instructions
* display information about status and about rsync
* data tab fix





project-root/
│
├── src/
│   └── main/
│       ├── java/               ← Java GUI source
│       │   └── GUI/
│       │       ├── profiles/
│       │       │   ├── host_config.properties
│       │       │   └── pi_profile.properties
│       │       │
│       │       ├── BuildGUI.java
│       │       ├── RpiCommandTab.java
│       │       ├── SettingsTab.java
│       │       ├── DataTab.java
│       │       ├── DataTab.java
│       │       ├── HelpTab.java
│       │       ├── SetupWizard.java
│       │       └── Utility.java
│       │
│       └── resources/          ← Python scripts, profiles/, configs.py
│           ├── modem/
│           │   ├── test.py
│           │   └── ssh.sh
│           │
│           ├── Py3SQM/
│           │
│           ├── ssh/
│           │   ├── auto_setup.py
│           │   ├── configs_ssh.py
│           │   ├── first_time_setup.py
│           │   ├── host_ssh.py
│           │   ├── lora_child_ssh.py
│           │   ├── lora_parent_ssh.py
│           │   ├── rpi_ssh.py
│           │   ├── sensor_ssh.py
│           │   ├── sensor_stream.py
│           │   ├── ui_commands.py
│           │   ├── radio_runner.sh
│           │   ├── rpi_runner.sh
│           │   └── sensor_runner.sh
│           │
│           └── scripts/
│               ├── runradio.sh
│               ├── runrpi.sh
│               ├── runsensor.sh
│               └── cronjobs.txt
│
│
├── pom.xml                    ← Add Maven build settings and plugins here
├── setup/                     ← Optional: bundled Python files / installer scripts
└── target/                    ← Output .jar/.exe



jpackage --type dmg --name MotheterRemote --input app --main-jar GUIProject-1.0-SNAPSHOT.jar --main-class GUI.MainClass --dest output --app-version 1.0 --vendor "mpr-lab"--verbose
