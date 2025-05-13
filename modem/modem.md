# steps

follow the ECM instructions on <https://www.jeffgeerling.com/blog/2022/using-4g-lte-wireless-modems-on-raspberry-pi>.

when you're done, instantly do `sudo ip link set usb0 down`. otherwise it will just use "ethernet" (modem) for everything, which eats up the data plan. to use it again, do `sudo ip link set usb0 up`.

do `nmcli -f NAME,UUID,AUTOCONNECT,AUTOCONNECT-PRIORITY c` to see the networks and their priorities. the wired connection (probably #2) should have priority -999.

but that's the order in which it tries to connect to things, not the order in which it tries to use them. so do `route -n`, and see whether the metric number for `usb0` is less than `wlan0`. if it is, do `sudo apt install ifmetric` and `sudo ifmetric wlan0 50` (or any number lower than the ethernet `usb0`).

## autossh

`sudo apt install autossh`