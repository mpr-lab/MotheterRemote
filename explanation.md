# Detailed explanation of the codebase

There's a few ways to configure the setup, so I tried to get everything as modular as possible/reasonable.

The host machine is whatever computer we're using at Smith. It has to have a WiFi connection (and be on the same WiFi network as the main RPi)

The main RPi is at MacLeish and is... kind of set up? In theory, it could have the sensor directly attached via serial port, but in our setup has a radio instead. It uses radio to talk to an accessory/child RPi that actually has the sensor.

so: host --WiFi--> main RPi --radio--> accessory RPi --serial--> sensor --serial--> acc RPi --radio--> main RPi --WiFi--> host.

Now, let's go through each file in the comms folder.

## `configs.py`

This just holds the specific info for the machines/connections/locations we're working with. Don't touch it unless you know what you're doing, and always (ALWAYS!) comment out old values instead of deleting them.

## `parse_response.py`

This is how we understand what we get from the sensor. Its responses aren't human-readable, so this just prints them in a way we can understand. This actually can't be run directly as main; you have to call it through another program. This is really a whole bunch of string formatting and unit conversion, so let's move on to more exciting things.

## `ui_commands.py`

The sensor has a lot of built-in commands, but they have to be properly formatted and it's super finnicky. So I made a UI to just handle everything for you! If the UI is run directly (as main) then it doesn't actually send anything, it just prints the formatted command to the terminal.

We can also call this program from another program! In this case, the UI will return the formatted command to whatever called it, instead of just printing it.

## `host_to_client.py`

This is that first connection (host --WiFi--> main RPi) and the last one (main RPi --WiFi--> host). I'll go through this in mostly the order it's written.

Start with `main()`. We start by initializing `conn`, which is the `Server` object. It also references a global `output` (this seems to be a redundancy; I'll take another look at it shortly). Once the server is up, we start a thread with a UI loop so the user can type commands to send over.

What is a thread? Just a separate process so that we can have multiple things happen at once. We don't want the user's UI experience interrupted by incoming messages, and we don't want the incoming messages to be cut off if the user types something.

What is a server? No clue. I made a `Server` class in this file. It uses `socket`, which I will not explain because this does a better job than I ever could: https://www.datacamp.com/tutorial/a-complete-guide-to-socket-programming-in-python

### socket server

Go back to the top of the file. `init` runs whenever we create a server. It creates a host TCP server that's configured the way we want. This basically lets our computer expect and process messages it receives to its address and port. In the try/except, we call that connection `self.server`. If something goes wrong, we just close it, which just crashes the program.

We make a thread just for this server and set it to run forever and keep processing requests. We also tell it that it's a daemon (i.e. not the main thread) so that when the main thread dies this one will also die with it. THEN we actually start the thread.

We also have another socket method `send_to_rpi` that doesn't run when we initialize the server. This one isn't actually a server thing, it's literally just a socket (idk why it's in the `Server` class? another potential bug to fix). This sets up a socket, then tries to connect and send to the RPi's address and port. If something fails, we have options.

