/**
 * This file was written by Mingwei Samuel <mingwei.samuel@gmail.com>
 * Copyright (c) 2014 by Mingwei Samuel
 * 
 * See the included COPYING file for usage information. If no COPYING
 * file is included, this software is damaged and is likely stolen.
 * Please report missing COPYING files to <mingwei.samuel@gmail.com>
 * 
 * File:    Memkit.java
 * Project: Memkit
 * Created: 10:13:17 AM Jul 7, 2014
 */
package com.lugtech.memkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 
 * @author Mingwei Samuel
 */
public class Memkit extends JavaPlugin implements CommandExecutor, Listener, Runnable {
	
	/** number of bytes in a MB */
	private static final int MB = 1048576;
	/** how often to update the log */
	private static int FREQ = 200; //10 sec
	/** available functions */
	private static final String[] FUNCTIONS = new String[] { "threads", "worlds", "gc" };
	
	/** writes to log */
	private FileWriter usageLog;
	/** writes to cmd log */
	private FileWriter cmdLog;
	
	/** annotations */
	private final Queue<String> annotations = new LinkedList<String>();
	
	/** instance for reading stats */
	private PerformanceMonitor monitor;
	
	/* (non-Javadoc)
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onEnable() */
	@Override
	public void onEnable() {
		this.getDataFolder().mkdirs();
		
		this.getConfig().options().copyDefaults(true);
		this.getConfig().addDefault("directory", "./");
		String dir = this.getConfig().getString("directory");
		this.getConfig().addDefault("tickrate", 200);
		FREQ = this.getConfig().getInt("tickrate");
		this.saveConfig();
		
		File html = new File(this.getDataFolder(), dir + "graph.html");
		new File(this.getDataFolder(), dir).mkdirs();
		if (!html.exists()) {
			try {
				copyFile(this.getResource("graph.html"), html);
			}
			catch (IOException ioe) {
				this.getLogger().log(Level.SEVERE, "Could not save graph.html", ioe);
				return;
			}
		}
		
		File usageFile = new File(this.getDataFolder(), dir + "mem.log");
		try {
			usageFile.createNewFile();
			usageLog = new FileWriter(usageFile, true);
		}
		catch (IOException ioe) {
			this.getLogger().log(Level.SEVERE, "Could not open logging output stream " + usageFile.getName(), ioe);
			return;
		}
		
		File cmdFile = new File(this.getDataFolder(), dir + "cmd.log");
		try {
			cmdFile.createNewFile();
			cmdLog = new FileWriter(cmdFile, true);
		}
		catch (IOException ioe) {
			this.getLogger().log(Level.SEVERE, "Could not open logging output stream " + cmdFile.getName(), ioe);
			return;
		}
		
		annotations.add("<enabled>");
		
		this.getCommand("mem").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		monitor = new PerformanceMonitor();
		Bukkit.getScheduler().runTaskTimer(this, this, 10, FREQ);
	}
	
	/* (non-Javadoc)
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onDisable() */
	@Override
	public void onDisable() {
		annotations.add("<disabled>");
		run();
		try {
			usageLog.close();
		}
		catch (IOException ioe) {
			this.getLogger().log(Level.WARNING, "Could not close usage log", ioe);
		}
		try {
			cmdLog.close();
		}
		catch (IOException ioe) {
			this.getLogger().log(Level.WARNING, "Could not close cmd log", ioe);
		}
		monitor = null;
	}
	
	/**
	 * Returns a Bukkit-formatted string containing memory information
	 * @return
	 */
	public String[] getMemoryAnalysis() {
		String[] strings = new String[4];
		strings[0] = ChatColor.GOLD + "OS:  " + ChatColor.WHITE + monitor.getOSName() + "(" + monitor.getOSVersion() + ") " + monitor.getJVMArch() + "bit jvm";
		strings[1] = ChatColor.GOLD + "CPU: " + ChatColor.WHITE + "cores: " + monitor.getCPUCount() + ", load: " + monitor.getCPUPercent();
		strings[2] = ChatColor.GOLD + "TC:  " + ChatColor.WHITE + monitor.getThreadCount() + " threads";
		strings[3] = ChatColor.GOLD + "RAM: " + ChatColor.WHITE + monitor.getMemoryUsed() + "/" + monitor.getMemoryAllocated() + "/" + monitor.getMemoryMax()
				+ " MB";
		
		return strings;
	}
	
