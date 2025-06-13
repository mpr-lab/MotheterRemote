#!/bin/bash

echo "Running radio_runner.sh"

# make log file directory, if it doesn't exist
if [[ ! -e /var/tmp/ssh_debug ]]; then
    mkdir -p /var/tmp/ssh_debug
fi

# make log files, if they don't exist
if [[ ! -e /var/tmp/ssh_debug/radio_out.txt ]]; then
    touch /var/tmp/ssh_debug/radio_out.txt
fi
if [[ ! -e /var/tmp/ssh_debug/radio_err.txt ]]; then
    touch /var/tmp/ssh_debug/radio_err.txt
fi

# get date/time
dt="$(date '+%d/%m/%Y %H:%M:%S');"

# redirect stdout to log file
touch /var/tmp/ssh_debug/radio_out.txt
echo $dt >> /var/tmp/ssh_debug/radio_out.txt
exec 1>> /var/tmp/ssh_debug/radio_out.txt

# redirect stderr to log file
touch /var/tmp/ssh_debug/radio_out.txt
echo $dt >> /var/tmp/ssh_debug/radio_err.txt
exec 2>> /var/tmp/ssh_debug/radio_err.txt


all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [l]ora_child_ssh | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running lora_child_ssh.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Radio program not running! Attempting to start now."

    # redirect stdout to log file
    echo $dt >> /var/tmp/ssh_debug/radio_out.txt
    exec 1>> /var/tmp/ssh_debug/radio_out.txt

    # redirect stderr to log file
    echo $dt >> /var/tmp/ssh_debug/radio_err.txt
    exec 2>> /var/tmp/ssh_debug/radio_err.txt

    # $1 is first argument when script is called
    # run python file with first arg
    exec python3 ~/MotheterRemote/ssh/lora_child_ssh.py $1 2>> /var/tmp/ssh_debug/radio_err.txt 1>>/var/tmp/ssh_debug/radio_out.txt &
elif test $num_inst >= 1; then
    echo "More than one instance of lora_child_ssh.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi