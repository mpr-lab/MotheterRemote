import platform
import sys
import subprocess


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

#### IF CONFIG NOT INSTALLED: MAYBE THIS IS A WAY TO WORK AROUND THAT?
# if system == "Linux":
#     query = "ip -o link show | awk -F': ' '{print $2}'"
#     try:
#         interfaces = (
#             subprocess.check_output(query, shell=True).decode().strip().split("\n")
#         )
#     except Exception as e:
#         print(f"LINUX could not list network interfaces:\n{e}", file=sys.stderr)
#         quit()
#
#     for intf in interfaces:
#         command = f"ip -o -4 addr show {intf} | awk '{{print $4}}' | cut -d'/' -f1"
#         try:
#             ip = subprocess.check_output(command, shell=True).decode().strip()
#         except Exception as e:
#             print(
#                 f"LINUX could not find IP for interface {intf}:\n{e}", file=sys.stderr
#             )
#             ip = ""
#         intf_dict[intf] = ip


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

    stripped = [i.rstrip() for i in interfaces.split("\n")]

    intf = ""
    for i in stripped:
        if len(i) == 0:
            continue
        if not i[0].isspace():
            intf = i.strip(":")
        if "IPv4" in i:
            ip = i.strip().split(" ")[-1]
            intf_dict[intf] = ip


print(intf_dict)
# Safe fallback
host_name = host_name if 'host_name' in locals() else 'unknown_user'
host_addr = host_addr if 'host_addr' in locals() else 'unknown_host'

# FINAL OUTPUT
print(f"{host_name},{host_addr}")

# Determine OS platform label for Java
if system == "Windows":
    os_type = "windows"
elif system == "Darwin":
    os_type = "mac"
elif system == "Linux":
    os_type = "linux"
else:
    os_type = "unknown"

print(f"OS={os_type}")
