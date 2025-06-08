"""
Runs on an accessory RPi that communicates to the main RPi using LoRa radio. This RPi must be directly connected to the sensor_ssh.
"""

import datetime
import time
import serial
import threading
import os
import sys

# module imports
import configs_ssh
import sensor_ssh

# where to store data
acc_data_path: str = configs_ssh.acc_data_path

# radio connection
ADDR = configs_ssh.R_ADDR
BAUD = configs_ssh.R_BAUD

# text encoding
EOL = configs_ssh.EOL
EOF = configs_ssh.EOF
utf8 = configs_ssh.utf8

# device info
device_type = configs_ssh.device_type

# timing
long_s = configs_ssh.long_s
mid_s = configs_ssh.mid_s
short_s = configs_ssh.short_s


class Ser:
    """Serial connection for radio"""

    def __init__(self):
        """Initialize serial connection to device"""
        self.s = serial.Serial(ADDR, BAUD, timeout=None)
        try:
            if device_type == "SQM-LU":
                self.device = sensor_ssh.SQMLU()
            elif device_type == "SQM-LE":
                self.device = sensor_ssh.SQMLE()
            else:
                self.device = sensor_ssh.SQMLU()  # default
        except Exception as e:
            print(f"{e}", flush=True, file=sys.stderr)  # if device not connected, quit
            exit()
        self.device.start_continuous_read()  # start device listener
        time.sleep(mid_s)  # wait for setup
        self.radio = threading.Thread(target=self._listen_radio)  #  radio listener
        self.sensor = threading.Thread(target=self._listen_sensor)  #  sensor listener
        self.radio.start()
        self.sensor.start()
        self.backlog: list[tuple[str, datetime.datetime]] = []

    def _listen_radio(self) -> None:
        """Get incoming radio messages, send them to device"""
        print(
            f"Radio listener running in {threading.current_thread().name}",
            flush=True,
            file=sys.stderr,
        )
        while True:
            try:
                time.sleep(mid_s)  # wait
                full_msg = self.s.read_until(EOF.encode(utf8))  # get message
                msg_arr = full_msg.decode(utf8).split(EOL)  # decode and split

                for msg in msg_arr:  # go through each message
                    time.sleep(short_s)
                    m = msg.strip()  # strip whitespace
                    print(f"Received over radio: {m}", flush=True, file=sys.stdout)
                    if "rsync" in m:
                        self._rsync(m)  # deal with rsync
                    else:
                        self.backlog.append((m, datetime.datetime.now()))
                        self.device.rpi_to_client(m)  # send command
            except Exception as e:
                print(e, flush=True, file=sys.stderr)

    def _get_pair(self, resp: str) -> str:
        # sensor response prefix, corresponding message prefix
        pairs: dict[str, str] = {
            "r,": "rx",
            "i,": "ix",
            "c,": "cx",
            "zA": "zcal",
            "zB": "zcal",
            "zD": "zcal",
            "I,": "Ix",
            "z,": "zcal",
            "s,": "sx",
            "S,": "S,",
            "L0": "L0x",
            "L1": "L1x",
            "L3": "L3x",
            "L4": "L4x",
            "L5": "L5x",
            "LM": "LMx",
            "LI": "LIx",
            "LC": "LCx",
            "Lc": "LCx",
            "La": "Lax",
        }

        response_prefix = resp[0:2]
        print(f"response prefix {response_prefix}", flush=True, file=sys.stderr)
        message_prefix = pairs.get(response_prefix)  # get original message prefix
        print(f"message prefix {message_prefix}", flush=True, file=sys.stderr)

        if message_prefix == None:  # if not there, ignore
            print("return", flush=True, file=sys.stderr)
            return ""

        # loop through received, un-responded messages
        for i in range(0, len(self.backlog)):
            if self.backlog[i][1] == datetime.datetime.now() - datetime.timedelta(
                    seconds=60
            ):
                print(
                    f"backlogged request {self.backlog[i]} is old",
                    flush=True,
                    file=sys.stderr,
                )
                self.backlog.pop(i)  # erase old requests (one minute)
            elif self.backlog[i][0].startswith(message_prefix):
                print(
                    f"should respond to {self.backlog[i]} with {resp}",
                    flush=True,
                    file=sys.stderr,
                )
                self.backlog.pop(i)
                return resp
        return ""

    def _listen_sensor(self) -> None:
        """Get incoming sensor messages, send them over radio"""
        print(
            f"Listener loop running in {threading.current_thread().name}",
            flush=True,
            file=sys.stderr,
        )
        while True:
            time.sleep(mid_s)
            resp = self.device.client_to_rpi()  # get response from device
            if len(resp) != 0:  # if response has data
                print(f"Received from sensor: {resp}", flush=True, file=sys.stdout)
                m = EOL.join(resp)
                if self._get_pair(m) != "":
                    self._send(m)

    def _send(self, msg: str | list[str] = "test") -> None:
        """Send sensor responses to parent over radio

        Args:
            msg (str | list[str], optional): message(s) to send. Defaults to "test".
        """
        if isinstance(msg, list):
            m = EOL.join(msg)  # if list, collate into string
        else:
            m = msg
        print(f"Sending over radio: {m}", flush=True, file=sys.stdout)
        self.s.write((m + EOF).encode(utf8))

    def _send_loop(self) -> None:
        """Ui for debugging only. Sends message over radio"""
        while True:
            i = input("Send: ")
            self._send(i)

    def _rsync(self, s: str) -> None:
        """Handles rsync requests

        Args:
            s (str): request to handle
        """
        print("Handling rsync", flush=True, file=sys.stderr)
        if "list" in s:  # file list requested
            print("Sending file list", flush=True, file=sys.stderr)
            self._send(self._get_file_list())
        else:  # must be asking for specific file
            name = s.replace("rsync ", "").strip()  # rest of request is path
            if not os.path.isfile(name):  # if wrong, ignore
                print(f"Path {name} not found", flush=True, file=sys.stderr)
                return

            print(f"Reading file {name}", flush=True, file=sys.stderr)
            short = name.rfind("/")  # find where name starts at right of path
            short_name = name[short + 1 :]  # get name
            b = bytearray(f"rsync {short_name} {EOL}", utf8)  # prepend file name

            with open(name, "rb") as file:
                text = file.read()  # get text from file as bytes
                b.extend(text)  # append to bytearray
                b.extend(EOF.encode(utf8))  # EOF to finish
                print(f"File to send: {b.decode()}", flush=True, file=sys.stderr)

            self.s.write(b)  # send bytearray

    def _get_file_list(self) -> str:
        """Gets string list of all .dat files in the data directory on this RPi, with the corresponding date of modification

        Returns:
            str: name and modified date for each file, concatenated
        """

        def _all_file_list(path: str = "") -> list[str]:
            """Recursively finds all .dat files in the RPi data directory.

            Args:
                path (str, optional): current path to search. Defaults to current working directory.

            Returns:
                list[str]: all .dat files in current directory
            """
            to_return: list[str] = []
            try:
                file_list = os.listdir(path)  # get list of files
            except:
                print(
                    f"Cannot find directory {path}, returning",
                    flush=True,
                    file=sys.stderr,
                )
                return []  # something went wrong, stop recursing

            for entry in file_list:
                fullPath = os.path.join(path, entry)
                if os.path.isdir(fullPath):  # if directory, recurse on it
                    to_return.extend(_all_file_list(fullPath))
                if fullPath.endswith(".dat"):  # if .dat file, add to list
                    to_return.append(fullPath)
            return to_return

        l = _all_file_list(acc_data_path)
        a: list[str] = []
        a.append("rsync files")  # prepend header for parent processing
        for file in l:
            if file.endswith(".dat"):  # filter for dat files
                ctime = os.path.getmtime(file)  # seconds since 1970
                s = f"{file};{ctime}"  # entry with name and time
                a.append(s)
        c = str(a)  # convert array to string
        print(f"TO SEND: {c}", flush=True, file=sys.stderr)
        return c


if __name__ == "__main__":
    print("\n\n", flush=True, file=sys.stderr)
    s = Ser()