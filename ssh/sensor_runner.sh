#!/bin/bash

# get date/time
dt="$(date '+%d/%m/%Y %H:%M:%S');"

echo "Running sensor_runner.sh"

processes=$(ps -ef | grep [s]ensor_stream.py)
if [[ $? == 1 ]]; then # grep found nothing
    echo "Sensor streaming program not running!"

    # redirect stdout to log file
    echo $dt >> /var/tmp/ssh/sensor_output.txt
    exec 1>> /var/tmp/ssh/sensor_output.txt

    # redirect stderr to log file
    echo $dt >> /var/tmp/ssh_debug/stderr.txt
    exec 2>> /var/tmp/ssh_debug/sensor_error.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    /usr/bin/python3 ~/MotheterRemote/ssh/sensor_stream.py 2>> /var/tmp/ssh_debug/sensor_error.txt 1>>/var/tmp/ssh/sensor_output.txt &

elif [[ $processes ]]; then # grep returned something
    : # do nothing
elif [[ $? != 0 ]]; then # error
    echo "Command failed (not grep)."
else # something else went wrong
    echo "Command failed."
fi