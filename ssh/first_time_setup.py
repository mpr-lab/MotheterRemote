import os
import configs_ssh

os.system(f"chmod u+x {configs_ssh.rpi_repo}/ssh/radio_runner.sh")
os.system(f"chmod u+x {configs_ssh.rpi_repo}/ssh/rpi_runner.sh")