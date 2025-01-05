package com.example.gcmcPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration donorsConfig;
    private File donorsFile;
    private FileConfiguration nicknamesConfig;
    private File nicknamesFile;

    @Override
    public void onEnable() {
        getLogger().info("gcmcPlugin has been enabled!");
        createDonorsFile();
        createNicknamesFile();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("gcmcPlugin has been disabled!");
    }

    private void createDonorsFile() {
        donorsFile = new File(getDataFolder(), "donors.yml");
        if (!donorsFile.exists()) {
            donorsFile.getParentFile().mkdirs();
            try {
                donorsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create donors.yml!");
                e.printStackTrace();
            }
        }
        donorsConfig = YamlConfiguration.loadConfiguration(donorsFile);
    }

    private void createNicknamesFile() {
        nicknamesFile = new File(getDataFolder(), "nicknames.yml");
        if (!nicknamesFile.exists()) {
            nicknamesFile.getParentFile().mkdirs();
            try {
                nicknamesFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create nicknames.yml!");
                e.printStackTrace();
            }
        }
        nicknamesConfig = YamlConfiguration.loadConfiguration(nicknamesFile);
    }

    private void saveDonors() {
        try {
            donorsConfig.save(donorsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save donors.yml!");
            e.printStackTrace();
        }
    }

    private void saveNicknames() {
        try {
            nicknamesConfig.save(nicknamesFile);
        } catch (IOException e) {
            getLogger().severe("Could not save nicknames.yml!");
            e.printStackTrace();
        }
    }

    private String getDonationTag(int level) {
        switch (level) {
            case 5:
                return ChatColor.DARK_RED + "⬥ Netherite ⬥";
            case 4:
                return ChatColor.AQUA + "◆ Diamond ◆";
            case 3:
                return ChatColor.GOLD + "★ Gold ★";
            case 2:
                return ChatColor.GRAY + "✧ Silver ✧";
            case 1:
                return ChatColor.DARK_GRAY + "✦ Bronze ✦";
            default:
                return null; // No donation tag
        }
    }

    private String getTierColor(int level) {
        switch (level) {
            case 5:
                return ChatColor.DARK_RED.toString();
            case 4:
                return ChatColor.AQUA.toString();
            case 3:
                return ChatColor.GOLD.toString();
            case 2:
                return ChatColor.GRAY.toString();
            case 1:
                return ChatColor.DARK_GRAY.toString();
            default:
                return ChatColor.WHITE.toString(); // Default color
        }
    }

    private void updateDisplayName(Player player) {
        // Retrieve donation data
        int level = donorsConfig.getInt("donors." + player.getName() + ".level", -1);
        String tag = level != -1 ? getDonationTag(level) : null;

        // Retrieve nickname
        String nickname = nicknamesConfig.getString(player.getUniqueId().toString());
        String nameToUse = nickname != null ? nickname : player.getName();

        // Construct display name
        String displayName;
        if (tag != null) {
            displayName = tag + " [" + nameToUse + "]";
        } else {
            displayName = "[" + nameToUse + "]";
        }

        // Apply display name
        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
        player.setCustomName(displayName);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("add_to_donators")) {
            if (!sender.hasPermission("gcmcPlugin.add_to_donators")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /add_to_donators <username> <level>");
                return true;
            }

            String username = args[0];
            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid level! Please enter a number between 1 and 5.");
                return true;
            }

            Player target = Bukkit.getPlayer(username);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            donorsConfig.set("donors." + target.getName() + ".level", level);
            saveDonors();

            updateDisplayName(target);
            sender.sendMessage(ChatColor.GREEN + "Donation tag applied and data saved!");

            return true;
        } else if (command.getName().equalsIgnoreCase("setnickname")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /setnickname <nickname>");
                return true;
            }

            String nickname = args[0];

            if (nickname.length() > 16) {
                player.sendMessage(ChatColor.RED + "Nickname is too long! Maximum length is 16 characters.");
                return true;
            }

            if (!nickname.matches("^[a-zA-Z0-9_]+$")) {
                player.sendMessage(ChatColor.RED + "Nickname contains invalid characters! Use only letters, numbers, and underscores.");
                return true;
            }

            nicknamesConfig.set(player.getUniqueId().toString(), nickname);
            saveNicknames();

            updateDisplayName(player);
            player.sendMessage(ChatColor.GREEN + "Your nickname has been set to " + nickname + "!");

            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateDisplayName(event.getPlayer());
    }
}