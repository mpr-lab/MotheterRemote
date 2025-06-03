"""
Runs on host computer, sends a command and gets responses.
"""

import os
import importlib
import subprocess

# python module imports
import ui_commands
import configs

# WiFi/Ethernet connection info
rpi_addr = configs.rpi_addr
rpi_name = configs.rpi_name

# data storage and repository directories
host_data_path = configs.host_data_path
rpi_data_path = configs.rpi_data_path
rpi_repo = configs.rpi_repo

# text encoding
utf8 = configs.utf8
EOF = configs.EOF
EOL = configs.EOL
msg_len = configs.msg_len

# timing
long_s = configs.long_s
mid_s = configs.mid_s
short_s = configs.short_s

# global
allow_ui: bool = False  # whether ready to ask for user input
output: object

has_radio = True  # eventually this will be in configs


def send_to_rpi(m: str) -> str:
    m = "status"
    run_command = f"python3 {rpi_repo}/rpi_ssh.py {m}"
    s = f"ssh {rpi_name}@{rpi_addr} '{run_command}'"
    output = subprocess.check_output(s, shell=True)
    decoded = output.decode("utf-8")
    return decoded


def user_input(data: str) -> None:
    print(data)

    match data:
        case "status":
            _status()
        case "ui":
            # Match the received message to a command and act accordingly
            data = ui_commands.command_menu()
            send_to_rpi(data)  # Forward the generated command to the RPi
        case "rsync" | "sync":
            # Perform rsync to pull data from the RPi to the host
            _rsync()
        case "help":
            # Provide a help message listing valid commands
            help_msg = (
                "Commands:\n"
                "  ui   – open device UI\n"
                "  rsync|sync – copy data from sensor\n"
                "  help – this text\n"
            )
            print(help_msg)
        case "reload-config":
            importlib.reload(configs)
        case _:
            # Default case: treat message as a raw command to send to the RPi
            send_to_rpi(data)


def _status() -> None:
    output = send_to_rpi("status")
    if "AOK" in output:
        print("RPi is responding")
    else:
        print(f"RPi might not be responding properly:\n{output}")


def _ui_loop() -> None:
    """User input loop"""
    while True:
        s = input("\nType message to send: ")
        match s:
            case "ui":
                s = ui_commands.command_menu()
            case "rsync" | "sync":
                _rsync()
                continue
            case "status":
                _status()
                continue
            case "exit" | "quit" | "q":
                print("Ending program")
                exit()
            case "help":
                s = "Commands:\n\
                    ui: user interface to generate commands\n\
                    rsync | sync: get all recorded data from sensor\n\
                    status: check whether RPi is responding\n\
                    exit | quit | q: stop this program\n\
                    help: print this help menu"
                print(s.replace("    ", " "))
                continue
            case _:
                pass

        output = send_to_rpi(s)  # if message exists, send it
        print(output)


def _rsync() -> None:
    """Runs rsync command. Sends an rsync trigger in case radio is used"""
    s = f"rsync -avz -e ssh {rpi_name}@{rpi_addr}:{rpi_data_path} {host_data_path}"
    os.system(s)
    if has_radio:
        send_to_rpi("rsync")


def main() -> None:
    """Starts server and listens for incoming communications"""
    _ui_loop()


if __name__ == "__main__":
    main()
