import os

rpi_port = 9000
rpi_user = "pi"
host_port = 22
host_ip = "10.10.45.94"
rpi_ip = "10.10.32.91"

s = f"ssh -R {rpi_port}:{host_ip}:{host_port} {rpi_user}@{rpi_ip}"

print(s)

"ssh -b ipaddrfordevice -R 43022:localhost:22 username@publicip -p youropenport"
"ssh -b ipaddrfordevice -R 43022:localhost:22 username@publicip -p youropenport"

rpi_port = 8888
host_user = "skyeworster"
host_port = 33
host_ip = "10.10.45.94"
rpi_ip = "10.10.32.91"


s = f"ssh -R {host_port}:{rpi_ip}:{rpi_port} {host_user}@{host_ip}"
print(s)


"""
from stackoverflow

I was able to do this using reverse ssh.

On the IoT device I send the command,

ssh -b ipaddrfordevice -R 43022:localhost:22 username@publicip -p youropenport

then on the host after I have connected,

ssh localhost -p 43022

Would it happen to be the 7600 hat?
"""
