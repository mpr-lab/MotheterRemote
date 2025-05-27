# Serial Monitoring

These tools can be useful for debugging the radios. Each guide assumes you have two radio modules on two separate computers.

## minicom

Install minicom with `sudo apt install minicom`.

Simply running `minicom on` doesn't work, because the default device (probably `/dev/modem`) doesn't exist. We need to change the default device to the radio.

Run `sudo minicom -sc on` to get a cool interface, with color! `-s` puts us in the setup menu, and `-c` enables color. `sudo` is required to save our new configurations later; otherwise, we don't have write permissions.

Use the arrow keys to navigate to `Serial port setup` and hit enter. Type `a`, which puts your cursor in the `Serial Device` field. Replace the default device with the name of your radio, `/dev/ttyUSB_LORA` (or check `ls -l /dev/tty*` if you named it differently). Hit enter to confirm, then again to return to the main menu. Select `Save setup as dfl`; a `Configuration saved` box should appear. Select `Exit` to go to the main minicom CLI. You shouldn't need to repeat this process in the future, so just `minicom` is enough.

The minicom CLI doesn't use the same shortcuts as the normal Terminal. `ctrl-a` goes before every command; you can see a list of these commands with `ctrl-a` followed by `z`. Say we want to clear the screen. `ctrl-a z` shows us that `c` is the clear command; typing `c` from within the `z` menu clears the screen. We can also do `ctrl-a c` if we don't want to go through the help menu.

With two radios active and on minicom, you'll see that whatever gets typed into one minicom terminal appears on the other. If you're only working with a one-sided connection (if you only have one radio set up currently), then it's helpful to turn on echo with `ctrl-a e` so you can see what you're typing.

Exit minicom with `ctrl-a x` (recommended), or `ctrl-a q` (if you're in a hurry and don't want to hang up properly).

## kermit

Kermit is an older utility designed for file transfer over serial. (if I had known about this last year, I would have incorporated it into the LORA comms code instead of DIY-ing an rsync protocol; possible area for future students to explore?).

Install kermit with `sudo apt install ckermit`. Run it with `kermit`. Type `man` to see the manual, and read through the `Basic Commands` section (don't read the whole 1222-page manual).

Type `?` to view all commands. Then type `show ?` to see all arguments for the `show` command. Notice that kermit has auto-filled `show` in the command line to help you.

Set the following attributes:

`set port /dev/ttyUSB_LORA`

`set speed 115200`

From this point forwards, refer to Buddy for help.

## screen

Screen is probably the simplest option of the three. Install it with `sudo apt install screen`, and run it with `screen /dev/ttyUSB_LORA 115200`.

To quit: `ctrl-a`, then `:quit`, then enter.
