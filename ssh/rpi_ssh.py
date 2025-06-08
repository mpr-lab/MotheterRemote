# imports
import argparse
import sys

# module imports
import sensor_ssh
import lora_parent_ssh
import configs_ssh

device = sensor_ssh.SQMLE | sensor_ssh.SQMLU | lora_parent_ssh.Radio

output: device | None = None


def _device_search() -> None:
    """Determines whether a radio or sensor is connected by trying to create each device"""
    global output

    try:
        if configs_ssh.device_type == "SQM-LU":
            output = sensor_ssh.SQMLU()
        elif configs_ssh.device_type == "SQM-LE":
            output = sensor_ssh.SQMLE()
        else:
            output = sensor_ssh.SQMLU()  # default
        output.start_continuous_read()
        return
    except Exception as e:
        print(str(e), file=sys.stderr)
        print(f"SQM-LU or SQM-LE sensor not found, trying radio...", file=sys.stderr)

    try:
        output = lora_parent_ssh.Radio()
        return
    except Exception as e:
        print(str(e))
        print(f"No radio found at port {configs_ssh.R_ADDR}", file=sys.stderr)

    print("No radio or sensor found. Please check connection!", file=sys.stderr)


def main():
    print("NEW_ENTRY", file=sys.stdout)
    print("NEW_ENTRY", file=sys.stderr)
    parser = argparse.ArgumentParser(
        prog="rpi_ssh.py",
        description="Processes and responds to ssh commands from host computer",
        epilog="If no argument given, runs status",
    )

    parser.add_argument(
        "command",
        nargs="?",
        type=str,
        help="Whatever command/message the host wants to run",
    )
    args = vars(parser.parse_args())
    command = args.get("command")

    if command == None:
        print("AOK", file=sys.stdout)
        return

    if not isinstance(command, str):
        print(
            f"Command is not a string. command: {command}, type: {type(command)}",
            file=sys.stderr,
        )
        return

    if "rsync" in command:
        print("radio not implemented yet", file=sys.stderr)
        return

    if "status" in command:
        print("AOK", file=sys.stdout)
        return

    try:
        _device_search()  # connect if possible
        global output

        if output == None:
            print("Could not connect to device.", file=sys.stderr)
            if configs_ssh.has_radio:
                print(
                    f"Device should be radio at {configs_ssh.R_ADDR}", file=sys.stderr
                )
            else:
                print(
                    f"Device should be {configs_ssh.device_type} sensor at {configs_ssh.device_addr}",
                    file=sys.stderr,
                )
            return

        try:
            output.rpi_to_client(command)
        except Exception as e:
            print(f"Communication failed!\n{e}", file=sys.stderr)

    except Exception as e:
        print(f"Could not connect to device:\n{e}", file=sys.stderr)


if __name__ == "__main__":
    main()