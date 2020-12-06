package net.Indyuce.bountyhunters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.Indyuce.bountyhunters.api.CustomItem;
import net.Indyuce.bountyhunters.api.Message;
import net.Indyuce.bountyhunters.api.ParticlesRunnable;
import net.Indyuce.bountyhunters.api.PlayerData;
import net.Indyuce.bountyhunters.command.AddBountyCommand;
import net.Indyuce.bountyhunters.command.BountiesCommand;
import net.Indyuce.bountyhunters.command.HuntersCommand;
import net.Indyuce.bountyhunters.command.completion.AddBountyCompletion;
import net.Indyuce.bountyhunters.command.completion.BountiesCompletion;
import net.Indyuce.bountyhunters.comp.BountyHuntersPlaceholders;
import net.Indyuce.bountyhunters.comp.placeholder.DefaultParser;
import net.Indyuce.bountyhunters.comp.placeholder.PlaceholderAPIParser;
import net.Indyuce.bountyhunters.comp.placeholder.PlaceholderParser;
import net.Indyuce.bountyhunters.gui.PluginInventory;
import net.Indyuce.bountyhunters.gui.listener.GuiListener;
import net.Indyuce.bountyhunters.listener.BountyClaim;
import net.Indyuce.bountyhunters.manager.BountyManager;
import net.Indyuce.bountyhunters.manager.HuntManager;
import net.Indyuce.bountyhunters.version.PluginVersion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class BountyHunters extends JavaPlugin {
	public static BountyHunters plugin;
	private static PluginVersion version;
	private static PlaceholderParser placeholderParser;

	private static BountyManager bountyManager;
	private static HuntManager huntManager;

	private static Permission permission;
	private static Economy economy;

	private static FileConfiguration levels;
	private static FileConfiguration leaderboard;

	public void onEnable() {
		try {
			version = new PluginVersion(Bukkit.getServer().getClass());
			getLogger().log(Level.INFO, "Detected Server Version: " + version.toString());
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Your server version is not compatible.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// load first the plugin, then hunters and
		// last bounties (bounties need hunters setup)
		plugin = this;
		huntManager = new HuntManager();
		bountyManager = new BountyManager();

		// listeners
		Bukkit.getServer().getPluginManager().registerEvents(new BountyClaim(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new GuiListener(), this);

		saveDefaultConfig();

		// placeholders compatibility
		placeholderParser = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null ? new PlaceholderAPIParser() : new DefaultParser();
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
		leaderboard = ConfigData.getCD(this, "/cache", "leaderboard");

		// target particles
		if (BountyHunters.plugin.getConfig().getBoolean("target-particles.enabled"))
			new ParticlesRunnable().runTaskTimer(BountyHunters.plugin, 100, 100);

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

	public void onDisable() {
		bountyManager.saveBounties();

		for (PlayerData playerData : PlayerData.getLoaded())
			playerData.saveFile();

		ConfigData.saveCD(this, leaderboard, "/cache", "leaderboard");

		for (Player t : Bukkit.getOnlinePlayers())
			if (t.getOpenInventory() != null)
				if (t.getOpenInventory().getTopInventory().getHolder() instanceof PluginInventory)
					t.closeInventory();
	}

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

	public static PluginVersion getVersion() {
		return version;
	}

	public static FileConfiguration getCachedLeaderboard() {
		return leaderboard;
	}

	public static PlaceholderParser getPlaceholderParser() {
		return placeholderParser;
	}

	public void reloadConfigFiles() {
		levels = ConfigData.getCD(BountyHunters.plugin, "", "levels");
	}
}
