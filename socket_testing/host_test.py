import socket
import get_command
import parse_response

ip = "131.229.147.51"
port = 12345
print("test")
# s = socket.socket()
# s.connect(("131.229.152.158", port))
# print("Socket successfully created")
# while True:
#     c, addr = s.accept()


def to_client():
    # host waits for client to respond, then sends message
    pass


def from_client():
    pass


#     print("Got connection from", addr)
#     msg = get_command.command_from_host()
#     c.send(msg.encode())
#     resp = c.recv(1024)
#     get_command.from_socket(resp.decode())
#     parse_response.sort_response(resp.decode())
#     c.close()
