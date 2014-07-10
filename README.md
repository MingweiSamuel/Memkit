#Memkit
Memkit is a simple, light-weight bukkit plugin used for monitoring a server's use of memory, cpu, and more.

##Uses
- Memory usage (RAM)
- Processor load (CPU)
- Running threads
- Loaded chunks and entities
- Ask the JVM to free more memory
- Graphs RAM, CPU, threads, and players, alongside sent commands

##Quickstart
###Installation
1. Download memkit.jar and save the file in your server's `plugins/` folder.
2. Memkit will be run the next time your server restarts or `/reload` is used.
Memkit can be configured after the first run by editing the configuration files in the `plugins/Memkit/` folder.

###Usage
####Commands
- `/mem` - Displays a short summary of current server load
- `/mem threads` - Displays all running threads and their current state based on color:  
![Started: DARK_GREEN, Running: GREEN, Waiting: YELLOW, Blocked: RED, Terminated: DARK_RED](http://i.imgur.com/nXjt3Gn.png)
- `/mem worlds` - Lists each world along with the number of **loaded** chunks and entities
- `/mem gc` - Asks the JVM to "garbage-collect" unused object, thus freeing memory

#####Permissions
- `mem` - Allows user to use the `/mem ...` command. (default: op)

####Configuration (config.yml)
There are two parameters in the config.yml:
- `directory` (default: `./`)  
The directory to save the log files. The path is relative to Memkit's data folder
- `tickrate` (default: `200` - 10 sec)  
Sets how often Memkit should log server information in ticks. A lower number will result in larger but more detailed log file. (20 ticks = 1 second)

####Graphing
To view the graph, open `graph.html` with your web browser. If you are running Bukkit on a headless server, download `graph.html` along with `mem.log` and `cmd.log`, and open it on your local machine.  
The graph displays CPU and RAM usage, along with the number of players and threads. Commands are displayed as a "C" at the x-axis and can be hovered to show their content. A small section of the graph can be selected by clicking and draging, and can be reset by double-clicking.

##FAQ
- Does Memkit track my server?  
Unlike some plugins, Memkit does not track any part or statistic of your server. All data stays on your machine.
- Why doesn't Memkit show my CPU usage?  
Some JVMs, especially on Windows, do not implement a method to check CPU usage because it would be too difficult to calculate. There is no way around this.
- My CPU usage is above 100%. How is this possible?  
If the CPU usage is above 100%, it means the CPU is getting more requests than it can handle, causing calculations to back up. As a rule of thumb, average CPU usage below 70% is reasonable.
- Where can I submit a bug report?  
Bug can be reported [here](http://github.com/Lugtech/Memkit/issues) if any arise.
