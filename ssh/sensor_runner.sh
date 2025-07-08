#!/bin/bash

echo "Running sensor_runner.sh"

rpi_dir="~/sqmdata"
debug_dir=$rpi_dir/ssh_debug
repo_dir="~"

# make log file directory, if it doesn't exist
if [[ ! -e $debug_dir ]]; then
    mkdir -p $debug_dir
fi

# make log files, if they don't exist
if [[ ! -e $debug_dir/sensor_out.txt ]]; then
    touch $debug_dir/sensor_out.txt
fi
if [[ ! -e $debug_dir/sensor_err.txt ]]; then
    touch $debug_dir/sensor_err.txt
fi
if [[ ! -e $debug_dir/pysqm_out.txt ]]; then
    touch $debug_dir/pysqm_out.txt
fi
if [[ ! -e $debug_dir/pysqm_err.txt ]]; then
    touch $debug_dir/pysqm_err.txt
fi

# get date/time
dt="$(date '+%d/%m/%Y %H:%M:%S');"

all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [s]ensor_stream | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running sensor_streaming.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Sensor streaming program not running! Attempting to start now."

    # put dates in log files
    echo $dt >> $debug_dir/sensor_out.txt
    echo $dt >> $debug_dir/sensor_err.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    /usr/bin/python3 $repo_dir/MotheterRemote/ssh/sensor_stream.py 2>> $debug_dir/sensor_err.txt 1>>$debug_dir/sensor_out.txt &
elif test $num_inst > 1; then
    echo "More than one instance of sensor_streaming.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi

num_inst=$(echo "$all_procs" | grep [p]ysqm | wc -l)

if test $num_inst == 1 ; then # grep found one thing (the actual program)
    echo "Already running pysqm module"
elif test $num_inst == 0; then # grep didn't find program
    echo "Pysqm module not running! Attempting to start now."

    # put dates in log files
    echo $dt >> $debug_dir/pysqm_out.txt
    echo $dt >> $debug_dir/pysqm_err.txt

    # run python program in background (don't wait for it to finish, just let shell die)
    cd $repo_dir/MotheterRemote/Py3SQM
    /usr/bin/python3 -m pysqm 2>> $debug_dir/pysqm_err.txt 1>>$debug_dir/pysqm_out.txt &
elif test $num_inst > 1; then
    echo "More than one instance of pysqm is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi