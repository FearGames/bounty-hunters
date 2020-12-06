package net.Indyuce.bountyhunters.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import net.Indyuce.bountyhunters.BountyHunters;
import net.Indyuce.bountyhunters.ConfigData;
import net.Indyuce.bountyhunters.api.event.HunterLevelUpEvent;

public class PlayerData {
	private static Map<UUID, PlayerData> map = new HashMap<>();

	private UUID uuid;
	private String playerName;
	private FileConfiguration config;

	// caches the list of unlocked stuff
	private List<String> unlocked;

	// last time the player created a bounty
	private long lastBounty = 0;

	private PlayerData(OfflinePlayer player) {
		this.uuid = player.getUniqueId();
		this.playerName = player.getName();
		this.config = ConfigData.getCD(BountyHunters.plugin, "/userdata", uuid.toString());

		for (Value value : Value.values())
			if (!config.contains(value.getPath()))
				config.set(value.getPath(), value.getDefault());

		this.unlocked = config.getStringList("unlocked");
	}

	public static Collection<PlayerData> getLoaded() {
		return map.values();
	}

	public static PlayerData get(OfflinePlayer player) {
		if (!map.containsKey(player.getUniqueId()))
			map.put(player.getUniqueId(), new PlayerData(player));
		return map.get(player.getUniqueId());
	}

	public static boolean isLoaded(UUID uuid) {
		return map.containsKey(uuid);
	}

	public String getPlayerName() {
		return playerName;
	}

	public long getLastBounty() {
		return lastBounty;
	}

	public UUID getUUID() {
		return uuid;
	}

	public int getLevel() {
		return config.getInt("level");
	}

	public int getSuccessfulBounties() {
		return config.getInt("successful-bounties");
	}

	public int getClaimedBounties() {
		return config.getInt("claimed-bounties");
	}

	public List<String> getUnlocked() {
		return unlocked;
	}

	public String getQuote() {
		String format = BountyHunters.getLevelsConfigFile().getString("reward.quote." + config.getString("current-quote") + ".format");
		return AltChar.apply(format == null ? "" : format);
	}

	public String getTitle() {
		String format = BountyHunters.getLevelsConfigFile().getString("reward.title." + config.getString("current-title") + ".format");
		return AltChar.apply(format == null ? "" : format);
	}

	public double getValueDependingOnLevel(ConfigurationSection section, int level) {
		return section.getDouble("base") + section.getDouble("per-level") * level;
	}

	public int getBountiesNeededToLevelUp() {
		int bountiesNeeded = BountyHunters.getLevelsConfigFile().getInt("bounties-per-level");
		return bountiesNeeded - (getClaimedBounties() % bountiesNeeded);
	}

	public String getLevelProgressBar() {
		String lvlAdvancement = "";
		int bountiesNeeded = BountyHunters.getLevelsConfigFile().getInt("bounties-per-level");
		for (int j = 0; j < bountiesNeeded; j++)
			lvlAdvancement += (getClaimedBounties() % bountiesNeeded > j ? ChatColor.GREEN : ChatColor.WHITE) + AltChar.square;
		return lvlAdvancement;
	}

	public ItemStack getProfileItem(Consumer<SkullMeta> callback) {
		ItemStack profile = CustomItem.PROFILE.a().clone();
		SkullMeta profileMeta = (SkullMeta) profile.getItemMeta();
		profileMeta.setDisplayName(profileMeta.getDisplayName().replace("%name%", playerName).replace("%level%", "" + getLevel()));
		List<String> profileLore = profileMeta.getLore();

		String title = hasTitle() ? getTitle() : Message.NO_TITLE.getUpdated();
		for (int j = 0; j < profileLore.size(); j++)
			profileLore.set(j, profileLore.get(j).replace("%lvl-progress%", getLevelProgressBar()).replace("%claimed-bounties%", "" + getClaimedBounties()).replace("%successful-bounties%", "" + getSuccessfulBounties()).replace("%current-title%", title).replace("%level%", "" + getLevel()));

		profileMeta.setLore(profileLore);

		PlayerProfile profile2 = Bukkit.createProfile(uuid);
		if (profile2.hasTextures()) {
			profileMeta.setPlayerProfile(profile2);
		} else {
			Bukkit.getScheduler().runTaskAsynchronously(BountyHunters.plugin, () -> {
				if (!profile2.complete(true) || !profile2.hasTextures()) {
					BountyHunters.plugin.getLogger().warning("Unable to download " + uuid + "'s skin!");
					return;
				}
				Bukkit.getScheduler().runTask(BountyHunters.plugin, () -> {
					profileMeta.setPlayerProfile(profile2);
					callback.accept(profileMeta);
				});
			});
		}

		profile.setItemMeta(profileMeta);
		return profile;
	}

