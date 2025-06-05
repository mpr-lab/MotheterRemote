# export_config.py
# export_config.py
## RUN THIS TO UPDATE config.properties TO MAKE GUI RUN FOR YOUR MACHINE IF YOU CHANGE configs.py
import importlib.util
from pathlib import Path

cfg_path = Path("../comms/configs.py").resolve()  # relative or absolute
spec = importlib.util.spec_from_file_location("configs", cfg_path)
configs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(configs)     # configs is now a live module


with open("config.properties", "w") as f:
    f.write(f"host_addr={configs.host_addr}\n")
    f.write(f"host_name={configs.host_name}\n")
    f.write(f"host_data_path={configs.host_data_path}\n")

    f.write(f"rpi_name={configs.rpi_name}\n")
    f.write(f"rpi_addr={configs.rpi_addr}\n")
    f.write(f"rpi_repo={configs.rpi_repo}\n")
    f.write(f"rpi_data_path={configs.rpi_data_path}\n")

    f.write(f"acc_repo={configs.acc_repo}\n")
    f.write(f"acc_data_path={configs.acc_data_path}\n")
    f.write(f"acc_lora_port={configs.acc_lora_port}\n")

    f.write(f"R_ADDR={configs.R_ADDR}\n")
    f.write(f"R_BAUD={configs.R_BAUD}\n")

    f.write(f"device_type={configs.device_type}\n")
    f.write(f"device_addr={configs.device_addr}\n")
    f.write(f"observatory_name={configs.observatory_name}\n")
    f.write(f"debug={configs.debug}\n")
    f.write(f"tries={configs.tries}\n")

    f.write(f"LU_BAUD={configs.LU_BAUD}\n")
    f.write(f"LU_TIMEOUT={configs.LU_TIMEOUT}\n")

    f.write(f"LE_PORT={configs.LE_PORT}\n")
    f.write(f"LE_TIMEOUT={configs.LE_TIMEOUT}\n")
    f.write(f"LE_SOCK_BUF={configs.LE_SOCK_BUF}\n")

    f.write(f"host_port={configs.host_server}\n")
    f.write(f"rpi_port={configs.rpi_server}\n")
    f.write(f"msg_len={configs.msg_len}\n")

    f.write(f"EOL={configs.EOL}\n")
    f.write(f"EOF={configs.EOF}\n")
    f.write(f"encoding={configs.utf8}\n")

    f.write(f"long_s={configs.long_s}\n")
    f.write(f"mid_s={configs.mid_s}\n")
    f.write(f"short_s={configs.short_s}\n")

    f.write(f"remote_start={configs.remote_start}\n")
