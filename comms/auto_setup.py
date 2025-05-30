from curses.ascii import isspace
import platform
import sys
import subprocess

# connection type to first RPi.
# options are: ethernet, wifi, wifi_tailscale, cellular_tailscale
connection_type = "wifi_tailscale"

system = platform.system()
if system in ["Linux", "SunOS", "Darwin"]:
    unix = True
elif system == "Windows":
    unix = False
else:
    print("Operating system cannot be determined!", file=sys.stderr)
    quit()

if unix:
    try:
        host_name = subprocess.check_output("whoami", shell=True).decode().strip()
    except Exception as e:
        print(f"UNIX USER name could not be auto-filled:\n{e}", file=sys.stderr)

    try:
        host_addr = (
            subprocess.check_output("hostname", shell=True)
            .decode()
            .strip()
            .strip(".local")
        )
    except Exception as e:
        print(f"UNIX COMPUTER name could not be auto-filled:\n{e}", file=sys.stderr)

else:
    try:
        host_name = (
            subprocess.check_output("whoami", shell=True)
            .decode()
            .strip()
            .split("\\")[1]
        )
    except Exception as e:
        print(f"WINDOWS USER name could not be auto-filled:\n{e}", file=sys.stderr)

    try:
        host_addr = (
            subprocess.check_output("echo %COMPUTERNAME%", shell=True).decode().strip()
        )
    except Exception as e:
        print(f"WINDOWS COMPUTER name could not be auto-filled:\n{e}", file=sys.stderr)

intf_dict = {}

if system == "Linux":
    query = "ip -o link show | awk -F': ' '{print $2}'"
    try:
        interfaces = (
            subprocess.check_output(query, shell=True).decode().strip().split("\n")
        )
    except Exception as e:
        print(f"LINUX could not list network interfaces:\n{e}", file=sys.stderr)
        quit()

    for intf in interfaces:
        command = "ifconfig " + intf + " | grep 'inet' | head -n 1| awk '{print $2}'"
        try:
            ip = subprocess.check_output(command, shell=True).decode().strip()
        except Exception as e:
            print(
                f"LINUX could not find IP for interface {intf}:\n{e}", file=sys.stderr
            )
            quit()
        intf_dict[intf] = ip

if system == "Darwin":
    query = "networksetup -listallhardwareports | grep Device | awk '{print $2}'"
    try:
        interfaces = (
            subprocess.check_output(query, shell=True).decode().strip().split("\n")
        )
    except Exception as e:
        print(f"MacOS could not list network interfaces:\n{e}", file=sys.stderr)
        quit()

    for intf in interfaces:
        command = "ifconfig " + intf + " | grep 'inet ' | awk '{print $2}'"
        try:
            ip = subprocess.check_output(command, shell=True).decode().strip()
        except Exception as e:
            print(
                f"MacOS could not find IP for interface {intf}:\n{e}", file=sys.stderr
            )
            quit()
        intf_dict[intf] = ip

if system == "Windows":
    query = "ipconfig"
    try:
        interfaces = subprocess.check_output(query, shell=True).decode()
    except Exception as e:
        print(f"WINDOWS could not list network interfaces:\n{e}", file=sys.stderr)
        quit()

    print(interfaces)
    ar = interfaces.split("\n")
    intf = ""
    for i in ar:
        if not i[0].isspace():
            intf = i.strip(":")
        if "IPv4" in i:
            ip = i.strip().split(" ")[-1]
            intf_dict[intf] = ip


print(intf_dict)
