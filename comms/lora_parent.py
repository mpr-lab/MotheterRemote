"""
Handles LoRa communications for the main RPi.
"""

import time
import serial
import threading
import os

import configs

# radio connection
ADDR = configs.R_ADDR
BAUD = configs.R_BAUD

# text encoding
EOL = configs.EOL
EOF = configs.EOF
utf8 = configs.utf8

# timing
long_s = configs.long_s
mid_s = configs.mid_s
short_s = configs.short_s

rpi_data_path = configs.rpi_data_path

echo = True


class Radio:
    def __init__(self):
        self.data: list[str] = []
        self.s = serial.Serial(ADDR, BAUD, timeout=None)
        self.l = threading.Thread(target=self._listen)  # listener in background
        self.l.daemon = True
        self.l.start()

    def _start_listen(self) -> None:
        """tries to start listener, if not already running"""
        try:
            self.l.start()
        except RuntimeError:
            p("Listener already running")

    def _listen(self) -> None:
        """radio listener that runs continuously"""
        self.live = True
        while self.live:
            time.sleep(short_s)
            full_msg = self.s.read_until(EOF.encode(utf8))
            msg_arr = full_msg.decode(utf8).split(EOL)
            for msg in msg_arr:
                p(f"Received over radio: {msg}")
                if "rsync" in msg:
                    p("got rsync from radio")
                    self._rsync_from_radio(msg)
                else:
                    p("regular message")
                    self.data.append(msg)

    def _send(self, msg: str | list[str] = "rx") -> None:
        """sends message to child rpi over radio

        Args:
            msg (str | list[str], optional): message to send. Defaults to "rx".
        """
        if isinstance(msg, list):
            m = EOL.join(msg)
        else:
            m = msg
        self.s.write((m + EOF).encode(utf8))

    def return_collected(self) -> list[str]:
        """returns all data gathered since last collection

        Returns:
            list[str]: messages to send
        """
        d = self.data[:]  # pass by value, not reference
        self.data.clear()
        return d

    def send_loop(self) -> None:
        """ui for debugging only"""
        while True:
            i = input("Send: ")
            self._send(i)

    def rpi_to_client(self, m: str) -> None:
        """sends a message over radio

        Args:
            m (str): message to send
        """
        if m == "rsync":  # filter out rsync requests
            p("Got rsync request!")
            self._send("rsync list")  # ask child for list of dat files
            return
        p(f"Sending to radio: {m}")
        self._send(m)

    def client_to_rpi(self) -> str:
        """returns messages waiting in buffer

        Returns:
            str: messages to send, concatenated
        """
        msg_arr = self.return_collected()
        if len(msg_arr) != 0:
            p(f"Received over radio: {msg_arr}")
        return EOL.join(msg_arr)

    def _rsync_from_radio(self, m: str) -> None:
        """called when child rpi returns rsync data"""
        if "rsync files" in m:
            self._compare_files(m)

    def _ask_child_for_file(self, filename: str) -> None:
        """get file from child for rsync

        Args:
            s (str): file name
        """
        p(f"Rsync: asking for file {filename}")
        s = f"rsync {filename}"
        self._send(s)

    def _compare_files(self, m: str) -> None:
        """List which files to get from child. If parent (this rpi) doesn't have a file, or has an outdated version, send a request to the child to return the file.

        Args:
            m (str): list of all .dat files from child with last modified date
        """
        p("in compare files")
        parent = (
            self._get_file_list()
        )  # dict of all .dat files from this rpi with dates
        c_list = m.split(",")  # list of all child .dat files with dates

        p(f"CLIST: {c_list}")
        try:
            c_list.remove("rsync files")
        except Exception as e:
            p(str(e))
            p("couldn't remove rsync files header for some reason, cringe")

        p(f"CLIST: {c_list}")
        child: dict[str, int] = {}  # format child list as dict
        for i in c_list:
            j = i.split(";")
            p(f"j: {j}")
            child.update({j[0].strip(): int(j[1].strip())})

        p(f"CHILD: {child}")

        for c in child.keys():
            p_date = parent.get(c)
            c_date = child.get(c)
            p(f"{c_date} v {p_date}")
            if c_date == None:  # something must have broken somewhere
                continue
            elif p_date == None:  # no match in parent dict
                p(f"no match, getting {c} from child")
                self._ask_child_for_file(c)  # send request
            elif p_date <= c_date:  # if child file more recent
                p(f"more recent version found, getting {c} from child")
                self._ask_child_for_file(c)  # send request

    def _get_file_list(self) -> dict[str, int]:
        """gets dict of all .dat files in the data directory on this rpi, with the corresponding date of modification

        Returns:
            dict[str, int]: file name and modified date
        """

        def _all_file_list(path: str = os.getcwd()) -> list[str]:
            """recursively finds all .dat files in the rpi data directory.

            Args:
                path (str, optional): current path to search. Defaults to current working directory.

            Returns:
                list[str]: all .dat files in current directory
            """
            to_return: list[str] = []
            try:
                file_list = os.listdir(path)
            except:
                p(f"cannot find directory {path}, returning")
                return []
            for entry in file_list:
                # p(f"{path}  /  {entry}")
                fullPath = os.path.join(path, entry)
                if os.path.isdir(fullPath):  # recurse on directory
                    to_return.extend(_all_file_list(fullPath))
                if fullPath.endswith(".dat"):
                    to_return.append(fullPath)
            # p(str(to_return))
            return to_return

        l = _all_file_list(rpi_data_path)
        d: dict[str, int] = {}
        p(str(l))
        for file in l:
            if file.endswith(".dat"):  # filter for dat files
                ctime = os.path.getctime(file)  # seconds since 1970
                d.update({file: int(ctime)})  # new dict entry for name and date
        return d


def p(s: str) -> None:
    global echo
    if echo:
        os.system(f"echo {s}")
    else:
        print(s, flush=True)  # print, even if in thread


if __name__ == "__main__":
    s = Radio()
    s.send_loop()
