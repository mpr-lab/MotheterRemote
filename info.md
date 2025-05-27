# Sixfab setup

used $25 coupon that came with box, code G289D3 (capitalization matters)
we have another box, so could get $50 total

cheapest plan for US is with AT&T and T-Mobile. selected 500MB (smallest size), and total price up front is $9. Adding additional sim cards would cost $2/card.

## actual technical stuff

the rpi is pi@pi

ours is a Quectel, not a Telit.

Sixfab CORE will be discontinued by 2026, so that's not an option. looks like QMI is more advanced than we need. the best option is ECM.

`sudo apt purge modemmanager -y`

the tutorial says to use `pip3`, but that doesn't work with "externally managed environments" like RPis. Instead, do `sudo apt install pipx` and `pipx install atcom`. Then do `pipx ensurepath` so that it gets added to the `$PATH` environment variable.

I did the whole symlink thing from the readme, this time including the modem. using `ls -l dev/tty*`, I know the modem is currently on `ttyUSB2`.

Commands can be sent by typing `atcom AT...`, or `atcom --port /dev/ttyUSB2 AT...` if it doesn't find the modem automatically.

<https://docs.sixfab.com/docs/raspberry-pi-3g-4g-lte-base-hat-troubleshooting> is amazing for troubleshooting atcom!

## setting up the repo

I can't get the mpr-lab one cloned (username/password issue?), so I did the one on SWorster/MotheterRemote.

this works, yippee

second RPi: pi1, pi1, pi1. yeah, i'm not very creative with naming.

pi@10.10.32.91
pi@192.168.225.43 (modem)
pi1@10.10.17.182

on pi: added a crontab to force it to prioritize wifi. `crontab -e`, put `@reboot sudo ifmetric wlan0 50` (can also just run `sudo ifmetric wlan0 50` all the time, but that's not fun). can view/check priority order with `route -n`. lowest metric # = highest priority. the modem will show up as `eth1`, and will probably have metric # 100.

current spring break setup: rpi "pi" has wifi `pi@10.10.32.91` and modem `pi@192.168.225.43`. Connected to SQM sensor and radio module and hdmi monitor. Cannot ssh to the modem (idk why), but can obviously ssh via wifi.

rpi "pi1" has wifi `pi1@10.10.17.182`. Connected to radio only. Can ssh via wifi.

not sure whether the rpi is using the modem or the wifi? `ip route get 8.8.8.8 | sed -n 's/.*dev \([^\ ]*\).*/\1/p'` is a bonkers one-liner that prints the currently-used connection type. we want to see `wlan0`!

## serial monitoring

for all of these, you have to have it active on both rpis.

### minicom

`sudo apt install minicom`. `minicom -s -c on` to get cool interface, with color!

arrow key down to serial port setup, and hit enter. type `a`, which puts your cursor in the "serial device" field (/dev/modem by default). change it to whatever the LORA radio module is on (/dev/ttyUSB_LORA if set up correctly, see readme or do `ls -l /dev/tty*`). hit enter to confirm, then enter to return to the main menu. then go to "exit" to get the main minicom cli interface.

do ctrl-a then hit z (after releasing ctrl-a). all minicom commands will be visible, and you can type the letter associated with the command to run it. do "e" to get echo turned on, so you can read whatever gets sent (you can also read incoming messages if you are so inclined). if you do commands without the Z help screen, preface it with ctrl-a. so ctrl-a then e toggles the echo. setting, etc. ctrl-a x to exit.

### and now kermit, yay

`sudo apt install ckermit`.

`kermit`

`set port /dev/ttyUSB_LORA`

`set speed 115200`

and then just follow the manual, idk

### screen

it sucks. but it works.

`sudo apt install screen`

`screen /dev/ttyUSB_LORA 115200`

to quit, ctrl-a then `:quit` then enter.
