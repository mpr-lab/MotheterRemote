#!/bin/bash







autossh -M 0 -gNC -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -L 8090:localhost:22 robsonlab@gridlog.smith.edu



ssh -L 9998:host2:22 -N host1
ssh -L 9999:localhost:1234 -N -p 9998 localhost

ssh -L 9998:host2:22 -N host1
ssh -L 9999:localhost:1234 -N -p 9998 localhost

autossh -M 0 -gNC -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -L 8090:localhost:22 root@45.77.158.194



# on rpi:

# autossh -M 0 -gNC -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -L 15672:45.77.158.194:15672 bastion_host

# # -M specifies which port to use locally, or doesn't monitor at all if set to 0
# M = 0

# # The following are options for ssh, not autossh. autossh passes these to ssh without processing them.
# # -g allows remote hosts to connect to local forwarded ports
# # -N does not execute remote commands
# # -C asks for data to be compressed
# # -o is used to specify config options (see ssh_config man page for more info)

# ExitOnForwardFailure="yes" # if it can't properly set up ports, quit
# ServerAliveInterval="10" # how many seconds before it checks the connection
# ServerAliveCountMax="3" # how many failed connection checks before quitting

# # -L localport:host:hostport tells ssh to send all messages on localport to the host machine at hostport.

# localport=
# host=robsonlab@gridlog.smith.edu
# hostport=






# # environment variables. change these to match your setup!
# piname="pi"
# passphrase=""
# vpsname="root"
# vpsip="45.77.158.194"


# # have the rpi generate an ssh key with no passphrase
# ssh-keygen -t ed25519 -P $passphrase

# # install sshpass
# sudo apt install sshpass

# # sshpass to automate giving password. ssh into vps without strict security checking, on port 9000. & lets this run in the background.
# sshpass -p $passphrase ssh -o StrictHostKeyChecking=no -R 9000:localhost:22 $vpsname@$vpsip

# sshpass -p $passphrase ssh-copy-id $vpsname@$vpsip

# # old version, would still need password so not automated
# # ssh -R 9000:localhost:22 $vpsname@$vpsip &

# # commands for the vps to run
# commands="ssh-keygen -t ed25519 -P $passphrase"

# #
# ssh pi@localhost -p 9000



# # ssh user@host "determine_path; cat filename" >local_filename