	public String[] getThreadAnalysis() {
		Thread[] threads = monitor.getThreads();
		List<String> strings = new ArrayList<String>();
		strings.add(ChatColor.GOLD + "Threads: (" + threads.length + ")");
		
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < threads.length; i++) {
			switch (threads[i].getState()) {
			case NEW:
				builder.append(ChatColor.DARK_GREEN);
				break;
			case RUNNABLE:
				builder.append(ChatColor.GREEN);
				break;
			case WAITING:
			case TIMED_WAITING:
				builder.append(ChatColor.YELLOW);
				break;
			case BLOCKED:
				builder.append(ChatColor.RED);
				break;
			case TERMINATED:
				builder.append(ChatColor.DARK_RED);
				break;
			default:
				builder.append(ChatColor.BLACK); //error
			}
			builder.append(threads[i].getName());
			builder.append(", ");
			if (builder.length() > 50) {
				strings.add(builder.toString());
				builder.setLength(0); //reset
			}
		}
		return strings.toArray(new String[strings.size()]);
	}
	
	public String[] getWorldAnalysis() {
		List<World> worlds = Bukkit.getWorlds();
		String[] strings = new String[worlds.size() + 1];
		strings[0] = ChatColor.GOLD + "Worlds: (" + worlds.size() + ")";
		for (int i = 0; i < worlds.size(); i++)
			strings[i + 1] = ChatColor.GREEN + worlds.get(i).getName() + ": " + ChatColor.WHITE + worlds.get(i).getLoadedChunks().length + " chunks, "
					+ worlds.get(i).getEntities().size() + " entities";
		return strings;
	}
	
	/* (non-Javadoc)
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender,
	 * org.bukkit.command.Command, java.lang.String, java.lang.String[]) */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length < 1) {
			Arrays.asList(FUNCTIONS);
		}
		List<String> list = new ArrayList<String>();
		for (String function : FUNCTIONS)
			if (function.toUpperCase().startsWith(args[0].toUpperCase()))
				list.add(function);
		return list;
	}
	
	/* (non-Javadoc)
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command,
	 * java.lang.String, java.lang.String[]) */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1) {
			sender.sendMessage(getMemoryAnalysis());
			return true;
		}
		if (args[0].equalsIgnoreCase("threads")) {
			sender.sendMessage(getThreadAnalysis());
			return true;
		}
		if (args[0].equalsIgnoreCase("worlds")) {
			sender.sendMessage(getWorldAnalysis());
			return true;
		}
		if (args[0].equalsIgnoreCase("gc")) {
			System.gc();
			sender.sendMessage(ChatColor.GOLD + "Asked JVM to run garbage-collector");
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() */
	@Override
	public void run() {
		String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
		try {
			usageLog.append(date + ",");
			usageLog.append(monitor.getCPUPercent() + ",");
			usageLog.append(monitor.getMemoryUsed() + ",");
			usageLog.append(monitor.getMemoryAllocated() + ",");
			usageLog.append(monitor.getMemoryMax() + ",");
			usageLog.append(monitor.getThreadCount() + ",");
			usageLog.append(monitor.getPlayerCount() + "\n");
			usageLog.flush();
		}
		catch (IOException ioe) {
			getLogger().warning("Could not save usage log");
		}
		
		try {
			if (annotations.size() > 0) {
				cmdLog.append("{\"x\": \"" + date + "\", ");
				cmdLog.append("\"text\": \"Commands:");
				for (String ann = annotations.poll(); ann != null; ann = annotations.poll())
					cmdLog.append("\\n" + "/" + ann.replace("\\", "\\\\"));
				cmdLog.append("\"}\n");
				cmdLog.flush();
			}
		}
		catch (IOException ioe) {
			getLogger().warning("Could not save cmd log");
		}
	}
	
	@EventHandler(
			priority = EventPriority.MONITOR)
	public void onCommand(ServerCommandEvent event) {
		if (event.getCommand().isEmpty())
			return;
		annotations.add(event.getCommand());
	}
	
	/**
	 * Class that has methods for reading system usage
	 * 
	 * @author Mingwei Samuel
	 */
	public class PerformanceMonitor {
		/** OS info **/
		private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		/** RAM info **/
		private final Runtime runtime = Runtime.getRuntime();
		
		/**
		 * @return The number of cores avaliable to the JVM
		 */
		public int getCPUCount() {
			return os.getAvailableProcessors();
		}
		
		/**
		 * @return The cpu usage as a fraction out of 1 (I think)
		 */
		public double getCPUUsage() {
			return os.getSystemLoadAverage() / getCPUCount();
		}
		
		/**
		 * @return A formated string of the CPU percent usage, or ?% if unavailable
		 */
		public String getCPUPercent() {
			double usage = getCPUUsage();
			if (usage < 0)
				return "?%";
			return (((double) Math.round(usage * 1000)) / 10 + "%");
		}
		
		/**
		 * @return 32, 64, or zero if unknown
		 */
		public int getJVMArch() {
			String arch = os.getArch();
			if (arch.endsWith("64"))
				return 64;
			if (arch.endsWith("32"))
				return 32;
			return 0;
		}
		
		/**
		 * @return The OS's name
		 */
		public String getOSName() {
			return os.getName();
		}
		
		/**
		 * @return The OS's version
		 */
		public String getOSVersion() {
			return os.getVersion();
		}
		
		/**
		 * @return Heap memory currently used
		 */
		public long getMemoryUsed() {
			return (runtime.totalMemory() - runtime.freeMemory()) / MB;
		}
		
		/**
		 * @return Heap memory currently available
		 */
		public long getMemoryAllocated() {
			return runtime.totalMemory() / MB;
		}
		
		/**
		 * @return Heap memory the JVM is allowed to used overall
		 */
		public long getMemoryMax() {
			return runtime.maxMemory() / MB;
		}
		
		/**
		 * @return A array of threads
		 */
		public Thread[] getThreads() {
			Set<Thread> threads = Thread.getAllStackTraces().keySet();
			return threads.toArray(new Thread[threads.size()]);
		}
		
		/**
		 * @return The number of threads
		 */
		public int getThreadCount() {
			return getThreads().length;
		}
		
		/**
		 * @return The number of online players
		 */
		public int getPlayerCount() {
			return Bukkit.getOnlinePlayers().length;
		}
	}
	
	/**
	 * Copies files util
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	private static void copyFile(InputStream in, File fileOut) throws IOException {
		OutputStream out = new FileOutputStream(fileOut);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		in.close();
	}
}
