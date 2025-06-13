#!/bin/bash

echo "Running rpi_runner.sh"

# make log file directory, if it doesn't exist
if [[ ! -e /var/tmp/ssh_debug ]]; then
    mkdir -p /var/tmp/ssh_debug
fi

# make log files, if they don't exist
if [[ ! -e /var/tmp/ssh_debug/rpi_out.txt ]]; then
    touch /var/tmp/ssh_debug/rpi_out.txt
fi
if [[ ! -e /var/tmp/ssh_debug/rpi_err.txt ]]; then
    touch /var/tmp/ssh_debug/rpi_err.txt
fi

all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [r]pi_ssh | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running rpi_ssh.py. It should die after completing, instead of staying alive."
elif test $num_inst == 0; then # grep didn't find program
    echo "Starting new rpi_ssh.py instance now."

    # get date/time
    dt="$(date '+%d/%m/%Y %H:%M:%S');"

    # redirect stdout to log file
    echo $dt >> /var/tmp/ssh_debug/rpi_out.txt
    exec 1>> /var/tmp/ssh_debug/rpi_out.txt

    # redirect stderr to log file
    echo $dt >> /var/tmp/ssh_debug/rpi_err.txt
    exec 2>> /var/tmp/ssh_debug/rpi_err.txt

    # $1 is first argument when script is called
    # run python file with first arg
    exec python3 ~/MotheterRemote/ssh/rpi_ssh.py $1 2>> /var/tmp/ssh_debug/rpi_err.txt 1>>/var/tmp/ssh_debug/rpi_out.txt &
elif test $num_inst > 1; then
    echo "More than one instance of rpi_ssh.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi