# imports
import argparse
import time

# module imports
import sensor
import lora_parent
import configs

output: sensor.SQMLE | sensor.SQMLU | lora_parent.Radio


def _device_search() -> None:
    """Determines whether a radio or sensor is connected by trying to create each device"""
    global output

    try:
        if configs.device_type == "SQM-LU":
            output = sensor.SQMLU()
        elif configs.device_type == "SQM-LE":
            output = sensor.SQMLE()
        else:
            output = sensor.SQMLU()  # default
        output.start_continuous_read()
        return
    except Exception as e:
        print(str(e))
        print(f"SQM-LU or SQM-LE sensor not found, trying radio...")

    try:
        output = lora_parent.Radio()
        return
    except Exception as e:
        print(str(e))
        print(f"No radio found at port {configs.R_ADDR}")

    print("No radio or sensor found. Please check connection!")


def main():
    print("rpi_ssh.py running...")

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
        print("AOK")
        return

    if not isinstance(command, str):
        print(f"Command is not a string. command: {command}, type: {type(command)}")
        return

    if "rsync" in command:
        print("radio not implemented yet")
        return

    if "status" in command:
        print("AOK")
        return

    try:
        _device_search()  # connect if possible
        global output
        output.rpi_to_client(command)  # forward message to radio/sensor
    except Exception as e:
        print(f"Could not connect to device:\n{str(e)}")


main()