Option 1: if `remote_start` is set to true in the configs file, then we do a cool little maneuver with `_start_listener()`. We're connected to the RPi over WiFi, right? So we can just bash our way in! We start a thread on `os.system`, which is the command line, and execute an ssh command. This connects to the RPi and passes along a command to run the `rpi_wifi.py` file. The coolest part is that we're listening to the RPi through this thread! So we can see whatever `rpi_wifi.py`is doing on that machine. `send_to_rpi` just dies, and the user has to send the command again. (There's also a `_kill_listener()` function that kills our cool listener thread; it just uses a regular os command).

Option 2: if we don't have remote starting enabled, the command just doesn't get sent and the socket dies.

### what's a `ThreadedTCPRequestHandler`?

Oversimplified: in order to get TCP servers to do what you want them to do, you need to overwrite whatever it is they do by default. The method is basically "if I get a message, I do this". Originally, it just did nothing. So when we started the server that listens for incoming messages, we used:

```python
self.server = socketserver.TCPServer((host_addr, host_server), ThreadedTCPRequestHandler)
```

That last parameter is something we overwrote down below. First, we make sure it's a socket connection. Then we decode it and strip the whitespace. If there's multiple threads going on, we want to know which one is processing this, so we print the thread name and the data it received. Then we pass the data to a function that formats it.

### Who needs a GUI when the command line will do?

`_print_formatted` is simple, but it's got this weird `allow_ui` thing going on. That's because of the UI loop that I made. Basically, I wanted the user to be able to type commands, but also not get frustrated and keep trying things and inadvertently create a bajillion threads. So once the user enters a command, the UI's while loop stops. When a response is received or three seconds pass, the user can type again.

The UI loop itself is really simple. There's options to use the big UI command program (which should honestly be the default), to rsync (I'll explain shortly), kill the RPi, exit this program, and print all these commands. If there's a command to send, then we send it. Otherwise, we just loop again.

### rsync

This is a Linux command that copies files - it ensures that two directories have the same content, and we can even do this between computers. Sounds pretty cool, right? WRONG. The `os.system` call performs an rsync between the host and RPi. But that RPi doesn't have the data if it's on a radio connection to another RPi that actually has the sensor... so we need to somehow rsync the RPis over radio. We don't handle that here, but we do initiate the process by sending an "rsync" message to the RPi.

## `rpi_wifi.py`

Kind of similar to the host program. `main()` starts with `_device_search()`, which searches for devices. It starts with radio by running `output=lora_parent.Radio()`. This should create a `Radio` object IF a radio unit is connected. If there's no radio, then this will fail and we'll get an exception. It then tries to connect to an SQM sensor using `sensor.py`. If neither a radio nor sensor are found, the program crashes.

If only a radio is found, then we know there's an RPi out there with a radio and sensor attached. If only a sensor is found, then this is the only RPi we need to deal with. If both are found, then we have a serious problem because `rpi_wifi` isn't meant to run on the accessory RPi (but I assume the system will never be set up THAT poorly).

Regardless of which connection is found, it's made into an object called `output`. The sensor and radio have the same methods for integrating with this program, so the code here works the same either way.

Back in main, we start a thread on `_loop()` and then start up a server called `conn`.

### `Server`

This should be familiar, so I won't go into too much detail. Basically the addresses/ports are now set for the RPi instead of the host.

The main difference is in `ThreadedTCPRequestHandler`. Instead of just sending it out to be formatted, we now send the message to the `output` object (either radio or sensor). If it can't connect to the output device, it searches again.

### loop

This loops and listens for messages. We didn't do things this way with the host, because the host was only listening for RPi over WiFi. This RPi already has a back-and-forth with the host thanks to the server, and can forward messages from the host to the output device. but it still needs to receive messages from the output device and forward them to the host.

We start a new thread to listen on. Then we run `output.client_to_rpi()`. Both the sensor and radio have `client_to_rpi()` methods that return strings, so we'll get a string back either way.

The message could be a list or string. If it's a list, we join it together with end-of-line (EOL) characters and send it over the connection to host. Otherwise, we just send the original string.

### p()

I got tired of writing `print(s, flush=True)`, so now we have `p(s)`. basically, print statements don't cooperate well with threads. This forces them to print whatever they have, whenever they have it. I use this function in a bunch of these programs due to sheer laziness, but all of them can be replaced with `print(s, flush=True)`.

## `sensor.py`

This is the program that talks directly to the sensor. It's one of the `output` options from `rpi_wifi.py`, but is also an `output` for the `lora_child.py` program (we'll get to that later). Most of this code is unceremoniously yoinked from the `pysqm` module, but that code SUCKED so I did what I could to simplify it. Did I break it? Might have done. Water under the bridge, right?

