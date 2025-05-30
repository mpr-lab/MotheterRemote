from configs import rpi_addr, rpi_name, bird_recording_source, bird_recording_dest
import os

src = f"{rpi_name}@{rpi_addr}:{bird_recording_source}"


def sync_all():
    s = f"rsync -avz -e ssh {src} {bird_recording_dest}"
    os.system(s)


# unsure if this works
def sync_3days():
    s = f"rsync --progress --files-from=<(find {src} -mtime -3 -type f -exec basename {{}} \\;) {src} {bird_recording_dest}"
    os.system(s)
