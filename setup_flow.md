# setup flow

## host GUI

host auto-setup file runs, gets username and address of host machine. also gets all network interfaces and ip addresses. if this fails, prompt user for this info and provide instructions on how to get it. should save all these options, and check/update them regularly. user should then pick the one(s) they expect to change the least or that are guaranteed static (Tailscale Magic DNS, static Wifi IP given by institution).

### set up an rpi

each rpi should be a separate "item", each with its own setup and configs. ask user for rpi username and hostname.

ask user what the end system will look like:

- will there be a radio-connected rpi?
  - will the other end of the radio be on the host computer?
  - or on another (accessory) rpi?
- is Tailscale being used? (requires host and rpi both have internet access)
  - if yes, ask for rpi's magic dns
  - host magic dns should already be stored from auto-setup, but check it's legit and prompt user for it if needed
- if not TS, how is the host connected to the main rpi?
  - ethernet
  - wifi with static ip
  - wifi without static ip (not recommended)
  - static ddns address (if so, what actual connection type is being used behind it?)
  - cellular with VPS (which i am neither setting up nor debugging nor documenting)
  - cellular without Tailscale or VPS (which i refuse to even consider the practicality of)

then we actually need to set up the rpi itself.

## rpi gui

same auto-setup to get username/hostname and all IPs. pick an IP to use. guide user through process of setting up ssh keys to prevent password trouble (only need it one way, not like rpi will issue its own ssh commands to the host). can't do that without user in terminal (at least not easily), but we can at least generate the right commands for them.

have host send over all its configs, and have it yoink configs from the rpi (initiate from host). this should be a regular thing to ensure everything stays up-to-date, not just a one-time event. should also do every time something changes in setup configs. all configs changes should be made from host side, not rpi, even if there's a rpi-with-radio situation.

### rpi cli (if headless)

make some sort of cli that does the same stuff listed above. ok if janky and unpolished; as long as it's easy to see what goes wrong it should be fine.

## normal operation

rpi runs pysqm, collects data, stores in dir. host program runs rsync on rpi's dir to yoink data. if host changes any pysqm parameters, stop pysqm and reload it. if host changes any connection configs, do config exchange. if host loses connection to rpi, try connecting on all known IP addresses. if rpi loses connection to host, stay open to new connection if possible.

## the radio problem

the problem is that rsync doesn't work over radio. so i wrote a terrible godawful serial/socket rsync protocol that somehow kinda works. there's no options/settings to it; the main rpi and accessory pi just go back and forth comparing files/creation dates/file sizes, then the accessory rpi sends whatever the main rpi is missing. it doesn't work backwards, which is good! we don't want the main rpi forwarding stuff to the accessory.

other than that everything should work as intended, regardless of how the main rpi and host are connected.

## various concerns

i'm not worrying about where precisely stuff gets stored. it's not in tmp, so it's at least semi-permanent, and other than that i really don't care.

the current code for the lora system and actually the whole host-rpi system in general is extremely jank. i wrote it with very limited cli knowledge and having never worked in systems before. so it wasn't bad for a first attempt, but it's pretty obviously a first attempt. reworks strongly suggested.

when originally writing the comms code, i thought the goal was just talking to the sensor remotely. now i can see that the real issue is just data transfer and maintaining a connection with a remote. not that the sensor commands are useless; but i do feel that we're just now getting to the actual hard part.
