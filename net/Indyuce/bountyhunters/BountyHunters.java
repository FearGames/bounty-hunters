package net.Indyuce.bountyhunters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.Indyuce.bountyhunters.api.BountyManager;
import net.Indyuce.bountyhunters.api.CustomItem;
import net.Indyuce.bountyhunters.api.HuntManager;
import net.Indyuce.bountyhunters.api.Message;
import net.Indyuce.bountyhunters.api.PlayerData;
import net.Indyuce.bountyhunters.command.AddBountyCommand;
import net.Indyuce.bountyhunters.command.BountiesCommand;
import net.Indyuce.bountyhunters.command.HuntersCommand;
import net.Indyuce.bountyhunters.command.completion.AddBountyCompletion;
import net.Indyuce.bountyhunters.command.completion.BountiesCompletion;
import net.Indyuce.bountyhunters.comp.BountyHuntersPlaceholders;
import net.Indyuce.bountyhunters.gui.GuiListener;
import net.Indyuce.bountyhunters.gui.PluginInventory;
import net.Indyuce.bountyhunters.listener.BountyClaim;
import net.Indyuce.bountyhunters.listener.MainListener;
import net.Indyuce.bountyhunters.listener.UpdateNotify;
import net.Indyuce.bountyhunters.nms.json.Json;
import net.Indyuce.bountyhunters.nms.nbttag.NBTTags;
import net.Indyuce.bountyhunters.nms.title.Title;
import net.Indyuce.bountyhunters.util.VersionUtils;
import net.Indyuce.bountyhunters.version.SpigotPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class BountyHunters extends JavaPlugin {

	// plugins
	public static BountyHunters plugin;

	// systems
	private static BountyManager bountyManager;
	private static HuntManager huntManager;
	private static Economy economy;
	private static Permission permission;

	// no reflection nms
	public static Title title;
	public static Json json;
	public static NBTTags nbttags;

	// cached config files
	private static FileConfiguration levels;

	public HashMap<UUID, Long> lastBounty = new HashMap<UUID, Long>();

	public static Economy getEconomy() {
		return economy;
	}

	public static Permission getPermission() {
		return permission;
	}

	public static BountyManager getBountyManager() {
		return bountyManager;
	}

	public static HuntManager getHuntManager() {
		return huntManager;
	}

	public static FileConfiguration getLevelsConfigFile() {
		return levels;
	}

	public void reloadConfigFiles() {
		levels = ConfigData.getCD(BountyHunters.plugin, "", "levels");
	}

	public void onDisable() {
		bountyManager.saveBounties();

		for (PlayerData playerData : PlayerData.getPlayerDatas())
			playerData.saveFile();

		for (Player t : Bukkit.getOnlinePlayers())
			if (t.getOpenInventory() != null)
				if (t.getOpenInventory().getTopInventory().getHolder() instanceof PluginInventory)
					t.closeInventory();

	}

	public void onEnable() {

		// check for latest version
		SpigotPlugin spigotPlugin = new SpigotPlugin(this, 40610);
		if (spigotPlugin.isOutOfDate())
			for (String s : spigotPlugin.getOutOfDateMessage())
				getLogger().log(Level.INFO, "\u001B[32m" + s + "\u001B[37m");

		// load first the plugin, then hunters and
		// last bounties (bounties need hunters setup)
		plugin = this;
		huntManager = new HuntManager();
		bountyManager = new BountyManager();

		// listeners
		Bukkit.getServer().getPluginManager().registerEvents(new BountyClaim(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new MainListener(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new GuiListener(), this);

		saveDefaultConfig();

		if (getConfig().getBoolean("update-notify"))
			Bukkit.getServer().getPluginManager().registerEvents(new UpdateNotify(), this);

		try {
			VersionUtils.version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
			VersionUtils.splitVersion = VersionUtils.version.split("\\_");
			getLogger().log(Level.INFO, "Detected Server Version: " + VersionUtils.version);

			// no reflection nms, each class
			// corresponds to a server version
			title = (Title) Class.forName("net.Indyuce.bountyhunters.nms.title.Title_" + VersionUtils.version.substring(1)).newInstance();
			json = (Json) Class.forName("net.Indyuce.bountyhunters.nms.json.Json_" + VersionUtils.version.substring(1)).newInstance();
			nbttags = (NBTTags) Class.forName("net.Indyuce.bountyhunters.nms.nbttag.NBTTags_" + VersionUtils.version.substring(1)).newInstance();
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Your server version is not compatible.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// placeholderpi compatibility
		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new BountyHuntersPlaceholders().register();
			getLogger().log(Level.INFO, "Hooked onto PlaceholderAPI");
		}

		// vault compatibility
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		RegisteredServiceProvider<Permission> permProvider = getServer().getServicesManager().getRegistration(Permission.class);
		if (economyProvider != null && permProvider != null) {
			economy = economyProvider.getProvider();
			permission = permProvider.getProvider();
		} else {
			getLogger().log(Level.SEVERE, "Couldn't load Vault. Disabling...");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		try {
			File file = new File(getDataFolder(), "levels.yml");
			if (!file.exists())
				Files.copy(BountyHunters.plugin.getResource("default/levels.yml"), file.getAbsoluteFile().toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileConfiguration messages = ConfigData.getCD(this, "/language", "messages");
		for (Message pa : Message.values()) {
			String path = pa.name().toLowerCase().replace("_", "-");
			if (!messages.contains(path))
				messages.set(path, pa.getDefault());
		}
		ConfigData.saveCD(this, messages, "/language", "messages");

		FileConfiguration items = ConfigData.getCD(this, "/language", "items");
		for (CustomItem i : CustomItem.values()) {
			if (!items.contains(i.name())) {
				items.set(i.name() + ".name", i.getName());
				items.set(i.name() + ".lore", i.getLore());
			}
			i.update(items);
		}
		ConfigData.saveCD(this, items, "/language", "items");

		File userdataFolder = new File(getDataFolder() + "/userdata");
		if (!userdataFolder.exists())
			userdataFolder.mkdir();

		ConfigData.setupCD(this, "", "data");
		for (Player p : Bukkit.getOnlinePlayers())
			PlayerData.setup(p);

		// after levels.yml was loaded only
		// else it can't load the file
		reloadConfigFiles();

		// commands
		getCommand("addbounty").setExecutor(new AddBountyCommand());
		getCommand("bounties").setExecutor(new BountiesCommand());
		getCommand("hunters").setExecutor(new HuntersCommand());

		getCommand("addbounty").setTabCompleter(new AddBountyCompletion());
		getCommand("bounties").setTabCompleter(new BountiesCompletion());
	}

	public boolean checkPl(CommandSender sender, boolean msg) {
		boolean b = sender instanceof Player;
		if (!b && msg)
			sender.sendMessage(ChatColor.RED + "This command is for players only.");
		return b;
	}
}