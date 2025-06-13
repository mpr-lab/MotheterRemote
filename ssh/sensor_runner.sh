#!/bin/bash

echo "Running sensor_runner.sh"

# make log file directory, if it doesn't exist
if [[ ! -e /var/tmp/ssh_debug ]]; then
    mkdir -p /var/tmp/ssh_debug
fi

# make log files, if they don't exist
if [[ ! -e /var/tmp/ssh_debug/sensor_out.txt ]]; then
    touch /var/tmp/ssh_debug/sensor_out.txt
fi
if [[ ! -e /var/tmp/ssh_debug/sensor_err.txt ]]; then
    touch /var/tmp/ssh_debug/sensor_err.txt
fi
if [[ ! -e /var/tmp/ssh_debug/pysqm_out.txt ]]; then
    touch /var/tmp/ssh_debug/pysqm_out.txt
fi
if [[ ! -e /var/tmp/ssh_debug/pysqm_err.txt ]]; then
    touch /var/tmp/ssh_debug/pysqm_err.txt
fi

# get date/time
dt="$(date '+%d/%m/%Y %H:%M:%S');"

all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [s]ensor_stream | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running sensor_streaming.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Sensor streaming program not running! Attempting to start now."

    # redirect stdout to log file
    echo $dt >> /var/tmp/ssh_debug/sensor_out.txt
    exec 1>> /var/tmp/ssh_debug/sensor_out.txt

    # redirect stderr to log file
    echo $dt >> /var/tmp/ssh_debug/sensor_err.txt
    exec 2>> /var/tmp/ssh_debug/sensor_err.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    /usr/bin/python3 ~/MotheterRemote/ssh/sensor_stream.py 2>> /var/tmp/ssh_debug/sensor_err.txt 1>>/var/tmp/ssh_debug/sensor_out.txt &
elif test $num_inst >= 1; then
    echo "More than one instance of sensor_streaming.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi

num_inst=$(echo "$all_procs" | grep [p]ysqm | wc -l)

if test $num_inst == 1 ; then # grep found one thing (the actual program)
    echo "Already running pysqm module"
elif test $num_inst == 0; then # grep didn't find program
    echo "Pysqm module not running! Attempting to start now."

    # redirect stdout to log file
    echo $dt >> /var/tmp/ssh_debug/pysqm_out.txt
    exec 1>> /var/tmp/ssh_debug/pysqm_out.txt

    # redirect stderr to log file
    echo $dt >> /var/tmp/ssh_debug/pysqm_err.txt
    exec 2>> /var/tmp/ssh_debug/pysqm_err.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    cd ~/MotheterRemote/Py3SQM
    /usr/bin/python3 -m pysqm 2>> /var/tmp/ssh_debug/pysqm_err.txt 1>>/var/tmp/ssh_debug/pysqm_out.txt &

    num_inst=$(echo "$all_procs" | grep [p]ysqm | wc -l)
    echo $num_inst
elif test $num_inst >= 1; then
    echo "More than one instance of pysqm is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi