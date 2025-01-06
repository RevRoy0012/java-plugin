package com.example.gcmcPlugin;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration donorsConfig;
    private File donorsFile;
    private FileConfiguration nicknamesConfig;
    private File nicknamesFile;
    private final Map<UUID, TeleportRequest> teleportRequests = new HashMap<>();
    private final Map<UUID, BukkitRunnable> teleportTasks = new HashMap<>();
    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        getLogger().info("gcmcPlugin has been enabled!");
        createDonorsFile();
        createNicknamesFile();
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
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

    private void createRank(String teamName, String prefix) {
        if (!scoreboard.getTeams().contains(scoreboard.getTeam(teamName))) {
            Team team = scoreboard.registerNewTeam(teamName);
            team.setPrefix(prefix + ChatColor.RESET + " ");
        }
    }

    private void updatePlayerRank(Player player) {
        int level = donorsConfig.getInt("donors." + player.getName() + ".level", -1);
        String nickname = nicknamesConfig.getString(player.getUniqueId().toString());
        String nameToUse = nickname != null ? nickname : player.getName();

        ChatColor color = ChatColor.WHITE;
        String tag = "";

        switch (level) {
            case 5:
                color = ChatColor.DARK_RED;
                tag = ChatColor.DARK_RED + "[Netherite]";
                break;
            case 4:
                color = ChatColor.AQUA;
                tag = ChatColor.AQUA + "[Diamond]";
                break;
            case 3:
                color = ChatColor.GOLD;
                tag = ChatColor.GOLD + "[Gold]";
                break;
            case 2:
                color = ChatColor.GRAY;
                tag = ChatColor.GRAY + "[Silver]";
                break;
            case 1:
                color = ChatColor.DARK_GRAY;
                tag = ChatColor.DARK_GRAY + "[Bronze]";
                break;
        }

        String displayName = (tag.isEmpty() ? "" : tag + " ") + color + nameToUse;

        player.setDisplayName(displayName);
        player.setPlayerListName(displayName);
        player.setCustomName(displayName);

        String teamName = "donor_" + level;
        createRank(teamName, tag);
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.addEntry(player.getName());
        }
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

            if (level < 1 || level > 5) {
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

            updatePlayerRank(target);
            sender.sendMessage(ChatColor.GREEN + "Donation level applied for " + username + " with level " + level + ".");

            return true;
        } else if (command.getName().equalsIgnoreCase("setnick")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /setnick <nickname>");
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

            updatePlayerRank(player);
            player.sendMessage(ChatColor.GREEN + "Your nickname has been set to " + nickname + "!");

            return true;
        } else if (command.getName().equalsIgnoreCase("tpa")) {
            if (!(sender instanceof Player)) return true;
            Player requester = (Player) sender;
            if (args.length < 1) {
                requester.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                requester.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            if (target.equals(requester)) {
                requester.sendMessage(ChatColor.RED + "You cannot request to teleport to yourself.");
                return true;
            }
            if (teleportRequests.containsKey(target.getUniqueId())) {
                requester.sendMessage(ChatColor.RED + "This player already has a pending teleport request.");
                return true;
            }
            teleportRequests.put(target.getUniqueId(), new TeleportRequest(requester, target));

            TextComponent acceptButton = new TextComponent(ChatColor.GREEN + "[ACCEPT]");
            acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));

            TextComponent denyButton = new TextComponent(ChatColor.RED + "[DENY]");
            denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));

            target.spigot().sendMessage(new ComponentBuilder()
                    .append(ChatColor.YELLOW + requester.getName() + " has requested to teleport to you. ")
                    .append(acceptButton)
                    .append(" ")
                    .append(denyButton)
                    .create());
            requester.sendMessage(ChatColor.GREEN + "You have requested to teleport to " + target.getName() + ".");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (teleportRequests.containsKey(target.getUniqueId())) {
                        teleportRequests.remove(target.getUniqueId());
                        requester.sendMessage(ChatColor.RED + "The teleport request to " + target.getName() + " has expired.");
                        target.sendMessage(ChatColor.RED + "The teleport request from " + requester.getName() + " has expired.");
                    }
                }
            }.runTaskLater(this, 1200);
            return true;
        } else if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (!(sender instanceof Player)) return true;
            Player target = (Player) sender;
            TeleportRequest request = teleportRequests.get(target.getUniqueId());
            if (request == null) {
                target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
                return true;
            }
            teleportRequests.remove(target.getUniqueId());
            Player requester = request.getRequester();
            target.sendMessage(ChatColor.GREEN + "You have accepted the teleport request from " + requester.getName() + ".");
            requester.sendMessage(ChatColor.GREEN + "Your teleport request to " + target.getName() + " has been accepted. Teleporting...");

            BukkitRunnable teleportTask = new BukkitRunnable() {
                int countdown = 3;

                @Override
                public void run() {
                    if (countdown > 0) {
                        requester.sendTitle(ChatColor.YELLOW + "Teleporting in: " + countdown, "", 0, 20, 0);
                        countdown--;
                    } else {
                        requester.teleport(target.getLocation());
                        requester.sendTitle(ChatColor.GREEN + "Teleported!", "", 10, 70, 20);
                        teleportTasks.remove(requester.getUniqueId());
                        cancel();
                    }
                }
            };
            teleportTasks.put(requester.getUniqueId(), teleportTask);
            teleportTask.runTaskTimer(this, 0, 20);
            return true;
        } else if (command.getName().equalsIgnoreCase("tpdeny")) {
            if (!(sender instanceof Player)) return true;
            Player target = (Player) sender;
            TeleportRequest request = teleportRequests.get(target.getUniqueId());
            if (request == null) {
                target.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
                return true;
            }
            teleportRequests.remove(target.getUniqueId());
            Player requester = request.getRequester();
            target.sendMessage(ChatColor.RED + "You have denied the teleport request from " + requester.getName() + ".");
            requester.sendMessage(ChatColor.RED + "Your teleport request to " + target.getName() + " has been denied.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        teleportRequests.entrySet().removeIf(entry -> entry.getValue().getRequester().equals(player));
        updatePlayerRank(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (teleportTasks.containsKey(player.getUniqueId())) {
            teleportTasks.get(player.getUniqueId()).cancel();
            teleportTasks.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Teleportation canceled because you moved.");
        }
    }

    private static class TeleportRequest {
        private final Player requester;
        private final Player target;

        public TeleportRequest(Player requester, Player target) {
            this.requester = requester;
            this.target = target;
        }

        public Player getRequester() {
            return requester;
        }

        public Player getTarget() {
            return target;
        }
    }
}