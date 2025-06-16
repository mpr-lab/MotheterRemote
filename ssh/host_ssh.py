import os
import platform
import subprocess
import sys
import importlib

# External modules (must be provided)
import ui_commands_ssh
import configs_ssh

# Load from config
rpi_addr = configs_ssh.rpi_addr
rpi_name = configs_ssh.rpi_name
host_data_path = configs_ssh.host_data_path
rpi_data_path = configs_ssh.rpi_data_path
rpi_repo = configs_ssh.rpi_repo
has_radio = configs_ssh.has_radio

IS_WINDOWS = platform.system() == "Windows"

def send_to_rpi(m: str) -> str:
    # Fix command quoting for Windows
    inner_cmd = f"cd {rpi_repo}/ssh && ./rpi_runner.sh {m}"
    quoted_inner_cmd = f'"{inner_cmd}"' if IS_WINDOWS else f"'{inner_cmd}'"
    run_command = f"ssh {rpi_name}@{rpi_addr} {quoted_inner_cmd}"

    subprocess.run(run_command, shell=True)

    # Fix f-string issue with backslashes
    if IS_WINDOWS:
        tail_cmd = '"tail -n 1 /var/tmp/ssh_debug/rpi_out.txt"'
    else:
        tail_cmd = "'tail -n 1 /var/tmp/ssh_debug/rpi_out.txt'"

    read_cmd = f"ssh {rpi_name}@{rpi_addr} {tail_cmd}"

    try:
        output = subprocess.check_output(read_cmd, shell=True).decode()
    except subprocess.CalledProcessError as e:
        output = f"Error: {e}"
    return output.strip()



def user_input(data: str) -> None:
    print(data)

    match data:
        case "status":
            _status()
        case "ui":
            data = ui_commands_ssh.command_menu()
            print(send_to_rpi(data))
        case "rsync" | "sync":
            _rsync()
        case "help":
            help_msg = (
                "Commands:\n"
                "  ui   – open device UI\n"
                "  rsync|sync – copy data from sensor\n"
                "  help – this text\n"
                "  reload-config – reloads the configs_ssh module\n"
            )
            print(help_msg)
        case "reload-config":
            importlib.reload(configs_ssh)
        case _:
            print(send_to_rpi(data))


def _status() -> None:
    output = send_to_rpi("status")
    print(output)
    if "AOK" in output:
        print("RPi is responding")
    else:
        print("RPi might not be responding properly")


def _ui_loop() -> None:
    while True:
        s = input("\nType message to send: ").strip()
        match s:
            case "ui":
                s = ui_commands_ssh.command_menu()
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
                print("Commands:\n"
                      "  ui: user interface to generate commands\n"
                      "  rsync | sync: get data from sensor\n"
                      "  status: check RPi\n"
                      "  exit | quit | q: stop program\n"
                      "  help: print this help menu")
                continue
            case _:
                pass

        print(send_to_rpi(s))


def _rsync() -> None:
    # Prefer rsync, fallback to scp if on Windows
    if IS_WINDOWS:
        print("Using SCP instead of rsync on Windows...")
        s = f"scp -r {rpi_name}@{rpi_addr}:{rpi_data_path} {host_data_path}"
    else:
        s = f"rsync -avz -e ssh {rpi_name}@{rpi_addr}:{rpi_data_path} {host_data_path}"

    subprocess.run(s, shell=True)

    if has_radio:
        send_to_rpi("rsync")


def main() -> None:
    _ui_loop()


if __name__ == "__main__":
    if len(sys.argv) > 1:
        user_input(sys.argv[1])
    else:
        main()