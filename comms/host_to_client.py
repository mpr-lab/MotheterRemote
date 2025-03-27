"""
Runs on host computer, sends a command and gets responses.
"""

import os
import threading
import socketserver
import socket
import time

# python module imports
import ui_commands
import configs
import parse_response

# WiFi/Ethernet connection info
host_addr = configs.host_addr
rpi_addr = configs.rpi_addr
rpi_name = configs.rpi_name

# socket port numbers
host_server = configs.host_server
rpi_server = configs.rpi_server

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

# threads
server_thread: threading.Thread
rpi_thread: threading.Thread
loop_thread: threading.Thread


class Server:
    """Socket server to listen for incoming communications"""

    def __init__(self):
        print(f"Creating host server {host_addr}:{host_server}")
        socketserver.TCPServer.allow_reuse_address = True  # allows reconnecting

        # start TCP server
        try:
            self.server = socketserver.TCPServer(
                (host_addr, host_server), ThreadedTCPRequestHandler
            )
        except Exception as e:
            print(e)
            self.server.server_close()

        # run server in designated thread
        global server_thread
        server_thread = threading.Thread(target=self.server.serve_forever)
        server_thread.daemon = True  # Exit server thread when main thread terminates
        server_thread.start()
        print("Server loop running in", server_thread.name)

    def send_to_rpi(self, s: str) -> None:
        """simple socket connection that forwards a single message to the host, then dies

        Args:
            s (str): message to send
        """
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        try:
            sock.connect((rpi_addr, rpi_server))  # connect to server
            sock.sendall(f"{s}".encode(utf8))  # send everything
            print(f"Sent: {s}")
        except Exception as e:
            if str(e) == "[Errno 61] Connection refused":
                print("Client RPi is not running rpi_wifi.py")
                print("Use START to establish connection.")
            else:
                print(e)  # print error without halting
                print("Client RPi might not be running rpi_wifi.py")
                print("Wait approx. 1 minute before trying again.")
        finally:
            sock.close()  # die


class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    """overwrites TCPServer with custom handler"""

    pass


class ThreadedTCPRequestHandler(socketserver.BaseRequestHandler):
    """overwrites BaseRequestHandler with custom handler"""

    def handle(self):
        """custom request handler for TCP threaded server"""
        # ensure request is socket
        if not isinstance(self.request, socket.socket):
            print("ThreadedTCPRequestHandler: self.request not socket")
            return

        # when request comes in, decode and format it
        self.data = self.request.recv(msg_len).decode(utf8).strip()
        cur_thread = threading.current_thread()
        print(
            f"Received from {self.client_address[0]} in {cur_thread.name}: {self.data}"
        )
        _print_formatted(self.data)  # print formatted data to terminal


def _start_listener() -> None:
    """Sends command to prompt RPi to start listening"""
    # nohup allows rpi_wifi.py to run even after terminal ends
    # & lets process run the the background
    s = [f"ssh {rpi_name}@{rpi_addr} 'cd {rpi_repo}; nohup python3 rpi_wifi.py' &"]
    print("Sending command to RPi:", s)

    global rpi_thread
    rpi_thread = threading.Thread(target=os.system, args=s)  # run in dedicated thread
    rpi_thread.start()


def _kill_listener() -> None:
    """Kills RPi program via an SSH command"""
    s = f"ssh {rpi_name}@{rpi_addr} 'pkill -f rpi_wifi.py'"
    print("Sending command to RPi:", s)
    os.system(s)


def _print_formatted(s: str) -> None:
    """Prints formatted response from RPi

    Args:
        s (str): message to format
    """
    arr = s.split(EOL)
    for m in arr:
        print(parse_response.sort_response(m))
    global allow_ui
    allow_ui = True  # allow next user input


def _status() -> None:
    # is the host server (this computer) running?
    global server_thread
    try:
        server_thread.is_alive()
    except:
        print("Host server thread not created. This shouldn't be possible.")
        exit()
    if server_thread.is_alive():
        print(f"Host server: ALIVE")
    else:
        print(f"Host server: DEAD")
    print(
        f"\
        IP {host_addr}\n\
        Port {host_server}\n\
        Host name {configs.host_name}\n\
        {server_thread.name}"
    )

    global loop_thread
    try:
        loop_thread.is_alive()
    except:
        print("Host UI loop thread not created. This shouldn't be possible.")
        exit()
    if loop_thread.is_alive():
        print(f"Host UI loop: ALIVE")
    else:
        print(f"Host UI loop: DEAD")
    print(
        f"\
        {loop_thread.name}"
    )

    global conn
    global rpi_thread
    try:
        rpi_thread.is_alive()
        print(
            f"Thread to RPi: EXISTS\n\
        Activity on RPi will appear on this terminal."
        )
        conn.send_to_rpi("status")
    except:
        print(
            f"Thread to RPi: DOES NOT EXIST.\n\
        Activity on RPi will NOT appear on this terminal."
        )


def _ui_loop() -> None:
    """User input loop"""
    global conn, allow_ui
    while True:
        s = input("\nType message to send: ")
        match s:
            case "ui":
                s = ui_commands.command_menu()
            case "rsync" | "sync":
                _rsync()
                continue
            case "start":
                _start_listener()
                time.sleep(long_s)
                continue
            case "kill":
                _kill_listener()
                continue
            case "status":
                _status()
                time.sleep(mid_s)
                continue
            case "exit" | "quit" | "q":
                print("Ending program")
                exit()
            case "help":
                s = "Commands:\n\
                    ui: user interface to generate commands\n\
                    rsync | sync: get all recorded data from sensor\n\
                    start: starts the program running on the RPi\n\
                    kill: stop the program running on the RPi\n\
                    status: view the status of the entire system\n\
                    exit | quit | q: stop this program\n\
                    help: print this help menu"
                print(s.replace("    ", " "))
                continue
            case _:
                pass

        conn.send_to_rpi(s)  # if message exists, send it
        start = time.time()  # set up timer to wait for response
        while (time.time() - start < 3) and allow_ui == False:
            pass  # do nothing until current prompt dealt with, or timeout
        allow_ui = False  # disallow user input


def _rsync() -> None:
    """Runs rsync command. Sends an rsync trigger in case radio is used"""
    s = f"rsync -avz -e ssh {rpi_name}@{rpi_addr}:{rpi_data_path} {host_data_path}"
    os.system(s)
    conn.send_to_rpi("rsync")


def main() -> None:
    """Starts server and listens for incoming communications"""
    global conn, output
    conn = Server()  # start TCP server

    global loop_thread
    loop_thread = threading.Thread(target=_ui_loop)  # user input loop
    loop_thread.start()


if __name__ == "__main__":
    main()