The code starts by making a bunch of configurations into global variables, just so things are clearer to see later.

### `main()`

I didn't write this as a `main()` for some reason (I should probably standardize that). It should only be run as main for debugging purposes anyways. It just sends one message to the sensor, then waits for a response, then dies.

If you run it as main, you need to give it an argument: the command you'd want to send to the sensor. Just run it and put `rx` right after (this is a basic reading command). If you don't have the sensor connected, or if your configs are wrong, it'll crash.

It then creates an object for whatever device you have connected and sends a message to that device.

### SQM class

The two sensor types are SQMLU and SQMLE. We have an LU, not an LE, so it's impossible to test how the LE would work, but they should be able to perform the same actions. This means LU and LE can be subclasses of the same SQM class. We'll overwrite any methods that should be performed differently.

Big picture: the sensor needs to be able to have a send-and-receive loop, or read continuously, or return data to whatever called it. Those actions can be coded with the same logic. Instead of doing two versions of all the high-level procedures, we just have two versions of the literal "read" or "send" instructions that are the building blocks for the more complex actions.

I'll start with the high-level overview of each SQM method, and you'll just have to believe me that the methods they call exist and work properly. Then we'll go through the individual LU and LE methods.

#### `_reset_device()`

Just close the connection, wait, and restart it.

#### `_clear_buffer()`

A buffer is something that holds data coming in from some continuous stream. Clearing the buffer lets us start fresh with whatever new thing is coming in. This will be important later!

#### `send_and_receive()`

This is only used in `main()`. It sends a command and returns the response. This is a simplified version of what's in `pysqm`.

It starts by sending a command to the sensor. It waits a bit so that the sensor has time to respond. Then it reads the buffer (gets whatever new data's come in and hasn't been processed yet). We try to decode it (translate from bytes to utf8 string) and return that string. If the new data was empty or malformed, the `except` block gets triggered.

If we've already been through this some maximum number of times, then we just stop instead of hanging. If debug messages were enabled in the configs, then it crashes. Otherwise, it just returns an empty string.

If we still have tries left: wait a bit, reset the device, wait a bit more, try the whole thing again with a recursive call.

#### `start_continuous_read()`, `stop_continuous_read()`, `_listen()`, `_return_collected()`

These run a listener in a thread. The thead itself is called `self.t1`, whether the thread is alive is `self.live`, and the data it's collected is `self.data`.

We start the thread with `start_continuous_read()`, which sets up the variables, puts `_listen()` in a thread, and runs it.

The `_listen()` method is the loop that the thread runs. It alternates between waiting and reading the buffer.

`stop_continuous_read()` kills the thread by setting `self.live` to false and using `join` to wait for the thread to die. Because the loop in `_listen()` only runs when `self.live` is true, this will exit the loop.

`_return_collected()` returns the `self.data` buffer data and clears the buffer.

Notice how none of these methods actually put the data into the buffer. That's handled later!

#### `rpi_to_client()`

This sends a command to the sensor using `_send_command()`.

#### `client_to_rpi()`

This yeets the buffer and returns it.

#### stubs

These all get overwritten in the subclasses, and they function pretty much the same way in both. The stubs for them are up here so that the linter knows what's going on (if you want to know what a linter is, DM me on Slack! I love talking about them).

`start_connection()` starts a connection to the sensor.

`_close_connection()` waits until the sensor stops sending data (basically until the buffer is empty), then closes the connection.

`_read_buffer()` is how the SQM puts info into `self.data`! It pulls from the buffer, translates the bytes into a string, and appends that to `self.data`.

`_send_command()` sends a command to the sensor.

### SQMLE

This part is very sketchy, because a) I don't have this hardware and b) the source code is atrocious.

#### LE `init`

Tries to start a connection. If it can't, then it searches the network to find the sensor. Once it finds it, it starts the connection and clears the buffer.

#### LE `_search`

Instead of using serial like the LU, the LE uses socket (eww). I'm not going to go through the whole thing, but it basically listens for any incoming connections. I assume the LE broadcasts its identity in some way, which makes more sense than pinging every IP on the LAN.

### SQMLU

I spent more time on this than any other chunk of code. I still don't understand it.

#### LU `init`

Tries to connect to the address set in `configs.py`. Starts the connection, clears the buffer. If it can't connect, it searches for the sensor.

#### LU `_search`

Because we're connecting over serial and not socket, it's a physical connection instead of a network. The main difference here is that we can't just wait for the sensor to reach out to us, like the SQMLE does. Instead, we have to ping EVERY port on the RPi. This depends on what type of OS we're on. RPis have to be Linux so having Windows ports is unneccessary, but this worked fine when I pulled it from `pysqm` so I'm not going to mess with it.

For every port, start a serial connection and send an "ix" command. if we get a response back that starts with "i", then that's the sensor port.

#### `send_and_receive`, again

This is deprecated. I have no idea why it's here. It should do the same stuff as the SQM version?

## `lora_parent.py`

This is the other `output` option from the main RPi. It needs to handle comms between the accessory RPi and main RPi, in both directions.

If this is run as main, it initializes a `Radio` object and goes into a testing method (will explain below).

Remember how `rpi_wifi.py` had a listener that waited for messages from `output` and a listener that waited for messages from the host? And how it could also send messages to output or host independently of those listeners? Same deal! But this time, `rpi_wifi.py` is already covering some of the connections on its end. We need to send messages over radio, set up a buffer to store messages from the radio, and give `rpi_wifi.py` whatever's in our buffer when asked.

I'm skipping over some methods because I'll deal with the rsync stuff later.

### `init`

Start a daemon thread on the `_listen()` method, sets up variables.

### `_start_listen()`

If the listener isn't running, start it.

### `_listen()`

This is the listener that runs in the thread. It loops infinitely, but waits a bit between each loop.

