import sys
import time

import configs_ssh
import sensor_ssh

sensor: sensor_ssh.SQM


def main():
    global sensor
    if configs_ssh.device_type == "SQM-LU":
        sensor = sensor_ssh.SQMLU()
    elif configs_ssh.device_type == "SQM-LE":
        sensor = sensor_ssh.SQMLE()
    else:
        print(
            "Configs don't list device as sensor; sensor_stream.py shouldn't run.",
            file=sys.stderr,
        )
        return

    print(
        "sensor_stream.py running. Will monitor sensor over serial until killed. Output is directed to stdout (should be appended to a log file via bash script).",
        file=sys.stderr,
    )

    sensor.start_continuous_read()

    while True:
        time.sleep(60)
        m = sensor.client_to_rpi()
        if len(m) != 0:
            print(m, file=sys.stdout)


if __name__ == "__main__":
    main()