#!/bin/bash

# get date/time
dt="$(date '+%d/%m/%Y %H:%M:%S');"

echo "Running sensor_runner.sh"

all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [s]ensor_stream | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running sensor_streaming.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Sensor streaming program not running! Attempting to start now."

    # redirect stdout to log file
    touch /var/tmp/ssh_debug/sensor_output.txt
    echo $dt >> /var/tmp/ssh_debug/sensor_output.txt
    exec 1>> /var/tmp/ssh_debug/sensor_output.txt

    # redirect stderr to log file
    touch /var/tmp/ssh_debug/sensor_error.txt
    echo $dt >> /var/tmp/ssh_debug/sensor_error.txt
    exec 2>> /var/tmp/ssh_debug/sensor_error.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    /usr/bin/python3 ~/MotheterRemote/ssh/sensor_stream.py 2>> /var/tmp/ssh_debug/sensor_error.txt 1>>/var/tmp/ssh_debug/sensor_output.txt &
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi

num_inst=$(echo "$all_procs" | grep [p]ysqm | wc -l)

if test $num_inst == 1 ; then # grep found one thing (the actual program)
    echo "Already running sensor_streaming.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Pysqm module not running! Attempting to start now."

    # redirect stdout to log file
    touch /var/tmp/ssh_debug/pysqm_output.txt
    echo $dt >> /var/tmp/ssh_debug/pysqm_output.txt
    exec 1>> /var/tmp/ssh_debug/pysqm_output.txt

    # redirect stderr to log file
    touch /var/tmp/ssh_debug/pysqm_error.txt
    echo $dt >> /var/tmp/ssh_debug/pysqm_error.txt
    exec 2>> /var/tmp/ssh_debug/pysqm_error.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    /usr/bin/python3 -m ~/MotheterRemote/Py3SQM/pysqm 2>> /var/tmp/ssh_debug/pysqm_error.txt 1>>/var/tmp/ssh_debug/pysqm_output.txt &
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi