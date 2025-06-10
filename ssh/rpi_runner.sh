#!/bin/bash

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