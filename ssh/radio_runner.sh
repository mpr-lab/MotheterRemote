#!/bin/bash

echo "Running radio_runner.sh"

acc_rpi_dir="~/sqmdata"
debug_dir=$acc_rpi_dir/ssh_debug
repo_dir="~"


# make log file directory, if it doesn't exist
if [[ ! -e $debug_dir ]]; then
    mkdir -p $debug_dir
fi

# make log files, if they don't exist
if [[ ! -e /var/tmp/ssh_debug/radio_out.txt ]]; then
    touch $debug_dir/radio_out.txt
fi
if [[ ! -e /var/tmp/ssh_debug/radio_err.txt ]]; then
    touch $debug_dir/radio_err.txt
fi



all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [l]ora_child_ssh | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running lora_child_ssh.py"
elif test $num_inst == 0; then # grep didn't find program
    echo "Radio program not running! Attempting to start now."

    # get date/time
    dt="$(date '+%d/%m/%Y %H:%M:%S');"

    # put dates in log files
    echo $dt >> $debug_dir/radio_out.txt
    echo $dt >> $debug_dir/radio_err.txt

    # $1 is first argument when script is called
    # run python file with first arg
    exec python3 $repo_dir/MotheterRemote/ssh/lora_child_ssh.py $1 2>> $debug_dir/radio_err.txt 1>>$debug_dir/radio_out.txt &
elif test $num_inst > 1; then
    echo "More than one instance of lora_child_ssh.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi