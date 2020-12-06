package net.Indyuce.bountyhunters.command;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import net.Indyuce.bountyhunters.BountyHunters;
import net.Indyuce.bountyhunters.ConfigData;
import net.Indyuce.bountyhunters.api.AltChar;
import net.Indyuce.bountyhunters.api.Bounty;
import net.Indyuce.bountyhunters.api.CustomItem;
import net.Indyuce.bountyhunters.api.Message;
import net.Indyuce.bountyhunters.api.PlayerData;
import net.Indyuce.bountyhunters.api.event.BountyExpireEvent;
import net.Indyuce.bountyhunters.api.event.BountyExpireEvent.BountyExpireCause;
import net.Indyuce.bountyhunters.gui.BountyList;
import net.Indyuce.bountyhunters.version.VersionSound;

public class BountiesCommand implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		// open bounties menu
		if (args.length < 1) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			if (!sender.hasPermission("bountyhunters.list")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			new BountyList((Player) sender, 1).open();
			return true;
		}

		// help
		if (args[0].equalsIgnoreCase("help")) {
			if (!sender.hasPermission("bountyhunters.admin")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "-----------------[" + ChatColor.LIGHT_PURPLE + " BountyHunters Help " + ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "]-----------------");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + "Player Commands");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounty <player> <reward>" + ChatColor.WHITE + " sets a bounty on a player.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties" + ChatColor.WHITE + " shows current bounties.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/hunters" + ChatColor.WHITE + " opens the hunter leaderboard.");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties quotes" + ChatColor.WHITE + " lists available quotes.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties titles" + ChatColor.WHITE + " lists available titles.");
			sender.sendMessage("");
			sender.sendMessage(ChatColor.GRAY + "" + ChatColor.BOLD + "Admin");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties help" + ChatColor.WHITE + " shows the help page.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties reload" + ChatColor.WHITE + " reloads the config file.");
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "/bounties remove <player>" + ChatColor.WHITE + " removes a bounty.");
			return true;
		}

		// remove bounty
		if (args[0].equalsIgnoreCase("remove")) {
			if (!sender.hasPermission("bountyhunters.admin")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "Usage: /bounties remove <player>");
				return true;
			}

			Player player = Bukkit.getPlayer(args[1]);
			if (player == null) {
				sender.sendMessage(ChatColor.RED + "Couldn't find the player called " + args[1] + ".");
				return true;
			}

			if (!BountyHunters.getBountyManager().hasBounty(player)) {
				sender.sendMessage(ChatColor.RED + player.getName() + " does not have any bounty on him.");
				return true;
			}

			Bounty bounty = BountyHunters.getBountyManager().getBounty(player);
			BountyExpireEvent bountyEvent = new BountyExpireEvent(bounty, BountyExpireCause.ADMIN);
			Bukkit.getPluginManager().callEvent(bountyEvent);
			if (bountyEvent.isCancelled())
				return true;

			BountyHunters.getBountyManager().unregisterBounty(bounty);
		}

		// choose title
		if (args[0].equalsIgnoreCase("title")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player p = (Player) sender;
			if (!sender.hasPermission("bountyhunters.title")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			if (args.length < 2)
				return true;

			FileConfiguration levels = BountyHunters.getLevelsConfigFile();
			if (!levels.getConfigurationSection("reward.title").contains(args[1]))
				return true;

			PlayerData playerData = PlayerData.get(p);
			if (!playerData.hasUnlocked(args[1]))
				return true;

			playerData.setCurrentTitle(args[1]);
			p.playSound(p.getLocation(), VersionSound.ENTITY_PLAYER_LEVELUP.getSound(), 1, 2);
			Message.SUCCESSFULLY_SELECTED.format(ChatColor.YELLOW, "%item%", playerData.getTitle()).send(p);
		}

		// choose quote
		if (args[0].equalsIgnoreCase("quote")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player p = (Player) sender;
			if (!p.hasPermission("bountyhunters.quote")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			if (args.length < 2)
				return true;

			FileConfiguration levels = BountyHunters.getLevelsConfigFile();
			if (!levels.getConfigurationSection("reward.quote").contains(args[1]))
				return true;

			PlayerData playerData = PlayerData.get(p);
			if (!playerData.hasUnlocked(args[1]))
				return true;

			playerData.setCurrentQuote(args[1]);
			p.playSound(p.getLocation(), VersionSound.ENTITY_PLAYER_LEVELUP.getSound(), 1, 2);
			Message.SUCCESSFULLY_SELECTED.format(ChatColor.YELLOW, "%item%", playerData.getQuote()).send(p);
		}

		// choose title
		if (args[0].equalsIgnoreCase("titles")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player p = (Player) sender;
			if (!p.hasPermission("bountyhunters.title")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(p);
				return true;
			}

			Message.CHAT_BAR.format(ChatColor.YELLOW).send(p);
			Message.UNLOCKED_TITLES.format(ChatColor.YELLOW).send(p);

			PlayerData playerData = PlayerData.get(p);
			FileConfiguration levels = BountyHunters.getLevelsConfigFile();
			for (String s : levels.getConfigurationSection("reward.title").getKeys(false)) {
				String title = levels.getString("reward.title." + s + ".format");
				if (playerData.hasUnlocked(s)) {
					BaseComponent[] components = new ComponentBuilder("* " + ChatColor.GREEN + AltChar.apply(title))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("").append(Message.CLICK_SELECT.getUpdated()).color(net.md_5.bungee.api.ChatColor.WHITE).create()))
							.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bounties title " + s))
							.create();
					sender.sendMessage(components);
				}
			}
		}

		// quotes list
		if (args[0].equalsIgnoreCase("quotes")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "This command is for players only.");
				return true;
			}

			Player p = (Player) sender;
			if (!p.hasPermission("bountyhunters.quote")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(p);
				return true;
			}

			Message.CHAT_BAR.format(ChatColor.YELLOW).send(p);
			Message.UNLOCKED_QUOTES.format(ChatColor.YELLOW).send(p);

			PlayerData playerData = PlayerData.get(p);
			FileConfiguration levels = BountyHunters.getLevelsConfigFile();
			for (String s : levels.getConfigurationSection("reward.quote").getKeys(false)) {
				String quote = levels.getString("reward.quote." + s + ".format");
				if (playerData.hasUnlocked(s)) {
					BaseComponent[] components = new ComponentBuilder(ChatColor.GREEN + quote)
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("").append(Message.CLICK_SELECT.getUpdated()).color(net.md_5.bungee.api.ChatColor.WHITE).create()))
							.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bounties quote " + s))
							.create();
					sender.sendMessage(components);
				}
			}
		}

		// reload plugin
		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("bountyhunters.admin")) {
				Message.NOT_ENOUGH_PERMS.format(ChatColor.RED).send(sender);
				return true;
			}

			BountyHunters.plugin.reloadConfig();
			BountyHunters.plugin.reloadConfigFiles();

			FileConfiguration items = ConfigData.getCD(BountyHunters.plugin, "/language", "items");
			for (CustomItem i : CustomItem.values())
				i.update(items);

			sender.sendMessage(ChatColor.YELLOW + BountyHunters.plugin.getName() + " " + BountyHunters.plugin.getDescription().getVersion() + " reloaded.");
		}

		return false;
	}

}
