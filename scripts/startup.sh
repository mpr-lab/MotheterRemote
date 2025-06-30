#!/bin/bash

# ========== Startup Script for MPR Bio Remote ==========
# Logs will go to /var/tmp/ssh_debug/
LOG_DIR="/var/tmp/ssh_debug"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/startup.log"

echo "[Startup] Boot initiated at $(date)" >> "$LOG_FILE"

# Stop all running Python scripts
echo "[Startup] Killing existing Python processes..." >> "$LOG_FILE"
pkill -f python >> "$LOG_FILE" 2>&1

# Navigate to project folder
cd ~/Moth*/ssh || {
    echo "[Startup] Could not find Moth*/ssh directory." >> "$LOG_FILE"
    exit 1
}

# Try to pull updates (non-fatal if offline)
echo "[Startup] Attempting git pull..." >> "$LOG_FILE"
git pull >> "$LOG_FILE" 2>&1 || echo "[Startup] No internet or git pull failed." >> "$LOG_FILE"

# Run permission fix
echo "[Startup] Running first_time_setup.py..." >> "$LOG_FILE"
python3 first_time_setup.py >> "$LOG_FILE" 2>&1

# Determine mode (radio or non-radio) from flag file
if [ -f ~/radio_main ]; then
    echo "[Startup] Detected MAIN RPi in radio setup. Launching rpi_runner.sh..." >> "$LOG_FILE"
    ./rpi_runner.sh >> "$LOG_FILE" 2>&1 &
elif [ -f ~/radio_accessory ]; then
    echo "[Startup] Detected ACCESSORY RPi in radio setup. Launching radio and sensor..." >> "$LOG_FILE"
    ./radio_runner.sh >> "$LOG_FILE" 2>&1 &
    ./sensor_runner.sh >> "$LOG_FILE" 2>&1 &
else
    echo "[Startup] Detected RPi in sensor-only mode. Launching rpi and sensor..." >> "$LOG_FILE"
    ./rpi_runner.sh >> "$LOG_FILE" 2>&1 &
    ./sensor_runner.sh >> "$LOG_FILE" 2>&1 &
fi

echo "[Startup] Startup script complete." >> "$LOG_FILE"
