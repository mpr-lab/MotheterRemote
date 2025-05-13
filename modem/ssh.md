# VPS ssh-ing

on both the rpi and VPS, do `ssh-keygen -t ed25519` to get ssh keys. if they already exist, cool. if not, you'll have to hit enter twice to make it without a password.

on rpi, do `ssh -R 9000:localhost:22 root@45.77.158.194`. give password, make sure it connects. then back out of it and do `ssh-copy-id root@45.77.158.194`. give the password.

with the rpi ssh active: on VPS, do `ssh pi@localhost -p 9000`. give password.

but `ssh-copy-id` doesn't work...? `ssh-copy-id -i id_ed25519.pub pi@localhost` is what I'm trying (from VPS to rpi, while rpi ssh'd into VPS). getting `permission denied` every time, regardless of what password I use.

From the rpi, do `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~/.ssh`. (I did `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~/Desktop` then `mv Desktop/id_ed25519.pub ~/.ssh` because I was lazy).

then `cat id_ed25519.pub >> authorized_keys`, then `rm id_ed25519.pub`

## the fast version

rpi terminal:

- `ssh-keygen -t ed25519`
- `ssh-copy-id root@45.77.158.194` (give password)
- `scp root@45.77.158.194:/root/.ssh/id_ed25519.pub ~`
- `cat ~/id_ed25519.pub >> authorized_keys`
- `rm ~/id_ed25519.pub`

test connection with `ssh -R 9000:localhost:22 root@45.77.158.194` from rpi, then `ssh pi@localhost -p 9000` while in vps. neither should need a password.

## current setup

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

# ?

`ssh -R 2222:localhost:22 root@45.77.158.194` actually works, run from rpi

`sudo iptables -A INPUT -p tcp --dport 2222 -j ACCEPT` to open port 2222

OR `ufw allow 2222` (and `ufw status` to check)

did that for 2222 and 22, and 2222/tcp and 22/tcp. still no dice

on vps, go to /etc/ssh/sshd_config and set GatewayPorts yes (and uncomment it)

Then restart SSH on the remote server:sudo systemctl restart sshd

then `ssh -R 8000:localhost:4444 root@45.77.158.194`

## new problem

`ssh -R 8000:localhost:4444 root@45.77.158.194` on rpi, then `ssh root@45.77.158.194 -p 8000` on laptop. gives "connection reset by peer" error.

## new strat?

PI: `python -m http.server 4444`

PI: `ssh -R 8000:localhost:4444 root@45.77.158.194`




working autossh command:
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -R 4444:localhost:2222 root@45.77.158.194`

same command, but now runs in background (adds `-t` and `&`):
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -t -R 4444:localhost:8000 root@45.77.158.194 &`






PI: `ssh -R 4444:localhost:2222 root@45.77.158.194`

Laptop: `ssh -R 8000:localhost:4444 root@45.77.158.194`

So the server is on PI port 4444. the ssh tunnel forwards PI:4444 to VPS:2222.

### autossh

working autossh command:
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -R 4444:localhost:2222 root@45.77.158.194`

same command, but now runs in background (adds `-t` and `&`):
`autossh -M 0 -o "ExitOnForwardFailure=yes" -o "ServerAliveInterval=10" -o "ServerAliveCountMax=3" -t -R 4444:localhost:2222 root@45.77.158.194 &`

after this, start the server with `python -m http.server 4444` (in the same terminal)

then do `ssh -R 8000:localhost:4444 root@45.77.158.194` from laptop

http://45.77.158.194:8080/ doesn't work????? check VPS ports with `netstat -ano | grep 4444`:

```
tcp        0      0 0.0.0.0:4444            0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp6       0      0 :::4444                 :::*                    LISTEN      off (0.00/0/0)
```

# TODO

cronjob for autossh AND regularly

RPi initiating data dump not priority, but would be nice

"pull" data from RPi