It gets the message from the buffer and encodes it to a string. If the string contains "rsync", then we need to do an rsync, which I will explain later. Otherwise, it splits the message (if there's multiple "lines" in the string), and puts each one into the `data` buffer to be sent.

### `_send()`

Sends a message over radio to the child/accessory RPi. If the message is a list, join it using end-of-line characters.

### `return_collected()`

Yoinks data from the buffer, clears the buffer, and returns the data. Gets called whenever we need to send data over WiFi.

### `send_loop()`

This is the UI debugging component that runs when we run the program as main. The user can type in a message to send, and this sends it over radio.

### `rpi_to_client()` - radio version

We already had a sensor version of this; it sent a message to the sensor. We also need to send a message to the sensor, but over radio. Note that it filters out "rsync" and instead forwards "rsync list". This is because the pseudo-rsync thing I coded is awful and overly complex.

### `client_to_rpi()` - radio version

Gets data from the buffer and returns it as a message to send back to the host over WiFi.

## `lora_child.py`

This has to listen for messages over radio and forward them to the sensor, and listen for messages from the sensor and forward them over radio. Of course, there are exceptions.

Unlike `lora_parent`, this actually should be run as main. It will call `sensor.py` to interact with the device, but otherwise that's all that happens on the accessory/child RPi.

### radio `init`

Starts a radio serial connection as `self.s`. starts a device connection as `self.device`.

Have the device continuously read. This is done through `sensor.py`.

Start two listeners in two separate threads. One listens for incoming radio messages, the other for incoming sensor messages.

### `_listen_radio()`

Loops until killed, pauses between each loop. Reads a message or messages, decodes and splits them. Each message is sent to the sensor.

If the message is rsync-related, do rsync stuff with it.

### `_listen_sensor()`

Loops until killed, pauses between each loop. Gets message from SQM device and sends it over radio.

### `_send`

If the message is a list, join it with end-of-line characters. Then send it over radio.

### `_send_loop()`

This is just for debugging purposes, like the one in `lora_parent.py`. You can set up two RPis with two sensor modules and have them talk back and forth to test the connection.

## RSYNC

rsync is an extremely well made and unbelievably useful protocol. If you rsync two directories (even ones on two different computers over ssh), you end up with the exact same stuff in both directories. If they both have the same file, but with different content, then it gives the newer one precedent. There's also a bunch of cool options for it as well, so it's customizable for a ton of scenarios.

But that only works on a network. We do not have a network. We have two radio modules that are about as cooperative as sulfuric acid and chlorine bleach. It's pretty clear that the standard rsync protocol wouldn't work at all here.

So I made rsync. But worse.

### If there's no radio

...then things are easy. That's just regular rsync over ssh! Which is exactly what we do between the host and main RPi in `host_to_client.py`. In fact, that's so powerful that it completely covers both the non-radio case AND the non-radio half of the radio setup.

### if there's a radio

...then the host and main RPi still do regular rsync to transfer files between them. But we still need to get files from the accessory RPi to the main RPi. Here's how that chain goes.

The host sends a "rsync" message to the main RPi. `rpi_wifi` calls `lora_parent` to forward that message over radio. However, `lora_parent` instead sends "rsync list" to be more specific about what we want (the list of files the accessory RPi has collected).

`lora_child.Ser._listen_radio()` gets "rsync list" over the radio. it sees "rsync" in the message, so it forwards it to `lora_child.Ser._rsync()`. "list" is in the message, so we get a list of files to send with `_get_file_list()`.

`_get_file_list()` uses an inner function `_all_file_list()` to get all .dat files in the specified directory. It does this using `os.listdir` to get all files, then recursing through any folders it finds. If it sees a .dat file, it adds it to the list.

Once `_get_file_list()` has the list of .dat files, it finds the mtime (when the file was last modified) and appends it to the name. It throws all that info into one string that starts with "rsync files", then returns it to `_rsync()`, which sends it over radio.

`lora_parent.Radio._listen()` gets a message with "rsync" over the radio, so it passes it to `_rsync_from_radio()`. The message starts with "rsync files", so it passes it to `_compare_files()`.

`_compare_files()` uses the same `_get_file_list()` method that `lora_child` used earlier. It compares each file and date to figure out which files the child needs to send over (files that the parent doesn't have, or that are newer than the parent's version). For each file, it sends a message over radio using `_ask_child_for_file()`; these messages are formatted as "rsync {filename}".

`lora_child` gets the "rsync {filename}" message over radio. If it can't find that file, it quits. Otherwise, it sets up a bytearray (a large mutable buffer that we can throw the data into). It starts the bytearray with "rsync {short filename}". Then it reads the file and throws it into the bytearray. It then sends it over radio.

`lora_radio._rsync_from_radio()` gets a "rsync" message that doesn't start with "rsync files", so it must be a .dat file to store. It gets the filename from the first line, then writes the new file (or overwrites the old one).

### the main takeaway

To a hammer, every problem is a nail. To a SURF student with no IoT experience and a background in math, every bug is a systematic flaw that needs to be over-engineered out of existence.

I plan on making some improvements to rsync this semester (or having you do it, if I can convince you). The current implementation DOES work; it's just very inelegant.

## The scripts

Say a worst-case scenario happens – Smith's WiFi goes out, there's a blackout, etc. We don't want to go out to MacLeish just to reset the RPis! Instead, I set up some nifty cronjobs to automatically run everything.

### What's cron?

Linux systems can be given tasks to perform at scheduled times. This could be once a minute, or every day, or at 22:00 on day-of-month 2 and 3 and on every day-of-week from Monday through Friday in July (seriously, it's `0 22 2,3 7 1-5`). Play around with this https://crontab.guru/#0_22_2,3_7_1-5 to get a sense of how the scheduling works.

All the `.sh` files in the scripts folder are shell scripts. I'll explain the specifics of what they do in a bit. First, take a look at the text file.

### cronjobs.txt

Even though the radio range/connectivity is probably the reason the current setup doesn't work, I'm convinced that my cronjobs would also end up playing a role in bungling things.

Cronjobs start with the scheduling, which for most of these is just `* * * * *`.

Take the first one: `sudo chmod +x ~/MotheterRemote/scripts/runrpi.sh >> /tmp/perm 2>&1`. `sudo` just means we're doing important stuff. `chmod +x` means we're making something executable (otherwise, Linux would err on the side of caution and just not run it). `~/MotheterRemote/scripts/runrpi.sh` is the script we're running. `>>` means we're forwarding the output of that script to some log file, located in `/tmp/perm`. We want to put any error messages into a file, instead of only having them print to the terminal, so we use `2>&1` to redirect `stderr` (file descriptor 2) to `stdout` (file descriptor 1).

The next command is similar. We change the directory to the scripts folder, then run the `runrpi.sh` script and log the output. We do this every minute.

To prevent having a bajillion gigs of error log data, `0 12 * * 0 rm /tmp/perm ; rm /tmp/debug ; rm /tmp/rpi_wifi` deletes these files every Sunday at noon (I regret doing this, because I'd love to know what's going on).

The three commands for the radio RPi are nearly identical.

Then there's the sensor cronjobs. The first and fourth ones should be familiar by now. The second one is also similar, but only runs in certain times.

The third one also runs in certain times. `pkill -f pysqm` kills all processes whose name contains `pysqm`. I suppose I was worried about having the sensor running all the time, so I auto-kill it whenever it "shouldn't" be running (I seem to remember finding a few dozen instances of `pysqm` running at once during testing, which may explain my overzealousness).

### `runrpi.sh`

This is the script that runs on the main RPi.

The shebang at the top of the file `#!/bin/bash`, just specifies that we want to use the system's built-in bash.

`ps -ef | grep [r]pi_wifi` is a bash command that find any processes called "rpi_wifi". But it does this in a really cool way!

#### Excuse me while I nerd out for a moment

`ps` lists running processes (try it in your Terminal, you should get a ton!). The `-ef` flag is actually two flags: `-e` shows all system processes, and `-f` gives a bunch more detail about them.

Then we pipe (give, using the symbol `|`) the output of `ps -ef` to a `grep` command so that we can filter it. We want to find processes with the name `rpi_wifi`. However, we don't want to find the process we're currently running, because a `grep x` command spawns a process containing `x`! Instead, we take advantage of regex.

Say I want to search for both `grey` and `gray`. I can just say `gr[ea]y`, and bash will know that I want any of the possibilities in those brackets. If we only put one character in the brackets, then there's only one possible output: `[r]pi` will always evaluate to `rpi`. However, because the actual `grep` process for `[r]pi_wifi` doesn't contain `rpi_wifi`, we'll only find ACTUAL instances of `rpi_wifi.py`!

#### Back to bash

We want to actually do something with that output, though. We put the output in `processes`.

`$?` contains the value of the last command we ran (`ps...`). If it's equal to 1, that doesn't mean there was one instance found! It means that `grep` found nothing, and exited (quit) with a value of 1, meaning failure. This means that `rpi_wifi.py` isn't running, and we need to start it.

We start it with `nohup` (no hang-up), because we don't want this program to stop running if the computer goes to sleep. The actual command itself should be familiar by now! We have a `&` at the end of the command to force it to run in the background, instead of opening a terminal and spitting out whatever it's doing.

If `grep` actually managed to find something, we don't want to do anything. It's already running, which is what we want.

The other two cases take care of instances where something else might have gone wrong. I never saw either of these in testing.

Both `runradio.sh` and `runsensor.sh` do exactly the same thing with different programs.
