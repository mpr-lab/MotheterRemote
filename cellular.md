# Cellular Setup

There are three sections in this document:

- [Hardware](#hardware) explains how to set up the cellular modem.
- [Software setup](#software-setup) tests that the modem works properly and that the computer is `ssh` compatible.
- [Code setup](#code-setup) covers how to implement the actual code in this project.

If using the same equipment we used, start with the [Hardware](#hardware) section. If your equipment is different in some way and you need troubleshooting advice, start there as well. If you already have a Linux box with internet access, skip to the [Software setup](#software-setup) section.

The [Software setup](#software-setup) and [Code setup](#code-setup) sections will be relevant regardless of your particular hardware. Do not skip the [Software setup](#software-setup).

## Hardware

The goal of this section is to set up a Raspberry Pi with a cellular modem. The procedure outlined here assumes the following equipment:

- Raspberry Pi 4 Model B
- [Sixfab 4G/LTE Cellular Modem Kit for Raspberry Pi](https://sixfab.com/product/raspberry-pi-4g-lte-modem-kit/), EG25-G (North America and Global), Quectel

If your equipment differs, see the [following section](#generic-instructions). Otherwise, skip to [Sixfab HAT](#sixfab-hat).

### Generic instructions

Always follow the instructions and documentation provided with your hardware, using this tutorial as only a backup and source for troubleshooting information. Research your equipment thoroughly to ensure all parts are compatible.

The specific model of Raspberry Pi isn't particularly relevant; we just need a box that runs Linux and can connect to some compatible modem.

SIM plans vary drastically worldwide; choose a provider and plan that cover your area of operation and that are compatible with your modem. This tutorial was designed for GPRS (2.5G), but other cellular protocols should work fine (5G may be overkill).

### Sixfab HAT

Follow the first two steps in the [Sixfab tutorial documentation](https://docs.sixfab.com/docs/raspberry-pi-4g-lte-cellular-modem-kit-getting-started-with-ecm-mode) to set up the hardware and SIM card. Be sure to follow the ECM instructions, as Sixfab CORE will be discontinued in 2025. The provided documentation required further troubleshooting, so the following instructions are the procedure that worked in our case.

At step 3, uninstall the default modem manager with `sudo apt purge modemmanager -y`.

At step 4, be sure to select the right module. Ours was a Quectel, but check your device first!

`lsusb` and `lsusb -t` will show you all current USB connections. Compare the output with the images in the Sixfab tutorial.

Follow [these instructions](README.md/#fixed-usb-port-names) to set a fixed name for the modem (for example, `ttyUSB_modem`). If the modem is plugged in to a different physical port, it can still be recognized by this name. Running `ls -l /dev/tty*` should show you the modem's new name and current USB port.

Go to the [Sending AT Commands](https://docs.sixfab.com/page/sending-at-commands) documentation and briefly review it. Because your Raspberry Pi is likely an "externally-managed environment", `pip3` won't work. Instead, do `sudo apt install pipx` and `pipx install atcom`. Then do `pipx ensurepath` so that it gets added to the `$PATH` environment variable.

Commands can be sent by typing `atcom AT...`, or `atcom --port /dev/ttyUSB2 AT...` if it doesn't find the modem automatically.

If using a Sixfab SIM card, the APN is `super`.

Send the following commands:

```bash
atcom AT+CGDCONT=1,"IPV4V6","super"
atcom AT+QCFG="usbnet",1
atcom AT+CFUN=1,1
```

The modem will reboot after the last command; this lasts less than a minute. All three commands should return `OK`. If they don't, run the following:

```bash
atcom AT+QCFG="usbnet" # Check the configuration of the module. Should return 1
atcom AT+CPIN? # Check the SIM is detected by the module. Should return READY
atcom AT+CREG? # Check if the module is registered to the network. Should return 0,1 or 0,5
atcom AT+CGCONTRDP # Check the APN is right and have an IP. Should return the APN details and IP address.
```

[This documentation](https://docs.sixfab.com/docs/raspberry-pi-3g-4g-lte-base-hat-troubleshooting) is useful for troubleshooting ATCOM.

Run `ifconfig`. You should see either `usb0` or `eth1`; this is the modem interface.

## Software setup

This section assumes you have a working cellular modem on either `usb0` or `eth1` (check your interfaces with `ifconfig`).

If you've just set up the modem, it might have the highest priority; this eats up data incredibly quickly. Check which service is being used with this absolutely bonkers one-liner:

`ip route get 8.8.8.8 | sed -n 's/.*dev \([^\ ]*\).*/\1/p'`

It should be `wlan0` (WiFi). If it's something else (such as `usb0`), do `sudo ip link set usb0 down` as a quick fix (and `sudo ip link set usb0 up` to bring it online again later).

Run `nmcli` and find your cellular connection (look for "Quectel" or "Telit"). It's probably something like "Wired connection 2". Do `nmcli -f NAME,UUID,AUTOCONNECT,AUTOCONNECT-PRIORITY c` and check the priority of the cellular interface. It should be `-999` (negative means higher priority). If it isn't -999, that doesn't necessarily mean there's a problem! Just keep it in mind in case further troubleshooting is needed later.

We also want to view the metric on the routing table. `route -n` lets us see all interfaces and their metric numbers. We want our cellular modem to have a higher metric number (and therefore lower priority) than the WiFi `wlan0`. If it's already higher, leave it as is. To change it, run `sudo apt install ifmetric` and `sudo ifmetric wlan0 50` (or any number lower than cellular). Run `route -n` again to make sure it worked, then try `ip route get 8.8.8.8 | sed -n 's/.*dev \([^\ ]*\).*/\1/p'` and see if it returns `wlan0`.

Next, we need to ensure this change carries over if the Raspberry Pi is rebooted; we'll do this with a simple cronjob. Run `crontab -e` and add `@reboot sudo ifmetric wlan0 50` in a new line. Test that this works by restarting the Pi and running `route -n`.

### Tailscale

Tailscale is a VPN service that essentially creates a virtual LAN. Devices that are logged in on a network are given IP addresses and can be accessed by any other networked device.

Log in to Tailscale with a GitHub account; this can be a personal or organization account. Other users can be added later via email or an invite link, but only three users are allowed on a free plan.

On your computer, go to [the Tailscale download page](https://tailscale.com/download) and get the app. Up to a hundred devices can be added for free, so don't worry about having too many devices online.

The Raspberry Pi probably runs Raspbian Bullseye, so [follow the instructions](https://tailscale.com/download/linux/rpi-bullseye), which are also copied below:

```bash
sudo apt-get install apt-transport-https

curl -fsSL https://pkgs.tailscale.com/stable/raspbian/bullseye.noarmor.gpg | sudo tee /usr/share/keyrings/tailscale-archive-keyring.gpg > /dev/null
curl -fsSL https://pkgs.tailscale.com/stable/raspbian/bullseye.tailscale-keyring.list | sudo tee /etc/apt/sources.list.d/tailscale.list

sudo apt-get update
sudo apt-get install tailscale
```

Run `sudo tailscale up`, and go to the link it gives you to log in. You can go to this link from another device, if you don't want to deal with using a web browser on a headless Pi.

On the tailscale browser console, you can see the IP addresses of all connected devices. You can ssh to them via those IP addresses (`ssh pi@100.88.15.3`), or just with the computer's name (`ssh pi@pi`). It really is that easy!

The crontab `@hourly sudo tailscale up` will keep the connection active. I don't recommend running it on your personal computer, but it should be fine on the RPi and a dedicated work machine.

## Code setup
