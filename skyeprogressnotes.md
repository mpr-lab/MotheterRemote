# Skye's progress notes

An unedited collection of notes. Most of this has been polished and put into the actual project documentation. Some of this (like the VPS/autossh stuff) probably won't be relevant at all thanks to Tailscale. I figured that putting this all in one place would clean things up while still providing a last resort for future troubleshooting.

## Sixfab setup

used \$25 coupon that came with box, code G289D3 (capitalization matters)
we have another box, so could get $50 total

cheapest plan for US is with AT&T and T-Mobile. selected 500MB (smallest size), and total price up front is \$9. Adding additional sim cards would cost \$2/card.

### actual technical stuff

the rpi is pi@pi

ours is a Quectel, not a Telit.

Sixfab CORE will be discontinued by 2026, so that's not an option. looks like QMI is more advanced than we need. the best option is ECM.

`sudo apt purge modemmanager -y`

the tutorial says to use `pip3`, but that doesn't work with "externally managed environments" like RPis. Instead, do `sudo apt install pipx` and `pipx install atcom`. Then do `pipx ensurepath` so that it gets added to the `$PATH` environment variable.

I did the whole symlink thing from the readme, this time including the modem. using `ls -l dev/tty*`, I know the modem is currently on `ttyUSB2`.

Commands can be sent by typing `atcom AT...`, or `atcom --port /dev/ttyUSB2 AT...` if it doesn't find the modem automatically.

<https://docs.sixfab.com/docs/raspberry-pi-3g-4g-lte-base-hat-troubleshooting> is amazing for troubleshooting atcom!

### setting up the repo

I can't get the mpr-lab one cloned (username/password issue?), so I did the one on SWorster/MotheterRemote.

this works, yippee

second RPi: pi1, pi1, pi1. yeah, i'm not very creative with naming.

pi@10.10.32.91
pi@192.168.225.43 (modem)
pi1@10.10.17.182

