package com.example.donatorsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;

public class Main extends JavaPlugin {

    private FileConfiguration donorsConfig;
    private File donorsFile;
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        getLogger().info("DonatorsPlugin has been enabled!");

        createDonorsFile();
        createRanks("Founder", 6);
        createRanks("Netherite", 5);
    }

    @Override
    public void onDisable() {
        getLogger().info("DonatorsPlugin has been disabled!");
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

    private void saveDonors() {
        try {
            donorsConfig.save(donorsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save donors.yml!");
            e.printStackTrace();
        }
    }

    //          Used to replace ChatColor.<COLOR> + String to be down with basic code colors

    public static String color(String output){
        return ChatColor.translateAlternateColorCodes('&', output);
    }

    private String getDonationTag(int level) {
        switch (level) {
            case 6:
                return color("&c☀ Founder ☀");
            case 5:
                return color("&4⬥ Netherite ⬥");
            case 4:
                return color("&b◆ Diamond ◆");
            case 3:
                return color("&6★ Gold ★");
            case 2:
                return color("&7✧ Silver ✧");
            case 1:
                return color("&8✦ Bronze ✦");
            default:
                return color("&fDonator");
        }
    }


    //          This vvv Can be used to create a custom name plate in Tablist, Overhead, and in Chat

    private void createRanks(String Name, int Prefix){
        if (!scoreboard.getTeams().contains(Name)){
            Team team = scoreboard.registerNewTeam(Name);
            team.setPrefix(getDonationTag(Prefix) + " ");
        }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("add_to_donators")) {
            if (!sender.hasPermission("donatorsplugin.add_to_donators")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /add_to_donators <username> <level> <color/hex>");
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

            String colorInput = args[2];
            String colorCode;

            try {
                ChatColor color = ChatColor.valueOf(colorInput.toUpperCase());
                colorCode = color.toString();
            } catch (IllegalArgumentException e) {
                if (colorInput.matches("#[0-9A-Fa-f]{6}")) {
                    colorCode = net.md_5.bungee.api.ChatColor.of(colorInput).toString();
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid color! Use predefined colors or a valid hex code (e.g., #FF0000).");
                    return true;
                }
            }

            Player target = Bukkit.getPlayer(username);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            String donationTag = getDonationTag(level);

            target.setDisplayName(donationTag + " " + colorCode + target.getName() + ChatColor.RESET);
            sender.sendMessage(ChatColor.GREEN + "Updated " + target.getName() + "'s display name with donation tag!");

            donorsConfig.set("donors." + target.getName() + ".level", level);
            donorsConfig.set("donors." + target.getName() + ".tag", donationTag);
            donorsConfig.set("donors." + target.getName() + ".color", colorInput);
            saveDonors();
            sender.sendMessage(ChatColor.GREEN + "Donation data saved!");

            return true;
        }

        return false;
    }
}