	public void setLastBounty() {
		lastBounty = System.currentTimeMillis();
	}

	public boolean hasQuote() {
		return !getQuote().equals("");
	}

	public boolean hasTitle() {
		return !getTitle().equals("");
	}

	public boolean hasUnlocked(String path) {
		return unlocked.contains(path);
	}

	public void setLevel(int value) {
		config.set("level", value);
	}

	public void setSuccessfulBounties(int value) {
		config.set("successful-bounties", value);
	}

	public void setCurrentQuote(String value) {
		config.set("current-quote", value);
	}

	public void setCurrentTitle(String value) {
		config.set("current-title", value);
	}

	public void setClaimedBounties(int value) {
		config.set("claimed-bounties", value);
	}

	public void setUnlocked(List<String> value) {
		config.set("unlocked", value);
	}

	public void addLevels(int value) {
		setLevel(getLevel() + value);
	}

	public void addSuccessfulBounties(int value) {
		setSuccessfulBounties(getSuccessfulBounties() + value);
	}

	public void addClaimedBounties(int value) {
		setClaimedBounties(getClaimedBounties() + value);
	}

	public void addUnlocked(String value) {
		unlocked.add(value);
	}

	public void checkForLevelUp(Player player) {
		FileConfiguration levels = BountyHunters.getLevelsConfigFile();
		while (levelUp(levels, player)) {
		}
	}

	private boolean levelUp(FileConfiguration levels, Player player) {
		int nextLevel = getLevel() + 1;
		int neededBounties = nextLevel * levels.getInt("bounties-per-level");
		if (getClaimedBounties() < neededBounties)
			return false;

		HunterLevelUpEvent event = new HunterLevelUpEvent(player, nextLevel);
		Bukkit.getPluginManager().callEvent(event);

		Message.CHAT_BAR.format(ChatColor.YELLOW).send(player);
		Message.LEVEL_UP.format(ChatColor.YELLOW, "%level%", "" + nextLevel).send(player);
		Message.LEVEL_UP_2.format(ChatColor.YELLOW, "%bounties%", "" + levels.getInt("bounties-per-level")).send(player);

		List<String> chatDisplay = new ArrayList<String>();

		// titles
		for (String titleId : levels.getConfigurationSection("reward.title").getKeys(false))
			if (nextLevel >= levels.getInt("reward.title." + titleId + ".unlock") && !unlocked.contains(titleId)) {
				addUnlocked(titleId);
				chatDisplay.add(levels.getString("reward.title." + titleId + ".format"));
			}

		// death quotes
		for (String quoteId : levels.getConfigurationSection("reward.quote").getKeys(false))
			if (nextLevel >= levels.getInt("reward.quote." + quoteId + ".unlock") && !unlocked.contains(quoteId)) {
				addUnlocked(quoteId);
				chatDisplay.add(levels.getString("reward.quote." + quoteId + ".format"));
			}

		// send commands
		for (String s : levels.getStringList("reward.commands." + nextLevel))
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("%player%", player.getName()));

		// money
		double money = levels.getDouble("reward.money.base") + (nextLevel * levels.getDouble("reward.money.per-level"));
		BountyHunters.getEconomy().depositPlayer(player, money);

		// send json list
		String jsonList = money > 0 ? "\n" + Message.LEVEL_UP_REWARD.formatRaw(ChatColor.YELLOW, "%reward%", "$" + money) : "";
		for (String s : chatDisplay)
			jsonList += "\n" + Message.LEVEL_UP_REWARD.formatRaw(ChatColor.YELLOW, "%reward%", AltChar.apply(s));

		BaseComponent[] components = new ComponentBuilder(ChatColor.YELLOW + Message.LEVEL_UP_REWARDS.getUpdated())
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(jsonList.substring(1)).create()))
				.create();
		player.sendMessage(components);

		setLevel(nextLevel);
		setUnlocked(unlocked);
		return true;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof PlayerData ? ((PlayerData) object).getUUID().equals(getUUID()) : false;
	}

	public void saveFile() {
		config.set("unlocked", unlocked);
		ConfigData.saveCD(BountyHunters.plugin, config, "/userdata", uuid.toString());
	}

	public enum Value {
		CLAIMED_BOUNTIES(0),
		SUCCESSFUL_BOUNTIES(0),
		LEVEL(0),
		CURRENT_TITLE(""),
		CURRENT_QUOTE(""),
		UNLOCKED(new ArrayList<String>());

		private Object def;

		private Value(Object def) {
			this.def = def;
		}

		public String getPath() {
			return name().toLowerCase().replace("_", "-");
		}

		public Object getDefault() {
			return def;
		}
	}
}