on pi: added a crontab to force it to prioritize wifi. `crontab -e`, put `@reboot sudo ifmetric wlan0 50` (can also just run `sudo ifmetric wlan0 50` all the time, but that's not fun). can view/check priority order with `route -n`. lowest metric # = highest priority. the modem will show up as `eth1`, and will probably have metric # 100.

current spring break setup: rpi "pi" has wifi `pi@10.10.32.91` and modem `pi@192.168.225.43`. Connected to SQM sensor and radio module and HDMI monitor. Cannot ssh to the modem (idk why), but can obviously ssh via wifi.

rpi "pi1" has wifi `pi1@10.10.17.182`. Connected to radio only. Can ssh via wifi.

not sure whether the rpi is using the modem or the wifi? `ip route get 8.8.8.8 | sed -n 's/.*dev \([^\ ]*\).*/\1/p'` is a bonkers one-liner that prints the currently-used connection type. we want to see `wlan0`!

### serial monitoring

for all of these, you have to have it active on both rpis.

#### minicom

`sudo apt install minicom`. `minicom -s -c on` to get cool interface, with color!

arrow key down to serial port setup, and hit enter. type `a`, which puts your cursor in the "serial device" field (/dev/modem by default). change it to whatever the LORA radio module is on (/dev/ttyUSB_LORA if set up correctly, see readme or do `ls -l /dev/tty*`). hit enter to confirm, then enter to return to the main menu. then go to "exit" to get the main minicom cli interface.

do ctrl-a then hit z (after releasing ctrl-a). all minicom commands will be visible, and you can type the letter associated with the command to run it. do "e" to get echo turned on, so you can read whatever gets sent (you can also read incoming messages if you are so inclined). if you do commands without the Z help screen, preface it with ctrl-a. so ctrl-a then e toggles the echo. setting, etc. ctrl-a x to exit.

#### and now kermit, yay

`sudo apt install ckermit`.

`kermit`

`set port /dev/ttyUSB_LORA`

`set speed 115200`

and then just follow the manual, idk

#### screen

it sucks. but it works.

`sudo apt install screen`

`screen /dev/ttyUSB_LORA 115200`

to quit, ctrl-a then `:quit` then enter.

## VPS ssh-ing

on both the rpi and VPS, do `ssh-keygen -t ed25519` to get ssh keys. if they already exist, cool. if not, you'll have to hit enter twice to make it without a password.

on rpi, do `ssh -R 9000:localhost:22 root@45.77.158.194`. give password, make sure it connects. then back out of it and do `ssh-copy-id root@45.77.158.194`. give the password.

with the rpi ssh active: on VPS, do `ssh pi@localhost -p 9000`. give password.

but `ssh-copy-id` doesn't work...? `ssh-copy-id -i id_ed25519.pub pi@localhost` is what I'm trying (from VPS to rpi, while rpi ssh'd into VPS). getting `permission denied` every time, regardless of what password I use.

From the rpi, do `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~/.ssh`. (I did `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~/Desktop` then `mv Desktop/id_ed25519.pub ~/.ssh` because I was lazy).

then `cat id_ed25519.pub >> authorized_keys`, then `rm id_ed25519.pub`

### the fast version

rpi terminal:

- `ssh-keygen -t ed25519`
- `ssh-copy-id root@45.77.158.194` (give password)
- `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~`
- `cat ~/id_ed25519.pub >> authorized_keys`
- `rm ~/id_ed25519.pub`

test connection with `ssh -R 9000:localhost:22 root@45.77.158.194` from rpi, then `ssh pi@localhost -p 9000` while in vps. neither should need a password.

### current setup

first mac on smith vpn, second on local wifi

on 2nd mac (which just monitors the vps), `ssh root@45.77.158.194`, password `T-b25LiwK4XRVP!m`

on first mac terminal 1 and 2, `ssh robsonlab@gridlog.smith.edu`

terminal 1 `ssh pi@10.10.32.91` (calling this one pi)

on vps monitor, `ss` to list all subsystems. `ss | grep ssh` to get just ssh. this gives two tcp processes, with `peeraddress:port`. ignore anything with `FIN-WAIT-1`, which is a closed connection.

`67.172.40.196:54115`

`107.173.61.177:36132`

On pi, do `ssh root@44.77.158.194`. `ss` on the vps now shows

`131.229.0.27:59244`

`107.183.61.177:44288`

`107.183.61.177:57826`

`67.172.40.196:54115`

doing `netstat -atnp | grep 'ESTABLISHED.*sshd'` gives three connections.

`131.229.0.27:59244 ESTABLISHED 295827/sshd: root@p`

`107.173.61.177:54158 ESTABLISHED 295827/sshd: unknow`

`67.172.40.196:54115 ESTABLISHED 295827/sshd: root@p`

it seems these ports shift a bunch

`ssh -L 80:intra.example.com:80 gw.example.com`

`ssh -R 8080:localhost:80 public.example.com`

ssh -N -L localhost:3306:DATABASE_MACHINE:3306 BRIDGE_MACHINE_USER@BRIDGE_MACHINE

ssh -N -L localhost:3306:DATABASE_MACHINE:3306 root@45.77.158.194

reverse tunnel on pi to vps, then bridge from laptop through vps to rpi

reverse port forward from pi 22 to vps 22

## ?

`ssh -R 2222:localhost:22 root@45.77.158.194` actually works, run from rpi

`sudo iptables -A INPUT -p tcp --dport 2222 -j ACCEPT` to open port 2222

OR `ufw allow 2222` (and `ufw status` to check)

did that for 2222 and 22, and 2222/tcp and 22/tcp. still no dice

on vps, go to /etc/ssh/sshd_config and set GatewayPorts yes (and uncomment it)

Then restart SSH on the remote server:sudo systemctl restart sshd

then `ssh -R 8000:localhost:4444 root@45.77.158.194`

### new problem

`ssh -R 8000:localhost:4444 root@45.77.158.194` on rpi, then `ssh root@45.77.158.194 -p 8000` on laptop. gives "connection reset by peer" error.

### new strat?

PI: `python -m http.server 4444`

PI: `ssh -R 8000:localhost:4444 root@45.77.158.194`

working autossh command:
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -R 4444:localhost:2222 root@45.77.158.194`

same command, but now runs in background (adds `-t` and `&`):
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -t -R 4444:localhost:8000 root@45.77.158.194 &`

PI: `ssh -R 4444:localhost:2222 root@45.77.158.194`

Laptop: `ssh -R 8000:localhost:4444 root@45.77.158.194`

So the server is on PI port 4444. the ssh tunnel forwards PI:4444 to VPS:2222.

#### autossh

working autossh command:
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -R 4444:localhost:2222 root@45.77.158.194`

same command, but now runs in background (adds `-t` and `&`):
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -t -R 4444:localhost:2222 root@45.77.158.194 &`

after this, start the server with `python -m http.server 4444` (in the same terminal)

then do `ssh -R 8000:localhost:4444 root@45.77.158.194` from laptop

http://45.77.158.194:8080/ doesn't work????? check VPS ports with `netstat -ano | grep 4444`:

```bash
tcp        0      0 0.0.0.0:4444            0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp6       0      0 :::4444                 :::*                    LISTEN      off (0.00/0/0)
```

## TODO

cronjob for autossh AND regularly

RPi initiating data dump not priority, but would be nice

"pull" data from RPi

## steps

follow the ECM instructions on <https://www.jeffgeerling.com/blog/2022/using-4g-lte-wireless-modems-on-raspberry-pi>.

when you're done, instantly do `sudo ip link set usb0 down`. otherwise it will just use "ethernet" (modem) for everything, which eats up the data plan. to use it again, do `sudo ip link set usb0 up`.

do `nmcli -f NAME,UUID,AUTOCONNECT,AUTOCONNECT-PRIORITY c` to see the networks and their priorities. the wired connection (probably #2) should have priority -999.

but that's the order in which it tries to connect to things, not the order in which it tries to use them. so do `route -n`, and see whether the metric number for `usb0` is less than `wlan0`. if it is, do `sudo apt install ifmetric` and `sudo ifmetric wlan0 50` (or any number lower than the ethernet `usb0`).