#!/bin/bash

echo "Running rpi_runner.sh"

rpi_dir="~/sqmdata"
debug_dir="$rpi_dir/ssh_debug"
repo_dir="~"

# make log file directory, if it doesn't exist
if [[ ! -e $debug_dir ]]; then
    mkdir -p $debug_dir
fi

# make log files, if they don't exist
if [[ ! -e $debug_dir/rpi_out.txt ]]; then
    touch $debug_dir/rpi_out.txt
fi
if [[ ! -e $debug_dir/rpi_err.txt ]]; then
    touch $debug_dir/rpi_err.txt
fi

all_procs=$(ps -ef)
num_inst=$(echo "$all_procs" | grep [r]pi_ssh | wc -l)

if test $num_inst == 1; then # grep found one thing (the actual program)
    echo "Already running rpi_ssh.py. It should die after completing, instead of staying alive."
elif test $num_inst == 0; then # grep didn't find program
    echo "Starting new rpi_ssh.py instance now."

    # get date/time
    dt="$(date '+%d/%m/%Y %H:%M:%S');"

    # put dates in log files
    echo $dt >> $debug_dir/rpi_out.txt
    echo $dt >> $debug_dir/rpi_err.txt

    # $1 is first argument when script is called
    # run python file with first arg
    exec python3 $repo_dir/MotheterRemote/ssh/rpi_ssh.py $1 2>> $debug_dir/rpi_err.txt 1>>$debug_dir/rpi_out.txt &
elif test $num_inst > 1; then
    echo "More than one instance of rpi_ssh.py is running!"
else # something else went wrong
    echo "Command failed for unknown reasons; manual debugging required."
fi