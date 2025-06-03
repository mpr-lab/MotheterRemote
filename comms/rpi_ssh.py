# imports
import os
import argparse
import time

# module imports
import sensor
import lora_parent
import configs

device_type: str = "SQM-LU"  # for debugging only


def p(s: str) -> None:
    """Flushes buffer and prints. Enables print in threads

    Args:
        s (str): string to print
    """
    print(s, flush=True)


def _device_search() -> None:
    """Determines whether a radio or sensor is connected by trying to create each device"""
    global output

    try:
        if device_type == "SQM-LU":
            output = sensor.SQMLU()
        elif device_type == "SQM-LE":
            output = sensor.SQMLE()
        else:
            output = sensor.SQMLU()  # default
        output.start_continuous_read()
        return
    except Exception as e:
        p(str(e))
        p(f"SQM-LU or SQM-LE sensor not found, trying radio...")

    try:
        output = lora_parent.Radio()
        return
    except Exception as e:
        p(str(e))
        p(f"No radio found at port {configs.R_ADDR}")

    p("No radio or sensor found. Please check connection!")


def main():
    parser = argparse.ArgumentParser(
        prog="rpi_ssh.py",
        description="Processes and responds to ssh commands from host computer",
        epilog="If no argument given, runs ls",
    )

    parser.add_argument(
        "command",
        nargs="?",
        type=str,
        help="Whatever command/message the host wants to run",
    )
    args = vars(parser.parse_args())
    command = args.get("command")
    if not isinstance(command, str):
        print(f"Command is not a string. command: {command}, type: {type(command)}")
        exit()

    if "rsync" in command:
        print("radio not implemented yet")
    elif "status" in command:
        print("status")
    else:
        try:
            output.rpi_to_client(command)  # forward message to radio/sensor
        except Exception as e:
            p(str(e))
            p("Resetting output device")  # probably lost connection
            _device_search()  # reconnect if possible
            time.sleep(configs.long_s)
