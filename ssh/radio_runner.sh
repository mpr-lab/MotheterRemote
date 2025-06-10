#!/bin/bash

# make log file directory, if it doesn't exist
if [[ ! -e /var/tmp/ssh_debug ]]; then
    mkdir -p /var/tmp/ssh_debug
fi

# make log files, if they don't exist
if [[ ! -e /var/tmp/ssh_debug/radio_out.txt ]]; then
    echo whoami
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

# $1 is first argument when script is called
# run python file with first arg
exec python3 ~/MotheterRemote/ssh/lora_child_ssh.py $1 2>> /var/tmp/ssh_debug/radio_err.txt 1>>/var/tmp/ssh_debug/radio_out.txt &